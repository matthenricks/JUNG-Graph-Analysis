package Unimplemented;

/**
 * Unimplemented Algorithm to better organize and keep track of all correlations
 * @author MOREPOWER
 *
 */
public abstract class CorrelationAlgorithm {
	
	/**
	 * Function that creates a correlation between one or more data sets
	 * @return a value of the correlation
	 */
	public abstract double correlate();
	
	/**
	 * Function that returns the name of the correlation method. Used generally for a CSV file
	 * @return
	 */
	public abstract String getName();
}
