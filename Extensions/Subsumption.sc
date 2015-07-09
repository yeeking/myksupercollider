
Subsumption : Object {
  var 
  // creationArgs
  >out_buses, in_bus, 
  // behavioural parameters
  // change the synth def
  synth_cycle_speed, 
  // change the octave range
  octave_cycle_speed, 
  // change the key
  key_cycle_speed, 
  // read notes from audio in
  note_read_speed, 
  // range for intervals
  min_interval, 
  max_interval,
  // how many notes to play or similar
  synth_complexity, 
  max_synth_complexity, 
  // power available to different routines
  short_power, 
  long_power, 
  // the all important...
  interval,
  // analysis
  currentPitch, 
  currentTempo, 
  pitchReader, 
  beatTrack, 
  // fx
  fx_buff, 
  fx_synth, 
  fx_synth2, 
  // behaviours
  scaleGeneratorRout, 
  intervalShortenerRout, 
  intervalLengthenerRout, 
  keyCyclerRout, 
  synthPlayerRout, 
  octaveCyclerRout, 
  synthCyclerRout,
  synthComplexityRout, 
  watchDogRout, 
  allRouts, 
  // data
  >keys, 
  >octaves, 
  >synthdefs;

  *new{arg out_buses = [2, 3], in_bus=1;
	^super.newCopyArgs(out_buses, in_bus).prInit;
  }
  
  prInit{
	var server;
	server = Server.local;
	this.sendSynthDefs;
	// set some default values
	this.reset;
	fx_buff = Buffer.alloc(server, 2048, 1);
	
	intervalShortenerRout = Routine.new{
	  var rand;
	  inf.do{
		rand = 1.0.rand;
		// if short power is high enough, shorten the interval
		if (short_power > rand, {

		  interval = interval / 1.5;
		  if (interval < min_interval, {interval = max_interval});
		
		  // shortener has had a go... increase change of long next time
		  short_power = short_power - (0.2.rand);
		  long_power = long_power + (0.2.rand);
		  if (short_power <= 0, {short_power = 0;});
		  if (long_power >= 1, {long_power = 1;});
		  //("shorter! interval:"++interval++" sht power: "++short_power++" lng power "++long_power).postln
		});
		this.checkInterval.wait;
	  };
	};
	
	intervalLengthenerRout = Routine.new{
	  var rand;
	  inf.do{
		rand = 1.0.rand;
		// if long poewr is high enough, lengthen the interval
		if (long_power > rand, {
		  interval = interval * 1.5;
		  if (interval > max_interval, {interval = min_interval});
		  // lengthener has had a go - reduce its power
		  short_power = short_power + (0.2.rand);
		  long_power = long_power - (0.4.rand);
		  if (short_power >= 1, {short_power = 1;});
		  if (long_power <= 0, {long_power = 0;});
		  //("longer! interval:"++interval++" sht power: "++short_power++" lng power "++long_power).postln
		});
		this.checkInterval.wait;
	  };
	};
	
	keyCyclerRout = Routine.new{
	  inf.do{
		keys[0].postln;
		keys = keys.rotate(-1);		
		(interval * key_cycle_speed).wait
	  };
	};
 
	octaveCyclerRout = Routine.new{
	  inf.do{
		octaves = octaves.rotate(-1);
		(interval * octave_cycle_speed).wait
	  };
	};

	synthCyclerRout = Routine.new{
	  inf.do{
		synthdefs = synthdefs.rotate(5.rand);
		synth_cycle_speed = [1, 5, 10].choose;
		(interval * synth_cycle_speed).wait
	  };
	};
	
	synthPlayerRout = Routine.new{
	  inf.do{
		this.playSynth;
		this.checkInterval.wait;
	  };
	};
	
	synthComplexityRout = Routine.new{
	  inf.do{
		var int_rat;
		int_rat = interval/max_interval;
		synth_complexity = synth_complexity+1;
		synth_complexity = (int_rat * synth_complexity * max_interval)+1;
		if (synth_complexity > max_synth_complexity, {synth_complexity = 1});
		//format("synth complexity is % ", synth_complexity).postln;
		this.checkInterval.wait;
	  }
	};
	
	watchDogRout = Routine.new{
	  inf.do{
		short_power = 1.0.rand;
		long_power = 1.0.rand;
		interval = (max_interval.rand) + 0.1;
		10.wait;
	  };
	};
	

  }

  checkInterval{
	if (interval == 0, {^(10.rand)}, {^interval});
  }
  
  // this gets called by the synth playing routine
  playSynth{
	var freq, dur, freqs;
	// pick 4 notes from the scale
	freqs = Array.fill(3, {keys[0].choose;});

	{synth_complexity.do{
	  //freq = (keys[0].choose) * (octaves[0].choose);
	  freq = freqs.choose *  (octaves[0].choose);
	  dur = ((interval * 2).rand) + 0.01;
	  Synth(synthdefs[0], [\freq, freq, \dur, dur, \out_bus, out_buses.choose]);
	  (interval * [0, 0.5, 1, 0.125].choose).wait;
	};}.fork;
  }
  
  reset{
	interval = 1;
	synth_cycle_speed = 2;
	octave_cycle_speed = 2;
	key_cycle_speed = 30;
	note_read_speed = 1;
	min_interval = 0.1;
	max_interval = 10;
	synth_complexity = 1;
	max_synth_complexity = 25;
	short_power = 0.5;
	long_power = 0.5;
	keys = [
	  //MiscFuncs.getScale('cs', 'nat_minor'), 
	  //MiscFuncs.getScale('gs', 'nat_major'), 
	  MiscFuncs.getScale('cs', 'mel_minor'), 
	  MiscFuncs.getScale('b', 'blues'), 
	  MiscFuncs.getScale('a', 'romanian_minor')
	  //MiscFuncs.getScale('a', 'random')
	];
	octaves = [
	  [1], 
	  [0.5, 1, 2], 
	  [0.25, 0.5, 1], 
	  [0.25, 0.5, 1, 2, 4, 8], 
	  [0.125, 1, 8], 
	  [0.125, 0.25], 
	];
	synthdefs = [
	  "ss_sine", 
	  //"ss_saw", 
	  //	  "ss_pulse", 
	  "ss_dynklang", 
	  "ss_dynklang_glass", 
	  //	  "ss_form"
	];
	
  }
  
  run{
	// store the routs into an array for easy stop start reset
	allRouts = [
	  intervalShortenerRout, 
	  intervalLengthenerRout,
	  keyCyclerRout, 
	  octaveCyclerRout, 
	  synthCyclerRout, 
	  synthPlayerRout,
	  watchDogRout, 
	  synthComplexityRout 
	];

	allRouts.do{arg i;i.reset;i.play};

	this.setupFX;

  }
  
  stop{
	allRouts.do{arg i;i.stop;};
  }

  free{
	this.stop;
	fx_synth.free;
	fx_synth2.free;
	fx_buff.free;
  }

  setupFX{
	fx_synth = Synth("ss_fx_fft", [\buffer, fx_buff, \in1, out_buses[0], \in2, out_buses[1], \out_bus, 4]);
	fx_synth2 = Synth("ss_fx_rev", [\in1, out_buses[0], \in2, out_buses[1], \out_bus, 4]);
  }
  
  sendSynthDefs{
	var server;
	server = Server.local;
	// some simple tone players
	// some complex tone players
	// record live input
	// process live input
	
	SynthDef("ss_sine", {arg out_bus, freq, dur;
	  var chain, env, lfo;
	  lfo = SinOsc.ar(10 * dur, mul:Line.kr(0, Rand(dur, 100), dur * 0.75));
	  //env = EnvGen.kr(Env.perc(dur * 0.25, dur), doneAction:2, levelScale:0.02);
	  env = EnvGen.kr(Env.new([0, 1, 0.5, 0.75, 0], [0.001, 0.01, Rand(0.2, 0.5), dur], -2), doneAction:2, levelScale:0.025);
	  chain = SinOsc.ar(freq+lfo, mul:env*LFDNoise1.kr(0.5), phase:Rand(0, 1));
	  Out.ar(out_bus, chain);
	}).send(server);

	SynthDef("ss_saw", {arg out_bus, freq, dur;
	  var chain, env;
	  env = EnvGen.kr(Env.perc(dur * 0.25, dur), doneAction:2, levelScale:0.01);
	  chain = Saw.ar(freq, mul:env);
	  chain = RLPF.ar(chain, Line.kr(Rand(Rand(0, freq), freq*2), Rand(freq, freq*10), dur * 0.25), Rand(0.2, 0.5));
	  Out.ar(out_bus, chain);
	}).send(server);

	SynthDef("ss_pulse", {arg out_bus, freq, dur;
	  var chain, env, w_lfo, mod;
	  mod = SinOsc.ar(freq*(Rand(1, 3)), mul:Line.kr(0, Rand(10, 20), Rand(5, 10)));
	  w_lfo = SinOsc.ar(Rand(0.1, 10), mul:Line.kr(0.001, Rand(0.2, 1), dur*0.5));
	  //env = EnvGen.kr(Env.perc(dur * 0.01, dur), doneAction:2, levelScale:0.02);
	  env = EnvGen.kr(Env.new([0, 0.5, 1, 0.5, 0], [Rand(0, 1), dur*0.25, dur*0.25, dur*0.5], 4), doneAction:2, levelScale:0.025);
	  chain = Pulse.ar(freq+mod, mul:env, width:w_lfo);
	  chain = chain + Saw.ar(freq * 0.25, mul:env);
	  chain = LPF.ar(chain, LFDNoise1.kr(0.2).range(50, 7000));
	  chain = HPF.ar(chain, LFDNoise1.kr(0.2).range(50, 2000));

	  Out.ar(out_bus, chain);
	}).send(server);

	
	SynthDef("ss_dynklang", {arg out_bus, freq, dur;
	  var chain, env, parts, tones, mults, noise, rings, env2;
	  tones = 5;
	  parts = Array.fill(tones, {arg i;1 + (Rand(0.1, (i+0.0).rand))});
	  parts[0] = 1;
	  mults = Array.fill(tones, {Rand(0.1, 1)});
	  mults[0] = 1;
	  rings = Array.fill(tones, {Rand(0.5, dur*2)});
	  rings[0] = 5;
	  noise = PinkNoise.ar(XLine.kr(0.2, 0.001, 0.2));
	  chain = Klank.ar(`[parts, mults, rings], noise, freqscale:freq);
	  //env = EnvGen.kr(Env.perc(0.001, dur), doneAction:2, levelScale:0.01);
	  //env = EnvGen.kr(Env.new([0, 1, 0.25, 0], [0.001, 0.01, dur]), doneAction:2, levelScale:0.01);
	  env = EnvGen.kr(Env.new([0, 1, 0.5, 0.75, 0], [0.01, 0.01, Rand(0.2, 0.5), dur], -8), doneAction:2, levelScale:0.025);
	  Out.ar(out_bus, chain * env);
	}).send(server);

	
	SynthDef("ss_dynklang_glass", {arg out_bus, freq, dur;
	  var chain, env, parts, tones, mults, noise, rings;
	  tones = 5;
	  //parts = Array.fill(tones, {arg i;1 + ((i+0.0).rand)});
	  //parts = Array.fill(tones, {arg i;1 + (Rand(0.1, (i+0.0).rand))});
	  parts = Array.fill(tones, {Rand(1.0, 1.1)});
  parts[0] = 1;
	  mults = Array.fill(tones, {Rand(0.5, 1)});
	  mults[0] = 1;
	  rings = Array.fill(tones, {Rand(0.1, dur*2)});
	  rings[0] = dur * 2;
	  noise = ClipNoise.ar(XLine.kr(0.2, 0.001, 0.001));
	  chain = Klank.ar(`[parts, mults, rings], noise, freqscale:freq);
	  //env = EnvGen.kr(Env.perc(0.001, dur), doneAction:2, levelScale:0.01);
	  env = EnvGen.kr(Env.new([0, 1, 0.5, 0.75, 0], [0.001, 0.01, Rand(0.2, 0.5), dur], -2), doneAction:2, levelScale:0.025);
	  Out.ar(out_bus, chain * env);
	}).send(server);
	

	SynthDef("ss_form", {arg freq, out_bus, dur;
	  var chain, env, lfo;
	  lfo = SinOsc.ar(Rand(0.1, 4), mul:Line.kr(0, Rand(0, 5), dur*0.5));
	  //env = EnvGen.kr(Env.perc(0.001, dur), doneAction:2, levelScale:0.01);
	  env = EnvGen.kr(Env.new([0, 1, 0.25, 0.5, 0], [0.01, 0.01, Rand(0.2, 0.5), dur], -2), doneAction:2, levelScale:0.01);
	  chain = Formant.ar(freq+lfo, XLine.kr(Rand(100, 1000),Rand(100, 7000), dur*0.125), Rand(500, 500));
	  Out.ar(out_bus, chain*env);
	}).send(server);


	SynthDef("ss_fx_fft", {arg buffer, in1=0, in2=1, out_bus=2;
	  var chain, in, chaini, lfo, chainL, chainR, env;
	  in = In.ar([in1, in2]);
	  in = in.mean * 2;
	  //in = in * LFDNoise1.ar(0.1).range(0, 1);
	  chain = FFT(buffer, in);
	  lfo = LFDNoise1.kr(0.1).range(0, 2);
	  // filter quiet partials
	  chain = PV_MagAbove(chain, lfo); 
	  // scramble it a bit
	  //chain = PV_MagShift(chain, 1, SinOsc.ar(SinOsc.kr(0.01, mul:0.1), mul:10, add:12));
	  // smear to remove scrambling artefacts
	  chain = PV_MagSmear(chain, 5);
	  chaini = IFFT(chain);
	  //chaini = Normalizer.ar(chaini, 0.02, 0.02);
	  chaini = HPF.ar(chaini, 40);
	  chaini = chaini * (1+lfo) * 0.6;
	  chainL = chaini;
	  chainR = chaini;
	  2.do{
		chainL = AllpassL.ar(chainL, 0.05, 0.05.rand, 4.0);
	  };
	  2.do{
		chainR = AllpassL.ar(chainR, 0.05, 0.05.rand, 4.0);
	  };
	  Out.ar(out_bus, [chainL, chainR]);
	  
	}).send(server); 
	
	SynthDef("ss_fx_rev", { arg out_bus, in1, in2;
	  var chain, in, chaini, lfo, chainL, chainR, env;
	  in = In.ar([in1, in2]);
	  in = in.mean;
	  //in = Normalizer.ar(in, 0.02, 0.02);
	  //in = in * LFSaw.kr(0.1).range(0, 1);
	  env = EnvGen.kr(
		Env.new([0.1, 1, 0.1], [0.5, 0.5]), 
		timeScale:10, 
		gate:Dust.kr(0.1));
	  in = in * env;
	  chainL = in;
	  chainR = in;
	  3.do{
		chainL = AllpassL.ar(chainL, 0.01, 0.01.rand, 10.0);
	  };
	  3.do{
		chainR = AllpassL.ar(chainR, 0.01, 0.01.rand, 10.0);
	  };
	  Out.ar(out_bus, [chainL, chainR]);
	}).send(server);
	
	//	SynthDef("ss_fm", {arg out_bus, freq, dur;
	  
	//	}).send(s);

  }  
}


// this version reacts to onsets and collects pitches

SubsumptionOnsetPitch : SubsumptionOnset {
  var pitches, new_scale, pitch_count, chromatic, >collectNotes;
  prInit{
	super.prInit;
	pitches = PitchScanner.new({arg val;this.addPitch(val)});
	new_scale = Array.fill(6, {0});
	pitch_count = 0;
	chromatic = Array.fill(127, {arg i;i.midicps});
	collectNotes = 1;
  }

  addPitch{arg freq;
	if (freq > 0, {
	  freq = (chromatic.indexIn(freq)).midicps;
	  if (freq < 400, {freq = freq * 2});
	  
	  if (collectNotes == 1 && new_scale.indexOf(freq) == nil, {
		// we don't have this freq yet!
		//format("storing pitch % already there index: %", freq, new_scale.indexOf(freq)).postln;  
		new_scale[0] = freq;
		new_scale = new_scale.rotate(-1);
		pitch_count = pitch_count + 1;
		if (pitch_count > 5, {
		  // have a nice new scale - put it in the keys array
		  pitch_count = 0;
		  keys[keys.size-1] = new_scale; 
		  
		  //("new scale! "++new_scale).postln;
		});
	  });
	});
  }

  run{
	super.run;
	pitches.run;
  }

  free{
	super.free;
	pitches.free;
  }
}


// this version has an onset detector which influences the interval
// and triggers synth notes

SubsumptionOnset : Subsumption{
  var osc_resp, last_interval, last_onset, onset_synth, fft_buffer;
  var onset_power, onset_mode;

  prInit{
	var server;
	server = Server.local;
	// the osc responder which responds to onsets
	osc_resp = OSCresponderNode(server.addr, '/tr', {arg time,responder,msg;
	  //this.startRecord();
	  //[time, responder, msg].postln;
	  this.onset(msg[3]);
	}).add;
	fft_buffer = Buffer.alloc(server, 1024, 1);
	super.prInit;
  }

  onset{arg amp;
	var now;
	now = thisThread.seconds;
	last_interval = now - last_onset;

	last_onset = now;
	// now what to do...
	if (amp > 0.5, {onset_mode = 1}, {onset_mode = 0});
	
	if (onset_mode == 1, {
	  interval = last_interval * ([1, 2, 4, 8].choose);
	  this.playSynth}
	);
	
	//format("last interval was % meas_amp % o_power % onset on %", last_interval, amp, onset_power, onset_mode).postln;

  }
  

  run{
	super.run;
	onset_synth = Synth("submsumption_onsets", [\in_bus, in_bus, \fft_b, fft_buffer]);
  }

  reset{
	super.reset;
	last_onset = thisThread.seconds;
	onset_power = 0;
	onset_mode = 0;
  }
  free{
	super.free;
	osc_resp.remove;
	onset_synth.free;
	fft_buffer.free;
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	super.sendSynthDefs;
	SynthDef("submsumption_onsets", {arg rec_in=1, fft_b;
	  var onsets, fft, in, amp;
	  in = AudioIn.ar(rec_in);
	  fft = FFT(fft_b, in);
	  onsets = Onsets.kr(fft, 0.2, \rcomplex);
	  amp = Amplitude.kr(in, 0.01, 0.1);
	  SendTrig.kr(onsets, 200, amp);
	}).send(server);
  }

}

SubsumptionTempo : Subsumption{
  var tempo, oscResp;
  prInit{
	super.prInit;
	tempo = TempoDetect.new;//(in_bus: in_bus, id:101, intervalCB:{arg int;int.postln;this.playSynth});
	tempo.intervalCB_({arg int;
	  // synth length
	  interval = (int * [1, 2, 4].choose);
	  // multi synth in between length
	  min_interval = (int * [0.5, 0.25].choose);
	  //{(int * [0, 1, 2].choose).wait; this.playSynth}.fork;});
	  this.playSynth;
	});
	
	//{(int * [0, 1, 2].choose).wait; this.playSynth}.fork;});
	synthPlayerRout = Routine.new{
	  // stop the rout!
	};
	intervalShortenerRout = Routine.new{
	};
	
	intervalLengthenerRout = Routine.new{
	};
  }

  reset{
	super.reset;
	max_synth_complexity = 1;
	synthdefs = [
	  //"ss_sine", 
	  //"ss_saw", 
	  //"ss_pulse", 
	  "ss_dynklang", 
	  //"ss_form"
	];	
  }
  
  run{
	super.run;
	tempo.run;
  }

  stop{
	super.stop;
	tempo.intervalCB_({arg int;
	  int.postln;
	});
  }

  free{
	super.free;
	tempo.free;
  }
}



// this version has an onset detector which influences the interval
// and triggers synth notes

SubsumptionOnsetV2 : Subsumption{
  var osc_resp, last_interval, last_onset, onset_synth, fft_buffer;
  var onset_power, onset_mode;
  prInit{
	var server;
	server = Server.local;
  // the osc responder which responds to onsets
	osc_resp = OSCresponderNode(server.addr, '/tr', {arg time,responder,msg;
	  //this.startRecord();
	  //[time, responder, msg].postln;
	  this.onset(msg[3]);
	}).add;
	fft_buffer = Buffer.alloc(server, 1024, 1);
	super.prInit;
  }
  onset{arg amp;
	var now;
	now = thisThread.seconds;
	last_interval = now - last_onset;

	last_onset = now;
	// now what to do...
	//if (amp > 0.5, {onset_mode = 1}, {onset_mode = 0});
	onset_mode = 1;

	if (onset_mode == 1, {
	  interval = last_interval * ([1, 2, 4, 8].choose);
	  this.playSynth}
	);
	
	//format("last interval was % meas_amp % o_power % onset on %", last_interval, amp, onset_power, onset_mode).postln;

  }
  run{
	super.run;
	onset_synth = Synth("submsumption_onsets", [\in_bus, in_bus, \fft_b, fft_buffer]);
  }
  reset{
	super.reset;
	last_onset = thisThread.seconds;
	onset_power = 0;
	onset_mode = 0;
  }
  free{
	super.free;
	osc_resp.remove;
	onset_synth.free;
	fft_buffer.free;
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	super.sendSynthDefs;
	SynthDef("submsumption_onsets", {arg rec_in=1, fft_b;
	  var onsets, fft, in, amp;
	  in = AudioIn.ar(rec_in);
	  fft = FFT(fft_b, in);
	  onsets = Onsets.kr(fft, 0.2, \rcomplex);
	  amp = Amplitude.kr(in, 0.01, 0.1);
	  SendTrig.kr(onsets, 200, amp);
	}).send(server);
  }
}

