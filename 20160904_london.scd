(
o = Server.local.options;
o.numOutputBusChannels = 8;
o.numInputBusChannels = 8;
o.hardwareBufferSize = 64;
s = Server.local.boot;
s.doWhenBooted{
	m = MykDrummingSimulator.new(midi_dev:1);// 1 for fireface
	m.click_out = 1;// probably don't want to hear it
	m.midi_out.latency = 0;
	m.resetMarkov;
	m.bpm(250);
	m.useInternalClock(1);
	m.midiout_channel = 0;
}
)
m.stop;
m.simulate
m.train;
m.vodkaShot

m.resetMarkov;

	