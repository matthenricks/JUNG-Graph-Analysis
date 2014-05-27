package GraphAnalyzers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import edu.uci.ics.jung.graph.Graph;

/**
 * Abstract class for all graph measure analyzers
 * 	Graph information can be either read or analyzed. A method is included to make that decision based off the 
 *  availability of the attempted output space for the measure
 * @author MOREPOWER
 *
 */
public abstract class AnalyzerDistribution {

	/**
	 * Analyzes the graph for a specific measure. The measures are outputted to a specified filepath then returned
	 * @param graph
	 * @param filepath
	 * @return a mapping of all the verticies to their measures
	 * @throws IOException
	 */
	public abstract Map<String, Double> analyzeGraph(Graph<String, String> graph, String filepath) throws IOException;
	
	/**
	 * Reads in the measure values of a previously exported analysis
	 * @throws FileNotFoundException 
	 * @throws Error 
	 * @throws IOException 
	 */
	public abstract Map<String, Double> read(String filepath) throws FileNotFoundException, IOException, Error;
	
	public Map<String, Double> computeOrProcess(Graph<String, String> graph, String filepath) throws FileNotFoundException, IOException, Error {
		if ((new File(filepath)).exists() && (new File(filepath)).isFile())
//			try {
				return read(filepath);
//			} catch (Exception e) {
		// TODO: Could make it so it will overwrite the file, but this is unsafe
//				return analyzeGraph(graph, filepath);
//			}
		else
			return analyzeGraph(graph, filepath);
	}
	
	/**
	 * Returns the name of the type of analysis
	 */
	public abstract String getName();
	
}
