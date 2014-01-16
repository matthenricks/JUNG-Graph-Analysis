/**
 * 
 */
package GraphCreation;

import java.io.IOException;

import edu.uci.ics.jung.graph.Graph;

/**
 * This interface is for all the classes used to generate graphs from all the data sets.
 * @author MOREPOWER
 *
 */
public interface GraphLoader {

	/**
	 * Function to load in a graph, can either import, generate, or tweak depending on execution
	 * @return The graph in question
	 * @throws IOException 
	 */
	public Graph<String, String> loadGraph() throws Error, IOException;
	// Could use Class<? extends Graph>, but then we'd have to use NewInstance and not be able to add onto existing graphs

	/**
	 * Getter for important information about the graphLoader
	 * @return a digest of that information
	 */
	public String getInformation();
}
