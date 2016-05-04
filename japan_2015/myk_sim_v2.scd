s = Server.local.boot;
~ctl_funcs.at('p_1')
// attempt to capture midi data into a markov chain
(
~pad_markov = MykMarkov.new;
~knob_markov =
MykMarkov.new;
~mpd = MPD.new(lpd_mode:1);
~ctl_funcs = Dictionary.newFrom (List[
	"p_1", {
		~log.value("s1 start rec");
		~sampler1.startRecord;
	},
	"p_2", {
		~log.value("s1 stop rec");
		~sampler1.stopRecord;},
	"p_3", {
		~log.value("s1 start play");
		~sampler1.startPlay;},
	"p_4", {
		~log.value("s1 drone play");
		~sampler1.stopPlay;},
	"c_1", {arg val;
		~log.value("s1 grain len");
		~sampler1.updateCtl(0, val, true)},
	"c_2", {arg val;
		~log.value("s1 grain count");
		~sampler1.updateCtl(1, val, true)},
	"c_3", {arg val;
		~log.value("s1 pitch");
		~sampler1.updateCtl(2, val, true)},
	"c_4", {arg val;
		~log.value("s1 position");
		~sampler1.updateCtl(3, val, true)},
	"p_5", {~log.value("s2 start rec");
		~sampler2.startRecord; },
	"p_6", {~log.value("s2 stop rec");
		~sampler2.stopRecord;},
	"p_7", {~log.value("s2 start play");
		~sampler2.startPlay;},
	"p_8", {~log.value("s2 drone play");
		~sampler2.stopPlay;},
	"c_5", {arg val; ~log.value("s2 grain len");
		~sampler2.updateCtl(0, val, true)},
	"c_6", {arg val; ~log.value("s2 grain count");
		~sampler2.updateCtl(1, val, true)},
	"c_7", {arg val; ~log.value("s2 pitch");
		~sampler2.updateCtl(2, val, true)},
	"c_8", {arg val; ~log.value(("s2 position"++val));
		~sampler2.updateCtl(3, val, true)},
	//"c_14", {arg vel; (~log.value("p9 func: ver "++(vel)));~v.set(\len, vel / 127);},
	// fx controllers
	//"p_9", {arg vel; ~v.set(\len, max(vel, 5) / 128);},
	"p_10", {},
	"p_11", {},
	"p_12", {},

]);
~sampler1 = GranularSampler2.new(setup_midi:0);
~sampler2 = GranularSampler2.new(setup_midi:0);

8.do{arg i;
	~log.value("Setting up pad "++(i+1));
	~mpd.onNoteOn(i+1, {arg vel;
		var key;
		//vel.postln;
		~pad_markov.addFreq("p_"++(i+1));
		~pad_markov.addBeat(min_beat:0.025, max_beat:4.0);
		//~pad_markov.addBeat();
		~ctl_funcs.at("p_"++(i+1)).value(vel);
	});
};

8.do{arg i;
	var cc = i+1;
	~mpd.onCC(cc, {arg val;
		//cc.postln;
		//val.postln;
		var key;
		key = "c_"++(i+1);
		~knob_markov.addFreq(key++"_"++(val));
		~knob_markov.addBeat(min_beat:0.025, max_beat:4.0);
		~ctl_funcs.at(key).value(val);
	});
};

~log = {arg msg; msg.postln;};

)

(
~pad_ctl = {
	inf.do{
		var pad, beat;
		pad = ~pad_markov.nextFreq;
		if (pad != nil, {
			~log.value(pad);
			~ctl_funcs[pad].value;
		});
		beat = ~pad_markov.nextBeat;
		if (beat == nil, {beat = 0.5});
		beat.wait;
	};
}.fork;

~knob_ctl = {
	inf.do{
		var info, cc, val, beat;
		info = ~knob_markov.nextFreq;//
		if (info != nil, {
			~log.value(info);
			info = info.split($_);
			cc = info[1].asInt();
			val = info[2].asInt();
			~ctl_funcs["c_"++cc].value(val);
		});
		beat = ~knob_markov.nextBeat;
		if (beat == nil, {beat = 0.5});
		beat.wait;
	};
}.fork;

)


~knob_ctl.stop;
~pad_ctl.stop;



~log = {arg msg; };

~pad_markov.nextBeat;
~pad_markov = MykMarkov.new;
~pad_markov.addBeat(0.1);
~pad_markov.addBeat();
~pad_markov.beatChain


SynthDef("verb", {arg len = 1.0;
	var c;
	c = In.ar(0);
	len = max(0.01, len);
	4.do{
		c = AllpassC.ar(c, 0.2, Rand(0.05, 0.2) * len, Rand(2.0, 10.0));
	};
	Out.ar([0, 1], c);
}).add;

~v = Synth("verb");
~v.free;
~v.set(\len, 1.0);

~rec = MykRecord.new;
~rec.record;
~rec.save;





