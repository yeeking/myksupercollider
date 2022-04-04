// simple velocity sensitive, polyphonic sample playing class
// channel is zero indexed midi channel
// notes are midi notes to respond to [1, 2, 3]
// files are files to play for midi notes ["tes.wav",etc]
// fx_bus is sampler-fx bus
// out_bus is audio out bus

DrumKit : Object {
  // arguments
  var channel, >notes, files, >mix, fx_bus, >out_bus, ccs, killer_note, 
  // state
  fx_active=0, >vel_map, 
  // synths etc
  fx_synth, buffers, note_resp, cc_resp, 
  // default sound indexes
  <bd=0, <sn=1, <hh=2, <th=3, <tl=4, <cr=5, <rd=6;

  *new {arg channel=0, notes, files, mix, fx_bus=24, out_bus=0, ccs=[14, 15];
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs).prInit;
  }
  // set up a defualt kit
  *default {
	var channel, notes, files, mix, fx_bus, out_bus, ccs, dir;
	channel = 0;
	dir = "/home/matthew/Audio/sounds/YamahaVintageKit/";
	files = [
	  [dir++"Kik_01.wav", dir++"Kik_02.wav", dir++"Kik_03.wav", dir++"Kik_04.wav"],
	  [dir++"SN_B_01.wav", dir++"SN_B_02.wav", dir++"SN_B_03.wav", dir++"SN_B_04.wav"],
	  [dir++"Cl_HH_01.wav",dir++"Cl_HH_02.wav", dir++"Cl_HH_03.wav"],
	  [dir++"Hi_Tom_01.wav", dir++"Hi_Tom_02.wav", dir++"Hi_Tom_03.wav", dir++"Hi_Tom_04.wav",],
	  [dir++"Mid_Tom_01.wav", dir++"Mid_Tom_02.wav", dir++"Mid_Tom_03.wav", dir++"Mid_Tom_04.wav"],
	  [dir++"Szl_Cym_01.wav", dir++"Szl_Cym_03_bell.wav"],
	  [dir++"Rd_Cymb_01.wav", dir++"Rd_Cymb_02.wav", dir++"Rd_Cymb_03.wav",dir++"Rd_Cymb_04_bell.wav"],
	  [dir++"SN_B_01.wav", dir++"SN_B_02.wav", dir++"SN_B_03.wav", dir++"SN_B_04.wav"],
	];
	mix = Array.fill(files.size, {1});
	notes = Array.fill(files.size, {arg i; i});
	fx_bus = 32;
	out_bus = 0;
	ccs = [0,1,2,3];
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs).prInit;
  }

  // set up a defualt kit
  *tr606 {
	var channel, notes, files, mix, fx_bus, out_bus, ccs, dir;
	channel = 0;
	dir = "/home/matthew/Audio/sounds/tr606/";
	files = [
	  [dir++"0_TR606_kick.wav"], 
	  [dir++"1_TR606_snare.wav"], 
	  [dir++"TR606_cl_hat.wav"], 
	  [dir++"TR606_hi_tom.wav"], 
	  [dir++"TR606_lo_tom.wav"], 
	  [dir++"TR606_cymbal.wav"],
	  [dir++"TR606_open_hat.wav"], 
	];
	mix = Array.fill(files.size, {1});
	notes = Array.fill(files.size, {arg i; i});
	fx_bus = 32;
	out_bus = 0;
	ccs = [0,1,2,3];
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs).prInit;
  }

  // set up a defualt kit
  *tr808 {
	var channel, notes, files, mix, fx_bus, out_bus, ccs, dir;
	channel = 0;
	dir = "/home/matthew/Audio/sounds/tr808/";
	files = [
	  [dir++"1_808_Kick_short.wav"], 
	  [dir++"2_808_Snare_lo1.wav"], 
	  [dir++"3_808_Hat_closed.wav"], 
	  [dir++"808_Lo_Tom.wav"], 
	  [dir++"808_Hi_Tom.wav"], 
	  [dir++"808_Cymbal-high.wav"],
	  [dir++"808_Cowbell.wav"]
	];
	mix = Array.fill(files.size, {1});
	notes = Array.fill(files.size, {arg i; i});
	fx_bus = 32;
	out_bus = 0;
	ccs = [0,1,2,3];
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs).prInit;
  }

  
  // set up a defualt kit
  *agitation {
	var channel, notes, files, mix, fx_bus, out_bus, ccs, dir;
	channel = 0;
	dir = "/home/matthew/Audio/finn/music_of_the_mind/samples_for_live_show/agitation/";
	files = [
	  [dir++"ag_1.wav"], 
	  [dir++"ag_2.wav"], 
	  [dir++"ag_3.wav"], 
	  [dir++"ag_4.wav"], 
	];
	mix = Array.fill(files.size, {1});
	notes = Array.fill(files.size, {arg i; i});
	fx_bus = 32;
	out_bus = 0;
	ccs = [0,1,2,3];
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs).prInit;
  }




  // set up a defualt kit
  *amen {
	var channel, notes, files, mix, fx_bus, out_bus, ccs, dir, dir2;
	channel = 0;
	dir = "/home/matthew/Audio/sounds/amen/";
	files = [
	  [dir++"amen_bd2.wav", dir++"amen_bd4.wav"], 
	  [dir++"amen_sn2.wav", dir++"amen_sn1.wav"], 
	  [dir++"amen_hh1.wav"],
	  [dir++"amen_crash.wav"], 
	];
	mix = Array.fill(files.size, {1});
	notes = Array.fill(files.size, {arg i; i});
	fx_bus = 32;
	out_bus = 0;
	ccs = [0,1,2,3];
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs).prInit;
  }

  // set up a defualt kit
  *tr707 {
	var channel, notes, files, mix, fx_bus, out_bus, ccs, dir;
	channel = 0;
	dir = "/home/matthew/Audio/sounds/tr707/";
	files = [
	  [dir++"0_BassDrum1.wav"], 
	  [dir++"1_Snare1.wav", dir++"Snare2.wav", dir++"RimShot.wav"], 
	  [dir++"0_HhC.wav"], 
	  [dir++"3_HiTom.wav"], 
	  [dir++"LowTom.wav"], 
	  [dir++"Crash.wav"],
	  [dir++"Ride.wav"],
	  [dir++"HandClap.wav"], 
	  [dir++"CowBell.wav"],
	  [dir++"Tamb.wav"],

	];
	mix = Array.fill(files.size, {1});
	notes = Array.fill(files.size, {arg i; i});
	fx_bus = 32;
	out_bus = 0;
	ccs = [0,1,2,3];
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs).prInit;
  }

  // set up a defualt kit
  *skins {
	var channel, notes, files, mix, fx_bus, out_bus, ccs, dir;
	channel = 0;
	dir = "/home/matthew/Audio/sounds/skinner_kit/";
	files = [
	  [dir++"bd_li.wav", dir++"bd1.wav", dir++"bd_poke.wav"], 
	  [dir++"sn1.wav", dir++"sn2.wav", dir++"sn3.wav", dir++"sn4.wav"], 
	  [dir++"hh_cl.wav"], 
	  [dir++"tom1.wav"], 
	  [dir++"tom2.wav"], 
	  [dir++"crash_li.wav"] 
	];
	mix = Array.fill(files.size, {1});
	notes = Array.fill(files.size, {arg i; i});
	fx_bus = 32;
	out_bus = 0;
	ccs = [0,1,2,3];
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs).prInit;
  }

  prInit{
	var server, tempA, f, o;
	server = Server.local;
	this.sendSynthDefs;
	buffers = Array.fill(files.size, {arg i;
	  Array.fill(files[i].size, {arg j;
		("loading single file into bank "++i++" pos "++j++" "++files[i][j]).postln;		  
		Buffer.read(server,files[i][j]);
	  });
	}
	);
	"respnder notes..".postln;
	notes.postln;
	note_resp = NoteOnResponder (
	  {|src, chan, num, vel|
		this.playSound(num, vel);
	  }, 
	  nil, 
	  
	  channel, 
	  notes, 
	  nil);
	// calculate logarithmic velocity map
	// (this is log10(127)
	o = 5;
	f = log(128+o) - log(o);
	vel_map = Array.fill(127, {arg i;log(i+o) - log(o) / f});
	vel_map.postln;
  }
  
  playSound{arg note, vel;
	var ind, col, cols;
	
	ind = notes.indexOf(note);
	if (ind == nil, {^nil});
	// how many samples mapped to this note?
	cols = buffers[ind].size;
	// choose one based on velocity
	col = ((vel-1)/127*cols).floor;
	//("playing note "++note++" sound "++ind).postln;
	//	if (fx_active == 0, {
	//fx_synth = Synth("dk_comp", [\out_bus, out_bus, \in_bus, fx_bus]);
	//fx_active = 1;
	//});
	vel = vel_map[vel-1] * mix[ind];
	Server.local.makeBundle(0.01, {
	  Synth("dk_play", [
		//\out_bus, fx_bus, 
		\out_bus, out_bus, 
		\bufnum, buffers[ind][col].bufnum, 
		\vel, vel]);
	});
  }
  
  free{
	fx_synth.free;
	note_resp.remove;
	cc_resp.remove;
	buffers.size.do{arg i;buffers[i].free};
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	
	SynthDef("dk_play", {arg out_bus, bufnum, vel;
	  var playBuf, midiTo1, env;
	  //midiTo1 = 1/127*vel;
	  playBuf = PlayBuf.ar(1,bufnum, loop:0);
	  //env = EnvGen.kr(Env.perc(0.001, BufDur.kr(bufnum)*2, 1), doneAction:2);
	  FreeSelfWhenDone.kr(playBuf);
	  //playBuf = Compander.ar(playBuf, playBuf, thresh:MouseX.kr(0, 1), slopeBelow:0.2,slopeAbove:0.5,clampTime:MouseY.kr(0, 1),relaxTime:0.1);
	  //Out.ar(out_bus, playBuf*midiTo1);
	  Out.ar([out_bus, out_bus+1], playBuf*vel * 0.5);
	  //Out.ar(0, SinOsc.ar(220));
	}).send(server);

  }
}

DrumKitGran2 : Object {
  // arguments
  var channel, notes, files, >mix, fx_bus, out_bus, ccs, killer_note,  
  // state
  fx_active=0, >vel_map, midiTo1, play_synths, 
  // how long to play samples for
  play_length, 
  // where in the sample to start playback
  play_offset, 
  // -- these are controlled by the midi controller
  // how fast to play the sample
  >play_speed, 
  // what pitch to play the sample
  >play_pitch, 
  // how granular should it sound?
  >play_gran,
  // add random values to the other midi controlled values
  >play_random, 
  // -- end of controlled by midi controller

  // synths etc
  fx_synth, buffers, note_resp, cc_resp;

  *new {arg channel, notes, files, mix, fx_bus=24, out_bus=0, ccs=[14, 15, 12, 13], killer_note=0;
	^super.newCopyArgs(channel, notes, files, mix, fx_bus, out_bus, ccs, killer_note).prInit;
  }

  prInit{
	var server, tempA, f, o;
	server = Server.local;
	this.sendSynthDefs;
	buffers = Array.fill(files.size, {arg i;
	  Array.fill(files[i].size, {arg j;
		("loading single file for note "++notes[i]++" into bank "++i++" pos "++j++" "++files[i][j]).postln;		  
		Buffer.read(server,files[i][j]);
	  });
	}
	);

	// play_synths makes it mono
	play_synths = Array.fill(files.size, {Synth.newPaused("dk_play_gran")});

	"respnder notes..".postln;
	notes.postln;
	note_resp = NoteOnResponder (
	  {|src, chan, num, vel|
		if (num == killer_note, 
		  {this.killSounds;}, 
		  {this.playSound(num, vel);});
	  }, 
	  nil, 
	  channel, 
	  notes.add(killer_note), 
	  nil
	);
	  	// calculate logarithmic velocity map
	// (this is log10(127)
	o = 5;
	f = log(128+o) - log(o);
	vel_map = Array.fill(127, {arg i;log(i+o) - log(o) / f});
	vel_map.postln;

	// set some defaults for the playback parameters
	play_speed = 164;
	play_pitch = 64;
	play_gran = 64;
	play_random = 0;
	midiTo1 = 1/127;
	// now set up the cc responders 
	cc_resp = CCResponder({|src,chan,num,val|
	  var ind;
	  //[src,chan,num,val].postln;
	  // update the appropriate class field
	  ind = ccs.indexOf(num);
	  //val = val * midiTo1;
	  switch (ind, 
		0, {play_speed = val}, 
		1, {play_pitch = val}, 
		2, {play_gran = val}, 
		3, {play_random = val}
	  );
	  // now update the playing synths
	  this.updateSynths;
	}, 
	  nil, // any src
	  channel, // my channel
	  ccs, // only my ccs
	  nil);
	
	ccs.postln;
	("The killer note is "++killer_note).postln;
  }
	
  updateSynths{
	play_synths.do{arg synth;
	  synth.set(
		//\out_bus, fx_bus, 
		\grain_length,play_gran+((play_random + 0.01).rand), 
		\grain_count,400, 
		\grain_pitch, play_pitch + ((play_random + 0.01).rand), 
		\playback_position, (play_speed + ((play_random + 0.01).rand)));
	};

  }

  killSounds{
	//"killing sounds".postln;
	//reset the pitch as well...
	play_pitch = 64;
	play_gran = 127;
	play_speed = 64;
	play_synths.size.do{arg i;play_synths[i].run(false)};
  }
	
  playSound{arg note, vel;
	var ind, col, cols;
	
	ind = notes.indexOf(note);
	("Playing sound at ind "++ind).postln;
	// how many samples mapped to this note?
	cols = buffers[ind].size;
	// choose one based on velocity
	col = ((vel-1)/127*cols).floor;
	//("playing note "++note++" sound "++ind).postln;
	//	if (fx_active == 0, {
	//fx_synth = Synth("dk_comp", [\out_bus, out_bus, \in_bus, fx_bus]);
	//fx_active = 1;
	//});
	vel = vel_map[vel-1] * mix[ind];
	play_synths[ind].free;
	play_synths[ind] = Synth("dk_play_gran", [
	  //\out_bus, fx_bus, 
	  \out_bus, out_bus, 
	  \bufnum, buffers[ind][col].bufnum, 
	  \level, vel, 
	  \grain_length,play_gran+((play_random + 0.01).rand), 
	  \grain_count,400, 
	  \grain_pitch, play_pitch + ((play_random + 0.01).rand), 
	  \playback_position, (play_speed + ((play_random + 0.01).rand))]);
	
  }	
  
  free{
	fx_synth.free;
	note_resp.remove;
  	cc_resp.remove;
	buffers.size.do{arg i;buffers[i].free};
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	
	SynthDef("dk_play_gran", {arg bufnum, grain_length=64, grain_count=64, grain_pitch=64, playback_position=127, out_bus=0, level=64;
	  var midiTo1, chain, trig, freq, buflength, one_grain_length, position_lfo, lfo_speed, loop_length, env, env_length;
	  midiTo1 = 1/127;
	  loop_length = BufDur.kr(bufnum);
	  playback_position  = playback_position  + 1;// + LFDNoise1.kr(0.5).range(1, 20);
	  // length is defined by the length of the recorded snippet
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
	  // in this synth, the playback_position 
	  // controls the playback speed??
	  // playback_postition comes in at 0-254
	  playback_position = (playback_position  * midiTo1)+0.05;
	  //playback_position = MouseX.kr(0, 1);
	  lfo_speed = (loop_length.reciprocal) * playback_position;
	  // calculate the env_length.
	  // if the playback speed is low, we want a long envelope
	  env_length = (loop_length * (1/(lfo_speed+0.1))) + (loop_length * 0.5);
	  env = EnvGen.kr(Env.new([0, 1, 1, 0], [0, env_length*0.5, env_length * 0.5]), doneAction:1, levelScale:level);
	  position_lfo = LFSaw.kr(freq:lfo_speed, iphase:1, mul:(loop_length*0.5), add:(loop_length*0.5));
	  chain = TGrains.ar(
		numChannels:2, trigger:trig, bufnum:bufnum, rate:grain_pitch, 
		centerPos:position_lfo, dur:grain_length, interp:4, amp:2);
	  chain = chain.mean;
	  //chain = LPF.ar(chain, 5000);
	  //chain = Compander.ar(LPF.ar(chain, 5000), chain, thresh:0.5, slopeBelow:0.2,slopeAbove:0.7,clampTime:0.1,relaxTime:0.1) + chain;
	  Out.ar(out_bus, chain *env);
	}).send(server);
  }
}
