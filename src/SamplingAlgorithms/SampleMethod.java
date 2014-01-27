package SamplingAlgorithms;

import Utils.CSV_Builder;
import edu.uci.ics.jung.graph.Graph;

/**
 * Interface for all sampling methods
 * @author MOREPOWER
 *
 */
public interface SampleMethod {

	public CSV_Builder sampleGraph(Graph<String, String> parent);
	public Graph<String, String> getGraph();
}
