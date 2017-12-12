/**
* General purpose midi markov system
* trains on a live midi input and regenerates
* using it
*/
MykMidiMarkov : Object {
	var channel, log_filename, <mykMidiObserver, <mykMarkov, mykMarkov2, <id_lookup, clock, rests_obs, training, generating, >interval, total_rests, total_notes, midiout, max_rests, energy, >synthMode, phase;

	*new{arg channel = 0, log_filename = "/home/scratch/log.txt", midi_dev = 0;

		^super.newCopyArgs(channel, log_filename).prInit(midi_dev);
	}

	prInit{arg enable_viz, midi_dev;
	 	//MIDIClient.init;
		// used to map ids to objects for quick retrieval
		id_lookup = MykObjId.new;

		clock = MykClock.new;
		total_rests = 1;
		total_notes = 1;
		interval = 0.125;
		max_rests = 4;
		max_rests = 1/interval * 8;
		energy = 1;
		phase = 0;

		mykMarkov = MykMarkov.new;
		training = true;
		generating = true;

		synthMode = false;

		midiout = MykMidi.new;

		rests_obs = 0;

		mykMidiObserver = MykMidiObserver.new(channel);
		mykMidiObserver.debug = false;

		mykMidiObserver.callback = {arg observation;
			var obs_id, all_false;

			if (training,  {
				//observation.postln;

				// check for an empty observation
				all_false = true;
				observation.do{arg obs; if (obs == true, {all_false = false});};
				// remove all the false items
				observation = observation.reject { |item| item == false};
				// generate an id for the state
				obs_id = id_lookup.getId(observation.asCompileString);

				// if we got an empty one, count it
				// if not, reset the counter
				if (all_false, {
					rests_obs = rests_obs + 1;
					// now prepare a rest to the chain
					obs_id = id_lookup.getId('rest');
				}, {// we saw an event
					observation.postln;
					rests_obs = 0;

				});
				// make sure we don't send too many empty ones
				if (rests_obs < max_rests, {
					"MYKMidiMarkov: sending data to markov".postln;

					mykMarkov.addFreq(obs_id);
					// store the number of rests or non-rests
					if (all_false, {// stored a rest
						total_rests = total_rests + 1;
					}, {// else stored a note
							total_notes = total_notes + 1;
					});
				});
			});
		};
		// periodically step the midi observer
		// which will trigger the observed events in the
		// previous step being sent through to the markov
		clock.add(1, {mykMidiObserver.step;}, [interval]);
		// periodically request
		clock.add(2, {
			if (generating, {
				this.generateMidiEvents;
			});
		}, [interval]);
		this.sendSynthDefs;
		clock.run;
	}

	train{
		training = true;
	}

	generate{
		generating = true;
	}
	generateMidiEvents1{
	}
	generateMidiEvents{
		var id, dict, note_rest_ratio;
		//"MykMIDIMark: generating midi out".postln;

		phase = phase + 0.01;
		energy = (sin(phase) + 1)*0.5;

		//energy.postln;
		if (0.05.coin, {
			energy = 0.75;
			phase = 0;
		});
/*		if (0.001.coin, {
			this.reset;
		});*/

		// note based on the ratio of rests to notes
		// then force that state onto the markov state
		// model
		note_rest_ratio = 1 / (total_notes / total_rests);
		id = mykMarkov.nextFreq;
			// now decide if this will ba  rest or a
	//id = id_lookup.getId("rest");
		dict = id_lookup.getObj(id);
		// now trigger the notes
		if (((dict != nil) && (dict.size != 0)), {// something to do here
			// convert the string into a
			// dictionary
			dict = dict.compile.value;
			dict.keys.do{arg n;
				this.playNote(n);
			};
		}, {
			//"nil dict or zero size dict".postln;
			id.postln;
		}
		);

	}

	playNote{arg note;
		//("MykMIDIMark: playing note "++note).postln;
		//SystemClock.sched(5, {"done".postln;});
		var len, vel;
		len = ((1.0 - energy) * 2.0) + 0.25;
		vel = (energy * 45) + 40;

		if(synthMode == true, {// synth mode
			Synth("markovSynth", [
				\note, note,
				\vel,vel,
				\len, len
			]);
		},
		{// midi mode
			midiout.playNote(0, note, vel, len);
		});


	}

	stop{
		training = false;
		generating = false;
	}

	reset{
		mykMarkov = MykMarkov.new;
		rests_obs = 0;
	}

	sendSynthDefs{
		SynthDef("markovSynth", {arg note, vel, len;
			var c;
			c = SinOsc.ar(note.midicps);
			c = c * Line.kr(0.25, 0, 0.5, doneAction:2);
			Out.ar([0, 1], c);
		}).add;

	}

	setupSynthMode{
		synthMode = true;
		SynthDef("markovSynth", {arg note, vel, len;
			var c;
			c = Saw.ar(note.midicps);
			c = c + Pulse.ar((note - 24).midicps);
			c = RLPF.ar(c, Line.kr(0, min(note.midicps*10, 7000), len*0.1), 2.0);
			c = c * Line.kr(0.25 * (vel / 127), 0, 0.1+len, doneAction:2);
			Out.ar(30, c);
		}).add;

		SynthDef("markovRev", {
			var c;
			c = In.ar(30);
			4.do{
				c = AllpassC.ar(c, 0.05, Rand(0.01, 0.05), 15.0);
			};
			Out.ar([0, 1], c);
		}).add;

		{Synth("markovRev")}.defer(1);

	}


}