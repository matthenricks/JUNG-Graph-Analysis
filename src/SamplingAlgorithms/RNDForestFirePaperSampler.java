package SamplingAlgorithms;

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
 * Forest-fire sampling. Forest-fire sampling is considered a compromised method between 
 * snowball sampling and random sampling (Leskovec, Kleinberg, and Faloutsos 2006). 
 * It begins with randomly choosing a member i (i.e., ran- domly starting a fire at i), followed by 
 * generating a random number r that is geometrically distributed with mean pf/{l - pf), 
 * where (I -pf) is the parameter in the geometric distribution. 
 * 
 * Here, r is the number of burning successes before the first burning failure, supported on the set {0,1,2,3,...},
 * and pf is called the "forward-burning probability." If the fire dies (r = 0), we add the new member i 
 * to the sample and randomly select another member in the network (i.e., randomly start another fire). 
 * Otherwise, we select member i's up to r out-links to members that were not yet visited, 
 * denoted as j,, J2, ..., jr. and add these members into the sample (i.e., the fire is forward-burned 
 * to these members). We then apply the forest-fire sampling recursively to each of j I, J2,..., 
 * jr until the number of members burned in the sample reaches n, while excluding duplicated members from 
 * the sample (i.e., members cannot be "burned" for twice).
 * 
 * In terms of a directed graph, each node has a forward burning and a backwards burning probability. the
 * backwards burning probability has a mean equal to r(pf/(1-pf))
 * 
 * @author iosbomb
 *
 */
public class RNDForestFirePaperSampler implements TargetedSampleMethod {
	// Alpha is the percentage of the overall graph's vertexes it will fulfill
	// Threshold is the chance of doing a sampling method vs the other
	protected double alpha, forest_rnd;
	protected int seed;
	
	// Variables to store whether the algorithm is forward or backwards burning
	protected double pFB, pBB;
	
	/**
	 * Returns a number of failures before the first success based off a geometric distribution. 
	 * @param probability - rate of failure
	 * @param max - the maximum number that matters in this calculation (to avoid needless calculation)
	 * @param rndGen
	 * @return
	 */
	public int getNodesAllowed(double probability, int max, Random rndGen) {
		int retVal = 0;
		while (retVal <= max && rndGen.nextDouble() <= probability)
			retVal++;
		
		return retVal;
	}

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
	
	public RNDForestFirePaperSampler(double alpha, double forest_rnd, int seed, EdgeType type, double forwardProb, double backwardMult) {
		this.alpha = alpha;
		this.forest_rnd = forest_rnd;
		this.seed = seed;
		
		this.pFB = forwardProb;
		this.pBB = backwardMult*forwardProb;
		
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
	
	public RNDForestFirePaperSampler(double alpha, double forest_rnd, EdgeType type, double forwardBurning, double backwardsBurningMult) {
		this(alpha, forest_rnd, (new Random()).nextInt(), type, forwardBurning, backwardsBurningMult);
	}
	
	public RNDForestFirePaperSampler(double alpha, double forest_rnd, EdgeType type, double forwardBurning) {
		this(alpha, forest_rnd, (new Random()).nextInt(), type, forwardBurning, 0);
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
		/** Note, the above iterator will be used for Random Sampling **/
		
		// Run the sampling. This will alternate between adding the nodes, one by one, and setting them up for adding
		while (continueSampling()) {
			// Check to see if the queue for the forest fire is done
			if (nodeQueue.isEmpty()) {
				// Select Random Sampling
				if (rndGen.nextDouble() > forest_rnd) {
					if (randomSample(mainVGetter) == false)
						break; // All of the nodes have been sampled
				} else {
					// Attempt to rekindle the fire, if available, or rekindle if needed
					if (fireQueue.isEmpty()) {
						if (randomSample(mainVGetter) == false)
								break;
					} else {
						forestSample(parentGraph, rndGen);
					}
				}
			} else {
				// This is the function that represents what happens when the flame spreads to nodes
				
				// Add the node to the sample (if not already there)
				// The tree is on fire if new
				String newFlame = nodeQueue.pollFirst();
				if (sampledGraph.addVertex(newFlame))
					fireQueue.add(newFlame);
				
				// Add the edge and/or back-vertices
				String pred = nodePred.pollFirst();
				if (sampledGraph.addVertex(pred))
					fireQueue.add(pred);
				// Now add the edges, handle Undirected issues first
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
			
			ArrayList<String> succ = new ArrayList<String>(parentGraph.getSuccessors(node));
			int nodesPossible = getNodesAllowed(pFB, succ.size(), rndGen);
			
			Collections.shuffle(succ);
			int i = 0;
			for (String neighbor : succ) {
				// Check to see if you should add neighbors
				if (i >= nodesPossible)
					break;
				
				// Connect to the neighbor if not
				nodeQueue.add(neighbor);
				nodePred.add(node);
			}
			
			// Now connect to the predecessors
			succ.clear();
			
			if (parentGraph.getDefaultEdgeType() == EdgeType.DIRECTED) {
				succ = new ArrayList<String>(parentGraph.getPredecessors(node));
				nodesPossible = getNodesAllowed(pBB, succ.size(), rndGen);
				
				Collections.shuffle(succ);
				i = 0;
				
				for (String neighbor : succ) {
					// Check to see if you should add neighbors
					if (i >= nodesPossible)
						break;
					
					// Connect to the neighbor if not
					nodeQueue.add(node);
					nodePred.add(neighbor);
				}
			}
		}
		fireQueue.clear();		
		forestSample++;
	}
	
	protected boolean randomSample(Iterator<String> mainVGetter) {
		String current_vertex = null;
		// Add another random node, first get to an open position
		while (mainVGetter.hasNext() && sampledGraph.getVertices().contains(current_vertex = mainVGetter.next()))
			continue;
		// Add the random selected node
		if (current_vertex != null){
			sampledGraph.addVertex(current_vertex);
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