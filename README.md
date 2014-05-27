JUNG-Graph-Analysis
===================

JUNG Graph Analysis CCB

Running Example:
java -Xmx4G -jar analysisRunner.jar --load vertTvert Epinions.txt DIRECTED \t --timeout 24 HOURS --output Output

Options:
	 *    --load (what graph are you going to load?)
	 *      twitterImport
	 *         <path> <edgeType>
	 *      genericImport
	 *         <path>
	 *      erdosRenyi
	 *         <#nodes: int> <probability: double>
	 *      barabasi
	 *         <initial vertexes> <added edges per steps> <steps> <edgeType>
	 *      vertTvert
	 *         <path> <edgeType>
	 *    --output (where is the output folder going to be)
	 *      <string>
	 *    --timeout (what is the longest sample-thread time limit)
	 *      <int of time> <timeunit>
	 *    --pop (not required)
	 *      <path to a folder of all the analysis :: String>

-Xmx_G sets the amount of gigibytes of the max size

Section Descriptions
1) DataAnalyzers
	These classes serve to analyze the data created from the graph analyzers. Currently correlation, retrieval, and distribution shape analyzers are included. 
2) GraphAnalyzers
	These classes serve to calculate measures based off the graph input. For example, Local Clustering Coefficient and Betweenness Centrality distributions are two of these
3) GraphCreation
	These classes either import in the graphs into JUNG-format Graph<String, String> or generate a graph into that format. Only SparseGraphs of either Directed or Undirected are supported
4) Main
	These classes store the more procedural classes used to actually run the analysis
5) Sampling Algorithms
	These classes create sample graphs from a population graph. As of now, all are targeted to a specific alpha or proportion of the population.
6) Utils
	These classes store hardcode as well as the argument reader and jobTracker(time-tracker)