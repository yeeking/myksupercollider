// this object provides utility methods for livecoding with the MPD type controllers.
// 

MPD : Object {

  var channel, 
  <notes,ccs,  
  noteOnResp, noteOffResp, ccResp, 
  noteOnCBs, noteOffCBs, ccCBs;
  
  *new{arg channel=0, lpd_mode=0;
	^super.newCopyArgs(channel).prInit(lpd_mode);
  }

  prInit{arg lpd_mode=0;
	// pad 1 on the mpd is the bottom right pad...
	if (lpd_mode == 0, {
	  notes = [49, 55, 51, 53, 
		48, 47, 45, 43,
		42, 44, 46, 34,
		36, 37, 38, 40];
	}, 
	  {
		notes = [42, 44, 46,34,36,37,38,40]
	  });
	
	// my mpd has ccs 1-16 with a couple skipped...
	//ccs = Array.fill(16, {arg i;i+1});
	ccs = [1, 2, 3, 4, 5, 6, 7,8, 9, 10, 11, 12, 13, 14, 15];
	// an array of functions that are called when a note on occurs. 
	//one entry for each pad 
	noteOnCBs = Array.fill(notes.size, {{}});
	noteOffCBs = Array.fill(notes.size, {{}});
	ccCBs = Array.fill(ccs.size, {{arg val;}});
	
	this.setupResponders;
  }
  
  setupResponders{
	noteOnResp = NoteOnResponder({|src,chan,note,vel|
	  //"note on responder triggered".postln;
	  //[src, chan, note, vel].postln;
	  this.noteOn(note, vel);
	},  
	  nil, 
	  channel, 
	  notes,
	  nil);
	
	noteOffResp = NoteOffResponder({|src,chan,note,vel|
	  //"note off responder triggered".postln;
	  //[src, chan, note, vel].postln;
	  this.noteOff(note, vel);
	},  
	  nil, 
	  channel, 
	  notes,
	  nil);
	
	ccResp = CCResponder({|src,chan,num,val|
	  //"cc responder triggered".postln;
	  //[src, chan, num, val].postln;
	  this.cc(num, val);
	}, 
	  nil, 
	  channel, 
	  ccs, 
	  nil);
	
  }
  
  free{
	noteOnResp.remove;
	noteOffResp.remove;
	ccResp.remove;
  }

  noteOn{arg num, vel;
	noteOnCBs[notes.indexOf(num)].value(vel);
  }
  
  noteOff{arg num, vel;
	noteOffCBs[notes.indexOf(num)].value(vel);
  }

  cc{arg num, val;
	ccCBs[ccs.indexOf(num)].value(val);
  }
  
  // pads are '1' indexed, as written on the mpd itself
  onNoteOn{ arg pad, callback;
	noteOnCBs[pad -1] = callback;
  }
  
  onNoteOff{ arg pad, callback;
	noteOffCBs[pad-1] = callback;
  }

  onCC{arg cc_no, callback;
	ccCBs[ccs.indexOf(cc_no)] = callback;
  }
}