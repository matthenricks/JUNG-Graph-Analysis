package Main;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import Correlation.BCCorrelator;
import GraphAnalyzers.BCAnalyzer;
import GraphAnalyzers.WCCSizeAnalysis;
import GraphCreation.BasicGraph;
import GraphCreation.GeneratedGraph;
import SamplingAlgorithms.RDBFSSample;
import Utils.ArgumentReader;
import Utils.CSV_Builder;
import Utils.CSV_Builder_Objects.CSV_Percent;
import Utils.CSV_Builder_Objects.CSV_Double;
import Utils.HardCode;
import Utils.JobTracker;
import edu.uci.ics.jung.graph.Graph;

/**** java -Xms512m -Xmx3G -jar crawler.jar ****/
// TODO: Runtime.availableProcessors -> get the available hardware threads
// Use this to increase the heap size for the shared space of the threads

/**
 * The objective of this class is to cleanly run the project. This will involve these series of steps:
 * 	Graph Creation (1)
 * 	Calculation of Real BC (1.1)
 * 	Sampling of Graph (1.2)
 * 	Calculation of Sample BC (1.2.1)
 * 	Accuracy Analysis (1.2.2)
 *  Plotting (1.2.3) -- TODO: UNDONE
 * As noted above, BC calculation is separate from all other types of calculation and splitting since it takes so long
 * @author MOREPOWER
 *
 */
public class BCRunnerSimple {
	
	static String pSummaryPostfix = "/summary.txt";
	static String pCorrPostfix = "/analysis.csv";
	static String pMetricPostfix = "/metrics.txt";
	static String pWccPostfix = "/WCC.csv";
	static String pBcPostfix = "/bc.csv";
	static String pSamplesFolder = "/samples/";
	static String pSampleData = "/data.dat";
	// Implemented within the function. #{name} references a variable
	// String pSampleFolder = "/#{sampleName}/"
	// String pSampleData = "/#{sampleName}.dat"
	

	static ExecutorService threadPool;
	
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
		
		BufferedWriter bw = Utils.FileSystem.createFile(outputDir + pMetricPostfix);
		bw.write("The number of edges are: " + graph.getEdgeCount());
		bw.newLine();
		bw.write("The number of indices are: " + graph.getVertexCount());
		bw.newLine();
		bw.close();
		tracker.endTracking("basic writing of " + jobName);
		
		// Get the WCC of this graph so that we better understand it's density
		tracker.startTracking("WCC calculation of " + jobName);
		Integer WCC = WCCSizeAnalysis.analyzeBasicClusterInformation(graph, outputDir + pWccPostfix);
		tracker.endTracking("WCC calculation of " + jobName);
		return new CSV_Builder(WCC);
	}
	
	/** Data: Population -> Split -> Samples
	 ** Analysis: Population -> BC -> postBCPOP
	 ** Analysis: Sample -> BC of Sample -> pullBCPOP -> analysis sample vs population -> correlation
	 ** Analysis: Population -> Basic Stats Output
	 ** Analysis: Sample -> Basic Stats Output
	 */
	static ConcurrentHashMap<String, Double> populationBC;
	private static Lock popBCSetLock = new ReentrantLock();
	private static Condition popBCWait = popBCSetLock.newCondition();
	
	/**
	 * A class for the thread that handles the job of the population
	 * In the end, this will post it's BC value for the samples to utilize
	 * @author MOREPOWER
	 *
	 */
	public static class PopulationThread implements Runnable {

		Callable<ConcurrentHashMap<String, Double>> myThread;
		int myTimeout;
		TimeUnit myUnit;
		
		public PopulationThread(Callable<ConcurrentHashMap<String, Double>> thread, int timeout, TimeUnit unit) {
			myThread = thread;
			myTimeout = timeout;
			myUnit = unit;
		}
		
		@Override
		public void run() {
			ConcurrentHashMap<String, Double> popMap = null;
			try {
				popMap = myThread.call();
			} catch (Exception e) {
				System.err.println("Error in population BC calculation");
				e.printStackTrace();
				threadPool.shutdown();
				return;
			}
			setBCPop(popMap);
		}
		
	}
	public static void setBCPop(ConcurrentHashMap<String, Double> popBC) {
		popBCSetLock.lock();
		System.out.println("Population BC Values Posted");
		populationBC = popBC;
		popBCWait.signalAll();
		popBCSetLock.unlock();
	}
	public static ConcurrentHashMap<String, Double> pullBCPop() throws InterruptedException {
		// Wait until the populationBC has been written to
		popBCSetLock.lock();
		while(populationBC == null) {
			System.out.println("Sample thread waiting for BC");
			popBCWait.await();
		}
		popBCSetLock.unlock();
		System.out.println("Population BC Values Pulled");
		
		return populationBC;
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
		// ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls
		public CSV_Builder call() throws Exception {
			// Use the ID to consolidate everything together
			CSV_Builder cID = new CSV_Builder(this.ID);
			
			// Make this entire thing an alpha loop
			for (double alpha = 0.01; alpha <= 1.0; alpha += 0.05) {
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

				
				String aFolder = sampleDir + "/" + "alpha" + HardCode.dcf.format(alpha * 10000);
				Utils.FileSystem.createFolder(aFolder);
				
				/** Now begin the analysis **/
				// Write out the  write out the basic information pertaining to the graph
				CSV_Builder cWCC = BCRunnerSimple.writeBasicInformation(sampleTracker, sample, aFolder, sampleName);
				
				sampleTracker.startTracking("BC Calculation of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
				HashMap<String, Double> sampleBC = BCAnalyzer.analyzeGraphBC(sample, aFolder + pBcPostfix);
				sampleTracker.endTracking("BC Calculation of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
				
				try {
					// Output the graph in the down-time
					BasicGraph.exportGraph(sample, aFolder + pSampleData);
					
					/** Now begin the comparison **/ 
					// Pull the population BC values
					ConcurrentHashMap<String, Double> popBC = pullBCPop();
					
					// Generate the BC Builder to link
					CSV_Builder cBCDur = new CSV_Builder(sampleTracker.getJobTime("BC Calculation of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000)));
					
					// sample nodes, sample edges, WCC, [BC Duration, corrSize(#), corrSize(%), Spearmans, Pearsons, Error, Kendalls]
					for (double corrSize = 0.1; corrSize < 1.0; corrSize += 0.1) {
						int compNumber = (int)Math.floor(parentGraph.getVertexCount() * corrSize);
						if (compNumber < 1) compNumber = 1;
						
						sampleTracker.startTracking("Correlate Sample: " + corrSize + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
						double[] result = BCCorrelator.runBCComparison(
								popBC,
								sampleBC,
								compNumber < sampleBC.size() ? compNumber : sampleBC.size());
						sampleTracker.endTracking("Correlate Sample: " + corrSize + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
	
						CSV_Builder cCorrSize = new CSV_Builder(compNumber, // Added corrSize(#)
								new CSV_Builder(new CSV_Double(corrSize), // Added corrSize(%)
									new CSV_Builder(new CSV_Double(result[0]), // Added spearmans
										new CSV_Builder(new CSV_Double(result[1]), // Added pearsons
												new CSV_Builder(new CSV_Double(result[2]), // Added error
														new CSV_Builder(new CSV_Double(result[3]), // Added Kendall
																new CSV_Builder(new CSV_Percent(result[4])))))))); // Added Accuracy
	
						// Link the corrSize to the BCDuration
						cBCDur.LinkTo(cCorrSize);
					}
					
					// Write out the respective job times
					BufferedWriter jobOutput = Utils.FileSystem.createFile(aFolder + pSummaryPostfix);
					sampleTracker.writeJobTimes(jobOutput);
					jobOutput.close();
					
					/** Return all the pertinent information by using the CSV Builders **/
					// sample nodes, sample edges, WCC, <-- [BC Duration, corrSize(#), corrSize(%), Spearmans, Pearsons, Error, Kendalls]
					// Link the last values and return them to maintain the above order. --signifies already linked
					cWCC.LinkTo(cBCDur);
					
					// Link: Iterations, Real Alpha, Real Threshold, <-- WCC...
					cSamplingStats.LinkToEnd(cWCC);
					
					// Link: ID <-- alpha(%),  parentAlpha, sample vert, sample edge <-- Iterations, real alpha, real threshold...
					cID.LinkTo(new CSV_Builder(new CSV_Percent(alpha), 
							new CSV_Builder(sample.getVertexCount(),
							new CSV_Builder(sample.getEdgeCount(),
									cSamplingStats))));
					
				} catch (Exception e) {
					System.err.println("An error occured in sample: " + sampleName);
					e.printStackTrace();
					stopAllThreads();
				}
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
				
		/*** BEGIN THE CONCURRENCY ***/		
		// Either run a thread to load the BC, or import in a finished BC import
		if (loader.myPopPath == null) {
			Callable<ConcurrentHashMap<String, Double>> popThread = new BCAnalyzer.CallableGraphBC(graph, loader.myOutput + pBcPostfix, mainTracker, "Main BC Calculation");
			threadPool.execute(new PopulationThread(popThread, loader.myTimeOut, loader.myTimeOutUnit));

			// Write out its basic information
			writeBasicInformation(mainTracker, graph, loader.myOutput, "population");
		} else {
			setBCPop(BCAnalyzer.readGraphBCConcurr(loader.myPopPath + "/" + HardCode.pBcPostfix));
		}
		
		/** Begin the sampling **/
		// Create the folder space for the processes
		String sampleOverallDir = loader.myOutput + pSamplesFolder;
		Utils.FileSystem.createFolder(sampleOverallDir);
			
		
		// Input: threshold, maxSample(%), maxSample(#)
		// Output: alpha (%), sample nodes, sample edges, WCC, BC Duration--[corrSize(#), corrSize(%), Spearmans, Pearsons, Error, Kendalls]
		LinkedList<CSV_Builder> results = new LinkedList<CSV_Builder>();
				
		
		Integer sampleNumber = new Integer(1);
		
		// Begin the sampling!
		double[] thresholdValues = {0, 1};
		for (double threshold : thresholdValues) {
		// for (double threshold = 0; threshold <= 1.0; threshold += 0.2) {
			String tFolder = sampleOverallDir + "/" + "thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			// A total of log2(1/0.0035) iterations
			
			double[] maxSampleValues = {0.0001, 0.005, 1.0};
			for (double maxSample : maxSampleValues) {
			// for (double maxSample = 0.0075; maxSample <= 1.0; maxSample *= 2) {
				// Easy way of doing a different version of max
				int maxEdgeAdd = (int)(Math.max((int)Math.ceil((double)graph.getVertexCount() * maxSample), 1));
				String mFolder = tFolder + "/max" + HardCode.dcf.format(maxSample * 10000);
				Utils.FileSystem.createFolder(mFolder);
				
				// Create an array to hold the different threads
				ArrayList<Callable<CSV_Builder>> tasks = new ArrayList<Callable<CSV_Builder>>();
				
				// Finally run the analysis
				for (int replica = 0; replica < 5; replica++) {
					// Uniquely name this version and create its folder
					String sampleName = "sample" + sampleNumber;
					String sampleDir = mFolder + "/" + sampleName;
					Utils.FileSystem.createFolder(sampleDir);
					
					// Create the shell of the sample
					RDBFSSample rndSample = new RDBFSSample(0.00, threshold, maxEdgeAdd);
					
					// Run the sample threads
					// ID, alpha (%),  sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls
					tasks.add(new SampleThreadRunner(sampleDir, sampleName, rndSample, graph, sampleNumber++));
				}

				// Add the results onto the last to be added CSV_Builder
				CSV_Builder cMaxEdge = new CSV_Builder(maxEdgeAdd);

				// maxEdge(#) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls
				for (Future<CSV_Builder> retVal : threadPool.invokeAll(tasks, loader.myTimeOut, loader.myTimeOutUnit)) {
					cMaxEdge.LinkTo(retVal.get());
				}
				
				// cMaxEdge = maxSample(#), replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls
				// input = threshold, maxSample(%), cMaxEdge
				CSV_Builder input = new CSV_Builder(new CSV_Percent(threshold), //threshold
											new CSV_Builder(new CSV_Percent(maxSample), //maxSample (%)
													cMaxEdge //maxSample (#) --> sample output data
											));
				
				results.add(input);
				tasks.clear();
			}
		}
		
		// Add the sample type and link that to all the inputs
		CSV_Builder sampleType = new CSV_Builder("RDBFSSample");
		
		// Results array: threshold, maxSample(%), maxSample(#), replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls
		// Link: sampleType, threshold...
		for (CSV_Builder builder : results) {
			sampleType.LinkTo(builder);
		}
		// Lastly add the sample type used and the overall graph information
		CSV_Builder mainData = new CSV_Builder(graph.getVertexCount(), // parent node count
				new CSV_Builder(graph.getEdgeCount(), // parent edge count
						sampleType)); // sample method type
		
		// Output the statistics on the correlations
		BufferedWriter csvOutput = Utils.FileSystem.createFile(loader.myOutput + pCorrPostfix);
		csvOutput.write("\"Parent Node Count\","
				+ "\"Parent Edge Count\","
				+ "\"Sample Method Type\","
				+ "\"Threshold\","
				+ "\"Maximum Sample (%)\","
				+ "\"Maximum Sample (#)\","
				+ "\"Replica ID\","
				+ "\"Alpha (%)\","
				+ "\"Sample Node Count\","
				+ "\"Sample Edge Count\","
				+ "\"Iterations\","
				+ "\"Real Alpha\","
				+ "\"Real Threshold\","
				+ "\"Sample WCC Count\","
				+ "\"Sample BC Duration\","
				+ "\"Correlation Sample (#)\","
				+ "\"Correlation Sample (%)\","
				+ "\"Spearmans Correlation\","
				+ "\"Pearsons Correlation\","
				+ "\"Average Error\","
				+ "\"Kendalls Distance\","
				+ "Accuracy Metric");
		csvOutput.newLine();
		mainData.writeCSV(csvOutput);
		csvOutput.close();
		
		// End the tracking over the entire job
		mainTracker.endTracking("overall job");
		
		// Write out the respective job times
		BufferedWriter jobOutput = Utils.FileSystem.createFile(loader.myOutput + pSummaryPostfix);
		mainTracker.writeJobTimes(jobOutput);
		jobOutput.close();
		
		System.out.println("DONE");
	}
}
