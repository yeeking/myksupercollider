// an object to contain some handy functions

MiscFuncs : Object {

  *findClosestFreq{arg freq, haystack;
	var smallestDiff=1000, closestFreq, diff=0; 
	//var closest;
	// to find a matching freq
	haystack.size.do{arg i;
	  // how close is the incoming freq to the one in the array?
	  diff = haystack.at(i)-freq;
	  // make it +ve
	  diff = diff*diff;
	  diff = diff.sqrt;
	  if (diff < smallestDiff, 
		{smallestDiff=diff;
		  closestFreq = haystack.at(i);}
	  );
	};
	freq = closestFreq;
	^freq;
  }

  // returns an array of frequency values that represent the requested scale
  // e.g. getScale('as', 'dorian'); returns the frequences that represent an a# dorian scale
  
  *getScale{ arg key, type;
	var intervals, startIndex, chromatic, scale, currentPos;
	// check for reandom
	if (key=='random',  
	  {^Array.fill(12, {1000.0.rand})}
	);
	
	// get the intervals, i.e. an array of semitone counts between notes in this scale
	intervals = this.getScaleIntervals.matchAt(type);
	// now generate the scale
	chromatic = this.getChromaticFreqs;
	// get the start index in the chromatic scale, i.e. the position of the first note in the scale
	currentPos = this.getKeyLookupDict.matchAt(key);
	
	scale = Array.fill(intervals.size, {arg i;
	  var note;
	  currentPos;
	  // pull the freq for the current note
	  note = chromatic.wrapAt(currentPos);
	  // move to the next note for next time
	  currentPos = currentPos + intervals.at(i);
	  note;
	});
	^scale;
  }
  


  // returns a dictionary of the intervals for all the scales it knows about
  // you can then pull the intervals from the dict for a given scale
  // intervals.matchAt('dorian'); -> [2, 1, 2, 2, 2, 1, 2]

  *getScaleIntervals{
	var intervals;
	intervals = Dictionary[
	  ['nat_major', 0] -> [2, 2, 1, 2, 2, 2, 1],  
	  ['nat_minor', 1] -> [2, 1, 2, 2, 1, 2, 2],  
	  ['harm_minor', 2] -> [2, 1, 2, 2, 1, 2, 1],  
	  ['mel_minor', 3] -> [2, 1, 2, 2, 2, 2, 1],  
	  ['pent_major', 4] -> [2, 3, 2, 2, 3],  
	  ['pent_blues', 5] -> [3, 2, 1, 1, 3],  
	  ['dorian', 6] -> [2, 1, 2, 2, 2, 1, 2],  
	  ['romanian_minor', 7] -> [2, 1, 3, 1, 2, 1, 2],  
	  ['aug_dim_blues', 8] -> [1, 2, 1, 2, 1, 2, 1, 2],  
	  ['blues', 9] -> [3, 2, 1, 1, 3, 2],
	  ['suling', 10] -> [2, 1, 4, 1, 4],
	  ['tone_row', 11] -> Array.fill(12, {12.rand+1}), 
	  ['chromatic', 12] -> Array.fill(12, {arg i; i}), 
	  ['baris', 13] -> [4, 1,2, 4], 
	  ['random', 14] -> Array.fill(12.rand, {arg i; 5.rand}), 
	  ['really_random', 15] -> Array.fill(12.rand, {arg i; 5.0.rand})
	];
	^intervals;
  }

  // prints the available scales out to the console. relates to the getScaleIntervals function
  *printAvailableScales{
	this.availableScales.postln;
  }

  *availableScales{
    ^['nat_major','nat_minor','harm_minor','mel_minor','pent_major','pent_blues','dorian','romanian_minor','aug_dim_blues','blues', 'suling', 'chromatic', 'tone_row', 'really_random'];
  }

  *availableKeys{
	^['a','as','b','c','cs','d','ds','e','f','fs','g','gs','tone_row'];
  }
  
  // retuns a Dict that maps key names, e.g. 'a_s' (a #)  to starting indexes in the 
  // array returned by getNoteFreqArray. e.g. 'a' returns '0' as A is the first freq in the array

  *getKeyLookupDict{
	^Dictionary[
	  'a' ->0, 
	  'as'->1, 
	  'b'->2, 
	  'c'->3, 
	  'cs'->4, 
	  'd'->5, 
	  'ds'->6, 
	  'e'->7, 
	  'f'->8, 
	  'fs'->9, 
	  'g'->10, 
	  'gs'->11,
	  'tone_row'->12.rand
	];
  }

  // generates an array of the freqs for the chromatic scale

  *getChromaticFreqs{
	var notes;
	notes = [440.00, 466.16,493.88,523.35,554.37,587.33,622.25,659.26,698.46,739.99,783.99,830.61];
	^notes;
  }
  
  // returns a dictionary mapping note names to frequency values using equal temperament tuning
  // note names are: n_a, n_as, n_b and so on, all sharps no flats
  *getNoteToFreqMap {
	var noteMap;
	noteMap = Dictionary[
	  ['n_a',0] ->440.00,
	  ['n_as',1]->466.16,
	  ['n_b',2]->493.88, 
	  ['n_c',3]->523.35,
	  ['n_cs',4]->554.37, 
	  ['n_d',5]->587.33, 
	  ['n_ds',6]->622.25,
	  ['n_e',7]->659.26, 
	  ['n_f',8]->698.46,
	  ['n_fs',9]->739.99, 
	  ['n_g',10]->783.99,
	  ['n_gs',11]->830.61
	];
	
	^noteMap;
  }

  // returns a dictionary mapping key names to arrays of notes (as freqs) that make up that key
  *getKeyToNoteArrayMap{
	var keyMap, notes;	
	notes = this.getNoteToFreqMap;
	keyMap = Dictionary[
	  ['random', 0] ->Array.fill(12, {notes.matchAt(12.rand)}), 
	  ['f_sharp', 1] -> [notes.matchAt('n_a'), notes.matchAt('n_b'), notes.matchAt('n_cs'), notes.matchAt('n_d'), notes.matchAt('n_e'), notes.matchAt('n_fs'), notes.matchAt('n_gs')], 
	  ['c_major', 2] -> [notes.matchAt('n_c'), notes.matchAt('n_d'), notes.matchAt('n_e'), notes.matchAt('n_f'), notes.matchAt('n_g'), notes.matchAt('n_a'), notes.matchAt('n_b')]
	];
	^keyMap;
  }

  // returns an array describing the pad-note mappings for my v-drums

  *getVDrumNotes {
	var pads;
	pads = Dictionary [
	  'BD'->36, 
	  'SD'->38, 
	  'SD RIM'->40,
	  'OH EDGE'->26, 
	  'OH MIDDLE'->46, 
	  'CH EDGE'->22, 
	  'CH MIDDLE'->42, 
	  'HH CTL'->44, 
	  'TOM 1'->48, 
	  'TOM 2'->45, 
	  'CY EDGE'->32, 
	  'CY TOP'->51, 
	  'CY BELL'->53
	];
	^pads;
  }

  // returns an array containing all the note numbers on
  // the mpd pad

  *getMPDNoteArray{ arg start=0, end = 16, bank=0;
	var notes, mpd, count;
	count = end-start;
	if (bank==0, 
	  {mpd = [49, 55, 51, 53,
		48, 47, 45, 43,
		42, 44, 46, 34,
		36, 37, 38, 40];}, 
	  {mpd = [73, 74, 71, 39, 
		56, 62, 63, 64, 
		65, 66, 76, 77, 
		54, 69, 81, 80];}
	);
	if (count>16, {count = 16});
	
	notes = Array.fill(count, {arg i;
	  mpd[i+start];
	});
	
	^notes;
  }

  //returns a dictionary which maps pad number to midi note
  // where pads go 0-15, starting top left
  // e.g. 0->49
  
  *getMPDNoteDict{ arg start=0, end = 16, bank=0;
	var notes, mpd, count;
	
	count = end-start;
	
	if (bank==0, 
	  {mpd = [49, 55, 51, 53,
		48, 47, 45, 43,
		40, 38, 46, 44,
		37, 36, 42, 82];}, 
	  {mpd = [73, 74, 71, 39, 
		56, 62, 63, 64, 
		65, 66, 76, 77, 
		54, 69, 81, 80];}
	);

	if (count>16, {count = 16});
	
	notes = Dictionary.new;
	
	count.do{arg i;
	  notes.add(mpd[i+start] -> i);
	};
	
	^notes;
  }
  
  

  // returns note on bus for the sent midi channel

  *chanNoteOnToBus{ arg chan;
	^chan * 40;
  }

  // returns velocity bus for the sent midi channel

  *chanVelToBus{ arg chan;
	^chan * 40 + 1;
  }

  // this class method returns the appropriate note off bus
  // for the sent note on the sent channel

  *chanNoteOffToBus{ arg chan;
	^chan * 40 + 2;
  }
  
  // this class method returns the control bus that should
  // be used for the sent controller on the sent midi channel
  // chan is midi channel, num is the controller number, starting at 0
  *chanCtlToBus {arg  chan, num;
	^chan * 40 + num + 3;
  }
}