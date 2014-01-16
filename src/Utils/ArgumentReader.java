package Utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import GraphCreation.BarabasiAlbertGraphGenerator;
import GraphCreation.BasicGraph;
import GraphCreation.ErdosRenyiGraphGenerator;
import GraphCreation.FriendsTwitterDataImporter;
import GraphCreation.GraphLoader;
import edu.uci.ics.jung.graph.util.EdgeType;

public class ArgumentReader {
	
	private static String sOutputHeader = "--output";
	private static String sGraphLoaderHeader = "--load";
	private static String sTimeUnitHeader = "--timeout";
	private static String sCorrelationSampleHeader = "--corrSize";
	
	public GraphLoader myGraphLoader;
	public String myOutput;
	public int myTimeOut;
	public TimeUnit myTimeOutUnit;
	public int myCorrelationSample;
	
	/***
	 * Loads the default values for the arguments to be entered
	 */
	public ArgumentReader() {
		myGraphLoader = new ErdosRenyiGraphGenerator(0.4, 500);
		myOutput = Utils.FileSystem.findOpenPath("Test/DefaultOutput/").toString();
		myTimeOut = 1;
		myTimeOutUnit = TimeUnit.HOURS;
		myCorrelationSample = 1000;
	}

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
	 *    --output
	 *      <string>
	 *    --timeout
	 *      <int of time> <timeunit>
	 *    --corrSize
	 *      <int for corrSize>
	 * @param args
	 * @return the arguments read in
	 * @throws IOException
	 * @throws Error - whenever parameters don't match any of the shown paths
	 */
	public static ArgumentReader read(String[] args) throws IOException, Error {
		// Look in the arguments for the import type and it's respective variables
		ArgumentReader loader = new ArgumentReader();
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase(sGraphLoaderHeader)) {
				if (args[++i].equalsIgnoreCase("twitterImport")) {
					try {
						String dLoc = args[++i];
						if (args[++i].equalsIgnoreCase("directed")) {
							loader.myGraphLoader = new FriendsTwitterDataImporter(dLoc, EdgeType.DIRECTED);
						} else if (args[i].equalsIgnoreCase("undirected")) {
							loader.myGraphLoader = new FriendsTwitterDataImporter(dLoc, EdgeType.UNDIRECTED);
						} else {
							throw new Error("Edge type unhandled: " + args[i]);
						}
					} catch (Error e) {
						System.err.println("Error in successive variables after twitterImport");
						e.printStackTrace();
					}
				} else if (args[i].equalsIgnoreCase("genericImport")) {
					try {
						String dLoc = args[++i];
						loader.myGraphLoader = new BasicGraph(dLoc);
					} catch (Error e) {
						System.err.println("Error in successive variables after genericImport");
						e.printStackTrace();
					}
				} else if (args[i].equalsIgnoreCase("erdosRenyi")) {
					try {
						double probability = Double.valueOf(args[++i]);
						int number = Integer.valueOf(args[++i]);
						loader.myGraphLoader = new ErdosRenyiGraphGenerator(probability, number);
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
							loader.myGraphLoader = new BarabasiAlbertGraphGenerator(initialVerts, addedEdgesPerStep, totalSteps, EdgeType.DIRECTED);
						} else if (args[i].equalsIgnoreCase("undirected")) {
							loader.myGraphLoader = new BarabasiAlbertGraphGenerator(initialVerts, addedEdgesPerStep, totalSteps, EdgeType.UNDIRECTED);
						} else {
							throw new Error("Edge type unhandled: " + args[i]);
						}
					} catch (Error e) {
						System.err.println("Error in successive variables after erdosRenyi");
						e.printStackTrace();
					}
				} else {
					throw new Error("Unhandled graph creation type");
				}
			} else if (args[i].equalsIgnoreCase(sOutputHeader)) { 				/**** Importing the output location *****/
				loader.myOutput = args[++i];
			} else if (args[i].equalsIgnoreCase(sTimeUnitHeader)) {
				try {
					loader.myTimeOut = Integer.valueOf(args[++i]);
					loader.myTimeOutUnit = TimeUnit.valueOf(args[++i]);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.print("Order by time<int> then unit<TimeUnit>. Try one of these values instead for the TimeUnit: \n");
					for (TimeUnit t : TimeUnit.values())
						System.err.print(t.toString() + ",");
					return null;
				}
			} else if (args[i].equalsIgnoreCase(sCorrelationSampleHeader)) {
				try {
					loader.myCorrelationSample = Integer.valueOf(args[++i]);
				} catch (Exception e) {
					System.err.println("The correlation value must be an integer constant");
					e.printStackTrace();
				}
			}
		}
		
		// Return the finished loader
		return loader;
	}
}
