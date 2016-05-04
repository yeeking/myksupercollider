s.kill;
(
o = Server.local.options;
o.numOutputBusChannels = 2;
o.numInputBusChannels = 4;
o.hardwareBufferSize = 64;
s = Server.local.boot;
)


~bd = MykBonk.new(audio_in:0, callback:{arg vel; ("bd"++vel).postln;});
~bd.callback = {arg vel; ("bd"++vel).postln;};

~sn = MykBonk.new(audio_in:1, callback:{arg vel; ("sn"++vel).postln;}, osc_id:31);
~sn.callback= {arg vel; ("sn"++vel).postln;};
~sn.setSens(0.01);

~t1 = MykBonk.new(audio_in:2, callback:{arg vel; ("sn"++vel).postln;}, osc_id:32);
~t1.callback= {arg vel; ("t1"++vel).postln;};

~t2 = MykBonk.new(audio_in:3, callback:{arg vel; ("sn"++vel).postln;}, osc_id:33);
~t2.callback= {arg vel; ("t2"++vel).postln;};


~bd.run;
~sn.run;
~t1.run;
~t2.run;

~t1.free;

~sn.callback = {arg vel;
	Synth("sn", [\vel, vel]);
};

~bd.callback = {arg vel;
	Synth("bd", [\vel, vel * 10]);
	~verb.set(\room, vel * 5);
	~verb.set(\mix, 0.5);
	~verb.set(\damp, [0.1, 0.5, 1.0].choose);
};

SynthDef("bd", {arg vel = 0.5;
	var c, l;
	l =vel * 5;
	c = Saw.ar(Line.kr(500, 50, 0.1));
	c = c * Line.kr(1, 0.1, l, doneAction:2);
	c = c + ClipNoise.ar(Line.kr(0.5, 0, Rand(l/5, l/3)));
	Out.ar(30, c);
	Out.ar([0, 1], c*0.25);
}).add;

SynthDef("bd", {arg vel;
	var c;
	c = SinOsc.ar(Line.kr(500, 50, 0.1));
	c = c * Line.kr(1, 0.1, 0.25, doneAction:2);
	Out.ar(30, c);
	Out.ar([0, 1], c*0.1);
}).add;

~verb = Synth("verb");
~verb.free;



SynthDef("verb", {|mix = 0.25, room = 0.15, damp = 0.5|
	  Out.ar([0, 1],
        FreeVerb.ar(
			      In.ar(30),
            mix,
            room,
            damp
        )
    );
}).add;


SynthDef("sn", {arg vel;
	var c, f;
	f = Rand(2, 20) * 100;
	c = SinOsc.ar(f) * Line.kr(0.25, 1.0, 0.1, doneAction:2);
	c = c + WhiteNoise.ar(Line.kr(0.5, 0, 0.25));
	c = c * Line.kr(1, 0.1, 0.5, doneAction:2);
	Out.ar(30, c);
	Out.ar([0, 1], c * 0.1 * vel * 5);
}).add;

SynthDef("sn", {arg vel;
	var c, f;
	f = Rand(2, 20) * 100;
	c = PMOsc.ar(f * 0.125, f, 10) * Line.kr(0.25, 1.0, 0.1, doneAction:2);
	c = c + WhiteNoise.ar(Line.kr(0.5, 0, 0.25));
	c = c * Line.kr(1, 0.1, 0.25, doneAction:2);
	Out.ar(30, c);
	Out.ar([0, 1], c * 0.1 * vel * 5);
}).add;

SynthDef("sn", {arg vel = 0.1;
	var c, f, l;
	l = vel * 5;
	f = Rand(1, 2.0) * 10;
	f = f * Line.ar(1, Rand(0.9, 0.01), Rand(l/2, l));
	c = PMOsc.ar(f * 0.25, f*5, 20) * Line.kr(0.25, 1.0, Rand(0.1, 1.0), doneAction:2);
	c = c + ClipNoise.ar(Line.kr(0.5, 0, Rand(l/5, l/3)));
	c = c * Line.kr(1, 0.1,  Rand(l/5, l), doneAction:2);
	Out.ar(30, c*2);
	Out.ar([0, 1], c * 0.1 * vel * 5);
}).add;


~sn.run;
~sn.free;




~s = Synth("on");
SynthDef("on", {
    var sig, chain, onsets, pips;
    // A simple generative signal
    //sig = LPF.ar(Pulse.ar(TIRand.kr(63, 75, Impulse.kr(2)).midicps), LFNoise2.kr(0.5).exprange(100, 10000)) * Saw.ar(2).range(0, 1);
    // or, uncomment this line if you want to play the buffer in
    //sig = PlayBuf.ar(1, d, BufRateScale.kr(d), loop: 1);
	  sig = SoundIn.ar(1);
//    chain = FFT(b, sig);
	  chain = FFT(LocalBuf.new(1024, 1), sig);

    onsets = Onsets.kr(chain, MouseX.kr(0,1), \rcomplex);

    // You'll hear percussive "ticks" whenever an onset is detected
    pips = WhiteNoise.ar(EnvGen.kr(Env.perc(0.001, 0.1, 0.2), onsets));
    Out.ar(0, Pan2.ar(sig, -0.75, 0.2) + Pan2.ar(pips, 0.75, 1));
}).add;