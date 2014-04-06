package SamplingAlgorithms;

public interface TargetedSampleMethod extends SampleMethod {
	/**
	 * Changes the alpha target of the sampling method
	 * @param alpha
	 */
	public void changeAlpha(double alpha);
	
}
