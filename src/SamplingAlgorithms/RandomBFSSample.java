package SamplingAlgorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class RandomBFSSample implements SampleMethod {

	double alpha, threshold;
	int seed;
	
	public RandomBFSSample(double alpha, double threshold, int seed) {
		this.alpha = alpha;
		this.threshold = threshold;
		this.seed = seed;
	}
	public RandomBFSSample(double alpha, double threshold) {
		this.alpha = alpha;
		this.threshold = threshold;
		seed = (new Random()).nextInt();
	}

	// TODO: This may need some Spring cleaning
	public Graph<String, String> sampleGraph(Graph<String, String> parentGraph) {
		Random rand = new Random(seed);
		double prob = rand.nextDouble();
		List<String> vertices = new ArrayList<String>(parentGraph.getVertices());
		Collections.shuffle((vertices)); 
		Graph<String, String> sampledBFSGraph = new UndirectedSparseGraph<String, String>();
		Collection<String> neighbors = new TreeSet<String>(); 
		ArrayList<String> removevertex = new ArrayList<String>();
		// seed set
		removevertex.add(vertices.get(0));
		//System.out.println(vertices.get(0));
		neighbors = parentGraph.getNeighbors(vertices.get(0));
		//System.out.println(neighbors);
		sampledBFSGraph.addVertex(vertices.get(0));
		for(String vertex: neighbors) {
			sampledBFSGraph.addVertex(vertex);
			sampledBFSGraph.addEdge(parentGraph.findEdge(vertices.get(0), vertex), vertices.get(0), vertex);	
		}
		
		for(int i = 1; i < Math.floor(alpha*parentGraph.getVertexCount()); i++) { 
			prob = rand.nextDouble();
			if(prob < threshold) { //FROM HERE, FIX PROBABILITY
				//System.out.println(vertices.get(i));				
				neighbors = parentGraph.getNeighbors(vertices.get(i));
				//System.out.println(neighbors); 
				removevertex.add(vertices.get(i)); 
				sampledBFSGraph.addVertex(vertices.get(i));
				for(String vertex: neighbors) {
					sampledBFSGraph.addVertex(vertex);
					boolean copy = false;
					for(String edge: sampledBFSGraph.getEdges()) {
						if(edge.equalsIgnoreCase(parentGraph.findEdge(vertices.get(i), vertex))) {
							copy = true;
							break;
						}
					}
					if(copy == false) {
						sampledBFSGraph.addEdge(parentGraph.findEdge(vertices.get(i), vertex), vertices.get(i), vertex);
					}
				}
				//System.out.println("samping size: " + Math.floor(sampledBFSGraph.getVertexCount()));
			}	 else {
				List<String> bfsvertices = new ArrayList<String>(sampledBFSGraph.getVertices());
				for(int n=0; n < removevertex.size(); n++){
					for(int m=0; m<sampledBFSGraph.getVertexCount(); m++){
						if(removevertex.get(n).equalsIgnoreCase(bfsvertices.get(m))){
							bfsvertices.remove(m);
							m-=1;
							break;
						}
					}
				}
				Collections.shuffle((bfsvertices));	//  randomization of unique vertices.				
				String bfsvertex = bfsvertices.get(0);
				sampledBFSGraph.addVertex(bfsvertex);
				neighbors = parentGraph.getNeighbors(bfsvertex);				
				removevertex.add(bfsvertex);
				for(String vertex: neighbors) {
					sampledBFSGraph.addVertex(vertex);
					boolean copy = false;
					for(String edge: sampledBFSGraph.getEdges()) {
						if(edge.equalsIgnoreCase(parentGraph.findEdge(bfsvertex, vertex))) {
							copy = true;
							break;
						}
					}
					if(copy == false) {
					sampledBFSGraph.addEdge(parentGraph.findEdge(bfsvertices.get(0), vertex), bfsvertices.get(0), vertex);	
					}
				}
				//System.out.println("samping size: " + Math.floor(sampledBFSGraph.getVertexCount()));
			}
		}
		return sampledBFSGraph;
	}
}
