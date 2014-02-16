package Main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Correlation.MeasureComparison;
import GraphAnalyzers.AnalyzerDistribution;
import GraphAnalyzers.BCAnalyzer;
import GraphAnalyzers.DegreeAnalyzer;
import GraphAnalyzers.EDAnalyzer;
import GraphAnalyzers.WCCSizeAnalysis;
import GraphCreation.BasicGraph;
import GraphCreation.GeneratedGraph;
import SamplingAlgorithms.RDBFSSample;
import Utils.ArgumentReader;
import Utils.CSV_Builder;
import Utils.CSV_Builder_Objects.CSV_Percent;
import Utils.HardCode;
import Utils.JobTracker;
import edu.uci.ics.jung.graph.Graph;

/**
 * The objective of this class is to cleanly run the project. This will involve these series of steps:
 * As noted above, BC calculation is separate from all other types of calculation and splitting since it takes so long
 * @author MOREPOWER
 *
 */
public class AnalysisRunner {
	
	static volatile ExecutorService threadPool;
	
	/**
	 * Convenience function to stop all the executor services if there's an error
	 */
	public static void stopAllThreads() {
		threadPool.shutdownNow();
	}
	
	/**
	 * Function to write out basic information
	 * @param tracker
	 * @param graph
	 * @param outputDir
	 * @param jobName
	 * @throws IOException 
	 * @returns a CSV_Builder object with the WCC values
	 */
	public static CSV_Builder writeBasicInformation(JobTracker tracker, Graph<String, String> graph, String outputDir, String jobName) throws IOException {
		// Output some defining graph metrics
		tracker.startTracking("basic writing of " + jobName);
		
		if ((new File(outputDir + HardCode.pMetricPostfix)).exists()) {
			System.out.println("The basic information for the graph: " + jobName + " exists");
		}
		
		BufferedWriter bw = Utils.FileSystem.createFile(outputDir + HardCode.pMetricPostfix);
		bw.write("The number of edges are: " + graph.getEdgeCount());
		bw.newLine();
		bw.write("The number of indices are: " + graph.getVertexCount());
		bw.newLine();
		bw.close();
		tracker.endTracking("basic writing of " + jobName);
		
		// Get the WCC of this graph so that we better understand it's density
		tracker.startTracking("WCC calculation of " + jobName);
		Integer WCC = -1;
		if ((new File(outputDir + HardCode.pWccPostfix)).exists()) {
			WCC = (int)Math.floor((new WCCSizeAnalysis()).read(outputDir + HardCode.pWccPostfix).get("Total WCC Clusters"));
		} else {
			WCC = (int)Math.floor((new WCCSizeAnalysis()).analyzeGraph(graph, outputDir + HardCode.pWccPostfix).get("Total WCC Clusters"));
		}
		tracker.endTracking("WCC calculation of " + jobName);
		return new CSV_Builder(WCC);
	}
	
	/**
	 * The different holders for the population data -- to be shared among threads
	 */
	// Volatile is needed since they aren't pulled from a function
	static volatile ConcurrentHashMap<String, Double> populationBC;
	static volatile ConcurrentHashMap<String, Double> populationDegree;
	static volatile ConcurrentHashMap<String, Double> populationED;
	
	private static Lock popSetLock = new ReentrantLock();
	private static Condition popWait = popSetLock.newCondition();
	
	public static void setPop(Map<String, Double> popBC, Map<String, Double> popDegree, Map<String, Double> popED) {
		popSetLock.lock();
		System.out.println("Population Values Posted");
		populationBC = new ConcurrentHashMap<String, Double>(popBC);
		populationDegree = new ConcurrentHashMap<String, Double>(popDegree);
		populationED = new ConcurrentHashMap<String, Double>(popED);
		popWait.signalAll();
		popSetLock.unlock();
	}
	public static void waitForPop() throws InterruptedException {
		// Wait until the populationBC has been written to
		popSetLock.lock();
		while(populationBC == null || populationDegree == null || populationED == null) {
			System.out.println("Sample thread waiting for Population BC Values");
			popWait.await();
		}
		popSetLock.unlock();
		System.out.println("Population Values Pulled");		
	}
		
	public static class SampleThreadRunner implements Callable<CSV_Builder> {
		
		private String sampleDir, sampleName;
		private Graph<String, String> parentGraph;
		private RDBFSSample sampleMethod;
		private Integer ID;
		
		/**
		 * Constructor for the sample runner thread. This involves the thread management of sampling, analysis, and correlation.
		 * @param sampleDir - directory of the sample to save it's data and files
		 * @param sampleName - the unique name of the sample
		 * @param sampleMethod - the method of sampling used to obtain the sample
		 * @param parentGraph - the parent graph to be sampled
		 * @param executorService - the executor (this isn't necessarily needed)
		 */
		public SampleThreadRunner(String sampleDir, String sampleName, RDBFSSample sampleMethod, 
				Graph<String, String> parentGraph, Integer ID) {			
			this.sampleDir = sampleDir;
			this.sampleName = sampleName;
			this.sampleMethod = sampleMethod;
			this.parentGraph = parentGraph;
			this.ID = ID;
		}

		@Override
		// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		public CSV_Builder call() throws Exception {
			// Use the ID to consolidate everything together
			CSV_Builder cID = new CSV_Builder(this.ID);
			
			// Make this entire thing an alpha loop
			double[] alphaArea = {
					0.01, 0.02, 0.03, 0.04, 
					0.05, 0.1, 0.15, 
					0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
			
			for (double alpha : alphaArea) {
				// Record it's own job stuff in here!
				JobTracker sampleTracker = new JobTracker();

				// Now set the max sample
				sampleMethod.changeAlpha(alpha);

				// First we need to actually generate the sample graph
				sampleTracker.startTracking("Initial sampling of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
				// Iterations, Real Alpha, Real Threshold
				CSV_Builder cSamplingStats = sampleMethod.sampleGraph(parentGraph);
				Graph<String, String> sample = sampleMethod.getGraph();
				sampleTracker.endTracking("Initial sampling of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));

				// Create a folder segmenting by alpha
				String aFolder = sampleDir + "/" + "alpha" + HardCode.dcf.format(alpha * 10000);
				Utils.FileSystem.createFolder(aFolder);

				// Output the graph
				BasicGraph.exportGraph(sample, aFolder + HardCode.pDataFix);
				
				/** Now begin the analysis **/
				// Write out the  write out the basic information pertaining to the graph
				CSV_Builder cWCC = BCRunnerSimple.writeBasicInformation(sampleTracker, sample, aFolder, sampleName);

				// Wait for the population thread to finish
				waitForPop();

				Map<String, AnalyzerDistribution> analyzers = new HashMap<String, AnalyzerDistribution>();
				analyzers.put(HardCode.pBcPostfix, (new BCAnalyzer()));
				analyzers.put(HardCode.pDegreePostfix, (new DegreeAnalyzer()));
				analyzers.put(HardCode.pEDPostfix, (new EDAnalyzer()));
				
				// Run all of the analyzers
				for (Entry<String, AnalyzerDistribution> analyzer : analyzers.entrySet()) {
					// Find and process the BC of the graph
					sampleTracker.startTracking(analyzer.getValue().getName() + " of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
					HashMap<String, Double> sampleValue = new HashMap<String, Double>(analyzer.getValue().analyzeGraph(sample, aFolder + analyzer.getKey()));
					sampleTracker.endTracking(analyzer.getValue().getName() + " of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
				
					// Generate the CSV_Builder to link
					CSV_Builder cDur = new CSV_Builder(sampleTracker.getJobTime(analyzer.getValue().getName() + " of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000)));
					cDur.LinkTo(new CSV_Builder(analyzer.getValue().getName()));
					
					if (analyzer.getValue() instanceof BCAnalyzer)
						cDur.LinkToEnd(MeasureComparison.compare(populationBC, sampleValue));
					else if (analyzer.getValue() instanceof DegreeAnalyzer)
						cDur.LinkToEnd(MeasureComparison.compare(populationDegree, sampleValue));
					else
						cDur.LinkToEnd(MeasureComparison.compare(populationED, sampleValue));

					// Connect the CSV to the duration
					cWCC.LinkTo(cDur);
				}

				// Write out the respective job times
				BufferedWriter jobOutput = Utils.FileSystem.createFile(aFolder + HardCode.pSummaryPostfix);
				sampleTracker.writeJobTimes(jobOutput);
				jobOutput.close();
					
				/** Return all the pertinent information by using the CSV Builders **/
				
				// Link: Iterations, Real Alpha, Real Threshold, <-- WCC...
				cSamplingStats.LinkToEnd(cWCC);
					
				// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
				cID.LinkTo(new CSV_Builder(new CSV_Percent(alpha), 
						new CSV_Builder(sample.getVertexCount(),
							new CSV_Builder(sample.getEdgeCount(),
									cSamplingStats))));					
			}
			return cID;
		}
	}
	
	/**
	 * This shall run the entire sequence. The argument input sequence isn't well designed yet  
	 * @param args
	 * @throws Error 
	 * @throws IOException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws ExecutionException 
	 * @throws TimeoutException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ExecutionException, TimeoutException, InterruptedException {
		
		// Set up the thread executors
		threadPool = Executors.newCachedThreadPool();
		
		// Read in the arguments
		ArgumentReader loader = ArgumentReader.read(args);
		
		// This may be inefficient, but it's easier to keep track of
		JobTracker mainTracker = new JobTracker();
		mainTracker.startTracking("overall job");
		
		// Make the overall output folder
		Utils.FileSystem.createFolder(loader.myOutput);
		
		// Import in the overall graph
		StringBuilder summary = new StringBuilder();
		Graph<String, String> graph = loader.myGraphLoader.loadGraph();
		summary.append(loader.myGraphLoader.getInformation());
		
		// If the graph was generated, export that graph
		if (loader.myGraphLoader instanceof GeneratedGraph) {
			mainTracker.startTracking("Export Generated Graph");
			BasicGraph.exportGraph(graph, loader.myOutput + HardCode.pDataFix);
			mainTracker.endTracking("Export Generated Graph");
		}

		/*** BEGIN THE Analysis ***/
		
		/** Run the population analysis **/
		// Either run a thread to load the BC, or import in a finished BC import
		String sPopBC = HardCode.pBcPostfix, 
				sPopDegree = HardCode.pDegreePostfix,
				sPopED = HardCode.pEDPostfix;
		if (loader.myPopPath == null) {
			Utils.FileSystem.createFolder(loader.myOutput + HardCode.pDistroFolder);
			sPopBC = loader.myOutput + HardCode.pDistroFolder + sPopBC;
			sPopDegree = loader.myOutput + HardCode.pDistroFolder + sPopDegree;
			sPopED = loader.myOutput + HardCode.pDistroFolder + sPopED;
		} else {
			sPopBC = loader.myPopPath + sPopBC;
			sPopDegree = loader.myPopPath + sPopDegree;
			sPopED = loader.myPopPath + sPopED;
		}
		
		/** Now generate/load the results **/
		ConcurrentHashMap<String, Double> popBC, popDegree, popED;
		// BC
		if ((new File(sPopBC)).exists()) {
			popBC = new ConcurrentHashMap<String, Double>(
					(new BCAnalyzer()).read(sPopBC));
		} else {
			mainTracker.startTracking("Pop BC Calculation");
			popBC = new ConcurrentHashMap<String, Double>(
					(new BCAnalyzer()).analyzeGraph(graph, sPopBC));
			mainTracker.endTracking("Pop BC Calculation");
		}
		// Degree
		if ((new File(sPopDegree)).exists()) {
			popDegree = new ConcurrentHashMap<String, Double>(
					(new DegreeAnalyzer()).read(sPopDegree));
		} else {
			mainTracker.startTracking("Pop Degree Calculation");
			popDegree = new ConcurrentHashMap<String, Double>(
					(new DegreeAnalyzer()).analyzeGraph(graph, sPopDegree));
			mainTracker.endTracking("Pop Degree Calculation");
		}
		// Ego-centric Density
		if ((new File(sPopED)).exists()) {
			popED = new ConcurrentHashMap<String, Double>(
					(new EDAnalyzer()).read(sPopED));
		} else {
			mainTracker.startTracking("Pop Egocentric Density Calculation");
			popED = new ConcurrentHashMap<String, Double>(
					(new EDAnalyzer()).analyzeGraph(graph, sPopED));
			mainTracker.endTracking("Pop Egocentric Density Calculation");
		}
		// Generic Information - checks now to see if already existent
		// TODO: Make this stored for later
		writeBasicInformation(mainTracker, graph, loader.myOutput, "population");

		AnalysisRunner.setPop(popBC, popDegree, popED);

		
		/** Begin the sampling **/
		// Create the folder space for the processes
		String sampleOverallDir = loader.myOutput + HardCode.pSamplesFolder;
		Utils.FileSystem.createFolder(sampleOverallDir);
		
		// The CSV's from the sample section that will be combined with the Population information
		LinkedList<CSV_Builder> results = new LinkedList<CSV_Builder>();

		int replicaID = 0;
		
		// Begin the sampling!
		double[] thresholdArea = {0, 0.5, 1.0};
		for (double threshold : thresholdArea) {
			String tFolder = sampleOverallDir + "/" + "thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			// A total of log2(1/0.0035) iterations
			int[] maxSampleArea = {1, 10, graph.getVertexCount()-1};
			for (int maxSampleAdd : maxSampleArea) {
				// Calculate the percentage of the main graph this is
				double maxSample = (double)(graph.getVertexCount()-1)/(double)maxSampleAdd;
				String mFolder = tFolder + "/max" + HardCode.dcf.format(maxSample * 10000);
				Utils.FileSystem.createFolder(mFolder);
				
				// Create an array to hold the different threads
				ArrayList<Callable<CSV_Builder>> tasks = new ArrayList<Callable<CSV_Builder>>();
				
				// Finally run the analysis
				for (int replica = 0; replica < 5; replica++) {
					// Uniquely name this version and create its folder
					String sampleName = "sample" + replicaID;
					String sampleDir = mFolder + "/" + sampleName;
					Utils.FileSystem.createFolder(sampleDir);
					
					// Create the shell of the sample
					RDBFSSample rndSample = new RDBFSSample(0.00, threshold, maxSampleAdd);
					
					// Run the sample threads
					// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
					tasks.add(new SampleThreadRunner(sampleDir, sampleName, rndSample, graph, replicaID++));
				}

				// Add the results onto the last to be added CSV_Builder
				CSV_Builder cMaxSample = new CSV_Builder(maxSampleAdd);

				// maxEdge(#) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls
				for (Future<CSV_Builder> retVal : threadPool.invokeAll(tasks, loader.myTimeOut, loader.myTimeOutUnit)) {
					cMaxSample.LinkTo(retVal.get());
				}
				
				// cMaxEdge = maxSample(#), // Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
				// input = threshold, maxSample(#), cMaxEdge
				CSV_Builder input = new CSV_Builder(new CSV_Percent(1-threshold), //threshold
											cMaxSample //maxSample (#) --> sample output data
										);
				
				results.add(input);
				tasks.clear();
			}
		}
		
		// Add the sample type and link that to all the inputs
		CSV_Builder sampleType = new CSV_Builder("RDBFSSample");
		
		// Results array: threshold, maxSample(#), // Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Link: sampleType
		for (CSV_Builder builder : results) {
			sampleType.LinkTo(builder);
		}

		// Lastly add the sample type used and the overall graph information
		CSV_Builder mainData = new CSV_Builder(graph.getVertexCount(), // parent node count
				new CSV_Builder(graph.getEdgeCount(), // parent edge count
						sampleType)); // sample method type
		
		// Results array: threshold, maxSample(#), ID, alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Link: sampleType

		// Output the statistics on the correlations
		BufferedWriter csvOutput = Utils.FileSystem.createFile(loader.myOutput + HardCode.pCorrPostfix);
		csvOutput.write("\"Parent Node Count\","
				+ "\"Parent Edge Count\","
				+ "\"Sample Method Type\","
				+ "\"BFS:RND\","
				+ "\"Maximum Sample (#)\","
				+ "\"Replica ID\","
				+ "\"Alpha (%)\","
				+ "\"Sample Node Count\","
				+ "\"Sample Edge Count\","
				+ "\"Iterations\","
				+ "\"Real Alpha\","
				+ "\"Real BFS:RND\","
				+ "\"Sample WCC Count\","
				+ "\"Sample Metric Duration\","
				+ "\"Measure\","
				+ "\"Sample Correlation Alpha (%)\","
				+ "\"Spearmans\","
				+ "\"Pearsons\","
				+ "\"Error\","
				+ "\"KL\","
				+ "\"Population P/R Alpha\","
				+ "\"Sample P/R Alpha\","
				+ "\"Precision\","
				+ "Recall"
			);
		csvOutput.newLine();
		mainData.writeCSV(csvOutput);
		csvOutput.close();
		
		// End the tracking over the entire job
		mainTracker.endTracking("overall job");
		
		// Write out the respective job times
		BufferedWriter jobOutput = Utils.FileSystem.createFile(loader.myOutput + HardCode.pSummaryPostfix);
		mainTracker.writeJobTimes(jobOutput);
		jobOutput.close();
	}
}
