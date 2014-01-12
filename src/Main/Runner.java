package Main;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import Correlation.BCCorrelator;
import GraphAnalyzers.PopulationBC;
import GraphAnalyzers.SampleBC;
import GraphAnalyzers.WCCSizeAnalysis;
import GraphCreation.BarabasiAlbertGraphGenerator;
import GraphCreation.BasicGraph;
import GraphCreation.ErdosRenyiGraphGenerator;
import GraphCreation.FriendsTwitterDataImporter;
import GraphCreation.GraphLoader;
import SamplingAlgorithms.RandomSampleNode;
import SamplingAlgorithms.SampleMethod;
import Utils.JobTracker;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**** java -Xms512m -Xmx3G -jar crawler.jar ****/
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
public class Runner {
	
	// Generic format we've been using for doubles
	static DecimalFormat dcf = new DecimalFormat("0.000");
	
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
	 * Runs through the args and attempts to load a graph. The potential values are listed below.
	 * Additionally, a summary is returned through the StringBuilder
	 *    --load
	 *      twitterImport
	 *         <path> <edgeType>
	 *      genericImport
	 *         <path>
	 *      erdosRenyi
	 *         <probability: double> <#nodes: int>
	 *      barabasi
	 *         <initial vertexes> <added edges per steps> <steps> <edgeType>
	 * @param args
	 * @param jt
	 * @return the generated graph
	 * @throws IOException
	 * @throws Error - whenever parameters don't match any of the shown paths
	 */
	public static Graph<String, String> importGraph(StringBuilder retVal, String[] args, JobTracker jt) throws IOException, Error {
		// Look in the arguments for the import type and it's respective variables
		GraphLoader gL = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("--load")) {
				if (args[++i].equalsIgnoreCase("twitterImport")) {
					try {
						String dLoc = args[++i];
						if (args[++i].equalsIgnoreCase("directed")) {
							gL = new FriendsTwitterDataImporter(dLoc, EdgeType.DIRECTED);
						} else if (args[i].equalsIgnoreCase("undirected")) {
							gL = new FriendsTwitterDataImporter(dLoc, EdgeType.UNDIRECTED);
						} else {
							throw new Error("Edge type unhandled: " + args[i]);
						}
						retVal.append("The location of the twitter graph is at: " + dLoc + "\n");
					} catch (Error e) {
						System.err.println("Error in successive variables after twitterImport");
						e.printStackTrace();
					}
				} else if (args[i].equalsIgnoreCase("genericImport")) {
					try {
						String dLoc = args[++i];
						gL = new BasicGraph(dLoc);
						retVal.append("The location of the generic graph is at: " + dLoc + "\n");
					} catch (Error e) {
						System.err.println("Error in successive variables after genericImport");
						e.printStackTrace();
					}
				} else if (args[i].equalsIgnoreCase("erdosRenyi")) {
					try {
						double probability = Double.valueOf(args[++i]);
						int number = Integer.valueOf(args[++i]);
						gL = new ErdosRenyiGraphGenerator(probability, number);
						retVal.append("An ErdosRenyiGraph was generated with values: probability ("  + dcf.format(probability) + "), " +
								"vertex number (" + number + ")\n");
					} catch (Error e) {
						System.err.println("Error in successive variables after erdosRenyi");
						e.printStackTrace();
					}
				} else if (args[i].equalsIgnoreCase("barabasi")) {
					try {
						int initialVerts = Integer.valueOf(args[++i]);
						int addedEdgesPerStep = Integer.valueOf(args[++i]);
						if (initialVerts < addedEdgesPerStep) {
							throw new Error("Cannot add more edges per additional node than initial vertexes");
						}
						int totalSteps = Integer.valueOf(args[++i]);
						if (args[++i].equalsIgnoreCase("directed")) {
							gL = new BarabasiAlbertGraphGenerator(initialVerts, addedEdgesPerStep, totalSteps, EdgeType.DIRECTED);
						} else if (args[i].equalsIgnoreCase("undirected")) {
							gL = new BarabasiAlbertGraphGenerator(initialVerts, addedEdgesPerStep, totalSteps, EdgeType.UNDIRECTED);
						} else {
							throw new Error("Edge type unhandled: " + args[i]);
						}
						retVal.append("A BarabasiAlbertGraph was generated with values: initial vertexes ("  + initialVerts + "), " +
								"added edges (" + addedEdgesPerStep + ") " +
								"total steps (" + totalSteps + ") " +
								"edge type (" + args[i] + ")\n");
					} catch (Error e) {
						System.err.println("Error in successive variables after erdosRenyi");
						e.printStackTrace();
					}
				} else {
					throw new Error("Unhandled graph creation type");
				}
			}
		}
		
		if (gL == null) {
			throw new Error("Main graph never loaded");
		}
		
		// Load the graph
		Graph<String, String> graph;
		jt.startTracking("Main Graph Import");
		graph = gL.loadGraph();
		jt.endTracking("Main Graph Import");
		return graph;
	}
	
	
	/******** Functions Needed For Concurrent Design Where Analysis is Concurrent *********/
	
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
			mainTracker.startTracking("basic writing " + jobName);
			try {
				writeBasicGraphInformation(graph, outputDir + pMetricPostfix);
			} catch (IOException e1) {
				System.err.println("Error in the Basic Writing of basic information thread: " + jobName);
				e1.printStackTrace();
				return;
			}
			mainTracker.endTracking("basic writing " + jobName);
			// Get the WCC of this graph so that we better understand it's density
			mainTracker.startTracking("WCC calculation" + jobName);
			try {
				WCCSizeAnalysis.analyzeBasicClusterInformation(graph, outputDir + pWccPostfix);
			} catch (IOException e) {
				System.err.println("Error in the WCC of basic information thread: " + jobName);
				e.printStackTrace();
			}
			mainTracker.endTracking("WCC calculation" + jobName);
		}
	}
	
	/** Data: Population -> Split -> Samples
	 ** Analysis: Population -> BC -> postBCPOP
	 ** Analysis: Sample -> BC -> pullBCPOP -> analysis -> correlation
	 ** Analysis: Population -> Basic Stats Output
	 ** Analysis: Sample -> Basic Stats Output
	 */
	static ConcurrentHashMap<String, Double> populationBC;
	private static Lock popBCSetLock = new ReentrantLock();
	private static Condition popBCWait = popBCSetLock.newCondition();
	
	/**
	 * A class that serves to bring the gap between the threaded BC analysis and posting the population BC
	 * @author MOREPOWER
	 *
	 */
	public static class PopBCPostRunnable implements Runnable {

		ExecutorService myThreadPool;
		Callable<ConcurrentHashMap<String, Double>> myThread;
		
		public PopBCPostRunnable(ExecutorService threadpool, Callable<ConcurrentHashMap<String, Double>> thread) {
			myThreadPool = threadpool;
			myThread = thread;
		}
		
		@Override
		public void run() {
			Future<ConcurrentHashMap<String, Double>> future = myThreadPool.submit(myThread);
			while (!future.isDone()) {
				if (!future.isCancelled())
					continue;
				else
					myThreadPool.shutdownNow();
			}
			
			try {
				setBCPop(future.get());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	public static void setBCPop(ConcurrentHashMap<String, Double> popBC) {
		popBCSetLock.lock();
		populationBC = popBC;
		System.out.println("Population BC Values Posted");
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
		
		private ExecutorService executorService;
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
				Graph<String, String> parentGraph, ExecutorService executorService) {			
			this.executorService = executorService;
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
			executorService.execute(new BasicInformationRunnable(sampleTracker, sample, sampleDir, sampleName));
			// Run a thread to do the BC analysis
			Callable<HashMap<String, Double[]>> sampleBCRunner = new SampleBC.CallableGraphBC(sample, sampleDir + pBcPostfix, sampleTracker, "BC of " + sampleName);
			Future<HashMap<String, Double[]>> sampleBCFuture = executorService.submit(sampleBCRunner);

			try {
				// Output the graph in the down-time
				BasicGraph.exportGraph(sample, sampleDir + pSampleData);
				
				// Wait until the BC is done
				while (sampleBCFuture.isDone() == false) {
					Thread.sleep(500);
					Thread.yield();
				}
				
				/** Now begin the comparison **/
				// Pull the population BC values
				ConcurrentHashMap<String, Double> popBC = pullBCPop();
				HashMap<String, Double[]> sampleBC = sampleBCFuture.get();
				double[] result = BCCorrelator.runBCComparison(
						popBC,
						sampleBC,
						compNumber);
				
				// Write out the respective job times
				BufferedWriter jobOutput = Utils.FileSystem.createFile(sampleDir + pSummaryPostfix);
				jobOutput.append(sampleTracker.getJobTimes());
				jobOutput.close();
				
				// Return the result!
				return result;
				
			} catch (Exception e) {
				System.err.println("An error occured in sample: " + sampleName);
				e.printStackTrace();
				executorService.shutdownNow();
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
	 */
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
		
		// This may be inefficient, but it's easier to keep track of
		JobTracker mainTracker = new JobTracker();
		mainTracker.startTracking("overall job");
		
		// TODO: Make this better
		String outputDir = null;
		if (args[0].equalsIgnoreCase("--output")) {
			outputDir = args[1];
		}
		Utils.FileSystem.createFolder(outputDir);
		
		// Import in the overall graph
		StringBuilder summary = new StringBuilder();
		Graph<String, String> graph = importGraph(summary, args, mainTracker);
		
		/*** BEGIN THE CONCURRENCY ***/
		ExecutorService executorService = Executors.newFixedThreadPool(20);
		// Run a thread to get the population BC
		// This manages it being posted for the other threads to pull
		Callable<ConcurrentHashMap<String, Double>> popThread = new PopulationBC.CallableGraphBC(graph, outputDir + pBcPostfix, mainTracker, "Main BC Calculation");
		executorService.execute(new PopBCPostRunnable(executorService, popThread));
		
		// Run a thread to write out basic information pertaining to the graph
		executorService.execute(new BasicInformationRunnable(mainTracker, graph, outputDir, "population"));
		
		/** Begin the sampling **/
		// Create the folder space for the processes
		String sampleOverallDir = outputDir + pSamplesFolder;
		Utils.FileSystem.createFolder(sampleOverallDir);
			
		// Map to correlate the names of the samples to their respective outputs
		HashMap<String, Future<Double[]>> sampleOutputs = new HashMap<String, Future<Double[]>>(10);
		
		// Begin the sampling!
		for (double alpha = 0.1; alpha < 1.0; alpha += 0.2) {
			// Uniquely name this version
			String sampleName = "RSN" + dcf.format(alpha);
			String sampleDir = sampleOverallDir + "/" + sampleName;
			Utils.FileSystem.createFolder(sampleDir);
			
			// We are going to try this with the sampling method based off alpha
			SampleMethod rndSample = new RandomSampleNode(alpha);
			executorService.submit(new SampleThreadRunner(sampleDir, sampleName, 50, rndSample, graph, executorService));			
		}
		
		// Wait for everything to finish
		try {
			executorService.awaitTermination(24, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			System.out.println("Error in threads... Very helpful");
			e.printStackTrace();
		}
		
		// Output the statistics on the correlations
		BufferedWriter metricOutput = Utils.FileSystem.createFile(outputDir + pCorrPostfix);
		metricOutput.write("\"sampling method\",\"true correlation\", \"adj correlation\"");
		metricOutput.newLine();
		for (String key : sampleOutputs.keySet()) {
			Double[] output = null;
			try {
				output = sampleOutputs.get(key).get();
			} catch (Exception e) {
				System.out.println("Did everything actually print...");
				e.printStackTrace();
			}
			metricOutput.append(key + "," + dcf.format(output[0]) + "," + dcf.format(output[1]));
			metricOutput.newLine();
		}
		metricOutput.close();
		
		// End the tracking over the entire job
		mainTracker.endTracking("overall job");
		
		// Write out the respective job times
		BufferedWriter jobOutput = Utils.FileSystem.createFile(outputDir + pSummaryPostfix);
		jobOutput.append(mainTracker.getJobTimes());
		jobOutput.close();
		
		// Shut down the executor service
		executorService.shutdown();	
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
		
		summary.write("mean : " + dcf.format(sms.getMean()) + " std error : " + dcf.format(sms.getStandardDeviation()));
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
