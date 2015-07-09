//
OctaveWaveSynth : Object {
  
  var 
  noteOnResponder;

  *new{ arg channel;
	^super.new.init(channel);
  }

  init{ arg channel;
	this.sendSynthDefs;
	this.initMidi(channel);
  }
  // initialises the midi responders
  
  initMidi{ arg channel;
	// reposnd to any note on my channel
	noteOnResponder = NoteOnResponder(
	  { |src, chan, num, vel| // another way to say arg src, chan, num, vel;
		// velocity defines the number of partials
		if (num == 36, {
		5.do{
		  var freq, attack, sustain, release, outBus;//, vel=10;
		  num = rrand(num-12, num + 12);
		  freq = (num+[-24, -12, 0, 12, 24, 36].choose).midicps;
		  //attack = 127.reciprocal * vel;
		  attack = 1.0.rand;
		  sustain = 2.0.rand;
		  release = 127.reciprocal * vel;
		  outBus = [0,1].choose;
		  //freq.postln;
		  switch (4.rand, 
		  	0, {Synth("simpleSine", [\frequency, freq, \outBus, outBus, \attackTime, attack, \sustainTime, sustain, \releaseTime, release]);},
		  	1, {Synth("simpleSaw", [\frequency, freq, \outBus, outBus, \attackTime, attack, \sustainTime, sustain, \releaseTime, release]);},
		  	2, {Synth("simplePulse", [\frequency, freq, \outBus, outBus, \attackTime, attack, \sustainTime, sustain, \releaseTime, release]);},
			3, {Synth("simpleNoise", [\frequency, freq, \outBus, outBus, \attackTime, attack, \sustainTime, sustain, \releaseTime, release]);}
		  );
		};
		});
	  }, 
	  nil,           // midi id
	  channel,       // midi channel 0-16
	  nil,     // notes to repond to 
	  nil);             // velocitiesto respond to 
	
	
  }
  
  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef(
	  "simpleSine", { arg frequency=220, outBus=0, attackTime=1, sustainTime=1, releaseTime=1;
		var chain, env;
		env = EnvGen.kr(
		  Env.new([0, 1, 0.5, 0], [attackTime, sustainTime, releaseTime], 'linear'), levelScale: 0.05, doneAction:2);
		chain = SinOsc.ar(frequency)*env;
		Out.ar(outBus, chain * 0.1);
	  }
	).send(server);
	
	SynthDef(
	  "simpleSaw", { arg frequency=220, outBus=0, attackTime=1, sustainTime=1, releaseTime=1;
		var chain, env;
		env = EnvGen.kr(
		  Env.new([0, 1, 0.5, 0], [attackTime, sustainTime, releaseTime], 'linear'), levelScale: 0.05, doneAction:2);
		chain = Saw.ar(frequency)*env;
		Out.ar(outBus, chain * 0.1);
	  }
	).send(server);
	
	SynthDef(
	  "simplePulse", { arg frequency=220, outBus=0, attackTime=1, sustainTime=1, releaseTime=1;
		var chain, env;
		env = EnvGen.kr(
		  Env.new([0, 1, 0.5, 0], [attackTime, sustainTime, releaseTime], 'linear'), levelScale: 0.05, doneAction:2);
		chain = Pulse.ar(frequency)*env;
		Out.ar(outBus, chain * 0.1);
	  }
	).send(server);
	
	SynthDef(
	  "simpleNoise", {arg frequency=220, outBus=0, attackTime=1, sustainTime=1, releaseTime=1;
		var chain, env;
		env = EnvGen.kr(
		  Env.new([0, 1, 0.5, 0], [attackTime, sustainTime, releaseTime], 'linear'), levelScale: 1, doneAction:2);
		chain = Resonz.ar(ClipNoise.ar(1.0), frequency, 0.007, mul:1)*env;
		Out.ar(outBus, chain * 0.1);
	  }
	).send(server);
	
	SynthDef("simpleFormant", {arg frequency=220, outBus=0, attackTime=0.01, sustainTime=0.1, releaseTime=0.2, vel=127, start, end;
	  var sound, midiTo1, level, env;
	  midiTo1 = 1/127;
	  level = vel*midiTo1;
	  env = EnvGen.ar(Env.new(
		[0, 1, 0.5, 0], 
		[attackTime, sustainTime, releaseTime], 'welch'), levelScale:level+0.2*0.8, doneAction:2);
	  sound = Formant.ar([frequency, frequency*0.5], XLine.kr(start, end, 0.5), XLine.kr(end, start, 0.1), 0.125);
	  //sound = SinOsc.ar([frequency, frequency*0.5]);
	  Out.ar(0, sound*env * 0.1);
	  
	}).send(server); 


  }

  free{
	noteOnResponder.remove;
  }
  

}