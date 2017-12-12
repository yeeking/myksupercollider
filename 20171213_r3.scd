


s.boot;
~mark = MykMidiMarkov.new;
~mark.setupSynthMode;
~mark.synthMode = false;

~mark.playNote(54);

~mark.interval = 2;

~mark.reset;



MIDIClient.init;

m = MIDIOut(0);
m.noteOn(0, 61, 60);
m.noteOff(0, 61, 60);

m.
{6.do{arg d;
	d.postln;
	m = MIDIOut(d);
	16.do{arg i;
		i.postln;
		m.noteOn(i, 61, 60);
		0.25.wait;
		m.noteOff(i, 61, 60);
		0.25.wait;
	};
	0.25.wait;
}}.fork
m.noteOn(16, 60, 60);
m.noteOn(16, 61, 60);
m.noteOff(16, 61, 60);
m.allNotesOff(16);

MIDIOut
~midi_out = MIDIOut.new(2);
~midi_out.noteOn(3, 64, 64);

~midi = MykMidi.new;
~midi.playNote(0);

{
	var note, energy;
	note = 32;
	energy = 0.1;
	inf.do{
		//~midi.playNote;
		~midi.playNote(0, note + [12, 24].choose, rrand(32, 127), rrand(0.1, energy * 0.5));
		energy = energy * 0.7;
		if (energy < 0.2, {energy = 0.1});
		note = note + [1, 2, 12, -2, -12, -3].choose;
		if (note > 96, {note = rrand(24, 32);});
		if (note < 24, {note = rrand(24, 32);});

		rrand(0.1, 0.6 * energy ).wait;
		if (0.1.coin, {
			rrand(0.5, 2.0).wait;
		});
		if (0.05.coin, {
			rrand(2.0, 4.0).wait;
		});
	};
}.fork;


~m = MIDIOut.new(1)
~m.noteOn(16, 60, 60);
~m.noteOff(16, 60, 60);

MIDIClient.init;
a  =MIDIClient.destinations[2];

a

m = MIDIOut(2);
m.noteOn(16, 60, 60);
m.noteOn(16, 61, 60);
m.noteOff(16, 61, 60);
m.allNotesOff(16);


~m = MIDIOut.newByName("Akai MPD24-Akai MPD24 MIDI ");

SystemClock.sched(5, {"done".postln;});


(
var f;
f = "2 + 1".compile.postln;
a = f.value.postln;
)

a

// compute and sample from rest/ not rest distribution
d = "rest";
d = Dictionary.new.asCompileString.size;
d.put(\32, true);

c = d.asCompileString
c = c.compile.value;//.postln
c.keys

d == "rest"
e == "rest"

e.keys.do{arg  n;n.postln; e.at(n).postln};

a = []

~mark.mykMarkov.nextFreq;
~mark.id_lookup.getObj(~mark.mykMarkov.nextFreq;);


{inf.do{~mark.mykMarkov.nextFreq.postln;0.5.wait;}}.fork;
~mark.mykMidiObserver.debug = true;

~mark.mykMidiObserver.callback = {arg obs;obs.postln;};
~mark.mykMidiObserver.step;

~d = Dictionary.new;
~d.put(\32, true);
~k = ~d.keys;,
~d.collect{|item| false};

// symbol types:

Dictionary // not sorted, difficult to do minimal duplications, convenient storage

Array // can be sorted, difficult as storage, can be intermediate structure

// write to dictionary as the notes come in
// store arrays out to symbol table?? Or

// problem:
d = Dictionary.new.putPairs([\a, false, \b, 2]);
e = Dictionary.new.putPairs([\b, 2, \a, 1]);

d = d.reject { |item| item == false};

d
d.reject(|item| items == false);

f = Dictionary.new;
f.put(d, 1);
f.at(e);


~k.asArray
~d.asArray
~d.getPairs

MykKeyIndexer.sc:MykObjId : Object {


d = (hello: 9, whello: 77, z: 99);
d.getPairs;
MykDrummingSimulator.sc:		id_lookup = MykObjId.new;
MykKeyIndexer.sc:MykObjId : Object {
MykMarkovViz.sc:		id_lookup = MykObjId.new;
MykMidiMarkov.sc:



~d.

