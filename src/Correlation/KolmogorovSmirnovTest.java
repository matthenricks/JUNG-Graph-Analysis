package Correlation;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import jsc.independentsamples.SmirnovTest;
import Utils.CSV_Builder;

public class KolmogorovSmirnovTest {

	// TODO: Remove synchronized for the hell of it
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
				e.printStackTrace();
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
		return smt.getTestStatistic();
	}
	
	public static CSV_Builder runTest(double[] population, double[] sample, double[] badValues) {
		// TODO: Test if this is correct
//		int zeroesP = arrCount(population, badValues), zeroesS = arrCount(sample, badValues);
//		double[] populationAdj = new double[population.length-zeroesP];
//		double[] sampleAdj = new double[sample.length-zeroesS];
//		
//		int counter = 0;
//		for (double val : population) {
//			boolean contains = false;
//			for (double bad : badValues) {
//				if (bad == val) {
//					contains = true;
//					break;
//				}
//			}
//			if (contains == false)
//				populationAdj[counter++] = val;
//		}
//		
//		counter = 0;
//		for (double val : sample) {
//			boolean contains = false;
//			for (double bad : badValues) {
//				if (bad == val) {
//					contains = true;
//					break;
//				}
//			}
//			if (contains == false)
//				sampleAdj[counter++] = val;
//		}

		return new CSV_Builder(new Utils.CSV_Builder_Objects.CSV_Double(runSmirnov(population, sample)) // data adjusted
				);

//		return new CSV_Builder(new Utils.CSV_Builder_Objects.CSV_Double(runSmirnov(population, sample)), // data unadjusted,
//				new CSV_Builder(new Utils.CSV_Builder_Objects.CSV_Double(runSmirnov(populationAdj, sampleAdj))) // data adjusted
//				);
	}
	
	private static int arrCount(double[] array, double[] values) {
		int counter = 0;
		for (double val : array) {
			for (double check : values) {
				if (check == val) {
					counter++;
					break;
				}
			}
		}
		return counter;
	}

	public static CSV_Builder runTest(ConcurrentHashMap<String, Double> population, HashMap<String, Double> sample, double[] badValues) {
		// Sort the two hashmap entry sets and send them to be processed
		double[] pop = new double[population.size()];
		double[] sam = new double[sample.size()];		
		int counter = 0;
		for (Entry<String, Double> val : population.entrySet()) {
			pop[counter++] = val.getValue();
		}
		counter = 0;
		for (Entry<String, Double> val : sample.entrySet()) {
			sam[counter++] = val.getValue();
		}
		
		return runTest(pop, sam, badValues);
	}
}
