package GraphAnalyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import Utils.HardCode;
import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.graph.Graph;

public class DegreeAnalyzer extends AnalyzerDistribution {
	
	public static String myHeader = "\"userid\",\"degree\"";
	
	@Override
	public Map<String, Double> analyzeGraph(Graph<String, String> graph,
			String filepath) throws IOException {
		// First create the writer
		BufferedWriter writer = Utils.FileSystem.createNewFile(filepath);
		writer.write(myHeader);
		writer.newLine();
		
		// Hashmap for the degree scoring
		HashMap<String, Double> degreeDistro = new HashMap<String, Double>(graph.getVertexCount());
		
		// Run the degree algorithm
		DegreeScorer<String> ds = new DegreeScorer<String>(graph);
		
		// Print out the results
		Integer degree;
		for (String vertex : graph.getVertices()) {
			degree = ds.getVertexScore(vertex);
			writer.write(vertex + "," + degree.toString());
			writer.newLine();
			degreeDistro.put(vertex, (double)degree);
		}
		
		// Close to writer
		writer.close();
		
		return degreeDistro;
	}

	@Override
	public Map<String, Double> read(String filepath)
			throws FileNotFoundException, IOException, Error {
		// First create the reader
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		
		String data;
		if ((data = br.readLine()) == null || data.equals(myHeader) == false) {
			br.close();
			throw new Error("DEAnalyzer Import Doesn't Match Header: " + myHeader);
		}
		
		HashMap<String, Double> result = new HashMap<String, Double>();
		while ((data = br.readLine()) != null) {
			// Import in "userID, bcScore"
			data = data.replaceAll("\"", "");
			String[] items = HardCode.separateReg.split(data);
			if (items.length != 2) {
				br.close();
				throw new Error("Data Split was incorrectly formatted: " + data);
			}
			
			result.put(items[0].trim(), Double.valueOf(items[1].trim()));
		}

		br.close();
		
		return result;
	}

	@Override
	public String getName() {
		return "Degree Distribution";
	}
	
	
	/*** OLD FUNCTIONS ***/
	
	/**
	 * Analyze and write out the Degree of the graph
	 * @param graph - the graph you want to analyze
	 * @param path - the output location of the degree analysis
	 * @throws IOException
	 */
	public static HashMap<String, Double> analyzeGraphDegree(Graph<String, String> graph, String path) throws IOException {
		
		// First create the writer
		BufferedWriter writer = Utils.FileSystem.createNewFile(path);
		writer.write(myHeader);
		writer.newLine();
		
		// Hashmap for the degree scoring
		HashMap<String, Double> degreeDistro = new HashMap<String, Double>(graph.getVertexCount());
		
		// Run the degree algorithm
		DegreeScorer<String> ds = new DegreeScorer<String>(graph);
		
		// Print out the results
		Integer degree;
		for (String vertex : graph.getVertices()) {
			degree = ds.getVertexScore(vertex);
			writer.write(vertex + "," + degree.toString());
			writer.newLine();
			degreeDistro.put(vertex, (double)degree);
		}
		
		// Close to writer
		writer.close();
		
		return degreeDistro;
	}
}
