s.boot;

a = {SinOsc.ar(220)}.play;

a.free;

// three stage sound

~notes = [60, 62, 64];


// Start off with a noise source passed through
// three
//

// 1) whoosh


// 2) form a major chord

60

SynthDef("chord", {
	arg notes = [60, 62, 64];
	var c;//, notes;
	//notes = [60, 62, 64];
	c = Array.fill(notes.size, {arg i;
		SinOsc.ar(notes[i].midicps);
	}).mean;
	Out.ar([0, 1], c);
}).add;

a = Synth("chord");

// 3) splinter into FFT stuff




