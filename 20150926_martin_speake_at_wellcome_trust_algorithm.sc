(
// double clickm above here and execute

o = Server.local.options;
//o.numOutputBusChannels = 6;
s = Server.local.boot;

s.doWhenBooted{
~eve = MykEventStream.new;
	{
		0.5.wait; // wait until the synthdefs are ready...
		~eve.run;
	}.fork;

// quesiton and response mode
~play = {inf.do{arg step;
	// play some notes, then wait.
	var events = rrand(10, 20);
	var energy = rrand(0.7, 1.0);
	("question "++step).postln;
	events.do{
		~eve.next(energy * 127.0);
		energy = energy * 0.8;
		// vary the timing a bit!
		~eve.bar_length = rrand(0.9, 1.0);
	};
	// wipe the memory occasionally
	if (0.4.coin, {
			"brain wiped".postln;
			~eve.reset

		}, {rrand(0.1, 2.0).wait;});
	// wait for a few seconds
	//4.0.wait;
}}.fork;
}
)

// stop the noise... please!
~play.stop;
~eve.reset
