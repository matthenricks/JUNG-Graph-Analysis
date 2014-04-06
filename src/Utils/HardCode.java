package Utils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

/**
 * A class for the sole purpose of including all of the global hard-coded values in
 * @author MOREPOWER
 *
 */
public class HardCode {
	
	/**
	 * Function that drops the numbers after a number of digits after the decimal
	 * @param l1
	 * @param decimalLength
	 * @return
	 */
	public static double floorValue(double d, int decimalLength) {
		return ((double) Math.floor(d * Math.pow(10, decimalLength))) / Math.pow(10, decimalLength);
	}
	
	/**
	 * Function that drops all the numbers after a number of digits after the decimal
	 * @param l1
	 * @param decimalLength
	 * @return
	 */
	public static double[] floorAllValues(double[] l1, int decimalLength) {
		for (int i = 0; i < l1.length; i++) {
			l1[i] = ((double) Math.floor(l1[i] * Math.pow(10, decimalLength))) / Math.pow(10, decimalLength);
		}
		return l1;
	}
	/**
	 * Function that drops all the numbers after a number of digits after the decimal
	 * @param l1
	 * @param decimalLength
	 * @return
	 */
	public static List<Double> floorAllValues(List<Double> l1, int decimalLength) {
		for (int i = 0; i < l1.size(); i++) {
			l1.set(i, ((double) Math.floor(l1.get(i) * Math.pow(10, decimalLength))) / Math.pow(10, decimalLength));
		}
		return l1;
	}
	/**
	 * Function that drops all the numbers after a number of digits after the decimal
	 * @param l1
	 * @param decimalLength
	 * @return
	 */
	public static Double[] floorAllValues(Double[] l1, int decimalLength) {
		for (int i = 0; i < l1.length; i++) {
			l1[i] = ((double) Math.floor(l1[i] * Math.pow(10, decimalLength))) / Math.pow(10, decimalLength);		
		}
		return l1;
	}

	
	
	// Folder structural Strings for where information is being held/in what type of file
	public static String pSummaryPostfix = "/summary.txt";
	public static String pCorrPostfix = "/analysis.csv";
	public static String pMetricPostfix = "/metrics.txt";
	public static String pWccPostfix = "/WCC.csv";
	public static String pBcPostfix = "/bc.csv";
	public static String pDegreePostfix = "/degree.csv";
	public static String pEDPostfix = "/egocentric-density.csv";
	public static String pSamplesFolder = "/samples/";
	public static String pDistroFolder = "/distributions";
	public static String pDataFix = "/data.dat";
	
	// Formatting variables for how doubles will be displayed
	public static DecimalFormat dcf3 = new DecimalFormat("0.00000");
	public static DecimalFormat dcf = new DecimalFormat("00000");
	public static DecimalFormat dcfP = new DecimalFormat("0.000%");
	// Quick reference to a Pattern used to decompose CSV files
	public static Pattern separateReg= Pattern.compile("\\,", Pattern.DOTALL);
	
	/**
	 * A class that's used with ExecutorService to set thread priorities. Callable is just a thread with a safe variable in the class.
	 * @author MOREPOWER
	 *
	 */
	public static class PriorityThreadFactory implements ThreadFactory {
		// The priority of all the threads coming out of this factory
		final int priority;
		public PriorityThreadFactory(int priority) {
			this.priority = priority;
		}
		public PriorityThreadFactory() {
			this.priority = Thread.MIN_PRIORITY;
		}
		public Thread newThread(Runnable r) {
			Thread t =  new Thread(r);
			t.setPriority(priority);
			return t;
		}
	 }
}
