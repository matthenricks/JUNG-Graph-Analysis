package JUnitTests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import GraphAnalyzers.BCAnalyzer;
import GraphCreation.BarabasiAlbertGraphGenerator;
import GraphCreation.GraphLoader;
import SamplingAlgorithms.RNDBFSSampler;
import SamplingAlgorithms.RNDBFSSingleSampler;
import SamplingAlgorithms.RNDWalkSampler;
import Utils.FileSystem;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;


public class SampleGraphs {

	/**
	 * Testing for the newer version of the old method
	 */
	@Test
	public void TestNewSample() throws IOException, Error {
		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(200, 10, 50, EdgeType.DIRECTED);
		population = gL.loadGraph();
		
		RNDBFSSampler sampler = new RNDBFSSingleSampler(1.0, 0.4, EdgeType.DIRECTED);
		System.out.println(sampler.sampleGraph(population).getCSV());
		Graph<String, String> sample = sampler.getGraph();

		JUnitUtils.checkGraphs(sample, population);
		
		gL = new BarabasiAlbertGraphGenerator(200, 10, 50, EdgeType.UNDIRECTED);
		population = gL.loadGraph();
		
		sampler = new RNDBFSSingleSampler(1.0, 0.4, EdgeType.UNDIRECTED);
		System.out.println(sampler.sampleGraph(population).getCSV());
		sample = sampler.getGraph();

		JUnitUtils.checkGraphs(sample, population);
	}
	
	
	@Test
	public void TestRndWalkSample() throws IOException, Error {
		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(200, 10, 50, EdgeType.DIRECTED);
		population = gL.loadGraph();
		
		RNDWalkSampler sampler = new RNDWalkSampler(1.0, 1.0, EdgeType.DIRECTED);
		System.out.println(sampler.sampleGraph(population).getCSV());
		Graph<String, String> sample = sampler.getGraph();

		JUnitUtils.checkGraphs(sample, population);
		
		gL = new BarabasiAlbertGraphGenerator(200, 10, 50, EdgeType.UNDIRECTED);
		population = gL.loadGraph();
		
		sampler = new RNDWalkSampler(1.0, 1.0, EdgeType.UNDIRECTED);
		System.out.println(sampler.sampleGraph(population).getCSV());
		sample = sampler.getGraph();

		JUnitUtils.checkGraphs(sample, population);
	}
	
	
	/**
	 * Sample a graph completely and check if they're the same. Looks at vertexes in addition to edges
	 * @throws Error 
	 * @throws IOException 
	 */
	@Test
	public void TestDirectedGraph() throws IOException, Error {
		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(200, 10, 50, EdgeType.DIRECTED);
		population = gL.loadGraph();
		
		RNDBFSSampler sampler = new RNDBFSSampler(1.0, 0.4, EdgeType.DIRECTED);
		System.out.println(sampler.sampleGraph(population).getCSV());
		Graph<String, String> sample = sampler.getGraph();

		JUnitUtils.checkGraphs(sample, population);
	}
	
	/**
	 * Sample a graph completely and check if they're the same. Looks at vertexes in addition to edges
	 * @throws Error 
	 * @throws IOException 
	 */
	@Test
	public void TestSampling() throws IOException, Error {
		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(200, 10, 50, EdgeType.DIRECTED);
		population = gL.loadGraph();
		
		
		RNDBFSSampler sampler = new RNDBFSSampler(0.1, 0.4, population.getDefaultEdgeType());
		System.out.println(sampler.sampleGraph(population).getCSV());
		Graph<String, String> sample = sampler.getGraph();
		
		Collection<String> sampleVertexes = sample.getVertices();
		
		assertTrue (Math.abs(sample.getVertexCount() - population.getVertexCount()*0.1) < 1);
		
		sampler.changeAlpha(0.4);
		System.out.println(sampler.sampleGraph(population).getCSV());
		Graph<String, String> sample2 = sampler.getGraph();
		
		assertTrue (Math.abs(sample.getVertexCount() - population.getVertexCount()*0.4) < 1);
		
		assertTrue (sample2.getVertices().containsAll(sampleVertexes));
		
		// This should be true, since getGraph() only returns a reference
		JUnitUtils.checkGraphs(sample, sample2);
	}
	
	@Test
	public void TestUndirectedGraph() throws IOException, Error {
		Graph<String, String> population;
		GraphLoader gL = new BarabasiAlbertGraphGenerator(200, 10, 50, EdgeType.UNDIRECTED);
		population = gL.loadGraph();
		
		RNDBFSSampler sampler = new RNDBFSSampler(1.0, 0.4, population.getDefaultEdgeType());
		System.out.println(sampler.sampleGraph(population).getCSV());
		Graph<String, String> sample = sampler.getGraph();
		
		JUnitUtils.checkGraphs(sample, population);
		
		Map<String, Double> g2 = (new BCAnalyzer()).analyzeGraph(sample, FileSystem.findOpenPath("bc2").toString());
		Map<String, Double> g1 = (new BCAnalyzer()).analyzeGraph(population, FileSystem.findOpenPath("bc1").toString());
		Map<String, Double> g3 = new HashMap<String, Double>();
		g3.putAll(g2);
		
		Set<Entry<String, Double>> s1 = g1.entrySet();
		Set<Entry<String, Double>> s2 = g2.entrySet();
		
		// v228=171.3865
		// v228=342.7731
		
		assertTrue(s2.removeAll(s1));
		assertTrue(s1.removeAll(g3.entrySet()));
		
		assertTrue(s1.size() == 0);
		assertTrue(s2.size() == 0);
	}	
}
