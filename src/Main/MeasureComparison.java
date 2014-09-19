package Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import DataAnalyzers.Correlations;
import DataAnalyzers.KolmogorovSmirnovTest;
import DataAnalyzers.PrecisionRecallF;
import Utils.CSV_Builder;
import Utils.CSV_Builder_Objects;

/**
 * Class that integrates the correlation classes with the process of analyzing data
 * @author MOREPOWER
 *
 */
public class MeasureComparison {
	
	// The sizes of the top of the sample/population that are compared in terms of correlation
	static final double[] correlationSampleSizes = {0.05, 0.1, 0.2, 0.3, 0.4, 1.0};
	
	// The sizes of the population and sample that are compared during the PR function
	static final double[] PRPopulationSizes = {0.1};
	static final double[] PRSampleSizes = {0.1, 1.0};
	
	/**
	 * Comparator to sort the entries in the Hashmaps
	 */
	public static Comparator<Entry<String, Double>> entrySort = new Comparator<Entry<String,Double>>() {
		@Override
		public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
			int initComp = arg1.getValue().compareTo(arg0.getValue());
			if (initComp == 0) {
				return arg1.getKey().compareTo(arg0.getKey());
			} else {
				return initComp;
			}
		}
	};
	
	/**
	 * Comparator to sort the entries in the Hashmaps
	 */
	public static Comparator<Entry<String, Double>> entrySortBackwards = new Comparator<Entry<String,Double>>() {
		@Override
		public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
			int initComp = arg0.getValue().compareTo(arg1.getValue());
			if (initComp == 0) {
				return arg0.getKey().compareTo(arg1.getKey());
			} else {
				return initComp;
			}
		}
	};
	
	/**
	 * Main comparing function that compares the population to the sample. First, all values are pulled and sorted;
	 * then, spearmans, pearsons, error, precision, and recall are computed.
	 * @param population - the mapping of the population values
	 * @param sample - the mapping of the sample values
	 * @returns a CSV_Builder storing: 
	 *   Correlation: [alpha, spearmans, pearsons, error]
	 *   P/R: [pop-alpha, sam-alpha, precision, recall]
	 */
	public static List<CSV_Builder> compare(Map<String, Double> population, Map<String, Double> sample) {
		
		// Pull the hashmaps into a sortable structure
		ArrayList<Entry<String, Double>> populationValues = new ArrayList<Entry<String, Double>>(population.entrySet());
		ArrayList<Entry<String, Double>> sampleValues = new ArrayList<Entry<String, Double>>(sample.entrySet());
		// Sort the sample and population
		Collections.sort(populationValues, entrySort);
		Collections.sort(sampleValues, entrySort);
		
		// Run the analyses
		List<CSV_Builder> cCorr = correlationCompare(population, sampleValues);
		for (CSV_Builder csv : cCorr) {
			if (!csv.LinkToEnd(PRCompare(populationValues, sampleValues)))
				throw new Error("PR Compare CSV Adding Failed");
		}
		
		return cCorr;
	}

	
	 /* Main comparing function that compares the population to the sample. First, all values are pulled and sorted;
	 * then, spearmans, pearsons, error, precision, and recall are computed.
	 * @param population - the mapping of the population values
	 * @param sample - the mapping of the sample values
	 * @returns a CSV_Builder storing: 
	 *   Correlation: [alpha, spearmans, pearsons, error]
	 *   P/R: [pop-alpha, sam-alpha, precision, recall]
	 */
	public static List<CSV_Builder> compare(Map<String, Double> population, Map<String, Double> sample, boolean reverseSort) {
		
		// Pull the hashmaps into a sortable structure
		ArrayList<Entry<String, Double>> populationValues = new ArrayList<Entry<String, Double>>(population.entrySet());
		ArrayList<Entry<String, Double>> sampleValues = new ArrayList<Entry<String, Double>>(sample.entrySet());
		// Sort the sample and population
		if (!reverseSort) {
			Collections.sort(populationValues, entrySort);
			Collections.sort(sampleValues, entrySort);
		} else {
			Collections.sort(populationValues, entrySortBackwards);
			Collections.sort(sampleValues, entrySortBackwards);
		}
		// Run the analyses
		List<CSV_Builder> cCorr = correlationCompare(population, sampleValues);
		for (CSV_Builder csv : cCorr) {
			if (!csv.LinkToEnd(PRCompare(populationValues, sampleValues)))
				throw new Error("PR Compare CSV Adding Failed");
		}
		
		return cCorr;
	}

	
	/**
	 * Computes the Precision, Recall for the two sorted arrays for all permutations of the tested sample sizes
	 * @param populationSorted
	 * @param sampleSorted
	 * @return a list of CSVs of composition: [pop-alpha, sam-alpha, Precision, Recall]
	 */
	public static List<CSV_Builder> PRCompare(ArrayList<Entry<String, Double>> populationSorted, ArrayList<Entry<String, Double>> sampleSorted) {
		
		List<CSV_Builder> csvs = new ArrayList<CSV_Builder>(2);
		
		for (double popAlpha : PRPopulationSizes) {
			// Obtain the sample of the population
			int popSize = (int)Math.ceil(populationSorted.size()*popAlpha);
			HashSet<String> populationSample = new HashSet<String>(popSize);
			for (int i = 0; i < popSize; i++) {
				populationSample.add(populationSorted.get(i).getKey());
			}

			// Create a CSV_Builder to contain all the data
			CSV_Builder cPopSize = new CSV_Builder(new CSV_Builder_Objects.CSV_Percent(popAlpha));

			// Obtain the samples of the samples
			for (double sampleAlpha : PRSampleSizes) {
				// Sampling part
				int sampleSize = (int)Math.ceil(sampleSorted.size()*sampleAlpha);
				HashSet<String> sampleSample = new HashSet<String>(popSize);
				for (int i = 0; i < sampleSize; i++) {
					sampleSample.add(sampleSorted.get(i).getKey());
				}
				
				// Now look for all the cross-sections
				cPopSize.LinkTo(new CSV_Builder(new CSV_Builder_Objects.CSV_Double(sampleAlpha),
						PR(populationSample, sampleSample)));
			}
			
			csvs.add(cPopSize);
		}
		
		return csvs;
	}

	
	/**
	 * Computes the Precision, Recall for the two distributions. It does this taking the top % when % is identified through length
	 * @param populationSorted
	 * @param sampleSorted
	 * @param lengths - a list of lengths for the PR function to compare the top %s of
	 * @return a list of CSVs of composition: [popAlpha, sampleAlpha, Precision, Recall]
	 */
	public static List<CSV_Builder> PRCompare(ArrayList<Entry<String, Double>> populationSorted, ArrayList<Entry<String, Double>> sampleSorted, double[] lengths) {
		
		List<CSV_Builder> csvs = new ArrayList<CSV_Builder>(lengths.length);
		Arrays.sort(lengths);
		
		// Define the two hashsets
		HashSet<String> populationSample = new HashSet<String>();
		HashSet<String> sampleSample = new HashSet<String>();
		
		// Overall counters for the hash-sets
		int popCounter = 0, samCounter = 0;
		
		// Loop through the lengths and run the analysis as the hashsets build to each size
		for (double length : lengths) {
			int popSize = (int)Math.ceil(populationSorted.size()*length);
			int samSize = (int)Math.ceil(sampleSorted.size()*length);
			// Obtain the sample of the population
			for (; popCounter < popSize; popCounter++) {
				populationSample.add(populationSorted.get(popCounter).getKey());
			}
			for (; samCounter < samSize; samCounter++) {
				sampleSample.add(sampleSorted.get(samCounter).getKey());
			}
			
			// Create a CSV_Builder to contain all the data (starting with the percentage of the top compared)
			CSV_Builder cCorrSize = new CSV_Builder(new CSV_Builder_Objects.CSV_Percent(length));
			// Now calculate all the cross-sections
			cCorrSize.LinkTo(PR(populationSample, sampleSample));
			// Add the csvs to a loop to eventually return 
			csvs.add(cCorrSize);
		}
		
		return csvs;
	}
	
	/**
	 * Calculate the precision and recall for a two sets. HashSets are recommended
	 * @param population
	 * @param sample
	 * @return a CSV Builder of composition: [Precision, Recall]
	 */
	public static CSV_Builder PR(Set<String> reference, Set<String> test) {
		return new CSV_Builder(new CSV_Builder_Objects.CSV_Double(PrecisionRecallF.Precision(reference, test)), 
					new CSV_Builder(new CSV_Builder_Objects.CSV_Double(PrecisionRecallF.Recall(reference, test)))
				);
	}
	
	
	/**
	 * Wrapper function for currently formatting the sample and population maps into arrays and running them on 
	 * the hardcoded alpha values. It does this by drawing the top _% of the sample and finding those values in
	 * the population sample. It places these two distributions in arrays and runs spearmans, pearsons, and the 
	 * average error.
	 * 
	 * @param population 
	 * @param sampleSorted
	 * @return List of CSV: [sampleAlpha, spearmans, pearsons, error]
	 */
	public static List<CSV_Builder> correlationCompare(Map<String, Double> population, ArrayList<Entry<String, Double>> sampleSorted) {
		
		// What would also be useful in saying a smaller amount of the sample is also well represented
		// The bottom may not have enough connections, but it makes sense that the top BC will have had their friends pulled
		
		ArrayList<CSV_Builder> csvs = new ArrayList<CSV_Builder>(correlationSampleSizes.length);
		for (double sliceSize : correlationSampleSizes) {
			int corrSize = (int)Math.ceil(sampleSorted.size()*sliceSize);
			// Populate the sample and population values
			double[] sampleValues = new double[corrSize];
			double[] popValues = new double[corrSize];
			for (int i = 0; i < corrSize; i++) {
				sampleValues[i] = sampleSorted.get(i).getValue();
				popValues[i] = population.get(sampleSorted.get(i).getKey());
			}
			// Add the results to a CSV
			csvs.add(new CSV_Builder(new CSV_Builder_Objects.CSV_Percent(sliceSize),
					new CSV_Builder(new CSV_Builder_Objects.CSV_Double(Correlations.spearmansCorrelation(popValues, sampleValues)),
							new CSV_Builder(new CSV_Builder_Objects.CSV_Double(Correlations.pearsonsCorrelation(popValues, sampleValues)),
									new CSV_Builder(new CSV_Builder_Objects.CSV_Double(Correlations.errorCalculation(popValues, sampleValues))
									)
					)))
			);
		}
		
		return csvs;
	}
	
	/**
	 * Wrapper class for running the KS-Test on a population and sample map
	 * @param population
	 * @param sample
	 * @return a CSV_Builder with the KS-Test D statistic
	 */
	public static CSV_Builder KSCompare(Map<String, Double> population, Map<String, Double> sample) {
		double[] pop = new double[population.size()];
		double[] sam = new double[sample.size()];
		
		int counter = 0;
		for (Entry<String, Double> entry : population.entrySet()) {
			pop[counter] = entry.getValue();
			counter++;
		}
		counter = 0;
		for (Entry<String, Double> entry : sample.entrySet()) {
			sam[counter] = entry.getValue();
			counter++;
		}
		
		return new CSV_Builder(new CSV_Builder_Objects.CSV_Double(KolmogorovSmirnovTest.runSmirnov(pop, sam)));
	}
}