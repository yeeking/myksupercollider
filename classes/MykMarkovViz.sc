/** helper functions to make it easier to visualise
* the data stored in MykMarkov objects
*/
MykMarkovViz : Object {
	var >markov, >viz_url,
	id_lookup
	;

	*new {arg markov, viz_url = "/Users/matthew/Dropbox/Audio/Canute/src/canute/canute.json";
		^super.newCopyArgs(markov, viz_url).prInit;
	}

	prInit {
		this.setMarkov(markov, viz_url);
		id_lookup = MykObjId.new;

	}

	setMarkov{arg markov_, viz_url;
		markov = markov_;
		markov.chain_callbacks = [{arg state, transitions;
			var last_state, obs_counts, obs_sum, node_str;
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
			obs_counts.keys.do{arg key, i;
				var count;
				count = obs_counts.at(key);
				obs_counts.put(key, count / obs_sum);
			};
			node_str = this.nodeToJSON(last_state, obs_counts);
			this.sendStringToVisualiser(node_str, viz_url);
		}];
	}

	/** converts a node in a state transition matrix into a string of JSON*/
	nodeToJSON {arg node_id, edge_data;
		var trans_str, json, f;
		// build a tr
		trans_str = "";
		edge_data.keys.do{arg key, i;
			var prob;
			prob = edge_data.at(key);
			trans_str = trans_str ++ "\n\{\"from\":" ++ node_id ++ ",\"to\":"++key;
			trans_str = trans_str ++ ", \"value\":"++(prob * 50)++ "},";
		};
		// trim off the last comma
		trans_str = trans_str[0 .. trans_str.size - 2];
		json = "{\n\"node\":"++node_id++",\n";
		json = json ++ "\"edges\":\n["++ (trans_str) ++ "\n]}";
		^json;

	}

	sendStringToVisualiser{arg str, viz_url;
		var f;
		//("saving string to "++viz_url).postln;
		f=File(viz_url, "w");
		f.write(str);
		f.close;
	}

	sendBangToVisualiser{arg node_id, viz_url;
		var f;
		f=File(viz_url, "w");
		f.write("{\n\"node\":"++node_id++",\n");
		f.write("\"edges\":\n[]}");
		f.close;
	}
}
	