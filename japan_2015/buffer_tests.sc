s = Server.local.boot;

// load a sound from a file into a buffer

p = Platform.resourceDir +/+ "sounds/a11wlk01.wav";
b = Buffer.read(s, p);

// play the sound with start/ stop control

~play = Synth("play", [\buf, b.bufnum]);
~play.set(\trig, 1);
~play.set(\trig, 0);

SynthDef("play", {arg buf, trig=1;
	var chain, p_s, p_e;
	p_s = 0;
	p_e = BufFrames.ir(buf);
	chain = BufRd.ar(1, buf,
		Phasor.ar(
			rate:trig,
			start:p_s,
			end:p_e));
	Out.ar([0, 1], chain);
}).add;


// play the buffer through an FFT chain
~play.free;
~play = Synth("play_fft", [\buf, b.bufnum]);
~play.set(\trig, 1);
~play.set(\trig, 0);

SynthDef("play_fft", {arg buf, trig=1;
	var chain, p_s, p_e;
	p_s = 0;
	p_e = BufFrames.ir(buf);
	chain = BufRd.ar(1, buf,
		Phasor.ar(
			rate:trig,
			start:p_s,
			end:p_e));

	chain = FFT(LocalBuf(2048), chain);
    chain = IFFT(chain); // inverse FFT
	Out.ar([0, 1], chain);
}).add;

// play the buffer through an FFT chain with
// stop and start controls that trigger a magfreeze
// and pause the buffer at the same time
~play.free;
~play = Synth("play_fft", [\buf, b.bufnum]);
~fun = {inf.do{
	~play.set(\trig, 1);
	0.5.wait;
	~play.set(\trig, 0);
	1.0.wait;
}}.fork;
~fun.stop;
SynthDef("play_fft", {arg buf, trig=1;
	var chain, p_s, p_e;
	p_s = 0;
	p_e = BufFrames.ir(buf);
	chain = BufRd.ar(1, buf,
		Phasor.ar(
			rate:trig,
			start:p_s,
			end:p_e));
	chain = FFT(LocalBuf(2048), chain);
	chain = PV_MagFreeze(chain, 1-trig);
    chain = IFFT(chain); // inverse FFT
	Out.ar([0, 1], chain);
}).add;

// timestretch controlled by freq
~play.free;
~play = Synth("play_fft", [\buf, b.bufnum]);
~play.set(\freq, 40000)
SynthDef("play_fft", {arg buf, trig=1, freq = 100;
	var chain, p_s, p_e;
	p_s = 0;
	p_e = BufFrames.ir(buf);
	trig = Pulse.ar(freq).range(0, 1);
	chain = BufRd.ar(1, buf,
		Phasor.ar(
			rate:trig,
			start:p_s,
			end:p_e));
	chain = FFT(LocalBuf(2048), chain);
	chain = PV_MagFreeze(chain, trig);
    chain = IFFT(chain); // inverse FFT
	Out.ar([0, 1], chain);
}).add;


~on = MykBonk.new;
~on.callback = {arg vel; if (vel > 0.03, {vel.postln;})};
~on.run;

~fid = MykFiddle.new;
~fid.run;
~fid.callback = {arg vel; vel.postln};

~mc = MykMarkov.new;
~fid.callback = {arg vel; ~mc.addFreq(vel)};

~cl = MykClock.new;
~cl.run;
~cl.add(0, {Synth("sine", [\f, ~mc.nextFreq.midicps])}, [1]);
~cl.remove(0);
~mc.nextFreq;

~samp = MykSampler.new;
~samp.recExt(len:1);
~samp.recInt();
~samp.play(speed:1, pitch:1);
~samp.fx();


SynthDef("sine", {arg f = 440;
	var c, e;
	c = SinOsc.ar(freq:f, mul:Line.kr(0.25, 0, Rand(0.1, 0.45), doneAction:2));
	Out.ar([0, 1], c);
}).add;

Synth("sine");

//

s = Server.local.boot;

~on = MykBonk.new;
~on.run;
~bank = MykSounds;

~on.callback = {arg vel; ~bank.play(1, [vel])}

1.isNumber
\test.isNumber

a = [10, 9, 8, 7, 4];

a.wrapAt(1);

~q = QuarksGui.new

~pl = {inf.do{
	Synth("MykSounds_bass", [\p1, [21, 25, 22].choose,  \p2, 0.5, \p3, rrand(1, 20)]);
	[0.125, 0.25].choose.wait;
}}.fork;
~pl = {inf.do{
	Synth("MykSounds_bass", [\p1, [21, 25, 22].choose,  \p2, 0.5, \p3, rrand(1, 20)]);
	[0.125, 0.25].choose.wait;
}}.fork;

Synth("MykSounds_bd", [\p3, rrand(0.1, 1.0)])



47.midicps;

Synth("MykSounds_sn", [\p1, 2.0, \p2, 1]);

SynthDef("MykSounds_oh", {arg p1, p2, p3;
	var c, e, fs, noise, bf;
	bf = 500;
	fs = Array.fill(5, {arg i; (i + 1) * bf});
	noise = RHPF.ar(ClipNoise.ar(0.25), bf * 2, 0.25);
	e = EnvGen.kr(Env.perc(0.01, 0.5), doneAction:2);

	c = Array.fill(fs.size, {arg i;
		BHiPass4.ar(noise, fs[i], 0.005 + (LFDNoise1.kr(Rand(5.0, 10.0)).range(-0.00025, 0.0025)));
		//SinOsc.kr(LFDNoise1.kr(0.5).range(1.0, 2.0)).range(0.01, 0.05));
	}).mean;
	Out.ar([0, 1], c * e);
}).add;
Synth("MykSounds_oh", [\p1, 2.0, \p2, 0.2]);

SynthDef("MykSounds_fm", {arg p1 = 22, p2 = 0.5, p3=0.5;
	var c1, c, e, freq, muls,  scalar;
	e = EnvGen.kr(Env.perc(0.01, Rand(0.1, 1.0)), doneAction:2);
	freq = p1.midicps;
	muls = [0.125, 0.25, 0.5, 1.5, 2, 4];
	scalar = Select.kr(p2 * muls.size, muls);
	c1 = PMOsc.ar(freq,freq * scalar, Rand(2.0, 10.0));
	c = SinOsc.ar(freq + (c1 * (freq * p3)));
	c = Normalizer.ar(c, 0.25);
	Out.ar([0, 1], c * e);
}).add;

Synth("MykSounds_fm", [\p1, rrand(32, 64), \p2, rrand(0, 1), \p3, rrand(1, 5)]);


SynthDef("MykSounds_bd", {arg p1=0.5, p2 = 0.5, p3 = 0.5;
	var c, e, bf;
	bf = (p1.midicps) * 0.5;
	e = EnvGen.kr(Env.perc(0.01, 0.25), doneAction:2);
	c = SinOsc.ar(freq:Line.kr(bf * 1.5, bf * 0.5, p1/5));
	c = c + RLPF.ar(Pulse.ar(bf), bf * 5 * p3, p2*4);
	c = Normalizer.ar(c, 0.5);
	Out.ar([0, 1], c * e);
}).add;

~s = MykSounds.new;
~s.play(rrand(0, 4), rrand(24, 64));


~cl = MykClock.new;
~cl.add(1, {~s.play(\bd, 32)}, [1]);
~cl.add(4, {~s.play(\oh, rrand(1, 127.0), 0.2, 0.1)}, [0.25], [1]);

~cl.add(2, {~s.play(\sn, 5)}, [2, 2, 1.5, 1.5]);
~cl.add(3, {~s.play(\fm, ~f, rrand(0.5, 0.5),rrand(0.1, 0.25))}, [0.25], [0.5]);

~fs = Array.fill(10, {arg i; i + 45});
~f = ~fs[1];
~cl.add(5, {
	~f = ~fs[1];
	[
		{~fs = ~fs.rotate(-1);},
		{~fs = ~fs.mirror();}
	].choose.value;

}, [0.25], [1]);

~fs.mirror;









~cl.remove(3)

)



~cl.run;
a = -10;
max(0, min(a, 1));



~rev.free;
~rev = Synth("del");
~rev2 = Synth("rev");
~rev2.set(\onoff, 1 );
~rev2.set(\onoff, 0);
~rev.set(\p2, 1.0);
~rev.set(\p1, 0.001);
~cl.add(2, {
	~rev.set(\onoff, 1);
	{~rev.set(\onoff, 0);}.defer(0.125);
	~rev.set(\p1, ~dt);
	~dt = ~dt + 0.001;
}, [2]);
~cl.remove(2);
~dt = 0.0001;
~rev.set(\p2, 1.0);


s = Server.local.boot;
~cl = MykClock.new;
~cl.run;
~cl.add(1, {Synth("plip");
//	~rev.set(\p2, rrand(0.01, 0.25));
}, [0.5]);
Synth("plip");


SynthDef("plip", {
	var c;
	c = ClipNoise.ar(Line.kr(0.5, 0, 0.005, doneAction:2));
	Out.ar(30, c);
}).add;

SynthDef("rev", {arg onoff=1, p1 = 0.5, p2 = 0.5;
	var in, chain, dt, mix;
	onoff = max(0, min(onoff, 1));
	p1 = Lag.kr(p1);
	p2 = Lag.kr(p2);
	onoff = Lag.kr(onoff);
	dt = max(0, min(p1, 1));
	dt = dt * 0.01;
	in = In.ar(30) ;
	chain = in * (1 - onoff);
	4.do{
		chain = AllpassC.ar(chain, 0.1, Rand(0.001, 0.0015) + dt, p2 * 5.0);
	};
	Out.ar([0, 1], chain + in);
}).add;


SynthDef("del", {arg onoff=1, p1 = 0.125, p2 = 0.5;
	var in, chain, dt, mix, chain2;
	onoff = max(0, min(onoff, 1));
	p1 = Lag.kr(p1, LFDNoise1.kr(0.1).range(0.1, 0.5));
	p2 = Lag.kr(p2);
	onoff = Lag.kr(onoff);
	dt = max(0, min(p1, 1));
	dt = dt * 0.45;
	in = In.ar(30) ;
	chain = in * onoff;
	chain = CombC.ar(chain, 0.5, 0.05+ dt, p2 * 5.0);
	chain2 = chain + CombC.ar(chain, 0.5, 0.1+ dt, p2 * 7.0) * 0.5;
	Out.ar(0, [
		chain + (0.5 * in),
		chain2 + (0.5 * in)
	]);
}).add;

(
var ind;
ind = \del;
switch (ind,
	\rev, {"reverb".postln;},
	\del, {"delay".postln;},
);

)



s = Server.local.boot;




s = Server.local.boot;
~cl = MykClock.new;
~cl.run;
~sn = MykSounds.new;
~cl.add(1, {~sn.play(\bd,54, 1.0, 0.5)}, [1]);

~cl.add(2, {~sn.play(\oh,rrand(1.0, 126), rrand(0.1, 1.0), 0.5)}, [2]);

~cl.add(3, {~sn.play(\bass,~fs[0].cpsmidi - [48 ].choose, 0.25, 1); ~fs = ~fs.rotate(-1)}, [0.25], [1]);
~cl.add(6, {~sn.play(\bass,~fs[1].cpsmidi - [ ].choose, 0.25, 1); ~fs = ~fs.rotate(-1)}, [0.25], [1]);

~cl.add(5, {~fs = ~fs.rotate(-1)}, [0.5], [0.25]);
~fs = ~fs.sort;


~cl.remove(5);

~fs = MiscFuncs.getScale(\a, \mel_minor);
~fs.choose;
~fs = Array.fill(3, {~fs.choose});

~sn.fx(\del, onoff:0);

~cl.add(4, {
//	~sn.fx(\del, p1:[0.01, 0.0001].choose);
	~sn.fx(\del, onoff:1);
	{~sn.fx(\del, onoff:0)}.defer(0.25);
}, [2]);

~cl.add(5, {
	~sn.fx(\del, p1:rrand(0.001, 0.005));
}, [1], [0.5]);

~sn.fx(\del, p1:0.001);




