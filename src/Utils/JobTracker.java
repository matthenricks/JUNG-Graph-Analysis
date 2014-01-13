package Utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to track various important statistics throughout the program
 * Note: A job must be finished before another job is added on
 * @author MOREPOWER
 *
 */
public class JobTracker {
	
	/**
	 * Private class to simplify the tracking of time within the tracker
	 * @author MOREPOWER
	 *
	 */
	private static class jobTimer {
		long start;
		long end;
		
		public jobTimer() {
			start = System.nanoTime();
		}
		
		/**
		 * Gets the difference in minutes
		 * @return
		 */
		public long getMinutes() {
			return Math.round(((double)(end-start))/(60.0*Math.pow(10, 9)));
		}
		/**
		 * Gets the difference in nano time
		 * @return
		 */
		public long getDifference() {
			return end - start;
		}
	}
	
	ConcurrentHashMap<String, jobTimer> timers;
	
	public JobTracker() {
		timers = new ConcurrentHashMap<String, jobTimer>();
	}
	
	public void startTracking(String jobName) {
		timers.put(jobName, new jobTimer());
	}
	
	public void endTracking(String jobName) {
		if (!timers.containsKey(jobName))
			throw new Error("Job " + jobName + "Doesn't exist. Cannot end tracking on inexistant job");
		
		jobTimer t = timers.get(jobName);
		t.end = System.nanoTime();
	}
	
	public String getJobTimes() {
		StringBuffer str = new StringBuffer();
		for (String tName : timers.keySet()) {
			str.append(tName + "\t" + timers.get(tName).getDifference() + "\n");
		}
		
		return "Job Times Are As Follows (in nanoseconds):\n" + str.toString();
	}
	
	public String getJobTimesMinutes() {
		StringBuffer str = new StringBuffer();
		for (String tName : timers.keySet()) {
			str.append(tName + "\t" + timers.get(tName).getMinutes() + "\n");
		}
		
		return "Job Times Are As Follows (in nanoseconds):\n" + str.toString();
	}
}