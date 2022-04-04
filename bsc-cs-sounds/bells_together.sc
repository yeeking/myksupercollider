// SynthDefs
s.boot;


(
SynthDef("fft_fx", {arg in_bus=30, out_bus=0, buffer, freeze = 0, smear = 0.01, threshold = 0.001, stretch = 1.0, shift = 0.5,teeth=0.5, comb_width=0.1, length=1, amp = 1;
	  var in, chain, chain2;
	in = In.ar(in_bus);
	//Out.ar([0, 1], in);
	in = Normalizer.ar(in, 0.25, 0.5);
	  freeze = freeze;//, LFDNoise1.kr(0.1).range(0.1, 4));
	  smear = smear;//, LFDNoise1.kr(0.1).range(0.1, 4));
	  threshold = threshold*20;//, LFDNoise1.kr(0.1).range(0.1, 4));
	  stretch = stretch+1.0;//, LFDNoise1.kr(0.1).range(0.1, 4));
	shift = (shift*0.1*400)-300;//, LFDNoise1.kr(0.1).range(0.1, 4));
	teeth = (teeth*1000.0) + 2.0;//, LFDNoise1.kr(0.1).range(0.1, 4));
	comb_width = (comb_width*0.5)+0.5;// LFDNoise1.kr(0.1).range(0.1, 4));

	comb_width = comb_width * LFDNoise1.kr(0.25).range(0.25, 1.0);
	teeth = teeth * LFDNoise0.kr(0.25);
	shift = shift * LFDNoise0.kr(0.25);
	smear = smear * LFDNoise0.kr(0.25);

	  //in = In.ar(in_bus);
	  // noise gate
	  in = Compander.ar(in, in,
		thresh: 0.01,
		slopeBelow: 10,
		slopeAbove: 1,
		clampTime: 0.01,
		relaxTime: 0.01
	  );

	  //in = Compander.ar(in, in, thresh:0.5, slopeBelow:0.2,slopeAbove:0.01,clampTime:0.01,relaxTime:0.7);
	  //in = In.ar(0);
	chain = FFT(LocalBuf(512, 1), in);
	  chain = PV_MagFreeze(chain, freeze>0);
	  chain = PV_MagAbove(chain, threshold);
	  //chain = PV_MagShift(chain, stretch, shift);
	   chain = PV_MagSmear(chain, smear);
	  chain = PV_RectComb(chain, teeth, comb_width);
	  chain = IFFT(chain);
	  //chain = chain * boost;
	chain = Normalizer.ar(chain, 0.1, 0.1);
	  chain = Compander.ar(chain, chain,
		thresh: 0.5,
		slopeBelow: 1,
		slopeAbove: 0.5,
		clampTime: 0.01,
		relaxTime: 0.01
	  );

	  //chain = Compander.ar(chain, chain, thresh:0.09,slopeBelow:0.1,slopeAbove:0.5,clampTime:0.01,relaxTime:0.01);
	amp = Lag.kr(amp, 0.5);
	chain = chain * amp;
	chain2 = chain;

	4.do{
	 	chain = AllpassL.ar(chain, 0.1, 0.1.rand, (length+0.1)*2.0.rand);
	 };
	 4.do{
		chain2 = AllpassL.ar(chain2, 0.1, 0.1.rand, (length+0.1)*2.0.rand);
	 };
	Out.ar(0, [chain, chain2]);
	}).add;

SynthDef("lekky", {arg freq = 500;
	var c, fs, sel, mf, cf, cfs, cf_sel, spd, mod_ind;
	spd = 1.5;
	cfs = [freq, freq*1.25, freq*4, freq*10];
	cf_sel = LFDNoise0.kr(spd)*(cfs.size+1);
	cf = Select.kr(cf_sel, cfs);
	fs = [3, 0.5, 1.25, 6.75];
	sel = LFDNoise0.kr(spd)*(fs.size +1);
	mf = Select.kr(sel, fs);
	mod_ind = SinOsc.ar(
		SinOsc.kr(1).range(1.0, 4)).range(5, 20);
	c = PMOsc.ar(cf, cf*mf, mod_ind);
	Out.ar([0, 1, 30], c*0.025*Line.kr(1, 0, 3.0, doneAction:2))
}).add;

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
	Out.ar([0, 1, 30], [c*0.125, c*0.125, c*0.25]);
}).add;

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


{
	"Creating fx synth".postln;
	~fx = Synth("fft_fx");
}.defer(1.0);


{
	"Configuring fx synth and creating bells".postln;
~fx.set(\stretch, 0.01);
~fx.set(\shift, 0.4);
~fx.set(\threshold, 0.001);
~fx.set(\smear, 2);
~fx.set(\teeth, 0.1);
~fx.set(\comb_width, 2.5);
~fx.set(\length, 4.0);

	~freq = 70.midicps;
~freqs = [~freq/1, ~freq*9/8.0, ~freq*3/2.0, ~freq*27/16, ~freq*4/5];
//~freqs = [~freq.midicps, (~freq+2).midicps, (~freq+3).midicps, (~freq + 6).midicps];

~bells = Array.fill(~freqs.size, {arg i;
	Synth("trig_bell",
		[\freq, ~freqs[i], \trig, i+40]);
});
}.defer(2.0);





/// set up the modes

{
	"Setting up the modes".postln;
~bfreq = 200;
~phases = [
	{"Phase 1: UoL logo - big gong".postln;
		~lo_bell = Synth("lo_bell", [\f, ~freq/4]);
	},
	{"Phase 2: plane -small blips".postln;

		~fx.set(\amp, 1.0);

	},
	{"Phase 3: pan -light gongs".postln;
		~fx.set(\amp, 1.0);
		~freq = rrand(56, 76).midicps;
		~freqs = [~freq/1, ~freq*9/8.0, ~freq*3/2.0, ~freq*27/16, ~freq*4/5];
		~bells.do{arg bell, i;
			bell.set(\freq, ~freqs[i]);
		};

	},
	{"Phase 4: side - lots of gongs".postln;
			   ~fx.set(\amp, 0.0);

		l = Synth("lekky");

	},
	{"Phase 5: plane goes - glitch".postln;
	   ~fx.set(\amp, 1.5);
		//l.free;
			{~fx.set(\amp, 0);}.defer(0.5);
			{s.stopRecording()}.defer(5.0);

	}
];
}.defer(0.5);

{
	"Setting up the osc responders".postln;
o = OSCFunc({ arg msg, time, addr, recvPort;
	//var scene;
	//[msg, time, addr, recvPort].postln;
	~scene = msg[1];
	~scene.postln;
	~phases[~scene-1].value;

}, '/scene');

p = OSCFunc({ arg msg, time, addr, recvPort;
	var x, y;
	//[msg, time, addr, recvPort].postln;
	x = msg[1];
	y = msg[2];
	if (~scene == 3, {
	//		("x:"++x++" y:"++y).postln;

		if (0.125.coin, {
			Synth("bell_trig", [\out, [40, 41, 42, 43].choose]);
		});
	});
	if (~scene == 4, {
	//		("x:"++x++" y:"++y).postln;
			if (0.75.coin, {
				Synth("bell_trig", [\out, [40, 41, 42, 43].choose]);
			});
	});
}, '/hole');
~rec_id = 0;
q = OSCFunc({ arg msg, time, addr, recvPort;
	var x, y;
	//[msg, time, addr, recvPort].postln;
		("Priming for record "++~rec_id).postln;
	x = msg[1];
		"received /rec".postln;
	~rec_id = ~rec_id+1;

		s.prepareForRecord("/home/scratch/audio/uol/intro/intro"++~rec_id++".aiff");
		//s.prepareForRecord();
		{
			if(s.isRecording == false, {
				"Starting rec ".postln;
				s.record();
			});
		}.defer(0.25);
}, '/rec');
	"Ready to ring!".postln;
}.defer(3.0);

)

~rec_id

