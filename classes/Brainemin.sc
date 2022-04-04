// brain controlled theremin
Brainemin : Object{
  var com_port, <>notes, synth_id, >theremin, osc_resp, 
  raw = 5,
  att = 3,
  med = 4,
  spec_off = 6,
  spec_bands = 7, 
  test_rout;
  
  *new{arg com_port, notes=[ 60, 61, 63, 64, 66, 67, 69, 70, 72, 73, 75, 76, 78, 79, 81, 82], synth_id=1;
	^super.newCopyArgs(com_port, notes, synth_id).prInit;
  }
  
  prInit{
	this.sendSynthDefs;
  }

  run{
	("Brainemin running on port "++com_port).postln;
	("Synth is theremin"++(synth_id)).postln;
	theremin = Synth("theremin"++(synth_id));
	osc_resp = OSCresponderNode(nil, ("/brainCOM"++com_port), {arg time, responder, msg;
	  msg.postln;
	  msg[att].postln;
	  // send meditation and attention to the two theremins
	  if (msg[att] == 0, {
		//theremin.set(\pitch, msg[spec_off+2]);
		theremin.set(\freq, notes[msg[spec_off+2]*notes.size]);
		theremin.set(\mod, notes[msg[spec_off+3]]);
		//theremin.set(\pitch2, msg[spec_off+3]);
	  }, 
		{
		  theremin.set(\freq, notes[msg[att]*notes.size / 100]);
		  theremin.set(\mod, notes[msg[med]*notes.size / 100]);
		  //theremin.set(\pitch, msg[att]);
		  //theremin.set(\pitch2, msg[med]);
		}
	  );
	}).add;
  }
  
  test{
	test_rout = 
	{
	  inf.do{
		theremin.set(\freq, notes[rrand(0, notes.size-1)]);
		rrand(1.5, 3.5).wait;
	  };
	}.fork;
  }

  free{
	theremin.free;
	osc_resp.remove;
	test_rout.stop;
  }
  
  sendSynthDefs{
	var server;
	server = Server.local;
	
	SynthDef("theremin3", {arg freq=60, mod=0.5;
	  var c,vary,rf;
	  //c = Impulse.ar(1);
	  freq = Lag.kr(freq.midicps, LFDNoise1.kr(0.1).range(2, 4.0));
	  rf = (freq*0.25).reciprocal;
	  vary = rf / 100 * mod;
	  c = WhiteNoise.ar(rf * 0.02);
	  4.do{
		c = c + CombC.ar(c, 0.5, SinOsc.kr(0.1).range(vary, vary * 0.1) + rf, 0.9);
	  };
	  4.do{
		c = AllpassL.ar(c, 0.05, Rand(0.001, 0.05), 5.0);
	  };
	  Out.ar([0,1], c * 0.01);
	}).send(server).writeDefFile;

	SynthDef("theremin2", {arg freq=60, mod = 0.5, vibrato;
	  var c, lfo;
	  freq = Lag.kr(freq.midicps, LFDNoise1.kr(0.1).range(1, 3.0));
	  lfo = SinOsc.kr(vibrato, mul:freq / 20);
	  //freq = [freq + lfo, freq2 + lfo];
	  freq = [freq + lfo];
	  //c = SinOsc.ar(freq, mul:0.5) + Formant.ar(freq, 500, 200, 0.125) * 0.25;
	  c = MoogFF.ar(SinOsc.ar(freq*0.125, mul:0.2) + Saw.ar([freq * 0.125, freq*0.124], mul:0.5).mean, freq * 0.5 + SinOsc.kr(0.1).range(freq * 0.5, 5*freq), (SinOsc.kr(5).range(2.5, 3.4)));
	  //4.do{
	  //c = AllpassL.ar(c, 0.05, Rand(0.001, 0.05), Rand(4.0, 6.0));
	  //  };
	  Out.ar([0, 1], c * 0.5);
	}).send(server).writeDefFile;

	SynthDef("theremin1", {arg freq=60, vibrato;
	  var c, lfo;
	  freq = Lag.kr(freq.midicps, LFDNoise1.kr(0.1).range(0.5, 3.0));
	  lfo = SinOsc.kr(vibrato, mul:freq / 20);
	  //freq = [freq + lfo, freq2 + lfo];
	  freq = [freq + lfo];
	  c = SinOsc.ar(freq, mul:0.5) + Formant.ar(freq, 500, 200, 0.125) * 0.25;
	  4.do{
		c = AllpassL.ar(c, 0.05, Rand(0.001, 0.05), 5.0);
	  };
	  Out.ar([0, 1], c * 0.5);
	}).send(server).writeDefFile;
  } 
}