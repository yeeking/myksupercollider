//a instrument that uses waveforms///////////////////
//first set rawfilepath to let Stk know where to look
{StkGlobals.ar(1,1,Platform.userExtensionDir ++"/SC3Plugins/StkInst/rawwaves")}.play;

{StkGlobals.ar(1,1,"/home/scratch/src/supercollider/sc3-plugins/external_libraries/stk/rawwaves")}.play;

s.boot;
//Define mandolin synthdef
(
SynthDef(\helpMandolin,{arg out=0,freq=220,gate=1,amp=1;
	var sig,env;
	env = EnvGen.kr(Env.asr(0,1,0.1),gate,doneAction:2);
	sig = StkInst.ar(Stk.at("Mandolin"),freq, gate, amp, 0.5)*env;
	Out.ar(out, sig.dup);
}).add;


SynthDef(\helpMandolin,{arg out=0,freq=220,gate=1,amp=1;
	var sig,env;
	env = EnvGen.kr(Env.asr(0,1,0.1),gate,doneAction:2);
	sig = StkInst.ar(5,freq, gate, amp, 0.5)*env;
	//StkInst.ar(
	Out.ar(out, sig.dup);
}).add;
)
)
Stk.at("Mandolin")

//Use it.
(

Pbind(
    \instrument, \helpMandolin,
    \dur , 0.25,
    \degree, Pseq([1,5,7+3], inf),
    \amp , Pseq([0.9,0.7,0.5],inf),
).play;
)