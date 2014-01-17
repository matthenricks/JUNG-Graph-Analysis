package GraphCreation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Class for importing graphs that are composed of lists of vertexes to other vertexes
 * @author iosbomb
 *
 */
public class VertexTVertexImporter implements GraphLoader {

	final String myIgnore = "#";
	final String mySplit = "\t";
	
	String path;
	EdgeType edgeType;
	public VertexTVertexImporter(String path, EdgeType edgeType) {
		this.path = path;
		this.edgeType = edgeType;
	}
	
	protected Graph<String, String> generateGraph() {
		if (edgeType.equals(EdgeType.DIRECTED))
			return new DirectedSparseGraph<String, String>();
		else
			return new UndirectedSparseGraph<String, String>();
	}

	@Override
	public Graph<String, String> loadGraph() throws Error, IOException {
		Graph<String, String> graph = generateGraph();
		BufferedReader br = new BufferedReader(new FileReader(path));
		String data;
		while((data = br.readLine()) != null) {
			if (!data.startsWith(myIgnore)) {
				// Import in A"\t"B
				String[] items = data.split(mySplit);
				if (items.length != 2) {
					br.close();
					throw new Error("Data Split was incorrectly formatted: " + data);
				}
				// Clean the pulled out values
				String source = items[0].trim();
				String dest = items[1].trim();
				
				// Add the edges (and vertexes). Don't cover false case, duplication of edges can cause that. Undirected Graphs may be a problem
				graph.addEdge(source + "-" + dest, source, dest);
			}
		}
		br.close();
		return graph;
	}

	@Override
	public String getInformation() {
		return "A->B " + edgeType.name() + "  Graph located at: " + path + "\n";
	}
}
