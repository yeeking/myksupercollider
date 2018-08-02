SuperDirt.start

s.boot;

SuperDirt.start


Stk

SynthDef("StkClarinet", {arg  freq=440,reedstiffness=64,noisegain=10,vibfreq=64,
				vibgain=10,breathpressure=64, gain=0.2,gate=1,bus=0,
				lag = 0.1,sloc=0,riset=0.2,decayt=0.2 ;

		var z,env;
		env  =  EnvGen.kr(Env.adsr(attackTime:riset, decayTime:0,sustainLevel:1,
						releaseTime:0.1,peakLevel:1),gate:gate, doneAction:0);

		z = StkClarinet.ar(
			//freq:Lag.kr(MouseX.kr, lag),
		freq:MouseX.kr(25, 2000),
			reedstiffness:reedstiffness,
		noisegain:MouseY.kr(0, 127),
			vibfreq:vibfreq,
			vibgain:vibgain,
		breathpressure:MouseY.kr(60, 96),
			trig:gate);
		Out.ar(bus, Pan2.ar(z,sloc)*env*gain);
	}).load(s);


Synth("StkClarinet", [\riset, 12]);
