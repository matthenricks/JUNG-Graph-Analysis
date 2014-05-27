package JUnitTests;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Utils used in JUnit tests only
 * @author MOREPOWER
 *
 */
public class JUnitUtils {
		
	/**
	 * Attempts to remove all of the list within a specific tolerance. It's one for one, so each removal is expressed in both lists
	 * @param l1
	 * @param l2
	 * @param tolerance
	 * @return
	 */
	protected static boolean removeAllWithTolerance(List<Double> l1, List<Double> l2, double tolerance) {
		Collections.sort(l1);
		Collections.sort(l2);
		
		int i1 = 0, i2 = 0;
		while (i1 < l1.size() && i2 < l2.size()) {
			if (Math.abs(l1.get(i1) - l2.get(i2)) <= tolerance) {
				l1.remove(i1);
				l2.remove(i2);
			} else {
				if (l1.get(i1) > l2.get(i2)) {
					i2++;
				} else {
					i1++;
				}
			}
		}
		
		return true;
	}
	
	protected static void checkGraphs(Graph<String, String> sample, Graph<String, String> population) {
		// Initial assertions
		assertTrue(sample.getEdgeCount() == population.getEdgeCount());
		// The names of the vertexes may be different
		assertTrue(sample.getVertexCount() == population.getVertexCount());
		
		// Check to see if all the edges are the same (not the id's, but everything else)
		// point[x] -> point[y] -> edge type -> edge amount
		HashMap<String, HashMap<String, HashMap<EdgeType, Integer>>> popEdges = new HashMap<String, HashMap<String, HashMap<EdgeType, Integer>>>();
		// Populate this monster
		for (String edge : population.getEdges()) {
			Pair<String> xy = population.getEndpoints(edge);
			EdgeType type = population.getEdgeType(edge);
			
			if (popEdges.get(xy.getFirst()) == null)
				popEdges.put(xy.getFirst(), new HashMap<String, HashMap<EdgeType, Integer>>());
			if (popEdges.get(xy.getFirst()).get(xy.getSecond()) == null)
				popEdges.get(xy.getFirst()).put(xy.getSecond(), new HashMap<EdgeType, Integer>());
			if (popEdges.get(xy.getFirst()).get(xy.getSecond()).get(type) == null)
				popEdges.get(xy.getFirst()).get(xy.getSecond()).put(type, 0);
			// Increment the count
			popEdges.get(xy.getFirst()).get(xy.getSecond()).put(type, popEdges.get(xy.getFirst()).get(xy.getSecond()).get(type) + 1);
		}
		
		// Now check the monster against the sample
		for (String edge : sample.getEdges()) {
			Pair<String> xy = population.getEndpoints(edge);
			EdgeType type = population.getEdgeType(edge);
			
			if (popEdges.get(xy.getFirst()) == null)
				assertTrue(false);
			if (popEdges.get(xy.getFirst()).get(xy.getSecond()) == null)
				assertTrue(false);
			if (popEdges.get(xy.getFirst()).get(xy.getSecond()).get(type) == null)
				assertTrue(false);

			assertTrue(popEdges.get(xy.getFirst()).get(xy.getSecond()).get(type) > 0);
			popEdges.get(xy.getFirst()).get(xy.getSecond()).put(type, popEdges.get(xy.getFirst()).get(xy.getSecond()).get(type) - 1);
		}
	}
}
