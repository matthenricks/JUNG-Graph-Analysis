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
	
	static List<Comparator<Entry<String, Double[]>>> BCComparisons = new ArrayList<Comparator<Entry<String, Double[]>>>();
	static {
		BCComparisons.add(new Comparator<Entry<String, Double[]>>() {
			@Override
			public int compare(Entry<String, Double[]> arg0,
					Entry<String, Double[]> arg1) {
				Double comp = (arg0.getValue()[0] - arg1.getValue()[0]);
				if (comp == 0) 
					return 0;
				else 
					return (comp > 0) ? 1 : -1;
			}
		});
		BCComparisons.add(new Comparator<Entry<String, Double[]>>() {
			@Override
			public int compare(Entry<String, Double[]> arg0,
					Entry<String, Double[]> arg1) {
				Double comp = (arg0.getValue()[1] - arg1.getValue()[1]);
				if (comp == 0) 
					return 0;
				else 
					return (comp > 0) ? 1 : -1;
			}
		});
	};
	
	public static class BCCorrelationThread implements Callable<double[]> {

		ConcurrentHashMap<String, Double> popBC;
		HashMap<String, Double[]> sampleBC;
		int nodeNum;
		/**
		 * Constructor for the BCCorrelation function
		 * @param popBC
		 * @param sampleBC
		 * @param nodeNum
		 */
		public BCCorrelationThread(ConcurrentHashMap<String, Double> popBC, HashMap<String, Double[]> sampleBC, int nodeNum) {
			this.popBC = popBC;
			this.sampleBC = sampleBC;
			this.nodeNum = nodeNum;
			
			// Run checks
			if (sampleBC.keySet().size() < nodeNum)
				throw new Error("Sample cannot be less the the nodes being run");
		}
		
		@Override
		public double[] call() throws Exception {
			// First get the smallest values from the sample set for population BC
			List<Entry<String, Double[]>> sampleValues = new ArrayList<Entry<String, Double[]>>();
			for (Entry<String, Double[]> value : sampleBC.entrySet())
				sampleValues.add(value);
			
			double[] correlations = new double[BCComparisons.size()];
			
			// Obtain the selected values from the big sets and generate correlations from them
			// Additionally, plot them
			for (int i = 0; i < BCComparisons.size(); i++) {
				// Resort the sample to get the right values
				Collections.sort(sampleValues, BCComparisons.get(i));
				
				// Obtain the values of the population sample and the values of the two doubles
				double[] popValues = new double[sampleValues.size()];
				double[] sample = new double[sampleValues.size()];
				
				// Add the selected values
				for (int counter = 0; counter < sampleValues.size(); counter++) {
					popValues[counter] = popBC.get(sampleValues.get(counter).getKey());
					sample[counter] = sampleValues.get(counter).getValue()[i];
				}
				
				// Compute the correlation
				correlations[i] = Correlation.Correlations.spearmansCorrelation(popValues, sample);
			}
			
			return correlations;
		}
		
	};
	
	/**
	 * Obtains the correlation values of each BC sample in question and plots the samples against the population
	 * @param popBC
	 * @param sampleBC
	 * @param n - the number of nodes to be compared
	 */
	public static double[] runBCComparison(ConcurrentHashMap<String, Double> popBC, HashMap<String, Double[]> sampleBC, int n) {
		
		// First get the smallest values from the sample set for population BC
		List<Entry<String, Double[]>> sampleValues = new ArrayList<Entry<String, Double[]>>();
		for (Entry<String, Double[]> value : sampleBC.entrySet())
			sampleValues.add(value);
		
		double[] correlations = new double[BCComparisons.size()];
		
		// Obtain the selected values from the big sets and generate correlations from them
		// Additionally, plot them
		for (int i = 0; i < BCComparisons.size(); i++) {
			// Resort the sample to get the right values
			Collections.sort(sampleValues, BCComparisons.get(i));
			
			// Obtain the values of the population sample and the values of the two doubles
			double[] popValues = new double[sampleValues.size()];
			double[] sample = new double[sampleValues.size()];
			
			// Add the selected values
			for (int counter = 0; counter < sampleValues.size(); counter++) {
				popValues[i] = popBC.get(sampleValues.get(i).getKey());
				sample[i] = sampleValues.get(i).getValue()[0];
			}
			
			// Compute the correlation
			correlations[i] = Correlation.Correlations.spearmansCorrelation(popValues, sample);
			
			// Now plot them!
			// TODO: 
		}
		
		return correlations;
	}
}
