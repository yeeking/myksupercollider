MykBCI : Object {
	var osc_resp, >callback, <last_msg, <attention, <meditation, <spectrum, sonif, spec_max;

	*new {
		^super.newCopyArgs.prInit;
	}

	prInit{
		last_msg = nil;
		spec_max = 10000;
		OSCFunc.newMatching({|msg, time, addr, recvPort|
			last_msg = msg[1..];
			meditation = last_msg[9];
			attention = last_msg[10];
			this.setSpectrum(last_msg[1..8]);
			callback.value(spectrum, attention, meditation);
		}, '/brain');
		callback = {arg spec, att, med;
			"MykBCI".postln;
			[spec, att, med].postln;
		};
		this.sendSynthDefs;
	}
	// adaptively normalises the spectrum
	setSpectrum{arg raw_spectrum;
		var temp_max;
		temp_max = raw_spectrum.maxItem;
		if (temp_max > spec_max, {spec_max = temp_max;});
		spectrum = raw_spectrum / spec_max;
	}

	sonify{
		sonif = Synth("MykBCI_son");
		callback = {
			sonif.set(\s1, spectrum[0]);
			sonif.set(\s2, spectrum[1]);
			sonif.set(\s3, spectrum[2]);
			sonif.set(\s4, spectrum[3]);
			sonif.set(\s5, spectrum[4]);
			sonif.set(\s6, spectrum[5]);
			sonif.set(\s7, spectrum[6]);
			sonif.set(\s8, spectrum[7]);
		}
	}

	pitch{arg midi_note;
		sonif.set(\bf, midi_note.midicps);
	}


	free{
		sonif.free;
		callback = {};
	}

	sendSynthDefs{
		SynthDef("MykBCI_son", {arg bf = 200, s1,s2,s3,s4,s5,s6,s7,s8;
			var c1, c2, spectrum;
			//bf = Lag.kr(bf, LFDNoise0.kr(2).range(0.1,0.5));
			spectrum = [s1,s2,s3,s4,s5,s6,s7,s8];
			c1 = Array.fill(8, {arg i;
				var f;
				f = (i+1) * rrand(0.95, 1.05) * bf;
				SinOsc.ar(f, mul:Lag.kr(spectrum[i], 5.0));
			}).mean;

			c2 = Array.fill(8, {arg i;
				var f;
				f = (i+1) * rrand(0.95, 1.05) * bf;
				SinOsc.ar(f, mul:Lag.kr(spectrum[i], 10.0));
			}).mean;
			Out.ar(0, [c1, c2]);
		}).add;
	}


}