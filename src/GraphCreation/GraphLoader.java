/**
 * 
 */
package GraphCreation;

import java.io.IOException;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.graph.Graph;

/**
 * This interface is for all the classes used to generate graphs from all the data sets.
 * @author MOREPOWER
 *
 */
public abstract class GraphLoader {

	int vCount = 0;
	int eCount = 0;
	
	// Factory's to help control the naming convention and creation of vertexes/edges
	Factory<String> vertexFactory = new Factory<String>() {			
		@Override
		public String create() {
			vCount++;
			return "v" + Integer.toString(vCount); 
		}
		
	}; 
	Factory<String> edgeFactory = new Factory<String>() {
		@Override
		public String create() {
			eCount++;
			return "e" + Integer.toString(eCount);
		}		
	};
	
	/**
	 * Function to load in a graph, can either import, generate, or tweak depending on execution
	 * @return The graph in question
	 * @throws IOException 
	 */
	public abstract Graph<String, String> loadGraph() throws Error, IOException;
	// Could use Class<? extends Graph>, but then we'd have to use NewInstance and not be able to add onto existing graphs

	/**
	 * Getter for important information about the graphLoader
	 * @return a digest of that information
	 */
	public abstract String getInformation();
}
