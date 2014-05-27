package DataAnalyzers;

import java.util.Set;

/**
 * Class that contains custom precision, recall, and F-Measure functions
 * 
 * @author MOREPOWER
 *
 */
public class PrecisionRecallF {
	
	/**
	 * Given a set of reference values and a set of test values, return the fraction of test values that appear in the reference set. 
	 * In particular, return |reference&test|/|test|. If test is empty, then return None.
	 * @param population
	 * @param sample
	 * @return
	 */
	public static double Precision(Set<String> reference, Set<String> test) {
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
	public static double Recall(Set<String> reference, Set<String> test) {
		int common = 0;
		for (String entry : reference) {
			if (test.contains(entry))
				common++;
		}
		return (double)common / (double)reference.size();
	}
	
	/**
	 * Calculates the F-Measure based off the precision and recall values with a weight.
	 * The weight is a ratio that increases or decreases the importance of precision.
	 *   A formal equation can be found on Wikipedia: http://en.wikipedia.org/wiki/Precision_and_recall
	 * Note: This function is unsafe for precision/recall values of 0 and for alpha <= 0
	 * @param precision
	 * @param recall
	 * @param alpha
	 * @return the F-Measure
	 */
	public static double FMeasure(double precision, double recall, double alpha) {
		return 1/(alpha/precision + (1-alpha)/recall);
	}
}
