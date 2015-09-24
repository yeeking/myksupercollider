MykSounds : Object {
	var all_defs, delay, reverb, fft, dist, dist_buff;

	*new {
		^super.newCopyArgs().prInit;
	}

	prInit {
		this.sendSynthDefs;
		all_defs = [
			"bass",
			"fm",
			"bd",
			"sn",
			"oh",
			"ch"
		];
		dist_buff = Buffer.alloc(Server.local, 512, 1);
		{
			dist_buff.setn(0, Array.fill(dist_buff.numFrames, {rrand(-1.0, 1.0)}));

		}.defer(1.0);

		// create fx synths once synthdefs are ready
		{
			reverb = Synth("MykSounds_rev", [\onoff:0]);
			delay = Synth("MykSounds_del", [\onoff:0]);
			fft = Synth("MykSounds_fft", [\onoff:0]);
			//dist = Synth("MykSounds_dist", [\onoff:0]);
			dist = Synth("MykSounds_distws", [\buff, dist_buff, \onoff:0]);
		}.defer(2.0);
	}

    /** play a sound... send in \bass, \sn etc. for ind or an integer. p1 is in the range 0-127 for notes, p2 and p3 are 0-1*/
	play{arg ind, freq = 60, p1 = 0.5, p2=0.5, p3=0.1, len = 0.5, amp=1.0;
		var sdef;
		if (ind.isNumber, {
			// numeric index
			sdef = all_defs.wrapAt(ind);
		},
		{
			// '\test' symbol index
			sdef = ""++ind;
		});
		Synth("MykSounds_"++sdef, [\freq, freq, \p1, p1, \p2, p2, \p3, p3, \amp, amp, \len, len]);

	}
	/** control the fx units.
	* ind can be \rev, \del or \fft
	* onoff can be 1 or 0 to switch the specified unit on or off
	* p1 and p2 change fx params, in range 0-1
	*/
	fx {arg ind = \rev, onoff = -1, p1 = -1, p2 = -1, amp = -1.0;
		var node;
		switch (ind,
			\rev, {node = reverb;},
			\del, {node = delay},
			\fft, {node = fft},
			\dist, {node = dist}
		);
		if (onoff != -1, {
			node.set(\onoff, onoff);
		});// only change if they explicitly request it
		if (amp != -1, {
			node.set(\amp, amp);
		});// only change if they explicitly request it
		if (p1 != -1, {
			node.set(\p1, p1);
		});
		if (p2 != -1, {
			node.set(\p2, p2);
		});
	}

	sendSynthDefs{
		SynthDef("MykSounds_bass", {arg freq = 45, len ,p1, p2, p3, amp=1.0;
			var c, e;
			freq = (freq.midicps / 2);
			//freq = freq.midicps;
			e = EnvGen.kr(Env.perc(0.01, len), doneAction:2);
			c = Array.fill(32, {arg i;
				var lfo, f;
				f = freq.midicps * (i+1);
				lfo = SinOsc.ar(p1).range(-1 * (f / 10), f / 10);
				SinOsc.ar(f + lfo);
			}).mean;
			//c = RLPF.ar(c, (freq * (p3 * 2)) * Line.kr(0, 1, p2), p1);
			c = Normalizer.ar(c, 0.25);
			Out.ar(30, c * e * amp);
		}).add;
		SynthDef("MykSounds_fm", {arg freq, len, p1 = 0.5, p2 = 0.5, p3=0.5, amp=1.0;
			var c1, c, e, muls, lfo,  scalar;
			e = EnvGen.kr(Env.perc(0.01, len), doneAction:2);
			freq = freq.midicps;
			lfo = SinOsc.ar(3.0).range(-1 * (freq / 5), freq/5);
			muls = [0.25, 0.5, 1.5, 2, 3, 4];
			scalar = Select.kr(p1 * muls.size, muls);
			c1 = PMOsc.ar(freq,(freq * scalar) + (p3 * lfo), (p2 * 10) + 1);
			//c = SinOsc.ar(freq + (c1 * (freq * p3)));
			c = c1;
			c = Normalizer.ar(c, 0.5);
			Out.ar(30, c * e * 0.1 * amp);
		}).add;

		SynthDef("MykSounds_bd", {arg freq, len, p1=0.5, p2 = 0.5, p3 = 0.5, amp = 1.0;
			var c, e, bf;
			bf = (freq.midicps) * 0.5;
			e = EnvGen.kr(Env.perc(0.01, len), doneAction:2);
			c = SinOsc.ar(freq:Line.kr(bf * 1.5, bf * 0.5, len/3));
			c = c + RLPF.ar(Pulse.ar(bf), min(8000, bf * 25 * p3), p2*4);
			c = Normalizer.ar(c, 0.5);
			Out.ar([0, 1, 31], c * e * amp);
		}).add;

		SynthDef("MykSounds_sn", {arg freq, len, p1 = 0.5, p2, p3, amp=1.0;
			var c, e, f, fc;
			// tone
			f = freq.midicps;
			fc = Line.kr(f*1.5, f, 0.075);
			c = SinOsc.ar([fc, fc * 2]) * Line.kr(1, 0, p1);
			// crack
			c = c + ClipNoise.ar(Line.kr(0.75, 0, p2));
			// snare rattle
			c = c + WhiteNoise.ar(EnvGen.kr(Env.perc(0.15, len), levelScale:p3));
			e = EnvGen.kr(Env.perc(0.01, len), doneAction:2);
			Out.ar([30, 31, 0, 1], c * e * 0.0125 * amp);
		}).add;
		SynthDef("MykSounds_oh", {arg freq, len, p1, p2, p3, amp=1.0;
			var c, e, fs, noise, bf;
			freq = freq / 127;
			bf = 500;
			fs = Array.fill(5, {arg i; (i + p2) * bf});
			noise = RHPF.ar(ClipNoise.ar(0.25), bf * 2, 0.25);
			e = EnvGen.kr(Env.perc(0.01, len), doneAction:2);

			c = Array.fill(fs.size, {arg i;
				BHiPass4.ar(noise, fs[i], 0.025 + (LFDNoise1.kr(p1 * 50).range(-0.025, 0.05)));
				//SinOsc.kr(LFDNoise1.kr(0.5).range(1.0, 2.0)).range(0.01, 0.05));
			}).mean;
			Out.ar([30, 31], c * e * 0.0125 * amp);
		}).add;
		SynthDef("MykSounds_ch", {arg p1, p2, p3;

		}).add;

		SynthDef("MykSounds_rev", {arg onoff=1, p1 = 0.5, p2 = 0.5, amp = 1.0;
			var in, chain, chain2, dt, mix;
			onoff = max(0, min(onoff, 1));
			p1 = Lag.kr(p1);
			p2 = Lag.kr(p2);
			onoff = Lag.kr(onoff);
			dt = max(0, min(p1, 1));
			dt = dt * 0.01;
			in = In.ar(30);
			chain = in * onoff * amp;
			chain2 =in * onoff * amp;
			4.do{
				chain = AllpassC.ar(chain, 0.1, Rand(0.001, 0.0015) + dt, p2 * 5.0);
			};
			4.do{
				chain2 = AllpassC.ar(chain2, 0.1, Rand(0.001, 0.0015) + dt, p2 * 5.0);
			};
			chain = chain * 1.5;
			chain2 = chain2 * 1.5;
			Out.ar(0, [chain + in, chain2 + in]);
		}).add;

		SynthDef("MykSounds_del", {arg onoff=1, p1 = 0.125, p2 = 0.5, amp = 1.0;
			var in, chain, dt, mix, chain2;
			onoff = max(0, min(onoff, 1));
			p1 = Lag.kr(p1, LFDNoise1.kr(0.1).range(0.1, 1.0));
			p2 = Lag.kr(p2);
			onoff = Lag.kr(onoff);
			dt = max(0, min(p1, 1));
			dt = dt * 0.45;
			in = In.ar(30);
			chain = in * onoff * amp;
			chain = CombC.ar(chain, 0.5, 0.05+ dt, p2 * 5.0);
			chain2 = chain + CombC.ar(chain, 0.5, 0.1+ dt, p2 * 7.0) * 0.5;
			Out.ar(0, [
				chain + (0.5 * in),
				chain2 + (0.5 * in)
			]);
		}).add;

		SynthDef("MykSounds_fft", {arg onoff = 1, p1 = 0.5, p2 = 0.5, amp = 1.0;
			var c, in, c1;
			in = In.ar(30);
			c = FFT(LocalBuf(2048, 1), in * Lag.kr(onoff));
			c = PV_LocalMax(c, SinOsc.kr(LFDNoise1.kr(0.5).range(0.1, 1.0)).range(0.1, p1));
			c = PV_MagFreeze(c, Dust.kr(p2 * 2.0));
			c = IFFT(c);
			c = Normalizer.ar(c, 0.0125);
			c1 = c;
			//bit of reverb
			4.do{
				c = AllpassC.ar(c, 0.1, rrand(0.05, 0.1), p1 * 5);
			};
			4.do{
				c1 = AllpassC.ar(c1, 0.1, rrand(0.05, 0.1), p1 * 5);
			};
			Out.ar(0, [(c* amp), (c1 * amp)]);
		}).add;

		SynthDef("MykSounds_dist", {arg onoff = 0, amp = 0.5, p1 = 0.5, p2 = 1.0;
			var in, c,c2, phase, freq;
			in = In.ar(31);
			freq = (p1 * 127.0).midicps;
			phase = in * pi * 2;
			c = FBSineC.ar(freq:phase * freq, c:phase, im:p2 * 10);
			c = Normalizer.ar(c, 1.0);
			c2 =FBSineC.ar(freq:phase * freq * 2, c:phase, im:p2 * 10);
			c2 = Normalizer.ar(c2, 1.0);
			Out.ar(0, [c, c2] * amp * onoff);
			//Out.ar(1, in);
		}).add;
		SynthDef("MykSounds_distws", {arg buff, onoff = 0, amp = 0.5, p1 = 0.5, p2 = 1.0;
			var in, c,c2, phase, freq;
			in = In.ar(31);
			c = Shaper.ar(buff, in * p1, 0.5);
			c = Normalizer.ar(c, 0.5);
			Out.ar([0, 1], c * amp * 0.1 * onoff);
			//Out.ar(1, in);
		}).add;
	}
}