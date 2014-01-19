package Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to track various important statistics throughout the program
 * Note: A job must be finished before another job is added on
 * @author MOREPOWER
 *
 */
public class JobTracker {
	
	// TODO: Make it so we can consolidate jobs into other jobs and import JobTrackers into each other
	
	final int SecondToNano = 1000000000;
	
	/**
	 * Abstract timer class
	 * @author MOREPOWER
	 *
	 */
	private abstract class AbstractTimer {
		public abstract void start();
		public abstract void stop();
		public abstract LinkedHashMap<String, Long> getTimes();
	}

	/**
	 * Normal class for a timer using wall time
	 * 	Note: this does not check to see if you're starting/ending twice
	 * @author MOREPOWER
	 *
	 */
	private class NormalTimer extends AbstractTimer {

		private Long start, end;
		public NormalTimer() {}
		
		@Override
		public void start() {
			start = System.currentTimeMillis();
		}

		@Override
		public void stop() {
			end = System.currentTimeMillis();
		}

		@Override
		public LinkedHashMap<String, Long> getTimes() {
			LinkedHashMap<String, Long> times = new LinkedHashMap<String, Long>();
			times.put("wall(Mili)", end - start);
			return times;
		}
	}
	
	private class ExactTimer extends NormalTimer {
		// "User time" is the time spent running your application's own code.
		long startUser, endUser;
		// "System time" is the time spent running OS code on behalf of your application (such as for I/O).
		// "CPU time" is the sum of both of them
		long startCPU, endCPU;
		
		public ExactTimer() {
			super();
			if (!ManagementFactory.getThreadMXBean().isCurrentThreadCpuTimeSupported()) {
				throw new Error("Exact Timer is unsupported, use Normal Timer");
			}
		}
		
		@Override
		public void start() {
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		    startUser = bean.getCurrentThreadUserTime();
			startCPU = bean.getCurrentThreadCpuTime();
			super.start();
		}

		@Override
		public void stop() {
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		    endUser = bean.getCurrentThreadUserTime();
			endCPU = bean.getCurrentThreadCpuTime();
			super.stop();
		}

		@Override
		public LinkedHashMap<String, Long> getTimes() {
			LinkedHashMap<String, Long> times = super.getTimes();
			times.put("User(nano)", endUser - startUser);
			times.put("User(seconds)", (endUser - startUser)/SecondToNano);
			times.put("System(nano)", (endCPU - startCPU) - (endUser - startUser));
			times.put("System(seconds)", ((endCPU - startCPU) - (endUser - startUser))/SecondToNano);
			times.put("CPU(nano)", endCPU - startCPU);
			times.put("CPU(seconds)", (endCPU - startCPU)/SecondToNano);
			return times;
		}
	}
	
	ConcurrentHashMap<String, AbstractTimer> timers;
	
	public JobTracker() {
		timers = new ConcurrentHashMap<String, AbstractTimer>();
	}
	
	public void startTracking(String jobName) {

		AbstractTimer temp = null;
		if (ManagementFactory.getThreadMXBean().isCurrentThreadCpuTimeSupported())
			temp = new ExactTimer();
		else
			temp = new NormalTimer();
		
		temp.start();
		timers.put(jobName, temp);
	}
	
	public void endTracking(String jobName) {
		if (!timers.containsKey(jobName))
			throw new Error("Job " + jobName + "Doesn't exist. Cannot end tracking on inexistant job");
		
		timers.get(jobName).stop();
	}
	
	public void writeJobTimes(BufferedWriter bw) throws IOException {
		for (String tName : timers.keySet()) {
			bw.append(tName);
			bw.newLine();
			AbstractTimer curr = timers.get(tName);
			for (String timeType : curr.getTimes().keySet()) {
				bw.append("\t" + timeType + ": " + curr.getTimes().get(timeType));
				bw.newLine();
			}
		}
	}
	
	public void writeJobTimes(BufferedWriter bw, String preFix) throws IOException {
		for (String tName : timers.keySet()) {
			bw.append(preFix + tName);
			bw.newLine();
			AbstractTimer curr = timers.get(tName);
			for (String timeType : curr.getTimes().keySet()) {
				bw.append(preFix + "\t" + timeType + ": " + curr.getTimes().get(timeType));
				bw.newLine();
			}
		}	
	}
}