PitchScanner2 : Object{
  var callbackFunc, interval, audioInBus, pitchCtlBus, scanner,
  freqAnalSynth, synthRunning = 0, pollRout, bus;//, last_freq;

  *new{arg callbackFunc={arg freq;freq.postln}, interval=0.01, audioInBus=1, pitchCtlBus=130;
	^super.newCopyArgs(callbackFunc, interval, audioInBus, pitchCtlBus).prInit;
  }

  prInit{
	this.sendSynthDefs;
	bus = Bus.new(\control, pitchCtlBus, 1);
  }

  run{
	if (synthRunning == 0, {
	  freqAnalSynth = Synth("pitchReader2", [\audioInBus, audioInBus, \ctlOutBus, pitchCtlBus]);
	  synthRunning = 1;
	});
	// set off a routine that polls a bus every 'interval' secs
	pollRout = {
	  inf.do{
		bus.get({arg value; callbackFunc.value(value);});
		interval.wait;
	  };
	}.fork;
  }

  free{
	pollRout.stop;
	freqAnalSynth.free;
  }


  sendSynthDefs{
	var server;
	server = Server.local;

	// freq analysis synth
	SynthDef("pitchReader2", {arg audioInBus=1, ctlOutBus=0;
	  var audioIn, outBus, freq, hasFreq;
	  audioIn = AudioIn.ar(audioInBus);
	  # freq, hasFreq = Pitch.kr(audioIn, ampThreshold: 0.02, median: 7, initFreq:0);
	  Out.kr(ctlOutBus, freq);
	}).send(server);
  }

}



// copy of the pd object cos its really handy
MykFiddle : Object{
  var >callback, interval, audioInBus, pitchCtlBus, scanner, last_freq;

  *new{arg callback={arg freq;freq.postln}, interval=0.01, audioInBus=1, pitchCtlBus=130;
	^super.newCopyArgs(callback, interval, audioInBus, pitchCtlBus).prInit;
  }

  prInit{
	scanner = PitchScanner2.new({arg freq; this.readPitch(freq)}, interval, audioInBus, pitchCtlBus);
	last_freq = 0;
  }

  run{
	scanner.run;
  }

  free{
	scanner.free;
  }

  readPitch{arg freq;
	freq = (freq.cpsmidi.round);
	//("MYKFiddle: last, new "++last_freq++","++new_freq).postln;
	if (freq != last_freq, {
	  //("MYKFiddle: last, new "++last_freq++","++freq).postln;
	  callback.value(freq);
	  last_freq = freq;
	});
  }
}

// general purpose language side pitch scanner
// call new (callbackFunc, interval, audioInBusBus, pitchCtlBus)
// call run
// callbackFunc - this will be called with arg freq which is the latest frequency
// interval - interval between calls to callbackFunc
// audioInBus - 1 indexed audio input (uses AudioIn)
// pitchCtlBus - which control bus to read and write the pitch information


PitchScanner : Object {

  var callbackFunc, interval, audioInBus, pitchCtlBus, oscResponder, oscPollRoutine, freqAnalSynth, synthRunning = 0;

  *new {arg callbackFunc, interval=0.25, audioInBus=1, pitchCtlBus=128;
	^super.newCopyArgs(callbackFunc, interval, audioInBus, pitchCtlBus).prInit;

  }

  prInit{
	var server;
	server = Server.local;
	this.sendSynthDefs;
	// set up the osc responder
	oscResponder = OSCresponderNode(server.addr, '/c_set', { arg time, responder, msg;
	  //[\respond, time, responder, msg].postln;
	  //	  { lastFreq = msg.last }.defer;
	  { // this is waht is actually run when the callback happens...
		if (msg.at(msg.size-2)==pitchCtlBus && msg.last > 0, {// the penultimate element in msg is the ctl bus no.
		  //('read bus '++pitchCtlBus++" value "++msg.last).postln;
		  callbackFunc.value(msg.last);}
		);
	  }.defer;
	}).add;

	// set up the osc poller routine
	oscPollRoutine = Routine({
	  inf.do {
		// this will cause a reading to be taken from the freq bus
		server.sendMsg(\c_get, pitchCtlBus);
		//"picth scanner interval...".postln;
		//interval.postln;
		interval.wait;
		//0.125.wait;
	  };
	});

  }

  changeInterval{ arg newInterval;
	interval = newInterval;
  }

  // call this to set the pitch scanning in action
  run {
	if (synthRunning==0,
	  // run has been called for the first time - create the synth
	  {
		freqAnalSynth = Synth("pitchReader", [\audioInBus, audioInBus, \ctlOutBus, pitchCtlBus]);
		synthRunning = 1;
	  });
	oscPollRoutine.reset;
	oscPollRoutine.play;
  }

  stop {
	oscPollRoutine.stop;
  }

  // destructor
  free {
	freqAnalSynth.free;
	oscResponder.remove;
	oscPollRoutine.stop;
  }

  sendSynthDefs{
	var server;
	server = Server.local;

	// freq analysis synth
	SynthDef("pitchReader", {arg audioInBus=1, ctlOutBus=0;
	  var audioIn, outBus, freq, hasFreq;
	  audioIn = AudioIn.ar(audioInBus);
	  # freq, hasFreq = Pitch.kr(audioIn, ampThreshold: 0.02, median: 7, initFreq:0);
	  Out.kr(ctlOutBus, freq);
	}).send(server);

  }

}