package SamplingAlgorithms;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class RNDWalkMetroHastingsSampler extends RNDWalkSampler {
	
	public RNDWalkMetroHastingsSampler(double alpha, double walk_rnd, int seed, EdgeType type) {
		super(alpha, walk_rnd, seed, type);
	}
	
	public RNDWalkMetroHastingsSampler(double alpha, double walk_rnd, EdgeType type) {
		super(alpha, walk_rnd, (new Random()).nextInt(), type);
	}
	
	protected static class PercentageGroup<T> {
		double percentage;
		T data;
		public PercentageGroup(T data, double percentage) {
			this.data = data;
			this.percentage = percentage;
		}
		@Override
		public String toString() {
			return data.toString() + ", " + percentage;
		}
	}
	
	/**
	 * Takes in a group of objects with an attached percentage, then returns the selected one	
	 * @param group
	 * @return
	 */
	private PercentageGroup<String> pickPercent(LinkedList<PercentageGroup<String>> group) {
		double selection = (new Random()).nextDouble();
		double counter = 0;
		PercentageGroup<String> value = null;
		
		Iterator<PercentageGroup<String>> iter = group.iterator();
		do {
			if (iter.hasNext()) {
				value = iter.next();
				counter += value.percentage;
			} else 
				return null;
		} while (counter < selection);
			
		return value;
	}
	
	@Override
	protected String pickNextWalk(Collection<String> neighbors,
			Graph<String, String> parentGraph) {
		// Make the selection non-uniform. Look at in-nodes for neighbors and out-nodes for the current percentage
		PercentageGroup<String> selection = null;
		LinkedList<PercentageGroup<String>> perNeighbors = new LinkedList<PercentageGroup<String>>();
		double size = neighbors.size();
		for (String neighbor : neighbors) {
			perNeighbors.add(new PercentageGroup<String>(
					neighbor, 
					(1/size) * Math.min(1, size/(double)parentGraph.getPredecessorCount(neighbor))
					));
		}
		do {
			selection = pickPercent(perNeighbors);
		} while (selection == null);
		
		return selection.data;
	}
	
//	public static void main(String[] args) throws Exception {
//		UndirectedSparseGraph<String, String> dGraph = new UndirectedSparseGraph<String, String>();
//		dGraph.addVertex("A");
//		dGraph.addVertex("B");
//		dGraph.addVertex("C");
//		dGraph.addVertex("D");
//		dGraph.addVertex("E");
//		dGraph.addVertex("F");
//		dGraph.addVertex("G");
//		
//		dGraph.addEdge("e1", "A", "B");
//		dGraph.addEdge("e2", "A", "C");
//		dGraph.addEdge("e3", "A", "D");
//		dGraph.addEdge("e4", "B", "E");
//		dGraph.addEdge("e5", "B", "F");
//		dGraph.addEdge("e6", "C", "G");
//		dGraph.addEdge("e7", "D", "C");
//		dGraph.addEdge("e8", "G", "A");
//		
//		RNDWalkMetroHastingsSampler rnd = new RNDWalkMetroHastingsSampler(0.3, 0.4, EdgeType.DIRECTED);
//		System.out.println(rnd
//				.pickNextWalk(dGraph.getSuccessors("B"),dGraph));
//	}
}