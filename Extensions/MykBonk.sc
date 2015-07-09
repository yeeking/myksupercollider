// lang side onset detector like bonk in pd
MykBonk : Object{
  
  var audio_in, >callback, osc_id, osc_resp, onset_synth;
  
  *new{arg audio_in = 1, callback = {arg amp;("MykBonk: "++amp).postln;}, osc_id = 301 ;
	^super.newCopyArgs(audio_in, callback, osc_id).prInit;
  }

  prInit{
	var server;
	this.sendSynthDefs;
	server = Server.local;
	// the osc responder which responds to onsets
	osc_resp = OSCresponderNode(server.addr, '/tr', {arg time,responder,msg;
	  //("MykBonk: bonk"++[time, responder, msg]).postln;
	  if (msg[2] == osc_id, {
		//("MykBonk: bonk"++[time, responder, msg]).postln;
		callback.value(msg[3]);
	  });
	}).add;
  }

  run{
	onset_synth = Synth("MykBonkOnset", [\rec_in, audio_in, \osc_id_no, osc_id]);
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
	SynthDef("MykBonkOnset", {arg rec_in = 1, osc_id_no, sens = 0.2;
	  var onsets, fft, in, amp;
	  //in = SoundIn.ar(rec_in);
	  in = AudioIn.ar(1);
	  //fft = FFT(LocalBuf.new(1024, 1), in)
	  fft = FFT(LocalBuf.new(1024, 1), in, hop:0.25);
	  onsets = Onsets.kr(fft, sens, \rcomplex);
	  amp = Amplitude.kr(in, 0.01, 0.1);
	  SendTrig.kr(onsets, osc_id_no, amp);
	}).send(server).writeDefFile;
  }
}