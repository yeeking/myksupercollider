(
(
~cl = MykClock.new;
~cl.run;
~midi = MykMidi.new;
~vir = 4;
~korg = 0;
~dom = 5;
~vs =0;
10.do{arg i;
~cl.remove(i);
};

~rim = 40;
~sn = 38;
~bd = 36;
~ltom = 39;
~hat = 42;

~notes  = [38];

~cl.remove(0);



~mark = MykMarkov.new;
~mark.addFreq([[~hat, 32], [~bd, 127]]);

~mark.addFreq([[~hat, 64]]);



~mark.addFreq([[~bd, 96]]);
~mark.addFreq([[0, 0]]);
~mark.addFreq([[~sn,40],[ ~hat, 64]]);
~mark.addFreq([[~sn, 127]]);
~mark.addFreq([[~sn,30],[ ~hat, 64]]);



~mark.addFreq([[~sn,24][ ~hat, [64]]);

	~mark.nextFreq;
~mark.reset




~notes = Array.fill(10, {arg i; i + 33});
// dnb
	~cl.run;
~cl.add(0, {
		~mark.nextFreq.do{arg data;
			var note, vel;
			note = data[0];
			vel = data[1];
			if (note > 0, {
				~midi.playNote(~vs, note, vel, len:0.1);
			});
	}},  [0.25], [1]);

)

~cl.bpm(140);

~cl.add(0, {
	~midi.playNote(~vs, ~hat, 24, len:0.1);

}, [0.25, 0.25, 0.25, 0.25], [1, 0, 0, 0.5]);

~cl.add(1, {
	~midi.playNote(~vs, ~bd, 127, len:0.1);

}, [0.25, 0.25, 0.25, 0.25], [0.25, 0, 0, 0.5]);

~cl.add(2, {
	~midi.playNote(~vs, ~sn, rrand(64, 127), len:0.1);
}, [0.25, 0.25, 0.25, 0.25], [0.25, 0, 0, 0.5], 0.25);

~cl.add(1, {
	~midi.playNote(~vs, ~bd, 127, len:0.1);

}, [0.75], [0.5]);

~cl.add(2, {
	~midi.playNote(~vs, ~sn,  127, len:0.1);

}, [0.75], [0.25], 0.25);



~cl.add(6, {
	Synth("bd", [\l, 1.0]);
}, [1, 0.75, 0.25, 0.5, 0.5, 0.5,0.5], [1, 1, 0, 1, 0,1, 1]);
~cl.add(7, {
	Synth("sn", [\harsh, 0.1]);
}, [2], [1], 1

)