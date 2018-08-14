/**
 * simple midi wrapper class
 *
 */
MykMidi : Object {
	var midi_out, midi_in;

	*new{
		^super.new.prInit;
	}

	prInit{
		MIDIClient.init;
		MIDIClient.destinations;
		try{
			//"MYKMidi: Connecting to MIDI out device: Fireface 800 (40D)".postln;
			//midi_out = MIDIOut.newByName("Fireface 800 (40D)", "Port 1");
			//midi_out = MIDIOut.newByName("Akai MPD24-Akai MPD24 MIDI 2");

			//midi_out = MIDIOut.new(3);
			midi_out = MIDIOut(0);// this works for linux and qjackctl connected

			midi_out.latency = 0;


		};

	}
	// kill all notes
	panic{
		16.do{arg chan;
			127.do{arg note;
				midi_out.noteOff(chan, note, 0);
			};
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


	noteOn{arg chan = 0, num = 64, vel = 64, len = 0.5;
		this.playNote(chan, num, vel, len);
	}

	cc{arg chan = 0, num = 0, val = 64, wait = 0;
		if (this.prHaveMidiOut(), {
			SystemClock.sched(wait, {
				midi_out.control(chan, num, val);
			});
		});
	}

	prHaveMidiOut{
		if (midi_out == nil, {
			"MykMidi: no midi out available...".postln;
			^false;
		}, {
			^true;
		});
	}
}