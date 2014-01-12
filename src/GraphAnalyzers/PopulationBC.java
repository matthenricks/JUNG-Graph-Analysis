package GraphAnalyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import Utils.JobTracker;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.graph.Graph;

public class PopulationBC {
	
	static DecimalFormat dcf = new DecimalFormat("0.000");
	final static String myHeader = "\"userid\",\"bcScore\"";
	private static Pattern separateReg= Pattern.compile("\\,", Pattern.DOTALL);
	
	/**
	 * Reads in the population BC file of the graph
	 * @param path - the path to the data file
	 * @return - a HashMap<String, Double> to compare against the other graph
	 * @throws IOException
	 */
	public static HashMap<String, Double> readGraphBC(String path, Set<String> keyValues) throws IOException {
		// First create the reader
		BufferedReader br = new BufferedReader(new FileReader(path));
		
		String data;
		if (!(data = br.readLine()).equals(myHeader)) {
			br.close();
			throw new Error("PopulationBC Import Doesn't Match Header: " + myHeader);
		}
		
		HashMap<String, Double> result = new HashMap<String, Double>();
		while ((data = br.readLine()) != null) {
			// Import in "userID, bcScore"
			data = data.replaceAll("\"", "");
			String[] items=separateReg.split(data);
			if (items.length != 2) {
				br.close();
				throw new Error("Data Split was incorrectly formatted: " + data);
			}
			if (keyValues.contains(items[0].trim()))
				result.put(items[0].trim(), Double.valueOf(items[1].trim()));
		}

		br.close();
		
		return result;
	}
	
	/**
	 * Reads in the population BC file of the graph
	 * @param path - the path to the data file
	 * @return - a HashMap<String, Double> to compare against the other graph
	 * @throws IOException
	 */
	public static HashMap<String, Double> readGraphBC(String path) throws IOException {
		// First create the reader
		BufferedReader br = new BufferedReader(new FileReader(path));
		
		String data;
		if (!(data = br.readLine()).equals(myHeader)) {
			br.close();
			throw new Error("PopulationBC Import Doesn't Match Header: " + myHeader);
		}
		
		HashMap<String, Double> result = new HashMap<String, Double>();
		while ((data = br.readLine()) != null) {
			// Import in "userID, bcScore"
			data = data.replaceAll("\"", "");
			String[] items=separateReg.split(data);
			if (items.length != 2) {
				br.close();
				throw new Error("Data Split was incorrectly formatted: " + data);
			}
			
			result.put(items[0].trim(), Double.valueOf(items[1].trim()));
		}

		br.close();
		
		return result;
	}
	
	/**
	 * Analyze and write out the BC of the graph
	 * @param graph
	 * @param path - the location this will write the BC to
	 * @throws IOException
	 */
	public static HashMap<String, Double> analyzeGraphBC(Graph<String, String> graph, String path) throws IOException {
		
		// First create the writer
		BufferedWriter bw = Utils.FileSystem.createFile(path);
		bw.write(myHeader+"\n");
		
		// Run the centrality algorithm
		BetweennessCentrality<String, String> cm = new BetweennessCentrality<String, String>(graph);
		cm.setRemoveRankScoresOnFinalize(false); 
		cm.evaluate();
		
		HashMap<String, Double> results = new HashMap<String, Double>();
		
		// Print out the results
		double value;
		for (String vertex : graph.getVertices()) {
			value = cm.getVertexRankScore(vertex);
			bw.write(vertex + "," + dcf.format(value) + "\n");
			results.put(vertex, value);
		}
		
		// Close to writer
		bw.close();
		
		return results;
	}

	
	/**
	 * A class that allows for easy threading of the Population BC function. All arguments are import in.
	 * @author MOREPOWER
	 *
	 */
	public static class CallableGraphBC implements Callable<ConcurrentHashMap<String, Double>> {

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
		 * BC Function that does maintain the values in a HashMap
		 * Additionally, this tracks the time required for the BC and output
		 */
		@Override
		public ConcurrentHashMap<String, Double> call() throws Exception {
			// First create the writer
			BufferedWriter bw = Utils.FileSystem.createFile(myPath);
			bw.write(myHeader+"\n");
			
			// Run the centrality algorithm
			ConcurrentHashMap<String, Double> hashMap = new ConcurrentHashMap<String, Double>(myGraph.getVertexCount());
			BetweennessCentrality<String, String> cm = new BetweennessCentrality<String, String>(myGraph);
			cm.setRemoveRankScoresOnFinalize(false);
			jt.startTracking(myName);
			cm.evaluate();
			jt.endTracking(myName);
						
			// Print out the results
			jt.startTracking(myName + "output");
			double value;
			for (String vertex : myGraph.getVertices()) {
				value = cm.getVertexRankScore(vertex);
				bw.write(vertex + "," + dcf.format(value) + "\n");
				hashMap.put(vertex, value);
			}
			jt.endTracking(myName + "output");
			
			// Close to writer
			bw.close();
			
			return hashMap;
		}
	}

	
	/**
	 * A class that allows for easy threading of the Population BC function. All arguments are import in.
	 * @author MOREPOWER
	 *
	 */
	public static class ThreadedGraphBC implements Runnable {

		Graph<String, String> myGraph;
		String myPath, myName;
		JobTracker jt;
		
		public ThreadedGraphBC(Graph<String, String> graph, String path, JobTracker jobTracker, String jobName) {
			myGraph = graph;
			myPath = path;
			jt = jobTracker;
			myName = jobName;
		}
		
		/**
		 * BC Function that does not maintain the values in a HashMap
		 * Additionally, this tracks the time required for the BC and output
		 */
		@Override
		public void run() {
			// First create the writer
			try {
				BufferedWriter bw = Utils.FileSystem.createFile(myPath);
				bw.write(myHeader+"\n");
				
				// Run the centrality algorithm
				BetweennessCentrality<String, String> cm = new BetweennessCentrality<String, String>(myGraph);
				cm.setRemoveRankScoresOnFinalize(false);
				jt.startTracking(myName);
				cm.evaluate();
				jt.endTracking(myName);
							
				// Print out the results
				jt.startTracking(myName + "output");
				double value;
				for (String vertex : myGraph.getVertices()) {
					value = cm.getVertexRankScore(vertex);
					bw.write(vertex + "," + dcf.format(value) + "\n");
				}
				jt.endTracking(myName + "output");
				
				// Close to writer
				bw.close();
			} catch (IOException e) {
				System.err.println("Error in FileWriter of the threaded population BC");
				e.printStackTrace();
			}
		}
	}
}
