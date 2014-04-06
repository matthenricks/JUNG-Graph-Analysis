package GraphAnalyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import Utils.HardCode;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Graph;

public class WCCSizeAnalysis extends AnalyzerDistribution {
	
	static DecimalFormat dcf = new DecimalFormat("0.000");
	final static String myHeader = "\"cluster number\",\"size\"";
	
	/**
	 * Generates information regarding the weakly clustered components and outputs them in a CSV at a specified location
	 * @param sgraph
	 * @param summary
	 * @throws IOException
	 */
	public static Integer analyzeBasicClusterInformation(Graph<String, String> sgraph, String path) throws IOException{

		// Create the summary file
		BufferedWriter summary = Utils.FileSystem.createNewFile(path);
		
		WeakComponentClusterer<String, String> clusters = new WeakComponentClusterer<String, String>();
		Set<Set<String>> setx = clusters.transform(sgraph);
		summary.write(myHeader + "\n");
		int i = 0;
		for(Set<String> ss: setx){
			i++;
			// TODO: Should this write out the involved nodes instead? It lacks some information removing this feature
			summary.write("component " + i + "," + ss.size());
			summary.newLine();
		}
		
		summary.close();
		return setx.size();
	}

	@Override
	public Map<String, Double> analyzeGraph(Graph<String, String> graph,
			String filepath) throws IOException {
		// Create the summary file
		BufferedWriter summary = Utils.FileSystem.createNewFile(filepath);
		
		WeakComponentClusterer<String, String> clusters = new WeakComponentClusterer<String, String>();
		Set<Set<String>> setx = clusters.transform(graph);
		summary.write(myHeader + "\n");
		int i = 0;
		for(Set<String> ss: setx){
			i++;
			// TODO: Should this write out the involved nodes instead? It lacks some information removing this feature
			summary.write("component " + i + "," + ss.size());
			summary.newLine();
		}
		
		summary.close();
		
		TreeMap<String, Double> retVal = new TreeMap<String, Double>();
		retVal.put("Total WCC Clusters", (double)setx.size());
		
		return retVal;
	}

	@Override
	public Map<String, Double> read(String filepath)
			throws FileNotFoundException, IOException, Error {
		// First create the reader
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		
		String data;
		if (!(data = br.readLine()).equals(myHeader)) {
			br.close();
			throw new Error("WCC Import Doesn't Match Header: " + myHeader);
		}
		
		// TODO: This will just read in the total amount of lines that exist
		TreeMap<String, Double> clusterAmount = new TreeMap<String, Double>();
		double counter = 0;
		while ((data = br.readLine()) != null) {
			// Import in "userID, bcScore"
			data = data.replaceAll("\"", "");
			String[] items = HardCode.separateReg.split(data);
			if (items.length != 2) {
				br.close();
				throw new Error("Data Split was incorrectly formatted: " + data);
			}
			
			counter++;
		}
		
		clusterAmount.put("Total WCC Clusters", counter);
		br.close();
		
		return clusterAmount;
	}

	@Override
	public String getName() {
		return "WCC Analysis";
	}
}
