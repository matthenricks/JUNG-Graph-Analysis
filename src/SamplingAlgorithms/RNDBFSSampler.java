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
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Sampling method that can mix properties of a random sampling and a BFS. This is done by a 
 * step-wise procedure where a random number is compared to a threshold for one or the other.
 *   Random Sampling is done by selecting a non-sampled node from the population and adding it and it's neighbors
 *   BFS is done by selecting all possible out-links of all nodes in the sample; pulling a random sample if an island is discovered.
 * Directionality is taken into account: only out-links will be added for RND and BFS
 * The function adds nodes in a step-wise queue procedure, so not all neighbors or queued nodes are guaranteed to be added if the sample fills its proportion before the step is over.
 * @author MOREPOWER
 *
 */
public class RNDBFSSampler implements TargetedSampleMethod {
	// Alpha is the percentage of the overall graph's vertexes it will fulfill
	// Threshold is the chance of doing a sampling method vs the other
	protected double alpha, bfs_rnd;
	protected int seed;
	
	// As a SparseGraph, this can support both Directed and Undirected edges
	protected Graph<String, String> sampledGraph;
	protected LinkedList<String> addingQueue;
	
	// Count of how many random vs bfs samples were used
	protected int randomSample;
	protected int bfsSample;
	
	public RNDBFSSampler(double alpha, double bfs_rnd, int seed, EdgeType type) {
		this.alpha = alpha;
		this.bfs_rnd = bfs_rnd;
		this.seed = seed; 
		
		// Variables needed for the tracking of the growing sample
		if (type.equals(EdgeType.DIRECTED)) {
			sampledGraph = new DirectedSparseGraph<String, String>();	
		} else if (type.equals(EdgeType.UNDIRECTED)) {
			sampledGraph = new UndirectedSparseGraph<String, String>();
		} else {
			throw new Error("Unrecognized Edge Type");
		}
		
		addingQueue = new LinkedList<String>();
		randomSample = 0;
		bfsSample = 0;
	}
	
	public RNDBFSSampler(double alpha, double bfs_rnd, EdgeType type) {
		this(alpha, bfs_rnd, (new Random()).nextInt(), type);
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
		
		String current_vertex;
		while (continueSampling()) {
			current_vertex = null;
			
			// After a step is complete, add another
			if (addingQueue.isEmpty()) {
				
				// Select Random Sampling
				if (rndGen.nextDouble() > bfs_rnd) {
					// Random Sampling Setup
					while (mainVGetter.hasNext() && (sampledGraph.getVertices().contains(current_vertex = mainVGetter.next())
							|| addingQueue.contains(current_vertex)))
						continue;
					if (current_vertex != null){
						setupRND(current_vertex, parentGraph);
						randomSample++;
					} else {
						if (!mainVGetter.hasNext())
							break;
					}
				} else {					
					if (setupBFS(parentGraph) == false) {
						// Random Sampling Setup
						while (mainVGetter.hasNext() && (sampledGraph.getVertices().contains(current_vertex = mainVGetter.next())
								|| addingQueue.contains(current_vertex)))
							continue;
						if (current_vertex != null){
							setupRND(current_vertex, parentGraph);
							randomSample++;
						} else {
							if (!mainVGetter.hasNext())
								break;
						}
					} else {
						bfsSample++;						
					}
				}
			} else {
				// Pull nodes off of the queue
				addNode(addingQueue.pollFirst(), parentGraph);
			}
		}
	
		/**
		 * Return the actual alpha, then actual threshold
		 */
		double actualAlpha = (double)sampledGraph.getVertexCount() / (double)parentGraph.getVertexCount();
		if (randomSample + bfsSample == 0)
			throw new Error("Random and Thresh can't be 0 at this point");
		double actualThreshold = (double)bfsSample / (double)(randomSample + bfsSample);
		return new CSV_Builder(new CSV_Percent(actualAlpha),
				new CSV_Builder(new CSV_Percent(actualThreshold)));		
	}
	
	protected boolean goodParent(Graph<String, String> parentGraph) {
		return parentGraph.getVertexCount() == 0 || parentGraph.getEdgeCount() == 0;
	}
		
	/**
	 * Try to make all the nodes expand out one step
	 * @param parentGraph
	 * @return
	 */
	protected boolean setupBFS(Graph<String, String> parentGraph) {
		if (!addingQueue.isEmpty())
			throw new Error("Adding Queue must be empty before additional steps are taken");
		
		// Add all of the possible neighbors
		for (String vertex : sampledGraph.getVertices()) {
			addingQueue.addAll(parentGraph.getSuccessors(vertex));
		}
		// Remove all of the neighbors that are currently in the graph
		addingQueue.removeAll(sampledGraph.getVertices());
		
		return (addingQueue.size() > 0);
	}
	
	/**
	 * This queues the random step to be added on the current vertex
	 * @param current_vertex
	 * @param parentGraph
	 * @return
	 */
	protected boolean setupRND(String current_vertex, Graph<String, String> parentGraph) {
		// Add the selected vertex
		addingQueue.add(current_vertex);
		
		// Add the vertexes neighbors and make their neighbors neighbors new possibilities
		for (String neighbor : parentGraph.getSuccessors(current_vertex)) {
			if (!(sampledGraph.getVertices().contains(neighbor) || addingQueue.contains(neighbor))) {
				addingQueue.add(neighbor);
			}
		}
		
		return true;
	}
	
	/**
	 * Adds the node to the sampleGraph
	 * Just adds the current_vertex and places all the neighbors in the neighbors possible
	 * @param current_vertex
	 * @param parentGraph
	 * @return
	 */
	protected boolean addNode(String current_vertex, Graph<String, String> parentGraph) {
		// Check to see if it was already added
		if (sampledGraph.containsVertex(current_vertex))
			return false;
		
		// Add the selected vertex
		sampledGraph.addVertex(current_vertex);
		addAllEdges(current_vertex, parentGraph);
	
		return true;
	}
	
	protected void addAllEdges(String current_vertex, Graph<String, String> parentGraph) {
		Collection<String> allNodes = sampledGraph.getVertices();
		for (String vert : allNodes) {
			Collection<String> edges = parentGraph.findEdgeSet(vert, current_vertex);
			edges.addAll(parentGraph.findEdgeSet(current_vertex, vert));
			for (String edge : edges) {
				if (!sampledGraph.containsEdge(edge)) {
					Pair<String> endPoints = parentGraph.getEndpoints(edge);
					// Check to see if the undirected counterpart is already added
					String revEdge = sampledGraph.findEdge(endPoints.getSecond(), endPoints.getFirst());
					if (revEdge != null && EdgeType.UNDIRECTED == sampledGraph.getEdgeType(revEdge)) {
						System.out.println("Duplicate edge...");
					} else {
						sampledGraph.addEdge(edge, endPoints.getFirst(), endPoints.getSecond(), parentGraph.getEdgeType(edge));
					}
				}
			}
		}
	}
}