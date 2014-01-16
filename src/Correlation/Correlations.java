package Correlation;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

public class Correlations {
	
	/**
	 * Takes in a population and sample BC array and computes the pearson correlation.
	 * @param popBC - a list of all the pop BCs in question
	 * @param sampleBCs - a list of all the sample BCs in question
	 * @return a list of correlation values for each sample to the population
	 */
	public static double pearsonsCorrelation(double[] series1, double[] series2) {
		// Now process the values and generate the correlation
		PearsonsCorrelation scr = new PearsonsCorrelation();
		return scr.correlation(series1, series2);
	}
		
	/**
	 * Takes in a population and sample BC array and computes the various correlations.
	 * @param popBC - a list of all the pop BCs in question
	 * @param sampleBCs - a list of all the sample BCs in question
	 * @return a list of correlation values for each sample to the population
	 */
	public static double spearmansCorrelation(double[] series1, double[] series2) {
		// Now process the values and generate the correlation
		SpearmansCorrelation scr = new SpearmansCorrelation();
		return scr.correlation(series1, series2);
	}
	
	/**
	 * Calculates the average error of series
	 * @param trueSeries
	 * @param sampleSeries
	 * @return
	 */
	public static double errorCalculation(double[] trueSeries, double[] sampleSeries) {
		double error = 0.0;
		for (int i = 0; i < trueSeries.length; i++) {
			error += Math.pow(trueSeries[i] - sampleSeries[i], 2);
		}
		return (Math.sqrt(error))/trueSeries.length;
	}
	
}