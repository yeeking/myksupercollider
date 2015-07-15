
// this object wraps up the functions
// and synth defs for a drum sampler
// which can load a set of samplers into buffers
// then trigger and stop them inn response to incoming midi data

// the idea is that you initialise this object with a directoy, midi channel and a start note number
// and it loads all the samples in that directory into buffers then responds to the incoming midi data
// as appropriate

DrumSamplerPoly : Object {

  var
  fileArray,
  bufferArray, 
  noteOnResponder,
  noteOffResponder;
  
  // class method, constructor
  // sampleDir - a directory containing samples that will all be loaded into buffers
  // channel - midi channel
  // startNote - samples are mapped to midi notes starting at this note
  
  *new{ arg sampleDir, channel=0, startNote=0, noteMap=1, outBus=0;
	^super.new.init(sampleDir, channel, startNote, noteMap, outBus);
  }

  // instance intialisation method called by *new
  
  init { arg sampleDir, channel, startNote, noteMap, outBus;
	// for testing
	//var fileArray, bufferArray, noteArray, sampleDir = "/home/matthew/Audio/data/sophie/docklands/drum_sounds/", responder, server, fileCount, startNote = 48, channel = 0;;
	// for compilation
	var server, fileArray, noteArray, bufferArray;

	server=Server.local;
	this.sendSynthDefs;

	// get the audio files in the directory as an array	
	fileArray = (sampleDir++"*.wav").pathMatch;

	// iterate the array and for each file, create a buffer
	// and store it in the buffer array
	bufferArray = Array.fill(fileArray.size, {arg i;
	  //i;
	  ("loading file "++fileArray[i]).postln;
	  Buffer.read(server,fileArray.at(i));
	}
	);
	("drum sampler loaded "++bufferArray.size++" files").postln;

	// set up a midi note on responder 
	// when a note comes in, creata a sample player synth
	// for the appropriate buffer

	// first make an array of the notes to respond to
	// (must be a neater way to do this... )
	if (noteMap==0, 
	  {
		// generate a contiguous note list
		noteArray = Array.fill (bufferArray.size, {arg i;
		  startNote+i;}
		);
	  }, 
	  {
		// use the mpd map
		noteArray = this.getMPDNoteArray(bufferArray.size);
	  }
	);// end if on note map  

	noteOnResponder = NoteOnResponder(
	  { |src, chan, num, vel| // another way to say arg src, chan, num, vel;
		// do this if the incoming data matches the spec below
		//num.post;
		//" matches my array spec".postln;
		// reset the veocity bus??
		//server.sendMsg("/c_set", MiscFuncs.chanNoteOffToBus(channel), 0);
		Synth("oneshotPoly", [
		  \bufNum, bufferArray[noteArray.indexOf(num)].bufnum, 
		  \outBus, outBus, \vel, vel]);
	  }, 
	  nil,                              // accept values from any midi source
	  channel,                                 //[2, 7], // accept channels bewteen 2 and 7
	  noteArray,		// could also be { |num| num.even } or _.even
	  nil);                             //{ |vel| vel > 50 });
	
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef(
	  "oneshotPoly", {arg bufNum, outBus, vel;
		var playBuf, midiTo1;
		midiTo1 = 1/127*vel;
		playBuf = PlayBuf.ar(1,bufNum, loop:0);
		FreeSelfWhenDone.kr(playBuf);
		Out.ar(outBus, playBuf*midiTo1);
	  }
	).send(server);
  }

  // free the buffers and the note responders

  free{
	// iterate the buffer array 
	// freeing each one
	bufferArray.do({arg i;
	  i.free;
	});
	noteOnResponder.remove;
  }

  // generates an array of notes suitable for
  // the mpd16

  getMPDNoteArray{ arg count = 16;
	var notes, mpd;
	mpd = [49, 55, 51, 53,
	  48, 47, 45, 43,
	  40, 38, 46, 44,
	  37, 36, 42, 82];
	
	if (count>16, {count = 16});

	notes = Array.fill(count, {arg i;
	  mpd[i];
	});
	
	^notes;
	
  }

}