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

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException, ExecutionException, TimeoutException {
		
		// Create the basis for all the inputs
		String[] input = {
				"--output",
				null, // output must go here
				"--timeout",
				"HOURS",
				"24",
				"--load",
				"erdosRenyi",
				null, // probability
				null // number of nodes
		};
		
		// Send them through
		for (int nodeNum = 500; nodeNum <= 1600; nodeNum = nodeNum * 2) {
			for (double probability = 0.2; probability <= 0.8; probability += 0.2) {
				input[1] = "Test/ERDOS-" + nodeNum + "-" + HardCode.dcf.format(probability * 100);
				input[7] = String.valueOf(probability);
				input[8] = String.valueOf(nodeNum);
				BCRunner.main(input);
			}
		}
	}
}
