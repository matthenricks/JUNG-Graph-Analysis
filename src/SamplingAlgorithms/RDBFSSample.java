package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import edu.uci.ics.jung.graph.Graph;

/***
 * This class is a sampling algorithm that samples a graph with a mixture of BFS and Random Node sampling in addition to a max degree taken bias
 * @author MOREPOWER
 *
 */
public class RDBFSSample extends RandomBFSSample {

	int maxDegree;

	// It will use Max Degree as a limiter for how much it should take
	// NOTE: Max Degree is a number for how many neighbors it will take. This will bias it towards taking 
	// more influential nodes since they'll be more chances and reduce the overall network size as an upper bound
	// Upper Bound = (#nodes * alpha) * (maxDegree+1)
	public RDBFSSample(double alpha, double threshold, int maxDegree, int seed) {
		super(alpha, threshold, seed);
		this.maxDegree = maxDegree;
	}
	
	public RDBFSSample(double alpha, double threshold, int maxDegree) {
		super(alpha, threshold);
		this.maxDegree = maxDegree;
	}

	@Override
	/***
	 * Adds the node center, but places a limit on it
	 */
	protected boolean addNode(String current_vertex,
			Graph<String, String> sampledBFSGraph,
			Graph<String, String> parentGraph, HashSet<String> processed,
			ArrayList<String> neighbors) {
		
		// Add the area around the selected node, itself and neighbors
		sampledBFSGraph.addVertex(current_vertex);
		processed.add(current_vertex);
		
		// Shuffle the neighbors so each has an even probability of being randomly selected
		ArrayList<String> currNeighbors = new ArrayList<String>(parentGraph.getNeighbors(current_vertex));
		Collections.shuffle(currNeighbors);
		for(int counter = 0; counter < currNeighbors.size() && counter < maxDegree; counter++) {
			String vertex = currNeighbors.get(counter);
			// Add new vertex (if needed)
			if (!sampledBFSGraph.containsVertex(vertex)) {
				sampledBFSGraph.addVertex(vertex);
				neighbors.add(vertex);
			}
			String nEdge = sampledBFSGraph.findEdge(vertex, current_vertex);
			if (!sampledBFSGraph.containsEdge(nEdge))
				sampledBFSGraph.addEdge(parentGraph.findEdge(vertex, current_vertex), vertex, current_vertex, parentGraph.getDefaultEdgeType());
		}
		
		return true;
	}

}
