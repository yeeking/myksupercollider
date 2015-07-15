/**
* Observes midi input against a click
*/
MykMidiObserver : Object {

	var
	channel,
	midi_responder,
	step_events,
	>callback,
	>debug,
	// all observed notes
	obs_notes;

	*new{arg channel = 0;
		^super.newCopyArgs(channel).prInit;
	}

	prInit{
		debug = true;
		// in case we aren't connected to the midi interface yet
		MIDIIn.connectAll;
		// set a default callback that prints
		// notes observed this step
		callback = {arg notes;
			"notes obs this step: ".postln;
			notes.postln;};
		// create a midi responder for incoming notes
		midi_responder = MIDIFunc.noteOn({arg ...args;
			this.noteOn(args[1], args[0]);
			if (debug, {
				"MykMidiObserver:: debug mode (MykMidiObserver.debug = false to kill):: got some midi".postln;
				args.postln

			});
			}
		);
		// use this to store observed notes
		obs_notes = Dictionary.new;
		this.initStepEvents;
	}


	/** time period has ended - send observations to callback and reset memory*/
	step{
		if (step_events.keys().size > 0, {
			callback.value(step_events);
			this.initStepEvents();
		})
	}

	initStepEvents{
		// maintain a list of all observed note numbers
		// and set them all to rest by default
		step_events = obs_notes.collect{|item| false};
		//step_events = ();
	}

	/** triggered when a midi noteon happens.
	* Causes it to store that event in this time slice
	*/
	noteOn{arg note, vel;
		// remember we have seen this note
		// so can correctly generate rests later
		obs_notes.put(note, true);
		// nah - this needs to be a dictionary
		// notenum:rest_state
		step_events.put(note, true);
	}

	free{
		midi_responder.free;
	}

}

/**
* unit tests for MykMidiObserver
*/

MykMidiObserverTest : Object{
	*test{
		var obs, notes;
		obs = MykMidiObserver.new;
		obs.callback = {arg obs_notes; notes = obs_notes};

		obs.step();
		// notes should be empty
		"empty dict? ".postln;
		notes.postln;
		// send some notes in
		MIDIIn.doNoteOnAction(1, 1, 5, 64);
		// should print {5 -> true)
		obs.step();
		"dict with 5 set to true".postln;
		notes.postln;

		obs.step();
		"dict with 5 set to false".postln;
		notes.postln;

		MIDIIn.doNoteOnAction(1, 1, 10, 64);
		MIDIIn.doNoteOnAction(1, 1, 11, 64);
		obs.step();
		"dict with 5 set to false, 10 and 11 are true".postln;
		notes.postln;

		obs.free;

	}
}