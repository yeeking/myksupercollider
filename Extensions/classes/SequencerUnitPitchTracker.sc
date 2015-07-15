// This class is a special kind of sequencer that
// does monophonic pitch analysis on an incoming audio signal
// and generates a synthesised polyphonic melody based on the pitches coming in
// it stores the amp data in the first row of the control data
// and freq in the second row

// example:
//a = SequencerUnitPitchTracker.new(synthType:'xylo',noteRange:[1, 2, 3], modeSwitch:1);
//a.run
//a.triggerPeriodArray_([0.5]);
//a.triggerPeriodArray_([0.5, 1, 2]);
// set polyphony to 1
//a.playbackPolyphony_(1);
// set playback mode to looped
//a.playbackMode_(1);
// and random
//a.playbackMode_(0);
//a.playbackPolyphony_(3);
// stop scanning in notes
//a.writeNewNotes_(0);
// scan at 0.125 per second
//a.scanInterval_(0.5);


SequencerUnitPitchTracker : SequencerUnit {
  var 
  <responderNoteDesc = "[0] -> trigger an event, [1] -> generate a new sequcne",
  <responderCtlDesc = "[0]",
  ctlArrayWidth = 4, // how many params in the ctl seq
  ctlArrayLength= 4, // how many steps in the ctl seq?
  oscResponder, // stores the osc reponder object which store amp and freq data to the control array
  oscPollRoutine, // stores the routine that polls the control buses that store freq and amp data
  notePlayRoutine, // routine that plays notes using the data from the audio analysis
  freqAnalSynth, // stores a reference to the freq analysing synth for free-ing later on
  ampAnalSynth, // stores a reference to the amp analysing synth for free-ing later on
  lastFreq = 0,// prob get rid of this
  ctlSeqWritePos=0,// where to erite the next detected note in the pitch sequence 
  ctlSeqReadPos=0, // where to read the next note to play from the sequence
  >scanInterval = 0.125, 
  chromaticScale,
  >writeNewNotes=1, // set to 0 to stop note scanning
  >playbackMode=1, // 0 for randomly chosen notes, 1 to loop throught the stored sequence
  >playbackPolyphony=3, // how many notes to play each time triggerEvent is called
  >triggerPeriodArray,  // an array of times in seconds to wait between calls to triggerEvent
  pitchCtlBus = 2, // which control bus to use for pitch
  pitchScanner;

  // this should initialise all sequences that are used
  initialiseSequence{
	var server, oct, allNotes;
	server = Server.local;
	// this sets up a default set of waiting times between calls to triggerEvent 
	// from which one is chosen at random
	triggerPeriodArray = [0.5, 1, 2];
	// either: if you have two sorts of sequence, one might have a prefixed length
	//this.initialiseCtlSeq(ctlArrayWidth, ctlArrayLength);
	// or: use the length sent to the constructor as seqLength
	this.initialiseCtlSeq(ctlArrayWidth, ctlArrayLength);
	// set up the 12 tones
	// that is used for frequency mapping
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
	chromaticScale;//.postln;

	pitchScanner = PitchScanner.new({arg value;this.addNewFreq(value);}, scanInterval, 1, pitchCtlBus);
	
	// set up the note playing routine
	notePlayRoutine = Routine({
	  inf.do { 
		this.triggerEvent(0, 0);
		triggerPeriodArray.choose.wait;
	  };
	});

  }

  // adds the sent frequency to the current frequency array 
  addNewFreq{arg freq;
	var smallestDiff=1000, closestFreq, diff=0; 
	if (writeNewNotes==1, {
	  // put freq to row 1 of the ctl seq at current position
	  switch (mode, 
		0, {// just store whatever freq came in
		  ctlSeq.at(0).put(ctlSeqWritePos, freq);}, 
		1, {// scan through several octaves of the chromatic scale 
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
		  ctlSeq.at(0).put(ctlSeqWritePos, closestFreq);
		}
	  );
	  //ctlSeq.at(0);//.postln;
	  ctlSeqWritePos = ctlSeqWritePos+1;
	  if (ctlSeqWritePos>=ctlSeq.at(0).size, 
		{ctlSeqWritePos=0}
	  );
	  ctlSeq.at(0);//.postln;
	});
  }

  // call this method to tell it to choose the next frequency at random from the 
  // stored list
  randomFreqMode{
  }

  // call this method to tell it to loop through the stored frequency list
  loopFreqMode{
	
  }

  setControl{ arg num, val;}
 
  // gets called by the osc responder everytime the osc poller routine asks for a 
  // new value
  triggerEvent{arg note=0, vel=0;
	// save the freq and amp values to the ctl array rows 0 and 1
	//note = 
	playbackPolyphony.do{
	  var freq;
	  // set the freq based on the octaves passed into the constructor
	  //freq = lastFreq*noteMultis.choose;
	  switch (playbackMode, 
		0, {freq = ctlSeq.at(0).choose * noteMultis.choose;}, 
		1, {
		  // next one in the sequence
		  freq = ctlSeq.at(0).at(ctlSeqReadPos);
		  // move along
		  ctlSeqReadPos = ctlSeqReadPos+1;
		  if (ctlSeqReadPos>=ctlSeq.at(0).size, 
			{ctlSeqReadPos = 0;}
		  );
		});	
	  //Synth("simpleSine", [\freq, freq, \vel, 127.0.rand, \ctl1, 127.0.rand]);
	  Synth(synthDef, [
		\freq, freq, 
		\vel, ctlSeq.at(1).at(ctlSeqPos), 
		\ctl1, ctlSeq.at(2).at(ctlSeqPos), 
		\ctl2, ctlSeq.at(3).at(ctlSeqPos), 
		\outBus, [0,1].choose]);
	  0.125.rand.wait;
	};
  }

  // this sets the osc polling and synth generating routines in action
  // as well as running the synths that generate the analysis data
  run {
	pitchScanner.run;
	oscPollRoutine.play;
	notePlayRoutine.play;
  }

  // overidden from parent so we can free the osc responder also
  free {
	super.free;
	//freqAnalSynth.free;
	pitchScanner.free;
	oscResponder.remove;
	oscPollRoutine.stop;
	notePlayRoutine.stop;
  }

  sendSynthDefs{
	var server;
	server = Server.local;

	// simple playback synth
	SynthDef("simpleSine", {arg freq, ctl1;
	  var env, midiTo1, dur;
	  midiTo1 = 1/127;
	  dur = ctl1 * midiTo1*2;
	  env = EnvGen.kr(Env.perc(0.01, dur, 0.25, -4), doneAction:2);
	  Out.ar(0, SinOsc.ar(freq, mul:env));
	}).send(server);

	// simple playback synth
	SynthDef("xylo", {arg freq, ctl1, ctl2, outBus;
	  var env, midiTo1, dur, noiseEnv, noiseDur, noise;
	  midiTo1 = 1/127;
	  dur = ctl1 * midiTo1*2;
	  noiseDur = ctl2*midiTo1*0.2;
	  // envelope for the noise burst
	  noiseEnv = EnvGen.kr(Env.perc(0.001, noiseDur, 0.25, -4));
	  noise = Resonz.ar(PinkNoise.ar(1.0), freq, 0.1, mul:10)*noiseEnv;
	  env = EnvGen.kr(Env.perc(0.01, dur, 0.25, -4), doneAction:2);
	  Out.ar(outBus, SinOsc.ar(freq, mul:env)+noise);
	}).send(server);
	
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
	  Out.ar(outBus, chain);
	}).send(server);



  }
}