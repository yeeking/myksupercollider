// a combination of the melody seq and the multifx seq
// in that it has a persistent synth that is sent values (multiFX)
// but also has a melody line (meldoy)
// only really designed to work with the additive synth at the moment

SequencerUnitPersistentSynth : SequencerUnitMultiFX {

  var 
  <responderNoteDesc = "[0]->trigger note, [1]->new sequence note",
  <responderCtlDesc = "[0]->initialise note sequence with random notes from key [0] ",
  ctlArrayWidth = 4, // one for each fx param
  ctlArrayLength= 9,
  partialCount = 100;
  
  // overidden so the first channel is a stepped sequence from 0-partial count not just random..
  initialiseCtlSeq{ arg width, length;
	// generate 2 d array for ctlSeq
	ctlSeq = Array.fill(partialCount, {
		Array.fill(length, {arg i; i});
	  });
	// scrable the second row - that'll give us a random melody seq 
	//ctlSeq.put(0, ctlSeq.at(0).scramble);
  }
  
  sendSynthDefs{
 	var server;
	server = Server.local;
	
	SynthDef("additiveRand2",{arg outBus, ctl1, ctl2, ctl3, envGate=1, envRelease=0.1;
	  var partialMults, sines, envelope, partialCount,partialVolume, partialMute, baseFreq, midiTo1;
	  midiTo1 = 1/127;
	  partialCount = 100;
	  // normalise partial mute to number of partials
	  partialMute = ctl1*midiTo1*partialCount;
	  //baseFreq = ctl1.midicps*LFDNoise1.kr(ctl3*midiTo1*5);
	  baseFreq = ctl1.midicps+LFDNoise1.kr(ctl3*midiTo1, mul:ctl1*0.0625);
	  //baseFreq = ctl1.midicps+(Spring.ar(ToggleFF.ar(Dust.ar(LFDNoise1.kr(1, mul:5, add:5))), LFDNoise1.kr(2, mul:2, add:2), 0.00001)*ctl1*0.0625);
	  // generate the partial mult array
	  partialMults = Array.fill(partialCount, {Rand(30.0)});
	  // make the sin oscs
	  sines = Array.fill(partialCount, 
		{
		  arg i;
		  //SinOsc.ar(partialMults[i]*baseFreq,0,mul:partialVolume>i*0.5)
		  // partials with vibrato
		  //SinOsc.ar(SinOsc.ar(0.4, add:200, mul:1)+baseFreq*partialMults[i], 0, (partialMute>i)*0.2);
		  SinOsc.ar(baseFreq*partialMults[i], 0, (partialMute>i)*0.2);
		});
	  // now add the osc with the base freq
	  sines.add(SinOsc.ar(baseFreq,0,1));
	  
	  // make the envelope
	  envelope = EnvGen.ar(
		Env.adsr(0.02, 0.2, 0.25, envRelease, 1, -4), envGate,
		doneAction:0
	  );
	  // mix them together
	  Out.ar(outBus, sines.mean*envelope*10);
	}).send(server)

  }
}