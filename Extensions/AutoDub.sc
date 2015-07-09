// this class is an auto dub fx generator// instantiate and call run


AutoDub : Object {

  var 
  // config
  in_bus, out_bus, >interval=2, 
  // synths
  fx_synths, 
  // misc
  fx_states, 
  // routines
  fxMixRoutine, 
  fxSettingRoutine;

  *new{ arg in_bus=1, out_bus=4;
	^super.newCopyArgs(in_bus, out_bus).prInit;
  }

  prInit{
	var names;
	names = this.getSynthDefNames;
	this.sendSynthDefs;
	// prepare the synth array
	fx_synths = Array.newClear(names.size);
	// env states for all fx synths
	fx_states = Array.series(names.size, 0, 0);
	// first is on!
	fx_states.put(0, 1);

	fxMixRoutine = Routine{
	  inf.do{
		this.open;
		interval.wait;
		this.close;
		interval.wait;
	  };
	};
  }
  
  open{
	Routine.new{
	  // periodically change the mix
	  // - shift the states along one
	  fx_states = fx_states.rotate(-1);
	  fx_states.postln;
	  
	  // - change settings on the synth thats
	  // about to come on
	  fx_states.size.do{arg i;
		fx_synths[i].set(\ctl1, 127.0.rand);
		fx_synths[i].set(\ctl2, 127.0.rand);
		fx_synths[i].set(\ctl3, 127.0.rand);
		fx_synths[i].set(\ctl4, 127.0.rand);
	  };
	  
	  // - swicth one synth on, the rest off
	  fx_states.size.do{arg i;
		fx_synths[i].set(\on, fx_states.at(i));
	  };
	}.play;
  }
  
  // shuts the effect down
  close{
	// now switch them all off
	fx_states.size.do{arg i;
	  fx_synths[i].set(\on, 0);
	};
  }
  
  run {
	var names;
	names = this.getSynthDefNames;
	names.size.do{arg i;
	  fx_synths[i] = Synth(names[i], [\out_bus, out_bus]);
	};
	this.play;
  }

  play{
	fxMixRoutine.reset;
	fxMixRoutine.play;
  }

  stop{
	fxMixRoutine.stop;
  }
  
  free{
	this.stop;
	fx_synths.size.do{arg i;
	  fx_synths[i].free;
	}
  }

  getSynthDefNames{
	var names;
	names = ["AutoDub_verb_amp", "AutoDub_delay_amp", "AutoDub_filt_amp", "AutoDub_pitcher"];
	//names = ["pulse", "sine"];
	^names;
  }
  
  sendSynthDefs{
	var server = Server.local;
	
	SynthDef("AutoDub_pitcher", {arg in_bus=1, out_bus=0, ctl1, ctl2, ctl3, ctl4, on=0;
	  var chain, amp, offset, midiTo1, decayTime, env;
	  midiTo1 = 1/127;
	  ctl1 = Lag.kr(ctl1, 1.0);
	  ctl2 = Lag.kr(ctl2, 1.0);
	  ctl3 = Lag.kr(ctl3, 1.0);
	  ctl4 = Lag.kr(ctl4, 1.0);
	  // 0 -10
	  decayTime = ctl2*midiTo1*10;
	  // 0-4
	  offset = ctl1*midiTo1*4;
	  chain = In.ar(in_bus);
	  amp = Lag.kr(Amplitude.kr(chain, 0.01, 0.01), 1.0);
	  chain = PitchShift.ar(chain, 0.02, offset*amp*1000, timeDispersion:0.2*amp);
	  // envelopes
	  env = EnvGen.kr(Env.asr(0, 1, 0, 1), gate:on);
	  chain = chain*env;
	  chain = [chain, chain];
	  4.do{
		chain = AllpassL.ar(chain, 0.01, 0.01.rand, decayTime+0.1);
	  };
	  chain = MoogFF.ar(chain, (amp*5000)+200, 0.9);
	  Out.ar(out_bus, chain*0.2);
	}).send(server);
	

	// big reverb
	SynthDef("AutoDub_verb_amp", {arg in_bus=1, out_bus=0, ctl1=127, ctl2=64, ctl3=64, ctl4=64, on=0;
	  var chain, midiTo1, delTime, decayTime, env, env2;
	  ctl1 = Lag.kr(ctl1, 0.2);
	  ctl2 = Lag.kr(ctl2, 0.2);
	  ctl3 = Lag.kr(ctl3, 0.2);
	  ctl4 = Lag.kr(ctl4, 0.2);
	  chain = In.ar(in_bus);
	  midiTo1 = 1/127;
	  // 0-1
	  delTime = ctl1*midiTo1;
	  // 0-10
	  decayTime = ctl2*midiTo1*10;
	  // envelopes
	  env = EnvGen.kr(Env.asr(0, 1, 0, 1), gate:on);
	  //env = 1;
	  env2 = EnvGen.kr(Env.perc(0.001, 0.2, 1), gate:(Amplitude.kr(chain, 0.001, 0.1)>0.1));
	  chain = chain * env * env2;
	  
	  chain = [chain, chain];
	  4.do{
		chain = AllpassL.ar(chain, 0.11, ((0.1.rand)*delTime)+0.01, decayTime);
	  };
	  Out.ar(out_bus, chain* 0.2);
	}).send(server);
	
	// big reverb
	SynthDef("AutoDub_delay_amp", {arg in_bus=1, out_bus=0, ctl1=127, ctl2=64, ctl3=64, ctl4=64, on=0;
	  var chain, midiTo1, delTime, decayTime, env, env2, panSpeed;
	  ctl1 = Lag.kr(ctl1, 0.2);
	  ctl2 = Lag.kr(ctl2, 0.2);
	  ctl3 = Lag.kr(ctl3, 0.2);
	  ctl4 = Lag.kr(ctl4, 0.2);
	  chain = In.ar(in_bus);
	  midiTo1 = 1/127;
	  // 0.25-1
	  delTime = (ctl1*midiTo1*0.75)+0.25;
	  // 0-10
	  decayTime = ctl2*midiTo1*10;
	  // 0-10
	  panSpeed = ctl3*midiTo1*20;
	  // envelopes
	  env = EnvGen.kr(Env.asr(0, 1, 0, 1), gate:on);
	  //env = 1;
	  env2 = EnvGen.kr(Env.perc(0.001, 0.2, 1), gate:(Amplitude.kr(chain, 0.001, 0.1)>0.1));
	  chain = chain * env * env2;
	  //4.do{
	  chain = CombL.ar(chain, 0.41, (0.4*delTime)+0.01, decayTime);
	  // now some panning action
	  chain = Pan2.ar(chain, LFDNoise1.kr(panSpeed).range(-1, 1));
	  //};
	  Out.ar(out_bus, chain*0.2);
	}).send(server);
	
	// big reverb
	SynthDef("AutoDub_filt_amp", {arg in_bus=1, out_bus=0, ctl1=127, ctl2=64, ctl3=64, ctl4=64, on=0;
	  var chain, midiTo1, delTime, decayTime,maxFreq, env, env2, filtEnv, amp, openTime;
	  ctl1 = Lag.kr(ctl1, 0.2);
	  ctl2 = Lag.kr(ctl2, 0.2);
	  ctl3 = Lag.kr(ctl3, 0.2);
	  ctl4 = Lag.kr(ctl4, 0.2);
	  chain = In.ar(in_bus);
	  midiTo1 = 1/127;
	  // 0-2
	  openTime = ctl1*midiTo1;
	  // 0-10
	  maxFreq = (ctl4.midicps*0.25)+1500;
	  // 0-1
	  delTime = ctl2*midiTo1;
	  // 0-10
	  decayTime = ctl3*midiTo1*4;
	  
	  // envelopes
	  env = EnvGen.kr(Env.asr(0, 1, 0, 1), gate:on);
	  //env = 1;
	  amp = Amplitude.kr(chain, 0.001, 0.1);
	  
	  env2 = EnvGen.kr(Env.perc(0.001, 0.2, 1), gate:(amp>0.2));
	  
	  filtEnv = EnvGen.kr(Env.perc(openTime, openTime, maxFreq), gate:(amp>0.3));
	  chain = chain * env;
	  // swept filter
	  chain = MoogFF.ar(chain, filtEnv, 0.6);
	  chain = [chain, chain];
	  4.do{
		chain = AllpassL.ar(chain, 0.11, ((0.1.rand)*delTime)+0.01, decayTime);
	  };
	  //chain = CombL.ar(chain, 0.41, (0.4*delTime)+0.01, decayTime);
	  Out.ar(out_bus, chain*0.2);
	}).send(server);

	
  }
}