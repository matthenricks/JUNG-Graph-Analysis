package DataAnalyzers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

/**
 * Class that holds the wrappers used for Pearsons, Error, and Spearmans Correlation.
 * Note, custom methods were created due to errors existent in JSC
 * @author MOREPOWER
 *
 */
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
	 * Pearson's Correlation with a degenerate handling scheme. Both series must be the same length
	 *   If both series are degenerate and have the same mean, return 1, else return 0
	 *   If one series is degenerate and the other isn't, return Double.NaN
	 * @param series1 - the first series of data. 
	 * @param series2 - the second series of data. 
	 * @return the pearsons correlation between the series
	 */
	public static double pearsonsCorrelation(double[] series1, double[] series2) {
		if (series1.length != series2.length)
			return Double.NaN;
		
		// Do each in 3 passes
		double mean1 = 0, mean2 = 0;

		// Calculate the means
		for (int i = 0; i < series1.length; i++) {
			mean1 += series1[i];
			mean2 += series2[i];
		}
		mean1 /= series1.length;
		mean2 /= series2.length;
		
		// Calculate the values
		double nominator = 0;
		double var1 = 0;
		double var2 = 0;
		for (int i = 0; i < series1.length; i++) {
			double diff1 = series1[i] - mean1;
			double diff2 = series2[i] - mean2;
			nominator += diff1*diff2;
			var1 += Math.pow(diff1, 2);
			var2 += Math.pow(diff2, 2);
		}

		// Handle degenerate variable cases
		// var1=var2=0, m1!=m2 -> 0, m1=m2 -> 1
		// var1=0, var2>0, Undef
		if (var1 == 0) {
			if (mean1 != mean2)
				return 0;
			else if (var2 == 0) // Means are equal
				return 1;
			else
				return Double.NaN;
		} else if (var2 == 0) {
			if (mean1 != mean2)
				return 0;
			else if (var1 == 0) // Means are equal
				return 1;
			else
				return Double.NaN;
		}

		// Return the normal equation
		return nominator / (Math.pow(var1, 0.5)*Math.pow(var2, 0.5));
	}
	
	/**
	 * Ranking algorithm, a tolerance of 0.0001 is used
	 * @param values
	 * @return
	 */
	private static double[] Rank(double[] values)
	{
		final double tolerance = 0.0001;
		
		double[] sortedList = Arrays.copyOf(values, values.length);
		Arrays.sort(sortedList);
		
		double[] ranks = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			int index = Arrays.binarySearch(sortedList, values[i]);
			// Check around to see how many numbers are around the ranking
			double num = sortedList[index];
			// Iterate back till it changes
			int j = 1;
			for (; j <= index && Math.abs(sortedList[index - j] - num) < tolerance; j++);
			// Iterate forward till it changes
			int k = index + 1;
			for (; k < sortedList.length && Math.abs(sortedList[k] - num) < tolerance; k++);
			
			// Place the real rank in ranks
			ranks[i] = index - j + k + 2;
			ranks[i] /= 2.0;
		}

		return ranks;
	}
	
	/**
	 * Spearmans Rank Correlation with a degenerate handling scheme. Both series must be the same length
	 *   If both series are degenerate and have the same mean, return 1, else return 0
	 *   If one series is degenerate and the other isn't, return Double.NaN
	 *   This always assumes duplicate values ***
	 * @param series1 - the first series of data. The series needs to correspond with series2.
	 * @param series2 - the second series of data. This must correspond to the first series.
	 * @return the spearman's rank correlation between the series
	 */
	public static double spearmansCorrelation(double[] series1, double[] series2) {
		
		if (series1.length != series2.length)
			return Double.NaN;
		
		/** Included ranking algorithm **/
		double[] rank1 = Rank(series1);
		double[] rank2 = Rank(series2);	
			
		/** Now run the spearman's correlation on the algorithm **/
		
		// Do each in 3 passes
		double mean1 = 0, mean2 = 0;

		// Calculate the means
		for (int i = 0; i < rank1.length; i++) {
			mean1 += rank1[i];
			mean2 += rank2[i];
		}
		mean1 /= rank1.length;
		mean2 /= rank2.length;
		
		// Calculate the values
		double nominator = 0;
		double var1 = 0;
		double var2 = 0;
		for (int i = 0; i < series1.length; i++) {
			double diff1 = rank1[i] - mean1;
			double diff2 = rank2[i] - mean2;
			nominator += diff1*diff2;
			var1 += Math.pow(diff1, 2);
			var2 += Math.pow(diff2, 2);
		}
		

		// Handle degenerate variable cases
		// var1=var2=0, m1!=m2 -> 0, m1=m2 -> 1
		// var1=0, var2>0, Undef
		if (var1 == 0) {
			if (mean1 != mean2)
				return 0;
			else if (var2 == 0) // Means are equal
				return 1;
			else
				return Double.NaN;
		} else if (var2 == 0) {
			if (mean1 != mean2)
				return 0;
			
			// else if (var1 == 0) // Means are equal
			//	return 1;
			else
				return Double.NaN;
		}
		
		return nominator / Math.pow(var1*var2, 0.5);		
	}
	
	
	/**
	 * Calculates the average error of series in Euclidean distance.
	 *   Note: Both series must be of the same length and not be 0, or Double.NaN is returned
	 * @param trueSeries - the actual data
	 * @param sampleSeries - the predicted data
	 * @return the average error
	 */
	public static double errorCalculation(double[] trueSeries, double[] sampleSeries) {
		if (trueSeries.length == sampleSeries.length && trueSeries.length > 0) {
			double error = 0.0;
			for (int i = 0; i < trueSeries.length; i++) {
				error += Math.pow(trueSeries[i] - sampleSeries[i], 2);
			}
			return (Math.sqrt(error))/trueSeries.length;
		} else
			return Double.NaN;
	}
	
}