package GraphCreation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.io.PajekNetReader;
import edu.uci.ics.jung.io.PajekNetWriter;


/**
 * A class that allows for importing and exporting of graphs in the same style as the normal loaders
 * NOTE: The edges of this graph will not match, since vertex and edge ids will not be the same. The value of the vertex and edges will though
 * @author MOREPOWER
 *
 */
public class BasicGraph extends ImportedGraph {

	int count = 0;
	
	public BasicGraph(String path) {
		super(path);
	}
	
	public Graph<String, String> loadGraph() throws Error, IOException {
				
		BufferedReader reader = new BufferedReader(new FileReader(new File(this.path)));
		String type = reader.readLine();
		if (type == null) {
			reader.close();
			throw new Error("First line is empty");			
		}
		
		if (EdgeType.valueOf(type) == EdgeType.DIRECTED) {
			PajekNetReader<DirectedSparseGraph<String, String>, String, String> readerD = new PajekNetReader<DirectedSparseGraph<String, String>, String, String>(
					vertexFactory, edgeFactory);
			
			DirectedSparseGraph<String, String> imported = new DirectedSparseGraph<String, String>();
			readerD.load(reader, imported);
			reader.close();
			return imported;
			
		} else if (EdgeType.valueOf(type) == EdgeType.UNDIRECTED) {
			PajekNetReader<UndirectedSparseGraph<String, String>, String, String> readerU = new PajekNetReader<UndirectedSparseGraph<String, String>, String, String>(
					vertexFactory, edgeFactory);
			
			UndirectedSparseGraph<String, String> imported = new UndirectedSparseGraph<String, String>();
			readerU.load(reader, imported);
			reader.close();
			return imported;
			
		} else {
			reader.close();
			throw new Error("Unrecognized edge type");			
		}
	}
	
	/**
	 * Helper to just write the class
	 */
	private static void exportGraph(Graph<String, String> graph, BufferedWriter writer) throws IOException {
		// First write out what type of graph it is
		writer.write(graph.getDefaultEdgeType().toString());
		writer.newLine();
		// Now write information pertinent to the graph
		PajekNetWriter<String, String> graphWriter = new PajekNetWriter<String, String>();
		graphWriter.save(graph, writer);
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
		BufferedWriter writer = Utils.FileSystem.createFileIfNonexistant(path);
		if (writer != null) {
			exportGraph(graph, writer);
			writer.close();
		}
	}
	
	/**
	 * Exports the data in the graph to the path. Errors if not unique
	 *    -Edge Type
	 *    -Pajek Formatted List
	 * @param graph
	 * @param location
	 * @throws IOException 
	 */
	public static void exportNewGraph(Graph<String, String> graph, String path) throws IOException {
		BufferedWriter writer = Utils.FileSystem.createNewFile(path);
		exportGraph(graph, writer);
		writer.close();
	}
	
	@Override
	public String getInformation() {
		return "Basic Graph located at: " + this.path + "\n";
	}
}
