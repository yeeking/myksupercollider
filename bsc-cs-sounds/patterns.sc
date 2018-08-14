

m = MykMidi.new;
m.panic;

~vir = 4;
~korg = 0;
~dom = 5;


~cl = MykClock.new;
~cl.remove(0);
~cl.

~notes = [51, 53, 56, 58, 60];

~chords = [
	[~notes[0], ~notes[1], ~notes[3]],
	[~notes[2], ~notes[4], ~notes[0]],
]



~notes = ~notes - 5;
~notes = ~notes.scramble;

~notes = [55, 58, 63, 65, 68]
~notes = [55, 58, 63];
~mel = Array.fill(8, {~notes.choose});

~it = 0;
~fs = [
	{
	m.playNote(~dom, ~notes[0], len:
	0.05);
	m.playNote(~vir, ~notes[0], len:
	0.05);
	m.playNote(~korg, ~notes[0], len:
	0.05);
	~notes = ~notes.rotate(-1);
	~it = ~it + 1;
	if (~it == (~notes.size * 8), {
		~notes = ~notes.scramble;
		~it = 0;
	});
},
	{
		~cl.remove(0);
	}
];


~notes = ~notes.scramble;
~


~cl.remove(2);

~cl.add(1, ~fs[1], [5]);

~cl.add(2,
	{~cl.add(0, ~fs[0], [0.25], [1]);}
	,
	[10], [1], 5);

~cl.bpm(120);
~cl.bpm(60);

~cl.add(3, {~cl.bpm([240].choose)});




~cl.add(0, {
	m.playNote(~korg, ~mel[0], len:0.1);
	m.playNote(~dom, ~mel[0], len:0.1);
	~mel = ~mel.rotate(-1);
}, [0.25, 0.5, 0.25]);

~cl.add(0, {
	m.playNote(~korg, ~mel[0], len:1.0);
	m.playNote(~dom, ~mel[0], len:1.0);
	m.playNote(~vir, ~mel[0], len:1.0);
}, [3]);



~notes.do{arg n;
	m.playNote(~vir, n, len:5.0);
};









remove(0);


