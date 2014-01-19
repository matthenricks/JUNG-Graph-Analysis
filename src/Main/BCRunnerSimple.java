package Main;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import SamplingAlgorithms.SampleMethod;
import Utils.ArgumentReader;
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
	 */
	public static void writeBasicInformation(JobTracker tracker, Graph<String, String> graph, String outputDir, String jobName) {
		// Output some defining graph metrics
		tracker.startTracking("basic writing of " + jobName);
		try {
			BufferedWriter bw = Utils.FileSystem.createFile(outputDir + pMetricPostfix);
			bw.write("The number of edges are: " + graph.getEdgeCount());
			bw.newLine();
			bw.write("The number of indices are: " + graph.getVertexCount());
			bw.newLine();
			bw.close();
		} catch (IOException e1) {
			System.err.println("Error in the Basic Writing of basic information: " + jobName);
			e1.printStackTrace();
			return;
		}
		tracker.endTracking("basic writing of " + jobName);
		// Get the WCC of this graph so that we better understand it's density
		tracker.startTracking("WCC calculation of " + jobName);
		try {
			WCCSizeAnalysis.analyzeBasicClusterInformation(graph, outputDir + pWccPostfix);
		} catch (IOException e) {
			System.err.println("Error in the WCC of basic information: " + jobName);
			e.printStackTrace();
		}
		tracker.endTracking("WCC calculation of " + jobName);
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
	
	public static class SampleThreadRunner implements Callable<double[]> {
		
		private String sampleDir, sampleName;
		private Graph<String, String> parentGraph;
		private SampleMethod sampleMethod;
		private int compNumber;
		
		/**
		 * Constructor for the sample runner thread. This involves the thread management of sampling, analysis, and correlation.
		 * @param sampleDir - directory of the sample to save it's data and files
		 * @param sampleName - the unique name of the sample
		 * @param compNumber - the amount of nodes the same is going to take (TODO: could make this a class)
		 * @param sampleMethod - the method of sampling used to obtain the sample
		 * @param parentGraph - the parent graph to be sampled
		 * @param executorService - the executor (this isn't necessarily needed)
		 */
		public SampleThreadRunner(String sampleDir, String sampleName, int compNumber, SampleMethod sampleMethod, 
				Graph<String, String> parentGraph) {			
			this.sampleDir = sampleDir;
			this.sampleName = sampleName;
			this.sampleMethod = sampleMethod;
			this.parentGraph = parentGraph;
			this.compNumber = compNumber;
			
			if (compNumber > parentGraph.getVertexCount())
				throw new Error("Sampled nodes cannot be greater than the population's vertexes");
		}

		@Override
		public double[] call() throws Exception {
			// Record it's own job stuff in here!
			JobTracker sampleTracker = new JobTracker();
			// First we need to actually generate the sample graph
			sampleTracker.startTracking("Initial sampling of " + sampleName);
			Graph<String, String> sample = sampleMethod.sampleGraph(parentGraph);
			sampleTracker.endTracking("Initial sampling of " + sampleName);
			
			/** Now begin the analysis **/
			// Write out the  write out the basic information pertaining to the graph
			BCRunnerSimple.writeBasicInformation(sampleTracker, sample, sampleDir, sampleName);
			
			sampleTracker.startTracking("BC Calculation of " + sampleName);
			HashMap<String, Double> sampleBC = BCAnalyzer.analyzeGraphBC(sample, sampleDir + pBcPostfix);
			sampleTracker.endTracking("BC Calculation of " + sampleName);
			
			try {
				// Output the graph in the down-time
				BasicGraph.exportGraph(sample, sampleDir + pSampleData);
				
				/** Now begin the comparison **/
				// Pull the population BC values
				ConcurrentHashMap<String, Double> popBC = pullBCPop();
				sampleTracker.startTracking("Correlate Sample: " + sampleName);
				double[] result = BCCorrelator.runBCComparison(
						popBC,
						sampleBC,
						compNumber < sampleBC.size() ? compNumber : sampleBC.size());
				sampleTracker.endTracking("Correlate Sample: " + sampleName);
				
				// Write out the respective job times
				BufferedWriter jobOutput = Utils.FileSystem.createFile(sampleDir + pSummaryPostfix);
				sampleTracker.writeJobTimes(jobOutput);
				jobOutput.close();
				
				// Return the result!
				return result;
				
			} catch (Exception e) {
				System.err.println("An error occured in sample: " + sampleName);
				e.printStackTrace();
				stopAllThreads();
			}
			
			return null;
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
		if (loader.myPopBCPath == null) {
			Callable<ConcurrentHashMap<String, Double>> popThread = new BCAnalyzer.CallableGraphBC(graph, loader.myOutput + pBcPostfix, mainTracker, "Main BC Calculation");
			threadPool.execute(new PopulationThread(popThread, loader.myTimeOut, loader.myTimeOutUnit));

			// Write out its basic information
			writeBasicInformation(mainTracker, graph, loader.myOutput, "population");
		} else {
			setBCPop(BCAnalyzer.readGraphBCConcurr(loader.myPopBCPath));
		}
		
		/** Begin the sampling **/
		// Create the folder space for the processes
		String sampleOverallDir = loader.myOutput + pSamplesFolder;
		Utils.FileSystem.createFolder(sampleOverallDir);
			
		// Hacked together class
		class inputData {
			double threshold;
			double alpha;
			int replicationNumber;
			double maxSize;
			public inputData(double t, double a, double maxSize, int replicationNumber) {
				threshold = t;
				alpha = a;
				this.maxSize = maxSize;
				this.replicationNumber = replicationNumber;
			}
			public String toString() {
				return "RSN" + 
						HardCode.dcf.format(threshold*10000) + "-" + 
						HardCode.dcf.format(alpha*10000) + "-" + 
						HardCode.dcf.format(maxSize*10000) + "-" + 
						replicationNumber;
			}
		};
		
		
		// Get the amount of nodes in the sample to correlate (on max)
		int corrNumber = (int)Math.floor(graph.getVertexCount() * loader.myCorrelationPercent);
		if (corrNumber < 1) corrNumber = 1;		
		
		int totalIterations = 6*9*4*3;
		int inputOn = 0;
		ArrayList<inputData> inputs = new ArrayList<inputData>(totalIterations);
		ArrayList<double[]> results = new ArrayList<double[]>(totalIterations);
		
		// Begin the sampling!
		for (double threshold = 0; threshold <= 1.0; threshold += 0.2) {
			String tFolder = sampleOverallDir + "/" + "thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			// A total of log2(1/0.0035) iterations
			for (double alpha = 0.0035; alpha < 1.0; alpha = alpha * 2) {
				String aFolder = tFolder + "/" + "alpha" + HardCode.dcf.format(alpha * 10000);
				Utils.FileSystem.createFolder(aFolder);
				// Now set the max sample
				// TODO: Make this boundless
				for (double maxSample = 0.2; maxSample <= 1.4; maxSample += 0.4) {
					// Easy way of doing a different version of max
					int maxEdgeAdd = (maxSample == 1.4) ? Integer.MAX_VALUE : Math.max((int)Math.ceil(maxSample/alpha) - 1, 1);
					String mFolder = aFolder + "/max" + HardCode.dcf.format(maxSample * 10000);
					Utils.FileSystem.createFolder(mFolder);
					
					// Finally run the analysis
					ArrayList<Callable<double[]>> tasks = new ArrayList<Callable<double[]>>();
					// TODO: This is currently how it's threaded... This could be improved with a function
					for (int replica = 0; replica < 3; replica++) {
						// Hacked together to store the variables
						inputData input = new inputData(threshold, alpha, maxSample, replica);
						// Uniquely name this version and create its folder
						String sampleName = input.toString();
						String sampleDir = mFolder + "/" + sampleName;
						Utils.FileSystem.createFolder(sampleDir);
						
						// Select the sampling method
						RDBFSSample rndSample = new RDBFSSample(alpha, threshold, maxEdgeAdd);
						// Run the sample threads
						inputs.set(inputOn + replica, input);
						tasks.add(new SampleThreadRunner(sampleDir, sampleName, corrNumber, rndSample, graph));
					}
					for (Future<double[]> retVal : threadPool.invokeAll(tasks, loader.myTimeOut, loader.myTimeOutUnit)) {
						results.set(inputOn++, retVal.get());
					}
					tasks.clear();
				}
			}
		}
		
		// Output the statistics on the correlations
		BufferedWriter metricOutput = Utils.FileSystem.createFile(loader.myOutput + pCorrPostfix);
		metricOutput.write("\"ReplicationID\",\"Edge-Percentage\",\"Threshold\",\"Alpha\",\"Spearmans Correlation\", \"Pearsons Correlation\", Average Error");
		metricOutput.newLine();
		for (inputOn = 0; inputOn < results.size(); inputOn++) {
			inputData key = inputs.get(inputOn);
			if (key == null)
				break;
			// Write in the input
			metricOutput.append(key.replicationNumber + "," + HardCode.dcf3.format(key.maxSize) + "," + HardCode.dcf3.format(key.threshold) + "," + HardCode.dcf3.format(key.alpha) + ",");
			
			double[] corr = results.get(inputOn);
			// Write in the output
			metricOutput.append(HardCode.dcf3.format(corr[0]) + "," + 
					HardCode.dcf3.format(corr[1]) +
					HardCode.dcf3.format(corr[2]));
			metricOutput.newLine();
		}
		metricOutput.close();
		
		// End the tracking over the entire job
		mainTracker.endTracking("overall job");
		
		// Write out the respective job times
		BufferedWriter jobOutput = Utils.FileSystem.createFile(loader.myOutput + pSummaryPostfix);
		mainTracker.writeJobTimes(jobOutput);
		jobOutput.close();
	}
}
