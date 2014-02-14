package Correlation;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

public class Correlations {
	
	/**
     * Computes the Kendall's Tau rank correlation coefficient between the two arrays.
     *
     * @param xArray first data array
     * @param yArray second data array
     * @return Returns Kendall's Tau rank correlation coefficient for the two arrays
     * @throws DimensionMismatchException if the arrays lengths do not match
     * @credit: http://commons.apache.org/proper/commons-math/jacoco/org.apache.commons.math3.stat.correlation/KendallsCorrelation.java.html#L158
     */
	public static double kendallsCorrelation(double[] xArray, double[] yArray) throws DimensionMismatchException {

		if (xArray.length != yArray.length) {
            throw new DimensionMismatchException(xArray.length, yArray.length);
        }

        final int n = xArray.length;
        final int numPairs = n * (n - 1) / 2;

        @SuppressWarnings("unchecked")
        Pair<Double, Double>[] pairs = new Pair[n];
        for (int i = 0; i < n; i++) {
            pairs[i] = new Pair<Double, Double>(xArray[i], yArray[i]);
        }

        Arrays.sort(pairs, new Comparator<Pair<Double, Double>>() {
            @Override
            public int compare(Pair<Double, Double> pair1, Pair<Double, Double> pair2) {
                int compareFirst = pair1.getFirst().compareTo(pair2.getFirst());
                return compareFirst != 0 ? compareFirst : pair1.getSecond().compareTo(pair2.getSecond());
            }
        });

        int tiedXPairs = 0;
        int tiedXYPairs = 0;
        int consecutiveXTies = 1;
        int consecutiveXYTies = 1;
        Pair<Double, Double> prev = pairs[0];
        for (int i = 1; i < n; i++) {
            final Pair<Double, Double> curr = pairs[i];
            if (curr.getFirst().equals(prev.getFirst())) {
                consecutiveXTies++;
                if (curr.getSecond().equals(prev.getSecond())) {
                    consecutiveXYTies++;
                } else {
                    tiedXYPairs += consecutiveXYTies * (consecutiveXYTies - 1) / 2;
                    consecutiveXYTies = 1;
                }
            } else {
                tiedXPairs += consecutiveXTies * (consecutiveXTies - 1) / 2;
                consecutiveXTies = 1;
                tiedXYPairs += consecutiveXYTies * (consecutiveXYTies - 1) / 2;
                consecutiveXYTies = 1;
            }
            prev = curr;
        }
        tiedXPairs += consecutiveXTies * (consecutiveXTies - 1) / 2;
        tiedXYPairs += consecutiveXYTies * (consecutiveXYTies - 1) / 2;

        int swaps = 0;
        @SuppressWarnings("unchecked")
        Pair<Double, Double>[] pairsDestination = new Pair[n];
        for (int segmentSize = 1; segmentSize < n; segmentSize <<= 1) {
            for (int offset = 0; offset < n; offset += 2 * segmentSize) {
                int i = offset;
                final int iEnd = FastMath.min(i + segmentSize, n);
                int j = iEnd;
                final int jEnd = FastMath.min(j + segmentSize, n);

                int copyLocation = offset;
                while (i < iEnd || j < jEnd) {
                    if (i < iEnd) {
                        if (j < jEnd) {
                            if (pairs[i].getSecond().compareTo(pairs[j].getSecond()) <= 0) {
                                pairsDestination[copyLocation] = pairs[i];
                                i++;
                            } else {
                                pairsDestination[copyLocation] = pairs[j];
                                j++;
                                swaps += iEnd - i;
                            }
                        } else {
                            pairsDestination[copyLocation] = pairs[i];
                            i++;
                        }
                    } else {
                        pairsDestination[copyLocation] = pairs[j];
                        j++;
                    }
                    copyLocation++;
                }
            }
            final Pair<Double, Double>[] pairsTemp = pairs;
            pairs = pairsDestination;
            pairsDestination = pairsTemp;
        }

        int tiedYPairs = 0;
        int consecutiveYTies = 1;
        prev = pairs[0];
        for (int i = 1; i < n; i++) {
            final Pair<Double, Double> curr = pairs[i];
            if (curr.getSecond().equals(prev.getSecond())) {
                consecutiveYTies++;
            } else {
                tiedYPairs += consecutiveYTies * (consecutiveYTies - 1) / 2;
                consecutiveYTies = 1;
            }
            prev = curr;
        }
        tiedYPairs += consecutiveYTies * (consecutiveYTies - 1) / 2;

        // Added Divisor Check
        int concordantMinusDiscordant = numPairs - tiedXPairs - tiedYPairs + tiedXYPairs - 2 * swaps;
        double divisor = Math.sqrt((numPairs - tiedXPairs) * (numPairs - tiedYPairs));
        return (divisor != 0) ? (double)concordantMinusDiscordant / divisor : -999;		 
	 }
	
	
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