s.boot;
~f = MykFiddle.new;
~f.run;
~mark = MykMarkov.new;
~viz = MykMarkovViz.new(~mark);
~f.callback = {arg f; f.postln;~mark.addFreq(f)};
~on = MykBonk.new;
~on.run;
~on.callback = {arg lev;
	var f;
	if (lev > 0.25, {
	f = ~mark.nextFreq;
	f.postln;
	~s.play(\fm, ~mark.nextFreq - 24);
	});
};
~s.fx(\del, onoff:1);

~on.callback = {};
~s = MykSounds.new;
~s.play(\bass, rrand(32, 65));
