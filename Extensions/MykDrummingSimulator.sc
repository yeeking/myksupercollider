/** builds a multi order markov chain
* from incoming midi events by observing
* windowed events. Depends on MykMarkov and mykMidiObserver
* uses either an OSC /step message or an inernal
* clock to set the windows
*/
MykDrummingSimulator : Object {
	var channel,
	mykMidiObserver, <mykMarkov, mykClock, oscResponder;

  	*new{arg channel = 0;
		^super.newCopyArgs(channel).prInit;
	}

	prInit{
		this.sendSynthDefs;
		mykMarkov = MykMarkov.new;
		mykMidiObserver = MykMidiObserver.new(channel);

		// create an internal clock in case we don't want to
		// use external osc messages
		mykClock = MykClock.new;

		// when we have collected observations in a step...
		mykMidiObserver.callback = {arg observation;
			mykMarkov.addFreq(observation);
		}

	}
	/** control the simulator from an external OSC clock message */
	useOscClock {arg msg = "clock";
		this.stopClock;

	}
	/** control the simulator from an internal clock */
	useInternalClock {arg out_channel = 0;
		this.stopClock;
		this.startClock(out_channel);
	}

	resetMarkov{
		mykMarkov = MykMarkov.new;
	}

	free{
		mykMidiObserver.free;
		oscResponder.free;
	}

	/** start the intermal clock*/
	startClock{arg out_channel;
		mykClock.run;

		mykClock.add(0,
			{mykMidiObserver.step},
			[0.5],
			[1,0,0,0]);

		mykClock.add(1,
			{Synth("MykDrummingSimulator_tick", [\freq, 400, \out, out_channel])},
			[0.5],
			[1,0,0,0]);
		mykClock.add(2,
			{Synth("MykDrummingSimulator_tick", [\freq, 200, \out, out_channel])},
			[0.5],
			[0,1,1,1]);
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
			c = WhiteNoise.ar(0.5);
			c = c + SinOsc.ar(freq);
			c = c * Line.kr(0.5, 0, 0.125, doneAction:2);
			Out.ar(out, c);
		}).add;
	}


}