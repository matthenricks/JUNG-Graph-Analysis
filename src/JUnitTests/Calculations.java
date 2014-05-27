package JUnitTests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import DataAnalyzers.KolmogorovSmirnovTest;
import GraphAnalyzers.BCAnalyzer;
import GraphAnalyzers.InDegreeAnalyzer;
import GraphAnalyzers.OutDegreeAnalyzer;
import GraphCreation.BarabasiAlbertGraphGenerator;
import GraphCreation.GraphLoader;
import SamplingAlgorithms.RNDBFSSampler;
import Utils.FileSystem;
import Utils.HardCode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Tests some of the functionality involving calculations.
 * @author MOREPOWER
 *
 */
public class Calculations {

	@Test
	public void degreeDirectedTest() throws IOException {
		DirectedSparseGraph<String, String> dGraph = new DirectedSparseGraph<String, String>();
		dGraph.addVertex("A");
		dGraph.addVertex("B");
		dGraph.addVertex("C");

		dGraph.addEdge("e1", "A", "B");
		dGraph.addEdge("e2", "B", "A");
		dGraph.addEdge("e3", "C", "B");
		
		Map<String, Double> ret = (new InDegreeAnalyzer()).analyzeGraph(dGraph, FileSystem.findOpenPath("in.csv").toString());
		assert(ret.get("A") == 1.0);
		assert(ret.get("B") == 2.0);
		assert(ret.get("C") == 0.0);

		ret = (new OutDegreeAnalyzer()).analyzeGraph(dGraph, FileSystem.findOpenPath("out.csv").toString());
		assert(ret.get("A") == 1.0);
		assert(ret.get("B") == 1.0);
		assert(ret.get("C") == 1.0);
	}
	
	@Test
	public void degreeUndirectedTest() throws IOException {
		UndirectedSparseGraph<String, String> dGraph = new UndirectedSparseGraph<String, String>();
		dGraph.addVertex("A");
		dGraph.addVertex("B");
		dGraph.addVertex("C");

		dGraph.addEdge("e1", "A", "B");
		dGraph.addEdge("e2", "B", "A");
		dGraph.addEdge("e3", "C", "B");
		
		Map<String, Double> ret = (new InDegreeAnalyzer()).analyzeGraph(dGraph, FileSystem.findOpenPath("in.csv").toString());
		assert(ret.get("A") == 1.0);
		assert(ret.get("B") == 2.0);
		assert(ret.get("C") == 1.0);

		ret = (new OutDegreeAnalyzer()).analyzeGraph(dGraph, FileSystem.findOpenPath("out.csv").toString());
		assert(ret.get("C") == 1.0);
	}
	
	@Test
	public void roundingTest() {
		double arg[] = {1.12345};
		assertTrue(HardCode.floorAllValues(arg, 5)[0] == 1.12345);
		assertTrue(HardCode.floorAllValues(arg, 4)[0] == 1.1234);
		assertTrue(HardCode.floorAllValues(arg, 0)[0] == 1);
	}
	
	@Test
	public void KSTest() {
		double[] array1 = {0, 1, 2, 10, 2.2, 3.3, 6};
		double[] array2 = Arrays.copyOf(array1, array1.length);
		Arrays.sort(array2);
		assertTrue(KolmogorovSmirnovTest.runSmirnov(array1, array2) == 0);
		double[] array3 = {0, 1, 2, 10, 2.2, 3.3, 5};
		assertTrue(KolmogorovSmirnovTest.runSmirnov(array1, array3) != 0);

		double[] redwell={23.4, 30.9, 18.8, 23.0, 21.4, 1, 24.6, 23.8, 24.1, 18.7, 16.3, 20.3, 14.9, 35.4, 21.6, 21.2, 21.0, 15.0, 15.6, 24.0, 34.6, 40.9, 30.7, 24.5, 16.6, 1, 21.7, 1, 23.6, 1, 25.7, 19.3, 46.9, 23.3, 21.8, 33.3, 24.9, 24.4, 1, 19.8, 17.2, 21.5, 25.5, 23.3, 18.6, 22.0, 29.8, 33.3, 1, 21.3, 18.6, 26.8, 19.4, 21.1, 21.2, 20.5, 19.8, 26.3, 39.3, 21.4, 22.6, 1, 35.3, 7.0, 19.3, 21.3, 10.1, 20.2, 1, 36.2, 16.7, 21.1, 39.1, 19.9, 32.1, 23.1, 21.8, 30.4, 19.62, 15.5};
		double[] whitney={16.5, 1, 22.6, 25.3, 23.7, 1, 23.3, 23.9, 16.2, 23.0, 21.6, 10.8, 12.2, 23.6, 10.1, 24.4, 16.4, 11.7, 17.7, 34.3, 24.3, 18.7, 27.5, 25.8, 22.5, 14.2, 21.7, 1, 31.2, 13.8, 29.7, 23.1, 26.1, 25.1, 23.4, 21.7, 24.4, 13.2, 22.1, 26.7, 22.7, 1, 18.2, 28.7, 29.1, 27.4, 22.3, 13.2, 22.5, 25.0, 1, 6.6, 23.7, 23.5, 17.3, 24.6, 27.8, 29.7, 25.3, 19.9, 18.2, 26.2, 20.4, 23.3, 26.7, 26.0, 1, 25.1, 33.1, 35.0, 25.3, 23.6, 23.2, 20.2, 24.7, 22.6, 39.1, 26.5, 22.7};
		assertTrue(KolmogorovSmirnovTest.runSmirnov(redwell, whitney) == 0.2204113924050633);
	}
	
	@Test
	public void BCTestUndirected() throws IOException, Error {
		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(300, 20, 200, EdgeType.UNDIRECTED);
		population = gL.loadGraph();

		Map<String, Double> a1 = (new BCAnalyzer()).analyzeGraph(population, FileSystem.findOpenPath("bc").toString());
		Map<String, Double> a2 = (new BCAnalyzer()).analyzeGraph(population, FileSystem.findOpenPath("bc").toString());

//		HashMap<String, Double> a1 = GraphAnalyzers.BCAnalyzer.analyzeGraphBC(population, FileSystem.findOpenPath("bc").toString());
//		HashMap<String, Double> a2 = GraphAnalyzers.BCAnalyzer.analyzeGraphBC(population, FileSystem.findOpenPath("bc").toString());		
		assertTrue (a1.entrySet().containsAll(a2.entrySet()));
		
		RNDBFSSampler sampler = new RNDBFSSampler(1.0, 0, population.getDefaultEdgeType());
		sampler.sampleGraph(population);
		Graph<String, String> sample = sampler.getGraph();
		a2 = (new BCAnalyzer()).analyzeGraph(sample, FileSystem.findOpenPath("bc").toString());

		JUnitUtils.checkGraphs(sample, population);
		List<Double> a1V = new ArrayList<Double>(a1.values());
		List<Double> a2V = new ArrayList<Double>(a2.values());
		Collections.sort(a1V);
		Collections.sort(a2V);
		List<Double> a3V = new ArrayList<Double>(a1V);
		
		// Check to see if the smirnov test is working
		
		double[] a1d = new double[a1V.size()];
		double[] a2d = new double[a2V.size()];
		for (int i = 0; i < a1d.length; i++) {
			a1d[i] = a1V.get(i);
			a2d[i] = a2V.get(i);
		}
		
		System.out.println(KolmogorovSmirnovTest.runSmirnov(a1d, a2d));
		
		JUnitUtils.removeAllWithTolerance(a1V, a2V, 0);
		JUnitUtils.removeAllWithTolerance(a2V, a3V, 0);
		
		assertTrue (a1V.isEmpty());
		assertTrue (a2V.isEmpty());
	}
	
	
	@Test
	public void BCTestDirected() throws IOException, Error {
		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(300, 20, 200, EdgeType.DIRECTED);
		population = gL.loadGraph();

		Map<String, Double> a1 = (new BCAnalyzer()).analyzeGraph(population, FileSystem.findOpenPath("bc").toString());
		Map<String, Double> a2 = (new BCAnalyzer()).analyzeGraph(population, FileSystem.findOpenPath("bc").toString());

//		HashMap<String, Double> a1 = GraphAnalyzers.BCAnalyzer.analyzeGraphBC(population, FileSystem.findOpenPath("bc").toString());
//		HashMap<String, Double> a2 = GraphAnalyzers.BCAnalyzer.analyzeGraphBC(population, FileSystem.findOpenPath("bc").toString());		
		assertTrue (a1.entrySet().containsAll(a2.entrySet()));
		
		RNDBFSSampler sampler = new RNDBFSSampler(1.0, 0, population.getDefaultEdgeType());
		sampler.sampleGraph(population);
		Graph<String, String> sample = sampler.getGraph();
		a2 = (new BCAnalyzer()).analyzeGraph(sample, FileSystem.findOpenPath("bc").toString());

		JUnitUtils.checkGraphs(sample, population);
		List<Double> a1V = new ArrayList<Double>(a1.values());
		List<Double> a2V = new ArrayList<Double>(a2.values());
		Collections.sort(a1V);
		Collections.sort(a2V);
		List<Double> a3V = new ArrayList<Double>(a1V);
		
		// Check to see if the smirnov test is working
		
		double[] a1d = new double[a1V.size()];
		double[] a2d = new double[a2V.size()];
		for (int i = 0; i < a1d.length; i++) {
			a1d[i] = a1V.get(i);
			a2d[i] = a2V.get(i);
		}
		
		System.out.println(KolmogorovSmirnovTest.runSmirnov(a1d, a2d));
		
		JUnitUtils.removeAllWithTolerance(a1V, a2V, 0);
		JUnitUtils.removeAllWithTolerance(a2V, a3V, 0);
		
		assertTrue (a1V.isEmpty());
		assertTrue (a2V.isEmpty());
	}

}
