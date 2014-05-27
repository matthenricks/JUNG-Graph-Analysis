package JUnitTests;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import GraphCreation.BarabasiAlbertGraphGenerator;
import GraphCreation.BasicGraph;
import GraphCreation.GraphLoader;
import Utils.FileSystem;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Tests the import/export functionality of the BasicGraph
 * @author MOREPOWER
 *
 */
public class ExportImportGraphs {

	/**
	 * Export a directed graph to make sure the edges transfer
	 * @throws Error 
	 * @throws IOException 
	 */
	@Test
	public void testDIRECTED() throws IOException, Error {

		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(40, 20, 200, EdgeType.DIRECTED);
		population = gL.loadGraph();
		
		File openFile = FileSystem.findOpenPath("export");
		BasicGraph.exportGraph(population, openFile.toString());
		
		Graph<String, String> imported = (new BasicGraph(openFile.toString())).loadGraph();
		
		JUnitUtils.checkGraphs(imported, population);
	}

	/**
	 * Export a directed graph to make sure the edges transfer
	 * @throws Error 
	 * @throws IOException 
	 */
	@Test
	public void testUNDIRECTED() throws IOException, Error {

		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(40, 20, 200, EdgeType.UNDIRECTED);
		population = gL.loadGraph();
		
		File openFile = FileSystem.findOpenPath("export");
		BasicGraph.exportGraph(population, openFile.toString());
		
		Graph<String, String> imported = (new BasicGraph(openFile.toString())).loadGraph();
		
		JUnitUtils.checkGraphs(imported, population);
	}
	
}