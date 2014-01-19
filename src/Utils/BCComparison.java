package Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import GraphAnalyzers.BCAnalyzer;

/**
 * This function will take in two HashMaps of BC and check if they are equal
 * @author iosbomb
 *
 */
public class BCComparison {

	public static void main(String[] args) throws IOException {
		
		if (args.length != 3) {
			System.out.println("Must enter the <path1> <path2> <output_path> as arguments to run this function");
			return;
		}
		
		String pathA = args[0];
		String pathB = args[1];
		BufferedWriter output = Utils.FileSystem.createFile(args[2]);
		if (pathA == null || pathB == null)
			throw new Error("This function must accept 2 paths to the csv and an output for the result");
		
		HashMap<String, Double> seriesA = BCAnalyzer.readGraphBC(pathA);
		HashMap<String, Double> seriesB = BCAnalyzer.readGraphBC(pathB);
		
		for (String key : seriesA.keySet()) {
			if (seriesA.get(key) - seriesB.get(key) == 0) {
				if (key != null) {
					seriesB.remove(key);
				} else {
					output.write("This key is null in both: " + key);
					output.newLine();
				}
			} else {
				output.write("These hashmaps are not the same at key value: " + key);
				output.newLine();
				output.close();
				throw new Error("These hashmaps are not the same at key value: " + key);
			}
		}
		
		if (seriesB.keySet().size() != 0) {
			output.write("Unaccounted for BC exists");
			output.close();
			throw new Error("Unaccounted for BC exists");
		}
		
		output.write("These two BC samples are fine!");
		output.close();
	}

}
