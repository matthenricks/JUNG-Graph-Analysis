package SamplingAlgorithms;

/***
 * This class is a sampling algorithm that samples a graph with a mixture of BFS and Random Node sampling in addition to a max degree taken bias
 * @author MOREPOWER
 *
 */
public class RDBFSSample {

	double alpha, threshold;
	int maxDegree, seed;

	// TODO: Make this function. It will use Max Degree as a limiter for how much it should take
	// NOTE: Max Degree is a number for how many neighbors it will take. This will bias it towards taking 
	// more influential nodes since they'll be more chances and reduce the overall network size as an upper bound
	// Upper Bound = (#nodes * alpha) * (maxDegree+1)
	public RDBFSSample() {
		
	}
}
