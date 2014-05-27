package SamplingAlgorithms;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Sampling method that can mix properties of a random sampling and a BFS. This is done by a 
 * step-wise procedure where a random number is compared to a threshold for one or the other.
 *   Random Sampling is done by selecting a non-sampled node from the population and adding it and it's neighbors
 *   BFS is done by selecting all possible out-links of ONE node in the sample randomly. The neighbors and itself are then added
 * Directionality is taken into account: only out-links will be added for RND and BFS
 * The function adds nodes in a step-wise queue procedure, so not all neighbors or queued nodes are guaranteed to be added if the sample fills its proportion before the step is over.
 * @author MOREPOWER
 *
 */
public class RNDBFSSingleSampler extends RNDBFSSampler {
	
	public RNDBFSSingleSampler(double alpha, double bfs_rnd, int seed, EdgeType type) {
		super(alpha, bfs_rnd, seed, type);
	}
	
	public RNDBFSSingleSampler(double alpha, double bfs_rnd, EdgeType type) {
		super(alpha, bfs_rnd, type);
	}
	
	/**
	 * Only extends the BFS one connected node at a time
	 * @param parentGraph
	 * @return
	 */
	@Override
	protected boolean setupBFS(Graph<String, String> parentGraph) {
		if (!addingQueue.isEmpty())
			throw new Error("Adding Queue must be empty before additional steps are taken");
		
		Collection<String> availableVerts = new HashSet<String>(sampledGraph.getVertices());
		Iterator<String> sampleVerts = availableVerts.iterator();
		
		// Check every existing vertex in the sample for neighbors and add any that aren't added.
		HashSet<String> neighbors = new HashSet<String>();
		while (sampleVerts.hasNext() && neighbors.isEmpty()) {
			neighbors.addAll(parentGraph.getSuccessors(sampleVerts.next()));
			neighbors.removeAll(sampledGraph.getVertices());
		}
		
		if (neighbors.isEmpty())
			return false;
		else {
			addingQueue.addAll(neighbors);
			return true;
		}
	}
}