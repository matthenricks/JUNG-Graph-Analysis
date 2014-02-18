package GraphCreation;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.algorithms.generators.random.EppsteinPowerLawGenerator;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class EppsteinPowerLawGraphGenerator implements GraphLoader {

	int vCount = 0, eCount = 0;
	
	static Factory<Graph<String, String>> FactoryUndirected = new Factory<Graph<String,String>>() {
		@Override
		public Graph<String, String> create() {
			return new UndirectedSparseGraph<String, String>();
		}
	};	
	// Factory's to help control the naming convention and creation of vertexes/edges
	Factory<String> vertexFactory = new Factory<String>() {			
		@Override
		public String create() {
			vCount++;
			return "v" + Integer.toString(vCount); 
		}
		
	}; 
	Factory<String> edgeFactory = new Factory<String>() {
		@Override
		public String create() {
			eCount++;
			return "e" + Integer.toString(eCount);
		}		
	};
	
	
	
	// Must be UNDIRECTED
	Graph<String, String> sampleGraph;
	int numVerts, numEdges, seed;
	
	public EppsteinPowerLawGraphGenerator(int vertexes, int edges, int seed) {		
		numVerts = vertexes;
		numEdges = edges;
		this.seed = seed;
		
		throw new Error("UNFINISHED CLASS");
	}
	
	public EppsteinPowerLawGraphGenerator(int vertexes, int edges) {
		this(vertexes, edges, (new Random()).nextInt());
	}
	
	@Override
	public Graph<String, String> loadGraph() throws Error, IOException {

		EppsteinPowerLawGenerator<String, String> graphGen = new EppsteinPowerLawGenerator<String, String>(
				EppsteinPowerLawGraphGenerator.FactoryUndirected,
				this.vertexFactory,
				this.edgeFactory,
				numVerts,
				numEdges,
				seed
			);
		
		return graphGen.create();
	}

	@Override
	public String getInformation() {
		// TODO
		return null;
	}

}