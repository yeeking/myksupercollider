// my drum sounds
~dir = "/home/matthew/Audio/sounds/myk/";
~notes = [36, 37, 44, 42, 40, 38, 46, 34]; 
//~notes = [36, 40, 38, 46, 51, 32]; 
~files = [
[~dir++"sn_rim_comp.wav", ~dir++"sn_mid_fat3_comp.wav", ~dir++"sn_mid_tough_comp.wav"], 
[~dir++"bd_hev2_comp.wav",~dir++"bd_hev_comp.wav"],
[~dir++"Cl_HH_01.wav", ~dir++"Cl_HH_02.wav"], 
[~dir++"Rd_Cymb_01.wav"],
[~dir++"Szl_Cym_02.wav", ~dir++"Szl_Cym_01.wav"],
[~dir++"Flr_Tom_01.wav", ~dir++"Mid_Tom_01.wav"], 
[~dir++"Hi_Tom_01.wav"], 
[~dir++"sn_edge_pang2_comp.wav", ~dir++"sn_rim_ring_comp.wav"]
];
~mix = [0.5, 0.5, 0.5, 0.1, 0.1, 0.5, 0.5, 0.5];
~kit = DrumKit.new(0, ~notes, ~files, ~mix, out_bus:0);
 ~kit.mix_(~mix);
~kit.free;

~verb.free;
~delay.free;

~dir = "/home/matthew/Audio/sounds/amen/";
~dir2 = "/home/matthew/Audio/sounds/TR808/";
//~notes = [36, 37, 38, 40, 42, 44, 46, 34]; 
~notes = [36, 37, 38, 40, 42, 44, 46, 34]; 
//~notes = [36, 40, 38, 46, 51, 32]; 
~files = [
[~dir++"amen_sn2.wav", ~dir++"amen_sn1.wav"], 
[~dir++"amen_bd2.wav", ~dir++"amen_bd4.wav"], 
[~dir++"amen_hh1.wav"],
[~dir++"amen_crash.wav"], 
[~dir2++"2_808_Snare_lo1.wav"], 
[~dir2++"1_808_Kick_short.wav"], 
[~dir2++"3_808_Hat_closed.wav"], 
[~dir2++"808_Lo_Tom.wav", ~dir2++"808_Hi_Tom.wav"]
];
~mix = [0.5, 0.5, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25];
~kit2 = DrumKit.new(0, ~notes, ~files, ~mix, out_bus:0);
~kit2.mix_(~mix)
~kit2.free;
SUPER COLLIDER

~dir = "/home/matthew/Audio/sounds/allpass/";
~notes = [36, 37, 38, 40, 42, 44, 46, 34]; 
//~notes = [36, 40, 38, 46, 51, 32]; 
~files = [
[~dir++"allpass_sn_tight.wav", ~dir++"allpass_sn_ringing.wav"], 
[~dir++"allpass_bd_flap_sm.wav", ~dir++"allpass_bd_low.wav", ],
[~dir++"allpass_hat_high.wav"], 
[~dir++"allpass_springy_sm.wav"], 
[~dir++"allpass_springy.wav"], 
[~dir++"allpass_tom.wav"], 
[~dir++"allpass_little_snare_sm.wav"], 
[~dir++"allpass_big_wash.wav"]
];

~mix = [1, 1, 1, 0.5, 0.5, 1, 1, 1];
~kit = DrumKit.new(0, ~notes, ~files, ~mix, out_bus:1);
~kit.free;
~kit.mix_([0, 0, 0, 0, 0, 0, 0, 0]);
//~kit.free;
~times = [0.12];
60/0.12
~notes = [36, 37, 38, 40, 42, 44, 46, 34, ]; 
~notes = [46, 38];
~play = Routine{
  inf.do{
	~kit.playSound(~notes[0], 127);
	~notes = ~notes.rotate(-1);
	~times.choose.wait;
  };
}.play;
~play.stop
Routine{
  
}.play;
FM SOUNDS 

~dir = "/home/matthew/Audio/sounds/fm/";
~notes = [36, 37, 38, 40, 42, 44, 46, 34]; 
//~notes = [36, 40, 38, 46, 51, 32]; 
~files = [
[~dir++"alien_tune_in.wav"],
[~dir++"greetings.wav"],
[~dir++"mindray.wav"],
[~dir++"presence.wav"],
[~dir++"reminder.wav"],
[~dir++"fm.wav"],
[~dir++"wggler2.wav"],
[~dir++"wash.wav"]
];
~mix = [1, 1, 1, 0.5, 0.5, 1, 1, 1];
~kit2 = DrumKit.new(0, ~notes, ~files, ~mix, out_bus:1);
~kit2.free;


~prime = SequencerUnitSamplerPrime.new(
  // prime, stop, step, newseq, samplers 1, 2, 3
  //respNoteArray:[44, 82, 46, 46, 36, 38,40, 48, 45], 
  respNoteArray:[34, 40, 34, 34, 36, 37, 38, 42, 44, 43],
  polyphony:5, 
  // fx synth options, looper synth options
  //synthType:[['SU_verb'], ['SU_sampleLooperNoise']],  
  synthType:[['SU_verb'], ['SU_sampleLooperSpring']],  
  respCtlArray:[0],
  respChannel:0,
  seqLength:100, 
  outBus:[0], 
  fxBus:20,
  ctlBus:22, 
  inBus:1, 
  // tuned mode?
  modeSwitch:0, 
  musicalKeys:['g'],
  musicalScales:['dorian'], 
  noteRange:[0.125, 0.25]
);

~prime.free;