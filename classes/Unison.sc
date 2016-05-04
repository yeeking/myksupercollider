Unison : Object {
	var
	// config
	audio_in, audio_out, >ratios, >times,
	// state
	buffer, fft_buffer, rec_synth, play_r;
	*new {arg audio_in=1, audio_out=[4, 5], ratios=[1];
		^super.newCopyArgs(audio_in, audio_out, ratios).prInit;
	}
	prInit{
		var server;
		server = Server.local;
		buffer = Buffer.alloc(server, 44100, 1);
		fft_buffer = Buffer.alloc(server, 512, 1);
		this.sendSynthDefs;
		times = [0.1, 0.125];
		play_r = Routine{
			inf.do{
				Synth("unison_play_fade", [\play_out, audio_out.choose, \ratio, ratios.choose, \play_buff, buffer]);
				times.choose.wait;
			};
		};
	}

	run{
		// start the recording routine
		rec_synth = Synth("unison_record", [\rec_in, audio_in, \rec_buf, buffer, \fft_buff, fft_buffer]);
		play_r.play;
	}

	free{
		rec_synth.free;
		buffer.free;
		fft_buffer.free;
		play_r.stop;
	}

	sendSynthDefs{
		var server;
		server = Server.local;
		SynthDef("unison_record", {arg rec_in=1, active=1, rec_buff, fft_buff;
			var chain, in, rec, trig, run, play;
			in = AudioIn.ar(rec_in)*active;
			chain = FFT(fft_buff, in);
			chain = PV_JensenAndersen.ar(chain,threshold:0.025,waittime:1.0);
			trig = Decay.ar(0.1*chain,0.01);
			//trig = Onsets.kr(chain, MouseX.kr(0, 2)).poll;
			// keep recording after event for 0.6 secs (or buflength?)
			run = EnvGen.kr(Env.perc(0.1, 0.5, 1), gate:trig);
			active = active * run;
			rec = RecordBuf.ar(in, rec_buff, run:run, loop:0,trigger:trig);
		}).send(server);

		SynthDef("unison_record_new", {arg rec_in=1, active=1, rec_buff, fft_buff;
			var chain, in, rec, trig, run, play, onsets, pips;
			in = AudioIn.ar(rec_in);
			chain = FFT(fft_buff, in);
			//chain = PV_JensenAndersen.ar(chain,threshold:0.025,waittime:1.0);
			//trig = Decay.kr(0.1*chain,0.01);
			onsets = Onsets.kr(chain, MouseX.kr(0, 1), \rcomplex).poll;
			//pips = WhiteNoise.ar(EnvGen.kr(Env.perc(0.001, 0.1, 0.2), onsets));
			// keep recording after event for 0.6 secs (or buflength?)
			run = EnvGen.kr(Env.perc(0.1, 0.5, 2), gate:onsets);
			active = active * run;
			//run.poll;
			rec = RecordBuf.ar(in, rec_buff, run:run, loop:0,trigger:run);
			//Out.ar(0, pips)
		}).send(server);


		SynthDef("unison_play_fade", {arg ratio=1, play_buff, play_out=0;
			var play, env;
			play = PlayBuf.ar(1, play_buff,
				BufRateScale.kr(play_buff)*ratio, loop:1);
			env = EnvGen.kr(Env.perc(1.0, 1.0, 0.1), gate:1,doneAction:2);
			Out.ar(play_out, play*env);
		}).send(server);
	}
}

// possibly slighty more reliable version of unison

Unison2 : Object {

	var
	// config
	>audio_in, >audio_out, >ratios, >times,
	// state
	buffers, play_buff, rec_buff, fft_buffer, onset_synth, comp_synth, play_r, osc_resp, recording;

	*new {arg audio_in=1, audio_out=0, ratios=[1], midi_mode = 0;
		^super.newCopyArgs(audio_in, audio_out, ratios).prInit(midi_mode);
	}

	prInit{arg midi_mode;
		var server;
		server = Server.local;
		buffers = [Buffer.alloc(server, 100000, 1), Buffer.alloc(server, 100000, 1)];
		play_buff = 0;
		rec_buff = 1;
		//fft_buffer = Buffer.alloc(server, 512, 1);
		this.sendSynthDefs;
		times = [0.01, 0.05];
		recording = 0;
		// the synthesis routine
		play_r = Routine{
			inf.do{
				if (recording == 0, {
					Synth("unison2_play_fade", [\play_out, [32,33].choose, \ratio, ratios.choose, \play_buff, buffers[play_buff]]);});
				times.choose.wait;
			};
		};
		if (midi_mode == 0, {
			// the osc responder which responds to onsets
			osc_resp = OSCresponderNode(server.addr, '/tr', {arg time,responder,msg;
				this.startRecord();
				//[time, responder, msg].postln;
			}).add;
		}, {
				var m, n;
				("setting up midi trigger on note "++midi_mode).postln;
				m = MPD.new;
				n = MIDIFunc.noteOn({arg ...args;
					var note;
					note = args[1];
					if (note == midi_mode, {
						args.postln;
						this.startRecord();
					});
				}); // match any noteOn
		});
	}

	// call this to start playback
	run{
		onset_synth = Synth("unison2_onsets", [\rec_in, audio_in, \fft_b, fft_buffer]);
		comp_synth = Synth("unison2_comp", [\audio_out, audio_out]);
		play_r.reset;
		play_r.play;
	}

	free{
		onset_synth.free;
		buffers[0].free;
		buffers[1].free;
		fft_buffer.free;
		comp_synth.free;
		play_r.stop;
		osc_resp.remove;
	}

	// this is called by the oscresponder when an onset(send trig) occurs
	startRecord{
		//"unison2:onset".postln;
		if (recording == 0, {
			play_buff = 1-play_buff;
			rec_buff = 1-rec_buff;
			//"unison2: recording".postln;
			recording = 1;
			Synth("unison2_rec", [\rec_in, audio_in, \rec_b, buffers[rec_buff]]);
			// wait a while then reset recording
			{0.2.wait;recording = 0;}.fork;
		});
	}

	sendSynthDefs {
		var s;
		s = Server.local;

		// simple recorder which fills up the buffer then dies

		SynthDef("unison2_rec", {arg rec_in, rec_b;
			var in, rec, env;
			in = AudioIn.ar(rec_in);
			// envelope the input to remove clicks
			env = EnvGen.kr(Env.new([0, 1, 1, 0], [0.001, 1.0, 0.001]), doneAction:2, gate:1);
			rec = RecordBuf.ar(in*env, rec_b, loop:0);
		}).send(s);

		SynthDef("unison2_play_fade", {arg ratio=1, play_buff, play_out=0;
			var play, env, amp;
			//amp = In.kr(30);
			amp = 1;
			play = PlayBuf.ar(1, play_buff,
				BufRateScale.kr(play_buff)*ratio, loop:1);
			//  env = EnvGen.kr(Env.perc(0.5, 0.5, 0.1), gate:1,doneAction:2);
			env = EnvGen.kr(Env.new([0, 0.1, 0], [0.5, 0.5], 'welch'), gate:1,doneAction:2);
			// send it out to one of two buses where a compressor is listening
			Out.ar(32, play*env*amp);
			//Out.ar([0, 1], play*env);
		}).send(s);

		SynthDef("unison2_comp", {arg audio_out = 0;
			var in1, in2, comp1, comp2;

			in1 = In.ar(32);
			in2 = In.ar(33);
			in1 = Normalizer.ar(in1, 0.5) * SinOsc.kr(1.0/Rand(5.0, 10.0)).range(0.25, 1.0);
			in2 = Normalizer.ar(in2, 0.5) * SinOsc.kr(1.0/Rand(5.0, 10.0)).range(0.25, 1.0);
			in1 = in1 + LPF.ar(PitchShift.ar(in1,pitchRatio:0.5));
			in2 = in2 + LPF.ar(PitchShift.ar(in2,pitchRatio:0.5));

			// comp1 = Compander.ar(in1, in1,
			// 	thresh: 0.25,
			// 	slopeBelow: 1,
			// 	slopeAbove: 0.5,
			// 	clampTime: 0.01,
			// 	relaxTime: 0.01
			// ) * SinOsc.kr(1.0/Rand(5.0, 7.0)).range(0.5, 1.0);
			// comp2 = Compander.ar(in2, in2,
			// 	thresh: 0.25,
			// 	slopeBelow: 1,
			// 	slopeAbove: 0.5,
			// 	clampTime: 0.01,
			// 	relaxTime: 0.01
			// ) * SinOsc.kr(1.0/Rand(5.0, 7.0)).range(0.5, 1.0);

			Out.ar(audio_out, in1 + in2);
		}).send(s);

		SynthDef("unison2_onsets", {arg rec_in=1, fft_b;
			var onsets, fft, in;
			in = AudioIn.ar(rec_in);
			//in = Normalizer.ar(in, 1.0);
			//fft = FFT(fft_b, in);
			fft = FFT(LocalBuf(2048, 2), in);
			onsets = Onsets.kr(fft, 0.2, \rcomplex);
			SendTrig.kr(onsets, 99, 0.5);
			// implement an lfo to vary the volume of the effect slowy.
			//Out.kr(30, SinOsc.kr(1.0/Rand(5.0, 7.0)).range(0.5, 1.0));
		}).send(s);

	}


}