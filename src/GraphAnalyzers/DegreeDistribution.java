package GraphAnalyzers;

import java.io.BufferedWriter;
import java.io.IOException;

import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.graph.Graph;

public class DegreeDistribution {
	
	public static String myHeader = "\"userid\",\"degree\"";
	
	/**
	 * Analyze and write out the Degree of the graph
	 * @param graph - the graph you want to analyze
	 * @param path - the output location of the degree analysis
	 * @throws IOException
	 */
	public static void analyzeGraphDegree(Graph<String, String> graph, String path) throws IOException {
		
		// First create the writer
		BufferedWriter writer = Utils.FileSystem.createFile(path);
		writer.write(myHeader);
		writer.newLine();
		
		// Run the degree algorithm
		DegreeScorer<String> ds = new DegreeScorer<String>(graph);
		
		// Print out the results
		Integer degree;
		for (String vertex : graph.getVertices()) {
			degree = ds.getVertexScore(vertex);
			writer.write(vertex + "," + degree.toString());
			writer.newLine();
		}
		
		// Close to writer
		writer.close();
	}
}
