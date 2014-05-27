package DataAnalyzers;

import jsc.independentsamples.SmirnovTest;

/**
 * Class holder for the KS-Test for comparing distributions. 
 * @author MOREPOWER
 *
 */
public class KolmogorovSmirnovTest {

	final static double threshold = 0.000001;
	
	/**
	 * Wrapper function for the KS-Test. This function catches errors caused by the approximation, making extremely
	 * small values equal 0, as well as catching infinity cases.
	 * @param population - the population or true distribution
	 * @param sample - the distribution to compare to the population
	 * @return the value of the Kolmogorov-Smirnov test statistic, D.
	 * @Documentation: http://www.jsc.nildram.co.uk/api/jsc/independentsamples/SmirnovTest.html
	 */
	public static double runSmirnov(double[] population, double[] sample) {
		if (population.length < 2 || sample.length < 2) {
			System.out.println("Pop/Sample is too small");
			return Double.NaN;
		}
		
		SmirnovTest smt = null;
		try {
			smt = new SmirnovTest(population, sample);
		}
		catch (java.lang.IllegalArgumentException e) {
			if (e.getMessage().matches("^Invalid SP.+E\\-[0-9]+$")) {
//				e.printStackTrace();
				return 0;
			} else if (e.getMessage().matches("^Invalid SP -Infinity$")) {
				e.printStackTrace();
				return Double.NEGATIVE_INFINITY;
			} else if (e.getMessage().matches("^Invalid SP Infinity$")) {
				e.printStackTrace();
				return Double.POSITIVE_INFINITY;
			} else {
				e.printStackTrace();
				return Double.NaN;
			}
		}
		catch (java.lang.RuntimeException e) {
			e.printStackTrace();
			return Double.NaN;
		}
		return smt.getTestStatistic();
	}
	
}
