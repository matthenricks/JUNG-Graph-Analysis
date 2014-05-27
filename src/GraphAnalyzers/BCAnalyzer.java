package GraphAnalyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import Utils.HardCode;
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality;
import edu.uci.ics.jung.graph.Graph;

/**
 * Analyzer class that calculates betweenness centrality
 * @author MOREPOWER
 *
 */
public class BCAnalyzer extends AnalyzerDistribution {
	
	// The header for the csv
	final static String myHeader = "\"userid\",\"bcScore\"";
	
	// the amount of sig figs. Since this is an approximation, there is a level of randomness, this can remove
	final static int sigFigs = 7;
	
	@Override
	public Map<String, Double> analyzeGraph(Graph<String, String> graph,
			String filepath) throws IOException {
		// First create the writer
		BufferedWriter bw = Utils.FileSystem.createNewFile(filepath);
		bw.write(myHeader+"\n");
		
		// Run the centrality algorithm
		BetweennessCentrality<String, String> cm = new BetweennessCentrality<String, String>(graph);
		cm.setRemoveRankScoresOnFinalize(false); 
		cm.evaluate();
		
		HashMap<String, Double> results = new HashMap<String, Double>();
		
		// Print out the results
		double value;
		for (String vertex : graph.getVertices()) {
			value = Utils.HardCode.floorValue(cm.getVertexRankScore(vertex), sigFigs);
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
		if ((data = br.readLine()) == null || data.equals(myHeader) == false) {
			br.close();
			throw new Error("BCAnalyzer Import Doesn't Match Header: " + myHeader);
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
}
