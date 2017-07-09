
~m = MykMidi.new;
~m. noteOn(9, 47);

(
o = Server.local.options;
o.numOutputBusChannels = 8;
o.numInputBusChannels = 8;
o.hardwareBufferSize = 64;
s = Server.local.boot;
s.boot;
s.doWhenBooted{
	~snd = MykSounds.new;
	~snd.fx(\rev, onoff:1);
	~v = MykVdrum.new;
	~v.free;
	~v.velToLengthMode(factor:0.25);
	~v.on(\sn, {arg vel;
		//~snd.play(0, freq:rrand(24, 56), len:vel / 127.0, amp:4, p1:vel / 127.0);
		~snd.fx(\rev, p1:(vel / 16.0) + 0.1, amp:0.5);
	});
	~v.on(\tom2, {arg vel;
		var onoff = 1;
		if (vel <32, {onoff = 0});
		~snd.fx(\rev, onoff:onoff, p1:(vel / 127.0)+0.1, amp:0.5);
		//~snd.fx(\dist, onoff:1-onoff, p1:(vel / 127.0)+0.1);
	});
		"ready!".postln;

}
)

~snd.fx(\fft, onoff:1);

// alternative set up
(
o = Server.local.options;
o.numOutputBusChannels = 8;
o.numInputBusChannels = 8;
o.hardwareBufferSize = 64;
s = Server.local.boot;
s.boot;
s.doWhenBooted{
	~snd = MykSounds.new;
	~v = MykVdrum.new;
	~v.free;
	~v.velToLengthMode(factor:0.25);
	~v.on(\bd, {arg vel;
		~snd.fx(\dist, onoff:1, p1:0.01, p2:0.5);
		~snd.play(\bd, freq:20, amp:4, len:vel / 127.0 * 2);
	});
	~v.on(\sn, {arg vel;
		~snd.play(\fm, freq:rand(20, 50), amp:4, p1:rrand(0.1, 1.0), len:vel / 127.0);
		~snd.fx(\rev, onoff:1, p2:rrand(0, 0.1), p1:(vel/127.0 * 0.3) + 0.7);
	});
		"ready!".postln;

}
)



(
o = Server.local.options;
o.numOutputBusChannels = 8;
o.numInputBusChannels = 8;
o.hardwareBufferSize = 64;
s = Server.local.boot;
s.boot;
s.doWhenBooted{
	~snd = MykSounds.new;
	~v = MykVdrum.new;
	~v.free;
	~v.velToLengthMode(factor:0.25);
	~v.on(\bd, {arg vel;
		//~snd.fx(\dist, onoff:1, p1:0.01, p2:0.5);
		~snd.play(\fm, freq:rrand(20.0, 35.0), amp:1, len:vel / 127.0 * 2, p1:rrand(0.1, 1.0));
	});
	~v.on(\hh, {arg vel;
		~snd.fx(\del, p1:rrand(0.01, 1.0));
	});
	~v.on(\tom1, {arg vel;
		var onoff;
		onoff = 0;
		if (vel > 70, {onoff = 1});
		~snd.fx(\del, onoff:onoff, amp:0.25, p2:(vel / 127.0 * 0.5) + 0.5, p1:vel/127.0 + 0.01);
	});
	~v.on(\tom2, {arg vel;
		//~snd.fx(\dist, onoff:1, p1:0.01, p2:0.5);
		var f = 40;
		~snd.play(\fm, freq:f * [1,0.5, 1.5].choose, amp:1, len:vel / 127.0 * 2, p1:rrand(0.1, 1.0))
	});

	"ready!".postln;
}
)

~snd.fx(\del, onoff:1, p1:0.5, p2:0.4);
s.boot;
~snd = MykSounds.new;
~snd.fx(\rev, onoff:1);
~snd.play(0);
~snd.fx(\rev, onoff:0,  p1:0.5, p2:0.4);
~snd.play(\fm,freq:20 );


