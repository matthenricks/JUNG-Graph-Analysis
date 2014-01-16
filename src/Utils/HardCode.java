package Utils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.regex.Pattern;
import java.util.concurrent.ThreadFactory;

import GraphCreation.BarabasiAlbertGraphGenerator;
import GraphCreation.BasicGraph;
import GraphCreation.ErdosRenyiGraphGenerator;
import GraphCreation.FriendsTwitterDataImporter;
import GraphCreation.GraphLoader;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * A class for the sole purpose of including all of the global hard-coded values in
 * @author MOREPOWER
 *
 */
public class HardCode {
	

	public static DecimalFormat dcf3 = new DecimalFormat("0.000");
	public static DecimalFormat dcf = new DecimalFormat("00000");
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
