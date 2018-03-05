SuperDirt.start;


SynthDef("rev", {
	var c;
	c = SoundIn.ar([0, 1]);
	4.do{
		c = AllpassC.ar(c, 0.005, rrand(0.001, 0.005), 15);
	};
	Out.ar([0, 1], c*0.25);
}).add;

SynthDef("del", {
	var c, c1, c2;
	c = SoundIn.ar([0, 1]);
	c = PitchShift.ar(c, 0.2,  LFDNoise1.kr(0.1).range(0.1, 0.75));
	c1 = c;
	c2 = c;
	c1 = CombC.ar(c, 0.5, LFDNoise0.kr(0.1).range(0.1, 0.5), 10.0);
	c2 = CombC.ar(c, 0.5, LFDNoise0.kr(0.1).range(0.1, 0.5), 10.0);

	Out.ar(0, [c1, c2]*0.25);
}).add;


~rev.free;
~del.free;
~rev = Synth("rev");
~del = Synth("del");



