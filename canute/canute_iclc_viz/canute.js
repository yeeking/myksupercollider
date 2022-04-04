var nodeIds, shadowState, nodesArray, nodes, edgesArray, edges, network, node_ids, edge_ids;



$(function() {
    
    nodes = new vis.DataSet([]);
    edges = new vis.DataSet([]);
    startNetwork();
    // set up a periodic load operation
    // on the json file
    console.log("setting up the poller...");
    // we'll use this to remember which nodes we have...
    node_ids = [];
    edge_ids = [];
    var poller =  setInterval(function(){
	loadData();
    }, 500)
    var poller =  setInterval(function(){
	//network.stabilize();
    }, 30000)
});


function loadData(){
    $.getJSON( "./canute.json?", function(data){
//	console.log(data);
//	console.log("doing node update on "+data.node);
	changed_node = data.node;
	changed_edges = data.edges;
	
	// step 1: do we have this node in nodesArray?
	if (checkForNodeId(changed_node, node_ids)){
	    // yes we have this node... just give it a poke
	    // find the vis.js id for this node
	    //	    var node_id = 
	    //var node = {id:changed_node, label:changed_node};
	    var inactiveCol = '#00FF00';
	    var activeCol = '#0000FF';
	    // set every node to the inavtive color
	    nodes.update([{id:changed_node, x: 150, color:{background:inactiveCol}}]);
	    for (var i=0;i<node_ids.length;i++){
		if (node_ids[i] != changed_node){
		    nodes.update([{id:node_ids[i], color:{background:activeCol}}]);
		}
	    }
	}
	else {
	    // no? push it onto nodesArray
	    console.log("adding new node "+changed_node);
//	    setTimeout(function(){
	    var node = {id:changed_node, label:changed_node};		
	    nodes.add(node);
	    node_ids.push(changed_node);
	   
//	    }, Math.random()*3000);
	}
	// step 2: check for all the nodes in the edges
	for (var i=0;i<changed_edges.length;i++){
	    if (! checkForNodeId(changed_edges[i].from, node_ids)){
		console.log("adding new node "+changed_edges[i].from);
		nodes.add({id:changed_edges[i].from, label:changed_edges[i].from});
		node_ids.push(changed_edges[i].from);
	    }
	    if (! checkForNodeId(changed_edges[i].to, node_ids)){
		console.log("adding new node "+changed_edges[i].to);
		nodes.add({id:changed_edges[i].to, label:changed_edges[i].to});
		node_ids.push(changed_edges[i].to);
	    }
	}
	// step 3: remove edges from changed_node
	for (var i=0;i<changed_edges.length;i++){
	    var matched_id = edgeNeedsUpdate(changed_edges[i], edge_ids);
	    if (!matched_id){
		edge_id = edges.add(changed_edges[i]);
		edge_ids.push({id:edge_id[0],
			       from:changed_edges[i].from,
			       to:changed_edges[i].to,
			       value:changed_edges[i].value
			      });
	    }
	    else {// already got the edge - just update.
		if (matched_id != -1){
		    changed_edges[i].id = matched_id;
		    // spread the timing out a bit
		    var edge = changed_edges[i];
		    setTimeout(function(){
			edges.update(edge);
		    }, Math.random()*3000);
		    
		}
	    }
	}// end iterate changed edges
	//console.log(nodes);
	//network.stabilize();
    });
}

function edgeNeedsUpdate(edge, edge_ids){
    var needs_update = false;
    for (var i=0;i<edge_ids.length;i++){
	if (edge.to == edge_ids[i].to &&
	    edge.from == edge_ids[i].from
	   ){
	    if (edge_ids[i].value != edge.value){
		edge_ids[i].value = edge.value;
		needs_update = edge_ids[i].id;
	    }
	    else {
		// node exists, but does not need an update or an add
		needs_update = -1;
	    }
	    break;
	}
    }
    return needs_update;
}


function startNetwork() {
    
    shadowState = false;
    
    // create a network
    var container = document.getElementById('mynetwork');
    var data = {
        nodes: nodes,
        edges: edges
    };
    var options = {
	"physics": {
	    "barnesHut": {
		"damping": 0.7
	    }, 
	    "maxVelocity": 3,
	    "minVelocity": 0.01,
	}
    };

    network = new vis.Network(container, data, options);
}

function addNode() {
    var newId = (Math.random() * 1e7).toString(32);
    nodes.add({id:newId, label:"I'm new!"});
    nodeIds.push(newId);
}

function changeNode1() {
    var newColor = '#' + Math.floor((Math.random() * 255 * 255 * 255)).toString(16);
    nodes.update([{id:1, color:{background:newColor}}]);
}

function removeRandomNode() {
    var randomNodeId = nodeIds[Math.floor(Math.random() * nodeIds.length)];
    nodes.remove({id:randomNodeId});

    var index = nodeIds.indexOf(randomNodeId);
    nodeIds.splice(index,1);
}

function changeOptions() {
    shadowState = !shadowState;
    network.setOptions({nodes:{shadow:shadowState},edges:{shadow:shadowState}});
}

function resetAllNodes() {
    nodes.clear();
    edges.clear();
    nodes.add(nodesArray);
    edges.add(edgesArray);
}

function resetAllNodesStabilize() {
    resetAllNodes();
    network.stabilize();
}

function setTheData() {
    nodes = new vis.DataSet(nodesArray);
    edges = new vis.DataSet(edgesArray);
    network.setData({nodes:nodes, edges:edges})
}

function resetAll() {
    if (network !== null) {
        network.destroy();
        network = null;
    }
    startNetwork();
}




function checkForNodeId(node_id, nodeArray){
    var have_node = false;
    for (var i=0;i<nodeArray.length; i++){
        if (nodeArray[i] == node_id) {
	    have_node = true;
	    break;
        }
    }
    return have_node;
}

