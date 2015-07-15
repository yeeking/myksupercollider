// classes for my new improviser - 'The Chopper'

// Midi controllable chopper
// in the style of a looping sampler

MidiChopper : Object{
  var in_bus, out_bus, pads, cccs, 
  <>step_intervals, 
  <>seg_length, 
  seq_pos, rec_start_time, loop_length, plan, play_rout, play_synths, seq_length, orig_seq_length,
  mpd, chopper, 
  // a chopper to copy rhythm and pitch sequences from
  >other_chopper;
  
  *new{arg in_bus=1, out_bus=0, pads=[13, 14, 15, 16], cccs=[1, 2, 3, 4];
	^super.newCopyArgs(in_bus, out_bus, pads, cccs).prInit;
  }
  
  prInit{
	// setup the mpd
	mpd = MPD.new;

	// 
	//mpd.onNoteOn(pads[0], {arg vel;this.playStep});
	mpd.onNoteOn(pads[0], {arg vel;this.scramble});
	mpd.onNoteOn(pads[1], {arg vel;this.startRecord});
	mpd.onNoteOff(pads[1], {arg vel;this.stopRecord});
	mpd.onNoteOn(pads[3], {arg vel;this.playLoop});
	mpd.onNoteOn(pads[2], {arg vel;this.stopLoop});

	//m.setOnNoteOff(6, {arg vel;h.set([\gate, 0]);});
	mpd.onCC(1, {arg val;var midiTo1 = 1/127;seg_length=(val*midiTo1)+0.001});
	mpd.onCC(2, {arg val;var midiTo1 = 1/127;step_intervals=[(val*midiTo1*0.25)+0.005]});
	mpd.onCC(3, {arg val;seq_length = val+1;});
	// setup the chopper
	chopper = TheChopper.new;
	// some sensible detaults
	step_intervals = [0.5];
	seg_length = 1;
	seq_length = 0;
  }

  bpm{arg bpm;
	step_intervals = [60/bpm/2];
  }

  free{
	mpd.free;
	chopper.free;
  }

  // starts a record and analysis thread
  
  startRecord{
	'startRecord'.postln;
	rec_start_time = thisThread.seconds;
	chopper.record(index:0, rec_length:20);
  }

  // calculate the length of the plan we want to make

  stopRecord{
	'loop length: '.post;
	loop_length = thisThread.seconds - rec_start_time;
	loop_length.postln;
	// now get the plan
	// (need some way to force analysis to stop and yield)
	chopper.stopRecord(0, loop_length);
	// sending in 10 for the interval arg so none of the seg lengths
	// get cropped
	plan = chopper.getSimplePlan(0, [10], 1);
	seq_length = plan.segments.size;
	orig_seq_length = seq_length;
  }

  // play the next step in the playback plan

  playStep{
	//"playing a step".postln;
	plan.playStep(crop:seg_length, limit:seq_length);
  }

  scramble{
	plan.scramble;
  }


  // play the playback plan looped!
  
  playLoop{
	this.stopLoop;
	play_rout = Routine{
	  inf.do{
		this.playStep;
		step_intervals.choose.wait;
	  };
	}.play;
  }

  stopLoop{
	seq_length = orig_seq_length;
	play_rout.stop;
  }

  seqLength{arg len;
	seq_length = min(orig_seq_length, len);
  }

  // generates a new sequence for this choppper which has trigger
  // times taken from the 'otherChoppper' variable, i.e. plays this
  // choppers events at the times of the other choppers events

  followOthersRhythm{
  }

  // generates a new sequence for this chopper which has segment
  // pitches the same as the 'otherChopper' variable, in other words,
  // play this chopper's segments at the pitches of the other
  // chopper's segments

  followOthersPitch{
  }

}


TheChopper : Object {
  var 
  inBus, outBus, 
  // note and onset memories
  stm_freq, stm_onset, ltm_freq, ltm_onset, 
  // note and onset markov chains
  mc_freq, mc_onset, 
  // each buffer has 
  chopperBuffers, 
  bufferAnalyser;
  
  *new { arg inBus=1, outBus=0;
	^super.newCopyArgs(inBus, outBus).prInit;
  }
  
  prInit{
	// create the buffer analyser
	bufferAnalyser = BufferAnalyser.new;
	// create chopperBuffers
	chopperBuffers = Array.fill(1, {
	  ChopperBuffer.new(inBus, outBus, bufferAnalyser);
	});
	// 
  }

  record{arg index=0, rec_length=0;
	chopperBuffers[index].record(rec_length);
  }

  stopRecord{arg index=0, rec_length;
	chopperBuffers[index].stopRecord(rec_length);
  }

  // test method for playing contents of a buffer as they were recorded

  playBuffer{arg index=0;
	chopperBuffers[index].playBuffer;
  }

  printOnsets{arg index=0;
	chopperBuffers[index].onsets.postln;
  }

  getSimplePlan{arg index=0, intervals=[0.5], repeats=1;
	var plan;
	// play a regular interval plan 
	plan = ChopperPlaybackPlan.getRegularEventPlan(chopperBuffers[index],intervals);	
	^plan;
  }

  // generates a simple playback plan
  // then plays it!
  // also returns the plan
  playSimplePlan{arg index=0, intervals=[0.5], repeats=1;
	var plan = this.getSimplePlan(index, intervals, repeats);
	plan.play(repeats);
	^plan;
  }

  free{
	bufferAnalyser.free;
	chopperBuffers.do{arg i;i.free};
  }


  // generates a playback plan based on current state

  getPlaybackPlan{
	
  }

}

// represents a description of what to play
// - a freq list, a timing list and segment list, where 
// the segments are sections in a buffer

ChopperPlaybackPlan : Object {
  var 
  // the buffer to play - includes info about real onset times and freqs
  chopperBuffer, 
  // segments in the buffer
  <segments, 
  // when each segment should be played
  >segment_starts, 
  // where am I in my plan?
  position;
  
  *getRegularEventPlan{arg chopperBuffer, intervals;
	^super.newCopyArgs(chopperBuffer).prInitRegularEvent(intervals);
  }

  prInit{
	this.sendSynthDefs;
	segments = chopperBuffer.segments;
	position = 0;
  }

  prInitRegularEvent{ arg intervals;
	var onsets, freqs, buffer;
	this.prInit;
	// when to trigger the segments - regularly in this case...
	segment_starts = Array.fill(segments.size, {arg i; i*(intervals.choose);});
	"Generated segment start sequence: ".postln;
	segment_starts.postln;

	// now make sure segment lengths are not overlapping
	segments.size.do{arg i;
	  if (segments[i].lengthSecs > (intervals.choose), {
		segments[i].lengthSecs_(intervals.choose);
	  });
	};
  }


  /// public functions

  // randomly re-arrange the segments in this plan

  scramble{
	segments = segments.scramble;
  }

  // simple playback

  play{arg repeats=1;
	var server, delay, total_length;
	server = Server.local;
	// total length of this plan
	total_length = segment_starts[segment_starts.size-1] + segments[segments.size-1].lengthSecs;
	repeats.do{ arg rep;
	  delay = total_length * rep;
	  segment_starts.size.do{ arg i;
		//  ("playing segment "++i++" frame offset "++segment_onsets[i]++" length sec "++segment_lengths[i]++" waiting "++segment_starts[i]).postln;
		("playing a seg at time: "++(segment_starts[i]+delay)).postln;
		server.makeBundle(segment_starts[i]+delay, {
		  Synth("cpp_simple_play", 
			[
			  \buffer, chopperBuffer.buffer, 
			  \offsetFrames, segments[i].offsetFrames, 
			  \lengthSecs, segments[i].lengthSecs
			]);
		});
	  };
	};
  }

  // plays a single step from this score
  // crop is treated as a ratio of the total 
  // segment length (0-1)
  playStep{arg crop=0, limit=0;
	var server, length, seg_len;
	server = Server.local;
	
	if (crop == 0, {seg_len = segments[position].lengthSecs}, {seg_len = crop*segments[position].lengthSecs});
	
	server.makeBundle(0.01, {
	  Synth("cpp_simple_play", 
		[
		  \buffer, chopperBuffer.buffer, 
		  \offsetFrames, segments[position].offsetFrames, 
		  \lengthSecs, seg_len
		]);
	});
	length = segments[position].lengthSecs;
	position = position+1;
	if (limit > 0 && position > limit, {position = 0});
	if (position > ((segments.size)-1), {position = 0});
	^length;
	
  }
  
  sendSynthDefs{
	var server;
	server = Server.local;
	// play the sent buffer starting at offsetFrmaes for lengthSecs
	SynthDef("cpp_simple_play", {arg out_bus=0, buffer, offsetFrames, lengthSecs;
	  var sig, env, env2, sig2;
	  // fade in and out env
	  env = EnvGen.kr(Env.new([0, 1, 1, 0], [0.001, lengthSecs-0.002, 0.001]), doneAction:2);
	  env2 = EnvGen.kr(Env.perc(0.001, 0.1));
	  sig2 = SinOsc.ar(800, mul:env2);
	  sig = PlayBuf.ar(1, buffer, BufRateScale.kr(buffer), loop: 0, startPos:offsetFrames);
	  Out.ar(0, sig*env);
	  //Out.ar(1, sig2);
	}).send(server);
  }

}

// represnets a segment in a buffer - where does it start in the buffer, how long does it last, what is its mean frequency

ChopperSegment : Object{
  var
  <buffer, <offsetFrames, <freq, <>lengthSecs, <offsetSecs;
  *new {arg buffer, offsetFrames, freq, nextOffsetFrames;
	^super.newCopyArgs(buffer, offsetFrames, freq).prInit(nextOffsetFrames);
  }
  // constructor - calculates secs offset and length based on next offset
  prInit{ arg nextOffsetFrames;
	offsetSecs = offsetFrames/buffer.sampleRate;
	lengthSecs = (nextOffsetFrames/buffer.sampleRate) - offsetSecs;
	("new segment: frameOffset: "++offsetFrames++" secs offset: "++offsetSecs++" length: "++lengthSecs++" freq: "++freq).postln;
  }
}


// represents a buffer and its extracted features

ChopperBuffer : Object {
  var 
  inBus, outBus, 
  bufferAnalyser, 
  <rec_length, 
  <buffer, <segments;
  
  *new {arg inBus, outBus, bufferAnalyser;
	^super.newCopyArgs(inBus, outBus, bufferAnalyser).prInit;
  }

  prInit{
	this.sendSynthDefs;
	// constructor
	buffer = Buffer.alloc(Server.local,48000 * 20,1);
	// force a default buffer
	//buffer = Buffer.read(Server.local, "/home/matthew/Audio/sounds/amen/29933__ERH__amen.wav");
  }
  
  ///// public methods

  // record from inBus into the buffer

  record{ arg rec_time=1;
	// remember how long we record for
	rec_length=rec_time;
	//var wait_time;
	"chopperBuffer:record".postln;
	// create a record synth
	Synth("cb_recorder", [\buffer, buffer, \audioInBus, inBus]);
	// start analysis a little time after recording
	{0.01.wait;this.analyseBuffer(rec_time)}.fork;
  }

  // call this to stop record now and store whatever data hsa been 
  // gleaned from buffer analysis
  // length is the amount of time we recorded...
  stopRecord{arg length;
	var sampleRate, new_onsets, new_freqs, onsets_freqs;
	("ChopperBuffer:record stopped, length "++length).postln;
	// pull the current data out of the analyser
	bufferAnalyser.stopAnalysis;
	onsets_freqs = bufferAnalyser.getData;
	("ChopperBuffer - data length from analyser: "++onsets_freqs.size).postln;
	new_onsets = onsets_freqs[0];
	new_freqs = onsets_freqs[1];
	// now create Chopper Segments for all the onsets
	sampleRate = buffer.sampleRate;
	segments = Array.fill(new_onsets.size-1, {arg i;
	  ChopperSegment.new(buffer:buffer, offsetFrames:new_onsets[i], freq:new_freqs[i], nextOffsetFrames:new_onsets[i+1]);
	});
	segments.add(ChopperSegment.new(buffer:buffer, offsetFrames:new_onsets[new_onsets.size-1], freq:0, nextOffsetFrames:length*buffer.sampleRate));
	("ChopperBuffer - segment count:"++segments.size).postln;
  }
  
  // play the buffer directly
  
  playBuffer{
	Synth("cb_play", [\buffer, buffer, \outBus, outBus]);
  }
 
  free{
	"ChopperBuffer:free".postln;
	buffer.free;
  }
 
  ////// end public methods


  analyseBuffer{arg length;
	"chopperBufffer:analyserBuffer".postln;
	// pass the analyser my buffer and a callback function which updates
	// my onsets
	bufferAnalyser.getOnsetsAndFreqs(buffer, length, {arg onsets_freqs;
	  var sampleRate, new_onsets, new_freqs;
	  new_onsets = onsets_freqs[0];
	  new_freqs = onsets_freqs[1];
	  "ChopperBuffer:updating onsets...".postln;
	  // now create Chopper Segments for all the onsets
	  sampleRate = buffer.sampleRate;
	  segments = Array.fill(new_onsets.size-1, {arg i;
		ChopperSegment.new(buffer:buffer, offsetFrames:new_onsets[i], freq:new_freqs[i], nextOffsetFrames:new_onsets[i+1]);
	  });
	  segments.add(ChopperSegment.new(buffer:buffer, offsetFrames:new_onsets[new_onsets.size-1], freq:0, nextOffsetFrames:length*buffer.sampleRate));
	});
  }
  
  
  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef("cb_recorder", {arg buffer, audioInBus=1;
	  // when this gets spawned, simply write from the requested audio input to the requested bus
	  var audioIn, recorder,bufLength;
	  bufLength = BufFrames.kr(buffer)/BufSampleRate.kr(buffer);
	  audioIn = AudioIn.ar(audioInBus);
	  recorder = RecordBuf.ar(audioIn, buffer, loop:0);
	  // this envelope kills the synth after bufLength seconds
	  EnvGen.kr(Env.perc(0, bufLength), 1.0, doneAction: 2) ;
	  //EnvGen.kr(Env.perc(0, 1), 1.0, doneAction: 2) ;
	}
	).send(server);//.writeDefFile;
	
	SynthDef("cb_play", {arg buffer, outBus=0;
	  var sig;
	  sig = PlayBuf.ar(1, buffer, BufRateScale.kr(buffer), loop: 0);
	  FreeSelfWhenDone.kr(sig);
	  Out.ar(outBus, sig * 0.1);
	}).send(server);
	
  }
}

// extracts features from a sent buffer

BufferAnalyser : Object {

  var 
  //  >bufnum, 
  fft_buffer, 
  osc_resp,
  still_running, 
  anal_synth, 
  <data;

  *new {
	^super.new.prInit;
  }
  // pseudo private pseudo constructor
  prInit{
	var server;
	server = Server.local;
	this.sendSynthDefs;
	fft_buffer = Buffer.alloc(Server.local, 512, 1);
	// a 3 channel array
	data = Array.new;
	osc_resp = OSCresponderNode(server.addr, '/tr', {arg time,responder,msg;
	  [time, responder, msg].postln;
	  data = data.add([time, responder, msg]);
	}).add;
  }
  
  run{
	
  }

  free{
	fft_buffer.free;
	osc_resp.remove;
  }
  
  stopAnalysis{
	"bufferanalyser stopping analysis early! ".postln;
	still_running = false;
	anal_synth.free;
  }

  getData{
	("bufferanalyser returning some data - length is  "++data.size).postln;
	//^this.processRawOsc(data.drop(1));
	^this.processRawOsc(data);
  }

  getOnsetsAndFreqs{arg source_buffer, anal_time, callback;
	var length, start;
	//start = 
	// reset the data array
	data = Array.new;
	still_running = true;
	// create an onset detector synth
	anal_synth = Synth.new("ba_Onsets_Freqs", [\fft_buff, fft_buffer, \source_buff, source_buffer, \anal_time, anal_time]);
	// wait till the synth dies
	//length = source_buffer.numFrames/ source_buffer.sampleRate;
	("BufferAnalyser:getOnsetsAndFreqs - waiting for "++anal_time++" seconds ").postln;
	SystemClock.sched(anal_time, {
	  if (data.size > 0 && still_running, 
		{callback.value(this.processRawOsc(data.drop(1)))})});
  }
	

  // pulls the onsets out of the sent osc, which will be of the form:
  // [ 1213634832.9318, an OSCresponderNode, [ /tr, 1001, 99, 28864 ] ]
  // [ 1213634832.9318, an OSCresponderNode, [ /tr, 1001, 99, 28864 ] ]
  // onsets come in on 99, freqs on 99
  // ...

  // in other words, osc[i][2][3];

  processRawOsc{arg osc;
	var onsets, freqs;
	"bufferAnalyser prccing some OSC!".postln;
	onsets = Array.new;
	freqs = Array.new;
	osc.size.do{arg i;
	  if (osc[i][2][2] == 99, {onsets = onsets.add(osc[i][2][3])});
	  if (osc[i][2][2] == 100, {freqs = freqs.add(osc[i][2][3])});
	};
	onsets.postln;
	freqs.postln;
	^[onsets, freqs];
  }
  
  // changes the times in the sent event data array to 
  // be relative to the first event

  toRelTime{arg events;
	var first, events_1d;
	("bufferanalyser: to relTime"++" size of events: "++events.size).postln;
	first = events[0][0];
	// also convert to 1d array with just the data
	events_1d = Array.newClear(events.size);
	events.size.do{arg i;
	  events_1d[i] = events[i][0] - first;
	  //events[i][0] = events[i][0] - first;
	}
	^events_1d;
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef("ba_Onsets", {arg fft_buff, source_buff, sens=0.25, anal_time;
	  var sig, chain, onsets, pips, killer, lookup, b;
	  b = source_buff;
	  lookup = Phasor.ar(0, BufRateScale.kr(b) * 1, 0, BufFrames.kr(b));
	  sig =  BufRd.ar(1, b, lookup, 1, 1);
	  //sig = PlayBuf.ar(1, source_buff, BufRateScale.kr(source_buff), loop: 0);
	  //FreeSelfWhenDone.kr(sig);
	  killer = EnvGen.kr(Env.new([0, 0], [anal_time]), doneAction:2);
	  chain = FFT(fft_buff, sig);
	  //sens = MouseX.kr(0, 1);
	  sens = 0.4;
	  onsets = Onsets.kr(chain, sens, \rcomplex);
	  // test code - plays pips 
	  //pips = WhiteNoise.ar(EnvGen.kr(Env.perc(0.001, 0.1, 0.2), onsets));
	  //Out.ar(0, Pan2.ar(sig, -0.75, 0.2) + Pan2.ar(pips, 0.75, 1));
	  //Out.ar(1, sig);
	  // end test code
	  SendTrig.kr(onsets, 99, lookup);
	  //SendTrig.kr(pitch, 100, lookup);
	}).send(server);
	
	SynthDef("ba_Onsets_Freqs", {arg fft_buff, source_buff, sens=0.05, anal_time;
	  var sig, chain, onsets, pips, killer, lookup, b, freq, hasFreq, test_sig;
	  b = source_buff;
	  //sens = MouseX.kr(0, 1).poll;
	  sens = 0.4;
	  // kills the synth
	  killer = EnvGen.kr(Env.new([0, 0], [anal_time]), doneAction:2);
	  // buffer reader
	  lookup = Phasor.ar(0, BufRateScale.kr(b) * 1, 0, BufFrames.kr(b));
	  sig =  BufRd.ar(1, b, lookup, 1, 1);
	  // pitch tracker
	  test_sig = AudioIn.ar(1);
	  //test_sig = sig;
	  # freq, hasFreq = Pitch.kr(test_sig, ampThreshold:0.03, median: 7);
	  //Out.ar(0, SinOsc.ar(freq:freq, mul:0.1));
	  //Out.ar(0, test_sig);
	  
	  // onset detector
	  chain = FFT(fft_buff, sig);
	  onsets = Onsets.kr(chain, sens, \rcomplex);
	  // test code plays pips on the onset
	  //pips = WhiteNoise.ar(EnvGen.kr(Env.perc(0.001, 0.1, 0.2), onsets));
	  //Out.ar(0, Pan2.ar(sig, -0.75, 0.2) + Pan2.ar(pips, 0.75, 1));
	  //Out.ar(1, sig);
	  // end test code

	  SendTrig.kr(onsets, [99, 100], [lookup, freq]);
	  //SendTrig.kr(pitch, 100, lookup);
	}).send(server);
	
  }
}