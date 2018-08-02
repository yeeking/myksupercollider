// TODO:

// add control
// - map a control to a synth param

// set control

// change sound

// osc interface


// supercollider synthesizer set
// that can be controlled
// via OSC messages
// designed for use with the
// grovepi sensors ish

PunkSoundHackers : Object {

	var currentSynth, synthDefs, ctlToParamMap;

	/** public interface */
	*new{
		^super.new.prInit;
	}
	// stop synths and remove
	// OSC responders
	stop{
	}

	// change the sound
	// this can be done over OSC
	// with /set_sound name
	// if soundName param is false,
	// just go to the next sound
	setSound{arg soundName = false;
	}

	// adds a control to the synthesizer
	// This can be done over OSC
	// with /add_control ctlName 0 100

	// the user gets to name the control
	// each control that gets added
	// is mapped automatically to a
	// parameter on the synth
	// if they set a paramName
	// they can choose what the control does
	addControl{arg ctlName="slider", min=0, max=100, paramInd = false;
		//var paramInd;
		if (paramInd == false, {
			// normal mode - param name not specified
			// so just map the control to an unassigned
			// param
			paramInd = this.prMax(ctlToParamMap.values.asArray);
			if (paramInd == nil, {paramInd = 0});

		});
		ctlToParamMap.put(ctlName, Dictionary["index"->paramInd, "min"->min, "max"->max]);
		("ctl added: "++ctlName++" "++paramInd++" "+min++"").postln;


	}

	// set the value of a controller
	// that was previously named with addControl
	// can be called from OSC with
	// /set_control

	//
	setControl{arg ctlName, val;
	}


	/** private */

	// constructor
	prInit{
		this.prSendSynthDefs;

		synthDefs = ["FM", "add"];
		ctlToParamMap = Dictionary[];


		{this.prStartSynths}.defer(2);
	}

	prStartSynths{
		currentSynth = Synth.new("psh_FM");
	}
	// deletes and recreates the synths
	prResetSynths{
	}

	// setup the OSC receiver
	prSetupOSCResponder{
	}

	// remove the OSC receiver
	prRemoveOSCRespnder{
	}

	prSendSynthDefs{
		SynthDef("psh_FM", {arg freq = 220, p1 = 0.5;
			var c;
			c = PMOsc.ar(freq, freq*0.25, p1*5);
			Out.ar([0, 1], c*0.5);
		}).add;
		SynthDef("psh_add", {arg freq = 220, p1 = 0.5;
			var c;
			c = Array.fill(20, {arg i;
				var f = ((i+1) * freq);
				f = f + LFDNoise1.kr(0.1).range(0, f/10);
				SinOsc.ar(freq);
			}).mean;
			Out.ar([0, 1], c*0.5);
		}).add;
	}
	prMax{arg arr;

		var m = arr[0]["index"];
		if (m == nil, {^nil});// nothing there
		arr.do{arg v, i;if (v["index"] > m, {m = v["index"]});};
		^m;
	}
}
