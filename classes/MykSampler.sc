MykSampler : Object {
	var >out_bus, buffer;

	*new {arg out_bus = 0;
		^super.newCopyArgs(out_bus).prInit;
	}

	prInit {
		var server;
		server = Server.local;
		buffer = Buffer.alloc(server,server.sampleRate*10.0,1);

		this.sendSynthDefs;
	}

	sendSynthDefs {
		SynthDef("MykSampler_rec", {
		}).add;

		SynthDef("MykSampler_play", {
		}).add;

	}

}