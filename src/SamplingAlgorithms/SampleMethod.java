package SamplingAlgorithms;

import Utils.CSV_Builder;
import edu.uci.ics.jung.graph.Graph;

/**
 * Interface for all sampling methods
 * @author MOREPOWER
 *
 */
public interface SampleMethod {
	/**
	 * 
	 * @param parent - the parent graph that's being sampled
	 * @return a CSV_Builder detailing information pertinent to the sampling
	 */
	public CSV_Builder sampleGraph(Graph<String, String> parent);
	/**
	 * Getter to retrieve the sampled graph
	 * @return the sampled graph
	 */
	public Graph<String, String> getGraph();
}
