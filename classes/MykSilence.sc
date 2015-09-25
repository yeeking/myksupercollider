// silence detector. calls the callback when silence
// begins

MykSilence : Object {
	var  >threshold, >callback,  oscFunc, silent, sensor;

	*new{arg threshold = 0.1, callback = {arg amp; ("silence starts" ++ amp).postln};
		^super.newCopyArgs(threshold, callback).prInit;
	}
	prInit{
		this.sendSynthDefs;
		silent = 0;
		oscFunc = OSCFunc({ arg msg, time;
			var amp = msg[3];
			//[silent, amp].postln;
			if ((amp < threshold) && (silent ==0) , {
				silent = 1;
				//"silence starts ".postln;
				callback.value(amp);
			});
			if ((amp >= threshold) && (silent == 1) , {
				silent = 0;
				//"silence ends ".postln;
			});
		},'/tr', Server.local.addr);
	}


	run{
		sensor = Synth("MykSilence_detect");
	}

	free{
		sensor.free;
		oscFunc.free;
	}

	sendSynthDefs{
		SynthDef("MykSilence_detect", {
			var in, in2, amp;
			// change to sound in for live
			//in = In.ar(30);
			in = SoundIn.ar(0);
			in2 =  CompanderD.ar(in, in,
				thresh: 0.1,
				slopeBelow: 100,
				slopeAbove: 1,
				clampTime: 0.001,
				relaxTime: 0.01
			);
			amp = Amplitude.kr(in2);
			//amp.poll;
			SendTrig.kr(Impulse.kr(10), 99, amp);
			//Out.ar([0, 1], in2);
		}).add;
	}
}