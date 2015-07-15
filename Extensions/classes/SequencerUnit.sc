// abstract class that effectively defines an interface for a
// sequencable unit. an implementing class might be a bassline player
// or an fx controller

SequencerUnit : Object {
  var
  // 1 if the unit is active, 0 if not
  >activationState=true,
  // an array of midi notes to which the unit responds
  >responderNotes,
  <responderNoteDesc = "override value of responderNoteDesc to describe note response",
  // an array of midi control numbers to which the unit responds
  >responderCtls,
  <responderCtlDesc = "override value of responderCtlDesc to describe ctl response",
  // midi channel to listen on, 0-15
  >responderChannel,
  noteOnResponder, 
  noteOffResponder, 
  ctlResponder, 
  // when the sequence is pulled from a particular key, what is the lookup 'key' for 
  // that musical key?
  >keyIndexes,
  // used to choose a scale in the given key
  >scaleIndexes, 
  // the name of a synthdef 
  >synthDef,
  >seqArrayWidth,
  >seqArrayLength, 
  // an array of integer multiplies used to define the range of notes
  >noteMultis, 
  outBusNo, // used for audio out
  inBusNo, // used for audio in
  ctlBusNo, // used as a control bus, e.g in sampler used to send loop length to synths
  fxBusNo, 
  ctlSeq, 
  ctlSeqPos = 0, 
  mode, // this is sent in as 'modeSwitch' to the constructor. It allows the object to operate in different modes
  vel_map;
  
  // respNoteArray - an array of notes to which this object responds
  // respCtlArray - an array of control numbers to which this object responds
  // respChannel - the midi channel to which this obect responds (0-15)
  // musicalKeys - the musical keys to play melodies in, ['c', 'fs'] - chosen at random when making a new sequence
  // musicalScales - musical sclaes to choose from at random when making new sequence ['dorian', ['nat_major']]
  //                 see MiscFuncs.printAvailableScales
  // synthType - name of a synthdef to use e.g. 'bassSynth'
  // plyphony - how many tracks does the sequence have? can be used to define polyphony
  // seqLength - how many steps does the sequence have?  
  // noteRange - an array of mutlipliers to get from middle octave to desired octave, e.g. put in [0.25, 0.125]
  //             and the notes in the sequence will be 2 or 3 octaves down from middle octave (440 etc)
  // outBus - audio out bus
  // fxBus - internal fxBus for routing from sound synth to fx synth. should be unique.
  // modeswitch - puts the object in different modes, handy extra parameter you can use for anything.
  *new{arg respNoteArray, respCtlArray, respChannel, musicalScales, musicalKeys, synthType, polyphony, seqLength, noteRange, outBus, fxBus, inBus, ctlBus, modeSwitch=0;
	^super.new.init(respNoteArray, respCtlArray, respChannel, musicalScales, musicalKeys, synthType, polyphony, seqLength, noteRange, outBus, fxBus, inBus, ctlBus, modeSwitch);
  }
  
  init{arg respNoteArray, respCtlArray, respChannel, musicalScales, musicalKeys, synthType, polyphony, seqLength, noteRange, outBus, fxBus, inBus, ctlBus, modeSwitch;
	var o, f;
	'seq parent obj init'.postln;
	this.sendSynthDefs;
	responderNotes = respNoteArray;
	responderCtls = respCtlArray;
	responderChannel = respChannel;
	keyIndexes = musicalKeys;
	scaleIndexes = musicalScales;
	synthDef = synthType;
	seqArrayWidth = polyphony;
	seqArrayLength = seqLength;
	noteMultis = noteRange;
	outBusNo = outBus;
	fxBusNo = fxBus;
	inBusNo = inBus;
	ctlBusNo = ctlBus;
	mode = modeSwitch;
	this.initialiseSequence;
	this.initialiseResponder;
	//noteOnResponder = NoteOnResponder;
	//noteOffResponder = NoteOffResponder;
	//ctlResponder = CCResponder;

	// put velocity on log scale!
	// (this is log10(127)
	o = 5;
	f = log(128+o) - log(o);
	vel_map = Array.fill(127, {arg i;log(i+o) - log(o) / f});
	vel_map.postln;
	vel_map = vel_map * 127;
  }
  
  initialiseCtlSeq{ arg width, length;
	// generate 2 d array for ctlSeq
	ctlSeq = Array.fill(width, 
	  {
		Array.fill(length, {127.0.rand});
	  });
  }

  // tell the unit to do something. It may respond one or more notes
  // e.g. go to the next position in the sequence
  trigger {arg note, vel;
	//[note, vel].postln;
	if (activationState==true, 
	  {this.triggerEvent(note, vel);}
	);
  }


  free{
	noteOnResponder.remove;
	noteOffResponder.remove;
	ctlResponder.remove;
  }
  // this method is called by init on object instantiation
  // after the notes, controls and channel have been stored
  // override it with midi reponder initialisation and such
  initialiseResponder{
	// make a midi note responder
	noteOnResponder = NoteOnResponder(
	  {
		|src, chan, num, vel| // another way to say arg src, chan, num, vel;
		this.trigger(num, vel);
	  },// end resp proc function
	  nil, 
	  responderChannel, 
	  responderNotes, 
	  nil);// end noteOnResp
  }

   
  // override these methods -------

  // this method is called by init on object instantiation
  // generates random data for the sequence for which this object is a wrapper
  // e.g. a random set of notes in the case of a note sequencer
  // the set of values generated might depend on the state of the controls
  // as set by setControl
  initialiseSequence{}

  
  // this method is called by init on object instantiation
  // override this method so it sends the appropriate synthdefs to the server
  sendSynthDefs{}

  // used to send control data into the object instance
  // it is up to the class to define the response to the data
  // this would be called by the CCResponder but can be called directly for testing
  // or sequencing purposes...
  // e.g. 
  setControl{ arg num, val;}
  
  // this gets called if activationState==1
  // and should be overidden in the child class
  triggerEvent{arg note, vel;}
  
  // end // override these methods -------
}


// skeleton example class
// base sequencer unit classes on this
// i.e. make sure you put something in the mothods

SequencerUnitSkeleton : SequencerUnit {
  var 
  <responderNoteDesc = "[0] -> trigger an event, [1] -> generate a new sequcne",
  <responderCtlDesc = "[0]",
  ctlArrayWidth = 4, // how many params in the ctl seq
  ctlArrayLength= 9; // how many steps in the ctl seq?

  // this should initialise all sequences that are used
  initialiseSequence{
	// either: if you have two sorts of sequence, one might have a prefixed length
	//this.initialiseCtlSeq(ctlArrayWidth, ctlArrayLength);
	// or: use the length sent to the constructor as seqLength
	this.initialiseCtlSeq(ctlArrayWidth, seqArrayLength);
  }

  setControl{ arg num, val;}

  triggerEvent{arg note, vel;}

  sendSynthDefs{}
}