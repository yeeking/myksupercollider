s.boot;
call
~s= {Saw.ar([200, 300])}.play;
~s.free;

(
~on = MykBonk.new(1);
~freqs = [20];
~on.callback = {arg amp;

	Synth("stab", [\f, ~freqs[0].midicps,\len, min(amp * 10, 0.25) ]);
	~freqs = ~freqs.rotate(-1);
	~freqs.postln;
	if (0.5.coin, {
		~freqs = ~freqs +12;
		if (~freqs.maxItem > 80, {~freqs = ~freqs -12});
	});

	if (0.05.coin, {
		var start;
		start = rrand(20, 40);
		~freqs = Array.fill(rrand(2, 7), {arg i;
			start + (i) + [-2, 1, 2, 3, 4, -3, 5].choose;
		});
		if (0.5.coin, {~freqs = ~freqs.sort});
		//if (0.5.coin, {~freqs = ~freqs.shuffle});
		~v.set(\p, rrand(0.01, 1.0));
	});
};

SynthDef("stab", {arg f=220, len=0.5;
	var e, c;
	e = Line.kr(0.25, 0, len, doneAction:2);
	c = Array.fill(10, {arg i;
		var lfo;
		lfo = Line.kr(0, 1, len/5) * SinOsc.kr(LFDNoise1.kr(0.1).range(0.1, 5)).range(f/100, f/10);
		SinOsc.ar(f * (i+2) + lfo);
	}).mean;
	c = c + Impulse.ar(5);
	c = c + Pulse.ar([f, f*1.5], mul:0.5, width:Line.kr(0.75, 0.5, len/2)).mean;
	Out.ar(30, c*e);
}).add;

SynthDef("verb", {arg p=0.5;
	var c1, c2;
	p = max(0.1, p);
	p = Lag.kr(p, LFDNoise1.kr(0.1).range(1.0, 10.0));
	c1 = In.ar(30);
	c2 = c1;
	4.do{
		c1 = AllpassC.ar(c1, 0.05, 0.05.rand * p, 10.0);
	};
	4.do{
		c2 = AllpassC.ar(c2, 0.05, 0.05.rand * p, 10.0);
	};

	Out.ar(0, [c1, c2]);
}).add;

{
	~on.run;
	~v = Synth("verb");
}.defer(2);

)


~v = Synth("verb");
~v.free;


// some stuff to process a live kit coming
// in on a single mic

// onset detetor and pitch tracker
~on = MykBonk.new(1);
~on.run;
~on.setSens(0.6);
~f = MykFiddle.new(audioInBus:2);
~f.run;
~last_f = [1, 1, 1, 1];
// maintain last few freqs seen
~f.callback = {arg f;
	~last_f[0] = f;
	~last_f = ~last_f.rotate(-1)
};
~last_f.mean.postln;
// so last_f is the last frequency seen...
~on.callback = {arg v;
	var f;
	f = ~last_f[0];
	f.postln;
	if (f < 36, {
		"bd".postln;
	});
	if (f == 47, {
		"lt".postln;
	});
	if (f == 60, {
		"sn".postln;
	});
};

~s = {Saw.ar(freq:[220, 330])}.play;
~s.free;

~samp = LiveSamplerFX.new(in_bus:1);
~samp.run;
~samp.sample;
~samp.playSample(0);

~on.callback = {
	~samp.playSample(8.rand);
	if(0.25.coin, {~samp.sample;});
};

[1,2,3].mean


s.boot;
~c = CombBank(inBus:2);
~c.run;
~on = MykBonk.new(1);
~on.run;
~c.setLag(0.1);
~c.setDecay(0.1);
~c.free;
);

SynthDef("low", {arg f;
	var c, e;
	e = Line.kr(0, 1, Rand(0.1, 0.5));
	c = PMOsc.ar([f, f/2], [f/4, f*0.125], e * Rand(0.1, 10));
	c = c*Line.kr(0.25, 0, Rand(4.0, 20.0),  doneAction:2);
	Out.ar([0, 1], c*e);
}).add;

SynthDef("low", {arg f;
	var c, e, e2;
	e2 = EnvGen.kr(Env.perc(Rand(0, 10)+10, Rand(0, 10)+10, level:0.01), doneAction:2);
	e = Line.kr(0, 1, Rand(5.0, 0.0));

	c = PMOsc.ar([f, f/2], [f/4, f*0.125], e * Rand(0.1, 10));
	Out.ar([0, 1], (c*e2));
}).add;

SynthDef("low2", {arg f;
	var c, e, e2, e3;
	e2 = EnvGen.kr(Env.perc(Rand(0, 10.0)+10, Rand(0, 10.0)+10, level:0.01), doneAction:2);
	e = Line.kr(0, 1, Rand(5.0, 0.0));
	e3 = LFDNoise1.kr(0.1).range(f, f + f/10);
	c = PMOsc.ar([f+e3, f/2], [f/4, f*0.125], e * Rand(0.1, 10));
	Out.ar([0, 1], (c*e2));
}).add;
SynthDef("low3", {arg f;
	var c, e, e2, e3, len;
	len = 2.0;
	e2 = EnvGen.kr(Env.perc(Rand(0, len)+10, Rand(0, len)+10, level:0.01), doneAction:2);
	e = Line.kr(0, 1, Rand(len, 0.0));
	e3 = Line.kr(f/10, f/2, Rand(0, len) + 5);
	c = PMOsc.ar([f-e3, f/2], [f/4, f*0.125], e * Rand(0.1, 10));
	Out.ar([0, 1], (c*e2));
}).add;

{20.do{arg i;
	Synth("low3", [\f, rrand(5000, 7000)]);
	0.25.wait;
}}.fork;

~on.setSens(0.8);

~on.callback = {};

{20.do{arg i;
	Synth("low", [\f, rrand(i*20,i*40)]);
	0.25.wait;
}}.fork;


};


~f.free;
~on.run;

~on.free;
