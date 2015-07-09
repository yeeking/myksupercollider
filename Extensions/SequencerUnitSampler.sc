// this'll do it:
//a = SequencerUnitSampler.new(
//  respNoteArray:[38, 51, 46, 48, 53], 
//  respCtlArray:[0],
//  respChannel:0,
//  //synthType:'SU_fft_binScrambler', 
//  synthType:'SU_verb', 
//  seqLength:24, 
//  outBus:0, 
//  fxBus:20,
//  ctlBus:22, 
//  inBus:1);
//a.free;
//b = SequencerUnitSampler.new(
//  respNoteArray:[36, 32, 46, 45, 53], 
//  respCtlArray:[0],
//  respChannel:0,
//  //synthType:'SU_fft_binShifter', 
//  //synthType:'SU_compressor', 
//  synthType:'SU_verb', 
//  seqLength:24,  
//  outBus:1, 
//  fxBus:21,
//  ctlBus:23, 
//  inBus:1);
// use this as an input:
// {Out.ar(2, Clip.ar(SinOsc.ar(LFDNoise1.kr(0.5, mul:200, add:100), mul:300), 0.01))}.play
//
SequencerUnitSampler : SequencerUnit {
  var 
  sequencerUnit, // object composition - this is a sequencer unit object
  <responderNoteDesc = "[0]-> play, [1]-> record, [2]->stop, [3]->step forwards, [4]->make new sequence",
  <responderCtlDesc = "[0]",
  ctlArrayWidth = 5, // how many params in the ctl seq
  ctlArrayLength= 9, // how many steps in the ctl seq?
  //  fxSynth_name,
  rec_start_time, 
  myBuffer,
  myFFTBuffer, 
  fxSynth, 
  loopingSynth, 
  level = 64, 
  alive=0;
  
  // this should initialise all sequences that are used
  initialiseSequence{
	var server;
	server = Server.local;
	// set level to something audible
	level = 64;
	if (alive==0, 
	  {
		// allocate the buffer
		myBuffer = Buffer.alloc(server,480000,1);
		myFFTBuffer = Buffer.alloc(server,256,1);
		//'sample buf num: '.post;
		//myBuffer.bufnum.postln;
		//'seqArrayLength'.post;
		//seqArrayLength.postln;
		alive=1;
	  });
	// either: if you have two sorts of sequence, one might have a prefixed length
	//this.initialiseCtlSeq(ctlArrayWidth, ctlArrayLength);
	// or: use the length sent to the constructor as seqLength
	this.initialiseCtlSeq(ctlArrayWidth, seqArrayLength);
  }
  
  
  setControl{ arg num, val;}

  triggerEvent{arg note, vel;
	//note.postln;
	switch (note, 
	  responderNotes[0], {this.trig_play(vel, 2, 0)}, 
	  responderNotes[1], {this.trig_record}, 
	  responderNotes[2], {this.trig_stop}, 
	  responderNotes[3], {this.trig_step}, 
	  responderNotes[4], {this.trig_newSequence}
	);
	
  }
  
  // methods taht get triggered by midi notes
  
  // set the sampler playing (un mute and back to start??)
  trig_play{arg vel, len, loop;
	var server, ctl2, ctl3, pitch;
	//'sampler playing'.postln;

	server = Server.local;
	//pitch = this.getPitch(ctlSeq.at(2).at(ctlSeqPos));
	pitch = vel;
	// inverse correlation between requested velocity and pitch (hit drum hard, it's pitched low)
	//pitch = vel-127;
	//pitch = pitch*pitch;
	//pitch = pitch.sqrt;
	

	// higher pitch, lower level
	//level = pitch.reciprocal;
	level = 64;
	//'playLoop'.postln;
	// free the looping synth
	fxSynth.free;
	loopingSynth.free;
	// slow it the fuck down
	//2000.do {
	//};
	// re-create loop synth
	switch (loop, 
	  1, {loopingSynth = Synth.head(server, "SU_sampleLooper", [\bufNum, myBuffer.bufnum, \outBus, fxBusNo, \ctl2Set, len, \ctl1Set, pitch, \ctl3Set, ctlSeq.at(4).at(ctlSeqPos), \direction, 1, \loopSet, loop])}, 
	  0, {pitch.postln;
		loopingSynth = Synth.head(server, "SU_sampleOneShot", [\bufNum, myBuffer.bufnum, \outBus, fxBusNo, \ctl2Set, len, \ctl1Set, pitch])}
	);
	// create the fx unit
	//fxSynth = Synth.tail(serverm "", 
	fxSynth = Synth.after(loopingSynth, synthDef, [
	  \bufNum, myFFTBuffer.bufnum, \audioInBus, fxBusNo, \outBus, outBusNo, 
	  \ctl1Set, ctlSeq.at(3).at(ctlSeqPos), \ctl2Set, ctlSeq.at(4).at(ctlSeqPos),\levelSet, level, \ctl1Set, pitch]);
  }

  // record a new sample
  trig_record{
	// start the record synth
	//'sampler recording'.postln;
	//Synth("sampleRecorder", [\bufNum, myBuffer.bufnum, \audioInBus, inBusNo]); 
	Synth("SU_sampleRecorder", [\bufNum, myBuffer.bufnum, \audioInBus, 1]); 
  }

  // stops the sample from playing (mutes it?)
  trig_stop{
	//'sampler stopping'.postln;
	//level = value;
	fxSynth.set(\levelSet, 0);
  }

  // moves the ctl seq position marker forwards
  trig_step{
	var server, pitch, fx, len, trig;
	//'sampler settping thru seq '.postln;
	//'ctlSeqPos'.post;
	//ctlSeqPos.postln;
	len = ctlSeq.at(3).at(ctlSeqPos)*ctlSeq.at(4).at(ctlSeqPos).reciprocal;
	this.trig_play(ctlSeq.at(2).at(ctlSeqPos), len, 1);
	
	//	this.movePointer
	// why no ++??
	ctlSeqPos = ctlSeqPos+1;
	// if its hit the end, set to zero
	if (ctlSeqPos==seqArrayLength,
	  {ctlSeqPos=0;}
	);
  }
  
  // converts the sent value, i the range 0-127 to a pitch value for the sampler, where 36 is normal speed and + - 1 is
  // up/ down a semi tone
  getPitch{ arg ctlValue;
	//^36+(ctlValue-64*0.1);
	^ctlValue-64;
  }

  trig_newSequence{
	//'sampler initialising sequence'.postln;
	this.initialiseSequence;
  }

  // this method updates the time unit length
  // it measures the distnace bewteen consequtive calls to this method
  // the time unit length is used to multiply the bar count 
  // that is read from the ctl sequence, so is like the tempo
  samp_length{
  }

  free {
	super.free;
	noteOnResponder.remove;
	noteOffResponder.remove;
	ctlResponder.remove;
	loopingSynth.free;
	fxSynth.free;
	myBuffer.free;
	myFFTBuffer.free;
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef(
	  "SU_sampleRecorder", {arg bufNum, audioInBus=1;
		// when this gets spawned, simply write from the requested audio input to the requested bus
		var audioIn, recorder,bufLength;
		bufLength = BufFrames.kr(bufNum)/BufSampleRate.kr(bufNum);
		audioIn = AudioIn.ar(audioInBus);
		//audioIn = Compander.ar(audioIn, audioIn, 0.5, 10, 1, 0.01, 0.1);		
		recorder = RecordBuf.ar(audioIn, bufNum, loop:0);
		// this envelope kills the synth after bufLength seconds
		EnvGen.kr(Env.perc(0, bufLength), 1.0, doneAction: 2);
		
		//EnvGen.kr(Env.perc(0, 1), 1.0, doneAction: 2) ;
	  }
	).send(server);//.writeDefFile;

// simple sample looper
	SynthDef(
	  "SU_sampleLooper", {arg bufNum, outBus, bufLengthBus, ctl1Set, direction, ctl2Set, ctl3Set;
		var playBuf, lookup, bufLength, pitch, env_freq, actual_length, envelope, midiTo1;
		//bufLength = In.kr(bufLengthBus);
		bufLength = ctl2Set;
		// convert 0-127 to a ratio, where * 2 = up an octave
		// pitch is used as a multiplier on the frequency
		//pitch = Lag.kr(ctl1Set).midiratio*0.125;
		midiTo1 = 1/127;
		pitch = ctl1Set*midiTo1+0.5;
		//pitch = ctl1Set.midiratio*0.125+LFDNoise1.kr(1.0, mul:5);
		//pitch = ctl1Set*0.001+LFDNoise1.kr(1.0, mul:5);
		//Out.kr(pitchBus) = 64;
		// this has pitch mod
		//lookup = Phasor.ar(0, BufRateScale.kr(bufNum)*pitch*direction, start:0, end:bufLength*BufSampleRate.kr(bufNum))+SinOsc.ar(10.0, mul:MouseX.kr(0.01, 10000));
		//lookup = Phasor.ar(0, BufRateScale.kr(bufNum)*pitch*direction, start:0, end:bufLength*BufSampleRate.kr(bufNum))*SinOsc.ar(MouseY.kr(0.001, 10), mul:MouseX.kr(0.001, 1));

		// this has start and end point mod
		//lookup = Phasor.ar(
		//  0, 
		//  BufRateScale.kr(bufNum)*pitch*direction, 
		//  start:bufLength*BufSampleRate.kr(bufNum)*MouseX.kr(0, 1), 
		//  end:bufLength*BufSampleRate.kr(bufNum)*MouseY.kr(0, 1));
		lookup = Phasor.ar(
		  0, 
		  BufRateScale.kr(bufNum)*direction*pitch, 
		  //start:0, 
		  start:bufLength*BufSampleRate.kr(bufNum)*LFDNoise1.kr(ctl3Set*0.01, mul:0.2), 
		  end:bufLength*BufSampleRate.kr(bufNum)*LFDNoise1.kr(ctl2Set, mul:0.2));

		
		// actual length is bufLength corrected for pitch
		actual_length = bufLength/pitch; 
		// number of actual_lengths per second
		env_freq = actual_length.reciprocal;
		//envelope = LFTri.ar(env_freq);
		envelope = EnvGen.ar(Env([0, 1, 1, 0], [0.01, actual_length-0.02, 0.01]), 
		  Impulse.ar(env_freq), doneAction:0);
		playBuf = BufRd.ar(numChannels:1,bufnum:bufNum, phase:lookup);	
		//playBuf = PlayBuf.ar(numChannels:1,bufnum:bufNum, trigger:1, rate:pitch, loop:0);	
		// savage compressoion
		//playBuf = Compander.ar(playBuf, playBuf,thresh:0.09,slopeBelow:0.2,slopeAbove:0.5,clampTime:0.01,relaxTime:0.1);
		// noise gate it
		//		Compander.ar(playBuf, playBuf, MouseX.kr(0.1, 1), 10, 1, 0.01, 0.01);
		Out.ar(outBus, playBuf*envelope*0.5);
		//Out.ar(outBus, playBuf);
	  }
	).send(server);//.writeDefFile;

	SynthDef("SU_sampleOneShot", {arg bufNum, outBus, ctl1Set, ctl2Set;
	  var playBuf, midiTo1, envelope;
	  //midiTo1 = 1/127*vel;
	  midiTo1 = 1/127;
	  playBuf = PlayBuf.ar(1,bufNum, rate: ctl1Set*midiTo1*2, loop:0);
	  //FreeSelfWhenDone.kr(playBuf);
	  envelope = EnvGen.kr(Env.perc(0.01, 0.2, 1.0), doneAction:2);
	  //Out.ar(outBus, playBuf*midiTo1);
	  Out.ar(outBus, playBuf*envelope);
	}).send(server);

	SynthDef(
	  "SU_compressor", {arg bufNum, audioInBus, outBus, ctl1Set, trigSet, levelSet;
		var level, midiTo1, chain;
		midiTo1 = 1/127;
		level = levelSet*midiTo1;
		chain = In.ar(audioInBus);
		chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.2,slopeAbove:0.5,clampTime:0.01,relaxTime:0.1);
		chain = chain*2;
		Out.ar(outBus, chain*level);
	  }
	).send(server);

	SynthDef(
	  "SU_verb", {arg bufNum, audioInBus, outBus, ctl1Set, ctl2Set, trigSet, levelSet;
		var level, midiTo1, chain, cleanChain;
		midiTo1 = 1/127;
		ctl1Set = ctl1Set*midiTo1*0.05;
		level = levelSet*midiTo1;
		chain = In.ar(audioInBus)*level;
		cleanChain = chain;
		4.do {
		  chain = AllpassL.ar(chain, 0.05, ctl1Set*1.0.rand, 0.5.rand+0.1*LFDNoise1.kr(2.0, add:1, mul:0.5));
		};
		chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.1,slopeAbove:0.5,clampTime:0.2,relaxTime:0.01);
		//chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.2,slopeAbove:0.5,clampTime:0.01,relaxTime:0.1);
		Out.ar(outBus, chain+cleanChain*0.75);
	  }
	).send(server);


	// fft bin scrambler 
	SynthDef(
	  "SU_fft_binScrambler", {arg bufNum, audioInBus, outBus, ctl1Set, trigSet, levelSet;
		var midiTo1, chain, trigger,bouncer, clean, scramble, fftOut, level;
		midiTo1 = 1/127;
		level = levelSet*midiTo1;
		scramble = ctl1Set*midiTo1;
		// make a linen envelope that takes the shift amount down then up again
		// when a change is triggered
		trigger = trigSet;
		bouncer = EnvGen.kr(Env.new([0.9, 0, 1], [0.01, 1], 'linear'), trigger, doneAction:0);
		clean = In.ar(audioInBus);
		//clean = AudioIn.ar(1);
		chain = FFT(bufNum, clean);
		//chain = PV_BinScramble(chain, Lag.kr(scramble*bouncer) , 0.5*bouncer, trigger); 
		chain = PV_BinScramble(chain, scramble*bouncer , 0.5*bouncer, trigger); 
		chain = IFFT(chain)*bouncer;
		fftOut = chain;
		// now passthe fft chain through all pass filters
		4.do{
		  chain = AllpassL.ar(chain, 0.1, 0.1.rand*scramble, LFDNoise1.kr(10))+chain;
		};
		
		chain = chain+(fftOut*0.3);
		chain = chain * 0.25;

		Out.ar(outBus, chain*level);
	  }
	).send(server);//.writeDefFile;

  }
}