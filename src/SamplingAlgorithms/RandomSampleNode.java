package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class RandomSampleNode implements SampleMethod {
	
	double alpha;
	int seed;
	
	public RandomSampleNode(double alpha, int seed) {
		this.alpha = alpha;
		this.seed = seed;
	}
	public RandomSampleNode(double alpha) {
		this.alpha = alpha;
		seed = (new Random()).nextInt();
	}

	/**
	 * Returns a sample graph of the parent graph based off a series of BFS related to the proportion of alpha to |V|
	 * @param parentGraph - the graph being sampled from
	 * @param alpha - percentage of nodes that will correspond to steps
	 * @param seed - seed used for the random generation
	 * @return
	 */
	public Graph<String, String> sampleGraph(Graph<String, String> parentGraph) {

		if (parentGraph.getVertexCount() == 0 || parentGraph.getEdgeCount() == 0) 
			throw new Error("Parent Graph must contain vertexes and edges");

		// Set up the sampled graph frame
		Graph<String, String> sampledGraph;
		if (parentGraph.getDefaultEdgeType() == EdgeType.DIRECTED) {
			sampledGraph = new DirectedSparseGraph<String, String>();
		} else if (parentGraph.getDefaultEdgeType() == EdgeType.UNDIRECTED) {
			sampledGraph = new UndirectedSparseGraph<String, String>();
		} else {
			throw new Error("Edge Type of the parent graph is unknown");
		}
		
		// Randomly mix up the vertices in the parent
		List<String> vertices = new ArrayList<String>(parentGraph.getVertices());
		Collections.shuffle((vertices), new Random(seed));
		
		/** Add initial seed for the new graph **/
		// Create the seed of the sample from the initial node and one step of BFS (its neighbors)
		sampledGraph.addVertex(vertices.get(0));
		for(String vertex: parentGraph.getNeighbors(vertices.get(0))) {
			sampledGraph.addVertex(vertex);
			sampledGraph.addEdge(parentGraph.findEdge(vertices.get(0), vertex), vertices.get(0), vertex);
		}
		
		/** Additional BFS added to the sample **/
		for(int i = 1; i < Math.floor(alpha*parentGraph.getVertexCount()); i++) {
			// Add the next vertex and it's BFS
			sampledGraph.addVertex(vertices.get(i));
			for(String vertex: parentGraph.getNeighbors(vertices.get(i))) {
				// Add new vertex (if needed)
				if (!sampledGraph.containsVertex(vertex))
					sampledGraph.addVertex(vertex);
				String nEdge = sampledGraph.findEdge(vertex, vertices.get(i));
				if (!sampledGraph.containsEdge(nEdge))
					sampledGraph.addEdge(parentGraph.findEdge(vertex, vertices.get(i)), vertex, vertices.get(i), parentGraph.getDefaultEdgeType());
			}
		}
		
		return sampledGraph;
	}
}