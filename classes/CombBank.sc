// call new, then call run after the server has had time to make the synthdef map

CombBank : Object {

  var
  ctlResponder,
  myInBus,
  myOutBus,
  fxSynth,
  noteArray;

  *new {arg channel=0, lagCtl=1, decayCtl=2, noteCtl=3, outBus=0, inBus=1;
	^super.new.init(channel, lagCtl, decayCtl, noteCtl, outBus, inBus);
  }

  init {arg channel, lagCtl, decayCtl, noteCtl, outBus, inBus;

	this.sendSynthDefs;
	this.setupNoteArray;
	myInBus = inBus;
	myOutBus = outBus;
	ctlResponder = CCResponder (
	  {|src, chan, num, value|
		//'ctl num set: '.post;
		//value.postln;
		// call updates on synths
		//this.updateSynth(num, value);
		switch (num,
		  lagCtl, {
			fxSynth.set(\lagSet, value);
		  },
		  decayCtl, {
			fxSynth.set(\decaySet, value);
		  },
		  noteCtl, {
			this.updateCombs;
		  }
		);
	  },
	  nil,
	  channel,
	  [lagCtl, decayCtl, noteCtl],
	  nil
	);
  }

	setLag{arg value;
		fxSynth.set(\lagSet, value);
	}
	setDecay{arg value;
		fxSynth.set(\decaySet, value);
	}

  setFastReactSynth{
	fxSynth.set(\lagSet, 1);
	fxSynth.set(\decaySet, 64);
  }

  updateCombs{
	var n_index, multArray;
	//multArray = [0.125, 0.25, 0.5];
	multArray = [1, 2, 4];
	// choose a set of values from the note array
	// - pick an array index - could use value for note index or radomly do it
	n_index = noteArray.size.rand;

	// it doesn't like setting them all with a single set message
	// so I do it asynchronously instead. If i was a comb filter, i wouldn't like it either

	fxSynth.set(\note1Set, (multArray.choose*noteArray[n_index][0]));
	fxSynth.set(\note2Set, (multArray.choose*noteArray[n_index][1]));
	fxSynth.set(\note3Set, (multArray.choose*noteArray[n_index][2]));
	fxSynth.set(\note4Set, (multArray.choose*noteArray[n_index][3]));
  }

  setScale{arg freqs;
	noteArray = Array.fill(7, {
	  freqs = freqs.scramble();
	  Array.fill(4, {freqs = freqs.rotate;1/freqs[0]});
	});
  }

  // calculates the values for the note array
  setupNoteArray{
	// for max's blood runs pure track
	var n_a=440.00, n_b=493.88, n_cs=277.18, n_d=293.66, n_e=329.63, n_fs=369.99, n_gs=415.30;
	// [f#, a, b, e]
	// [c#, d, e, f#]
	// [e, g#. a, b]
	// [c#, b, d, f#]
	// [g#, a, b, c#]
	// [a, e, a, e]
	// [f#, e, f#, e]

	noteArray = [
	  [1/n_fs, 1/n_a, 1/n_b, 1/n_e],
	  [1/n_cs, 1/n_d, 1/n_e, 1/n_fs],
	  [1/n_e, 1/n_gs, 1/n_a, 1/n_b],
	  [1/n_cs, 1/n_b, 1/n_d, 1/n_fs],
	  [1/n_gs, 1/n_a, 1/n_b, 1/n_cs],
	  [1/n_a, 1/n_e, 1/n_a, 1/n_e],
	  [1/n_fs, 1/n_e, 1/n_fs, 1/n_e]
	  ];


  }

  // you need to call this after you instantiate the synth to make it run
  run {
	fxSynth = Synth("combBank",
	  [\audioInBus, myInBus, \outBus, myOutBus,
	  \lagSet, 0.01, \decaySet, 96, \note1Set, 0.1, \note2Set, 0.1, \note3Set, 0.1, \note4Set, 0.1]);

  }

  free {
	fxSynth.free;
	ctlResponder.remove;
  }

  sendSynthDefs{
	var server;
	server = Server.local;
	SynthDef(
	  "combBank", { arg audioInBus, outBus, note1Set, note2Set, note3Set, note4Set, decaySet,lagSet;
		var combArray, source, delay, decay, decayScalar, lagScalar, lagCtl, midiTo1, max_dt, min_dt;
		midiTo1 = 1/127;
		source = AudioIn.ar(audioInBus)*0.5;
		// gate it
		Compander.ar(source, source,
		  thresh: 0.25,
		  slopeBelow: 10,
		  slopeAbove: 1,
		  clampTime: 0.01,
		  relaxTime: 0.01
		);

		//decay = MouseY.kr(0.01, 10);
		decayScalar = (midiTo1)*(10-0.01);
		lagScalar = (midiTo1)*(10-0.001);
		lagCtl = lagSet*decayScalar+0.001;
		decay = decaySet*decayScalar+0.01;

		// the values for the comb delaya are read from the control buses
		// chordBus1-3
		//		combArray = Array.fill(4, {
		//  LFDNoise1.ar(1.0)*(CombN.ar(source,0.5, Lag.kr(note1Set, lagCtl), decay)+source);
		//});

		// now contrain the incoming notes to prevent crashing
		max_dt = 0.1;
		min_dt = 0.001;
		note1Set =  max(min_dt, clip2(note1Set, max_dt));
		note2Set =  max(min_dt, clip2(note2Set, max_dt));
		note3Set =  max(min_dt, clip2(note3Set, max_dt));
		note4Set =  max(min_dt, clip2(note4Set, max_dt));

		combArray=[
		  LFDNoise1.ar(0.5, mul:0.5, add:0.5)*(CombN.ar(source,0.1, Lag.kr(note1Set, lagCtl), decay)+source),
		  LFDNoise1.ar(0.5, mul:0.5, add:0.5)*(CombN.ar(source,0.1, Lag.kr(note2Set, lagCtl), decay)+source),
		  LFDNoise1.ar(0.5, mul:0.5, add:0.5)*(CombN.ar(source,0.1, Lag.kr(note3Set, lagCtl), decay)+source),
		  LFDNoise1.ar(0.5, mul:0.5, add:0.5)*(CombN.ar(source,0.1, Lag.kr(note4Set, lagCtl), decay)+source)
		];
		combArray = combArray.mean;
		combArray = Compander.ar(combArray, combArray,thresh:0.09,slopeBelow:0.1,slopeAbove:0.5,clampTime:0.01,relaxTime:0.2);
		Out.ar(outBus, combArray);
	  }
	).send(server);//.writeDefFile;

  }
}