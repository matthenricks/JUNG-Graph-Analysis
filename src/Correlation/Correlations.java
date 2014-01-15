package Correlation;

import java.util.List;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

public class Correlations {
	
	/**
	 * Takes in a population BC array and a set of sample BC values and computes the pearsons correlations.
	 * @param popBC - a list of all the pop BCs in question
	 * @param sampleBCs - a list of all the sample lists of BCs in question
	 * @return a list of correlation values for each sample to the population
	 */
	public static double[] pearsonsCorrelation(double[] popBC, List<double[]> sampleBCs) {
		// Now process the values and generate the correlation
		double[] correlations = new double[sampleBCs.size()];
		PearsonsCorrelation scr = new PearsonsCorrelation();
		for (int i = 0; i < sampleBCs.size(); i++) {
			correlations[i] = scr.correlation(popBC, sampleBCs.get(i));
		}

		return correlations;
	}
	
	/**
	 * Takes in a population and sample BC array and computes the pearson correlation.
	 * @param popBC - a list of all the pop BCs in question
	 * @param sampleBCs - a list of all the sample BCs in question
	 * @return a list of correlation values for each sample to the population
	 */
	public static double pearsonsCorrelation(double[] popBC, double[] sampleBC) {
		// Now process the values and generate the correlation
		PearsonsCorrelation scr = new PearsonsCorrelation();
		return scr.correlation(popBC, sampleBC);
	}
	
	/**
	 * Takes in a population BC array and a set of sample BC values and computes the various correlations.
	 * @param popBC - a list of all the pop BCs in question
	 * @param sampleBCs - a list of all the sample lists of BCs in question
	 * @return a list of correlation values for each sample to the population
	 */
	public static double[] spearmansCorrelation(double[] popBC, List<double[]> sampleBCs) {
		// Now process the values and generate the correlation
		double[] correlations = new double[sampleBCs.size()];
		SpearmansCorrelation scr = new SpearmansCorrelation();
		for (int i = 0; i < sampleBCs.size(); i++) {
			correlations[i] = scr.correlation(popBC, sampleBCs.get(i));
		}

		return correlations;
	}
	
	/**
	 * Takes in a population and sample BC array and computes the various correlations.
	 * @param popBC - a list of all the pop BCs in question
	 * @param sampleBCs - a list of all the sample BCs in question
	 * @return a list of correlation values for each sample to the population
	 */
	public static double spearmansCorrelation(double[] popBC, double[] sampleBC) {
		// Now process the values and generate the correlation
		SpearmansCorrelation scr = new SpearmansCorrelation();
		return scr.correlation(popBC, sampleBC);
	}
	
	
}