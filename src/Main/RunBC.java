package Main;

import java.io.BufferedWriter;
import java.io.IOException;

import GraphAnalyzers.BCAnalyzer;
import GraphAnalyzers.WCCSizeAnalysis;
import GraphCreation.BasicGraph;
import GraphCreation.GeneratedGraph;
import Utils.ArgumentReader;
import Utils.HardCode;
import Utils.JobTracker;
import edu.uci.ics.jung.graph.Graph;

public class RunBC {

	/**
	 * This main will just Run the BC/analysis and and generate samples on the side. That's it.
	 * @param args
	 * @throws Error 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, Error {
		JobTracker mainTracker = new JobTracker();
		mainTracker.startTracking("Argument Import");
		ArgumentReader loader = ArgumentReader.read(args);
		mainTracker.endTracking("Argument Import");
		
		// Make the overall output folder
		Utils.FileSystem.createFolder(loader.myOutput);
		
		// Import in the graph and record it's basic statistics
		mainTracker.startTracking("Graph Loading");
		Graph<String, String> graph = loader.myGraphLoader.loadGraph();
		mainTracker.endTracking("Graph Loading");
		
		mainTracker.startTracking("Calculate BC");
		BCAnalyzer.analyzeGraphBC(graph, loader.myOutput + HardCode.pBcPostfix);
		mainTracker.endTracking("Calculate BC");
		
		mainTracker.startTracking("Calculate WCC");
		WCCSizeAnalysis.analyzeBasicClusterInformation(graph, loader.myOutput + HardCode.pWccPostfix);
		mainTracker.endTracking("Calculate WCC");
		
		// If the graph was generated, export that graph
		if (loader.myGraphLoader instanceof GeneratedGraph) {
			mainTracker.startTracking("Export Generated Graph");
			BasicGraph.exportGraph(graph, loader.myOutput + HardCode.pDataFix);
			mainTracker.endTracking("Export Generated Graph");
		}
		
		BufferedWriter mySummary = Utils.FileSystem.createFile(loader.myOutput + HardCode.pSummaryPostfix);
		mySummary.append(loader.myGraphLoader.getInformation());
		mySummary.append("Actual graph statistics are as follows: ");
		mySummary.newLine();
		mySummary.write("\tEdges: " + graph.getEdgeCount());
		mySummary.newLine();
		mySummary.write("\tVertices: " + graph.getVertexCount());
		mySummary.newLine();
		mySummary.newLine();
		mySummary.append("The job times are as follows: ");
		mySummary.newLine();
		mainTracker.writeJobTimes(mySummary, "\t");
		mySummary.close();
	}

}
