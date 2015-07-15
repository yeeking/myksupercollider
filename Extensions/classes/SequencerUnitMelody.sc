// generatlisation of the melody sequencer
// with an extra creation argument added to define the polyphony

// // good settings!
// d = SequencerUnitMelody.new (
//   respNoteArray:[45, 53],
//   respCtlArray:[0],
//   respChannel:0,
//   musicalKeys:['random'],
//   musicalScales:['random'], 
//   synthType:'crazySynth',
//   polyphony:1,
//   seqLength:100, 
//   noteRange:[0.5, 0.25, 1, 4, 0.125], 
//   outBus:[1], 
//   fxBus:40);
// d.free;
// e = SequencerUnitMelody.new (
//   respNoteArray:[45, 53],
//   respCtlArray:[0],
//   respChannel:0,
//   musicalKeys:['random'],
//   musicalScales:['random'], 
//   synthType:'sineFMSynth',
//   polyphony:1,
//   seqLength:100, 
//   noteRange:[2], 
//   outBus:[1], 
//   fxBus:40);
// e.free;


SequencerUnitMelody : SequencerUnit {
  var sequence, 
  <responderNoteDesc = "[0]->trigger note, [1]->new sequence note",
  <responderCtlDesc = "[0]->initialise note sequence with random notes from key [0] ", 
  keyFs,// stores an array of the frequencies for the current key
  noteSeq, // stores an array of arrays of freqs for the current note sequence
  noteSeqPos = 0, // stores position in current note sequence
  ctlArrayWidth = 4,// 2 control sequences
  ctlArrayLength= 9, 
  availableSynthDefs, 
  // used by the GUI to set the number of octaves above the selected octave to 
  // pull notes from 
  octaveCount=1, 
  // this 
  noteMultiIndex=0;
  
  // methods overriden from parent class ----
  
  // picks a random key and generates a random sequence 
  // in that key
  initialiseSequence{
	var keyMap, key, scale;
	this.initialiseCtlSeq(ctlArrayWidth, ctlArrayLength);
	// generate 2 d array of freqs using a selection of freqs from a certain key
	scale = MiscFuncs.getScale(keyIndexes.choose.postln, scaleIndexes.choose.postln);
	noteSeq = Array.fill(seqArrayWidth, 
	  {// generate an array containing random notes from the scale
		// octaved up or down by a scalar chosen from the noteMultis array (e.g. 2 - up an octave)
		Array.fill(seqArrayLength, {scale.at(scale.size.rand)*(noteMultis.choose)});// end inner array fill
	  });// end outer array fill
  }
  free{
	noteOnResponder.remove;
	noteOffResponder.remove;
	ctlResponder.remove;
  }
  setControl{arg num, val;
  }

  triggerEvent{arg note, vel;
	var trigNote, newSeqNote, outMulti;
	// play a note
	trigNote = responderNotes[0];
	// generate a new sequence in the current key
	newSeqNote = responderNotes[1];
	// decide what to do 
	switch (note, 
	  trigNote, {
		// make a synth set up using the next step in the sequence
		//noteSeq[noteSeqPos].postln;
		var outMulti = vel/seqArrayWidth;
		if (outMulti < 30, {outMulti=outMulti+20});
		seqArrayWidth.do{arg i; 
		  var freq, ctl1, ctl2, ctl3, ctl4;
		  //outMulti.postln;
		  freq = noteSeq.at(i).at(noteSeqPos);
		  ctl1 = ctlSeq.at(0).at(ctlSeqPos);
		  ctl2 = ctlSeq.at(1).at(ctlSeqPos);
		  ctl3 = ctlSeq.at(2).at(ctlSeqPos);
		  ctl4 = ctlSeq.at(3).at(ctlSeqPos);
		  //ctl1.postln;
		  vel = vel_map[vel-1];
		  Synth.new(synthDef,[\freq, freq, \vel, (vel), \outBus, outBusNo.choose, \outMulti, outMulti, \ctl1, ctl1, \ctl2, ctl2, \ctl3, ctl3, \ctl4, ctl4]);
		};
		// why no ++??
		noteSeqPos = noteSeqPos+1;
		// if its hit the end, set to zero
		if (noteSeqPos==seqArrayLength,
		  {noteSeqPos=0;}
		);
		// why no ++??
		ctlSeqPos = ctlSeqPos+1;
		// if its hit the end, set to zero
		if (ctlSeqPos==ctlArrayLength,
		  {ctlSeqPos=0;}
		);
	  }, 
	  newSeqNote, {
		// generate a new sequence in the current key
		this.initialiseSequence;
	  }
	);// end switch
  }

  //// these methods are an implementation of my paramterGUI interface
  // (not sure how to do interfaces in sc...)


  // returns an array of values defining the parameter widgets recognised by this class
  // of the form [idS, labelS, typeS,  initialValueS, lowS, highS, stepS, listOptionsS]
  // (see ParameterWidget new method)
  parameterWidgetSpec{
	var spec;
	spec =[
	  // on off toggle
	  [{arg value; activationState=value;}, 0, "algotrigmelody on-off ", ParameterWidget.checkbox], 
	  // octave select list
	  [{arg value; 
		// choose frequency  multiplier array
		noteMultiIndex = value;
		noteMultis = this.makeNoteMultis;
		// create a new sequence using this octave setting
		this.initialiseSequence;
	  }, 2, "algotrigmelody octave ", ParameterWidget.listView, 0, 0, 0, 0, 
		[0, 1, 2, 3, 4, 5, 6, 7]], 
	  [{arg value; octaveCount=value+1;noteMultis=this.makeNoteMultis;this.initialiseSequence;}, 2, "algotrigmelody octave count ", ParameterWidget.listView, 0, 0, 0, 0, 
		[1, 2, 3]], 
	  // synth def select list
	  [{arg value; synthDef=availableSynthDefs.at(value);}, 2, "algotrigmelody synthDef ", ParameterWidget.listView, 0, 0, 0, 0, 
		availableSynthDefs], 
	  // scale select list
	  [{arg value; 
		scaleIndexes = [MiscFuncs.availableScales.at(value)];this.initialiseSequence;
	  }, 2, "algotrigmelody scale ", ParameterWidget.listView, 0, 0, 0, 0, 
		MiscFuncs.availableScales], 
	  // key select list
	  [{arg value; 
		keyIndexes = [MiscFuncs.availableKeys.at(value)];this.initialiseSequence;
	  }, 2, "algotrigmelody key ", ParameterWidget.listView, 0, 0, 0, 0, 
		MiscFuncs.availableKeys] , 
	  // polyphpny select list
	  [{arg value;seqArrayWidth=value+1;this.initialiseSequence;}, 2, "algotrigmelody poly ", ParameterWidget.listView, 0, 0, 0, 0, 
		[1, 2, 3, 4]],
	];
	
	^spec;
  }

  // this method generates an array of multipliers for note generation
  // e.g. if startOctave = 1 and this.octaveCount = 2
  // noteultis = [0.125, 0.25]  - the multipliers for the first 2 octaves

  makeNoteMultis { 
	
	var octaves, newMultis;
	octaves = [0.125, 0.25, 0.5, 1, 2, 4, 8];
	newMultis = Array.new;
	octaveCount.do{arg i;
	  newMultis = newMultis.add(octaves.wrapAt(i+noteMultiIndex));
	}
	^newMultis.postln;
  }

  ///// end methods from the parameterGUI interface
  
  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef("seq_bass", { arg freq=220, vel=64, ctl1=64, ctl2=64, ctl3=64, ctl4=64, outBus;
	  var car, mod, env, env2, midiTo1, ind, muls, mul, sel, len;
	  midiTo1 = 1/127;
	  vel = vel * midiTo1;
	  freq = freq *2;
	  //muls = [0.375, 0.75, 1.25, 1.375];
	  muls = [0.24, 2.212, 3.1545];
	  ind = ctl1*midiTo1*20*vel;
	  sel = ctl2*midiTo1*muls.size;
	  mul = Select.kr(sel, muls);
	  len = (ctl3*midiTo1*2)+0.05;
	  ctl4 = ctl4*midiTo1;
	  //env2 = EnvGen.kr(Env.new([0, 1, 0.5, 0], [0.025, 0.05, 0.5]), doneAction:0);
	  //env = EnvGen.kr(Env.new([0, 1, 0.5, 0], [0.05, 0.05, 0.5], -4), doneAction:2);
	  env = EnvGen.kr(Env.perc(0.005, 1.0), timeScale: len, levelScale:0.5, doneAction:2);
	  env2 = EnvGen.kr(Env.perc(0.005, ctl4*2), timeScale: len, doneAction:0);
	  mod = SinOsc.ar(freq * mul, mul:freq*env2*ind);
	  car = SinOsc.ar(freq+mod, mul:env) * Pulse.ar(freq*0.25, mul:env*ctl4);
	  Out.ar(outBus, car*0.5);
	}).send(server);

	//availableSynthDefs=["crazySynth", "sineFMSynth", "xyloSynth", "xyloSynthLFO", "vectorSynth", "pulseSynth", "bassSynth2", "bachSynth"];
	SynthDef("crazySynth", {arg freq, vel, outBus, outMulti=0.5, ctl1, ctl2, ctl3, ctl4;
	  var impulse, sine, chain, midiTo1,inforce, outforce, k, d;
	  // normalise everything from 0-1
	  midiTo1 = 1/127;
	  ctl1 = midiTo1*ctl1;
	  ctl2 = midiTo1*ctl2;
	  ctl3 = midiTo1*ctl3;
	  ctl4 = midiTo1*ctl4;
	  vel = midiTo1*vel;
	  //inforce = K2A.ar(MouseButton.kr(0,1,0)) > 0; 
	  //k = MouseY.kr(0.1, 20, 1);
	  //d = MouseX.kr(0.00001, 0.1, 1);
	  outforce = Spring.ar(1, Line.kr(ctl4*0.000001, ctl3*ctl2, vel), ctl1*0.1);
	  outforce = outforce * outforce;
	  freq = outforce * freq; // modulate frequency with the force
	  chain = SinOsc.ar(freq+LFDNoise1.kr(100, mul:freq*0.0625))*EnvGen.kr(Env.perc(0.01, 1.0*ctl2, vel), doneAction:2);
	  Out.ar(outBus, chain*0.2);
	}
	).send(server);
	SynthDef ("bassSynth2", {arg freq, outBus, ctl1, ctl2, ctl3, ctl4, vel;
	  var chain, env, midiTo1, pulse; 
	  midiTo1 = 1/127;
	  ctl1 = midiTo1*ctl1;
	  ctl2 = midiTo1*ctl2;
	  ctl3 = midiTo1*ctl3*0.25;
	  ctl4 = midiTo1*ctl4;
	  vel = vel*midiTo1;
	  env = EnvGen.ar(Env.perc(0.001, 0.1+vel, vel), doneAction:2);
	  //env = EnvGen.kr(Env.new([0, 1.0, 0.5, 0], [0.01, 0.05, 0.1+vel], 'exponential'), doneAction:2);
	  chain = Formant.ar(freq, XLine.kr(freq*0.0125, freq, 0.01), ctl1, 0.125);
	  pulse = LPF.ar(Pulse.ar(freq+SinOsc.ar(0.1, mul:10), Line.ar(0.01, 0.4, 0.1), mul:0.01), vel*127.midicps);
	  chain = chain + pulse;
	  Out.ar(outBus, chain*env);  
	}).send(server);
	
	// ctl1 controls mod index range. mod index is modulated by an XLine as well.
	SynthDef("sineFMSynth", {arg freq, vel, outBus=1, outMulti=0.5, ctl1, ctl2;
	  var chain, midiTo1;
	  midiTo1 = 1/127;
	  vel = midiTo1*vel;
	  // ctl1 1 will be 0-127
	  // normalise to 0->freq*0.25
	  ctl2 = midiTo1*ctl2*0.05;
	  ctl1 = midiTo1*ctl1*ctl2;
	  ctl1 = ctl1 * (freq*0.25);
	  outMulti = midiTo1*outMulti;
	  // FM synthesis
	  chain = SinOsc.ar(freq+(ctl1*SinOsc.ar(freq-ctl1, mul:XLine.kr(freq*0.25, freq*2, vel))), mul:vel);
	  chain = chain*EnvGen.kr(Env.perc(0.02, vel*4, 0.25, -4), doneAction:2);
	  chain = Out.ar(outBus, chain*outMulti*0.5);
	}// end synth graph function
	).send(server);

	SynthDef("sineSynth", {arg freq, vel, outBus=1, outMulti=0.5, ctl1;
	  var chain, midiTo1;
	  midiTo1 = 1/127;
	  vel = midiTo1*vel;
	  // ctl1 1 will be 0-127
	  // normalise to 0->4
	  ctl1 = midiTo1*ctl1;
	  ctl1 = ctl1 * 4;
	  outMulti = midiTo1*outMulti*2;
	  chain = SinOsc.ar(freq+(SinOsc.ar(vel*5, mul:10)*vel), mul:vel)*EnvGen.kr(Env.perc(0.02, vel*ctl1, 0.25, -4), doneAction:2);
	  //chain = chain*EnvGen.kr(Env.perc(0.02, vel*ctl1, 0.25, -4), doneAction:2);
	  chain = Out.ar(outBus, chain*outMulti*0.25);
	}// end synth graph function
	).send(server);


	SynthDef("bachSynth", {arg freq, vel, outBus=1, outMulti=0.5, ctl1, ctl2;
	  var chain, midiTo1, env, mod, envLength;
	  midiTo1 = 1/127;
	  vel = midiTo1*vel;
	  // ctl1 1 will be 0-127
	  // normalise to 0->4
	  ctl1 = midiTo1*ctl1;
	  ctl2 = midiTo1*ctl2;
	  ctl1 = ctl1 * 4;
	  envLength = vel*2;//*10*MouseX.kr(0.01, 1);
	  outMulti = midiTo1*outMulti*2;
	  mod = SinOsc.ar(0.125*freq, mul:Line.ar(0, freq*0.125, envLength*4));
	  env = EnvGen.kr(Env.perc(envLength+0.1, envLength, 0.25, -2), doneAction:2);
	  chain = Pulse.ar(freq+mod, SinOsc.ar(freq*0.125)*vel, mul:vel*env);
	  //chain = BPF.ar(chain, freq*2);
	  //chain = chain*EnvGen.kr(Env.perc(0.02, envLength, 0.25, -4), doneAction:2);
	  chain = Out.ar(outBus, chain*outMulti*0.25);
	}// end synth graph function
	).send(server);


	SynthDef("pulseSynth", {arg freq, vel, outBus=1, outMulti=0.5, ctl1;
	  var chain, midiTo1;
	  midiTo1 = 1/127;
	  vel = midiTo1*vel*0.5;
	  // ctl1 1 will be 0-127
	  // normalise to 0->4
	  ctl1 = midiTo1*ctl1;
	  ctl1 = ctl1 * 4;
	  outMulti = midiTo1*outMulti*2;
	  chain = Pulse.ar(freq, mul:vel)*EnvGen.kr(Env.perc(0.01, vel, 0.25, -4), doneAction:2);
	  //chain = chain*EnvGen.kr(Env.perc(0.02, vel*ctl1, 0.25, -4), doneAction:2);
	  chain = Out.ar(outBus, chain*outMulti*0.6);
	}// end synth graph function
	).send(server);
	
	

SynthDef("xyloSynth",
  {arg freq=440, outBus=0, ctl1=64, vel=64;
	var chain, mult1, mult2, mult3, dur, env, imp_env, midiTo1, ringTime;	
	midiTo1 = 1/127;
	mult1 = 1.33875;
	mult2 = 1.44;
	mult3 = 2.15375;
	dur = ctl1*midiTo1+0.2;
	vel = midiTo1*vel;
	//dur = 0.5;
	ringTime = dur*2;
	imp_env = EnvGen.kr(Env.perc(0.001, 0.02), levelScale:0.01);
	env = EnvGen.kr(Env.perc(0.01, ringTime*2, 1.0, -4), levelScale:0.1, doneAction:2);
	//chain = HPF.ar(PinkNoise.ar(imp_env), 100);
	chain = PinkNoise.ar(imp_env);
	chain = DynKlank.ar(`[[freq, freq*mult1, freq*mult2, freq*mult3], nil, [5, 2, 2, 1]], chain);
	chain = chain+SinOsc.ar(freq);
	//chain = HPF.ar(chain, 5);
	//chain = LeakDC.ar(chain);
	Out.ar(outBus, chain*env);
  }
).send(server);

	
	SynthDef("xyloSynthLFO",
	  {arg freq=440, outBus, ctl1, imp_env;
		var chain, mult1, mult2, mult3, dur, env, midiTo1, ringTime, lfo, harms, mod_freq;
		midiTo1 = 1/127;
		mult1 = 1.33875;
		mult2 = 1.44;
		mult3 = 2.15375;
		dur = ctl1*midiTo1+0.2;
		//dur = 0.5;
		ringTime = dur*10;
		env = EnvGen.kr(Env.perc(0.01, ringTime, 1.0, -4), doneAction:2, levelScale:0.1);
		imp_env = EnvGen.kr(Env.perc(0.001, 0.2), levelScale:0.5 * ctl1 * midiTo1);
		chain = HPF.ar(PinkNoise.ar(imp_env), 100);
		//chain = DynKlank.ar(`[[freq, freq*mult1, freq*mult2, freq*mult3], nil, [1, 1, 1, 1]], chain);
		//chain = DynKlank.ar(`[[freq, freq*mult1, freq*mult2, freq*mult3], [1.0, 0.25, 0.25, 0.25], [ringTime, ringTime, ringTime, ringTime]], chain*0.005);
		lfo = SinOsc.kr(Line.kr(0.1, 5, dur), mul:freq*0.02*Line.kr(0.1, 1, dur*0.5));
		mod_freq = freq+lfo;
		chain = DynKlank.ar(`[[freq, mod_freq*mult1, mod_freq*mult2, freq*mult3], [1.0, 0.5, 0.5, 0.25], [ringTime, ringTime, ringTime, ringTime]], chain*0.05);
		// harms
		//harms = Mix.ar(SinOsc.ar([freq, mod_freq*2, mod_freq*4], mul:[env*0.05, env*0.02, env*0.01]));
		//chain = chain;//+harms;
		//		chain = HPF.ar(chain, 5);
		//chain = LeakDC.ar(chain);
		Out.ar(outBus, chain*env);
	  }
	).send(server);
	
	SynthDef("bassSynth", {arg freq, vel, outBus=0, outMulti=0.5, ctl1;
	  var chain, midiTo1;
	  midiTo1 = 1/127;
	  outMulti = midiTo1*outMulti;
	  vel = (vel*midiTo1)+0.1;
	  chain=Pulse.ar(freq, LFNoise1.kr(1), mul:0.25);
	  chain=Resonz.ar(
		chain, 
		75+EnvGen.kr(Env.perc(0.001*vel, (1.0/vel), freq*4, -5))*SinOsc.ar(vel*5, add:1, mul:0.5), 
		0.2);
	  chain=chain*EnvGen.kr(Env.perc(0.01, (1.0/vel), 0.5, -5), doneAction:2);
	  Out.ar(outBus, chain*outMulti);
	}// end synth graph function
	).send(server);
	
	SynthDef("vectorSynth", {arg freq, ctl1, outBus, vel;
	  var sine, pulse, saw, chain, midiTo1, pwm, len;
	  midiTo1 = 1/127;
	  vel = vel * midiTo1;
	  len = vel * 2.0+0.1;
	  // normalise ctl1 for use in pulse width mod amount 
	  pwm = ctl1 * midiTo1 * 20;
	  // sine with amp and freq mod
	  sine = SinOsc.ar(freq+(Line.kr(0.01, 1)*LFDNoise1.kr(10, mul:freq*0.03125)), 
		// increasing amp mod over time
		mul:SinOsc.ar(XLine.ar(1, freq*2, len), phase: 1pi, mul:0.5, add:1););
	  pulse = Pulse.ar(freq,SinOsc.ar(XLine.ar(1, pwm, 0.5), phase: 0.5pi, mul:0.5, add:1), 
		// amp mod out of phase with sine amp mod
		mul:SinOsc.ar(XLine.ar(1, freq*2, len), phase: 2pi, mul:0.5, add:1));
	  pulse = pulse * 0.15;
	  saw = Saw.ar(freq*2, mul:Line.kr(0.01, 0.2, len));
	  chain = (sine+saw+pulse)*EnvGen.kr(Env.perc(0.1, len, vel*0.1), doneAction:2);
	  //chain = (sine)*EnvGen.kr(Env.perc(0.1, len, 0.2), doneAction:2);
	  Out.ar(outBus, chain*0.5);
	}).send(server);

  }
  
  
}
