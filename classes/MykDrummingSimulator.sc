/** builds a multi order markov chain
* from incoming midi events by observing
* windowed events. Depends on MykMarkov and mykMidiObserver
* uses either an OSC /step message or an inernal
* clock to set the windows
*/
MykDrummingSimulator : Object {
	var channel, viz_filename, tidal_host, tidal_port,
	<mykMidiObserver, <mykMarkov, mykClock, oscResponder, training, simulating, <midi_out, rests_obs, osc_tick_count, >click_out, id_lookup, viz_enabled = false, tidal_enabled, >midiout_channel=9;

  	*new{arg channel = 0, viz_filename = "/Users/matthew/Desktop/canute/canute.json", tidal_host, tidal_port, enable_viz = false, midi_dev = 0;
		^super.newCopyArgs(channel, viz_filename, tidal_host, tidal_port).prInit(enable_viz, midi_dev);
	}

	prInit{arg enable_viz, midi_dev;
	 	MIDIClient.init;
		midi_out = MIDIOut(midi_dev);
		("MYKdrumsim: connected to MIDI out: "++MIDIClient.destinations[midi_dev]).postln;
		click_out = 0;
		id_lookup = MykObjId.new;
		tidal_enabled = false;
		if (tidal_host != nil, {
			tidal_enabled = true;
		});
		// used to decide which click track sound to play
		// when getting an osc clock message
		osc_tick_count = 0;
		training = true;
		simulating = false;
		this.sendSynthDefs;
		mykMarkov = MykMarkov.new;
		if (enable_viz,{this.enableVizData(viz_filename);});
		mykMidiObserver = MykMidiObserver.new(channel);
		mykMidiObserver.debug = false;
		// create an internal clock in case we don't want to
		// use external osc messages
		mykClock = MykClock.new;
		// when we have collected observations in a step...
		mykMidiObserver.callback = {arg observation;
			if (viz_enabled, {
				// somehow inform the vizualiser that
				var obs_id = id_lookup.getId(observation.asCompileString);
				this.sendBangToVisualiser(obs_id);
			});
			if (training, {
				// check for an empty observation
				var all_false = true;
				observation.do{arg obs; if (obs == true, {all_false = false});};
				// if we got an empty one, count it
				// if not, reset the counter
				if (all_false, {rests_obs = rests_obs + 1; ("rests "++rests_obs).postln;}, {rests_obs = 0});
				// make sure we don't send too many empty ones
				if (rests_obs < 8, {
				"sending data to markov".postln;
				mykMarkov.addFreq(observation);
				});
			});
		}
	}
	/** control the simulator from an external OSC clock message */
	useOscClock {arg pattern = "/sync";
		this.stopClock;
		oscResponder = OSCFunc.newMatching({|msg, time, addr, recvPort|
//			msg.postln;
			if (osc_tick_count == 0,  {this.tickSound}, {this.tockSound});
			if (training, {mykMidiObserver.step});
			if (simulating, {this.generateMidi});
			if (tidal_enabled, {
				if (osc_tick_count == 0,  {
					this.generateTidal;
				});
			});
			osc_tick_count = (osc_tick_count + 1) % 4;
		}, pattern); // path matching
	}
	/** control the simulator from an internal clock */
	useInternalClock {
		this.stopClock;
		this.startClock();
	}
	bpm{arg bpm = 120;
		mykClock.bpm(bpm);
	}
	/** stop writing viz data to the file */
	disableVizData{
		mykMarkov.chain_callbacks = [{arg state, transitions;}];
		viz_enabled = false;
	}

	genTestMidi{
		MIDIIn.doNoteOnAction(1, 1, rrand(30, 40), 64); // spoof a note on
	}

	/** start writing viz data. optionally send in a filename arg */
	enableVizData{arg filename = false;
		var f;
		viz_enabled = true;
		if (filename == false, {filename = viz_filename;});
		mykMarkov.chain_callbacks = [{arg state, transitions;
			var last_state, obs_counts, obs_sum, trans_str;
			// key[0] will be the most recently observed state
			last_state = id_lookup.getId(state[0]);
			obs_counts = Dictionary.new;
			transitions.do{arg next_state;
				next_state = id_lookup.getId(next_state.asCompileString);
				if (obs_counts.at(next_state) == nil, {obs_counts.put(next_state, 0)});
				obs_counts.put(next_state, obs_counts.at(next_state) + 1);
			};
			obs_sum = obs_counts.sum;
			// build a tr
			obs_counts.keys.do{arg key, i;
				var count;
				count = obs_counts.at(key);
				obs_counts.put(key, count / obs_sum);
			};
			this.sendNodeToVisualiser(last_state, obs_counts, filename);
		}];
	}
	/** todo - put the file writing code into this function instead of */
	sendNodeToVisualiser{arg node_id, edge_data, filename;
		var trans_str, f;
		// build a tr
		trans_str = "";
		edge_data.keys.do{arg key, i;
			var prob;
			prob = edge_data.at(key);
			trans_str = trans_str ++ "\n\{\"from\":" ++ node_id ++ ",\"to\":"++key;
			trans_str = trans_str ++ ", \"value\":"++(prob * 50)++ "},";
		};
		// trim off the last comma
		trans_str = trans_str[0 .. trans_str.size - 2];
		f=File(filename, "w");
		f.write("{\n\"node\":"++node_id++",\n");
		f.write("\"edges\":\n["++ (trans_str) ++ "\n]}");
		f.close;
	}
	/** causes the visualiser to highlight one of the
	* nodes temporarily
	*/
	sendBangToVisualiser{arg node_id;
		var f;
//		viz_filename
		f=File(viz_filename, "w");
		f.write("{\n\"node\":"++node_id++",\n");
		f.write("\"edges\":\n[]}");
		f.close;
	}

	/** start training */
	train{
		training = true;
		simulating = false;
	}

	/** call this to run the simulation */
	simulate{
		simulating = true;
		training = false;
	}
	/** will generate some midi events using the training data*/
	generateMidi{
		var state, notes;
		state = mykMarkov.nextFreq;

		if (viz_enabled, {
			// somehow inform the vizualiser that
			var obs_id = id_lookup.getId(state.asCompileString);
			this.sendBangToVisualiser(obs_id);
		});

		notes = [];
		state.collect{|is_on, note|
			if(is_on, {
				notes = notes.add(note);
			});
		};
		notes.postln;
		notes.do{arg note;
			var vel = rrand(40, 96);
			midi_out.noteOn(midiout_channel, note, vel);
			// send midi out some point later...
			SystemClock.sched(0.25, {
				midi_out.noteOff(midiout_channel, note, vel);
			});
		}
	}

	generateTidal{
		var state, notes, tidal_osc, str;
		notes = [];
		str = "";
		16.do{arg i;
			//notes = notes.add([]);
			notes = [];
			state = mykMarkov.nextFreq;
			state.collect{|is_on, note|
				if(is_on, {
					notes = notes.add(note);
				});
			};
			str = str ++ " " ++ (notes.asCompileString);
		};
//		"tidal notes".postln;
//		str.postln;
		tidal_osc = NetAddr.new(tidal_host, tidal_port);
		tidal_osc.sendMsg("/tidal", str);
	}

	stop{
	}

	resetMarkov{
		mykMarkov = MykMarkov.new;
	}

	free{
		mykMidiObserver.free;
		oscResponder.free;
	}

	/** start the intermal clock*/
	startClock{
		mykClock.run;
		mykClock.add(0,
			{
				if (training, {"MykDrumSim: calling step on midi obs".postln;mykMidiObserver.step});
				if (simulating, {this.generateMidi});
				if (tidal_enabled, {this.generateTidal;});
			},
			[1.0],
			[1]);
		mykClock.add(1,
			{this.tickSound},
			[1.0],
			[1,0,0,0]);
		mykClock.add(2,
			{this.tockSound;},
			[1.0],
			[0,1,1,1]);
	}

	tickSound{
		Synth("MykDrummingSimulator_tick", [\freq, 400, \out, click_out]);
	}
	tockSound{
		Synth("MykDrummingSimulator_tick", [\freq, 200, \out, click_out]);
	}

	/** stop the internal clock */
	stopClock{
		mykClock.remove(0);
		mykClock.remove(1);
		mykClock.remove(2);
		mykClock.stop;
	}

	sendSynthDefs{
		SynthDef("MykDrummingSimulator_tick", {arg freq = 400, out = 0;
			var c;
			c = WhiteNoise.ar(0.25);
			c = c + SinOsc.ar(freq * 2);
			c = c * Line.kr(0.5, 0, 0.05, doneAction:2);
			Out.ar(out, c);
		}).add;
	}
}