// 
// synthDef - synth to use - can be updated live
// noteMultis - octave multipliers
// memoryLength, 
// polyphony, 
// audioInBus, 
// pitchBus
// growableMode - 1 to use a GrowableParser to play the synths, 0 to just play the current synthdef

EvoSynthPlayer : Object {

  var 
  // creation arguments
  >synthDef, noteMultis, memoryLength, >polyphony, audioInBus, pitchBus, growableMode, interpolMode, 
  noteIntervals, 
  stAmpMem, 
  stFreqMem, 
  ltAmpMem, 
  ltFreqMem, 
  // how frequently to read pitches and amplitudes
  interval = 0.25, 
  pitchScanner, 
  playbackRoutine, 
  ltWriteRoutine, 
  ltChooseRoutine, 
  notePosRoutine, 
  behaviourRoutine, 
  ltMemLength = 4, 
  // which of the stored sequences to read
  ltMemPos = 0, 
  // which of the notes in that stored sequence to read. 
  notePos = 0, 
  noteLength = 1, 
  ctl2 = 1, 
  playStMode = 0, 
  lastFreq = 0, 
  >octaves, 
  chromaticScale, 
  >outBus, 
  parser, 
  >forcePolyphony=100, 
  >playNoteFunc;

  *new { arg synthDef='xylo_evotest', noteMultis=[1], memoryLength=8, polyphony=1, audioInBus=1, pitchBus=5, growableMode=0, interpolMode=0;
  	^super.newCopyArgs(synthDef, noteMultis, memoryLength, polyphony, audioInBus, pitchBus, growableMode, interpolMode).prInit;
  }

  prInit {
	var allNotes, oct;
	this.sendSynthDefs;
	this.setupRoutines;
	this.playManyShortNotes;
	// simple 1d array
	stFreqMem = Array.fill(memoryLength, {0;});
	// 2d array 
	ltFreqMem = Array.fill(ltMemLength, {Array.fill(memoryLength, {0;});});
	pitchScanner = PitchScanner.new({arg value;this.storeFrequency(value);}, interval, audioInBus, pitchBus);
	octaves = [0.5, 1, 2];

	// set up the 8 octave chromatic scale used for getting closest frequencies
	chromaticScale = MiscFuncs.getChromaticFreqs;
	allNotes = Array.new(88);
	// now make it cover 8 octaves
	oct = 0.125;
	8.do{
	  chromaticScale.size.do{arg i;
		(chromaticScale.at(i)*oct);//.postln;
		allNotes = allNotes.add(chromaticScale.at(i)*oct);
	  };
	  oct = oct*2;
	};
	chromaticScale = allNotes;
	outBus = [0, 1];
	if (growableMode == 1, {
	  // need a parser
	  parser = GrowableParser.new(id:0, interpol_mode:interpolMode);
	});
	
  }

  // functions that the user should call 

  run {
	playbackRoutine.reset;
	ltWriteRoutine.reset;
	ltChooseRoutine.reset;
	notePosRoutine.reset;
	behaviourRoutine.reset;	

	ltWriteRoutine.play;
	ltChooseRoutine.play;
	notePosRoutine.play;
	playbackRoutine.play;
	behaviourRoutine.play;

	pitchScanner.run;
	if (growableMode == 1, {
	  parser.run;
	});
  }
  
  stop{
	playbackRoutine.stop;
	ltWriteRoutine.stop;
	ltChooseRoutine.stop;
	notePosRoutine.stop;
	behaviourRoutine.stop;
	pitchScanner.stop;
	if (growableMode == 1, {
	  parser.stop;
	});
  }
  
  free{
	this.stop;
	pitchScanner.free;
	if (growableMode == 1, {
	  parser.free;
	});
  }

  playFewLongNotes{
	// create a new set of intervals
	noteIntervals = Array.fill(8, {arg i; ((i+1.01)*0.5).rand});
	noteLength = 0.5.rand+0.5;
	ctl2 = 0.5.rand+0.5;
  }
  
  playManyShortNotes{
	// create a new set of intervals
	noteIntervals = Array.fill(8, {arg i; ((i+1.01)*0.2).rand});
	noteLength = 0.1.rand+0.1;
	ctl2 = 0.1.rand+0.1;
  }
  
  // only play notes from the short term memory...
  playFromShortTermMemory{
	playStMode = 1;
  }

  playFromLongTermMemory{
	playStMode = 0;
  }
  
  setupRoutines{

	playbackRoutine = Routine{
	  inf.do {
		min(polyphony, forcePolyphony).do{
		  //		  ("").postln
		  this.playNote;
		  this.updateNotePos;
		  // spread the notes out a bit...
		  0.1.rand.wait;
		};
		noteIntervals.choose.wait;
		//1.0.wait;
	  };
	};
	// this updates the contents of the long term memory 
	// by adding the short term memory
	ltWriteRoutine = Routine{
	  inf.do{
		this.updateLongTermMemory;
		// time for a complete memory write+2 .rand...
		5.wait;
		//((memoryLength/interval.rand)+2).wait;
	  }
	};
	// this chooses the sequence we are reading from the long term memory
	ltChooseRoutine = Routine{
	  inf.do{
		var memChoice;
		memChoice = (ltFreqMem.size.rand);
		ltMemPos = memChoice;
		// wait for st memory length * 4 
		// so we stick on this sequence for 4 'bars'
		((memoryLength/interval) * 4).wait;
	  };
	};

	// this one updates the position within the chosen note sequence
	// that gets played when play note gets triggered. 
	notePosRoutine = Routine{
	  inf.do{
		// TODO: put a switch statement in here for linear or random
		// 'next note' selection??
		this.updateNotePos;
		noteIntervals.choose.wait;
	  };
	};
	  
	// this one monitors the short term memory. 
	// and alters the playback behaviour accordingly
	behaviourRoutine = Routine{
	  inf.do{
		// calc the mean
		var mean, total=0, sd, prop_sd, octs;
		stFreqMem.size.do{ arg i;
		  total = total + stFreqMem.at(i);
		};
		mean = total/stFreqMem.size;
		total = 0;
		stFreqMem.size.do{ arg i;
		  total = total + (stFreqMem.at(i)-mean).pow(2);
		};
		sd = total.sqrt;
		"mean: ".post;
		mean.postln;
		"sd: ".post;
		sd.postln;
		// now calculate the octave range based on the sd
		prop_sd = sd/mean;
		"prop_sd: ".post;
		prop_sd.postln;
		
		// high polyphont for low variation
		polyphony = ((1/prop_sd).round)+1;
		"polyphony: ".post;
		polyphony.postln;
		
		// now calc the note intervals
		// high sd = short note intervals
		noteIntervals = Array.fill(8, {arg i; ((i+(1/prop_sd))*0.5).rand});
		noteIntervals.postln;
		//noteLength = (prop_sd*4).rand;
		//noteLength = (prop_sd*127).rand;
		
		//noteLength = 127.rand;

		// now calc the octaves
		prop_sd = prop_sd *4;
		octs = [0.125, 0.25, 0.5, 1, 2, 4];
		if (prop_sd < 1, {octaves = [1]}, {
		  var start;
		  // if prop_sd > 1
		  // add a selection of multipliers
		  octaves = Array.new;
		  start = octs.size.rand;
		  prop_sd.do{arg i;
			octaves = octaves.add(octs.wrapAt(i+start)
			);
		  }}
		);
		octaves.postln;

		// if sd is much higher than mean, they are playing lots of 
		// varied notes - go crazy!
				if (sd > (mean), 
		  {polyphony = 2;this.playFromLongTermMemory;this.playManyShortNotes;}, 
		  {polyphony = 4;this.playFromLongTermMemory;this.playFewLongNotes}
		);
		// extra mad
		if (sd > (mean*2), {
		  noteIntervals = Array.fill(8, {arg i; ((i+0.2)*0.1).rand});
		  polyphony = 1;
		  this.playFromShortTermMemory});
		// extra mellow
		if (sd < (mean*0.5), {polyphony = 3;this.playFromLongTermMemory});
		
		(2+5.0.rand).wait;
	  };
	}
	
  }

  updateNotePos{
	notePos = notePos+1;
	// reset if needed2
	if (notePos == memoryLength, {notePos = 0});
	//noteIntervals.choose.wait;
  }

  playNote{
	var freq;
	// basic version plays the oldest note in the stFreqMem
	// playing a note.
	if (playStMode == 1, 
	  {freq = stFreqMem.at(notePos);}, 
	  {freq = ltFreqMem.at(ltMemPos).at(notePos);}
	);
	// check if we are in the bootstrap phase where there are not many notes..
	if (freq ==0 , 
	  {freq = stFreqMem.at(stFreqMem.size-1)});
	//freq.postln;

	// now mutiply it to some octave 
	freq = octaves.choose * freq;
	
	//parser.playSound(level:1, freq:freq, length:noteLength, out_bus:outBus.choose);
	if (playNoteFunc == nil, {
	  // if we have no custom playNodeFunc
	if (growableMode == 1, {
	  freq.postln;
	  parser.playSound(level:1, freq:freq, length: noteLength);
	}, {
	  Synth(synthDef, [\freq, freq, \ctl1, noteLength, \ctl2, ctl2, \outBus, outBus.choose]);
	});
	},
	  // if we have a custom playNoteFunc:
	  {
		playNoteFunc.value(freq, noteLength, outBus.choose);
	  }
	);
  }
  
  storeFrequency{ arg freq;
	var smallestDiff=1000, closestFreq, diff=0; 
	if (freq != lastFreq && freq != 0, 
	  {
		// to find a matching freq
		chromaticScale.size.do{arg i;
		  // how close is the incoming freq to the one in the array?
		  diff = chromaticScale.at(i)-freq;
		  // make it +ve
		  diff = diff*diff;
		  diff = diff.sqrt;
		  if (diff < smallestDiff, 
			{smallestDiff=diff;
			  closestFreq = chromaticScale.at(i);}
		  );
		};
		freq = closestFreq;
		// only add freq if different from the last one we recorded
		stFreqMem = stFreqMem.rotate(-1);
		stFreqMem.put(stFreqMem.size-1, freq);
		lastFreq = freq;
	  });
  }
  
  storeAmp{ arg amp;
	// use an AmpScanner to read an amp value
	stAmpMem.rotate(-1);
	stAmpMem.put(stAmpMem.size-1, amp);
  }

  updateLongTermMemory{
	//"updating long term memory with this: ".postln;
	//stFreqMem.postln;
	// throw away the oldest memory 
	ltFreqMem = ltFreqMem.rotate(-1);
	// put current stFreqMem in long term... at a random place. That should keep it interesting
	ltFreqMem.put((ltFreqMem.size-1).rand, stFreqMem);
	//ltFreqMem.postln;
  }
  

  sendSynthDefs{
	var server;
	server = Server.local;

	// simple playback synth
	SynthDef("xylo_evotest", {arg freq, ctl1=64, ctl2=64, outBus=0;
	  var env, midiTo1, dur, noiseEnv, noiseDur, noise;
	  midiTo1 = 1/127;
	  //dur = ctl1 * midiTo1*2;
	  dur = ctl1;
	  noiseDur = ctl2*midiTo1*0.2;
	  // envelope for the noise burst
	  noiseEnv = EnvGen.kr(Env.perc(0.001, noiseDur, 0.25, -4));
	  noise = Resonz.ar(PinkNoise.ar(1.0), freq, 0.1, mul:10)*noiseEnv;
	  //env = EnvGen.kr(Env.perc(0.01, dur+1.0, 0.25, -4), doneAction:2);
	  env = EnvGen.kr(Env.new([0, 0.25, 0.1, 0], [0.01, dur*2, dur*3]), doneAction:2);
	  Out.ar(outBus, (SinOsc.ar(freq, mul:env)+noise) *0.1);
	}).send(server);


	
	// simple playback synth
	SynthDef("xylo_evotest", {arg freq, ctl1=64, ctl2=64, outBus=0;
	  var env, midiTo1, dur, noiseEnv, noiseDur, noise;
	  midiTo1 = 1/127;
	  //dur = ctl1 * midiTo1*2;
	  dur = ctl1;
	  noiseDur = ctl2*midiTo1*0.2;
	  // envelope for the noise burst
	  noiseEnv = EnvGen.kr(Env.perc(0.001, noiseDur, 0.25, -4));
	  noise = Resonz.ar(PinkNoise.ar(1.0), freq, 0.1, mul:10)*noiseEnv;
	  //env = EnvGen.kr(Env.perc(0.01, dur+1.0, 0.25, -4), doneAction:2);
	  env = EnvGen.kr(Env.new([0, 0.25, 0.1, 0], [0.01, dur*2, dur*3]), doneAction:2);
	  Out.ar(outBus, (SinOsc.ar(freq, mul:env)+noise)*0.1);
	}).send(server);

	// simple playback synth
	SynthDef("fm_evotest", {arg freq, ctl1=64, ctl2=64, outBus=0;
	  var env, midiTo1, dur, env2, carr, mod;
	  midiTo1 = 1/127;
	  dur = ctl1;
	  mod = ctl2;
	  //Select.kr();
	  
	  //env = EnvGen.kr(Env.perc(0.01, dur+1.0, 0.25, -4), doneAction:2);
	  env = EnvGen.kr(Env.new([0, 0.25, 0.1, 0], [ctl2*0.25, dur*2, dur*3]), doneAction:2);
	  env2 = EnvGen.kr(Env.new([0, 0.25, 0.1, 0], [0.01, ctl2*0.5, ctl1*3]), doneAction:2);

	  freq = SinOsc.ar(freq*2+(ctl2*10), mul:freq*LFNoise1.kr(0.2).range(0, 10))+freq;
	  freq = SinOsc.ar(freq, mul:freq*env2*ctl1*LFDNoise1.kr(0.5).range(0, 2))+freq;
	  freq = SinOsc.ar(freq*0.125, mul:ctl1*LFDNoise1.kr(0.5).range(0, 20))+freq;
	  carr = SinOsc.ar(freq, mul:env);
	  Out.ar(outBus, carr*0.05);
	}).send(server);


  }

  // end of functions that the user should call 
}


