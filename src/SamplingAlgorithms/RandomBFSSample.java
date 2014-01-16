package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class RandomBFSSample implements SampleMethod {

	double alpha, threshold;
	int seed;
	
	public RandomBFSSample(double alpha, double threshold, int seed) {
		this.alpha = alpha;
		this.threshold = threshold;
		this.seed = seed;
	}
	public RandomBFSSample(double alpha, double threshold) {
		this.alpha = alpha;
		this.threshold = threshold;
		seed = (new Random()).nextInt();
	}
	
	// TODO: this had a bias towards selecting more influential nodes because they have a higher
	// chance of being selected earlier, which puts them in the pool for the BFS, biasing the results
	// This also helps make the samples easily huge
	// A way to reduce this is to make it do all random sampling, then make it do all BFS afterwards.
	// This said, doing a BFS afterwards will still generate bias towards the end being more influential.
	// By influential, I mean, having many nodes pointing to it, not necessarily outwards though
	public Graph<String, String> sampleGraph(Graph<String, String> parentGraph) {
		
		if (parentGraph.getVertexCount() == 0 || parentGraph.getEdgeCount() == 0) 
			throw new Error("Parent Graph must contain vertexes and edges");

		// Set up the sampled graph frame
		Graph<String, String> sampledBFSGraph;
		if (parentGraph.getDefaultEdgeType() == EdgeType.DIRECTED) {
			sampledBFSGraph = new DirectedSparseGraph<String, String>();
		} else if (parentGraph.getDefaultEdgeType() == EdgeType.UNDIRECTED) {
			sampledBFSGraph = new UndirectedSparseGraph<String, String>();
		} else {
			throw new Error("Edge Type of the parent graph is unknown");
		}

		int totalIterations = (int)Math.ceil(alpha*parentGraph.getVertexCount());
		Random rand = new Random(seed);
		
		// A list to hold the set of Strings that is possibly selected
		// ArrayList probably has one of the quickest shuffles and I'm unsure about the Collection's randomness
		ArrayList<String> vertices = new ArrayList<String>(parentGraph.getVertices());
		Collections.shuffle(vertices);
		// Iterator to easily run through the values
		Iterator<String> mainVGetter = vertices.iterator();
		
		// Keeps track of the neighbors that have been added
		ArrayList<String> neighbors = new ArrayList<String>();
		
		// Keeps track of the already processed nodes
		HashSet<String> processed = new HashSet<String>(totalIterations);
		
		int BFS_lost = 0;
		String current_vertex;
		for(int i = 0; i < totalIterations; i++) { 
			
			// Select the node to add
			if (rand.nextDouble() < threshold) {
				while (processed.contains(current_vertex = mainVGetter.next()))
					continue;
			} else {
				current_vertex = selectRandomNeighbor(neighbors, processed);
				if (current_vertex == null) {
					BFS_lost++;
					continue;
				}
			}
			// Add the area around the selected node, itself and neighbors
			sampledBFSGraph.addVertex(current_vertex);
			processed.add(current_vertex);
			for(String vertex: parentGraph.getNeighbors(current_vertex)) {
				// Add new vertex (if needed)
				if (!sampledBFSGraph.containsVertex(vertex)) {
					sampledBFSGraph.addVertex(vertex);
					neighbors.add(vertex);
				}
				String nEdge = sampledBFSGraph.findEdge(vertex, current_vertex);
				if (!sampledBFSGraph.containsEdge(nEdge))
					sampledBFSGraph.addEdge(parentGraph.findEdge(vertex, current_vertex), vertex, current_vertex, parentGraph.getDefaultEdgeType());
			}
		}
		
		// Now add any missed BFS steps due to a lack of neighbors
		for (int i = 0; i < BFS_lost; i++) {
			current_vertex = selectRandomNeighbor(neighbors, processed);
			if (current_vertex == null) {
				// Resort to doing the random sampling method
				// TODO: @Shankar, is this a good method?
				while (processed.contains(current_vertex = mainVGetter.next()))
					continue;
			}
			
			// Add the area around the selected node, itself and neighbors
			sampledBFSGraph.addVertex(current_vertex);
			processed.add(current_vertex);
			for(String vertex: parentGraph.getNeighbors(current_vertex)) {
				// Add new vertex (if needed)
				if (!sampledBFSGraph.containsVertex(vertex)) {
					sampledBFSGraph.addVertex(vertex);
					neighbors.add(vertex);
				}
				String nEdge = sampledBFSGraph.findEdge(vertex, current_vertex);
				if (!sampledBFSGraph.containsEdge(nEdge))
					sampledBFSGraph.addEdge(parentGraph.findEdge(vertex, current_vertex), vertex, current_vertex, parentGraph.getDefaultEdgeType());
			}
		}
		
		return sampledBFSGraph;
	}
		
	private String selectRandomNeighbor(List<String> neighbors, Collection<String> processed) {
		// Randomize all the possible selections. This is potentially, grossly inefficient (depending on the remove)
		Collections.shuffle(neighbors);
		Iterator<String> neighborIterator = neighbors.iterator();
		String current_vertex = null;
		while (neighborIterator.hasNext()) {
			if (!processed.contains(current_vertex = neighborIterator.next())) {
				neighborIterator.remove();
				break;
			} else {
				neighborIterator.remove();
				current_vertex = null;
				continue;
			}
		}
		return current_vertex;
	}
}
