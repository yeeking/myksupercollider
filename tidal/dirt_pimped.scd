SuperDirt.start;

(
SynthDef('grunge2', {arg out, grunge = 0, pan=0;
var in, f,p;
p = min(1, grunge);
p = max(0, p);
f = Select.kr(p*3.asInteger, [1,2,3]);
in = In.ar(out, 2);
in = in.mean;
in = Compander.ar(in, in,
        thresh: 0.01,
        slopeBelow: 10,
        slopeAbove: 1,
        clampTime: 0.01,
        relaxTime: 0.01
    );
in = 0.5 * CombC.ar(in, 0.02, 0.01 + (SinOsc.kr(f*25).range(-0.005,
0.005) * p), 0.3 + (p));
ReplaceOut.ar(out, Pan2.ar(in, pan));
}).add;

SynthDef('glass2', {arg out, glass = 0.2, pan=0;
var in, f, c1, c2;
p = min(1, glass);
p = max(0, p);
f = Select.kr(p*5.asInteger, [1,2,3,4,5]);
f = p * 100;
in = In.ar(out,2);
// set up on off to the fx
in = in.mean;
//in = Normalizer.ar(in, 1.0, 0.01);
c1 =0.5 *  CombC.ar(in, 0.02, 0.01 + (SinOsc.ar(f*25).range(-0.005,
0.005) * p), 0.4 + (p));
c2 =0.5 *  CombC.ar(in, 0.02, 0.01 + (SinOsc.ar(f*30).range(-0.005,
0.005) * p), 0.3 + (p));

        ReplaceOut.ar(out, [c1, c2]);
}).add;

~dirt.addModule('grunge', { |dirtEvent|
  dirtEvent.sendSynth('grunge2',
    [
      out: ~out,
      grunge: ~grunge
    ]
  )
}, { ~grunge.notNil });

~dirt.addModule('glass', { |dirtEvent|
  dirtEvent.sendSynth('glass2',
    [
      out: ~out,
      glass: ~glass
    ]
  )
}, { ~glass.notNil });

)


