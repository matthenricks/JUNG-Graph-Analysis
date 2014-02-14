package GraphAnalyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import Utils.HardCode;
import Utils.JobTracker;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.graph.Graph;

public class BCAnalyzer implements AnalyzerDistribution {
	
	final static String myHeader = "\"userid\",\"bcScore\"";

	@Override
	public Map<String, Double> analyzeGraph(Graph<String, String> graph,
			String filepath) throws IOException {
		// First create the writer
		BufferedWriter bw = Utils.FileSystem.createFile(filepath);
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
			bw.write(vertex + "," + HardCode.dcf3.format(value) + "\n");
			results.put(vertex, value);
		}
		
		// Close to writer
		bw.close();
		
		return results;
	}
	
	@Override
	public Map<String, Double> read(String filepath) throws IOException, Error {
		// First create the reader
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		
		String data;
		if (!(data = br.readLine()).equals(myHeader)) {
			br.close();
			throw new Error("PopulationBC Import Doesn't Match Header: " + myHeader);
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
		return "Betweenness Centrality";
	}
	
	
	/* OLD STUFF NEEDED TO MAINTAIN OLDER VERSIONS. NOTE THEY ARE DUPLICATIONS */
	
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
	
	/**
	 * Reads in the population BC file of the graph
	 * @param path - the path to the data file
	 * @return - a HashMap<String, Double> to compare against the other graph
	 * @throws IOException
	 */
	public static ConcurrentHashMap<String, Double> readGraphBCConcurr(String path) throws IOException {
		// First create the reader
		BufferedReader br = new BufferedReader(new FileReader(path));
		
		String data;
		if (!(data = br.readLine()).equals(myHeader)) {
			br.close();
			throw new Error("PopulationBC Import Doesn't Match Header: " + myHeader);
		}
		
		ConcurrentHashMap<String, Double> result = new ConcurrentHashMap<String, Double>();
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
			bw.write(vertex + "," + HardCode.dcf3.format(value) + "\n");
			results.put(vertex, value);
		}
		
		// Close to writer
		bw.close();
		
		return results;
	}


	/**
	 * A class that allows for easy threading of the BC analysis function. All arguments are import in.
	 * @author MOREPOWER
	 *
	 */
	public static class CallableGraphHashBC implements Callable<HashMap<String, Double>> {

		Graph<String, String> myGraph;
		String myPath, myName;
		JobTracker jt;
		
		public CallableGraphHashBC(Graph<String, String> graph, String path, JobTracker jobTracker, String jobName) {
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
		public HashMap<String, Double> call() throws Exception {
			jt.startTracking("BC Calculation of " + myName);
			HashMap<String, Double> hashMap = BCAnalyzer.analyzeGraphBC(myGraph, myPath);
			jt.endTracking("BC Calculation of " + myName);			
			return hashMap;
		}
	}
	
	/**
	 * A class that allows for easy threading of the BC analysis function. All arguments are import in.
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
			jt.startTracking("BC Calculation of " + myName);
			cm.evaluate();
			jt.endTracking("BC Calculation of " + myName);
						
			// Print out the results
			jt.startTracking("Output of " + myName);
			double value;
			for (String vertex : myGraph.getVertices()) {
				value = cm.getVertexRankScore(vertex);
				bw.write(vertex + "," + HardCode.dcf3.format(value) + "\n");
				hashMap.put(vertex, value);
			}
			jt.endTracking("Output of " + myName);
			
			// Close to writer
			bw.close();
			
			return hashMap;
		}
	}	
}
