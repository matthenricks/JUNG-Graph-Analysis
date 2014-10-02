package Main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import GraphAnalyzers.AnalyzerDistribution;
import GraphAnalyzers.BCAnalyzer;
import GraphAnalyzers.EDAnalyzer;
import GraphAnalyzers.InDegreeAnalyzer;
import GraphAnalyzers.OutDegreeAnalyzer;
import GraphAnalyzers.WCCSizeAnalysis;
import GraphCreation.BasicGraph;
import GraphCreation.GeneratedGraph;
import SamplingAlgorithms.RNDBFSSingleSampler;
import SamplingAlgorithms.RNDForestFirePaperSampler;
import SamplingAlgorithms.RNDWalkMetroHastingsSampler;
import SamplingAlgorithms.RNDWalkSampler;
import SamplingAlgorithms.TargetedSampleMethod;
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
	
	static final int replicaLength = 10;
	
	static final String csv_header = "\"Parent Node Count\","
						+ "\"Parent Edge Count\","
						+ "\"Sample Method Type\","
						+ "\"BFS:RND\","
//						+ "\"Maximum Sample (#)\","
						+ "\"Replica ID\","
						+ "\"Alpha (%)\","
						+ "\"Sample Node Count\","
						+ "\"Sample Edge Count\","
						// CC values
						+ "\"Cross-Correlation Type\","
						+ "\"Percentage Amount\","
						+ "\"Jacaards\","
						+ "\"Jacaards2\","
						// End CC
						+ "\"Real Alpha\","
						+ "\"Real BFS:RND\","
						+ "\"Sample WCC Count\","
						+ "\"Sample Metric Duration\","
						+ "\"Measure\","
						+ "\"Sample Correlation Alpha (%)\","
						+ "\"Spearmans\","
						+ "\"Pearsons\","
						+ "\"Error\","
//						+ "\"KL\","
						+ "\"Population P/R Alpha\","
						+ "\"Sample P/R Alpha\","
						+ "\"Precision\","
						+ "\"Recall\","
						+ "\"KS Statistic\","
					;
	
	static final String csv_header_FF = 
			"\"Parent Node Count\","
			+ "\"Parent Edge Count\","
			+ "\"Sample Method Type\","
			+ "\"Forward Probability\","
			+ "\"Backward Probability\","
			+ "\"FF:RND\","
//			+ "\"Maximum Sample (#)\","
			+ "\"Replica ID\","
			+ "\"Alpha (%)\","
			+ "\"Sample Node Count\","
			+ "\"Sample Edge Count\","
			// CC values
			+ "\"Cross-Correlation Type\","
			+ "\"Percentage Amount\","
			+ "\"Jacaards\","
			+ "\"Jacaards2\","
			// End CC
			+ "\"Real Alpha\","
			+ "\"Real BFS:RND\","
			+ "\"Sample WCC Count\","
			+ "\"Sample Metric Duration\","
			+ "\"Measure\","
			+ "\"Sample Correlation Alpha (%)\","
			+ "\"Spearmans\","
			+ "\"Pearsons\","
			+ "\"Error\","
//			+ "\"KL\","
			+ "\"Population P/R Alpha\","
			+ "\"Sample P/R Alpha\","
			+ "\"Precision\","
			+ "\"Recall\","
			+ "\"KS Statistic\","
		;
	
	// Probability needed for the Forest Fire
	static final double[] forwardProb = {0.2, 0.5, 0.8};
	static final double[] backProb = {0, 0, 0};
	
	// Make this entire thing an alpha loop
	static final double[] alphaArea = {
			0.01, 0.02, 0.03, 0.04, 0.05, 
			0.08, 0.1, 0.2
	};
	
	static final int maxThreads = 5;
	
	/**
	 * 0.01, 0.02, 0.03, 0.04, // 0.05 }; 
			0.05, 0.1, 0.15, 
			0.2, 0.3, 0.4, 0.6 , 0.8, 1.0};1
	 */
	
	
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
		
		BufferedWriter bw = Utils.FileSystem.createFileIfNonexistant(outputDir + HardCode.pMetricPostfix);
		if (bw != null) {
			bw.write("The number of edges are: " + graph.getEdgeCount());
			bw.newLine();
			bw.write("The number of indices are: " + graph.getVertexCount());
			bw.newLine();
			bw.close();
			tracker.endTracking("basic writing of " + jobName);
		}
		
		// Get the WCC of this graph so that we better understand it's density
		tracker.startTracking("WCC calculation of " + jobName);
		Integer WCC = (new WCCSizeAnalysis()).computeOrProcess(graph, outputDir + HardCode.pWccPostfix).keySet().size();
		tracker.endTracking("WCC calculation of " + jobName);
		return new CSV_Builder(WCC);
	}
	
	/**
	 * The different holders for the population data -- to be shared among threads
	 */
	// Volatile is needed since they aren't pulled from a function
	static volatile ConcurrentHashMap<String, Double> populationBC;
	static volatile ConcurrentHashMap<String, Double> populationInDegree;
	static volatile ConcurrentHashMap<String, Double> populationOutDegree;
	static volatile ConcurrentHashMap<String, Double> populationED;

	
	private static Lock popSetLock = new ReentrantLock();
	private static Condition popWait = popSetLock.newCondition();
	
	public static void setPop(Map<String, Double> popBC, Map<String, Double> popInDegree, Map<String, Double> popOutDegree, Map<String, Double> popED) {
		popSetLock.lock();
		System.out.println("Population Values Posted");
		populationBC = new ConcurrentHashMap<String, Double>(popBC);
		populationInDegree = new ConcurrentHashMap<String, Double>(popInDegree);
		populationOutDegree = new ConcurrentHashMap<String, Double>(popOutDegree);
		populationED = new ConcurrentHashMap<String, Double>(popED);
		popWait.signalAll();
		popSetLock.unlock();
	}
	
	public static void waitForPop() throws InterruptedException {
		// Wait until the populationBC has been written to
		popSetLock.lock();
		while(populationBC == null || populationInDegree == null || populationOutDegree == null || populationED == null) {
			System.out.println("Sample thread waiting for Population BC Values");
			popWait.await();
		}
		popSetLock.unlock();
		System.out.println("Population Values Pulled");
	}
		
	public static class SampleThreadRunner implements Callable<CSV_Builder> {
		
		private String sampleDir, sampleName;
		private Graph<String, String> parentGraph;
		private TargetedSampleMethod sampleMethod;
		private Integer ID;
		
		/**
		 * Constructor for the sample runner thread. This involves the thread management of sampling, analysis, and correlation.
		 * @param sampleDir - directory of the sample to save it's data and files
		 * @param sampleName - the unique name of the sample
		 * @param sampleMethod - the method of sampling used to obtain the sample
		 * @param parentGraph - the parent graph to be sampled
		 * @param executorService - the executor (this isn't necessarily needed)
		 */
		public SampleThreadRunner(String sampleDir, String sampleName, TargetedSampleMethod sampleMethod, 
				Graph<String, String> parentGraph, Integer ID) {			
			this.sampleDir = sampleDir;
			this.sampleName = sampleName;
			this.sampleMethod = sampleMethod;
			this.parentGraph = parentGraph;
			this.ID = ID;
		}

		@Override
		// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Added at the end: [KS, KS adj]
		public CSV_Builder call() throws Exception {
			// Use the ID to consolidate everything together
			CSV_Builder cID = new CSV_Builder(this.ID);
			
			for (double alpha : alphaArea) {
				// Record it's own job stuff in here!
				JobTracker sampleTracker = new JobTracker();

				// Now set the max sample
				sampleMethod.changeAlpha(alpha);

				// Create a folder segmenting by alpha
				String aFolder = sampleDir + "/" + "alpha" + HardCode.dcf.format(alpha * 10000);
				Utils.FileSystem.createFolder(aFolder);

				// Only generate the graph if it needs generation
				Graph<String, String> sample;
				// All of the options should be the same Iterations, Real Alpha, Real Threshold
				CSV_Builder cSamplingStats;
				if ((new File(aFolder + HardCode.pDataFix)).isFile()) {
					// Load the already finished graph
					sample = (new BasicGraph(aFolder + HardCode.pDataFix)).loadGraph();
					cSamplingStats = new CSV_Builder("Imported", 
							new CSV_Builder("Imported")
					);
				} else {
					// First we need to actually generate the sample graph
					sampleTracker.startTracking("Initial sampling of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
					cSamplingStats = sampleMethod.sampleGraph(parentGraph);
					sample = sampleMethod.getGraph();
					sampleTracker.endTracking("Initial sampling of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));

					// Output the graph since it hasn't been exported yet
					BasicGraph.exportGraph(sample, aFolder + HardCode.pDataFix);
				}
				
				/** Now begin the analysis **/
				// Write out the  write out the basic information pertaining to the graph
				CSV_Builder cWCC = writeBasicInformation(sampleTracker, sample, aFolder, sampleName);

				// Wait for the population thread to finish
				waitForPop();

				// TODO: ANALYZERS HERE
				Map<String, AnalyzerDistribution> analyzers = new HashMap<String, AnalyzerDistribution>();
				analyzers.put(HardCode.pBcPostfix, (new BCAnalyzer()));
				analyzers.put(HardCode.pDegreeInPostfix, (new InDegreeAnalyzer()));
				analyzers.put(HardCode.pDegreeOutPostfix, (new OutDegreeAnalyzer()));
				analyzers.put(HardCode.pEDPostfix, (new EDAnalyzer()));
			
				// A grouping for all of the sample outputs
				HashMap<String, Double> bcVal = null;
				HashMap<String, Double> edVal = null;
				HashMap<String, Double> ideVal = null;
				HashMap<String, Double> odeVal = null;
				
				// Run all of the analyzers
				for (Entry<String, AnalyzerDistribution> analyzer : analyzers.entrySet()) {
					// Find and process the analyzer of the graph
					sampleTracker.startTracking(analyzer.getValue().getName() + " of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));
					HashMap<String, Double> sampleValue = new HashMap<String, Double>(analyzer.getValue().computeOrProcess(sample, aFolder + analyzer.getKey()));
					sampleTracker.endTracking(analyzer.getValue().getName() + " of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000));

					// Generate the CSV_Builder to link
					CSV_Builder cDur = new CSV_Builder(sampleTracker.getJobTime(analyzer.getValue().getName() + " of " + sampleName + "-a" + Utils.HardCode.dcf.format(alpha*10000)));
					cDur.LinkTo(new CSV_Builder(analyzer.getValue().getName()));					
					
					// Find the values within the graph and analyze
					if (analyzer.getValue() instanceof BCAnalyzer) {
						bcVal = sampleValue;
						
						cDur.LinkToEnd(MeasureComparison.compare(populationBC, sampleValue)); // [correlation, PR]
						cDur.LinkToEnd(MeasureComparison.KSCompare(populationBC, sampleValue)); // [KS]
					} else if (analyzer.getValue() instanceof InDegreeAnalyzer) {
						ideVal = sampleValue;

						cDur.LinkToEnd(MeasureComparison.compare(populationInDegree, sampleValue)); // [correlation, PR]
						cDur.LinkToEnd(MeasureComparison.KSCompare(populationInDegree, sampleValue)); // [KS]
					} else if (analyzer.getValue() instanceof OutDegreeAnalyzer) {
						odeVal = sampleValue;

						cDur.LinkToEnd(MeasureComparison.compare(populationOutDegree, sampleValue)); // [correlation, PR]
						cDur.LinkToEnd(MeasureComparison.KSCompare(populationOutDegree, sampleValue)); // [KS]
					} else {
						// THIS HAS BEEN SORTED BACKWARDS
						edVal = sampleValue;

						cDur.LinkToEnd(MeasureComparison.compare(populationED, sampleValue, true)); // [correlation, PR]
						cDur.LinkToEnd(MeasureComparison.KSCompare(populationED, sampleValue)); // [KS]
					}
					
					// Connect the CSV to the duration
					cWCC.LinkTo(cDur);
				}
				
				// Now try to cross-correlate the samples (with correlation and P/R)
				// Does the top 10% of a sample predict the top 10% of another sample?
				ArrayList<Entry<String, Double>> bcList = new ArrayList<Entry<String, Double>>(bcVal.entrySet());
				ArrayList<Entry<String, Double>> edList = new ArrayList<Entry<String, Double>>(edVal.entrySet());
				ArrayList<Entry<String, Double>> ideList = new ArrayList<Entry<String, Double>>(ideVal.entrySet());
				ArrayList<Entry<String, Double>> odeList = new ArrayList<Entry<String, Double>>(odeVal.entrySet());
				
				Collections.sort(bcList, MeasureComparison.entrySort);
				Collections.sort(ideList, MeasureComparison.entrySort);
				Collections.sort(odeList, MeasureComparison.entrySort);
				// This has been reversed since ED is better when it's small
				Collections.sort(edList, MeasureComparison.entrySortBackwards);
				
				double[] topPercentages = {0.1, 0.2, 0.5};
				
				// Returns: percent, precision, recall
				List<CSV_Builder> crossCorrelations = new LinkedList<CSV_Builder>();
				crossCorrelations.add(new CSV_Builder("BC-ED", MeasureComparison.PRCompare(bcList, edList, topPercentages)));
				crossCorrelations.add(new CSV_Builder("BC-InDegree", MeasureComparison.PRCompare(bcList, ideList, topPercentages)));
				crossCorrelations.add(new CSV_Builder("ED-InDegree", MeasureComparison.PRCompare(edList, ideList, topPercentages)));
				crossCorrelations.add(new CSV_Builder("OutDegree-BC", MeasureComparison.PRCompare(odeList, bcList, topPercentages)));
				crossCorrelations.add(new CSV_Builder("OutDegree-ED", MeasureComparison.PRCompare(odeList, edList, topPercentages)));
				crossCorrelations.add(new CSV_Builder("OutDegree-InDegree", MeasureComparison.PRCompare(odeList, ideList, topPercentages)));
				
				// Write out the respective job times
				BufferedWriter jobOutput = new BufferedWriter(new FileWriter(Utils.FileSystem.findOpenPath(aFolder + HardCode.pSummaryPostfix)));
				sampleTracker.writeJobTimes(jobOutput);
				jobOutput.close();
					
				/** Return all the pertinent information by using the CSV Builders **/
				
				// Link: Iterations, Real Alpha, Real Threshold, <-- WCC...
				cSamplingStats.LinkToEnd(cWCC);
				
				// Link: ID <-- alpha(%), vert(#), edge(#) <-- corr type, cross alpha, Precision, Recall  <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
				CSV_Builder internals = (new CSV_Builder(new CSV_Percent(alpha), 
						new CSV_Builder(sample.getVertexCount(),
							new CSV_Builder(sample.getEdgeCount()))));
				internals.LinkToEnd(crossCorrelations);
				internals.LinkToEnd(cSamplingStats);
				
				cID.LinkTo(internals);
			}
			return cID;
		}
	}
	
	/**
	 * This shall run the entire sequence. The argument input sequence isn't well designed yet  
	 * @param args
	 * @throws Exception 
	 * @throws Error 
	 */
	public static void main(String[] args) throws Exception {
		
		// Thresholds, don't do the random twice (aka having the 0.0)
		double[] thresholdArea = {0.0};
		double[] thresholdArea2 = {1.0};
		
		// Set up the thread executors
		threadPool = Executors.newCachedThreadPool();
		
		// Read in the arguments
		ArgumentReader loader = ArgumentReader.read(args);
		
		// This may be inefficient, but it's easier to keep track of
		JobTracker mainTracker = new JobTracker();
		mainTracker.startTracking("overall job");
		
		// Make the overall output folder
		Utils.FileSystem.createFolder(loader.myOutput);
		
		// Redirect the output, including errors
		System.setOut(new PrintStream(new FileOutputStream(new File(loader.myOutput + "/Console.txt"))));
		System.setErr(new PrintStream(new FileOutputStream(new File(loader.myOutput + "/Err.txt"))));

		// Import in the overall graph
		StringBuilder summary = new StringBuilder();
		Graph<String, String> graph;
		if (loader.myGraphLoader instanceof GeneratedGraph) {
			if ((new File(loader.myOutput + HardCode.pDataFix)).isFile() == false) {
				graph = loader.myGraphLoader.loadGraph();
				mainTracker.startTracking("Export Generated Graph");
				BasicGraph.exportGraph(graph, loader.myOutput + HardCode.pDataFix);
				mainTracker.endTracking("Export Generated Graph");
			} else {
				graph = (new BasicGraph(loader.myOutput + HardCode.pDataFix)).loadGraph();
			}
		} else {
			graph = loader.myGraphLoader.loadGraph();
		}
		summary.append(loader.myGraphLoader.getInformation());

		/*** BEGIN THE Analysis ***/
		
		/** Run the population analysis **/
		// Either run a thread to load the BC, or import in a finished BC import
		String sPopBC = HardCode.pBcPostfix, 
				sPopInDegree = HardCode.pDegreeInPostfix,
				sPopOutDegree = HardCode.pDegreeOutPostfix,
				sPopED = HardCode.pEDPostfix;
		if (loader.myPopPath == null) {
			Utils.FileSystem.createFolder(loader.myOutput + HardCode.pDistroFolder);
			sPopBC = loader.myOutput + HardCode.pDistroFolder + sPopBC;
			sPopInDegree = loader.myOutput + HardCode.pDistroFolder + sPopInDegree;
			sPopOutDegree = loader.myOutput + HardCode.pDistroFolder + sPopOutDegree;
			sPopED = loader.myOutput + HardCode.pDistroFolder + sPopED;
		} else {
			sPopBC = loader.myPopPath + sPopBC;
			sPopInDegree = loader.myPopPath + sPopInDegree;
			sPopOutDegree = loader.myPopPath + sPopOutDegree;
			sPopED = loader.myPopPath + sPopED;
		}
		
		/** Now generate/load the results **/
		ConcurrentHashMap<String, Double> popBC, popInDegree, popOutDegree, popED;
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
		// InDegree
		if ((new File(sPopInDegree)).exists()) {
			popInDegree = new ConcurrentHashMap<String, Double>(
					(new InDegreeAnalyzer()).read(sPopInDegree));
		} else {
			mainTracker.startTracking("Pop Degree Calculation");
			popInDegree = new ConcurrentHashMap<String, Double>(
					(new InDegreeAnalyzer()).analyzeGraph(graph, sPopInDegree));
			mainTracker.endTracking("Pop Degree Calculation");
		}
		// OutDegree
		if ((new File(sPopOutDegree)).exists()) {
			popOutDegree = new ConcurrentHashMap<String, Double>(
					(new OutDegreeAnalyzer()).read(sPopOutDegree));
		} else {
			mainTracker.startTracking("Pop Degree Calculation");
			popOutDegree = new ConcurrentHashMap<String, Double>(
					(new OutDegreeAnalyzer()).analyzeGraph(graph, sPopOutDegree));
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
		
		AnalysisRunner.setPop(popBC, popInDegree, popOutDegree, popED);

		// Set the folder to store the analysis results
		Utils.FileSystem.createFolder(loader.myOutput + HardCode.pAnalysisFolder);
		
		/** Begin the sampling **/
		// Create the folder space for the processes
		String sampleOverallDir = loader.myOutput + HardCode.pSamplesFolder;
		Utils.FileSystem.createFolder(sampleOverallDir);
		
		// Begin the sampling!
		String testName = "OLDBFSRND";
		CSV_Builder test = new CSV_Builder(testName);
		String mFolder = sampleOverallDir + testName;
		Utils.FileSystem.createFolder(mFolder);
		for (double threshold : thresholdArea2) {
			String tFolder = mFolder + "/thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			
			// Create an array to hold the different threads
			ArrayList<Callable<CSV_Builder>> tasks = new ArrayList<Callable<CSV_Builder>>();
			
			CSV_Builder input = new CSV_Builder(new CSV_Percent(threshold));
			
			// Run the analysis
			for (int replica = 0; replica < replicaLength; replica++) {
				// Uniquely name this version and create its folder
				String sampleName = "sample" + replica;
				String sampleDir = tFolder + "/" + sampleName;
				Utils.FileSystem.createFolder(sampleDir);
				
				// Create the shell of the sample
				TargetedSampleMethod sampleMethod = new RNDBFSSingleSampler(0.00, threshold, graph.getDefaultEdgeType());
				
				// Run the sample threads
				// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
				tasks.add(new SampleThreadRunner(sampleDir, sampleName, sampleMethod, graph, replica));

				if (tasks.size() >= maxThreads) {
					// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
					runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
					test.LinkTo(input);
					tasks.clear();
					input = new CSV_Builder(new CSV_Percent(threshold));
				}
			}
			
			// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
			runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
			test.LinkTo(input);
			tasks.clear();
		}
		
		
		// Lastly add the sample type used and the overall graph information
		CSV_Builder mainData = new CSV_Builder(graph.getVertexCount(), // parent node count
				new CSV_Builder(graph.getEdgeCount(), // parent edge count
						test)); // sample method type
		
		// Results array: threshold, maxSample(#), ID, alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Link: sampleType

		// Output the statistics on the correlations
		BufferedWriter csvOutput = Utils.FileSystem.createWriter(Utils.FileSystem.findOpenPath(loader.myOutput + HardCode.pAnalysisFolder + "/" + testName + HardCode.pCorrPostfix));
		csvOutput.write(csv_header);
		csvOutput.newLine();
		mainData.writeCSV(csvOutput);
		csvOutput.close();
		
		
		/** Now begin the completely random sampling */
		/** Begin New BFS Sampling! **/
		testName = "RND";
		test = new CSV_Builder(testName);
		mFolder = sampleOverallDir + testName;
		Utils.FileSystem.createFolder(mFolder);
		for (double threshold : thresholdArea) {
			String tFolder = mFolder + "/thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			
			// Create an array to hold the different threads
			ArrayList<Callable<CSV_Builder>> tasks = new ArrayList<Callable<CSV_Builder>>();
			
			CSV_Builder input = new CSV_Builder(new CSV_Percent(threshold));
			
			// Run the analysis
			for (int replica = 0; replica < replicaLength; replica++) {
				// Uniquely name this version and create its folder
				String sampleName = "sample" + replica;
				String sampleDir = tFolder + "/" + sampleName;
				Utils.FileSystem.createFolder(sampleDir);
				
				// Create the shell of the sample
				TargetedSampleMethod sampleMethod = new RNDBFSSingleSampler(0.00, threshold, graph.getDefaultEdgeType());
				
				// Run the sample threads
				// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
				tasks.add(new SampleThreadRunner(sampleDir, sampleName, sampleMethod, graph, replica));
				
				if (tasks.size() >= maxThreads) {
					// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
					runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
					test.LinkTo(input);
					tasks.clear();
					input = new CSV_Builder(new CSV_Percent(threshold));
				}
			}
			
			// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
			runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
			test.LinkTo(input);
			tasks.clear();
		}
		// Lastly add the sample type used and the overall graph information
		mainData = new CSV_Builder(graph.getVertexCount(), // parent node count
				new CSV_Builder(graph.getEdgeCount(), // parent edge count
						test)); // sample method type
		
		// Results array: threshold, maxSample(#), ID, alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Link: sampleType

		// Output the statistics on the correlations
		csvOutput = Utils.FileSystem.createWriter(Utils.FileSystem.findOpenPath(loader.myOutput + HardCode.pAnalysisFolder + "/" + testName + HardCode.pCorrPostfix));
		csvOutput.write(csv_header);
		csvOutput.newLine();
		mainData.writeCSV(csvOutput);
		csvOutput.close();
		
		
		/** Begin New BFS Sampling! **/
		testName = "BFSRND";
		test = new CSV_Builder(testName);
		mFolder = sampleOverallDir + testName;
		Utils.FileSystem.createFolder(mFolder);
		for (double threshold : thresholdArea2) {
			String tFolder = mFolder + "/thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			
			// Create an array to hold the different threads
			ArrayList<Callable<CSV_Builder>> tasks = new ArrayList<Callable<CSV_Builder>>();
			
			CSV_Builder input = new CSV_Builder(new CSV_Percent(threshold));
			
			// Run the analysis
			for (int replica = 0; replica < replicaLength; replica++) {
				// Uniquely name this version and create its folder
				String sampleName = "sample" + replica;
				String sampleDir = tFolder + "/" + sampleName;
				Utils.FileSystem.createFolder(sampleDir);
				
				// Create the shell of the sample
				TargetedSampleMethod sampleMethod = new RNDBFSSingleSampler(0.00, threshold, graph.getDefaultEdgeType());
				
				// Run the sample threads
				// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
				tasks.add(new SampleThreadRunner(sampleDir, sampleName, sampleMethod, graph, replica));

				if (tasks.size() >= maxThreads) {
					// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
					runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
					test.LinkTo(input);
					tasks.clear();
					input = new CSV_Builder(new CSV_Percent(threshold));
				}
			}
			
			// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
			runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
			test.LinkTo(input);
			tasks.clear();
		}
		// Lastly add the sample type used and the overall graph information
		mainData = new CSV_Builder(graph.getVertexCount(), // parent node count
				new CSV_Builder(graph.getEdgeCount(), // parent edge count
						test)); // sample method type
		
		// Results array: threshold, maxSample(#), ID, alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Link: sampleType

		// Output the statistics on the correlations
		csvOutput = Utils.FileSystem.createWriter(Utils.FileSystem.findOpenPath(loader.myOutput + HardCode.pAnalysisFolder + "/" + testName + HardCode.pCorrPostfix));
		csvOutput.write(csv_header);
		csvOutput.newLine();
		mainData.writeCSV(csvOutput);
		csvOutput.close();
		

		
		/** Random Walk Sampling! **/
		// Begin the sampling!
		testName = "RNDWalk";
		test = new CSV_Builder(testName);
		mFolder = sampleOverallDir + testName;
		Utils.FileSystem.createFolder(mFolder);
		for (double threshold : thresholdArea2) {
			String tFolder = mFolder + "/thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			
			// Create an array to hold the different threads
			ArrayList<Callable<CSV_Builder>> tasks = new ArrayList<Callable<CSV_Builder>>();
			
			CSV_Builder input = new CSV_Builder(new CSV_Percent(threshold));
			
			// Run the analysis
			for (int replica = 0; replica < replicaLength; replica++) {
				// Uniquely name this version and create its folder
				String sampleName = "sample" + replica;
				String sampleDir = tFolder + "/" + sampleName;
				Utils.FileSystem.createFolder(sampleDir);
				
				// Create the shell of the sample
				TargetedSampleMethod sampleMethod = new RNDWalkSampler(0.00, threshold, graph.getDefaultEdgeType());
				
				// Run the sample threads
				// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
				tasks.add(new SampleThreadRunner(sampleDir, sampleName, sampleMethod, graph, replica));

				if (tasks.size() >= maxThreads) {
					// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
					runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
					test.LinkTo(input);
					tasks.clear();
					input = new CSV_Builder(new CSV_Percent(threshold));
				}
			}
			
			// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
			runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
			test.LinkTo(input);
			tasks.clear();
		}
		// Lastly add the sample type used and the overall graph information
		mainData = new CSV_Builder(graph.getVertexCount(), // parent node count
				new CSV_Builder(graph.getEdgeCount(), // parent edge count
						test)); // sample method type
		
		// Results array: threshold, maxSample(#), ID, alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Link: sampleType

		// Output the statistics on the correlations
		csvOutput = Utils.FileSystem.createWriter(Utils.FileSystem.findOpenPath(loader.myOutput + HardCode.pAnalysisFolder + "/" + testName + HardCode.pCorrPostfix));
		csvOutput.write(csv_header);
		csvOutput.newLine();
		mainData.writeCSV(csvOutput);
		csvOutput.close();

		
		
		
		/** Random Metro-Hastings Walk Sampling! **/
		// Begin the sampling!
		testName = "MRNDWalk";
		test = new CSV_Builder(testName);
		mFolder = sampleOverallDir + testName;
		Utils.FileSystem.createFolder(mFolder);
		for (double threshold : thresholdArea2) {
			String tFolder = mFolder + "/thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			
			// Create an array to hold the different threads
			ArrayList<Callable<CSV_Builder>> tasks = new ArrayList<Callable<CSV_Builder>>();
			
			CSV_Builder input = new CSV_Builder(new CSV_Percent(threshold));
			
			// Run the analysis
			for (int replica = 0; replica < replicaLength; replica++) {
				// Uniquely name this version and create its folder
				String sampleName = "sample" + replica;
				String sampleDir = tFolder + "/" + sampleName;
				Utils.FileSystem.createFolder(sampleDir);
				
				// Create the shell of the sample
				TargetedSampleMethod sampleMethod = new RNDWalkMetroHastingsSampler(0.00, threshold, graph.getDefaultEdgeType());
				
				// Run the sample threads
				// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
				tasks.add(new SampleThreadRunner(sampleDir, sampleName, sampleMethod, graph, replica));

				if (tasks.size() >= maxThreads) {
					// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
					runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
					test.LinkTo(input);
					tasks.clear();
					input = new CSV_Builder(new CSV_Percent(threshold));
				}
			}
			
			// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls			
			runThreads(input, tasks, loader.myTimeOut, loader.myTimeOutUnit, tFolder);
			test.LinkTo(input);
			tasks.clear();
		}
		// Lastly add the sample type used and the overall graph information
		mainData = new CSV_Builder(graph.getVertexCount(), // parent node count
				new CSV_Builder(graph.getEdgeCount(), // parent edge count
						test)); // sample method type
		
		// Results array: threshold, maxSample(#), ID, alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Link: sampleType

		// Output the statistics on the correlations
		csvOutput = Utils.FileSystem.createWriter(Utils.FileSystem.findOpenPath(loader.myOutput + HardCode.pAnalysisFolder + "/" + testName + HardCode.pCorrPostfix));
		csvOutput.write(csv_header);
		csvOutput.newLine();
		mainData.writeCSV(csvOutput);
		csvOutput.close();
		

		/** Random Forest Sampling **/
		// Begin the sampling!
		/*
		 * TODO: THIS DOESN'T HAVE THE THREAD LIMITER
		testName = "ForestFire";
		test = new CSV_Builder(testName);
		mFolder = sampleOverallDir + testName;
		Utils.FileSystem.createFolder(mFolder);
		for (double threshold : thresholdArea2) {
			String tFolder = mFolder + "/thresh" + HardCode.dcf.format(threshold*10000);
			Utils.FileSystem.createFolder(tFolder);
			
			// Back-probability and Forward probability should always be the same size
			for (int i = 0; i < forwardProb.length; i++) {
				// Create an array to hold the different threads
				ArrayList<Callable<CSV_Builder>> tasks = new ArrayList<Callable<CSV_Builder>>();
				
				String fpFolder = tFolder + "/FP" + HardCode.dcf.format(forwardProb[i]*10000);
				Utils.FileSystem.createFolder(fpFolder);
				
				// Run the analysis
				for (int replica = 0; replica < replicaLength; replica++) {
					// Uniquely name this version and create its folder
					String sampleName = "sample" + replica;
					String sampleDir = fpFolder + "/" + sampleName;
					Utils.FileSystem.createFolder(sampleDir);
					
					// Create the shell of the sample
					TargetedSampleMethod sampleMethod = new RNDForestFirePaperSampler(0.00, threshold, graph.getDefaultEdgeType(), forwardProb[i], backProb[i]);
					
					// Run the sample threads
					// Link: ID <-- alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
					tasks.add(new SampleThreadRunner(sampleDir, sampleName, sampleMethod, graph, replica));
				}
				
				CSV_Builder input = new CSV_Builder(new CSV_Percent(threshold));
				// threshold(%) <-- replica ID, alpha (%), sample nodes, sample edges, Iterations, Real Alpha, Real Threshold, WCC, BC Duration, corrSize(%), corrSize(#), Spearmans, Pearsons, Error, Kendalls
				for (Future<CSV_Builder> retVal : threadPool.invokeAll(tasks, loader.myTimeOut, loader.myTimeOutUnit)) {
					try {
						input.LinkTo(retVal.get());
					} catch (ExecutionException e) {
						e.printStackTrace();
						System.err.println("culprit is one of: " + tFolder);
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.err.println("the iteration probably took too long: " + tFolder);						
					}
				}
				
				// Create a csv-section for this
				CSV_Builder run = new CSV_Builder(new CSV_Percent(forwardProb[i]), new CSV_Builder(new CSV_Percent(backProb[i]), input));
				
				// Link the input to the test it's on
				test.LinkTo(run);
				tasks.clear();	
			}
		}

		// Lastly add the sample type used and the overall graph information
		mainData = new CSV_Builder(graph.getVertexCount(), // parent node count
				new CSV_Builder(graph.getEdgeCount(), // parent edge count
						test)); // sample method type
		
		// Results array: threshold, maxSample(#), ID, alpha(%), vert(#), edge(#) <-- Iterations, real alpha, real threshold, WCC, Duration, Measure
		// Link: sampleType

		// Output the statistics on the correlations
		csvOutput = Utils.FileSystem.createWriter(Utils.FileSystem.findOpenPath(loader.myOutput + HardCode.pAnalysisFolder + "/" + testName + HardCode.pCorrPostfix));
		csvOutput.write(csv_header_FF);
		csvOutput.newLine();
		mainData.writeCSV(csvOutput);
		csvOutput.close();
		*/

		// Stop all the threads
		stopAllThreads();
		
		// End the tracking over the entire job
		mainTracker.endTracking("overall job");
		
		// Write out the respective job times
		BufferedWriter jobOutput = Utils.FileSystem.createWriter(Utils.FileSystem.findOpenPath(loader.myOutput + HardCode.pSummaryPostfix));
		mainTracker.writeJobTimes(jobOutput);
		jobOutput.close();
	}
	
	/**
	 * Wraps the threadPool invoke method
	 * @param input
	 * @param tasks
	 * @param timeOut
	 * @param unit
	 * @param tFolder
	 * @throws InterruptedException 
	 */
	private static void runThreads(CSV_Builder input, ArrayList<Callable<CSV_Builder>> tasks, int timeOut, TimeUnit unit, String tFolder) throws InterruptedException {
		for (Future<CSV_Builder> retVal : threadPool.invokeAll(tasks, timeOut, unit)) {
			try {
				input.LinkTo(retVal.get());
			} catch (ExecutionException e) {
				e.printStackTrace();
				System.err.println("culprit is one of: " + tFolder);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.err.println("the iteration probably took too long: " + tFolder);						
			}
		}
	}
}