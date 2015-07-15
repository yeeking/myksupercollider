// re-write of the SC side of the FPh, with no sequence memory this time
// sends the following messages to the 
// gui:
// tick
// reset
// receives the following:
// play_step

FinnPhonium : Object {
  var channels, steps, bpm, 
  play_rout, play_step_listener, reset_listener, <gui_speaker, >step_funcs, 
  // stores references to a synth for each channel
  channel_synths, 
  // stores names of synthdefs for each channel
  <>channel_synthdefs, 
  // different channel modes
  // play a note, value defines note
  <cm_synth_note=0, 
  // set a param on a note, value defines the param
  <cm_synth_arg1=1, 
  // ditto
  <cm_synth_arg2=2, 
  // set the length of a note (combine with note), value defines the length
  <cm_synth_len=3, 
  // play a synth from a bank of different synths, value defines which synth
  <cm_synth_bank=4, 
  // play a sample, value defines which one and will be quentized in the range 0-number of available samples-1
  <cm_sampler_play=5, 
  // tells the sampler to sample a new bank of samples in. value defines threshold
  <cm_sampler_sample=6, 
  // set a param on the sampler
  <cm_sampler_arg1=7,
  // set another param on the sampler
  <cm_sampler_arg2=8,
  // setting different effects parameters
  <cm_fx_arg1=9, 
  <cm_fx_arg2=10, 
  <cm_fx_arg3=11, 
  <cm_fx_arg4=12, 
  synth_bank, 
  live_sampler, 
  sample_count = 32, 
  >gate_time = 0.01;
  
  
  *new {arg channels=1, steps=20, bpm=120;
	^super.newCopyArgs(channels, steps, bpm).prInit;
  }

  prInit {
	this.sendSynthDefs;
	// create a connection to the gui
	gui_speaker = NetAddr("localhost", 57130); 
	this.reset(channels, steps, true);
	// selection of snthdefs, initially for analog percussion synths
	synth_bank = ["FP_perc1", "FP_perc2"];
	
	//live_sampler = LiveSampler.new(buf_count:sample_count);

	// create a responder for update messages from the gui
	play_step_listener = OSCresponderNode(nil, '/play_step', {arg time, responder, msg;
	  //[time, msg].postln;
	  this.playStep(msg);
	}).add;
	
	reset_listener =  OSCresponderNode(nil, '/reset', {arg time, responder, msg;
	  [time, msg].postln;
	  "reset received".postln;
	  this.reset(msg[1], msg[2], false);
	}).add;
  }
  reset{arg chs, sts, tell_gui = true;
	var new_step_funcs;
	("FinnPhonium resetting to chans: "++chs++" steps: "++sts).postln;

	// get rid of any channel synths
	
	// remember the new settings
	channels = chs;
	steps = sts;
	if (tell_gui == true, {	gui_speaker.sendMsg("/reset_memory", 0, chs, sts);});
	//gui_speaker.sendBundle(0.1, "/reset_memory", 0, chs, sts);
	// a blank array
	new_step_funcs = Array.fill(channels, {arg chan; Array.fill(steps, {arg step; 
	  {arg val;
		//("chan "++chan++" step "++step++" value "++val).postln;
	  }
	})});
	// try to copy over the old funcs
	new_step_funcs.size.do {arg chan;
	  // 
	  if (chan < step_funcs.size, {
		// we have a channel to copy
		new_step_funcs[chan].size.do{arg step;
		  if (step < step_funcs[chan].size, {
			// we have a step to copy:
			new_step_funcs[chan][step] = step_funcs[chan][step];
		  }, 
			// no step to copy - duplicate the last step
			new_step_funcs[chan][step] = step_funcs[chan][step_funcs[chan].size-1];
		  );
		};
	  } 
		// no channel to copy - do nothing
	  );
	};
	step_funcs = new_step_funcs;
	play_rout = Routine.new{
	  inf.do{
		this.tick;
		(60 / bpm).wait;
	  };
	};
	// free any channel synths
	channel_synths.do{arg synth;
	  if (synth != nil, {synth.free});
	};
	channel_synths = Array.newClear(channels);
	channel_synthdefs = Array.fill(channels, {arg i; "FP_synth"});
	// now create the effects synths...

  }
  
  tick{
	gui_speaker.sendMsg("/tick", 1);
  }

  setBPM{arg bpm;
	gui_speaker.sendMsg("/set_bpm", 0, bpm);
  }
  
  playStep{arg data;
	var step, val, server;
	server = Server.local;
	//data.postln;
	//step = data[1];
	// trigger the functions assigned to the steps
	server.makeBundle(0.01,{ 
	  channels.do{arg c;
		step = data[(c * 2)+1];
		val = data[(c * 2) + 2];
		if (val>0, {
		  step_funcs[c][step].value(val);
		});
	  };
	});
  }
  run{
	play_rout.play;
	live_sampler.run;
	// now create the effects synths...
  }
  free {
	play_rout.stop;
	play_step_listener.remove;
	reset_listener.remove;
	live_sampler.free;
	channel_synths.do{arg synth; synth.free};
  }
  
  // assign a function to a step

  setStepFunc{arg channel, func, step = nil;
	if (channel < channels, 
	  {
		if (step == nil, {
		  // assign to all steps
		  steps.do{arg i;
			step_funcs[channel][i] = func;
		  };
		},{
		  // else
		  if (step < steps, {
			// assign to one step
			step_funcs[channel][step] = func;
		  });
		} 
		);
	  }
	);
  }

  // print out info about the channel mode info
  getChannelModeInfo{
	//   // play a note, value defines note
	//   <cm_synth_note=0, 
	//   // set a param on a note, value defines the param
	//   <cm_synth_arg1=1, 
	//   // ditto
	//   <cm_synth_arg2=2, 
	//   // set the length of a note (combine with note), value defines the length
	//   <cm_synth_len=3, 
	//   // play a synth from a bank of different synths, value defines which synth
	//   <cm_synth_bank=4, 
	//   // play a sample, value defines which one and will be quentized in the range 0-number of available samples-1
	//   <cm_sampler_play=5, 
	//   // tells the sampler to sample a new bank of samples in. value defines threshold
	//   <cm_sampler_sample=6, 
	//   // set a param on the sampler
	//   <cm_sampler_arg1=7,
	//   // set another param on the sampler
	//   <cm_sampler_arg2=8,
	//   // setting different effects parameters
	//   <cm_fx_arg1=9, 
	//   <cm_fx_arg2=10, 
	//   <cm_fx_arg3=11, 
	//   <cm_fx_arg4=12, 
  }
  
  // call this to set up a preset function for steps in a channel
  // mode should be one of the variables in CAPS above...
  setChannelMode{arg channel, mode, target_channel=nil, synth = "FP_single_synth";
	var func, server;
	server = Server.local;
	func = {arg val; val.postln};
	switch (mode, 
	  cm_synth_note, {
		// play a synth
		func = {arg val; 
		  if (channel_synths[channel] == nil, {
			// no synth: create one and trigger it
			channel_synths[channel] = Synth(synth, [\freq, (val * 127).round.midicps, \t_trig, 1]);
		  },{
			channel_synths[channel].set(\t_trig, 1);
			channel_synths[channel].set(\freq, (val * 127).round.midicps);
		  })
		};
	  },  
	  cm_synth_arg1, {
		// set a param on a synth
		func = {arg val;
		  if (channel_synths[target_channel] != nil, {
			channel_synths[target_channel].set(\arg1, val);
		  })
		};
	  },  
	  cm_synth_arg2, {
		// set a param on a synth
		func = {arg val;
		  if (channel_synths[target_channel] != nil, {
			channel_synths[target_channel].set(\arg2, val);
		  })
		};
	  },  
	  cm_synth_len, {
		// set envelope length (well, if that's how the synthef works anyway)
	  	func = {arg val;
		  if (channel_synths[target_channel] != nil, {
			channel_synths[target_channel].set(\len, val);
		  })
		};
	  },  
	  cm_synth_bank, {
		// play a synth from a bank of synths
		func = {arg val;
		  Synth(synth_bank[val * synth_bank.size]);
		};
	  },  
	  cm_sampler_play, {
		// play the sent sample from a bank
		func = {arg val;
		  live_sampler.playSample((val * sample_count-1).round );
		};
	  },  
	  cm_sampler_sample, {
		// collect a new bank of samples
		func = {arg val;
		  live_sampler.sample(val / 10);
		};
	  },  
	  cm_sampler_arg1,{
		// set a param on the sample playback synths
	  },  
	  cm_sampler_arg2,{
		// set a param on the sample playback synths
	  },  
	  cm_fx_arg1, {
		// set a param on the fx unit
	  },  
	  cm_fx_arg2, {
		
	  },  
	  cm_fx_arg3, {
		
	  },  
	  cm_fx_arg4, {
		
	  }
	);
	this.setStepFunc(channel, func);
  }
  sendSynthDefs{
	var server;
	server = Server.local;	
	SynthDef("FP_single_synth", {arg freq, t_trig =0, len=0.5, arg1 = 0.5, arg2 = 0.5;
	  var c, e, fe, coff, gain, envpeak;
	  envpeak = Latch.kr(t_trig, t_trig);
	  coff = ((arg1 * 127).round.midicps) + freq;
	  gain = (arg2 * 3.95) + 0.05;
	  e = EnvGen.kr(Env.new([0, envpeak, 0.5, 0], [len * 0.1, len * 0.25, len * 0.25]), gate:t_trig);
	  fe = EnvGen.kr(Env.new([0, envpeak, 0.5, 0], [len * 0.1, len * 0.25, len * 0.25]), gate:t_trig);
	  c = MoogFF.ar((SinOsc.ar(freq:freq/2, mul:e) + Saw.ar(freq, mul:e)), fe * coff, gain);
	  Out.ar([0, 1], c * 0.25);
	}).send(server);	

	// dirty ring mod wave shaper - i do know how it works but that
	// doesn't really matter since it sounds like a motherfucker
	SynthDef("FP_ws", {arg freq, t_trig=0, len=0.5, arg1 = 0.5, arg2 = 0.5;
	  var c, trans, buff, e, envpeak;
	  envpeak = Latch.kr(t_trig, t_trig);
	  e = EnvGen.kr(Env.new([0, envpeak, 0.5, 0], [len * 0.1, len * 0.25, len * 0.25]), gate:t_trig);
	  buff = LocalBuf(64, 1);
	  trans = Saw.ar(freq + LFDNoise1.kr(0.4).range(1, freq/20), mul:arg1);
	  //trans = trans + Pulse.ar(freq + LFDNoise1.kr(0.4).range(1, freq/20), mul:arg2);
	  // write transfer fnuc in to b2
	  RecordBuf.ar(trans, buff, loop:1);
	  c = Shaper.ar(buff,SinOsc.ar(freq), arg2 * 10);  
	  Out.ar([0, 1], c * e * 0.1);
	}).send(server);
	

	// t_trig_version
	// based on a patch craeted by Nick Collins I think.1
	SynthDef("FP_steel", {arg freq=220, len=0.5, t_trig=0, arg1=0.25, arg2 = 0.5;  
	  var envpeak, ampmodenv;
	  var midinote;
	  var thisnote;
	  var mididiff;
	  var ratio;
	  var output, env;
	  var modes,modefreqs,modeamps;
	  var mu,t,e,s,k,f1,l,c,a,beta,beta2,density;
	  var decaytimefunc;
	  var material;
	  material= \steel; // \steel
	  freq = freq /4;
	  //don't know values of E and mu for a nylon/gut string
	  //so let's try steel
	  //radius 1 cm
	  a=0.01;
	  s=pi*a*a;
	  //radius of gyration
	  k=a*0.5;
	  if (material ==\nylon,{
		e=2e+7; 
		density=2000; 
	  },{//steel
		e= 2e+11; // 2e+7; //2e+11 steel;
		//density p= 7800 kg m-3 
		//linear density kg m = p*S
		density=7800; 
	  });
	  //density = density * MouseX.kr(0, 1);
	  mu=density*s;
	  t=100000;// * MouseX.kr(0, 1);
	  c= (t/mu).sqrt;	//speed of sound on wave
	  l=1.8;	//0.3
	  f1= c/(2*l);
	  beta= (a*a/l)*((pi*e/t).sqrt);
	  beta2=beta*beta;
	  modes=20;
	  modefreqs= Array.fill(modes,{arg i; 
		var n,fr;
		n=i+1;
		fr=n*f1*(1+beta+beta2+(n*n*pi*pi*beta2*0.125));
		if(fr>21000, {fr=21000; fr.postln}); //no aliasing
		fr
	  });
	  decaytimefunc= {arg freq2;
		var t1,t2,t3;
		var m,calc,e1dive2;
		//VS p 50 2.13.1 air damping
		m=(a*0.5)*((2*pi*freq2/(1.5e-5)).sqrt);
		calc= 2*m*m/((2*(2.sqrt)*m)+1);
		t1= (density/(2*pi*1.2*freq2))*calc;
		e1dive2=0.01; //a guess!
		t2= e1dive2/(pi*freq2);
		//leave G as 1
		t3= 1.0/(8*mu*l*freq2*freq2*1);
		1/((1/t1)+(1/t2)+(1/t3))
	  };
	  modeamps=Array.fill(modes,{arg i; decaytimefunc.value(modefreqs.at(i))});
	  //modefreqs.postln;
	  //modeamps.postln;
	  //{
	  //EnvGen.ar(Env.new([0.001,1.0,0.9,0.001],[0.001,0.01,0.3],'exponential'),WhiteNoise.ar)
	  //could slightly vary amps and phases with each strike?
	  envpeak = Latch.kr(t_trig, t_trig);
	  env = EnvGen.kr(Env.new([0,envpeak,1,0],[0,len,len/2]),gate:t_trig);
	  ampmodenv = EnvGen.kr(Env.new([0,envpeak,1,0],[len/3,len/3,len/3]),gate:t_trig);
	  //ampmodenv = EnvGen.kr(Env.new([0,envpeak,0],[len/2,len/2]),gate:t_trig);
	  //env = 1;
	  output=
	  //slight initial shape favouring lower harmonics- 1.0*((modes-i)/modes)
	  // def freq is midi note 37, so calc the number of semitones from default to 
	  // desired and convert it to a ratio
	  midinote = freq.cpsmidi;
	  thisnote = modefreqs.at(0).cpsmidi;
	  mididiff = (midinote - thisnote);//.abs;
	  // to get from thisnote to midinote
	  ratio = 2.pow(mididiff/12);
	  //ratio.poll;
	  output = Array.fill(modes,{arg i; 
		var e, mod;
		mod = arg1 * SinOsc.kr(LFDNoise1.kr(0.5).range(1.0, arg2*5)) * ampmodenv;
		//e = EnvGen.kr(Env.new([0,1,modeamps.at(i),0],[0,len,0]),gate:gate);
		e = EnvGen.kr(Env.new([0,envpeak,modeamps.at(i),0],[0,len * LFDNoise0.kr(1/len).range(0.5, 1),0], 'exponential'),gate:t_trig);
		SinOsc.ar(modefreqs.at(i)*ratio,1.0/modes, mul:e + mod);
	  }).mean;
	  //	Pan2.ar(output,0).mean;
	  output = output * env;
	  Out.ar([0, 1], output);
	  //Out.ar(1, SinOsc.ar(freq, mul:env));
	}).send(server);

	SynthDef("FP_test_synth", {arg freq;
	  Out.ar(0, Saw.ar(freq, mul:Line.kr(1, 0, 0.5, doneAction:2)));
	}).send(server);
	SynthDef("FP_perc1", {arg freq;
	  Out.ar(0, WhiteNoise.ar(mul:Line.kr(1, 0, 0.5, doneAction:2)));
	}).send(server);
	SynthDef("FP_perc2", {arg freq;
	  Out.ar(0, SinOsc.ar(freq:Line.kr(400, 50, 0.1), mul:Line.kr(1, 0, 0.5, doneAction:2)));
	}).send(server);
	
  }
}


// simple destination that uses the sequence data to play a note on a synth
FFDestinationPlayNote {
  
  setValue{arg val;
	Synth("FF_test_synth", [\freq, (val * 127).midicps]);
  }

}

