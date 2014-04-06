package Main;

import java.io.BufferedWriter;
import java.io.IOException;

import GraphCreation.BasicGraph;
import GraphCreation.GeneratedGraph;
import SamplingAlgorithms.RDBFSSample;
import SamplingAlgorithms.SampleMethod;
import Utils.ArgumentReader;
import Utils.HardCode;
import Utils.JobTracker;
import edu.uci.ics.jung.graph.Graph;

/**
 * This class takes in a parent graph and samples them using varying different parameters
 * @author iosbomb
 *
 */
// TODO: Add the sampling as another class they can record values on
// Different sample classes can turn into a parse-like structure
public class SampleGraph {

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
		
		// If the graph was generated, export that graph
		if (loader.myGraphLoader instanceof GeneratedGraph) {
			mainTracker.startTracking("Export Generated Graph");
			BasicGraph.exportGraph(graph, loader.myOutput + HardCode.pDataFix);
			mainTracker.endTracking("Export Generated Graph");
		}
		
		mainTracker.startTracking("Overall Graph Sampling");
		for (double threshold = 0; threshold <= 1.0; threshold += 0.2) {
			for (double alpha = 0.0035; alpha < 1.0; alpha = alpha * 2) {
				// Set the max limiter as to achieve the percentage below of the total population (if possible)
				// 1.0 is no limit as the amount of neighbor additions = possible neighbors
				for (double percentMaxLimit = 0; percentMaxLimit <= 1.0; percentMaxLimit += 0.2) {
					// Must be able to take at least take one neighbor
					int maxLimit = Math.max((int)Math.ceil(percentMaxLimit/alpha)-1, 1);
					
					JobTracker sampleTracker = new JobTracker();
					int NodeSize = 0;
					int EdgeSize = 0;
					
					String sampleName = "Sample-" + HardCode.dcf.format(percentMaxLimit*100) + "P-" + HardCode.dcf.format(threshold*10000) + "-" + HardCode.dcf.format(alpha*10000);
					Utils.FileSystem.createFolder(loader.myOutput + "/" + sampleName);
					
					for (int replica = 0; replica < 5; replica++) {
						// Select the sampling method
						SampleMethod rndSample = new RDBFSSample(alpha, threshold, maxLimit);
						// First we need to actually generate the sample graph
						sampleTracker.startTracking("Sampling of " + sampleName + ":" + replica);
						rndSample.sampleGraph(graph);
						Graph<String, String> sample = rndSample.getGraph();
						sampleTracker.endTracking("Sampling of " + sampleName + ":" + replica);
						
						NodeSize += sample.getVertexCount();
						EdgeSize += sample.getEdgeCount();
	
						sampleTracker.startTracking("Writing of " + sampleName + ":" + replica);
						BasicGraph.exportGraph(sample, loader.myOutput + "/" + sampleName + "/" + replica + ".dat");
						sampleTracker.endTracking("Writing of " + sampleName + ":" + replica);
					}
					
					String sampleOut = "Summary-" + HardCode.dcf.format(percentMaxLimit*100) + "P-" + HardCode.dcf.format(threshold*10000) + "-" + HardCode.dcf.format(alpha*10000);
					BufferedWriter bw = Utils.FileSystem.createNewFile(loader.myOutput + "/" + sampleName + "/" + sampleOut);
					bw.append("Average Node Size = " + NodeSize/5);
					bw.newLine();
					bw.append("Average Edge Size = " + EdgeSize/5);
					bw.newLine();
					sampleTracker.writeJobTimes(bw);
					bw.close();
				}
			}
		}
		mainTracker.endTracking("Overall Graph Sampling");
		
		BufferedWriter mySummary = Utils.FileSystem.createNewFile(loader.myOutput + HardCode.pSummaryPostfix);
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
