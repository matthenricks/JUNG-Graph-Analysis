package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import Utils.CSV_Builder;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;

/***
 * This class is a sampling algorithm that samples a graph with a mixture of BFS and Random Node sampling in addition to a max degree taken bias
 * @author MOREPOWER
 *
 */
public class RDBFSSample extends RandomBFSSample {

	int maxDegree;
	int iterations;
	
	// It will use Max Degree as a limiter for how much it should take
	// NOTE: Max Degree is a number for how many neighbors it will take. This will bias it towards taking 
	// more influential nodes since they'll be more chances and reduce the overall network size as an upper bound
	// Upper Bound = (#nodes * alpha) * (maxDegree+1)
	public RDBFSSample(double alpha, double threshold, int maxDegree, int seed) {
		super(alpha, threshold, seed);
		this.maxDegree = maxDegree;
		iterations = 0;
	}
	
	public RDBFSSample(double alpha, double threshold, int maxDegree) {
		this(alpha, threshold, maxDegree, (new Random()).nextInt());
	}

	@Override
	// Real Iterations, Real Alpha, Real Threshold
	public CSV_Builder sampleGraph(Graph<String, String> parentGraph) {
		// Add the average amount of neighbors and the number of iterations
		CSV_Builder parentCSV = super.sampleGraph(parentGraph);
		return new CSV_Builder(new Integer(iterations), parentCSV);
	}
	
	@Override
	/***
	 * Adds the node and it's immediate neighbors, but places a limit on the amount of neighbors
	 */
	protected boolean addNode(String current_vertex, Graph<String, String> parentGraph) {
		// Add the selected node first
		sampledGraph.addVertex(current_vertex);
		addedCoreVertexes.add(current_vertex);
		
		// Shuffle the neighbors so each has an even probability of being randomly selected
		// This obtains all of the connecting edges to this node
		ArrayList<String> currNeighbors = new ArrayList<String>(parentGraph.getNeighbors(current_vertex));
		Collections.shuffle(currNeighbors, new Random(seed));
		
		// Add all the neighbors that are first degree
		availableNeighbors.addAll(currNeighbors);
		// Add all the neighbors
		for(int counter = 0; counter < currNeighbors.size() && counter < maxDegree; counter++) {
			String neighbor = currNeighbors.get(counter);
			sampledGraph.addVertex(neighbor);
			// Add all the edges between the current vertex and it's selected neighbor
			Collection<String> edges = parentGraph.findEdgeSet(current_vertex, neighbor);
			for (String edge : edges) {
				if (!sampledGraph.containsEdge(edge)) {
					Pair<String> endPoints = parentGraph.getEndpoints(edge);
					sampledGraph.addEdge(edge, endPoints.getFirst(), endPoints.getSecond());
				}
			}
			edges.clear();
			edges = parentGraph.findEdgeSet(neighbor, current_vertex);
			for (String edge : edges) {
				if (!sampledGraph.containsEdge(edge)) {
					Pair<String> endPoints = parentGraph.getEndpoints(edge);
					sampledGraph.addEdge(edge, endPoints.getFirst(), endPoints.getSecond());
				}
			}
			// Add all the potential neighbors as potential selections
			availableNeighbors.addAll(parentGraph.getNeighbors(neighbor));
		}
		
		iterations++;		
		return true;
	}
}
