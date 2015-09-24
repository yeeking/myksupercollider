/**
 * simple midi wrapper class
 *
 */
MykMidi : Object {
	var midi_out;

	*new{
		^super.new.prInit;
	}

	prInit{
		MIDIClient.init;
		MIDIClient.destinations;
		try{
			midi_out = MIDIOut.newByName("Fireface 800 (40D)", "Port 1");
			midi_out.latency = 0;

		};

	}

	playNote{arg chan = 0, num = 64, vel = 64, len = 0.5;
		if (midi_out == nil, {
			"MykMidi: no midi out available...".postln;
		}, {
			midi_out.noteOn(chan, num, vel);
			// send midi out some point later...
			SystemClock.sched(len, {
				midi_out.noteOff(chan, num, vel);
			});
		});
	}
}