s.boot;
~s = MykSounds.new;
~cl = MykClock.new;
~cl.run;
~cl.add(0, {~s.play(\bd, freq:[50, 60].choose, );}, [0.25, 0.5, 0.25, 0.75, 0.5, 1], [0.75]);
~cl.add(2, {~s.play(\oh, len:0.0001, amp:3);}, [0.25]);
~s.fx(\fft, onoff:1, amp:4);
~cl.add(3, {
	~s.fx(\fft, onoff:1, amp:4);
	{~s.fx(\fft, onoff:0);	}.defer(0.5);
},
[3]);

~cl.add(6, {
	~s.fx(\fft, p1:rrand(0.01, 1.0));
});


~cl.add(1, {~s.play(\fm, );}, [2, 2, 2, 2.25, 1.75, 1.5], [1], 0.25);

~cl.add(4, {
	~s.play(\fm, freq:~fs[0], len:0.2, p1:0.5, p2:0.75, amp:0.75);
	~fs = ~fs.rotate(-1);
}, [0.125], [1]);

~cl.add(8, {
	~s.play(\fm, freq:~fs[1] + 12, len:0.2, p1:0.5, p2:0.75, amp:0.75);
}, [0.25], [1]);

~cl.add(9, {
	~s.play(\fm, freq:~fs[1] - 12, len:0.2, p1:0.1, p2:0.75, amp:0.75);
}, [1/3/2], [1]);

~cl.add(10, {
	~s.fx(\del, onoff:1, p1:rrand(0.0001, 1.0), p2:4.0);
	{~s.fx(\del, onoff:0);}.defer(0.5);
}, [3], [1]);


~cl.add(7, {
	~fs = ~fs + [12, -12, -10, 6, -6].choose;
}, [5]);

~cl.add(8, {
	~fs = Array.fill(3, {rrand(20, 43)});
}, [8]);

~cl.remove(2);



~cl.add(4, {~s.play(\fm, freq:32, p1:1.0, amp:1);}, 0.25);


~s.fx(\dist, onoff:1, amp:0.09, p1:0.005, p2:0.01);

~cl.remove(4)

~s.fx(\dist, onoff:1, p1:0.1);
~cl.add(0, {~s.play(\bd, freq:32, amp:4)});





