package GraphAnalyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import Utils.HardCode;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Graph;

/**
 * Graph Analyzer for Weakly Connected Components. Serves as a wrapper on JUNG
 * @author MOREPOWER
 *
 */
public class WCCSizeAnalysis extends AnalyzerDistribution {
	
	static DecimalFormat dcf = new DecimalFormat("0.000");
	final static String myHeader = "\"cluster number\",\"size\"";
	
	@Override
	// Note, this will only return the total WCC cluster count
	public Map<String, Double> analyzeGraph(Graph<String, String> graph,
			String filepath) throws IOException {
		// Create the summary file
		BufferedWriter summary = Utils.FileSystem.createNewFile(filepath);
				
		WeakComponentClusterer<String, String> clusters = new WeakComponentClusterer<String, String>();
		Set<Set<String>> setx = clusters.transform(graph);
		
		// Create the holder
		HashMap<String, Double> wccClusters = new HashMap<String, Double>();
		
		// Run the output
		summary.write(myHeader + "\n");
		int i = 0;
		for(Set<String> ss: setx){
			i++;
			wccClusters.put(String.valueOf(i),  new Double(ss.size()));	
			summary.write("component " + i + "," + ss.size());
			summary.newLine();
		}
		summary.close();

		return wccClusters;
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
		
		// Create the store for WCC
		HashMap<String, Double> clusters = new HashMap<String, Double>();

		while ((data = br.readLine()) != null) {
			// Import in "userID, bcScore"
			data = data.replaceAll("\"", "");
			String[] items = HardCode.separateReg.split(data);
			if (items.length != 2) {
				br.close();
				throw new Error("Data Split was incorrectly formatted: " + data);
			}
			
			clusters.put(items[0], Double.parseDouble(items[1]));
		}
		br.close();
		return clusters;
	}

	@Override
	public String getName() {
		return "WCC Analysis";
	}
}
