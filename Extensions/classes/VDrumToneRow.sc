// object that generates midi notes from a given key in response to 
// vdrum playing
// or plays note on a synth 
// different things can happen depending on the incoming note

// 1. playNotes - pick a note from the current key and play it vel maps to amp (
//                or inversely to octave shift??)
// 2. changeKeyNotes - change to the next key
// 3. noteProbNotes - change note probability - vel maps to note probability
//                           

VDrumToneRow : Object {

  var 
  noteOnResponder,
  playNotes, 
  changeKeyNotes, 
  noteProbNotes,
  // an array of arrays
  // each sub array contains notes in a key
  keyNotes,
  currentKeyIndex,
  toneRow, 
  toneRowPos;

  *new{ arg channel, keyIndex;
	^super.new.init(channel, keyIndex);
  }

  init { arg channel, keyIndex;
	this.sendSynthDefs;
	this.setupNoteArrays;
	// set up the note respomder
	this.setupMidiResponders(channel);
	// choose an initial key - this could use a creation argument
	currentKeyIndex = keyIndex; // start in f sharp 
	// now generate a tone row in this key
	this.generateToneRow;
	// set the tone row position to the start
	toneRowPos = 0;
  }

  setupMidiResponders{ arg channel;
	noteOnResponder = NoteOnResponder(
	  { |src, chan, num, vel|
		// choose the event to trigger
		if ( playNotes.indexOf(num)!=nil,
		  {
			// incoming note is in the playNotes array
			'going to generate a note'.postln;
			this.playNote(vel);
		  },
		  // incoming note not in playNotes
		  {
			if ( changeKeyNotes.indexOf(num)!=nil,
			  {
				// incoming note is in the changekey array
				'going to change key'.postln;
				this.changeRow;
			  },
			  {
				if ( noteProbNotes.indexOf(num)!=nil, 
				  {
					// incoming note is in the noteProb array
					'going to change note probability by velocity'.postln;
				  }
				)
			  }
			)		  }
		);
		
	  }, 
	  nil, // any midi id
	  channel, 
	  this.getAllResponseNotes,
	  nil);// all velocities
	
  }

  finnsInversion{
	var intervals, positions;
	// reverse the order of transpositions
	//
	"toneRow:".postln;
	toneRow.postln;
	intervals = Array.new(11);
	11.do{ arg i;
	  intervals.add (toneRow[i+1]-toneRow[i]);
	};
	"intervals".postln;
	intervals = intervals.reverse;
	positions = Array.fill(12, {arg i; i+1;});
	intervals.postln;	
	11.do{ arg i;
	  // new position = toneRow[0]+
	  toneRow[i+1] = positions.wrapAt(intervals[i]+toneRow[0]);
	};
	"new toneRow".postln;
	toneRow.postln;
  }

  // pick a note from the current key
  // choose an octave shift in an inverse relationship 
  // to the velocity (i.e. high velocity, low notes)
  // other parameters?? 

  playNote{ arg vel;
	var freq, oct;
	// basic version - just pick a note from the current key and play it
	//freq = keyNotes[currentKeyIndex].choose;
	// tone row version
	freq = keyNotes[currentKeyIndex].wrapAt(toneRow.wrapAt(toneRowPos)-1);
	//freq = toneRow.wrapAt(toneRowPos);
	toneRowPos = toneRowPos+1;
	// now choose an octave transposition
	//oct = [0.5, 2].choose;
	oct = 1;
	//	("chose a freq: "++freq).postln;
	//("got a vel: "++vel).postln;
	freq = freq*oct;
	Synth("simpleFormant", [\frequency, freq, \attackTime, 0.5.rand, \vel, vel*0.1, \start, 10000.0.rand, \end, 2000.0.rand]);
	Synth("klanks", [\frequency, freq, \outBus, 0, \attackTime, 0.3.rand, \vel, vel*0.5]);
	
  }

  changeRow{
	var mode;
	mode = 2.rand;
	switch (mode, 
	  0, {
		"mirror".postln;
		this.finnsInversion;
	  }, 
	  1, {
		"reverse".postln;
		toneRow = toneRow.reverse;
	  }
	);
  }
  
  changeKey{
	// 
  }

  changeProb{
  }

  // generates a 12 tone row in the current key 

  generateToneRow{
	var size;
	//toneRow = Array.fill(12, {keyNotes[currentKeyIndex].choose;});
	// tone row should not contain frequencies
	// but indexes in the master note array
	//size = keyNotes[currentKeyIndex].size;
	toneRow = Array.fill(12, {arg i; i+1;}).scramble;
	toneRow.postln;
  }

  // possible events in response to incoming midi

  // get an array containing all the midi response notes
  // essentially a concatenation of all the arrays generated
  // in setupNoteMaps

  getAllResponseNotes {
	'getting midi response notes...'.postln;
	//	(playNotes++changeKeyNotes++noteProbNotes).postln;
	^playNotes++changeKeyNotes++noteProbNotes;
  }

  // generates the arrays of notes to which we should respond
  // and the key arrays
  setupNoteArrays {
	var 
	n_a=440.00,
	n_as=466.16,
	n_b=493.88, 
	n_c=523.35,
	n_cs=554.37, 
	n_d=587.33, 
	n_ds=622.25,
	n_e=659.26, 
	n_f=698.46,
	n_fs=739.99, 
	n_g=783.99,
	n_gs=830.61;

	playNotes = MiscFuncs.getMPDNoteArray(12, 16);
	changeKeyNotes = MiscFuncs.getMPDNoteArray(8, 12);
	noteProbNotes = [6, 7];
	'setupNoteArrays'.postln;
	(playNotes++changeKeyNotes++noteProbNotes).postln;
	
	// define the array of keys
	// (or tone rows??)
	keyNotes = [
	  [n_a, n_b, n_e], // 
	  [n_a,n_as,n_b,n_c,n_cs,n_d,n_ds,n_e,n_f,n_fs,n_g,n_gs], // tone row 
	  [n_a, n_b, n_cs, n_d, n_e, n_fs, n_gs], 	  // f sharp
	  [n_c, n_d, n_e, n_f, n_g, n_a, n_b]
	];

  }
  
  sendSynthDefs{
	var server = Server.local;

	SynthDef("klanks", { arg frequency=220, outBus=0, attackTime=0.01, sustainTime=0.2, releaseTime=0.5, vel=127;
	  var klank, n, harm, amp, ring, env, level, midiTo1;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  n = 9;
	  env = EnvGen.kr(
		Env.new([0, 1, 0.5, 0], [attackTime.rand, sustainTime.rand, releaseTime.rand], 'welch'), levelScale: level, doneAction:2);

	  // harmonics
	  //harm = Control.names([\harm]).ir(Array.series(4,1,1).postln);
	  harm = [1,2,3.5];
	  // amplitudes
	  //amp = Control.names([\amp]).ir(Array.fill(4,0.05));
	  amp = [0.04, 0.03, 0.02];
	  // ring times
	  ring = [1, 1, 1];
	  //ring = Control.names([\ring]).ir(Array.fill(4,1));
	  klank = Klank.ar(`[harm,amp,ring], {ClipNoise.ar(0.003)}.dup, frequency);
	  Out.ar(outBus, klank*env*vel);
	}).send(server);

	SynthDef(
	  "simpleNoise", {arg frequency=220, outBus=0, attackTime=0.01, sustainTime=0.2, releaseTime=0.5, vel;
		var chain, midiTo1, level, env;
		midiTo1 = 1/127;
		level = vel*midiTo1;
		env = EnvGen.kr(
		  Env.new([0, 1, 0.5, 0], [attackTime, sustainTime, releaseTime], 'linear'), levelScale: level, doneAction:2);
		chain = Resonz.ar(ClipNoise.ar(1.0), [frequency, frequency*0.5], 0.0003, mul:50)*env;
		Out.ar(outBus, chain);
	  }
	).send(server);

	SynthDef(
	  "simplePulse", { arg frequency=220, outBus=1, attackTime=0.01, sustainTime=0.1, releaseTime=0.2;
		var chain, env;
		env = EnvGen.kr(
		  Env.new([0, 1, 0.5, 0], [attackTime, sustainTime, releaseTime], 'linear'), levelScale: 0.05, doneAction:2);
		chain = Pulse.ar(frequency)*env;
		Out.ar([0,1], chain);
	  }
	).send(server);
	
	SynthDef("simpleFormant", {arg frequency=220, outBus=1, attackTime=0.01, sustainTime=0.1, releaseTime=0.2, vel, start, end;
	  var sound, midiTo1, level, env;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  env = EnvGen.ar(                                                                                  		Env.new([0, 1, 0.2, 0], [attackTime, 0.1*level+0.1, 0.5*level], 'welch'), levelScale:level+0.2*0.8, doneAction:2);
	  sound = Formant.ar([frequency*2, frequency*0.5], XLine.kr(start, end, 0.5), XLine.kr(end, start, 0.1), 0.125);
	  //sound = SinOsc.ar([frequency, frequency*0.5]);
	  Out.ar(0, sound*env);
	  
	}).send(server); 

  }


  
  free {
	noteOnResponder.remove;
  }

  
}