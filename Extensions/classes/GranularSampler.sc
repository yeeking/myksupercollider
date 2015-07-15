// sampler - does grain based playbkac/ freeze etc...

//g = GranularSampler.new( channel:0, gIntervalBus:17, gLengthBus:18, gCountBus:19);
//g.startPlayback
//g.stopPlayback;
//g.free;

GranularSampler : Object {
  
  var 
  // creation arguments
  channel, start_note, out_bus, in_bus, start_ctl, mpd_mode, <>pitch, 
  // stuff that needs to be freed
  buffer, <play_synth, ctlResp, noteOnResp, noteOffResp, 
  // internal workings
  rec_start_time, loop_length, activation_state = 1, record_state,
  // midi controllable arguments
  grain_length=127, grain_count=64, grain_pitch=64, playback_position=0, level=32;
  *new {arg channel=0, start_note=0, out_bus=0, in_bus=1, start_ctl=8, mpd_mode=1, pitch=1;
	^super.newCopyArgs(channel, start_note, out_bus, in_bus, start_ctl, mpd_mode, pitch).prInit;
  }
  
  prInit{
	var server, notes;
	server = Server.local;
	buffer = Buffer.alloc(server,480000*2,1);
	this.sendSynthDefs;
	out_bus.postln;
	start_note.postln;
	in_bus.postln;
	start_ctl.postln;
	record_state = 0;
	ctlResp = CCResponder (
	  {|src, chan, num, value| 
		// call updates on synths
		if (activation_state==1, 
		  {this.updateCtl(num, value);}
		);
	  }, 
	  nil, 
	  channel, 
	  [start_ctl, start_ctl+1, start_ctl+2, start_ctl+3], 
	  nil
	);

	// setup the midi respomders
	
	// which notes does it respond to?
	if (mpd_mode==1, 
	  {
		notes = Array.fill(4, {arg i;
		  MiscFuncs.getMPDNoteArray()[start_note+i];
		});
	  }, 
	  {notes = Array.fill(4, {arg i;
		start_note+i;
	  });
	  });
	
	notes.postln;
	
	noteOnResp = NoteOnResponder(
	  { |src, chan, num, vel| // another way to say arg src, chan, num, vel;
		if (activation_state==1, 
		  {
			switch (num, 
			  notes[0], {this.startPlay("granular_buffer_player");}, 
			  notes[1], {if (record_state == 0, {this.startRecord;}, {this.stopRecord;})},
			  //notes[1], {this.startRecord}, 
			  notes[2], {this.stopPlay;},
			  notes[3], {this.startPlay("granular_buffer_player_lfo");}
			);
		  });
	  }, 
	  nil, // any midi id
	  channel,  // only my channel
	  notes, 
	  //[start_note, start_note+1, start_note+2, start_note+3], // only my 4 notes
	  nil);
	
	noteOffResp = NoteOffResponder(
	  { |src, chan, num, vel| 
		if (activation_state==1, 
		  {
			//this.stopRecord;
		  }
		);
	  }, 
	  nil, // any midi id
	  channel,  // only my channel
	  notes[1], // only my record note
	  nil
	);
  }

  run{

  }
  
  free{
	play_synth.free;
	buffer.free;
	ctlResp.remove;
	noteOnResp.remove;
	noteOffResp.remove;
  }

  updateCtl{ arg ctl, val;
	ctl = ctl - start_ctl;
	switch (ctl, 
	  0, {play_synth.set(\grain_length, val);grain_length=val;}, 
	  1, {play_synth.set(\grain_count, val);grain_count=val;}, 
	  2, {play_synth.set(\grain_pitch, val);grain_pitch=val;}, 
	  3, {play_synth.set(\playback_position, val*2);playback_position=val;}
	);
  }

  startPlay{ arg synth;
	"granular playing".postln;
	play_synth.free;
	// reset some values
	grain_length = 64;
	grain_count = 64;
	grain_pitch = 64;
	playback_position = 127;
	play_synth = Synth.new(synth, [
	  \bufnum, buffer.bufnum, 
	  \loop_length, loop_length, 
	  \grain_length, grain_length, 
	  \grain_count, grain_count, 
	  \grain_pitch, grain_pitch, 
	  \playback_position, playback_position, 
	  \out_bus, out_bus, 
	  \level, level]);
  }

  stopPlay{
	"granular stopped playing".postln;
	play_synth.free;
  }

  startRecord{
	"granular start record".postln;
	record_state = 1;
	rec_start_time = thisThread.seconds;
	Synth("granular_buffer_recorder", [\bufnum, buffer.bufnum, \audioInBus, in_bus]); 
  }

  stopRecord{
	"granular stop record".postln;
	record_state = 0;
	loop_length = thisThread.seconds - rec_start_time;
	loop_length.postln;
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	
	SynthDef("granular_buffer_recorder", {arg bufnum, audioInBus=1;
	  // when this gets spawned, simply write from the requested audio input to the requested bus
	  var audioIn, recorder,bufLength;
	  bufLength = BufFrames.kr(bufnum)/BufSampleRate.kr(bufnum);
	  audioIn = AudioIn.ar(audioInBus);
	  recorder = RecordBuf.ar(audioIn, bufnum, loop:0);
	  // this envelope kills the synth after bufLength seconds
	  EnvGen.kr(Env.perc(0, bufLength), 1.0, doneAction: 2) ;
	  //EnvGen.kr(Env.perc(0, 1), 1.0, doneAction: 2) ;
	}
	).send(server);//.writeDefFile;

	// all args come in as midi ctl data 0-127. 
	SynthDef("granular_buffer_player", {arg bufnum, loop_length, grain_length=64, grain_count=1, grain_pitch=64, playback_position=0.1, out_bus=0, level=64;
	  var midiTo1, chain, trig, freq, buflength, one_grain_length;
	  midiTo1 = 1/127;
	  // length is defined by the length of the recorded snippet
	  //buflength = BufDur.kr(bufnum);
	  grain_count = grain_count * 2;
	  buflength = loop_length;
	  // need to trigger grain_count grains in buffer.length
	  //grain_count = MouseX.kr(1, 100);
	  freq = grain_count * buflength.reciprocal;
	  trig = Impulse.ar(freq);
	  // grain length varies between buffer.length/grain_count and that * 2
	  one_grain_length = buflength/grain_count;
	  grain_length = grain_length * midiTo1 * one_grain_length * 4;
	  // convert midi control number to octave multiple
	  grain_pitch = (64/(grain_pitch+1)).reciprocal;
	  // normalise playback from 0-loop_length
	  playback_position = Lag.kr(playback_position, 0.1) * midiTo1 * loop_length;

	  //grain_length = MouseY.kr(0, one_grain_length*4);
	  chain = TGrains.ar(2, trig, bufnum, grain_pitch, playback_position, grain_length, level*midiTo1, 4);
	  //chain = TGrains.ar(2, trig, bufnum, 1.0, 1.0, 0.5, 1.0, 4);
	  //chain = SinOsc.ar(220, 0.5);
	  //chain = Compander.ar(chain, chain,thresh:0.09,slopeBelow:0.2,slopeAbove:0.5,clampTime:0.01,relaxTime:0.1);
	  Out.ar(out_bus, (chain.mean)*0.5);
	}).send(server);


	SynthDef("granular_buffer_player_lfo", {arg bufnum, loop_length, grain_length=64, grain_count=1, grain_pitch=64, playback_position=0.1, out_bus=0, level=64;
	  var midiTo1, chain, trig, freq, buflength, one_grain_length, position_lfo, lfo_speed;
	  midiTo1 = 1/127;
	  // length is defined by the length of the recorded snippet
	  //buflength = BufDur.kr(bufnum);
	  grain_count = grain_count * 2;
	  buflength = loop_length;
	  // need to trigger grain_count grains in buffer.length
	  //grain_count = MouseX.kr(1, 100);
	  freq = grain_count * buflength.reciprocal;
	  trig = Impulse.ar(freq);
	  // grain length varies between buffer.length/grain_count and that * 2
	  one_grain_length = buflength/grain_count;
	  grain_length = grain_length * midiTo1 * one_grain_length * 4;
	  // convert midi control number to octave multiple
	  grain_pitch = (64/(grain_pitch+1)).reciprocal;
	  // test...
	  // in this synth, the playback_position 
	  // controls the playback speed??
	  // playback_postition comes in at 0-254
	  //lfo_speed = playback_position * midiTo1;
	  playback_position = (playback_position  + 1) * midiTo1;
	  lfo_speed = (loop_length.reciprocal) * playback_position;
	  //  position_lfo = LFSaw.kr(loop_length.reciprocal, iphase:1, mul:(loop_length*0.5), add:(loop_length*0.5));
	  position_lfo = LFSaw.kr(freq:lfo_speed, iphase:1, mul:(loop_length*0.5), add:(loop_length*0.5));
	  chain = TGrains.ar(2, trig, bufnum, grain_pitch, position_lfo, grain_length, level*midiTo1, 4);
	  Out.ar(out_bus, (chain.mean) * 0.5);
	}).send(server);

  }
}


GranularSampler2 : GranularSampler{
  
  var >grain_pitches;

  prInit{
	super.prInit;
	// now set up the arrays
	this.resetArrays;
  }

  resetArrays{
	grain_pitches = [64, 96, 128, 32, 48];
  }

  startPlay{ arg synth = "granular_buffer_player_lfo";
	"granular2 playing".postln;
	play_synth.free;
	grain_pitch = grain_pitches[0];
	grain_pitches = grain_pitches.rotate(-1);

	play_synth = Synth.new(synth, [
	  \bufnum, buffer.bufnum, 
	  \loop_length, loop_length, 
	  \grain_length, grain_length, 
	  \grain_count, grain_count, 
	  \grain_pitch, grain_pitch * pitch, 
	  \playback_position, playback_position, 
	  \out_bus, out_bus, 
	  \level, level]);
  }

  stopPlay{
	"granular2 stopped playing".postln;
	play_synth.free;
	// now reset 
	grain_length = 64;
	grain_count = 64;
	grain_pitch = 64;
	playback_position = 127;
	
	this.resetArrays;
  }
  

}


GranSampler : GranularSampler2 {
}