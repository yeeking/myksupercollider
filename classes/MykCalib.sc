// auto calibrator
MykCalib : Object {
	var min;
	var max;

	*new {
		^super.newCopyArgs().prInit;
	}

	prInit {
		min = nil;
		max = nil;
	}
	// scale the incoming value within the previously obseved range
	// and return it
	getCalib{arg val;
		// bootstrap it
		if (min == nil, {min = val;^val});
		if (max == nil, {max = val;^val});

		if (val < min, {min = val});
		if (val > max, {max = val});
		//("range "++min.asString ++ "-"++ max.asString).postln;
		// scale from range min-max to 0-1
		//val.postln;
		val = val - min;
		//val.postln;
		val = val * (1 / (max - min));
		//val.postln;
		^val;

	}
	reset{
		max = nil;
		min = nil;
	}



	// quick unit tests...
	*test{
		var cal, x, eq;
		eq = {arg x, y, ind;
			ind.postln;
			(x.asString ++ " = "++y.asString++": "++(x==y).asString).postln;
		};
		cal = MykCalib.new;
		// first two calls set min and max
		x = cal.getCalib(0.1);
		eq.value(x, 0.1, 1);
		x = cal.getCalib(0.5);
		eq.value(x, 0.5, 2);
		// next call calibrates
		x = cal.getCalib(0.5);
		eq.value(x, 1.0, 3);
		x = cal.getCalib(0.1);
		eq.value(x, 0.0, 4);
		x = cal.getCalib(0.3);
		eq.value(x, (0.3 - 0.1)*(1/0.4), 5);
		// now test recalibration
		x = cal.getCalib(0.6);
		eq.value(x, 1.0, 6);
		x = cal.getCalib(0.5);
		eq.value(x, (0.5 - 0.1)*(1/0.5), 7);
	}

}