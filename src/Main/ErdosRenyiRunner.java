package Main;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import Utils.HardCode;

/***
 * A class created to throw series of arguments into the main runner class
 * @author iosbomb
 *
 */
public class ErdosRenyiRunner {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException, ExecutionException, TimeoutException, InterruptedException {
		
		// Create the basis for all the inputs
		String[] input = {
				"--output",
				null, // output must go here
				"--timeout",
				"24",
				"HOURS",
				"--load",
				"erdosRenyi",
				null, // number of nodes
				null // probability
		};
		
		// Send them through
		for (int nodeNum = 1000; nodeNum <= 4000; nodeNum = nodeNum * 2) {
			for (double probability = 0.015; probability <= 0.5; probability *= 2) {
				input[1] = "Test/ERDOS-" + nodeNum + "-" + HardCode.dcf.format(probability * 100);
				input[8] = String.valueOf(probability);
				input[7] = String.valueOf(nodeNum);
				BCRunnerSimple.main(input);
			}
		}
	}
}
