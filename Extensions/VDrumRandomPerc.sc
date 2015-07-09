// generates random percussion noise in response to incoming midi
// from vdrums/ etc

VDrumRandomPerc : Object {

  var 
  noteOnResponder, 
  noteMap;

  *new{ arg channel;
	^super.new.init(channel);
  }

  init{ arg channel;
	this.sendSynthDefs;
	this.makeNoteMap;
	noteOnResponder = NoteOnResponder(
	  {|src, chan, num, vel|
		this.playNote(num, vel);
	  }, 
	  nil, 
	  channel, 
	  nil, 
	  nil);
  }

  playNote{ arg num, vel;
	var synthDef;
	// what synthdef to use?
	synthDef = noteMap.at(num);
	"playing a note. incoming note: ".post;
	num.postln;
	"maps to: ".post;
	synthDef.postln;
	if (synthDef!=nil, {
	Synth.new(synthDef, [\vel, vel]);
	});
  }

  sendSynthDefs{
	var server = Server.local;
	
	SynthDef("bassDrum", {arg vel;
	  var midiTo1,level, env, partials, sound;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  env = EnvGen.ar(
		Env.new([0, 1, 0.2, 0], [0.001, 0.01, 1.0], 'welch'), levelScale:level*2, doneAction:2);

	  partials = Array.fill(10,  {arg i;
		var startFreq, endFreq;
		startFreq = (i+1)*150*level+25;
		endFreq = (i+1)*50*level+25;
		SinOsc.ar(XLine.kr(startFreq, endFreq, 0.5)*(1/i));
	  }
	  );
	  sound=partials.mean*env;  
	  Out.ar([0, 1], sound*2);
	}
	).send(server);

	SynthDef("snareDrum1", {arg vel, outBus=0, attackTime=0.01, sustainTime=0.1, releaseTime=0.1;
	  var midiTo1,level, env, partials, sound, noise;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  env = EnvGen.ar(
		Env.new([0, 1, 0.2, 0], [0.001, 0.01, 0.5], 'sine'), levelScale:level*0.5, doneAction:2);
	  noise = ClipNoise.ar(1.0);
	  partials = Array.fill(10,  {arg i;
		var startFreq, endFreq, part;
		startFreq = (i+1)*2000*level+25;
		endFreq = (i+1)*50*level+25;
		RLPF.ar(noise, XLine.kr(startFreq, endFreq, 0.5), 1-level+0.04);
	  }
	  );
	  sound=partials.mean*env;  
	  Out.ar([0, 1], sound);
	}
	).send(server);


	SynthDef("snareDrum", {arg vel, outBus=0, attackTime=0.01, sustainTime=0.1, releaseTime=0.1;
	  var midiTo1,level, env, partials, sound, noise;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  env = EnvGen.ar(
		Env.new([0, 1, 0.2, 0, 0], [0.001, 0.01, 1.0*level, 5], 'welch'), levelScale:level*0.5, doneAction:2);
	  noise = WhiteNoise.ar(1.0);
	  partials = Array.fill(10,  {arg i;
		var startFreq, endFreq, part;
		startFreq = (i+1)*2000*level+25;
		endFreq = (i+1)*50*level+25;
		part = RLPF.ar(noise, XLine.kr(startFreq, endFreq, 0.5), 1-level+0.04);
		//CombN.ar(part, 0.5, 0.05*level, 2.0/level)+part;
		//SinOsc.ar(XLine.kr(startFreq, endFreq, 0.5)*(1/i));
	  }
	  );
	  sound=partials.mean*env;  
	  sound =CombN.ar(sound, 0.5, 0.1*level+0.001, 2.0/level)+sound;
	  Out.ar([0, 1], sound);
	}
	).send(server);
	
	// uses a bouncing ball physical model to make a percussion like noise
	SynthDef("tomDrum", {arg vel;
	  var t, sf,trigger, env2, midiTo1, level, sound;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  // this envelope triggers the bouncing model when the synth is instantiated
	  trigger = EnvGen.ar(
		Env.new([0, 1, 0], [0, 0.1], 'welch'));
	  // this envelope controls overall level
	  env2 = EnvGen.ar(Env.perc(0.001, 10, 1, -8), levelScale:level, doneAction:2);
	  
	  t = TBall.ar(trigger, 0.4/level, 0.2.rand);
	  //Out.ar([0, 1], (Ringz.ar(t * 10, 2000*level, 0.1)+BrownNoise.ar(env2))*level); 
	  sound = Ringz.ar(t * 10, 7000*level, 0.1);
	  sound = CombN.ar(sound, 0.5, 0.03*level, 0.5/level)+sound;
	  Out.ar([0, 1], sound*env2); 
	  
	  //Out.ar([0, 1], 0.25*SinOsc.ar(XLine.kr(500, 400, 0.2, doneAction:2)));
	}
	).send(server);

	// uses a bouncing ball physical model to make a percussion like noise
	SynthDef("tomDrum2", {arg vel;
	  var t, sf,trigger, env2, midiTo1, level, sound;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  // this envelope triggers the bouncing model when the synth is instantiated
	  trigger = EnvGen.ar(
		Env.new([0, 1, 0], [0, 0.1], 'welch'));
	  // this envelope controls overall level
	  env2 = EnvGen.ar(Env.perc(0.001, 10, 1, -8), levelScale:level, doneAction:2);
	  
	  t = TBall.ar(trigger, 0.2/level, 0.1.rand);
	  //Out.ar([0, 1], (Ringz.ar(t * 10, 2000*level, 0.1)+BrownNoise.ar(env2))*level); 
	  sound = Ringz.ar(t * 10, 7000*level, 0.1);
	  sound = CombN.ar(sound, 0.5, 0.03*level, 0.5/level)+sound;
	  Out.ar([0, 1], sound*env2); 
	  
	  //Out.ar([0, 1], 0.25*SinOsc.ar(XLine.kr(500, 400, 0.2, doneAction:2)));
	}
	).send(server);


	SynthDef("hihat", {arg vel;
	  var env, midiTo1, level, sound;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  env = EnvGen.ar(
		Env.new([0, 1, 0.2, 0], [0.001, 0.01, 0.1], 'welch'), levelScale:level*0.5, doneAction:2);
	  sound = ClipNoise.ar(1.0);
	  sound = Resonz.ar(sound, 2000, 10*level);
	  Out.ar([0,1], sound*env);
	}).send(server);
	
  }

  makeNoteMap{
	var mpdNotes;
	// create lookup dictionary which
	// maps incoming notes to synthdef names
	mpdNotes = MiscFuncs.getMPDNoteArray(0, 16);
	noteMap = Dictionary.new;
	noteMap.add(mpdNotes[0] -> "bassDrum");
	noteMap.add(mpdNotes[1] -> "snareDrum");
	noteMap.add(mpdNotes[2] -> "tomDrum");
	noteMap.add(mpdNotes[3] -> "snareDrum1");
	noteMap.add(mpdNotes[4] -> "hihat");
	noteMap.add(mpdNotes[5] -> "tomDrum2");

  }

}