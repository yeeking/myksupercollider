// special type of SequencerUnitSampler that takes a priming note before recording
// i.e.:
// priming note comes in
// on next play note, take a new sample for that note
//a = SequencerUnitSamplerPrime.new(
//  // prime, stop, step, newseq, samplers 1, 2, 3
//  respNoteArray:[44, 36, 46, 46, 36, 38,40, 48, 45], 
//  polyphony:5, 
//  synthType:'SU_verb', 
//  respCtlArray:[0],
//  respChannel:0,
//  seqLength:24, 
//  outBus:0, 
//  fxBus:20,
//  ctlBus:22, 
//  inBus:1);
// use this as an input:
// {Out.ar(2, Clip.ar(SinOsc.ar(LFDNoise1.kr(0.5, mul:200, add:100), mul:300), 0.01))}.play
//
SequencerUnitSamplerPrime : SequencerUnit {

  var 
  sequencerUnit, // object composition - this is a sequencer unit object, well maybe one day...
  <responderNoteDesc = "[0]->prime for recording, [1]->stop, [2]->step, [3]->new seq, [4->4+polyphony]-> notes for trigger play/ record(when primed), low vel->fx playback high vel->simple pitched playback", 
  <responderCtlDesc = "[0]",
  ctlNoteCount = 4, // how many items in the respomderNoteArray before the play notes? (update if i add more controls e.g. mutate)
  ctlArrayWidth = 6, // how many params in the ctl seq
  ctlArrayLength= 4, // how many steps in the ctl seq?
  keyFs,// stores an array of the frequencies for the current key
  noteSeq, // stores an array of arrays of freqs for the current note sequence
  noteSeqPos = 0, // stores position in current note sequence
  //  fxSynth_name,
  rec_start_time, 
  myBuffer,// remove
  myBuffers,
  myFFTBuffer, // remove
  fxSynth, // remove
  loopingSynth, // remove
  fxSynths, 
  loopingSynths,
  fxBuses, // to connect looping synths to fx synths
  level = 64, 
  primed = 0,
  alive=0, 
  fxPolyphony=3; // for tuned fx, how many notes?
  
  // this should initialise all sequences that are used
  initialiseSequence{
	var server, scale;
	server = Server.local;
	// set level to something audible
	level = 64;
	if (alive==0, 
	  {this.initialiseBuffers;
		alive=1;
	  });
	// either: if you have two sorts of sequence, one might have a prefixed length
	//this.initialiseCtlSeq(ctlArrayWidth, ctlArrayLength);
	// or: use the length sent to the constructor as seqLength
	this.initialiseCtlSeq(ctlArrayWidth, seqArrayLength);
	// now make the melody sequence

	// generate 2 d array of freqs using a selection of freqs from a certain key
	scale = MiscFuncs.getScale(keyIndexes.choose.postln, scaleIndexes.choose.postln);
	noteSeq = Array.fill(fxPolyphony,// 3 note polyphony for tuned effects 
	  {// generate an array containing random notes from the scale
		// octaved up or down by a scalar chosen from the noteMultis array (e.g. 2 - up an octave)
		Array.fill(seqArrayLength, {scale.at(scale.size.rand)*(noteMultis.choose)});// end inner array fill
	  });// end outer array fill
  }
  
  initialiseBuffers{
	// make an array with one buffer for each note of polyphony
	var server = Server.local;
	myBuffers = Array.fill(seqArrayWidth, 
	  {Buffer.alloc(server,100000,1);}
	);
	
	// make the arrays for the playback synths
	loopingSynths = Array.newClear(seqArrayWidth);
	// and fx synths
	fxSynths = Array.newClear(seqArrayWidth);
	// and the fxsynth bus numbers
	fxBuses = Array.fill(seqArrayWidth, 
	  {arg i;
		//"fxBus: ".postln;
		//(i+ctlNoteCount+fxBusNo).postln;
		i+ctlNoteCount+fxBusNo;
	  }
	);
  }


  setControl{ arg num, val;}

  triggerEvent{arg note, vel;
	var counter;
	//note.postln;
	
	//switch (note, 
	  //responderNotes[0], {this.trig_play(vel, 2, 0)}, 
	if (note==responderNotes[0], {this.trig_prime;});
	if (note==responderNotes[1], {this.trig_stop;});
	if (note==responderNotes[2], {this.trig_step;});
	if (note==responderNotes[3], {this.trig_newSequence;});

	// if we get here, it's not one of the first (ctlNoteCount) notes
	// from the responderNotes array so it could be a play note
	// check all play notes to see if it is

	counter = 0;
	while ({counter != seqArrayWidth}, 
	  {//"incoming note: ".post;
		//note.postln;
		//"ctl note ".post;
		//responderNotes[counter+ctlNoteCount].postln;
		if (note==responderNotes[counter+ctlNoteCount], 
		  {
			// note matches on of the play notes
			// check if we are primed. if so, record
			if (primed==1, 
			  {
				this.trig_rec2(counter);
				this.trig_play2(vel, counter);
				primed=0;

			  }, 
			  // if not, play
			  {this.trig_play2(vel, counter);});
			// kill the while loop
			counter = seqArrayWidth-1;}
		);
		counter = counter+1;}
	);	
  }

  // methods taht get triggered by midi notes

  // set it up to record - the next play note that gets triggered will trigger play and record
  // at the same time
  trig_prime{
	//'priming'.postln;
	primed=1;
  }

  // play the sent buffer
  trig_play2{arg vel, index;
	var pitch, length, freq1, freq2, freq3, loopingSynthDef, server = Server.local;
	//"playing buffer: ".post;
	//index.postln;
	//"buffer index: ".postln;
	//	myBuffers[index].bufnum.postln;
	loopingSynths[index].free;
	fxSynths[index].free;
	pitch = vel;
	if (pitch<64, {pitch=pitch+32});

	//pitch = 64;
	length = 2;

	if (vel<64, 
	  {
		// play the looper for low velocities
		//"play loop".postln;
		// create a looping synth
		loopingSynthDef = synthDef.at(1).choose;//.postln;
		
		loopingSynths[index] = Synth.head(server, loopingSynthDef, [\bufNum, myBuffers[index].bufnum, \outBus, fxBuses[index], \ctl2Set, ctlSeq.at(4).at(ctlSeqPos), \ctl1Set, pitch, \ctl3Set, ctlSeq.at(4).at(ctlSeqPos), \direction, 1, \loopSet, 1]);
	  }, 
	  {
		// play the one shot for high velocities
		//"play one shot".postln;
		loopingSynths[index] = Synth.head(server, "SU_sampleOneShot", [\bufNum, myBuffers[index].bufnum, \outBus, fxBuses[index], \ctl2Set, length, \ctl1Set, pitch]);
	  });
	// now the fxsynth
	switch (mode, 
	  1, {
		// tuned - use the note seq 
		freq1 = noteSeq.at(0).at(noteSeqPos);
		freq2 = noteSeq.at(1).at(noteSeqPos);
		freq3 = noteSeq.at(2).at(noteSeqPos);

	  }, 
	  0, {
		// not tuned - use the ctl seq
		freq1 = ctlSeq.at(3).at(ctlSeqPos);
		freq2 = ctlSeq.at(4).at(ctlSeqPos);
		freq3 = ctlSeq.at(5).at(ctlSeqPos);
		
	  });
	//"f1: %\t, f2: %\t, f3: %\t %\n".postf(freq1, freq2, freq3);
	fxSynths[index] = Synth.after(loopingSynth, synthDef.at(0).choose, [
	  \audioInBus, fxBuses[index], \outBus, outBusNo.choose, 
	  \ctl1Set, freq1, \ctl2Set, freq2 ,\ctl3Set, freq3, \levelSet, level]);
	
  }
  
  trig_rec2{arg buffer;
	//"recording into buffer ".post;
	//buffer.postln;
	// record into the appropriate buffer
	Synth("SU_sampleRecorder", [\bufNum, myBuffers[buffer].bufnum, \audioInBus, 1]); 
  }

  // stops the sample from playing (mutes it?)
  trig_stop{
	//	'stopping samplers'.postln;
	//level = value;
	// set all the levels on the fx synths to 0
	fxSynths.size.do({arg i;
	  fxSynths.at(i).set(\levelSet, 0);
	});
  }

  // moves the ctl seq position marker forwards
  trig_step{
	var server, pitch, fx, len, trig,freq1, freq2, freq3, freq4;
	//'sampler settping thru seq '.postln;
	//'ctlSeqPos'.post;
	//ctlSeqPos.postln;

	// why no ++??
	ctlSeqPos = ctlSeqPos+1;
	// if its hit the end, set to zero
	if (ctlSeqPos==seqArrayLength,
	  {ctlSeqPos=0;}
	);
	
	// step through the melody sequence
	// why no ++??
	noteSeqPos = noteSeqPos+1;
	// if its hit the end, set to zero
	if (noteSeqPos==seqArrayLength,
	  {noteSeqPos=0;}
	);
	

	fxSynths.size.do({arg i;
	  switch (mode, 
		1, {
		  // tuned - use the note seq 
		  freq1 = noteSeq.at(0).at(noteSeqPos);
		  freq2 = noteSeq.at(1).at(noteSeqPos);
		  freq3 = noteSeq.at(2).at(noteSeqPos);
		  
		}, 
		0, {
		  // not tuned - use the ctl seq
		  freq1 = ctlSeq.at(3).at(ctlSeqPos);
		  freq2 = ctlSeq.at(4).at(ctlSeqPos);
		  freq3 = ctlSeq.at(5).at(ctlSeqPos);
		  
		});
	  //"f1: %\t, f2: %\t, f3: %\t %\n".postf(freq1, freq2, freq3);
	  fxSynths[i].set(\ctl1Set, freq1, \ctl2Set, freq2 ,\ctl3Set, freq3);
	  //fxSynths.at(i).set(\levelSet, 0, );
	});
	
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
	loopingSynths.size.do({arg i;
	  loopingSynths.at(i).free;
	});
	fxSynths.size.do({arg i;
	  fxSynths.at(i).free;
	});
	myBuffers.size.do({arg i;
	  myBuffers.at(i).free;
	});

  }

  // implemetation of the interface that allows this class to specify a gui and what the gui does
  
  parameterWidgetSpec{
	var spec;
	spec = [
	  [{arg value; activationState=value;this.stopLoop;}, 0, "primable on-off ", ParameterWidget.checkbox]
	];
	^spec;
  }


  sendSynthDefs{
	var server;
	server = Server.local;

	SynthDef("SU_sampleOneShot", {arg bufNum, outBus, ctl1Set, ctl2Set;
	  var playBuf, midiTo1, envelope;
	  //midiTo1 = 1/127*vel;
	  midiTo1 = 1/127;
	  playBuf = PlayBuf.ar(1,bufNum, rate: ctl1Set*midiTo1*2, loop:0);
	  //playBuf = PlayBuf.ar(1,bufNum, rate: 1, loop:0);
	  //FreeSelfWhenDone.kr(playBuf);
	  //envelope = EnvGen.kr(Env.perc(0.01, ctl2Set*midiTo1*4, 1.0), doneAction:2);
	  envelope = EnvGen.kr(Env.perc(0.01, 1.0, 1.0), doneAction:2);
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
		Out.ar(outBus, chain*level*0.25);
	  }
	).send(server);
	
	SynthDef(
	  "SU_verb", {arg bufNum, audioInBus, outBus, ctl1Set=64, ctl2Set, ctl3Set,trigSet, levelSet;
		var level, midiTo1, chain, cleanChain;
		midiTo1 = 1/127;
		ctl1Set = ctl1Set*midiTo1*0.05;
		level = levelSet*midiTo1;
		chain = In.ar(audioInBus)*level;
		cleanChain = chain;
		4.do {
		  chain = AllpassL.ar(chain, 0.06, ctl1Set*1.0.rand+0.01, 0.2.rand+0.1*LFDNoise1.kr(2.0, add:1, mul:0.5));
		  //chain = AllpassL.ar(chain, 0.05, ctl1Set*1.0.rand, 2.0.rand);
		  //chain = AllpassL.ar(chain, 0.05, 0.05.rand, 2.0.rand);
		};
		
		chain = Compander.ar(chain, chain,thresh:0.1,slopeBelow: 1,slopeAbove: 0.1,clampTime: 0.01,relaxTime: 0.01);

		//chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.1,slopeAbove:0.5,clampTime:0.2,relaxTime:0.01);
		//chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.2,slopeAbove:0.5,clampTime:0.01,relaxTime:0.1);
		Out.ar(outBus, (chain+cleanChain));
	  }
	).send(server);

	// this one is a tuned reverb where the incoming ctl1-3 values are assumed to be frequencies
	SynthDef(
	  "SU_verbTuned", {arg bufNum, audioInBus, outBus, ctl1Set=64, ctl2Set, ctl3Set, trigSet, levelSet;
		var level, midiTo1, chain, cleanChain, del1, del2, del3;
		midiTo1 = 1/127;
		// the delay that generates a given freq of rung
		// is the reciprocal of that freq
		del1 = ctl1Set.midicps.reciprocal;
		del2 = ctl2Set.midicps.reciprocal;
		del3 = ctl3Set.midicps.reciprocal;
		
		level = levelSet*midiTo1;
		chain = In.ar(audioInBus)*level;
		cleanChain = chain;
		// tuned allpass filters
		chain = CombL.ar(cleanChain, 0.2, del1, 10.0)+chain;
		chain = CombL.ar(cleanChain, 0.2, del2, 10.0)+chain;
		chain = CombL.ar(cleanChain, 0.2, del3, 10.0)+chain;
		chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.2,slopeAbove:0.5,clampTime:0.2,relaxTime:0.1);
		//chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.01,slopeAbove:0.1,clampTime:0.01,relaxTime:0.1);
		Out.ar(outBus, chain * 0.25);
	  }
	).send(server);

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

	// looping sampler that uses noise lfos to move the start and end points of the loop
	SynthDef(
	  "SU_sampleLooperNoise", {arg bufNum, outBus, bufLengthBus, ctl1Set, direction, ctl2Set, ctl3Set;
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
		  end:bufLength*BufSampleRate.kr(bufNum)*LFDNoise1.kr(1.0, mul:0.2));
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

	// looping sampler that uses Lines to move the start and end points of the loop to make a spring effect
	SynthDef(
	  "SU_sampleLooperSpring", {arg bufNum, outBus, bufLengthBus, ctl1Set, direction, ctl2Set, ctl3Set;
		var playBuf, lookup, bufLength, pitch, env_freq, actual_length, envelope, midiTo1;
		//bufLength = In.kr(bufLengthBus);
		bufLength = ctl2Set;
		// convert 0-127 to a ratio, where * 2 = up an octave
		// pitch is used as a multiplier on the frequency
		//pitch = Lag.kr(ctl1Set).midiratio*0.125;
		midiTo1 = 1/127;
		pitch = ctl1Set*midiTo1+0.5;
		//pitch = 1;
		//ctl2Set = ctl2Set*0.1;
		lookup = Phasor.ar(
		  0, 
		  BufRateScale.kr(bufNum)*direction*pitch, 
		  start:Line.ar(0, 200, ctl2Set*4)+Line.ar(0, 10000, 20), 
		  end:Line.ar(5000, 10, ctl3Set*0.02));
		// actual length is bufLength corrected for pitch
		actual_length = bufLength/pitch; 
		// number of actual_lengths per second
		env_freq = actual_length.reciprocal;
		//envelope = LFTri.ar(env_freq);
		envelope = EnvGen.ar(Env([0, 1, 1, 0], [0.01, actual_length-0.02, 0.01]), 
		  Impulse.ar(env_freq), doneAction:0);
		playBuf = BufRd.ar(numChannels:1,bufnum:bufNum, phase:lookup);	
			Compander.ar(playBuf, playBuf, MouseX.kr(0.1, 1), 10, 1, 0.01, 0.01);
		Out.ar(outBus, playBuf*envelope);
		//Out.ar(outBus, playBuf);
	  }
	).send(server);//.writeDefFile;


SynthDef(
	  "SU_dist_verb", {arg bufNum, audioInBus, outBus, ctl1Set, ctl2Set, trigSet, levelSet;
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
		//chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.2,slopeAbove:0.5,clampTime:0.2,relaxTime:0.1);
		Out.ar(outBus, chain+cleanChain*0.75);
	  }
	).send(server);

  }
}