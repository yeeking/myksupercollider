/**
 * useful stuff for working with vdrums
 *
 */
MykVdrum : Object {
  var midi_out, midi_in, note_on_func, all_funcs;

	*new{
		^super.new.prInit;
	}

	prInit{
		all_funcs = [];
		// we'll use this to send midi to the vdrums
		midi_out = MykMidi.new;
		// connect to all the available midi outs
		MIDIClient.destinations.do{arg name, i;
			MIDIIn.connect(i, MIDIClient.sources.at(i));
		};
	}

	// assuming local control has been turned off in the vdrums
	// this will map velocity to note length
	// using a cc 7 volume message.
	// It is not very subtle of course, e.g. when you have
	// multiple simultaneous triggers
	velToLengthMode{arg factor = 0.5;
		MIDIIn.removeFuncFrom(\noteOn, note_on_func);
		note_on_func = { arg src, chan, num, vel;
			("on and off"++num).postln;
			midi_out.cc(9, 7, 127);
			midi_out.playNote(9, num, vel);
			midi_out.cc(9, 7, 0, wait:((vel * factor) / 127.0));
		};
		all_funcs=  all_funcs.add(note_on_func);
		MIDIIn.addFuncTo(\noteOn, note_on_func);
	}

	// do something when they hit a pad
	on{arg pad = 'sn', callback = {arg vel, note; ['vdrum pad ', note, vel].postln};
		var pad_func;
		// decide which pad they hit
		pad_func = {arg src, chan, num, vel;
			if (this.pad_to_label(num) == pad, {
				callback.value(vel);
			});
			};
		// remember the function for later, so we can remove
		// it on reset
		all_funcs = all_funcs.add(pad_func);
		MIDIIn.addFuncTo(\noteOn, pad_func);

	}

	free{
		all_funcs.do{arg rm_func;
			MIDIIn.removeFuncFrom(\noteOn, rm_func);
		};
	}

	pad_to_label{arg pad;
			var p2l;
			p2l = (36:'bd', 38:'sn', 40:'rim', 48:'tom1', 45:'tom2', 46:'hh');
			^p2l[pad];
	}


}