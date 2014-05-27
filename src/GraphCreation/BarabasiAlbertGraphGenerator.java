package GraphCreation;

import java.io.IOException;
import java.util.TreeSet;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.algorithms.generators.random.BarabasiAlbertGenerator;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * Graph Generator using the BarabasiAlbert model. Wrapped from JUNG. Supports both UNDIRECTED and DIRECTED graphs
 * BarabasiAlbert is an evolving scale-free random graph generator. At each time step, a new vertex is created and is connected to existing 
 * vertices according to the principle of "preferential attachment", whereby vertices with higher degree have a higher probability of being selected for attachment
 * @author MOREPOWER
 *
 */
public class BarabasiAlbertGraphGenerator extends GraphLoader implements GeneratedGraph {

	static Factory<Graph<String, String>> FactoryDirected = new Factory<Graph<String,String>>() {
		@Override
		public Graph<String, String> create() {
			return new DirectedSparseGraph<String, String>();
		}
	};
		
	static Factory<Graph<String, String>> FactoryUndirected = new Factory<Graph<String,String>>() {
		@Override
		public Graph<String, String> create() {
			return new UndirectedSparseGraph<String, String>();
		}
	};
		
	Factory<Graph<String, String>> gFactory;
	int iVerts, nEdges, tSteps;
	
	/**
	 * Initial variable setters needed to run the Generator.
	 * @param initialVerts - initial verts to be added without connection to the graph.
	 * @param addedEdgesPerStep - edges added per step, for all steps leading, a random generator will be used
	 * @param totalSteps - the amount of additional vertexes and edge additions that will be added
	 * @param graphType
	 */
	public BarabasiAlbertGraphGenerator(int initialVerts, int addedEdgesPerStep, int totalSteps, EdgeType graphType) {
		
		if (initialVerts < addedEdgesPerStep) throw new Error("Initial Vertexes must be > added Edges per step");
		iVerts = initialVerts;
		nEdges = addedEdgesPerStep;
		tSteps = totalSteps;
		
		// Decide upon the graph type
		if (graphType == EdgeType.DIRECTED) {
			gFactory = FactoryDirected;
		} else if (graphType == EdgeType.UNDIRECTED) {
			gFactory = FactoryUndirected;
		} else {
			throw new Error("Unrecognized edge type");
		}
	}
	
	/**
	 * Starts with a graph of the initial vertexes (iVerts) and then adds another with nEdges tSteps times.
	 */
	public Graph<String, String> loadGraph() throws Error, IOException {
		/**
		 * There is no reason to track the initial nodes (new TreeSet<String>())
		 * tVerts are the initial nodes added
		 * nEdges are the edges added each step, one step makes this the max
		 */
		BarabasiAlbertGenerator<String, String> erg = new BarabasiAlbertGenerator<String, String>
			(gFactory, vertexFactory, edgeFactory, iVerts, nEdges, new TreeSet<String>());

		erg.evolveGraph(tSteps);
		return erg.create();
	}

	@Override
	public String getInformation() {
		return "Barabasi Graph built from :\n" +
				"\tInitial Verts: " +  iVerts + "\n" + 
				"\tNumber of Initial Edges: " +  nEdges + "\n" +
				"\tTotal Steps: " +  tSteps + "\n";
	}

}