// lang side onset detector like bonk in pd
MykBonk : Object{

  var audio_in, >callback, osc_id, auto_calibrate, sens,
	osc_resp, onset_synth, calib;

  *new{arg audio_in = 0, callback = {arg amp;("MykBonk: "++amp).postln;}, osc_id = 301, auto_calibrate=false, sens = 0.2, auto_run = false;
		^super.newCopyArgs(audio_in, callback, osc_id, auto_calibrate, sens).prInit(auto_run);
  }

  prInit{arg auto_run;
		var server;
		this.sendSynthDefs;
		// little trick to make creating multiple onsets on different inputs easier!
		osc_id = osc_id + audio_in;
		if (auto_calibrate, {calib = MykCalib.new;});
		server = Server.local;
		// the osc responder which responds to onsets
		osc_resp = OSCresponderNode(server.addr, '/tr', {arg time,responder,msg;
			//("MykBonk: bonk"++[time, responder, msg]).postln;
			if (msg[2] == osc_id, {
				//("MykBonk: bonk"++[time, responder, msg]).postln;
				if (auto_calibrate,
					{callback.value(calib.getCalib(msg[3]));},
					{callback.value(msg[3]);}
				);

			});
		}).add;
		if (auto_run, {
			{this.run;}.defer(0.25);
		});
  }

  run{
		onset_synth = Synth("MykBonkOnset", [\rec_in, audio_in, \osc_id_no, osc_id]);
		{this.setSens(sens)}.defer(0.5);
  }

  free{
	onset_synth.free;
	osc_resp.remove;
  }

  // change the sensitivity
  setSens{arg sens;
	onset_synth.set(\sens, sens);
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef("MykBonkOnset", {arg rec_in = 0, osc_id_no, sens = 0.2;
	  var onsets, fft, in, amp;
	  //in = SoundIn.ar(rec_in);
	  //in = AudioIn.ar(rec_in);
		in = SoundIn.ar(rec_in);

	  fft = FFT(LocalBuf.new(1024, 1), in, hop:0.25);
	  onsets = Onsets.kr(fft, sens, \rcomplex);
	  amp = Amplitude.kr(in, 0.01, 0.1);
	  SendTrig.kr(onsets, osc_id_no, amp);
	}).send(server).writeDefFile;
  }
}