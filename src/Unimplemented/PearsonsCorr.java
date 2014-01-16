package Unimplemented;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

public class PearsonsCorr extends CorrelationAlgorithm {

	double[] series1, series2;
	
	public PearsonsCorr(double[] set1, double[] set2) {		
		series1 = set1;
		series2 = set2;
		if (set1.length != set2.length)
			throw new Error("Sets need to be the same size to be correlated");
	}
	
	@Override
	public double correlate() {
		PearsonsCorrelation scr = new PearsonsCorrelation();
		return scr.correlation(series1, series2);
	}

	@Override
	public String getName() {
		return "Spearmans Correlation";
	}	
}
