package SamplingAlgorithms;

/**
 * Interface extension for all sample methods that have a target and grow to it
 * @author MOREPOWER
 *
 */
public interface TargetedSampleMethod extends SampleMethod {
	/**
	 * Changes the alpha target of the sampling method
	 * @param alpha
	 */
	public void changeAlpha(double alpha);
	
}
