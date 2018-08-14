// fm bass
~s3 = SequencerUnitMelody.new (
  respNoteArray:[37, 40],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['e'],
  musicalScales:['pent_blues'], 
  //musicalScales:['nat_major'], 
  synthType:'seq_bass',
  polyphony:1,
  seqLength:27, 
  noteRange:[0.25, 0.125], 
  outBus:[2, 3], 
  fxBus:40);
~s1.free;
~s2 = SequencerUnitMelody.new (
  respNoteArray:[46, 40],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['e'],
  musicalScales:['pent_blues'], 
  //musicalScales:['nat_major'], 
  synthType:'seq_bass',
  polyphony:1,
  seqLength:27, 
  noteRange:[1, 2], 
  outBus:[2, 3], 
  fxBus:40);
~s2.free;
~s2.free;

~s1.free;
~s1.noteMultis_([10]);
// new seq
~s1.triggerEvent(37, 127.rand);
r = Routine{
  inf.do{
	~s1.triggerEvent(37, 64.rand);	
	[0.25, 0.125].choose.wait;
  }
}.play;

e = EvoSynthPlayer.new();
e.outBus_([4, 5]);
e.outBus_([0, 1]);
e.run;
e.synthDef_('xylo_evotest');
e.synthDef_('fm_evotest');
e.stop;
e.free;
// weirdo pecussion
~b = SequencerUnitMelody.new (
  respNoteArray:[36, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  //musicalScales:['pent_blues'], 
  musicalScales:['nat_major'], 
  synthType:'crazySynth',
  polyphony:1,
  seqLength:27, 
  noteRange:[2, 3, 0.126], 
  outBus:[2, 3], 
  fxBus:40);
~c = SequencerUnitMelody.new (
  respNoteArray:[34, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  //musicalScales:['pent_blues'], 
  musicalScales:['nat_major'], 
  synthType:'crazySynth',
  polyphony:1,
  seqLength:27, 
  noteRange:[10, 0.1], 
  outBus:[2, 3], 
  fxBus:40);
~b.free;
~c.free;

~b1 = Buffer.alloc(s, 1024, 1);
~b2 = Buffer.alloc(s, 88200, 1);
//c.free;
SynthDef("evan2_capture", {arg active=1;
  var chain, in, rec, trig, run, play;
  in = AudioIn.ar(1)*active;
  chain = FFT(~b1.bufnum, in);
  chain = PV_JensenAndersen.ar(chain,threshold:0.3,waittime:1.0);
  trig = Decay.ar(0.1*chain,0.01);
  // keep recording after event for 0.6 secs (or buflength?)
  run = EnvGen.kr(Env.perc(0.1, 0.5, 1), gate:trig);
  active = active * run;
  rec = RecordBuf.ar(in, ~b2.bufnum, run:run, loop:0,trigger:trig);
}).send(s);

SynthDef("evan2_play", {arg ratio=1;
  var play, env;
  play = PlayBuf.ar(1, ~b2.bufnum,
	BufRateScale.kr(~b2.bufnum)*ratio, loop:0) ;
  env = EnvGen.kr(Env.perc(0.001, LFDNoise0.kr(0.1).range(0.5, 0.1), 0.1), gate:1,doneAction:2);
  //  Out.ar([20], play*env);
  Out.ar([2, 3], play*env);
}).send(s);

SynthDef("evan2_verb", {
  var in;
  in = In.ar([20, 20]);
  //    4.do{
  //	  in = AllpassL.ar(in, 0.05, 0.05.rand, 0.5)+in;
  //	};
  
  Out.ar(4, in);
}).send(s);

d = Synth("evan2_capture");
//d.free;
d.set([\active, 0]);
d.set([\active, 1]);
//f.free;
f = Synth("evan2_verb");

~rats = Array.fill(12, {arg i;2/(i+1)});

t = [0.1, 0.1];

r.stop;
r = Routine{
  inf.do{
	Synth("evan2_play", [\ratio, [0.5, 1,2].choose*~rats.choose]);
~rats = ~rats.rotate(1);
t.choose.wait;
};
}.play;
r.stop;

~b1.free;
~b2.free;

~evan = EvanParker.new(out_bus:0);
~evan.run;
~evan.stop;
~evan.free;
~evan.takeSample(0.1);
~evan.mode_(1);

// full on evan mode
~evan.allocateBuffer(1024);
~evan.allocateBuffer(44100);
~evan.takeSample(1);
~evan.noteLengths_(Array.fill(10, {(0.2.rand)+0.01;}));
~evan.notePlayIntervals_(Array.fill(10, {(0.1.rand)+0.02;}));
~evan.octaves_([0.25, 0.5, 1, 2, 4, 8, 16, 32]);
~evan.octaves_([1]);
~evan.polyphony_([1, 2, 3]);
~evan.polyphony_([1]);

c = Array.fill(200, {arg i; (201-i)*0.01});
// this is quite good
x.stop;
 x = Routine{
  inf.do{
	c = c.rotate(-1);
	c[0].postln;
	~evan.noteLengths_(Array.fill(10, {(c[0].rand)+0.01}));
	~evan.notePlayIntervals_(Array.fill(10, {(c[0].rand)+0.01}));
	//(c[0]*0.25).wait;
	0.2.wait;
  };
}.play;


// low one bass drum
~s1 = SequencerUnitMelody.new (
  respNoteArray:[45, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  //musicalScales:['pent_blues'], 
  musicalScales:['nat_major'], 
  synthType:'xyloSynth',
  polyphony:2,
  seqLength:27, 
  noteRange:[0.25, 0.125, 0.125, 0.5], 
  outBus:[2, 3], 
  fxBus:40);
// high one, hihat center open
~s2 = SequencerUnitMelody.new (
  respNoteArray:[48, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  musicalScales:['nat_major'], 
  synthType:'xyloSynth',
  polyphony:1,
  seqLength:7, 
  noteRange:[1, 2], 
  outBus:[2, 3], 
  fxBus:40);

// high one, hihat center open
~s3 = SequencerUnitMelody.new (
  respNoteArray:[47, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  musicalScales:['nat_major'], 
  synthType:'xyloSynth',
  polyphony:1,
  seqLength:7, 
  noteRange:[1, 2], 
  outBus:[2, 3], 
  fxBus:40);
// big sound on the crash
~s4 = SequencerUnitMelody.new (
  respNoteArray:[43, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  musicalScales:['nat_major'], 
  synthType:'xyloSynthLFO',
  polyphony:3,
  seqLength:7, 
  noteRange:[0.5, 1, 2], 
  outBus:[2,3], 
  fxBus:40);
 
~s1.free;
~s2.free;
~s3.free;
~s4.free;

