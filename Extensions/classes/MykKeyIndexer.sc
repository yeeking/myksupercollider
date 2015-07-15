/** maintains an object to id lookup which can be used to convert
* complex dictionaries into simple int-int mappings
*/
MykObjId : Object {

	var dict;

	*new{
		^super.newCopyArgs().prInit;
	}

	prInit{
		dict = Dictionary.new;
		dict.put('next_id', 0);
	}
/** get the unique id for the sent object. Generates an id if needed. */
	getId{arg obj;
		var id;
		id = dict.at(obj);
		if (id == nil, {
			id = dict.at('next_id');
			dict.put('next_id', (id + 1));
			dict.put(obj, id);
		});
		^id;
	}

}