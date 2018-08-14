~u = UltraPhaser.new(
  channel:0, feedbackCtl:4, 
  delayCtl:5, variationCtl:6, outBus:2, inBus:1);
~u.run;
~u.free;

~p  = PitchTracker.new(synthMode:0, audioIn:1, outBus:0, channel:0, freqOffsetCtl:12);
~p.run;
~p.free;


~t = [0.1];
~r.stop;
~r = Routine{
  inf.do{
	~u.updateCtl([5].choose, 127.rand);
	~t.choose.wait;
  };
}.play;
~r.stop;

~p  = PitchTracker.new(synthMode:0, audioIn:1, outBus:1, channel:0, freqOffsetCtl:12);
~p.run;
~p.free;

// RI 7
a = AutoDub.new(out_bus:4);
r = Routine.new{
  a.interval_([0.1, 0.25].choose.rand);
}.play;
// get the synths etc goign
a.run;
// stops the routine chaning the settings
a.stop;
// change to next synth
a.open;
a.free;

~fx = SequencerUnitMultiFX.new(
  respNoteArray:[42, 44],
  respCtlArray:[0],
  respChannel:0,
  synthType:'ultraphaserFX', 
  seqLength:4, 
  outBus:2, 
  fxBus:6);
~fx.run;
~fx.free;
~fxplay.stop;
~fxplay = Routine{
  inf.do{
	~fx.triggerEvent(71, 127.rand);
	~fx.triggerEvent(39, 127.rand);
	[1.0].choose.rand.wait;
  };}.play;


d = Synth("AutoDub_pitcher");
d.set([\ctl1, 127]);
SynthDef("robot", {
  var in, chain, ring, lfo;
  in = AudioIn.ar(1);
  lfo = LFDNoise1.kr(0.5).range(0.01, 0.02);
  ring = Pulse.ar((1/lfo)/2, width:lfo);
  chain = in * ring;
  chain = CombL.ar(in, 0.05, lfo);
  //chain = Clip.ar(chain, 0.1, 0.1) * 100;
  Out.ar([1], chain*0.2);
}).send(s);

~ot = Synth("robot");
~ot.free;