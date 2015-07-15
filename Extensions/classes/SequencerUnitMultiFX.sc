// E.G: 
// test file for the sequencer unit objects
//a = SequencerUnitMultiFX.new(
//  respNoteArray:[45, 53],
//  respCtlArray:[0],
//  respChannel:0,
//  synthType:'ultraphaserFX', 
//  seqLength:10, 
//  outBus:2, 
//  fxBus:6);
//a.run;
//a.free;

SequencerUnitMultiFX : SequencerUnit {
  var 
  <responderNoteDesc = "[0]->trigger note, [1]->new sequence note",
  <responderCtlDesc = "[0]->initialise note sequence with random notes from key [0] ",
  ctlArrayWidth = 4, // one for each fx param
  ctlArrayLength= 9, 
  synthReady=0, 
  fxSynth;

  initialiseSequence{
	this.initialiseCtlSeq(ctlArrayWidth, seqArrayLength);
  }
  
  setControl{ arg num, val;}

  triggerEvent{arg note, vel;
	var stepNote, newSeqNote;
	stepNote = responderNotes[0];
	newSeqNote = responderNotes[1];
	note.postln;
	switch (note, 
	  stepNote, {// read soem data from ctl seq and step forwards
		// pass data from ctlseq to the synth
		
		fxSynth.set("ctl1", ctlSeq.at(0).at(ctlSeqPos), "ctl2", ctlSeq.at(1).at(ctlSeqPos), "ctl3", ctlSeq.at(2).at(ctlSeqPos));
		// why no ++??
		ctlSeqPos = ctlSeqPos+1;
		// if its hit the end, set to zero
		if (ctlSeqPos==seqArrayLength,
		  {ctlSeqPos=0;}
		);
	  }, 
	  newSeqNote, {
		// generate a new sequence in the current key
		this.initialiseSequence;
	  });	
  }
  
  // overriden from parent such as to free the fxsynth
  free {
	super.free;
	fxSynth.free;
  }

  // implemetation of the interface that allows this class to specify a gui and what the gui does
  
  parameterWidgetSpec{
	var spec;
	spec = [
	  [{arg value; activationState=value;
		switch(activationState, 
		  true, {this.run}, 
		  false, {fxSynth.free}
		);
	  }, 0, "FX on-off ", ParameterWidget.checkbox]
	];
	^spec;
  }


  // as this has a permanent synth def, we use an init method
  // which should be called a little time after the .new method
  // so server can write synthdefs
  run {
	fxSynth = Synth(synthDef, [\inBus, fxBusNo, \outBus, outBusNo, \ctl1, 24, \ctl2, 64, \ctl3, 24]);
  }

  sendSynthDefs{
 	var server;
	server = Server.local;
	SynthDef("ultraphaserFX", {arg ctl1, ctl2, ctl3,  audioInBus=1, outBus;
	  // two parameters - buses for -60db time and delay time
	  // defaults are chan 0, ctl 0, 1 = bus 4 + 5
	  var source, source2, stages, length, midiTo1, feedbackScalar, delayScalar, feedback, delay, variation, outMultiplier, feedbackSet, delaySet, variationSet;
	  feedbackSet = ctl1;
	  delaySet = ctl2;
	  variationSet = ctl3;
		midiTo1 = 1/127;
		// scale midi to 0.01-0.5
		feedbackScalar = (midiTo1)*(3.0-0.01);
		// scale mudu to 0.0001-0.5
		delayScalar = (midiTo1)*(0.45-0.0001);
		//delayScalar = (midiTo1)*(0.01-0.0001);
		//delay = Lag.kr(delaySet*delayScalar+0.0001);
		// this needs to be even
		// as half the stages go the left and half to the right
		stages = 6;
		// scales the signal based on 
		outMultiplier = 1/stages;
	  //feedback = Lag.kr(feedbackSet*feedbackScalar+0.01);
	  //delay = Lag.kr(delaySet*delayScalar+0.0001);
	  feedback = feedbackSet*feedbackScalar+0.01;
	  delay = delaySet*delayScalar+0.0001;

		variation = variationSet*0.01;
		// source 1 goes in the left channel
		source = AudioIn.ar(audioInBus);
		// noise gate
		//source = Compander.ar(source, source, 0.1, 5, 1, 0.2, 0.2);
		// expander
		source = Compander.ar(source, source, thresh:0.5, slopeBelow:0.2,slopeAbove:0.01,clampTime:0.01,relaxTime:0.7);
		// source 2 goes in the right channel
		source2 = source;
		
		stages/2.do({source = source + 
		  (AllpassL.ar(source, 0.5, delay*1.0.rand*LFDNoise1.ar(variation, mul:0.5, add:1), feedback));});
		stages/2.do({source2 = source2 + 
		  (AllpassL.ar(source2, 0.5, delay*1.0.rand*LFDNoise1.ar(variation, mul:0.5, add:1), feedback));});
		// scale them down to 0.5. max (1/sat
		source = source*outMultiplier;
		source2 = source2*outMultiplier;
		Out.ar(outBus, 
		  [
			Compander.ar(source, source,thresh:0.09,slopeBelow:0.1,slopeAbove:0.5,clampTime:0.2,relaxTime:0.01) * 0.1, 
			Compander.ar(source2, source2,thresh:0.09,slopeBelow:0.1,slopeAbove:0.5,clampTime:0.2,relaxTime:0.01) * 0.1
		  ]
		);
	}).send(server);

	// pitch shifted reverb
	SynthDef("revPitchFX",  {arg inBus=0, outBus=0, ctl1, ctl2, ctl3;
	  var chain, rev_maxDelay, rev_max60db, rev_maxCutoff, midiTo1, rev_60db, rev_delay, rev_cutoff, pitchR;
	  rev_60db = ctl1;
	  rev_delay = ctl2;
 	  rev_cutoff = ctl3;
	  // pitch ration
	  pitchR = ctl1;
	  midiTo1 = 1/127;	  
	  // delay line length in allpasses for reverb
	  rev_maxDelay = 0.1;
	  // max decay for allpasses
	  rev_max60db = 1.5;
	  rev_maxCutoff = 12000;
	  // normalise rev_delay to 0-rev_maxDelay
	  rev_delay = rev_delay*midiTo1*rev_maxDelay+0.0025;
	  // normalise rev_60db to 0-rev_max60db
	  rev_60db = rev_60db*midiTo1*rev_max60db+0.5;
	  rev_cutoff = rev_cutoff*midiTo1*rev_maxCutoff;
	  // now build the chain
	  chain = In.ar(inBus);
	  chain = PitchShift.ar(chain, 0.02, pitchR*midiTo1*4, ctl2*LFDNoise1.kr(4)*midiTo1*4, ctl3*midiTo1*0.2)+chain;
	  //chain2 = chain;
	  4.do({
		chain = AllpassL.ar([LPF.ar(chain, rev_cutoff+500.0.rand), LPF.ar(chain, rev_cutoff+500.0.rand)], rev_maxDelay, rev_delay*(1.0.rand+0.01), rev_60db);
	  });
	  Out.ar(outBus, chain);
	}).send(server);
	
	// an airier reverb
	SynthDef("revCleanFX", {arg inBus=0, outBus=0, ctl1, ctl2, ctl3;
	  var chain, rev_maxDelay, rev_max60db, rev_maxCutoff, midiTo1, rev_60db, rev_delay, rev_cutoff;
	  rev_60db = ctl1;
	  rev_delay = ctl2;
 	  rev_cutoff = ctl3;
	  midiTo1 = 1/127;	  
	  // delay line length in allpasses for reverb
	  rev_maxDelay = 0.1;
	  // max decay for allpasses
	  rev_max60db = 10.0;
	  rev_maxCutoff = 12000;
	  // normalise rev_delay to 0-rev_maxDelay
	  rev_delay = rev_delay*midiTo1*rev_maxDelay+0.0025;
	  // normalise rev_60db to 0-rev_max60db
	  rev_60db = rev_60db*midiTo1*rev_max60db+2.0;
	  rev_cutoff = rev_cutoff*midiTo1*rev_maxCutoff+150;
	  // now build the chain
	  chain = In.ar(inBus);
	  //chain2 = chain;
	  4.do({
		chain = AllpassL.ar([LPF.ar(chain, rev_cutoff+SinOsc.kr(ctl3*0.25, mul:300.0.rand, add:400)), LPF.ar(chain, rev_cutoff+500.0.rand)], rev_maxDelay, rev_delay*(1.0.rand+0.01), rev_60db);
	  });
	  Out.ar(outBus, chain*0.2);
	}).send(server);
	
	// a tonal reverb with distortion and some dirty compression
	SynthDef("revDistFX", {arg inBus=0, outBus=0, ctl1, ctl2;
	  var chain, chain2, rev_delay, rev_60db, rev_maxDelay, rev_max60db, midiTo1;
	  rev_60db = ctl1;
	  rev_delay = ctl2;
	  midiTo1 = 1/127;
	  // delay line length in allpasses for reverb
	  rev_maxDelay = 0.025;
	  // max decay for allpasses
	  rev_max60db = 3;
	  // normalise rev_delay to 0-rev_maxDelay
	  rev_delay = rev_delay*midiTo1*rev_maxDelay+0.001;
	  // normalise rev_60db to 0-rev_max60db
	  rev_60db = rev_60db*midiTo1*rev_max60db+0.01;
	  chain = AudioIn.ar(0);
	  // noise gate it
	  chain = Clip.ar(chain, -0.01, 0.01)*100;
	  chain = Compander.ar(chain, chain, 0.1, 5, 1, 0.2, 0.2);
	  chain2 = chain;
	  4.do{arg i;
	  //chain = AllpassL.ar(chain, rev_maxDelay, rev_delay, rev_60db);
		chain = AllpassL.ar(chain, rev_maxDelay, rev_delay*(1.0.rand), rev_60db);
		chain2 = AllpassL.ar(chain2, rev_maxDelay, Lag.kr(rev_delay*1.0.rand, 5.0), rev_60db);
	  };
	  chain = Compander.ar([chain, chain2], chain2,thresh:0.09,slopeBelow:0.1,slopeAbove:0.5,clampTime:0.2,relaxTime:0.01);
	  Out.ar(outBus, chain);
	}).send(server);

	SynthDef("distFX", {arg inBus=0, outBus=0, ctl1, ctl2;
	  var chain, midiTo1;
	  chain = In.ar(inBus);
	  midiTo1 = 1/127;
	  //chain = AudioIn.ar(1);
	  // noise gate it
	  chain = Clip.ar(chain, -0.01, 0.01)*100;
	  chain = chain + DelayL.ar(chain, 0.1, SinOsc.kr(ctl1*midiTo1).range(0.001, 0.1));
	  chain = Compander.ar(chain, chain, 0.1, 5, 1, 0.2, 0.2);
	  Out.ar(outBus, chain);
	}).send(server);

	
	SynthDef("additiveRand2",{arg ctl1, ctl2, envGate=1, envRelease=0.1;
	  var partialMults, sines, envelope, partialCount,partialVolume, partialMute, baseFreq, midiTo1;
	  midiTo1 = 1/127;
	  partialCount = 100;
	  // normalise partial mute to number of partials
	  partialMute = ctl1*midiTo1*partialCount;
	  baseFreq = ctl1.midicps;
	  // generate the partial mult array
	  partialMults = Array.fill(partialCount, {rrand(0.5, 20.0)});
	  // make the sin oscs
	  sines = Array.fill(partialCount, 
		{
		  arg i;
		  //SinOsc.ar(partialMults[i]*baseFreq,0,mul:partialVolume>i*0.5)
		  // partials with vibrato
		  //SinOsc.ar(SinOsc.ar(0.4, add:200, mul:1)+baseFreq*partialMults[i], 0, (partialMute>i)*0.2);
		  SinOsc.ar(baseFreq*partialMults[i], 0, (partialMute>i)*0.2) * SinOsc.kr(LFDNoise1.kr(0.25).range(0.1, 0.5));
		});
	  // now add the osc with the base freq
	  sines.add(SinOsc.ar(baseFreq,0,1));
	  
	  // make the envelope
	  envelope = EnvGen.ar(
		Env.adsr(0.02, 0.2, 0.25, envRelease, 1, -4), envGate,
		doneAction:0
	  );
	  // mix them together
	  Out.ar([0,1], Pan2.ar(sines.mean)*envelope*10);
	}).send(server)

  }
}