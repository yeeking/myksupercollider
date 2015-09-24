MykEventStream : Object {
	var >quant, >max_len, mykPitch, mykSilence, pitch_markov, event_markov, playing_note, note_started, silence_started, midi;

	*new {
		^super.newCopyArgs.prInit;
	}

	prInit{
		playing_note = false;
		quant = 5;
		max_len = 3;
		silence_started = thisThread.seconds;

		event_markov = MykMarkov.new;
		pitch_markov = MykMarkov.new;
		midi = MykMidi.new;

		mykSilence = MykSilence.new;
		mykSilence.callback = {arg amp;
			this.silenceStarts;
		};
		mykPitch = MykFiddle.new;
		mykPitch.callback = {arg note;
			this.noteStarts(note);
		}

	}
	// two events of interest
	// to the event stream
	silenceStarts{
		// what is the transition? note to silence or
		// silence to silence?
		if (playing_note, {// note to silence
			// add a note to the event stream
			// as the note has ended now and we know the length.
			this.addEventToStream(
				'note',
				note_started,
				thisThread.seconds);
		});
		silence_started = thisThread.seconds;
		playing_note = false;
	}
	noteStarts{arg note;
		pitch_markov.addFreq(note);
		// figure out what kind of transition
		// it is
		// (note -> note or silence -> note?)
		if (playing_note, {// note to note
			this.addEventToStream(
				'note',
				note_started,
				thisThread.seconds);
		}, {// silence to note
			this.addEventToStream(
				'silence',
				silence_started,
				thisThread.seconds);
		});
		// remember when this note started
		note_started = thisThread.seconds;
		playing_note = true;
	}

	addEventToStream{arg type, start, end;
		var len, key;
		len = end - start;
		len = this.quantise(5, len);
		len = min(max_len, len);
		key = (type ++ "_" ++len);
		key.postln;
		event_markov.addFreq(key);
	}

	quantise {arg steps, val;
		var frac = val % 1;
		frac = (frac * steps).round / steps;
		val = (val.floor % val) + frac;
		^val;
	}
	// decide what to do next!
	next{
		var event, type, len, note;
		event = event_markov.nextFreq;
		if (event != nil, {
			event = event.split($_);
			type = event[0];
			len = event[1].asFloat;
			type.postln;
			if (type == "note", {
				note = pitch_markov.nextFreq;
				if (note != nil, {
					midi.playNote(0, note, 64, len);
				});
				len.wait;
			});
			if (type == "silence", {
				len.wait;
			});
		})

	}

	run{
		mykSilence.run;
		mykPitch.run;
	}

}