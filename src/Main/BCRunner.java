package Main;

import java.io.BufferedWriter;
import java.io.IOException;
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

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import Correlation.BCCorrelator;
import GraphAnalyzers.BCAnalyzer;
import GraphAnalyzers.WCCSizeAnalysis;
import GraphCreation.BarabasiAlbertGraphGenerator;
import GraphCreation.BasicGraph;
import GraphCreation.ErdosRenyiGraphGenerator;
import GraphCreation.FriendsTwitterDataImporter;
import GraphCreation.GraphLoader;
import SamplingAlgorithms.RandomBFSSample;
import SamplingAlgorithms.SampleMethod;
import Utils.ArgumentReader;
import Utils.HardCode;
import Utils.JobTracker;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

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
public class BCRunner {
	
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
	

	/**
	 * Series of executors used to better orient thread priority
	 */
	static ExecutorService highPriorityThread, midPriorityThread, minPriorityThread;
	// Placing the below code in the actual main function
//	static {
//		highPriorityThread = Executors.newCachedThreadPool(new Utils.HardCode.PriorityThreadFactory(Thread.MAX_PRIORITY));
//		midPriorityThread = Executors.newCachedThreadPool(new Utils.HardCode.PriorityThreadFactory(Thread.NORM_PRIORITY));
//		minPriorityThread = Executors.newCachedThreadPool(new Utils.HardCode.PriorityThreadFactory(Thread.MIN_PRIORITY));
//	}
	/**
	 * Convenience function to stop all the executor services if there's an error
	 */
	public static void stopAllThreads() {
		highPriorityThread.shutdownNow();
		midPriorityThread.shutdownNow();
		minPriorityThread.shutdownNow();
	}
	
	/**
	 * Thread to aid in running the basic metrics for a graph
	 */
	private static class BasicInformationRunnable implements Runnable {
		
		JobTracker mainTracker;
		Graph<String, String> graph;
		String outputDir;
		String jobName;
		public BasicInformationRunnable(JobTracker tracker, Graph<String, String> graph, String outputDir, String jobName) {
			this.mainTracker = tracker;
			this.graph = graph;
			this.outputDir = outputDir;
			this.jobName = jobName;
		}
		
		@Override
		public void run() {
			// Output some defining graph metrics
			mainTracker.startTracking("basic writing of " + jobName);
			try {
				writeBasicGraphInformation(graph, outputDir + pMetricPostfix);
			} catch (IOException e1) {
				System.err.println("Error in the Basic Writing of basic information thread: " + jobName);
				e1.printStackTrace();
				return;
			}
			mainTracker.endTracking("basic writing of " + jobName);
			// Get the WCC of this graph so that we better understand it's density
			mainTracker.startTracking("WCC calculation of " + jobName);
			try {
				WCCSizeAnalysis.analyzeBasicClusterInformation(graph, outputDir + pWccPostfix);
			} catch (IOException e) {
				System.err.println("Error in the WCC of basic information thread: " + jobName);
				e.printStackTrace();
			}
			mainTracker.endTracking("WCC calculation of " + jobName);
		}
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
			Future<ConcurrentHashMap<String, Double>> future = highPriorityThread.submit(myThread);
			while (!future.isDone()) {
				if (!future.isCancelled()) {
					Thread.yield();
					continue;
				} else {
					stopAllThreads();
				}
			}
			
			try {
				setBCPop(future.get(myTimeout, myUnit));
			} catch (TimeoutException e) {
				System.out.println("TIMEOUT ERROR IN POPULATION THREAD");
				stopAllThreads();
				e.printStackTrace();
			} catch (Exception e) {
				stopAllThreads();
				e.printStackTrace();
			} 
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
			// Run a thread to write out basic information pertaining to the graph
			minPriorityThread.execute(new BasicInformationRunnable(sampleTracker, sample, sampleDir, sampleName));
			// Run a thread to do the BC analysis
			Callable<HashMap<String, Double>> sampleBCRunner = new BCAnalyzer.CallableGraphHashBC(sample, sampleDir + pBcPostfix, sampleTracker, "BC of " + sampleName);
			Future<HashMap<String, Double>> sampleBCFuture = midPriorityThread.submit(sampleBCRunner);

			try {
				// Output the graph in the down-time
				BasicGraph.exportGraph(sample, sampleDir + pSampleData);
				
				// Wait until the BC is done
				// TODO: Make this more efficient. Unsure at this moment how threading works specifically
				while (sampleBCFuture.isDone() == false) {
					Thread.yield();
				}
				
				/** Now begin the comparison **/
				// Pull the population BC values
				ConcurrentHashMap<String, Double> popBC = pullBCPop();
				HashMap<String, Double> sampleBC = sampleBCFuture.get();
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
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ExecutionException, TimeoutException {
		
		// Set up the thread executors
		highPriorityThread = Executors.newCachedThreadPool(new Utils.HardCode.PriorityThreadFactory(Thread.MAX_PRIORITY));
		midPriorityThread = Executors.newCachedThreadPool(new Utils.HardCode.PriorityThreadFactory(Thread.NORM_PRIORITY));
		minPriorityThread = Executors.newCachedThreadPool(new Utils.HardCode.PriorityThreadFactory(Thread.MIN_PRIORITY));
		
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
		
		/*** BEGIN THE CONCURRENCY ***/
		
		// Run a thread to get the population BC
		// This manages it being posted for the other threads to pull
		Callable<ConcurrentHashMap<String, Double>> popThread = new BCAnalyzer.CallableGraphBC(graph, loader.myOutput + pBcPostfix, mainTracker, "Main BC Calculation");
		highPriorityThread.execute(new PopulationThread(popThread, loader.myTimeOut, loader.myTimeOutUnit));
		
		// Run a thread to write out basic information pertaining to the graph
		minPriorityThread.execute(new BasicInformationRunnable(mainTracker, graph, loader.myOutput, "population"));
		
		/** Begin the sampling **/
		// Create the folder space for the processes
		String sampleOverallDir = loader.myOutput + pSamplesFolder;
		Utils.FileSystem.createFolder(sampleOverallDir);
			
		// Hacked together class
		class inputData {
			double threshold;
			double alpha;
			int replicationNumber;
			public inputData(double t, double a, int replicationNumber) {
				threshold = t;
				alpha = a;
				this.replicationNumber = replicationNumber;
			}
			public String toString() {
				return "RSN" + HardCode.dcf.format(threshold*10000) + "-" + HardCode.dcf.format(alpha*10000) + "-" + replicationNumber;
			}
		};
		
		// Map to correlate the names of the samples to their respective outputs
		HashMap<inputData, Future<double[]>> sampleOutputs = new HashMap<inputData, Future<double[]>>(10);
		
		// Begin the sampling!
		for (double threshold = 0; threshold <= 1.0; threshold += 0.2) {
			for (double alpha = 0.0035; alpha < 1.0; alpha = alpha * 2) {
				for (int replica = 0; replica < 5; replica++) {
					// Hacked together to store the variables
					inputData input = new inputData(threshold, alpha, replica);
					// Uniquely name this version and create its folder
					String sampleName = input.toString();
					String sampleDir = sampleOverallDir + "/" + sampleName;
					Utils.FileSystem.createFolder(sampleDir);
					
					// Select the sampling method
					RandomBFSSample rndSample = new RandomBFSSample(alpha, threshold);
					// Run the sample threads
					sampleOutputs.put(
							input,
							midPriorityThread.submit(new SampleThreadRunner(sampleDir, sampleName, loader.myCorrelationSample, rndSample, graph))
							);
				}
			}
			break;
		}
		
		// Wait for everything to finish, getting all the correlation values
		HashMap<inputData, double[]> correlations = new HashMap<inputData, double[]>(100);
		try {
			for (inputData input : sampleOutputs.keySet()) {
				correlations.put(input, sampleOutputs.get(input).get(loader.myTimeOut, loader.myTimeOutUnit));
			}
			highPriorityThread.shutdown();
			// This should already be finished
			highPriorityThread.awaitTermination(30, TimeUnit.SECONDS);
			
			midPriorityThread.shutdown();
			midPriorityThread.awaitTermination(loader.myTimeOut, loader.myTimeOutUnit);
		} catch (InterruptedException e) {
			e.printStackTrace();
			stopAllThreads();
			return;
		}
		
		// Output the statistics on the correlations
		BufferedWriter metricOutput = Utils.FileSystem.createFile(loader.myOutput + pCorrPostfix);
		metricOutput.write("\"ReplicationID\",\"Threshold\",\"Alpha\",\"Spearmans Correlation\", \"Pearsons Correlation\", Average Error");
		metricOutput.newLine();
		for (inputData key : correlations.keySet()) {
			// Write in the input
			metricOutput.append(key.replicationNumber + "," + HardCode.dcf3.format(key.threshold) + "," + HardCode.dcf3.format(key.alpha) + ",");
			// Write in the output
			metricOutput.append(HardCode.dcf3.format(correlations.get(key)[0]) + "," + 
					HardCode.dcf3.format(correlations.get(key)[1]) +
					HardCode.dcf3.format(correlations.get(key)[2]));
			metricOutput.newLine();
		}
		metricOutput.close();
		
		// End the tracking over the entire job
		mainTracker.endTracking("overall job");
		
		// Write out the respective job times
		BufferedWriter jobOutput = Utils.FileSystem.createFile(loader.myOutput + pSummaryPostfix);
		mainTracker.writeJobTimes(jobOutput);
		jobOutput.close();
		
		// Make sure the basic threads are closed
		minPriorityThread.shutdown();
		try {
			// They shouldn't take longer then the rest
			minPriorityThread.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			System.err.print("Some of the unimportant threads didn't finish...\n");
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	/******** Additional Information Writers. NONE OF THEM ARE USED ********/

	/**
	 * Writes out basic information about a graph
	 * @param gLocation - the location of the graph being presented
	 * @param graph
	 * @param bw - the writer that will output this information
	 * @throws IOException 
	 */
	public static void writeBasicGraphInformation(Graph<String, String> graph, BufferedWriter bw) throws IOException {
		bw.write("The number of edges are: " + graph.getEdgeCount());
		bw.newLine();
		bw.write("The number of indices are: " + graph.getVertexCount());
		bw.newLine();
	}
	
	/**
	 * Writes out basic information about a graph
	 * @param gLocation - the location of the graph being processed
	 * @param graph - the graph location
	 * @param path - the output location of the information
	 * @throws IOException 
	 */
	public static void writeBasicGraphInformation(Graph<String, String> graph, String path) throws IOException {
		BufferedWriter bw = Utils.FileSystem.createFile(path);
		writeBasicGraphInformation(graph, bw);
		bw.close();
	}
	
	/**
	 * Writes out a basic summary of statistics on the observation list
	 * @param obs - the list of observations
	 * @param summary - the writer that the output will be sent
	 * @param pop - if the observations are sample or population based
	 * @throws IOException
	 */
	public static void summaryofStatistics(double[] obs, BufferedWriter summary, boolean pop) throws IOException { 	
		SummaryStatistics sms = new SummaryStatistics();
		for (double value : obs) {
			sms.addValue(value);  			
		}
		
		if (pop) 
			summary.write("population");
		else
			summary.write("sample");
		
		summary.write("mean : " + HardCode.dcf3.format(sms.getMean()) + " std error : " + HardCode.dcf3.format(sms.getStandardDeviation()));
		summary.newLine(); 
	}
	
	/**
	 * Writes out a basic summary of statistics on the observation list
	 * @param obs - the list of observations
	 * @param path - the path of the output
	 * @param pop - if the observations are sample or population based
	 * @throws IOException
	 */
	public static void summaryofStatistics(double[] obs, String path, boolean pop) throws IOException {
		BufferedWriter bw = Utils.FileSystem.createFile(path);
		summaryofStatistics(obs, bw, pop);
		bw.close();
	}
}
