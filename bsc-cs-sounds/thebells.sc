{ Klank.ar(`[[800, 1071, 1153, 1723], nil, [1, 1, 1, 1]], Impulse.ar(2, 0, 0.1)) }.play;

{ Klank.ar(`[[800, 1071, 1353, 1723], nil, [1, 1, 1, 1]], Dust.ar(8, 0.1)) }.play;

{ Klank.ar(`[[800, 1071, 1353, 1723], nil, [1, 1, 1, 1]], PinkNoise.ar(0.007)) }.play;
{ Klank.ar(`[[800*1.5, 1071*1.5, 1353*1.5, 1723*1.5], nil, [1, 1, 1, 1]], PinkNoise.ar(0.007)) }.play;

// a nice couple of bells

s.boot;
{ Klank.ar(`[[200/3, 671/3, 1153/3, 1723/3], nil, [1, 1, 1, 1]], PinkNoise.ar([0.007, 0.007]))  * 0.1}.play;

{ DynKlank.ar(`[[200/3, 671/3, 1153/3, 1723/3]*Line.kr(0.5, 0.75, 7.5), nil, [1, 1, 1, 1]], PinkNoise.ar([0.007, 0.007]))  * 0.1}.play;

{ Klank.ar(`[[200*8, 671*2, 1153*8, 1723*8], nil, [1, 1, 1, 1]], PinkNoise.ar([0.1*Dust.ar(4), 0.1*Dust.ar(4)])) }.play;





(
// set them from outside later:
SynthDef('help-dynKlank', {arg gate = 0;
    var freqs, ringtimes, signal, stim, env;
	//stim = PinkNoise.ar(0.001) + Impulse.ar(2, 0, 0.1);
	//stim = Impulse.ar(2, 0, 0.1);
/*	stim = EnvGen.ar(
		Env.new([0, 1.0, 0], [0.05, 0], 5), gate:Impulse.kr(0.5));*/
	//stim = EnvGen.kr(Env.perc(0.01, 0.5), gate:Impulse.kr(0.5));
	env = EnvGen.kr(Env.perc(0.01, 0.5), gate:gate);
	stim = env * PinkNoise.ar(0.007);
    freqs = Control.names([\freqs]).kr([800, 1071, 1153, 1723]);
	//freqs = freqs * MouseX.kr;

    ringtimes = Control.names([\ringtimes]).kr([1, 1, 1, 1]);
	signal = DynKlank.ar(`[freqs, nil, ringtimes ], stim);


    Out.ar(0, signal);
}).add;
)


b = Array.fill(6, {
	a = Synth('help-dynKlank');
});
b.do{arg it;
	it.set()
}


(
a = Synth('help-dynKlank');
b = Synth('help-dynKlank');
c = Synth('help-dynKlank');

)
(
a.setn(\freqs, [200/4, 671/4, 1153/4, 1723/4]);
a.setn(\ringtimes, [10.0, 5, 2, 2.0]);

x = 200


b.setn(\freqs, [200*4, 671*4, 2042*4, 2322*4]);

b.setn(\freqs, [200*4, 671*4, 1153*4, 1723*4]);
b.setn(\ringtimes, [1.0, 0.5, 0.5, 0.1]);
c.setn(\ringtimes, [1.0, 0.5, 0.5, 0.1]);

c.setn(\freqs, [200*4*1.5, 671*4*1.5, 1153*4*1.5, 1723*4*1.5]);

c.setn(\ringtimes, [5.0, 0.5, 0.5, 0.1]);


)
(
var p = {
	inf.do{
		a.set(\gate, 1);
		0.01.wait;
		a.set(\gate, 0);
		1.0.wait;
	};
}.fork;
var q = {
	inf.do{
		b.set(\gate, 1);
		0.01.wait;
		b.set(\gate, 0);
		rrand(0.125, 0.5).wait;
	};
}.fork;

var r = {
	inf.do{
		c.set(\gate, 1);
		0.01.wait;
		c.set(\gate, 0);
		rrand(0.125, 0.5).wait;
	};
}.fork;



p.free;

)
(
a.set(\gate, 1);
{a.set(\gate, 0);
}.defer(0.1);
)

a.setn(\freqs, [200/4, 671/4, 1153/4, 1723/4]);
a.setn(\ringtimes, [10, 5, 2]);

a.setn(\freqs, Array.rand(4, 400, 200));
a.setn(\freqs, [100, 100.1, 200, 400]);

a.setn(\ringtimes, Array.rand(10, 0.2, 2.0) );

// this is the shit
s.boot;

(
SynthDef("hi_bell", {
	var c;
	c = Klank.ar(`[[200*8, 671*2, 1153*8, 1723*8],
		nil, [1, 1, 1, 1]],
	PinkNoise.ar([0.1*Dust.ar(4), 0.1*Dust.ar(4)])) ;
	Out.ar([0, 1, 30], c);
}).add;
SynthDef("lo_bell", {arg f = 200;
	var c, e;
	c = DynKlank.ar(`[[f/3, 671/200 * f * 3, 1153/200*f/3, 1723/200*f*3], nil, [1, 1, 1, 1]], PinkNoise.ar([0.007, 0.007]));
	e = EnvGen.kr(Env.new([1, 1, 0], [3.0, 2.0], -4), doneAction:2);
	Out.ar([0, 1, 30], c*0.1*e);
}).add;

)

s.boot;
~hi_bell = Synth("hi_bell");
~hi_bell.free;
~lo_bell = Synth("lo_bell");
~lo_bell.free;

(
SynthDef("bell_trig", {arg out = 40;
	var c, e;
	e = EnvGen.kr(Env.perc(0.001, 0.01), doneAction:2);
	c = PinkNoise.ar(0.01);
	Out.ar(out, e*c);
}).add;

SynthDef("trig_bell", {arg trig = 40, freq = 200;
	var c;
	c = DynKlank.ar(`[[freq, 671.0/200*freq, 1153.0/200 * freq, 1723.0/200*freq],
		nil, [5.0, 2.5, 2.0, 1.5]],
	In.ar(trig)) ;
	Out.ar([0, 1, 30], [c*0.5, c*0.5, c*0.25]);
}).add;
)
(
~freq = 70.midicps;
~freqs = [~freq/1, ~freq*9/8.0, ~freq*3/2.0, ~freq*27/16, ~freq*4/5];
//~freqs = [~freq.midicps, (~freq+2).midicps, (~freq+3).midicps, (~freq + 6).midicps];

~bells = Array.fill(~freqs.size, {arg i;
	Synth("trig_bell",
		[\freq, ~freqs[i], \trig, i+40]);
});
)
~bells.do{arg b; b.free};

~lo_bell = Synth("lo_bell", [\f, ~freq/4]);
Synth("bell_trig", [\out, [40, 41, 42, 43].choose]);





// one of these trigged by the incoming OSC, but also going through spectral shit.



// then some buzz electricity sounds at the end
(
SynthDef("lekky", {arg freq = 500;
	var c, fs, sel, mf, cf, cfs, cf_sel, spd, mod_ind;
	spd = 1.5;
	cfs = [freq, freq*1.25, freq*4, freq*10];
	cf_sel = LFDNoise0.kr(spd)*(cfs.size+1);
	cf = Select.kr(cf_sel, cfs);
	fs = [0.25, 0.5, 1.25, 6.75];
	sel = LFDNoise0.kr(spd)*(fs.size +1);
	mf = Select.kr(sel, fs);
	mod_ind = SinOsc.ar(
		SinOsc.kr(1).range(1.0, 4)).range(5, 20);
	c = PMOsc.ar(cf, cf*mf, mod_ind);
	Out.ar([0, 1, 30], c*0.025*Line.kr(1, 0, 3.0, doneAction:2))
}).add;
)

l = Synth("lekky");
l.free;










