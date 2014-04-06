package Correlation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import Utils.CSV_Builder;
import Utils.CSV_Builder_Objects;
import Utils.CSV_Builder_Objects.CSV_Double;

public class MeasureComparison {
	
	static final double[] correlationSampleSizes = {0.05, 0.1, 0.2, 0.3, 0.4, 1.0};

	public static Comparator<Entry<String, Double>> entrySort = new Comparator<Entry<String,Double>>() {
		@Override
		public int compare(Entry<String, Double> arg0, Entry<String, Double> arg1) {
			return arg1.getValue().compareTo(arg0.getValue());
		}
	};
	
	public static List<CSV_Builder> compare(ConcurrentHashMap<String, Double> population, HashMap<String, Double> sample) {
		
		// Sort the two hashmap entry sets and send them to be processed
		ArrayList<Entry<String, Double>> populationValues = new ArrayList<Entry<String, Double>>(population.entrySet());
		ArrayList<Entry<String, Double>> sampleValues = new ArrayList<Entry<String, Double>>(sample.entrySet());
		
		Collections.sort(populationValues, entrySort);
		Collections.sort(sampleValues, entrySort);
		
		// Obtain the different metrics we are generating
		// [sampleAlpha, spearmans, pearsons, error]
		List<CSV_Builder> cCorr = correlationCompare(population, sample, populationValues, sampleValues);
		for (CSV_Builder csv : cCorr) {
			// [KL]
			if (!csv.LinkToEnd(KLCompare(population, sample, populationValues, sampleValues)))
				throw new Error("KL Compare CSV Adding Failed");
			// [popAlpha, sampleAlpha, Precision, Recall]
			if (!csv.LinkToEnd(PRCompare(populationValues, sampleValues)))
				throw new Error("PR Compare CSV Adding Failed");
		}
		
		// [sampleAlpha, spearmans, pearsons, error] + [KL] + [popAlpha, sampleAlpha, Precision, Recall]
		return cCorr;
	}

	
	/**
	 * Computes the Precision, Recall for the two sorted arrays.
	 * @param populationSorted
	 * @param sampleSorted
	 * @return a list of CSVs of composition: [popAlpha, sampleAlpha, Precision, Recall]
	 */
	public static List<CSV_Builder> PRCompare(ArrayList<Entry<String, Double>> populationSorted, ArrayList<Entry<String, Double>> sampleSorted) {
		
		// Check 100%/10% of the sample vs 10% of the population
		double[] populationSizes = {0.1};
		double[] sampleSizes = {0.1, 1.0};
		
		List<CSV_Builder> csvs = new ArrayList<CSV_Builder>(2);
		
		for (double popAlpha : populationSizes) {
			// Obtain the sample of the population
			int popSize = (int)Math.ceil(populationSorted.size()*popAlpha);
			HashSet<String> populationSample = new HashSet<String>(popSize);
			for (int i = 0; i < popSize; i++) {
				populationSample.add(populationSorted.get(i).getKey());
			}

			// Create a CSV_Builder to contain all the data
			CSV_Builder cPopSize = new CSV_Builder(new CSV_Builder_Objects.CSV_Percent(popAlpha));

			// Obtain the samples of the samples
			for (double sampleAlpha : sampleSizes) {
				// Sampling part
				int sampleSize = (int)Math.ceil(sampleSorted.size()*sampleAlpha);
				HashSet<String> sampleSample = new HashSet<String>(popSize);
				for (int i = 0; i < sampleSize; i++) {
					sampleSample.add(sampleSorted.get(i).getKey());
				}
				
				// Now look for all the cross-sections
				cPopSize.LinkTo(new CSV_Builder(new CSV_Builder_Objects.CSV_Double(sampleAlpha),
						PrecisionRecallF.PR(populationSample, sampleSample)));
			}
			
			csvs.add(cPopSize);
		}
		
		return csvs;
	}

	
	/**
	 * Computes the Precision, Recall for the two sorted arrays.
	 * @param populationSorted
	 * @param sampleSorted
	 * @return a list of CSVs of composition: [popAlpha, sampleAlpha, Precision, Recall]
	 */
	public static List<CSV_Builder> PRCompare(ArrayList<Entry<String, Double>> populationSorted, ArrayList<Entry<String, Double>> sampleSorted, double[] lengths) {
		
		List<CSV_Builder> csvs = new ArrayList<CSV_Builder>(lengths.length);
		
		for (double length : lengths) {
			// Obtain the sample of the population
			int popSize = (int)Math.ceil(populationSorted.size()*length);
			HashSet<String> populationSample = new HashSet<String>(popSize);
			for (int i = 0; i < popSize; i++) {
				populationSample.add(populationSorted.get(i).getKey());
			}

			// Create a CSV_Builder to contain all the data
			CSV_Builder cCorrSize = new CSV_Builder(new CSV_Builder_Objects.CSV_Percent(length));

			// Obtain the samples of the samples
			int sampleSize = (int)Math.ceil(sampleSorted.size()*length);
			HashSet<String> sampleSample = new HashSet<String>(popSize);
			for (int i = 0; i < sampleSize; i++) {
				sampleSample.add(sampleSorted.get(i).getKey());
			}
				
			// Now look for all the cross-sections
			cCorrSize.LinkTo(PrecisionRecallF.PR(populationSample, sampleSample));
			
			csvs.add(cCorrSize);
		}
		
		return csvs;
	}

	/**
	 * Compares the whole distributions of the population and sample through the KL measure
	 * @param population
	 * @param sample
	 * @param populationSorted
	 * @param sampleSorted
	 * @return a list of CSV's of composition: KL
	 */
	public static List<CSV_Builder> KLCompare(Map<String, Double> population, Map<String, Double> sample,
			ArrayList<Entry<String, Double>> populationSorted, ArrayList<Entry<String, Double>> sampleSorted) {
		
		// This looks at the different shape of distributions of the population and sample
		// As of now, it serves to further back up the correlation

		ArrayList<CSV_Builder> csvs = new ArrayList<CSV_Builder>(1);
		
		// Populate the sample and population values
		int counter = 0;
		double[] sampleValues = new double[sampleSorted.size()];
		double[] popValues = new double[populationSorted.size()];
				
		// Sort out all of the doubles
		for (; counter < sampleSorted.size(); counter++) {
			sampleValues[counter] = sampleSorted.get(counter).getValue();
			popValues[counter] = populationSorted.get(counter).getValue();
		}
		for (; counter < populationSorted.size(); counter++) {
			popValues[counter] = populationSorted.get(counter).getValue();
		}
		
		// Add the results to a CSV
		csvs.add(new CSV_Builder(new CSV_Double(KLCalculation.computeKL(population, sample))));
		return csvs;
	}
	
	/**
	 * Compares a population and sample through correlation
	 * @param population
	 * @param sample
	 * @param populationSorted
	 * @param sampleSorted
	 * @return List of CSV: [sampleAlpha, spearmans, pearsons, error]
	 */
	public static List<CSV_Builder> correlationCompare(
			Map<String, Double> population, Map<String, Double> sample,
			ArrayList<Entry<String, Double>> populationSorted, ArrayList<Entry<String, Double>> sampleSorted) {
		
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
}
