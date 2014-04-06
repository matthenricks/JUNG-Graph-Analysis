package Unimplemented;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;

public class CSVErrorBuilder {

	/**
	 * Small class to help fix an error I caused
	 * @param csvPath
	 * @param csvOutput
	 * @throws IOException 
	 */
	public static void fixCSV(String csvPath, String csvOutput) throws IOException {
		BufferedWriter writer = Utils.FileSystem.createNewFile(csvOutput);
		BufferedReader br = new BufferedReader(new FileReader(csvPath));
		
		// Re-output the first line
		writer.write(br.readLine());
		writer.newLine();
		// Split the line, re-output everything
		String data = null;
		while ((data = br.readLine()) != null) {
			String[] splits = data.split(",");
			for (int i = 0; i < splits.length-1; i++) {
				writer.write(splits[i] + ",");
			}
			// "Pearsons"
			writer.write(splits[splits.length-1].substring(0, 7) + ",");
			// "Error"
			writer.write(splits[splits.length-1].substring(8));
			writer.newLine();
		}
		
		br.close();
		writer.close();
	}
	
	public static void main(String[] args) {
		String h = "1234567890";
		if (h.substring(0, 7).equals("1234567")) {
			try {
				fixCSV(args[0], args[1]);
			} catch (IOException e) {
				System.err.print("ERROR IN FUNCTION");
				e.printStackTrace();
			}
		}
	}
}
