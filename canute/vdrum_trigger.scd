s.kill;
(
o = Server.local.options;
o.numOutputBusChannels = 8;
o.numInputBusChannels = 8;
o.hardwareBufferSize = 64;
s = Server.local.boot;

s.doWhenBooted{
	~snd = MykSounds.new;
	~fx_states = [0,0,0,0];
	~funcs = [
		{arg vel;
			(0.asString++":"++vel).postln;
				Synth("bd", [\vel, vel * 10]);
//			~snd.play(\bd, freq:vel * 127.0, p1:vel);
		},
		{arg vel;
			(1.asString++":"++vel).postln;
			//~snd.play(\sn, p1:vel);
		Synth("sn", [\vel, vel]);

		},
		{arg vel;
			(2.asString++":"++vel).postln;
			~snd.play(\ch, p1:vel);
			~snd.fx(\del, onoff:~fx_states[1], p1:rrand(0.1, 1.0), p2:rrand(0,1, 1));
			~fx_states[1].postln;
			~fx_states[1] = 1 - ~fx_states[1];
		},
		{arg vel;
			var onoff = 0;
			(3.asString++":"++vel).postln;
			if (vel < 0.5, {onoff = 1;});
			~snd.fx(\rev, onoff:onoff, p1:rrand(0.1, 1.0), p2:vel);
			~snd.fx(\fft, onoff:onoff, p2:rrand(0.01, 1.0), p1:vel, p3:rrand(0.1, 1.0));
			~snd.fx(\del, onoff:0, p2:0.05, p1:0.05);
		}
	];
	~ons = Array.fill(4, {arg i;
		MykBonk.new(audio_in:i, sens:0.01, callback:{arg vel; ~funcs[i].value(vel)}, auto_calibrate:true, auto_run:true);
	});

	SynthDef("bd", {arg vel = 0.5;
		var c, l, f;
		l = min(vel, 0.5);
		f = Line.kr(200 + (vel*50), 50, l);
		c = Saw.ar(f) + SinOsc.ar([f / 2, f/4]).mean;
		//c = c * Line.kr(1, 0.1, l, doneAction:2);
		c = c * EnvGen.kr(Env.perc(0.01, l), doneAction:2);
		c = c + ClipNoise.ar(Line.kr(0.5, 0, l/4, doneAction:2));
		Out.ar([40, 41], c);
		Out.ar([0, 1], c*0.01);
	}).add;
	SynthDef("sn", {arg vel = 0.1;
		var c, f, l;
		l = vel * 5;
		f = Rand(1, 2.0) * 10;
		f = f * Line.ar(1, Rand(0.9, 0.01), Rand(l/2, l));

		c = PMOsc.ar(f * 0.25, f*4.5, 20) * Line.kr(0.25, 1.0, 0.01);
		c = c + ClipNoise.ar(Line.kr(0.5, 0, l*0.1));
		c = c + RLPF.ar(WhiteNoise.ar(1), freq:Line.kr(0.1, 6000, l/5), rq:l);
		c = c * Line.kr(1, 0.1,  Rand(l/5, l), doneAction:2);
		Out.ar([40, 41], c);
	Out.ar([0, 1], c * 0.1 * vel);
}).add;
	SynthDef("bd", {arg vel = 0.1;
	var c, f, l;
	l = vel * 2;
	f = Rand(1, 2.0) * 10;
	f = f * Line.ar(1, Rand(0.9, 0.01), Rand(l/2, l));
	c = PMOsc.ar(f * 0.25, f*1.5, 20) * Line.kr(0.25, 1.0, 0.01);
	c = c + ClipNoise.ar(Line.kr(0.5, 0, l*0.1));
	c = c * Line.kr(1, 0.1,  Rand(l/5, l), doneAction:2);
		Out.ar([40, 41], c);
	Out.ar([0, 1], c * 0.1 * vel);
}).add;

}
)


~snd.fx(\dist, onoff:1, p2:rrand(0.01, 1.0), p1:rrand(0.01, 1));





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
	l = min(vel, 0.5);
	c = Saw.ar(Line.kr(500 + (2000 * vel), 50, 0.1));
	c = c * Line.kr(1, 0.1, l, doneAction:2);
	c = c + ClipNoise.ar(Line.kr(0.5, 0, Rand(l/5, l/3), doneAction:2));
	Out.ar(40, c*3);
	Out.ar([0, 1], c*0.05);
}).add;

SynthDef("bd", {arg vel;
	var c;
	c = SinOsc.ar(Line.kr(500, 50, 0.1));
	c = c * Line.kr(1, 0.1, 0.25, doneAction:2);
	Out.ar(40, c);
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
	Out.ar(40, c*2);
	Out.ar([0, 1], c * 0.1 * vel * 5);
}).add;


~sn.run;
~sn.free;



