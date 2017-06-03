~funcs = Dictionary[
	\fxOn->{arg name='reverb';
		var synth;
		("fxOn "++name).postln;
		synth = ~fx[name];
		if (name != Nil, {
			synth.set(\on, 1);
		});
	},
	\fxOff->{arg name='reverb';
		var synth;
		("fxOff "++name).postln;
		synth = ~fx[name];
		if (name != Nil, {
			synth.set(\on, 0);
		});
	},
	\fxParam->{arg name='reverb', value=0.5;
		var synth;
		("fxParam "++name++" v: "++value ).postln;
		synth = ~fx[name];
		if (name != Nil, {
			synth.set(\p, value.asFloat);
		});
	},
	\startSynths -> {
		("startSynths").postln;
		// create a dictionary of synths
		~fx = Dictionary.new;
		{
			("startSynths: creating synths").postln;
			~fx.add(\thru -> Synth('thru'));
			~fx.add(\reverb -> Synth('reverb'));
			~fx.add(\delay -> Synth('delay'));
			~fx.add(\flange -> Synth('flange'));
		}.defer(2);
	},
	\defs->{
		("defs: sending synthdefs ").postln;
		// pass through
		SynthDef('thru', {
			Out.ar(0, SoundIn.ar([0, 1]));
		}).add;

		SynthDef('reverb', {arg on = 1, p = 0.5;
			var in;
			p = min(1, p);
			p = max(0, p);
			in = SoundIn.ar([0, 1]);
			// set up on off to the fx
			in = in * on;
			4.do{
				in = AllpassC.ar(in, 0.05, (0.05.rand)*p, 2.0 + (p * 5));
			};
			Out.ar(0, in);
		}).add;
		SynthDef('delay', {arg on = 1, p = 0.75;
			var in, c1, c2;
			p = min(1, p);
			p = max(0, p);
			in = SoundIn.ar([0, 1]);
			// set up on off to the fx
			in = in.mean * on;
			c1 = Array.fill(2, {
				CombC.ar(in, 0.9, 0.2.rand + (p*0.7*LFDNoise1.kr(0.25).range(0.001, 1.0)), 5.0);
			}).mean;
			c2 = Array.fill(2, {
				CombC.ar(in, 0.9, 0.2.rand + (p*0.7*LFDNoise1.kr(0.25).range(0.001, 1.0)), 5.0);
			}).mean;

			Out.ar(0, [c1, c2]);
		}).add;
		SynthDef('flange', {arg on = 1, p = 0.75;
			var in;
			p = min(1, p);
			p = max(0, p);
			in = SoundIn.ar([0, 1]);
			// set up on off to the fx
			in = in.mean * on;
			in = CombC.ar(in, 0.02, SinOsc.kr(0.25).range(0.005, 0.02 * p), p);
			Out.ar(0, [in, in]);
		}).add;
		SynthDef('grunge', {arg on = 1, p = 0.75;
			var in, f;
			p = min(1, p);
			p = max(0, p);
			f = Select.kr(p*5.asInteger, [1,2,3,4,5]);
			in = SoundIn.ar([0, 1]);
			// set up on off to the fx
			in = in.mean * on;
			//in = Normalizer.ar(in, 1.0, 0.01);
			//in = CombC.ar(in, 0.02, 0.01 + (PMOsc.kr(100, 25*p, 100).range(-0.005, 0.005)), p * 3);
			in = CombC.ar(in, 0.02, 0.01 + (SinOsc.kr(f*25).range(-0.005, 0.005) * p), 0.5 + (p));
			Out.ar(0, [in, in]);
		}).add;
	}
];

s = Server.local.boot;
s.doWhenBooted{
	// send the defs
	~funcs['defs'].value;
	// start the synths
	~funcs['startSynths'].value;

	//n = NetAddr("192.168.7.2", 57120); // local machine
	OSCdef(\fx,  {|msg, time, addr, recvPort|
		var name, val;
		msg.postln;
		if(msg.size >= 3, {
			name = msg[1];
			val = msg[2].asFloat;
			(name ++ " p "++val).postln;
			if(val == 1, {'on'.postln;~funcs['fxOn'].value(name)});
			if(val == 0, {'off'.postln;~funcs['fxOff'].value(name)});
			if((val > 0) && (val < 1), {
				~funcs['fxParam'].value(name,val);
			});
		});
	}, '/fx'); // def style

};

m = NetAddr("192.168.7.2", 57120);
m = NetAddr("127.0.0.1", 57120);

// turn on reverb
m.sendMsg("/fx", "reverb", "1");
m.sendMsg("/fx", "reverb", "0");

m.sendMsg("/fx", "delay", "1");
m.sendMsg("/fx", "delay", "0");

m.sendMsg("/fx", "flange", "1");
m.sendMsg("/fx", "flange", "0");

m.sendMsg("/fx", "grunge", "1");
m.sendMsg("/fx", "grunge", "0");

// set a param
m.sendMsg("/fx", "reverb", 0.6);
m.sendMsg("/fx", "delay", 0.001);
m.sendMsg("/fx", "flange", 0.1);
m.sendMsg("/fx", "grunge", 0.7);


{
	20.do{arg i;
		m.sendMsg("/fx", "flange", 1/(i+1));
		0.5.wait;
	};
	'done'.postln;
}.fork;

//
Server.default = s = Server("belaServer", NetAddr("192.168.7.2", 57110));

s.initTree;
s.startAliveThread;


~a = {SinOsc.ar(400)}.play;
~a.free;


~fx[\reverb].set(\on, 0);


