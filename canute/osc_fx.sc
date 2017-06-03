/**
 * Please note that support for SuperCollider on Bela is still experimental,
 * so feel free to report issues here: https://github.com/sensestage/supercollider/issues
 *
 * This script simply starts scsynth, which then waits for messages.
 * Use the code in remote-examples to interact with the board from
 * the SuperCollider IDE running on the host.
 */

// NOTE: the settings provided here may not be the most sensible in terms of gains.

s = Server.default;

s.options.numAnalogInChannels = 8;
s.options.numAnalogOutChannels = 8;
s.options.numDigitalChannels = 16;
s.options.maxLogins = 64;  	   // set max number of clients

s.options.pgaGainLeft = 4;     // sets the pga gain of the left channel to 4 dB
s.options.pgaGainRight = 5;    // sets the pga gain of the right channel to 5 dB
s.options.headphoneLevel = -8; // sets the headphone level to -8 dB
s.options.speakerMuted = 0;    // the speakers are not muted (so active)
s.options.dacLevel = -5;       // sets the gain of the dac to -5 dB
s.options.adcLevel = -3;       // sets the gain of the adc to -3 dB
s.options.numMultiplexChannels = 0; // do not enable multiplexer channels
s.options.belaPRU = 0;         // select the PRU on which Bela audio will run

s.options.blockSize = 16;
s.options.numInputBusChannels = 2;
s.options.numOutputBusChannels = 2;

s.options.postln;

s.boot;
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
			~fx.add(\grunge -> Synth('grunge'));
			~fx.add(\glass -> Synth('glass'));

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
		SynthDef('flange', {arg on = 1, p = 0.25;
			var in;
			p = min(1, p);
			p = max(0, p);
			in = SoundIn.ar([0, 1]);
			// set up on off to the fx
			in = in.mean * on;
			in = 0.5 * CombC.ar(in, 0.02, SinOsc.kr(0.1).range(0.005, 0.02 * p), p);
			Out.ar(0, [in, in]);
		}).add;
		SynthDef('grunge', {arg on = 1, p = 0.4;
			var in, f;
			p = min(1, p);
			p = max(0, p);
			f = Select.kr(p*3.asInteger, [1,2,3]);
			in = SoundIn.ar([0, 1]);
			// set up on off to the fx
			in = in.mean * on;
			in = Compander.ar(in, in,
		        thresh: 0.01,
		        slopeBelow: 10,
		        slopeAbove: 1,
		        clampTime: 0.01,
		        relaxTime: 0.01
		    );
			in = 0.5 * CombC.ar(in, 0.02, 0.01 + (SinOsc.kr(f*25).range(-0.005, 0.005) * p), 0.3 + (p));
			Out.ar(0, [in, in]);
		}).add;
		SynthDef('glass', {arg on = 1, p = 0.2;
			var in, f, c1, c2;
			p = min(1, p);
			p = max(0, p);
			f = Select.kr(p*5.asInteger, [1,2,3,4,5]);
			f = p * 100;
			in = SoundIn.ar([0, 1]);
			// set up on off to the fx
			in = in.mean * on;
			//in = Normalizer.ar(in, 1.0, 0.01);
			c1 =0.5 *  CombC.ar(in, 0.02, 0.01 + (SinOsc.ar(f*25).range(-0.005, 0.005) * p), 0.4 + (p));
			c2 =0.5 *  CombC.ar(in, 0.02, 0.01 + (SinOsc.ar(f*30).range(-0.005, 0.005) * p), 0.3 + (p));

			Out.ar(0, [c1, c2]);
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
