package Correlation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that holds the function to correlate a population and sample BC score
 * Currently, all methods are static
 * @author MOREPOWER
 *
 */
public class BCCorrelator {
	
	public class BCCorrelationThread implements Callable<double[]> {

		ConcurrentHashMap<String, Double> popBC;
		HashMap<String, Double> sampleBC;
		int nodeNum;
		/**
		 * Constructor for the BCCorrelation function
		 * @param popBC
		 * @param sampleBC
		 * @param nodeNum
		 */
		public BCCorrelationThread(ConcurrentHashMap<String, Double> popBC, HashMap<String, Double> sampleBC, int nodeNum) {
			this.popBC = popBC;
			this.sampleBC = sampleBC;
			this.nodeNum = nodeNum;
			
			// Run checks
			if (sampleBC.keySet().size() < nodeNum)
				throw new Error("Sample cannot be less the the nodes being run");
		}
		
		@Override
		public double[] call() throws Exception {
			return BCCorrelator.runBCComparison(popBC, sampleBC, nodeNum);
		}
	};
	
	/**
	 * Obtains the correlation values of each BC sample in question
	 * @param popBC
	 * @param sampleBC
	 * @param n - the number of nodes to be compared
	 */
	public static double[] runBCComparison(ConcurrentHashMap<String, Double> popBC, HashMap<String, Double> sampleBC, int nodeNum) {
		
		// First convert the set into a list so it can be ordered from greatest->smallest
		List<Entry<String, Double>> sampleValues = new ArrayList<Entry<String, Double>>(sampleBC.entrySet());
		Collections.sort(sampleValues, new Comparator<Entry<String, Double>>() {
			@Override
			public int compare(Entry<String, Double> arg0,
					Entry<String, Double> arg1) {
				return arg1.getValue().compareTo(arg0.getValue());
			}
		});
			
		// Obtain the values of the population sample and the values of the two doubles
		double[] popValues = new double[nodeNum];
		double[] sample = new double[nodeNum];
		
		// Add the greatest selected values
		for (int counter = 0; counter < nodeNum; counter++) {
			if(sampleValues.get(counter) == null)
				System.out.print("CHECK ME\n");
			popValues[counter] = popBC.get(sampleValues.get(counter).getKey());
			sample[counter] = sampleValues.get(counter).getValue();
		}
		
		double[] retVal = {
				Correlations.spearmansCorrelation(popValues, sample),
				Correlations.pearsonsCorrelation(popValues, sample),
				Correlations.errorCalculation(popValues, sample)
		};
		
		return retVal;
	}
}
