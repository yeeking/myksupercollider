s.boot;

(
s.boot.doWhenBooted(

SynthDef("bd", {arg l = 0.2;
	var c, f;
	c = ClipNoise.ar(0.2);
	c = c * Line.kr(1, 0, 0.01);
	c = c+ CombC.ar(c, 0.1, 0.01, Rand(0.1, 0.125));
		f = Line.kr(200, 50, 0.1);
	c = c + SinOsc.ar(f, mul:0.4);
	Out.ar([0, 1], c* Line.kr(1, 0, l, doneAction:2));
}).add;

SynthDef("sn", {arg harsh = 0.01;
	var c, f;
	c = ClipNoise.ar(0.75);
	c = c * Line.kr(1, 0, harsh);
	c = c+ CombL.ar(c, 0.1, 0.025, Rand(0.6, 1.0));
	f = EnvGen.kr(Env.new(
		levels:[200, 2000, 500],
		times:[0.01, 0.01]) );
	//Line.kr(1000, 750, 0.05);
	c = c + Line.kr(1, 0, 0.2) * SinOsc.ar([f*0.95, f*1.05], mul:0.4);
	Out.ar(0, c* EnvGen.kr(Env.perc(0.01, 0.4), doneAction:2));
}).add;

SynthDef("hat", {arg lev = 0.5;
	var c;
	c = ClipNoise.ar(lev);
		c = RHPF.ar(c, Rand(4000, 5000),
			rq:0.08);
		c = c * EnvGen.kr(Env.perc(0.001, 0.1),  doneAction:2);
	//Out.ar(30, c*0.01);
	Out.ar([0, 1], c*0.07);
}).add;

SynthDef("test", {arg f=440;
	var c, e;
	c = Saw.ar(freq:f*0.25);
	c = c + Pulse.ar(freq:(f*2), mul:0.5, width:Rand(0.1, 1));
	c = MoogFF.ar(c, Line.kr(100, MouseX.kr(100, 5000), 0.01));
	c = c * Line.kr(0.5, 0, 0.5, doneAction:2);
	Out.ar(30, c);
}).add;

SynthDef("bass", {arg f=440;
	var c, e;
	c = Saw.ar(freq:f,mul:0.25);
	c = MoogFF.ar(c, Line.kr(5000, 3000, 0.21));
	c = c + PMOsc.ar(carfreq:f * 0.5, modfreq:f, pmindex:MouseY.kr(0.1, 10), mul:0.25);

	e = EnvGen.kr(Env.perc(0.01, 0.5), levelScale:0.5, doneAction:2);
	Out.ar([0, 1], c*e);
}).add;


SynthDef("verb", {
	var c;
	c = In.ar(30);
	c = FreeVerb2.ar(c, c, room:10, damp:0.01);
	Out.ar(0, c);
}).add;

)
)
(

~v = Synth("verb");
//~v.free;

~cl = MykClock.new;
~cl.run;
~notes = MiscFuncs.getScale('c', 'nat_major').cpsmidi;

~ints = [-1, -2, -1, -5];

~fast_bass = {
~cl.add(2, {
	Synth("bass", [\f, (~notes[0] - 36).midicps]);
}, [0.25, 0.5]);
};

~slow_Bass = {
	~cl.add(2, {
	Synth("bass", [\f, (~notes[0] - 36).midicps]);
	}, [1/3/0.5]);
};


~cl.add(3, {
	[~slow_bsas, ~fast_bass].choose.value;
});

~cl.add(0,
	{
		Synth("test", [\f, (~notes[0]+[0, 12].choose).midicps]);
	},
	[0.25], [0.5]);
~cl.add(1, {
	~ints = ~ints.rotate(-1);
	~notes = ~notes.rotate(~ints[0]);
}, [1], [1]);

/*~cl.add(1,
	{
		Synth("test", [\f, (~notes[0]+6).midicps]);
		~notes = ~notes.rotate(-1);
	},
	[0.25], [1], 0.125);*/

~cl.add(5, {
	Synth("hat");
}, [0.25]);
~cl.add(6, {
	Synth("bd");
}, [1]);
~cl.add(7, {
	Synth("sn");
}, [2, 2, 2, 2, 1.75, 0.25, 2, 2], [1,1,1,1,1,0,1,1]);
)


(
// electro
~cl.add(5, {
	Synth("hat", [\lev, 1.0]);
}, [0.25, 0.25, 0.25, 0.125, 0.125], [1]);
~cl.add(6, {
	Synth("bd", [\l, 1.0]);
}, [1, 0.75, 0.25, 0.5, 0.5, 0.5,0.5], [1, 1, 0, 1, 0,1, 1]);
~cl.add(7, {
	Synth("sn", [\harsh, 0.1]);
}, [2], [1]);




~cl.add(5, {
	Synth("hat", [\lev, 1.0]);
}, [0.25, 0.25, 0.25], [1]);

)


~cl.add(6, {
	Synth("bd", [\l, 1.0]);
}, [1, 0.75, 0.25, 0.5, 0.5, 0.5,0.5], [1, 1, 0, 1, 0,1, 1]);


~cl.add(6, {
	Synth("bd", [\l, 0.1]);
}, [0.25], [1, 1, 0, 1, 0,1, 1]);


~cl.add(7, {
	Synth("sn", [\harsh, 0.01]);
}, [0.125], [1]);


~cl.remove(5);
~cl.remove(6);
~cl.remove(7);


~cl.remove(5);


