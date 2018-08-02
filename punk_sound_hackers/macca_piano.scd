

(

// this shows the building of the piano excitation function used below

{

	var strike, env, noise;

	strike = Impulse.ar(0.01);

	env = Decay2.ar(strike, 0.008, 0.04);

	noise = LFNoise2.ar(3000, env);

	[strike, K2A.ar(env), noise]

}.plot(0.03); //.scope

)



(

// hear the energy impulse alone without any comb resonation

{

	var strike, env, noise;

	strike = Impulse.ar(0.01);

	env = Decay2.ar(strike, 0.008, 0.04);

	noise = LFNoise2.ar(3000, env);

	10*noise

}.scope

)




//single strike with comb resonation


(

{

	var strike, env, noise, pitch, delayTime, detune;

	strike = Impulse.ar(0.01);

	env = Decay2.ar(strike, 0.008, 0.04);



	pitch = (36 + 54.rand);



	Pan2.ar(

			// array of 3 strings per note

			Mix.ar(Array.fill(3, { arg i;

				// detune strings, calculate delay time :

				detune = #[-0.05, 0, 0.04].at(i);

				delayTime = 1 / (pitch + detune).midicps;

				// each string gets own exciter :

				noise = LFNoise2.ar(3000, env); // 3000 Hz was chosen by ear..

				CombL.ar(noise,		// used as a string resonator

					delayTime, 		// max delay time

					delayTime,			// actual delay time

					6) 				// decay time of string

			})),

			(pitch - 36)/27 - 1 // pan position: lo notes left, hi notes right

		)



}.scope

)




(

// synthetic piano patch (James McCartney)

var n;

n = 6;	// number of keys playing

play({

	Mix.ar(Array.fill(n, {	// mix an array of notes

		var delayTime, pitch, detune, strike, hammerEnv, hammer;



		// calculate delay based on a random note

		pitch = (36 + 54.rand);

		strike = Impulse.ar(0.1+0.4.rand, 2pi.rand, 0.1); // random period for each key

		hammerEnv = Decay2.ar(strike, 0.008, 0.04); // excitation envelope

		Pan2.ar(

			// array of 3 strings per note

			Mix.ar(Array.fill(3, { arg i;

				// detune strings, calculate delay time :

				detune = #[-0.05, 0, 0.04].at(i);

				delayTime = 1 / (pitch + detune).midicps;

				// each string gets own exciter :

				hammer = LFNoise2.ar(3000, hammerEnv); // 3000 Hz was chosen by ear..

				CombL.ar(hammer,		// used as a string resonator

					delayTime, 		// max delay time

					delayTime,			// actual delay time

					6) 				// decay time of string

			})),

			(pitch - 36)/27 - 1 // pan position: lo notes left, hi notes right

		)

	}))

})

)


