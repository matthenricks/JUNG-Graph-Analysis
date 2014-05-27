package GraphCreation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import Utils.HardCode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * The purpose of this class is to be able import in the Twitter data (friends.csv) and convert it to a JUNG graph.
 * The graph is able to be specified as Directed or Undirected
 * @author MOREPOWER
 *
 */
public class FriendsTwitterDataImporter extends ImportedGraph {

	final String myHeader = "\"user_id\",\"user_screen_name\",\"follower_id\",\"follower_screen_name\"";
	
	private Graph<String, String> graph;
	/**
	 * Main constructor for the class
	 * @param path - The location of the csv file being read
	 * @param graphType - The graph in which the data will be imported into
	 */
	public FriendsTwitterDataImporter(String path, Graph<String, String> graphType) {
		super(path);
		graph = graphType;
	}
	
	/**
	 * Main constructor for the class
	 * @param path - The location of the csv file being read
	 * @param graphType - The graph in which the data will be imported into
	 */
	public FriendsTwitterDataImporter(String path, EdgeType graphType) {
		super(path);
		
		if (graphType == EdgeType.DIRECTED) {
			graph = new DirectedSparseGraph<String, String>();
		} else if (graphType == EdgeType.UNDIRECTED) {
			graph = new UndirectedSparseGraph<String, String>();
		} else {
			throw new Error("Graph type: " + graphType.name() + " is not handled");
		}
	}
	
	/**
	 * Function to load the graph from a twitter file. Note, this will add edges/vertexes if not already there in a predisposed graph
	 */
	@Override
	public Graph<String, String> loadGraph() throws IOException, Error {
		BufferedReader br = new BufferedReader(new FileReader(this.path));
		String header = br.readLine();
		if (header.equals(myHeader) == false) {
			br.close();
			throw new Error("Header is not correct. It's \"" + header + "\" Instead of " + myHeader);
		}
		String data;
		while((data = br.readLine()) != null) {
			// Import in "user, userID, follower, followerID"
			data = data.replaceAll("\"", "");
			String[] items = HardCode.separateReg.split(data);
			if (items.length != 4) {
				br.close();
				throw new Error("Data Split was incorrectly formatted: " + data);
			}
			// Clean the pulled out values
			String source = items[0].trim();
			String sourceID = items[1].trim();
			String dest = items[2].trim();
			String destID = items[3].trim();
			
			// Add the edges (and vertexes). Don't cover false case, duplication of edges can cause that. Undirected Graphs may be a problem
			graph.addEdge(sourceID + "-" + destID, source, dest);
		}
		br.close();
		return graph;
	}
	
	@Override
	public String getInformation() {
		return "Twitter Graph located at: " + this.path + "\n";
	}
}