PV_fx : Object{
  
  var 
  in_bus, out_bus, ctls, channel, 
  // things that need to be freed
  fft_buff, fx_synth, cc_resp, note_resp, 
  // state
  freeze, smear, threshold, stretch, shift;
  
  *new { arg in_bus=1, out_bus=0, ctls = [1, 2, 3, 4, 5, 6], channel=0;
	^super.newCopyArgs(in_bus, out_bus, ctls, channel).prInit;
  }
  
  prInit{
	var server;
	server = Server.local;
	fft_buff = Buffer.alloc(server, 1024, 1);
	this.sendSynthDefs;
	freeze = 0;
	smear = 10;
	threshold = 10;
	stretch = 10;
	shift = 0;	
	cc_resp = CCResponder ({|src, chan, num, val|
	  //[src, chan, num, val].postln;
	  this.updateCtl(num, val);
	}, 
	  nil, channel, ctls, nil
	  //nil, nil, nil, nil
	);
  }
  
  run{
	fx_synth = Synth("PV_fx1", [
	  \in_bus, in_bus, \out_bus, out_bus, \buffer, fft_buff, \freeze, freeze, \smear, smear, \threshold, threshold, \stretch, stretch, \shift, shift]);
	
  }

  updateCtl{arg num, val;
	var midiTo1;
	midiTo1 = 1/127;
	val = val * midiTo1;
	num.postln;
	ctls[num].postln;
	switch (num, 
	  //ctls[0], {fx_synth.set([\freeze, val])}, 
	  //ctls[0], {fx_synth.set([\smear, val])}, 
	  ctls[0], {fx_synth.set([\threshold, val*20])}, 
	  ctls[1], {fx_synth.set([\stretch, (val*10)+1])}, 
	  ctls[2], {fx_synth.set([\shift, (val*400)-200])}, 
	  ctls[3], {fx_synth.set([\teeth, (val*1000)+2])}, 
	  ctls[4], {fx_synth.set([\comb_width, (val*0.5) + 0.5])}, 
	  ctls[5], {fx_synth.set([\length, val])}
	);
  }

  free{
	fft_buff.free;
	fx_synth.free;
	cc_resp.remove;
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef("PV_fx1", {arg in_bus, out_bus, buffer, freeze = 0, smear = 0, threshold = 0, stretch = 1, shift = 0,teeth=1, comb_width=1, length=1;
	  var in, chain;
	  freeze = Lag.kr(freeze, LFDNoise1.kr(0.1).range(0.1, 4));
	  smear = Lag.kr(smear, LFDNoise1.kr(0.1).range(0.1, 4));
	  threshold = Lag.kr(threshold, LFDNoise1.kr(0.1).range(0.1, 4));
	  stretch = Lag.kr(stretch*0.1, LFDNoise1.kr(0.1).range(0.1, 4));
	  shift = Lag.kr(shift*0.1, LFDNoise1.kr(0.1).range(0.1, 4));
	  teeth = Lag.kr(teeth, LFDNoise1.kr(0.1).range(0.1, 4));
	  comb_width = Lag.kr(comb_width, LFDNoise1.kr(0.1).range(0.1, 4));
	  
	  in = AudioIn.ar(in_bus);
	  //in = In.ar(in_bus);
	  // noise gate
	  in = Compander.ar(in, in,
		thresh: 0.01, 
		slopeBelow: 10,
		slopeAbove: 1,
		clampTime: 0.01,
		relaxTime: 0.01
	  );
	  
	  //in = Compander.ar(in, in, thresh:0.5, slopeBelow:0.2,slopeAbove:0.01,clampTime:0.01,relaxTime:0.7);
	  //in = In.ar(0);
	  chain = FFT(buffer, in);
	  chain = PV_MagFreeze(chain, freeze>0);
	  chain = PV_MagAbove(chain, threshold);
	  chain = PV_MagShift(chain, stretch, shift);
	  // chain = PV_MagSmear(chain, smear);
	  chain = PV_RectComb(chain, teeth, comb_width);
	  chain = IFFT(chain);
	  //chain = chain * boost;
	  
	  chain = Compander.ar(chain, chain, 
		thresh: 0.5, 
		slopeBelow: 1,
		slopeAbove: 0.5,
		clampTime: 0.01,
		relaxTime: 0.01
	  );

	  //chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.1,slopeAbove:0.5,clampTime:0.01,relaxTime:0.01);
	  
	  4.do{
		chain = AllpassL.ar(chain, 0.1, 0.1.rand, (length+0.1)*2.0.rand);
	  };
	  Out.ar(out_bus, chain*0.5);
	}).send(server);
  
}


}