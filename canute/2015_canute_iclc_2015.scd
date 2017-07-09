s = Server.local.boot;
m = MykDrummingSimulator.new(tidal_host:"10.0.1.3", tidal_port:4040);
m.click_out = 1;
m.midi_out.latency = 0;
m.resetMarkov;

m.useInternalClock(0);
m.useOscClock;

m.bpm(480);

n = NetAddr("localhost", NetAddr.langPort);

OSCFunc.newMatching({|msg, time, addr, recvPort| \matching.postln}, '/sync'); // path matching

m.train;
m.simulate

~del = Synth("del");
~del2 = Synth("del");
~osc = OSCFunc.newMatching({"tick".postln;}, "/clock");
~osc.remove;
~del.free;
~del2.free;

SynthDef("del", {
	var c;
	c = SoundIn.ar(0);
	c = AllpassC.ar(c, 0.05, SinOsc.kr(4).range(0.001, 0.05),  10.0);
	Out.ar([0, 1], c);
}).add;


m.genTestMidi;
m.midi_out.latency = 0;
m.resetMarkov;

NetAddr.langPort


m.train;
m.simulate


// make the simulator forget everything
m.mykMarkov.vodkaShot;

// every time we write a first order
// entry to the markov, write the entry to a file
(
var id_lookup = MykObjId.new;
~obs_counts = [];
m = MykDrummingSimulator.new;
m.useInternalClock(0);

m.mykMarkov.chain_callbacks = [{arg state, transitions;
	var last_state, obs_counts, obs_sum, trans_str;
	// key[0] will be the most recently observed state
	last_state = id_lookup.getId(state[0]);
	obs_counts = Dictionary.new;
	transitions.do{arg next_state;
		next_state = id_lookup.getId(next_state.asCompileString);
		if (obs_counts.at(next_state) == nil, {obs_counts.put(next_state, 0)});
		obs_counts.put(next_state, obs_counts.at(next_state) + 1);
	};
	obs_sum = obs_counts.sum;

	// build a tr
	trans_str = "";
	obs_counts.keys.do{arg key;
		var count;
		count = obs_counts.at(key);
		//obs_counts.put(key, count / obs_sum);
		trans_str = trans_str ++ "\n{from:" ++ last_state ++ ",to:"++key;
		trans_str = trans_str ++ ", weight:"++(count / obs_sum)++ "},";
	};
//	~obs_counts = ~obs_counts.add(obs_counts);
	// now convert the counts into probabilities
	// now count how many of each
	f=File("/Users/matthew/Desktop/canute.txt", "w");
	f.write("{\nnode:"++last_state++",\n");
	f.write("edges:\n["++ (trans_str) ++ "\n]}");
	f.close;
}];
)


~obs_counts[10]


~id_lookup = (next_id:0);
~id_lookup.put("wtf", 1000);
~id_lookup.at("wtf")
a = Dictionary.new;
a.put("wtf", 1000);
a.at("wtf")

~key
m.free;
m.mykMarkov.nextFreq;
m.mykMarkov.freqChain.keys().size;

m = MykMarkov.new;
m.addFreq(75);
m.addFreq(80);
m.nextFreq;


var all_false;
d = (1:false, 2:false);
all_false = true;
d.do{arg v;
	if (v == true, {all_false = false})
};
all_false;






d = m.mykMarkov.nextFreq;
n = [];
d.collect{|is_on, note|
	if (is_on, {n.add(note)});
};
n.postln;

n = NetAddr("127.0.0.1", NetAddr.langPort); // local machine

o = OSCFunc.newMatching({|msg, time, addr, recvPort| \matching.postln}, '/chat'); // path matching

m = NetAddr("127.0.0.1", NetAddr.langPort); // loopback

OSCFunc.free;
m.sendMsg("/chat", "Hello App 1");
