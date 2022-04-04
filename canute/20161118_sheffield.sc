o = Server.local.options;
o.blockSize = 256;

s.boot;
s.doWhenBooted(
	m = MykDrummingSimulator.new(tidal_host:"10.0.1.3", tidal_port:4040);
	//m.useInternalClock
	m.useOscClock;
);


s.boot;
(
s.doWhenBooted{
	~ons = MykBonk.new;
	~ons.run;
	~snd = MykSounds.new;
	~ons.callback = {arg val;
		if (val > 0.3, {
			~snd.fx([\rev, \del].choose, onoff:1, p1:1.0.rand, p2:1.0.rand);
			}, {
				~snd.fx(\del, p1:1.0.rand, p2:1.0.rand);

				~snd.fx(\rev, p1:1.0.rand, p2:1.0.rand);
				~snd.fx(\rev, onoff:0);
				~snd.fx(\del, onoff:0);
		});
	};
};
)

~ons.callback = {arg on;on.postln;};


s.boot;


