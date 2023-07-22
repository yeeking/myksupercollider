// I wanted to do some performance testing to see what my 
// best live Supercollider settings should be
// to avoid xruns
// running ubuntu 22.04 on thinkpad x1 yoga gen 5
// tested on RME Fireface UCX II and Presonus 1810c
// default kernel, set GRUB_CMDLINE_LINUX_DEFAULT="quiet splash intel_iommu=off" in /etc/default/grub which stops terrible performance on these machines
// conclusion: 
// I could not find a 'golden' jack setup  but
// setting supercoilldiers blocksize to same as jack's p size was good
// and can go quite low latency, like 256 and run quite a lot of stuff
// BUT... my m1 mac mini slaughtered the linux performance annoyingly.

(
o = Server.default.options;
o.blockSize = 256;// probably set this to the same as jacks' p value
o.numOutputBusChannels = 6;
Server.default.boot;
s.doWhenBooted{
~t = 0.3; // 0.5 is about the limit for a presonus 1810c / jackd -d alsa -d hw:1 -r 48000 -p 512 -n 3/ o.blockSize = 128
~t = 0.5; // 0.3 is about the limit for a presonus 1810c / jackd -d alsa -d hw:1 -r 96000 -p 1024 -n 3/ o.blockSize = 1024
~t = 0.125/2 // sorry Linux fans but my m1 mac mini 8gb goes here. really m1s are supercollider beasts

{inf.do{
	{
		var chain;
		chain = Array.fill(100, {
			SinOsc.ar(Rand(300.0, 1000.0));
		}).mean;
			chain = chain * EnvGen.kr(Env.perc(5.0, 5.0), doneAction:2);
		Out.ar([0, 1], chain*0.25);
	}.play;
		~t.wait;
}}.fork;}
)
