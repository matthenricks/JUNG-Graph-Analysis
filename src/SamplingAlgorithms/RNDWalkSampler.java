package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import Utils.CSV_Builder;
import Utils.CSV_Builder_Objects.CSV_Percent;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Sampler that operates off of a random walk or random sample, or a mixture of the two.
 *   Random: randomly selects an un-sampled node and sets the current node to it.
 *   Walk: randomly selects a neighbor of the current node, explores it and adds the edge. If more than 
 *   rerunMax steps occur before a node is added, a random node that hasn't been added is selected
 *   
 * This will always start with random, then choose between a walk or random based off the threshold
 * @author MOREPOWER
 *
 */
public class RNDWalkSampler implements TargetedSampleMethod {
	// Alpha is the percentage of the overall graph's vertexes it will fulfill
	// Threshold is the chance of doing a sampling method vs the other
	protected double alpha, walk_rnd;
	protected int seed;
	
	// As a SparseGraph, this can support both Directed and Undirected edges
	protected Graph<String, String> sampledGraph;
	protected String lastNode;
	
	// Count of how many random vs bfs samples were used
	protected int randomSample;
	protected int walkSample;
	
	// Tracks how many times the walk walked over itself in a row
	protected int rerunCount = 0;
	protected final int rerunMax = 200;
	
	public RNDWalkSampler(double alpha, double walk_rnd, int seed, EdgeType type) {
		this.alpha = alpha;
		this.walk_rnd = walk_rnd;
		this.seed = seed; 
		
		// Variables needed for the tracking of the growing sample
		if (type.equals(EdgeType.DIRECTED)) {
			sampledGraph = new DirectedSparseGraph<String, String>();	
		} else if (type.equals(EdgeType.UNDIRECTED)) {
			sampledGraph = new UndirectedSparseGraph<String, String>();
		} else {
			throw new Error("Unrecognized Edge Type");
		}
		
		randomSample = 0;
		walkSample = 0;
	}
	
	public RNDWalkSampler(double alpha, double walk_rnd, EdgeType type) {
		this(alpha, walk_rnd, (new Random()).nextInt(), type);
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
		/** Note, the above classes will be used for Random Sampling **/

		String current_vertex = null;
		if (lastNode == null) {
			if (sampledGraph.getVertexCount() != 0)
				throw new Error("Last node unchosen during a random walk...");			
			// Start at a random node
			if (mainVGetter.hasNext()){
				current_vertex = mainVGetter.next();
				sampledGraph.addVertex(current_vertex);
				lastNode = current_vertex;
				randomSample++;
			} else {
				throw new Error("No nodes were found to sample at beginning of Random Walk...");
			}
		}
		
		// Continue to sample if the nodes don't get to be too much
		while (continueSampling()) {

			// Select Random Sampling
			if (rndGen.nextDouble() > walk_rnd) {
				// Add another random node, first get to an open position
				while (mainVGetter.hasNext() && sampledGraph.getVertices().contains(current_vertex = mainVGetter.next()))
					continue;
				// Add the random selected node
				if (current_vertex != null){
					randomSample++;
				} else {
					if (!mainVGetter.hasNext())
						break;
				}
			} else {
				// Attempt to walk from the last node, moving forward only
				Collection<String> neighbors = new ArrayList<String>(parentGraph.getSuccessors(lastNode));
				if (neighbors.size() > 0) {
					
					// Pick a neighbor at Random
					current_vertex = pickNextWalk(neighbors, parentGraph);
					if (current_vertex == null)
						throw new Error("Impossible Error");
					
					// Attempt to add that neighbor, based off if it's already contained
					if (sampledGraph.containsVertex(current_vertex) == false) {
						// current_vertex is good! Add at bottom
						walkSample++;
					} else if (rerunCount >= rerunMax) {
						// Check to see if there are any open spots and give them equal probability, then hit them
						LinkedList<String> allNeighbors = new LinkedList<String>();
						// Add all of the possible neighbors
						for (String vertex : sampledGraph.getVertices()) {
							allNeighbors.addAll(parentGraph.getSuccessors(vertex));
						}
						// Remove all of the neighbors that are currently in the graph
						allNeighbors.removeAll(sampledGraph.getVertices());
						
						// Select the current vertex based off the total available neighbors, uniform weighting
						if (allNeighbors.size() != 0) {
							Collections.shuffle(allNeighbors);
							current_vertex = allNeighbors.get(0);
							walkSample++;
						} else {
							// All the connected nodes have been selected
							// Take a random free node
							while (mainVGetter.hasNext() && sampledGraph.containsVertex(current_vertex = mainVGetter.next()))
								continue;
							// Add the random selected node
							if (current_vertex != null){
								randomSample++;
							} else {
								if (!mainVGetter.hasNext())
									break;
							}
						}
					} else {
						rerunCount++;
						// Check to see if it's an UNDIRECTED graph and has the reversed connection
						if (parentGraph.getDefaultEdgeType() == EdgeType.UNDIRECTED) {
							if (sampledGraph.findEdge(current_vertex, lastNode) != null) {
								lastNode = current_vertex;
								continue;
							}
						}
						// Now check for the current edge
						String edge = parentGraph.findEdge(lastNode, current_vertex);
						if (edge != null)
							sampledGraph.addEdge(edge, lastNode, current_vertex);

						lastNode = current_vertex;
						continue;
					}
				} else {
					// An island was selected. Random sample please.
					while (mainVGetter.hasNext() && sampledGraph.containsVertex(current_vertex = mainVGetter.next()))
						continue;
					// Add the random selected node
					if (current_vertex != null){
						randomSample++;
					} else {
						if (!mainVGetter.hasNext())
							break;
					}
				}
			}
			// Add the selected current vertex
			sampledGraph.addVertex(current_vertex);
			String edge = parentGraph.findEdge(lastNode, current_vertex);
			if (edge != null)
				sampledGraph.addEdge(edge, lastNode, current_vertex);
			
			// Set the last node to be the added node
			lastNode = current_vertex;
			rerunCount = 0; // Note, the only place this is added, it has a continue to avoid this section
		}

		/**
		 * Return the actual alpha, then actual threshold
		 */
		double actualAlpha = (double)sampledGraph.getVertexCount() / (double)parentGraph.getVertexCount();
		if (randomSample + walkSample == 0)
			throw new Error("Random and Thresh can't be 0 at this point");
		double actualThreshold = (double)walkSample / (double)(randomSample + walkSample);
		return new CSV_Builder(new CSV_Percent(actualAlpha),
				new CSV_Builder(new CSV_Percent(actualThreshold)));		
	}
	
	protected String pickNextWalk(Collection<String> neighbors,
			Graph<String, String> parentGraph) {
		
		int rndCount = (new Random()).nextInt(neighbors.size());
		int start = 0;
		for (String node : neighbors) {
			if (start == rndCount) {
				return node;
			}
			start++;
		}
		return null;
	}

	protected boolean goodParent(Graph<String, String> parentGraph) {
		return parentGraph.getVertexCount() == 0 || parentGraph.getEdgeCount() == 0;
	}
}