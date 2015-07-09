// this aims to build up a bank of samples from an incoming audio
// signal based on an onset detector

LiveSampler : Object{
  
  var >in_bus, >out_bus, buf_count, osc_id, start_note,
  buffers, buf_times, rec_start,onsets, collect_samples, target_buffer, recording, thresh;
  
  *new{arg in_bus=1,out_bus=0, buf_count=8, osc_id=303, start_note=0;
	^super.newCopyArgs(in_bus, out_bus, buf_count, osc_id, start_note).prInit;
  }
  
  prInit{
	var server;
	collect_samples = true;
	server = Server.local;
	buffers = Array.fill(buf_count, {
	  // 5 second buffers
	  Buffer.alloc(server, 48000 * 5, 1);
	});
	buf_times = Array.fill(buf_count, {0.25});
	onsets = MykBonk.new(osc_id:osc_id, audio_in:in_bus, callback:{arg amp; this.onset(amp)});
	target_buffer = 0;
	thresh = 0.05;
	this.sendSynthDefs;
	"LiveSample using osc id 303. Call train to learn rhythm stopTrain to stop learning, sample to collect a set of samples, play to play samples back".postln;

	

  }

  run{
	onsets.run;  
  }

  train{
	"I'll email you when I've implemented this hah!".postln;
  }
  
  stopTraining {
	
  }
  
  freeMe{
	//onsets.free;
  }

  // start playing a sequence using the training data
  
  play{
	
  }

  playSample{arg bufnum, amp = 1, len = nil;
	//("LiveSample::playSample "++bufnum++" length is "++buf_times[bufnum]).postln;
	if (len == nil, {len = buf_times[bufnum]});
	Synth("livesample_play", [
	  \ratio, 1, 
	  \play_b, buffers[bufnum].bufnum, 
	  \out_bus, out_bus, 
	  \len, len, 
	  \amp, amp]);
  }
  
  // collect buf_count new samples
  sample{arg lev = 0.05;
	//"LiveSample::sample".postln;
	target_buffer = 0;
	thresh = lev;
	recording = false;
	collect_samples = true;
	// now stop recording the current sample if needed
  }
  
  
  // gets called by the onset detector
  onset{arg amp;
	//"LiveSample::onset".postln;
	
	if ((collect_samples == true) && (target_buffer < buf_count) && (amp > thresh), 
	  // now we either stop sampling the current sound or start sampling a new sound
	  {
		if (recording == true, 
		  // stop recording
		  {var rec_length;
			rec_length = thisThread.seconds - rec_start - 0.05;
			// ensure it is greater than 0
			rec_length = max(rec_length, 0.01);
			("Recorded "++rec_length++" into buffer "++target_buffer).postln;
			buf_times[target_buffer] = rec_length;
			target_buffer = target_buffer + 1;
			recording = false;
		  }
		);
		// now if there are more buffers available, start recording into the next one
		if (target_buffer < buf_count, 
		  {
			("Recording into buffer "++target_buffer).postln;
			rec_start = thisThread.seconds;
			recording = true;
			Synth("livesample_record", [\in_bus, in_bus, \rec_b, buffers[target_buffer].bufnum]);
		  }
		);
	  }
	);
  }
  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef("livesample_record", {arg in_bus, rec_b;
	  var in, env, rec;
	  in = AudioIn.ar(in_bus);
	  // envelope the input to remove clicks
	  env = EnvGen.kr(Env.new([0, 1, 1, 0], [0.001, 1.0, 0.001]), doneAction:2, gate:1);
	  rec = RecordBuf.ar(in*env, rec_b, loop:0);	  
	}).send(server);
	SynthDef("livesample_play", {arg ratio=1, play_b, out_bus=0, len, amp = 1;
	  var play, env;
	  play = PlayBuf.ar(1, play_b,
		BufRateScale.kr(play_b)*ratio, loop:1);
	  //  env = EnvGen.kr(Env.perc(0.5, 0.5, 0.1), gate:1,doneAction:2);
	  env = EnvGen.kr(Env.new([0, 1, 0], [0.01, len], 'welch'), gate:1,doneAction:2);
	  //env = EnvGen.kr(Env.new([0, 1, 0], [0.01, len]), gate:1,doneAction:2);
	  Out.ar(out_bus, play*env * 0.25 * amp);
	}).send(server);
  }
}

LiveSamplerFX : LiveSampler {
  
  var <>ctl1=64, <>ctl2=64, <>ctl3=64, <>ctl4=64;
  
   playSample{arg bufnum, amp = 1, len = nil, synthdef="";
	 //("LiveSample::playSample "++bufnum++" length is "++buf_times[bufnum]).postln;
	if (len == nil, {len = buf_times[bufnum]});
	Synth("livesample_play_gran", [
	  \ratio, 1, 
	  \play_b, buffers[bufnum].bufnum, 
	  \out_bus, out_bus, 
	  \len, (ctl4 / 127), 
	  \amp, amp, 
	  \ctl1Set, ctl1, 
	  \ctl2Set, ctl2, 
	  \ctl3Set, ctl3
	]);
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	super.sendSynthDefs;
	SynthDef(
	  "livesample_play_gran", {arg len=0.5, out_bus=0, play_b, bufLengthBus, ctl1Set=127, direction=0.5, ctl2Set=64, ctl3Set=32;
		var playBuf, lookup, bufLength, pitch, env_freq, actual_length, envelope, midiTo1;
		//bufLength = In.kr(bufLengthBus);
		bufLength = ctl2Set;
		// convert 0-127 to a ratio, where * 2 = up an octave
		// pitch is used as a multiplier on the frequency
		midiTo1 = 1/127;
		pitch = ctl1Set*midiTo1+0.5;
		//pitch = 1;
		lookup = Phasor.ar(
		  0, 
		  BufRateScale.kr(play_b)*direction*pitch, 
		  start:Line.ar(0, 200, ctl2Set*4)+Line.ar(0, 10000, 20), 
		  end:Line.ar(5000, 10, ctl3Set*0.02));
		// actual length is bufLength corrected for pitch
		actual_length = bufLength/pitch; 
		// number of actual_lengths per second
		env_freq = actual_length.reciprocal;
		//envelope = LFTri.ar(env_freq);
		envelope = EnvGen.ar(Env([0, 1, 1, 0], [0.01, actual_length-0.02, 0.01]), 
		  Impulse.ar(env_freq), doneAction:0);
		playBuf = BufRd.ar(numChannels:1,bufnum:play_b, phase:lookup);	
		Compander.ar(playBuf, playBuf, MouseX.kr(0.1, 1), 10, 1, 0.01, 0.01);
		Out.ar(out_bus, playBuf*envelope * Line.kr(0.5, 0, len, doneAction:2));
		//Out.ar(outBus, playBuf);
	  }
	).send(server);//.writeDefFile;

	SynthDef("livesample_play", {arg ratio=1, play_b, out_bus=0, len, amp = 1;
	  var play, env;
	  play = PlayBuf.ar(1, play_b,
		BufRateScale.kr(play_b)*ratio, loop:1);
	  //  env = EnvGen.kr(Env.perc(0.5, 0.5, 0.1), gate:1,doneAction:2);
	  env = EnvGen.kr(Env.new([0, 1, 0], [0.01, len], 'welch'), gate:1,doneAction:2);
	  //env = EnvGen.kr(Env.new([0, 1, 0], [0.01, len]), gate:1,doneAction:2);
	  Out.ar(out_bus, play*env * 0.25 * amp);
	}).send(server);
  }
 
}