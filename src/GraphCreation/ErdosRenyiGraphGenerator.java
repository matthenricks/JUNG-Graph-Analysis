package GraphCreation;

import java.io.IOException;

import org.apache.commons.collections15.Factory;

import edu.uci.ics.jung.algorithms.generators.random.ErdosRenyiGenerator;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

/**
 * Generates a random graph using the Erdos-Renyi binomial model (each pair of vertices is connected with probability p)
 * @author MOREPOWER
 *
 */
public class ErdosRenyiGraphGenerator implements GraphLoader {

	// Used to store the probability and number of nodes this will be created from
	// expected number of edges = prob*C(n,2)
	protected double probability;
	protected int vertNumber;
	// TODO: @Shankar, why is this the way it is? Is there a better way to name?
	protected int count = 0;

	// Factory's to help control the naming convention and creation of vertexes/edges
	Factory<String> vertexFactory = new Factory<String>() {			
		@Override
		public String create() {
			count++;
			return Integer.toString(count); 
		}
		
	}; 
	Factory<String> edgeFactory = new Factory<String>() {
		@Override
		public String create() {
			count++;
			return Integer.toString(count);
		}
		
	};
	Factory<UndirectedGraph<String, String>> gFactory = new Factory<UndirectedGraph<String,String>>() {			
		@Override
		public UndirectedGraph<String, String> create() {
			// TODO Auto-generated method stub
			return new UndirectedSparseGraph<String, String>();
		}
	};
	
	/**
	 * Public constructor for the class, just a variable setter
	 * @param p - the probability of an edge being created
	 * @param n - the number of nodes that will be created
	 */
	public ErdosRenyiGraphGenerator(double p, int n) {
		probability = p;
		vertNumber = n;
	}
	
	public Graph<String, String> loadGraph() throws Error, IOException {
		ErdosRenyiGenerator<String, String> erg = new ErdosRenyiGenerator<String, String>
			(gFactory, vertexFactory, edgeFactory, vertNumber, probability);
		return erg.create();
	}

}