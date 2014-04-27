package Correlation;

import java.util.HashSet;

import Utils.CSV_Builder;
import Utils.CSV_Builder_Objects;

public class PrecisionRecallF {
	
	// TODO: Could enhance efficiency by first sorting both arrays, then finding the intersection and dividing by each individually
	
	/**
	 * Calculate the precision and recall
	 * @param population
	 * @param sample
	 * @return a CSV Builder of composition: [Precision, Recall]
	 */
	public static CSV_Builder PR(HashSet<String> reference, HashSet<String> test) {
		return new CSV_Builder(new CSV_Builder_Objects.CSV_Double(precision(reference, test)), 
					new CSV_Builder(new CSV_Builder_Objects.CSV_Double(recall(reference, test)))
				);
	}
	
	/**
	 * Given a set of reference values and a set of test values, return the fraction of test values that appear in the reference set. 
	 * In particular, return |reference&test|/|test|. If test is empty, then return None.
	 * @param population
	 * @param sample
	 * @return
	 */
	public static double precision(HashSet<String> reference, HashSet<String> test) {
		int common = 0;
		for (String entry : test) {
			if (reference.contains(entry))
				common++;
		}
		return (double)common / (double)test.size();
	}
	
	/**
	 * Given a set of reference values and a set of test values, return the fraction of reference values that appear in the test set. 
	 * In particular, return |reference&test|/|reference|. If reference is empty, then return None.
	 * @param reference
	 * @param test
	 * @return
	 */
	public static double recall(HashSet<String> reference, HashSet<String> test) {
		int common = 0;
		for (String entry : reference) {
			if (test.contains(entry))
				common++;
		}
		return (double)common / (double)reference.size();
	}
	
	/**
	 * Calculates the F based off the precision and recall values
	 * @param precision
	 * @param recall
	 * @param alpha
	 * @return
	 */
	public static double F(double precision, double recall, double alpha) {
		return 1/(alpha/precision + (1-alpha)/recall);
	}
}
