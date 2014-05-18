package SamplingAlgorithms;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

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