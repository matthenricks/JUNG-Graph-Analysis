package GraphAnalyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import Utils.JobTracker;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.graph.Graph;

public class SampleBC {

	static DecimalFormat dcf = new DecimalFormat("0.000");
	final static String myHeader = "\"userid\",\"bcScore\",\"ncScore\"";
	private static Pattern separateReg= Pattern.compile("\\,", Pattern.DOTALL);
		
	/**
	 * Reads in the sample BC file of the graph, giving an normal and adjusted BC value
	 * @param path - the path to the data file
	 * @return - a HashMap<String, Double> to compare against the other graph
	 * @throws IOException
	 */
	public static HashMap<String, Double[]> readGraphBC(String path) throws IOException {
		// First create the reader
		BufferedReader br = new BufferedReader(new FileReader(path));
		
		String data;
		if (!(data = br.readLine()).equals(myHeader)) {
			br.close();
			throw new Error("SampleBC Import Doesn't Match Header: " + myHeader);
		}
		
		HashMap<String, Double[]> result = new HashMap<String, Double[]>();
		while ((data = br.readLine()) != null) {
			// Import in "userID, bcScore"
			data = data.replaceAll("\"", "");
			String[] items=separateReg.split(data);
			if (items.length != 3) {
				br.close();
				throw new Error("Data Split was incorrectly formatted: " + data);
			}
			Double[] values = {Double.valueOf(items[1].trim()), Double.valueOf(items[2].trim())};
			result.put(items[0].trim(), values);
		}

		br.close();
		
		return result;
	}
		
	/**
	 * Prints out a CSV file of the BC of the graph with a normalized vector of the below equation
	 * newBC = oldBC / ((norm-1)*(norm-2)/2)
	 * @param graph - the graph used in the calculation
	 * @param path - output path
	 * @throws IOException 
	 */
	public static HashMap<String, Double[]> analyzeGraphBC(Graph<String, String> graph, String path) throws IOException {
		
		// First create the writer
		BufferedWriter bw = Utils.FileSystem.createFile(path);
		bw.write(myHeader+"\n");
		
		// Run the centrality algorithm
		BetweennessCentrality<String, String> cm = new BetweennessCentrality<String, String>(graph);
		cm.setRemoveRankScoresOnFinalize(false); 
		cm.evaluate();
		
		HashMap<String, Double[]> results = new HashMap<String, Double[]>();
		
		// Print out the results
		double value, norm = graph.getVertexCount();
		norm = (norm-1)*(norm-2)/2;
		for (String vertex : graph.getVertices()) {
			value = cm.getVertexRankScore(vertex);
			Double[] values = {value, value/norm};
			bw.write(vertex + "," + dcf.format(values[0]) + "," + dcf.format(values[1]) + "\n");
			results.put(vertex, values);
		}
		
		// Close to writer
		bw.close();
		
		return results;
	}	

	/**
	 * A class that allows for easy threading of the Sample BC function. All arguments are import in.
	 * @author MOREPOWER
	 *
	 */
	public static class CallableGraphBC implements Callable<HashMap<String, Double[]>> {

		Graph<String, String> myGraph;
		String myPath, myName;
		JobTracker jt;
		
		public CallableGraphBC(Graph<String, String> graph, String path, JobTracker jobTracker, String jobName) {
			myGraph = graph;
			myPath = path;
			jt = jobTracker;
			myName = jobName;
		}
		
		/**
		 * BC Function that maintains the values in a HashMap
		 * Additionally, this tracks the time required for the BC and output
		 */
		@Override
		public HashMap<String, Double[]> call() throws Exception {
			// First create the writer
			BufferedWriter bw = Utils.FileSystem.createFile(myPath);
			bw.write(myHeader+"\n");
			
			// Run the centrality algorithm
			BetweennessCentrality<String, String> cm = new BetweennessCentrality<String, String>(myGraph);
			cm.setRemoveRankScoresOnFinalize(false); 
			cm.evaluate();
			
			HashMap<String, Double[]> results = new HashMap<String, Double[]>();
			
			// Print out the results
			double value, norm = myGraph.getVertexCount();
			norm = (norm-1)*(norm-2)/2;
			for (String vertex : myGraph.getVertices()) {
				value = cm.getVertexRankScore(vertex);
				Double[] values = {value, value/norm};
				bw.write(vertex + "," + dcf.format(values[0]) + "," + dcf.format(values[1]) + "\n");
				results.put(vertex, values);
			}
			
			// Close to writer
			bw.close();
			
			return results;
		}
	}
	




}