package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import Utils.CSV_Builder;
import Utils.CSV_Builder_Objects.CSV_Percent;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

// TODO: Make this extend the RandomSampleNode
public class RandomBFSSample implements SampleMethod {

	// Alpha is the percentage of the overall graph's vertexes it will fulfill
	// Threshold is the chance of doing a sampling method vs the other
	protected double alpha, threshold;
	protected int seed;
	
	// As a SparseGraph, this can support both Directed and Undirected edges
	protected Graph<String, String> sampledGraph;
	protected TreeSet<String> availableNeighbors;
	protected HashSet<String> addedCoreVertexes;
	
	protected int randomSample;
	protected int thresholdSample;
	
	public RandomBFSSample(double alpha, double threshold, int seed) {
		this.alpha = alpha;
		this.threshold = threshold;
		this.seed = seed; 
		
		// Variables needed for the tracking of the growing sample
		sampledGraph = new SparseGraph<String, String>();
		availableNeighbors = new TreeSet<String>();
		addedCoreVertexes = new HashSet<String>();
		 randomSample = 0;
		 thresholdSample = 0;
	}
	public RandomBFSSample(double alpha, double threshold) {
		this(alpha, threshold, (new Random()).nextInt());
	}
	
	public void changeAlpha(double alpha) {
		this.alpha = alpha;
	}
	
	public Graph<String, String> getGraph() {
		return sampledGraph;
	}
	
	protected int sampleMinVertexes;
	protected boolean continueSampling() {
		return (sampledGraph.getVertexCount() < sampleMinVertexes);
	}
	
	public CSV_Builder sampleGraph(Graph<String, String> parentGraph) {
		if (goodParent(parentGraph))
			throw new Error("Parent Graph must contain vertexes and edges");
		
		// Set the sample minimum to begin the loop
		sampleMinVertexes = (int)Math.ceil((double)parentGraph.getVertexCount() * alpha);
		
		Random rndGen = new Random(seed);
		
		// A list to hold the set of Strings that is possibly selected
		// ArrayList probably has one of the quickest shuffles and I'm unsure about the Collection's randomness
		ArrayList<String> vertices = new ArrayList<String>(parentGraph.getVertices());
		Collections.shuffle(vertices, rndGen);
		// Iterator to easily run through the values
		Iterator<String> mainVGetter = vertices.iterator();
			
		String current_vertex;
		while (continueSampling()) {
			// Select the node to add
			if (rndGen.nextDouble() < threshold) {
				while (addedCoreVertexes.contains(current_vertex = mainVGetter.next()))
					continue;
				randomSample++;
			} else {
				current_vertex = selectRandomNeighbor();
				if (current_vertex == null) {
					// Just use the random method instead
					while (addedCoreVertexes.contains(current_vertex = mainVGetter.next()))
						continue;
					randomSample++;
				} else {
					thresholdSample++;
				}
			}
			// Add the area around the selected node, itself and neighbors
			addNode(current_vertex, parentGraph);
		}
	
		/**
		 * Return the actual alpha, then actual threshold
		 */
		double actualAlpha = (double)sampledGraph.getVertexCount() / (double)parentGraph.getVertexCount();
		if (randomSample + thresholdSample == 0)
			throw new Error("Random and Thresh can't be 0 at this point");
		double actualThreshold = (double)thresholdSample / (double)(randomSample + thresholdSample);
		return new CSV_Builder(new CSV_Percent(actualAlpha),
				new CSV_Builder(new CSV_Percent(actualThreshold)));		
	}
	
	protected boolean goodParent(Graph<String, String> parentGraph) {
		return parentGraph.getVertexCount() == 0 || parentGraph.getEdgeCount() == 0;
	}
	
	protected boolean addNode(String current_vertex, Graph<String, String> parentGraph) {
		// Add the area around the selected node, itself and neighbors
		sampledGraph.addVertex(current_vertex);		
		// Shuffle the neighbors so each has an even probability of being randomly selected
		// This obtains all of the connecting edges to this node
		ArrayList<String> currNeighbors = new ArrayList<String>(parentGraph.getNeighbors(current_vertex));
		Collections.shuffle(currNeighbors, new Random(seed));
		// Add all the neighbors that are first degree
		availableNeighbors.addAll(currNeighbors);
		// Add all the neighbors
		for(int counter = 0; counter < currNeighbors.size(); counter++) {
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
		
		return true;
	}
	
	protected String selectRandomNeighbor() {
		// Randomize all the possible selections. This is potentially, grossly inefficient (depending on the remove)
		ArrayList<String> neighbors = new ArrayList<String>(availableNeighbors);
		Collections.shuffle(neighbors, new Random(seed));
		Iterator<String> neighborIterator = neighbors.iterator();
		String current_vertex = null;
		while (neighborIterator.hasNext()) {
			if (!addedCoreVertexes.contains(current_vertex = neighborIterator.next())) {
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
