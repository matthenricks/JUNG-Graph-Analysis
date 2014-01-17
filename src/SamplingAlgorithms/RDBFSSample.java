package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;

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
		// This obtains all of the connecting edges to this node
		ArrayList<String> currNeighbors = new ArrayList<String>(parentGraph.getIncidentEdges(current_vertex));
		// NOTE: This will count both input and output
		Collections.shuffle(currNeighbors);
		for(int counter = 0; counter < currNeighbors.size() && counter < maxDegree; counter++) {
			String edge = currNeighbors.get(counter);
			if (!sampledBFSGraph.containsEdge(edge)) {
				// Add the new vertex and edge to the sample graph
				Pair<String> edges = parentGraph.getEndpoints(edge);
				if (edges.getFirst() == current_vertex) {
					if (!sampledBFSGraph.containsVertex(edges.getSecond()))
						sampledBFSGraph.addVertex(edges.getSecond());
					sampledBFSGraph.addEdge(edge, current_vertex, edges.getSecond());
				} else {
					if (!sampledBFSGraph.containsVertex(edges.getFirst()))
						sampledBFSGraph.addVertex(edges.getFirst());
					sampledBFSGraph.addEdge(edge, edges.getFirst(), current_vertex);
				}
			}
		}
		
		return true;
	}

}
