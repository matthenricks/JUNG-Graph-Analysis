package GraphAnalyzers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import edu.uci.ics.jung.graph.Graph;

/**
 * General interface for an analyzer distribution
 *   Should separate into 3 different functions: calculate, export, and import
 *   Or, should remove and just make a series of static functions in a class
 * @author MOREPOWER
 *
 */
public interface AnalyzerDistribution {

	/**
	 * Returns a vertex or edge-based distribution of double values it calculated and exports the analysis
	 * @throws IOException 
	 */
	public Map<String, Double> analyzeGraph(Graph<String, String> graph, String filepath) throws IOException;
	
	/**
	 * Reads in the analysis of a previously exported version
	 * @throws FileNotFoundException 
	 * @throws Error 
	 * @throws IOException 
	 */
	public Map<String, Double> read(String filepath) throws FileNotFoundException, IOException, Error;
	
	/**
	 * Returns identifying information for the method
	 */
	public String getName();
	
}
