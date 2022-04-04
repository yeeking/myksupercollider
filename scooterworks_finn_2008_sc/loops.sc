{Array.fill(10, SinOsc.ar(XLine.kr(100.0.rand, 1000.0.rand)))}.play

~loop1.free; 

~loop1 = LoopSampler( channel: 0, ctl_buses:[1,2,5,4,3],
  loop_length_bus:30, fx_bus:35, fxSynth_type:0, start_note:0, outBus:0,
  mpd_mode:1); 

~loop2.free; 

~loop2 = LoopSampler( channel: 0,
  ctl_buses:[11,12,33,34,17], loop_length_bus:30, fx_bus:35,
  fxSynth_type:0, start_note:53, outBus:0, mpd_mode:0);

~funcs = [{~loop1.playLoop(1.0.rand + 1)}, {~loop1.startRecord;}, {~loop1.stopRecord}];
~funcs = [{~loop1.playLoop(0.1.rand )}, {~loop1.startRecord;}, {~loop1.stopRecord}];

~time = [0.25, 0.1];
~time = [1.0, 0.5];

~steps = Array.fill(12, {arg i;1/(i+1)});
//~funcs = [{~loop1.playLoop(~steps.choose)}];
~loops.stop;
~loops = Routine{
  inf.do{
	~funcs.choose.value;
	~time.choose.rand.wait;
  }
}.play;

~tone = {Out.ar(1, SinOsc.ar(880))}.play;
~tone.free;

~loops.stop;
h = GranularSampler.new(start_note:4, start_ctl:8, out_bus:1, mpd_mode:1);
h = GranularSampler2.new(start_note:4, start_ctl:8, out_bus:1, mpd_mode:1);
h.free;
h.startPlay("granular_buffer_player_lfo");
~t = [0.125];
a ={ inf.do{
	h.startPlay("granular_buffer_player");
	~t.choose.wait;
  };}.fork

h.grain_pitches_([24, 32, 32])

a = Array.fill(20, {arg i; 2.pow(i)});
h.grain_pitches_(a.scramble)

h.startRecord;
h.stopRecord;
h.stopPlay;
~t = [0.5, 0.1];
~r = Routine.new{
  inf.do{
	h.updateCtl((4.rand)+8, 64.rand);
	~t.choose.rand.wait;
  }
}.play
~r.stop;

r= Routine.new{
  100.do{ arg i;
	i.postln;
	h.updateCtl(9, (i));
	0.1.wait;  
  }
}.play;

