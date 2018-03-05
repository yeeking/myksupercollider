include("SuperDirt")



SuperDirt.start

Super

SynthDef("supersaw", {arg freq=440;
	var c;
	c = Line.ar(0.1, 0, 0.1, doneAction:2) * Saw.ar(freq );
	Out.ar([40, 1], c);
}).add;

SynthDef("supersaw", {arg freq=440;
	var c;
	c = Line.ar(0.1, 0, 0.1, doneAction:2) * PMOsc.ar(freq*2, freq*0.5, 5);
	Out.ar([40, 1], c);
}).add;


~v.free;

~v = Synth("rev");
SynthDef("rev", {
	var c;
	c = SoundIn.ar(40);
	4.do{
		c = AllpassC.ar(c, 0.03,0.02.rand, 10.0);
	};
	Out.ar([0, 1], c);
}).add;