MykMarkov : Object {

  var 
  // chains are simultaneously calculated with several orders, from 0->maxOrder
  maxOrder,
  // freqChain is a dictionary mapping lists of freqs to next possible freqs 
  <freqChain, 
  // beatChain is a dictionary mapping lists of beats to next possible beats 
  <beatChain, 
  // intChain is a dictionary mapping lists of intervals to next possible intervals 
  <intChain, 
  // pitch detector
  livePitch, filePitch, 
  freq_memory, beat_memory, int_memory, snake_freq_mem, snake_beat_mem, snake_int_mem, last_beat, last_freq, adding_freq, adding_beat, adding_int, playRout, onset_detect, 
  // train up the freq chain, the freq+beat chains, the freq, beat and interval chains or the beat chain using onsets
  <freq_mode=0, <beat_mode=1, <freq_beat_mode=2, <freq_beat_int_mode=3, <onset_mode=4;
  
  *new{arg maxOrder = 10;
	^super.newCopyArgs(maxOrder).prInit;
  }

  prInit{
	this.sendSynthDefs();
	freqChain = Dictionary.new;
	beatChain = Dictionary.new;
	intChain = Dictionary.new;
	// this makes sure the synthdefs are ready on the fiddle object
	livePitch = MykFiddle.new({arg freq; this.addFreq(freq)});
	// this makes sure the synthdefs are ready on the bonk
	onset_detect = MykBonk.new(osc_id:305, callback:{this.addBeat(0.125);});	
	filePitch = "";
	// set up the memories
	freq_memory = [];//Array.fill(maxOrder, {1});
	beat_memory = [];//Array.fill(maxOrder, {0.5});
	snake_freq_mem = [];//Array.fill(maxOrder, {1});
	snake_beat_mem = [];//Array.fill(maxOrder, {0.5});
	adding_freq = false;
	adding_beat = false;
	adding_int = false;

	last_freq = 60;
	("MYKMarkov: creating chains from zero up to "++maxOrder++" order. ").postln
  }
  
  trainRandomFreqs{
	freqChain = Dictionary.new;
	100.do{
	  freqChain.put(Array.fill(rrand(1, 6), {1000.0.rand}), Array.fill(rrand(1, 5), {1000.0.rand}));
	};
	
  }

  // loads and plays the sent audio file, capturing a list of notes from it
  // then processes this into a series of markov chains, from order 0 - > order (note count)
  trainFromAudioFile{arg filename;
	var player;
	
	//fiddle.run;
	
  }


  // as trainFreqsFromAudioFile but use a live input
  trainFromLiveInput{arg input = 0, mode=freq_beat_mode, min_beat=0.125;
	last_beat = thisThread.seconds;
	this.stopTraining;
	switch (mode, 
	  freq_mode, {
		"MykMarkov:training on freqs. Get out of the tub you freq! (HST)".postln;
		livePitch = MykFiddle.new({arg freq; this.addFreq(freq)});
		livePitch.run;
	  },
	  beat_mode, {
		"MykMarkov:training on beats. Beat on the brat with a baseball bat! (The Ramones)".postln;
		livePitch = MykFiddle.new({arg freq; this.addBeat(min_beat)});
		livePitch.run;
	  }, 	  
	  freq_beat_mode,  {
		"MykMarkov:training on beats and freqs. It's getting a beat freaky in here.".postln;
		livePitch = MykFiddle.new({arg freq; this.addFreq(freq);this.addBeat(min_beat)});
		livePitch.run;
	  },
	  freq_beat_int_mode, {
		//"MykMarkov:can't do freq intervals yet.".postln;
		"MykMarkov:training on beats, freqs and intervals..".postln;
		livePitch = MykFiddle.new({arg freq; this.addFreq(freq);this.addBeat(min_beat);this.addInterval(freq)});
		livePitch.run;
	  }, 
	  onset_mode, {
		"MykMarkov:training on onset intervals..".postln;
		onset_detect.free;
		onset_detect = MykBonk.new(osc_id:305, callback:{this.addBeat(min_beat);});	
		onset_detect.run;
	  }
	);
  }

  stopTraining{
	this.free;
  }
  
  free{
	livePitch.free;
	onset_detect.free;
  }

  
  // internal func that is called when a frequency is detected
  // basically adds the freq to all the chains (0-arbitrary state)
  addFreq{arg freq;
	var updated;
	// only add the freq if we are not already processing a freq (synchronized mode)
	if (adding_freq == false, {
	  adding_freq = true;
	  // note that we can't pass in the class fields by reference apparently??
	  // so we update the fields then return the updated versions, then store the updated versions
	  updated = this.updateChain(freq, freqChain, freq_memory);
	  freqChain = updated[0];
	  freq_memory = updated[1];
	  adding_freq = false;
	});	
  }

  // internal func that is called when an beat is detected
  addBeat{arg min_beat;
	var beat, beat_length, updated;//, prev_steps, next_steps;
	beat = thisThread.seconds;
	beat_length = beat - last_beat;
	// ignore too short beats
	if (beat_length < min_beat, {//"Beat Too short".postln;
	  ^nil;});
	// quantize
	beat_length = beat_length - (beat_length % min_beat);
	if (adding_beat == false, {
	  adding_beat = true;
	  updated = this.updateChain(beat_length, beatChain, beat_memory);
	  beatChain = updated[0];
	  beat_memory = updated[1];
	  last_beat = beat;
	  adding_beat = false;
	});
  }


  // internal func that is called when a frequency is detected
  // basically adds the freq to all the chains (0-arbitrary state)
  addInterval{arg freq;
	var updated, interval;
	if (adding_int == false, {
	  adding_int = true;
	  // calculate the interval
	  interval = last_freq - freq;
	  if (interval < 0, {interval = (interval % 12) - 12});
	  if (interval > 0, {interval = interval % 12});
	  updated = this.updateChain(interval, intChain, int_memory);
	  intChain = updated[0];
	  int_memory = updated[1];
	  last_freq = freq;
	  adding_int = false;
	});	
  }

  // general internal function which updates the sent chain with the sent value based on the sent memory state
  
  updateChain{arg value, chain, memory;
	var next_steps;
	//("updateChain called on memory "++memory++" \n chain "++chain).postln;
	// do nothing but store the value in the momory 
	// if we have no previous state information
	if (memory.size == 0, {memory = [value];}, {
	// now create a new state transition for all the available memory lengths
	memory.size.do{arg i;
	  var prev_steps;
	  // grab the i+1 last steps
	  prev_steps = memory.copyRange(0, i);
	  // see if there is something registered against this sequence
	  next_steps = chain.at(prev_steps);
	  if (next_steps == nil, 
		{// nothing registered - register a new key+value
		  chain.put(prev_steps, [value]);},
		{// something there - append to the existing key's value
		  chain.put(prev_steps, next_steps.add(value));
		})
	};
	// now remember this frequency.
	// are we bootstrapping the freq memory towardds maxOrder?
	if (memory.size < maxOrder, 
	  {// yes - just add it to the end
		memory = [value].addAll(memory);},
	  {// no - rotate it and replace the oldest 
		memory = memory.rotate(1);		  
		memory[0] = value;
	  }
	);
	});
	// now send back the updated stuff (no pass by reference apparently)
	^[chain, memory];
  }
  
  loadFreqs{arg filename;
	freqChain = this.loadDict(filename);
  }
  
  saveFreqs{arg filename;
	this.saveDict(filename, freqChain);
  }

  loadBeats{arg filename;	
	beatChain = this.loadDict(filename);
  }

  loadDict{arg filename;
	var dict, data, key, value;
	// reset the dict
	dict = Dictionary.new;
	data = FileReader.read(filename, true, delimiter:"\n");
	// get the raw parts
	data.do{arg line;
	  line = line.asString.split($=);
	  // remove the double brackets at start and end
	  key = line[0].replace("[ [", "[").asArray;
	  value = line[1].replace("] ]", "]").asArray;
	  key = thisProcess.interpreter.interpret(key);
	  value = thisProcess.interpreter.interpret(value);
	  dict.put(key, value);
	};
	^dict;
  }
  
  saveDict{arg filename, dict;
	var file;
	// save a dict to disk
	file = File.new(filename, "w");
	dict.keys.do{arg i;
	  var str;
	  str = (""++i++"="++dict.at(i)++"\n");
	  file.write(str);
	};
	file.close;	
	^dict;
  }


  nextFreq{
	var ind, key, value, freq, updated;
	// querying chain updates and returns the memory
	snake_freq_mem = this.queryChain(freqChain, snake_freq_mem); 
	^snake_freq_mem[0];
  }

  nextBeat{
	var ind, key, value, beat;
	snake_beat_mem = this.queryChain(beatChain, snake_beat_mem);
	^snake_beat_mem[0];
  }

  nextInterval{
	var ind, key, value, interval;
	snake_int_mem = this.queryChain(intChain, snake_int_mem);
	^snake_int_mem[0];
  }
  
  // general purpose internal function that queries the sent chain using the sent memory state
  // including updating the memory state

  queryChain{arg chain, memory;
	var ind, key, value;
	if (chain.size == 0, {"MykMarkov - error - no chain yet! ".postln;^[nil]});
	// check the memory has been initialised
	if (memory.size == 0, {
	  memory = Array.fill(maxOrder, {chain.asArray.flatten.choose});
	});
	// we search the chain using different orders until we find > 1 option. 
	block{|break|
	  maxOrder.do{arg i;
		ind = memory.size - i - 1;
		key = memory.copyRange(0, ind);
		// query the chain:
		value = chain.at(key);
		//("MykMarkov - key "++key++" value "++value).postln;
		if ((value != nil) && (value.size > 1), {//("MykMarkov - found > 1 option at order "++(maxOrder - i)).postln;
		  break.value(1)});
	  };
	};
	// lowest order only gave us one option:
	if (value != nil, {//("MykMarkov - found "++value.size++" options from the chain").postln; 
	  value = value.choose;});
	// oh dear, didn't find anything, return from the chain at random
	if (value == nil, {//"MykMarkov - using order 0 ".postln; 
	  value = chain.asArray.flatten.choose;});
	// add the value to the snake memory and rotate
	memory = memory.rotate(1);
	memory[0] = value;
	^memory;	
  }


  // returns the next frequency based on the sent previous freqs
  // using the highest possible order. 
  // will just chop out the first few freqs in the case where the length of prev_freqs > maxOrder
  // 
  getNextFreq{arg prev_freqs;
	var value, key, ind;
	if (freqChain.size == 0, {^prev_freqs[0]});
	if (prev_freqs.size > maxOrder, {prev_freqs = prev_freqs.copyRange(0, maxOrder-1)});
	// starting at the highest order chain, try to find a value for the key 'prev_freqs' from the chain
	prev_freqs.size.do{ arg i;
	  ind = prev_freqs.size - i - 1;
	  key = prev_freqs.copyRange(0, ind);
	  //("Trying to do a lookup with "++key).postln;
	  value = freqChain.at(key);
	  if (value != nil, {^value.choose});
	};
	// if we get here, we didn't find anything for even the first of the previous freqs
	// so try to find something from the chain at random
	if (value == nil, {//"MYKMarkov - no matches for that state. returning at random".postln;
	  //^freqChain.choose.choose
	  ^freqChain.asArray.flatten.choose;
	});
  }

  getNextBeat{arg prev_beats;
	var value, key, ind;
	if (beatChain.size == 0, {^prev_beats[0]});
	if (prev_beats.size > maxOrder, {prev_beats = prev_beats.copyRange(0, maxOrder-1)});
	// starting at the highest order chain, try to find a value for the key 'prev_beats' from the chain
	prev_beats.size.do{ arg i;
	  ind = prev_beats.size - i - 1;
	  key = prev_beats.copyRange(0, ind);
	  //("Trying to do a lookup with "++key).postln;
	  value = beatChain.at(key);
	  if (value != nil, {^value.choose});
	};
	// if we get here, we didn't find anything for even the first of the previous beats
	// so try to find something from the chain at random
	if (value == nil, {//"MYKMarkov - no matches for that state. returning at random".postln;
	  //^beatChain.choose.choose
	  ^beatChain.asArray.flatten.choose;
	});
  }

  // returns a loop of the requested length
  // should be played from index 0 - the end.
  
  getLoop{arg length, mode=freq_mode;
	var temp_freq_mem, temp_beat_mem, loop, freq, beat;
	
	loop = [];
	switch(mode, 
	  freq_mode, {
		temp_freq_mem = Array.fill(maxOrder, {freqChain.asArray.flatten.choose});
		length.do{
		  freq = this.getNextFreq(temp_freq_mem);
		  loop = loop.add(freq);
		  temp_freq_mem = temp_freq_mem.rotate(1);
		  temp_freq_mem[0] = freq;
		};
	  }, 
	  beat_mode, {
		temp_beat_mem = Array.fill(maxOrder, {beatChain.asArray.flatten.choose});
		length.do{
		  beat = this.getNextBeat(temp_beat_mem);
		  loop = loop.add(beat);
		  temp_beat_mem = temp_beat_mem.rotate(1);
		  temp_beat_mem[0] = beat;
		};
	  },  
	  freq_beat_mode, {
		temp_freq_mem = Array.fill(maxOrder, {freqChain.asArray.flatten.choose});
		temp_beat_mem = Array.fill(maxOrder, {beatChain.asArray.flatten.choose});
		length.do{
		  beat = this.getNextBeat(temp_beat_mem);
		  freq = this.getNextFreq(temp_freq_mem);
		  loop = loop.add(freq, beat);
		  temp_beat_mem = temp_beat_mem.rotate(1);
		  temp_beat_mem[0] = beat;
		  temp_freq_mem = temp_freq_mem.rotate(1);
		  temp_freq_mem[0] = freq;
		};
	  } 
	);
	^loop;
  }
  
  sendSynthDefs{
	var server;
	server = Server.local;
	
  }
  
  //d.keys.do{arg i;("key: "++i++" has value "++d.at(i)).postln};

}