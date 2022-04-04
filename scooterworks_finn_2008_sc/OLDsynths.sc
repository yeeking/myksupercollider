e = EvoSynthPlayer.new();
e.outBus_([4, 5]);
e.outBus_([0, 1]);
e.run;
e.free;
e.synthDef_('xylo_evotest');
e.synthDef_('fm_evotest');

//evan parker insulter
~evan = EvanParker.new(out_bus:2);
~evan.run;
~evan.stop;
~evan.free;
~evan.takeSample(0.1);
~evan.mode_(1);

// full on evan mode
~evan.allocateBuffer(1024);
~evan.allocateBuffer(88200);
~evan.takeSample(2);
~evan.noteLengths_(Array.fill(10, {(0.2.rand)+0.01;}));
~evan.notePlayIntervals_(Array.fill(10, {(0.1.rand)+0.02;}));
// slower
~evan.noteLengths_(Array.fill(10, {(5.0.rand)+0.01;}));
~evan.notePlayIntervals_(Array.fill(10, {(2.0.rand)+0.02;}));
~evan.notePlayIntervals_(Array.fill(10, {(0.5.rand)+0.02;}));

~evan.octaves_([0.25, 0.5, 1, 2, 4, 8, 16, 32]);
~evan.octaves_([1]);
~evan.polyphony_([1, 2, 3]);
~evan.polyphony_([1]);


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
{
inf.do{
  1.0.wait;
  ("sampling...").postln;
  ~evan.takeSample(1);
};
}.fork

~evan.mode_(1);

// full on evan mode
~evan.allocateBuffer(1024);
~evan.allocateBuffer(44100);
~evan.takeSample(1);

~evan.noteLengths_(Array.fill(10, {(5.0.rand)+0.01;}));
~evan.notePlayIntervals_(Array.fill(10, {(0.2.rand)+0.02;}));

~evan.octaves_([0.25, 0.5, 1, 2, 4, 8, 16, 32]);
~evan.octaves_([0.5]);
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
  musicalKeys:['cs'],
  //musicalScales:['pent_blues'], 
  musicalScales:['nat_major'], 
  synthType:'xyloSynth',
  polyphony:2,
  seqLength:27, 
  noteRange:[0.25, 0.125, 0.125, 0.5], 
  outBus:[0], 
  fxBus:40);
~s1.free;
// high one, hihat center open
~s2 = SequencerUnitMelody.new (
  respNoteArray:[48, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['cs'],
  musicalScales:['nat_major'], 
  synthType:'xyloSynth',
  polyphony:2,
  seqLength:7, 
  noteRange:[1, 2], 
  outBus:[0], 
  fxBus:40);
~s2.free;
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
~s3 = SequencerUnitMelody.new (
  respNoteArray:[43, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  musicalScales:['nat_major'], 
  synthType:'xyloSynthLFO',
  polyphony:3,
  seqLength:7, 
  noteRange:[0.5, 1, 2], 
  outBus:[0], 
  fxBus:40);

~s4 = SequencerUnitMelody.new (
  respNoteArray:[43, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  musicalScales:['nat_major'], 
  synthType:'sineFMSynth',
  polyphony:3,
  seqLength:4, 
  noteRange:[0.5, 1, 2], 
  outBus:[0], 
  fxBus:40);

~s3 = SequencerUnitMelody.new (
  respNoteArray:[45, 42],
  respCtlArray:[0],
  respChannel:0,
  musicalKeys:['g'],
  musicalScales:['nat_major'], 
  synthType:'sineSynth',
  polyphony:1,
  seqLength:5, 
  noteRange:[0.5], 
  outBus:[0], 
  fxBus:40);

~s3.free;
~s1.free;
~s2.free;
~s3.free;
~s4.free;

