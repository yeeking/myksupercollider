MykBeatTrack : Object {

	var audio_in, >callback, osc_id, osc_resp, bt_synth;

	*new{arg audio_in = 1, callback = {arg amp;("MykBeatTrackTempo: "++amp).postln;}, osc_id = 301 ;
		^super.newCopyArgs(audio_in, callback, osc_id).prInit;
	}


	prInit{
		var server;
		this.sendSynthDefs;
		server = Server.local;
		// the osc responder which responds to onsets
		osc_resp = OSCresponderNode(server.addr, '/tr', {arg time,responder,msg;
			if (msg[2] == osc_id, {
				//("MykBeatTrack: bonk"++[time, responder, msg]).postln;
				callback.value(msg[3]);
			});
		}).add;

		//sync message in a bundle with a time stamp
		//2 params:
		// number of ticks since start up (float)
		// tempo - beats per second
		// port 6060
	}

	run{
		bt_synth = Synth("MykBeatTrack", [\rec_in, audio_in, \osc_id_no, osc_id]);
	}

	free{
		bt_synth.free;
		osc_resp.remove;
	}

	sendSynthDefs{
		var server;
		server = Server.local;
		SynthDef("MykBeatTrack", {arg rec_in = 1, osc_id_no, sens = 0.2;

			var trackb,trackh,trackq,tempo;
			var source;
			var bsound,hsound,qsound;

			source= SoundIn.ar(0);

			#trackb,trackh,trackq,tempo=BeatTrack.kr(FFT(LocalBuf(1024, 1), source));

	//		bsound= Pan2.ar(LPF.ar(WhiteNoise.ar*(Decay.kr(trackb,0.05)),1000),0.0);

//			hsound= //Pan2.ar(BPF.ar(WhiteNoise.ar*(Decay.kr(trackh,0.05)),3000,0.66),-0.5);

			//qsound= Pan2.ar(HPF.ar(WhiteNoise.ar*(Decay.kr(trackq,0.05)),5000),0.5);

			//Out.ar(0, bsound+hsound+qsound);
			SendTrig.kr(trackb, osc_id_no, tempo);

		}).send(server).writeDefFile;
	}
}