package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

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
	
	protected Graph<String, String> getGraphFromInstance(Graph<String, String> parentGraph) {
		if (parentGraph.getVertexCount() == 0 || parentGraph.getEdgeCount() == 0) 
			throw new Error("Parent Graph must contain vertexes and edges");

		if (parentGraph.getDefaultEdgeType() == EdgeType.DIRECTED) {
			return new DirectedSparseGraph<String, String>();
		} else if (parentGraph.getDefaultEdgeType() == EdgeType.UNDIRECTED) {
			return new UndirectedSparseGraph<String, String>();
		} else {
			throw new Error("Edge Type of the parent graph is unknown");
		}
	}

	/**
	 * Returns a sample graph of the parent graph based off a series of BFS related to the proportion of alpha to |V|
	 * @param parentGraph - the graph being sampled from
	 * @param alpha - percentage of nodes that will correspond to steps
	 * @param seed - seed used for the random generation
	 * @return
	 */
	public Graph<String, String> sampleGraph(Graph<String, String> parentGraph) {

		// Set up the sampled graph frame
		Graph<String, String> sampledGraph = getGraphFromInstance(parentGraph);
		
		// Randomly mix up the vertices in the parent
		List<String> vertices = new ArrayList<String>(parentGraph.getVertices());
		Collections.shuffle((vertices), new Random(seed));
		
		/** Additional BFS added to the sample **/
		for(int i = 0; i < Math.ceil(alpha*parentGraph.getVertexCount()); i++) {
			// Add the next vertex and it's BFS
			sampledGraph.addVertex(vertices.get(i));
			for(String edge: parentGraph.getIncidentEdges(vertices.get(i))) {
				if (!sampledGraph.containsEdge(edge)) {
					// Add the new vertex and edge to the sample graph
					Pair<String> edges = parentGraph.getEndpoints(edge);
					if (edges.getFirst() == vertices.get(i)) {
						if (!sampledGraph.containsVertex(edges.getSecond()))
							sampledGraph.addVertex(edges.getSecond());
						sampledGraph.addEdge(edge, vertices.get(i), edges.getSecond());
					} else {
						if (!sampledGraph.containsVertex(edges.getFirst()))
							sampledGraph.addVertex(edges.getFirst());
						sampledGraph.addEdge(edge, edges.getFirst(), vertices.get(i));
					}
				}
			}
		}
		
		return sampledGraph;
	}
}