package Unimplemented;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
 * UNUSED FOREST FIRE SAMPLER. KEPT FOR POTENTIAL USE PURPOSES
 * 
 * 
 * Forest Fire (FF): FF is a randomized version of BFS, where for every neighbor v 
 * of the current node, we flip a coin, with probability of success p, to decide if we explore v. 
 * FF reduces to BFS for p = 1. It is possible that this process dies out before it covers all nodes. 
 * In this case, in order to make FF comparable with other techniques, we revive the process 
 * from a random node already in the model. 
 * 
 * In order to increase the efficiency as well as make it able to deal with sparse, multi-island graphs, we
 * Further add a condition. If no new node is added for 200 steps, it will pick a random node to add to the fire
 * in the sample that is untouched. If there aren't any of these, it will randomly select an open node.
 * 
 * In reference to edges that are sampled, only edges that are explored are taken.
 * Also, note that additional surrounding fire doesn't increase the chance of a node burning.
 *   This caused issues in larger samples due to the queue expanding too greatly
 * 
 * @author iosbomb
 *
 */
public class RNDForestFireSampler implements SamplingAlgorithms.TargetedSampleMethod {
	// Alpha is the percentage of the overall graph's vertexes it will fulfill
	// Threshold is the chance of doing a sampling method vs the other
	protected double alpha, forest_rnd;
	protected int seed;
	
	// As a SparseGraph, this can support both Directed and Undirected edges
	protected Graph<String, String> sampledGraph;
	// Queue to control the adding
	protected LinkedList<String> nodeQueue;
	// Queue to store the addition of Edges, holds the predecessors
	protected LinkedList<String> nodePred;
	// Queue to control the flaming nodes. They're on FIRE!
	protected HashSet<String> fireQueue;
	
	// Count of how many random vs bfs samples were used
	protected int randomSample;
	protected int forestSample;
	
	// Tracks how many times the walk walked over itself in a row
	protected int rerunCount = 0;
	protected final int rerunMax = 20;
	
	// Probability of burning catching another tree
	protected double spreadProbability;
	
	public RNDForestFireSampler(double alpha, double forest_rnd, int seed, EdgeType type, double spreadingProbability) {
		this.alpha = alpha;
		this.forest_rnd = forest_rnd;
		this.seed = seed;
		this.spreadProbability = spreadingProbability;
		
		nodeQueue = new LinkedList<String>();
		nodePred = new LinkedList<String>();
		fireQueue = new HashSet<String>();
		
		// Variables needed for the tracking of the growing sample
		if (type.equals(EdgeType.DIRECTED)) {
			sampledGraph = new DirectedSparseGraph<String, String>();	
		} else if (type.equals(EdgeType.UNDIRECTED)) {
			sampledGraph = new UndirectedSparseGraph<String, String>();
		} else {
			throw new Error("Unrecognized Edge Type");
		}
		
		randomSample = 0;
		forestSample = 0;
	}
	
	public RNDForestFireSampler(double alpha, double forest_rnd, EdgeType type, double spreadingProbability) {
		this(alpha, forest_rnd, (new Random()).nextInt(), type, spreadingProbability);
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
		
		// Run the sampling. This will alternate between adding the nodes, one by one, and setting them up for adding
		while (continueSampling()) {
			// Check to see if the queue for the forest fire is done
			if (nodeQueue.isEmpty()) {
				// Select Random Sampling
				if (rndGen.nextDouble() > forest_rnd || rerunCount >= rerunMax) {
					if (randomSample(mainVGetter) == false)
						break; // All of the nodes have been sampled
				} else {
					// Attempt to utilize the fire, if available, or rekindle if needed
					if (fireQueue.isEmpty()) {
						if (rekindle(rndGen) == false) {
							// If there are no current nodes
							if (randomSample(mainVGetter) == false)
								break;
						}
					} else {
						forestSample(parentGraph, rndGen);
					}
					// Add to the rerun count in case this goes on forever
					rerunCount++;
				}
			} else {
				// There are some stored up nodes to do the forest fire addition on!
				// Add the node
				String newFlame = nodeQueue.pollFirst();
				fireQueue.add(newFlame);
				if (sampledGraph.addVertex(newFlame)) {
					rerunCount = 0;
				}
				
				// Add the edge
				String pred = nodePred.pollFirst();
				// Now add the edges, handled Undirected issues first
				if (sampledGraph.getDefaultEdgeType() == EdgeType.UNDIRECTED) {
					if (sampledGraph.findEdge(newFlame, pred) != null)
						continue;
				}
				String edge = parentGraph.findEdge(pred, newFlame);
				if (edge != null)
					sampledGraph.addEdge(edge, pred, newFlame);
			}
		}
		/**
		 * Return the actual alpha, then actual threshold
		 */
		double actualAlpha = (double)sampledGraph.getVertexCount() / (double)parentGraph.getVertexCount();
		if (randomSample + forestSample == 0)
			throw new Error("Random and Thresh can't be 0 at this point");
		double actualThreshold = (double)forestSample / (double)(randomSample + forestSample);
		return new CSV_Builder(new CSV_Percent(actualAlpha),
				new CSV_Builder(new CSV_Percent(actualThreshold)));		
	}
	
	protected void forestSample(Graph<String, String> parentGraph, Random rndGen) {
		// Queue the flames for processing with their predecessor!
		for (String node : fireQueue) {
			for (String neighbor : parentGraph.getSuccessors(node)) {
				// Connect to each neighbor with the given probability
				if (rndGen.nextDouble() <= spreadProbability) {
					nodeQueue.add(neighbor);
					nodePred.add(node);
				}
			}
		}
		fireQueue.clear();		
		forestSample++;
	}
	
	/**
	 * Attempt to spur more fire by selecting a random already selected node and jump-starting the fire!
	 */
	protected boolean rekindle(Random rndGen) {
		ArrayList<String> nodes = new ArrayList<String>(sampledGraph.getVertices());
		if (nodes.size() == 0)
			return false;
		
		Collections.shuffle(nodes, rndGen);
		String node = nodes.get(0);
		fireQueue.add(node);
		
		forestSample++;
		return true;
	}
	
	protected boolean randomSample(Iterator<String> mainVGetter) {
		String current_vertex = null;
		// Add another random node, first get to an open position
		while (mainVGetter.hasNext() && sampledGraph.getVertices().contains(current_vertex = mainVGetter.next()))
			continue;
		// Add the random selected node
		if (current_vertex != null){
			sampledGraph.addVertex(current_vertex);
			rerunCount = 0;
			// TODO: Do we add edges here? I don't think so
			
			// Add the random node as a source of FIRE!
			fireQueue.add(current_vertex);
			randomSample++;
		} else {
			if (!mainVGetter.hasNext())
				return false;
		}
		return true;		
	}

	protected boolean goodParent(Graph<String, String> parentGraph) {
		return parentGraph.getVertexCount() == 0 || parentGraph.getEdgeCount() == 0;
	}
}