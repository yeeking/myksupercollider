// anazlyze a soundfile and store its data to a buffer

s.boot;

(
var sf;
// path to a sound file here
p = "/Users/matthew/Dropbox/Audio/Sounds/amen.wav";
// the frame size for the analysis - experiment with other sizes (powers of 2)
f = 1024;
// the hop size
h = 0.25;
// get some info about the file
sf = SoundFile.new( p );
sf.openRead;
sf.close;
// allocate memory to store FFT data to... SimpleNumber.calcPVRecSize(frameSize, hop) will return
// the appropriate number of samples needed for the buffer
y = Buffer.alloc(s, sf.duration.calcPVRecSize(f, h));
// allocate the soundfile you want to analyze
z = Buffer.read(s, p);
)

// this does the analysis and saves it to buffer 1... frees itself when done
(
SynthDef("pvrec", { arg bufnum=0, recBuf=1, soundBufnum=2;
	var in, chain;
	Line.kr(1, 1, BufDur.kr(soundBufnum), doneAction: 2);
	in = PlayBuf.ar(1, soundBufnum, BufRateScale.kr(soundBufnum), loop: 0);
	bufnum = LocalBuf.new(1024, 1); // uses frame size from above
	// note the window type and overlaps... this is important for resynth parameters
	chain = FFT(bufnum, in, 0.25, 1);
	chain = PV_RecordBuf(chain, recBuf, 0, 1, 0, 0.25, 1);
	// no ouput ... simply save the analysis to recBuf
	}).send(s);
)
a = Synth("pvrec", [\recBuf, y, \soundBufnum, z]);

// you can save your 'analysis' file to disk! I suggest using float32 for the format
// These can be read back in using Buffer.read

y.write(p++".scpv", "wav", "float32");

// play your analysis back ... see the playback UGens listed above for more examples.
(
SynthDef("pvplay", { arg out=0, recBuf=1;
	var in, chain, bufnum;
	bufnum = LocalBuf.new(1024, 1);
	chain = PV_PlayBuf(bufnum, recBuf, MouseX.kr(-1, 1), 50, 1);
	//chain = PV_PlayBuf(bufnum, recBuf, 1, BufSamples.kr(recBuf), 1);
	Out.ar(out, IFFT(chain, 1).dup);
	}).send(s);
SynthDef("pvplay2", { arg out=0, recBuf=1;
	var in, chain, bufnum;
	bufnum = LocalBuf.new(1024);
	chain = PV_BufRd(bufnum, recBuf, MouseX.kr(0.0, 1.0));
	Out.ar(out, IFFT(chain, 1).dup);
	}).send(s);
SynthDef("pvplay3", { arg out=0, recBuf=1, offset=0, length=1, speed = 1;
	var in, chain, bufnum;
	bufnum = LocalBuf.new(1024);
	//chain = PV_BufRd(bufnum, recBuf, Phasor.kr(1, 0.00025, 0, length, offset));
	chain = PV_BufRd(bufnum, recBuf, Phasor.kr(1, 1 / s.sampleRate * 10 * speed, 0, length, offset));
	Out.ar(out, IFFT(chain, 1).dup);
	}).send(s);

);
// mouseX controls speed
b = Synth("pvplay", [\out, 0, \recBuf, y]);
b.free;
// mouseX controls playhead position
c = Synth("pvplay2", [\out, 0, \recBuf, y]);
c.free;
// slicing the break on the language side
d = Synth("pvplay3", [\out, 0, \recBuf, y]);
d.set(\speed, 1.5);
//d.set(\speed, 0.25);
e = {
	inf.do{
		d.set(\offset, [0.625, 0.75, 0.25].choose);
		d.set(\length, [0.5, 0.25, 0.125].choose);
		[0.5, 0.125, 0.25].choose.wait;
	};
}.fork;

d.free;
e.stop;

// free the buffers
[y, z].do({arg me; me.free});