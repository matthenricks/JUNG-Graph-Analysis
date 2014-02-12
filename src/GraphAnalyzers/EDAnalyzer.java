package GraphAnalyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import Utils.HardCode;
import edu.uci.ics.jung.algorithms.metrics.Metrics;
import edu.uci.ics.jung.graph.Graph;

public class EDAnalyzer implements AnalyzerDistribution {
	
	final static String myHeader = "\"userid\",\"edScore\"";

	@Override
	public Map<String, Double> analyzeGraph(Graph<String, String> graph,
			String filepath) throws IOException {
		// First create the writer
		BufferedWriter bw = Utils.FileSystem.createFile(filepath);
		bw.write(myHeader+"\n");
		
		// Run the centrality algorithm
		
		Map<String, Double> result = Metrics.clusteringCoefficients(graph);
		
		// Print out the results
		for (Entry<String, Double> point : result.entrySet()) {
			bw.write(point.getKey() + "," + HardCode.dcf3.format(point.getValue()) + "\n");
		}
		
		// Close to writer
		bw.close();
		
		return result;
	}
	
	@Override
	public Map<String, Double> read(String filepath) throws IOException, Error {
		// First create the reader
		BufferedReader br = new BufferedReader(new FileReader(filepath));
		
		String data;
		if (!(data = br.readLine()).equals(myHeader)) {
			br.close();
			throw new Error("EDAnalyzer Import Doesn't Match Header: " + myHeader);
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
		return "Ego-Centric Density";
	}
}
