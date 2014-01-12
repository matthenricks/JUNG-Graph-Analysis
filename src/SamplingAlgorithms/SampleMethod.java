package SamplingAlgorithms;

import edu.uci.ics.jung.graph.Graph;

/**
 * Interface for all sampling methods
 * @author MOREPOWER
 *
 */
public interface SampleMethod {

	public Graph<String, String> sampleGraph(Graph<String, String> parent);
}
