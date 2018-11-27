
~fx = Synth("fft_fx");
~noise = {Out.ar([30], PinkNoise.ar(MouseX.kr(0, 1)))}.play;
~noise.free;
~fx.set(\stretch, 0.01);
~fx.set(\shift, 0.4);
~fx.set(\threshold, 0.001);
~fx.set(\smear, 2);
~fx.set(\teeth, 0.1);
~fx.set(\comb_width, 2.5);


~fx.set(\length, 4.0);

(
SynthDef("fft_fx", {arg in_bus=30, out_bus=0, buffer, freeze = 0, smear = 0.01, threshold = 0.001, stretch = 1.0, shift = 0.5,teeth=0.5, comb_width=0.1, length=1;
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
	chain2 = chain;

	4.do{
	 	chain = AllpassL.ar(chain, 0.1, 0.1.rand, (length+0.1)*2.0.rand);
	 };
	 4.do{
		chain2 = AllpassL.ar(chain2, 0.1, 0.1.rand, (length+0.1)*2.0.rand);
	 };
	Out.ar(0, [chain, chain2]);
	}).add;
)