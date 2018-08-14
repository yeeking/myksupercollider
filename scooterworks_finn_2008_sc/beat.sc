s=Server.local.boot;
b = Buffer.alloc(s, 1024, 1);
c = Buffer.alloc(s, 88200, 1);
b.free;
c.free;

SynthDef("evan2_capture", {arg active=1;
  var chain, in, rec, trig, run, play;
  in = AudioIn.ar(1)*active;
  chain = FFT(b.bufnum, in);
  chain = PV_JensenAndersen.ar(chain,threshold:0.1,waittime:1.0);
  trig = Decay.ar(0.1*chain,0.01);
  // keep recording after event for 0.6 secs (or buflength?)
  run = EnvGen.kr(Env.perc(0.1, 0.5, 1), gate:trig);
  active = active * run;
  rec = RecordBuf.ar(in, c.bufnum, run:run, loop:0,trigger:trig);
}).send(s);

SynthDef("evan2_play", {arg ratio=1;
  var play, env;
  play = PlayBuf.ar(1, c.bufnum,
	BufRateScale.kr(c.bufnum)*ratio, loop:0);
  env = EnvGen.kr(Env.perc(0.001, 1.0, 0.1), gate:1,doneAction:2);
  Out.ar([20], play*env);
}).send(s);

SynthDef("evan2_play", {arg ratio=1;
  var play, env;
  play = PlayBuf.ar(1, c.bufnum,
	BufRateScale.kr(c.bufnum), loop:1);
  env = EnvGen.kr(Env.perc(0.5, 1.0, 0.1), gate:1,doneAction:2);
  Out.ar([20], play*env);
}).send(s);



SynthDef("evan2_verb", {
  var in;
  in = In.ar([20, 20]);
  //  4.do{
  //	in = AllpassL.ar(in, 0.05, 0.05.rand, 0.5);
  //  };
  Out.ar([3, 4], in);
}).send(s);

d = Synth("evan2_capture");
//d.free;
d.set([\active, 0]);
d.set([\active, 1]);
e = Synth("evan2_verb");
//e.free;

~rats = Array.fill(12, {arg i;1/(i+1)});

t = [0.125, 0.1, 0.01];

r.stop;
r = Routine{
  inf.do{
	Synth("evan2_play", [\ratio, [0.5, 1,
	  2].choose*~rats.choose]);
~rats = ~rats.rotate(1);
t.choose.wait;
};
}.play;

r.stop;

b.free;

k = {Impulse.ar(MouseX.kr(0.1, 1000))}.play
k.free;