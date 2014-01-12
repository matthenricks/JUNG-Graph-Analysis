package GraphCreation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * A class that allows for importing and exporting of graphs in the same style as the normal loaders
 * @author MOREPOWER
 *
 */
public class BasicGraph implements GraphLoader {

	String myPath;
	public BasicGraph(String path) {
		myPath = path;
	}
	
	private static Pattern separateReg = Pattern.compile("\\,", Pattern.DOTALL);
	
	public Graph<String, String> loadGraph() throws Error, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(myPath));
		String data = reader.readLine();

		Graph<String, String> graph;
		if (data.equals("DIRECTED")) {
			graph = new DirectedSparseGraph<String, String>();
		} else if (data.equals("UNDIRECTED")) {
			graph = new UndirectedSparseGraph<String, String>();			
		} else {
			reader.close();
			throw new Error("Graph doesn't have a preferred EdgeType. This case is unhandled");
		}
		
		int edgeCount = 1;
		// TODO: This needs to be modified to be able to read very long lines...
		while ((data = reader.readLine()) != null) {
			String[] splits = separateReg.split(data);
			if (splits.length < 1)  {
				reader.close();
				throw new Error("There should always at least be a split for each vertex");
			}
			String vertex = splits[0].trim();
			graph.addVertex(vertex);
			for (int i = 1; i < splits.length; i++) {
				graph.addEdge("e" + edgeCount++, vertex, splits[i].trim());
			}
		}
		
		reader.close();
		
		return graph;
	}
	
	/**
	 * Exports the data in the graph to the path
	 *    -Edge Type
	 *    -Pajek Formatted List
	 * @param graph
	 * @param location
	 * @throws IOException 
	 */
	public static void exportGraph(Graph<String, String> graph, String path) throws IOException {
		BufferedWriter writer = Utils.FileSystem.createFile(path);
		if (graph.getDefaultEdgeType() == EdgeType.DIRECTED)
			writer.write("DIRECTED\n"); 
		else
			writer.write("UNDIRECTED\n");
		
		for (String vertex : graph.getVertices()) {
			writer.write(vertex + ",");
			for (String conn : graph.getSuccessors(vertex)) {
				writer.write(conn + ",");
			}
			writer.write("\n");
		}
	}
	
}
