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
				//~snd.play(\fm, amp:4,  freq:rrand(20, 40), p1:1.0.rand, p2:1.0.rand);
				~snd.fx(\rev, onoff:0);
				~snd.fx(\del, onoff:0);
		});
	};
};
)