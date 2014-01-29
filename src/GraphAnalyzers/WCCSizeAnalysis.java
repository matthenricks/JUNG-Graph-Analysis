package GraphAnalyzers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Set;

import Utils.CSV_Builder;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Graph;

public class WCCSizeAnalysis {
	
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
		BufferedWriter summary = Utils.FileSystem.createFile(path);
		
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
}
