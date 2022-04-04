0.00:	Uol log sequence     Gong
1.75:	plane with small blips    low activity
3.00:	moving pan of surface     medium activity
6:00:   side view 	          high activity
8:00:   plane disappars	          fx tail
(
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
	   ~fx.set(\amp, 2.0);
		l.free;

	}
];
)

OSCFunc.trace(true); // Turn posting on
OSCFunc.trace(false); // Turn posting on

o.remove;
(
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
		Synth("bell_trig", [\out, [40, 41, 42, 43].choose]);
	});
}, '/hole');
)
OSCFunc.free








