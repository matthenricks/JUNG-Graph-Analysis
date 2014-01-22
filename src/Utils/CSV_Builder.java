package Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * A class that allows you to easily format different strung values into a CSV
 * @author MOREPOWER
 * @param <T> - Any variable that extends an object that would like to be tracked. By default toString() is used for the CSV creation
 */
public class CSV_Builder extends Object {
	
	/**
	 * Serial number for the class, unsure of the reason why
	 */
	private static final long serialVersionUID = 5872505568663027860L;

	// The value of this spot in the chain of CSV_Builders
	Object value;
	LinkedList<CSV_Builder> next;
	
	/**
	 * Sets up a link to the next CSV file in the series. Can link to multiple that it predecesses
	 * @param next
	 * @return
	 */
	public boolean LinkTo(CSV_Builder next) {
		return this.next.add(next);
	}
	
	public CSV_Builder(Object value) {
		super();
		this.value = value;
		this.next = new LinkedList<CSV_Builder>();
	}
	
	public CSV_Builder(Object value, CSV_Builder next) {
		this(value);
		this.next.add(next);
	}
	
	public CSV_Builder(Object value, Collection<CSV_Builder> next) {
		this(value);
		this.next = new LinkedList<CSV_Builder>(next);
	}
	
	/**
	 * A function that returns a String CSV formatted to be easily output
	 * @param prior - the preceding String to this section of the CSV
	 * @return the CSV-formatted String
	 */
	protected String getCSV(String prior) {
		if (next.isEmpty()) {
			// This is the end of the line, just print out your value on the prior and a space: "\n"
			return prior + "," + value.toString() + "\n";
		} else {
			// Write out prior, then your value, then the values after you
			StringBuilder fullString = new StringBuilder();
			for (CSV_Builder part : next) {
				fullString.append(part.getCSV(prior + "," + value.toString()));
			}
			return fullString.toString();
		}
	}
	
	public String getCSV() {
		if (next.isEmpty()) {
			return value.toString() + "\n";
		} else {
			StringBuilder fullString = new StringBuilder();
			for (CSV_Builder part : next) {
				fullString.append(part.getCSV(value.toString()));
			}
			return fullString.toString();
		}
	}
	
	protected void writeCSV(BufferedWriter bw, String prior) throws IOException {
		if (next.isEmpty()) {
			// This is the end of the line, just print out your value on the prior and a space: "\n"
			bw.write(prior + "," + value.toString());
			bw.newLine();
		} else {
			// Write out prior, then your value, then the values after you
			for (CSV_Builder part : next) {
				part.writeCSV(bw, prior + "," + value.toString());
			}
		}
	}	
	public void writeCSV(BufferedWriter bw) throws IOException {
		if (next.isEmpty()) {
			// This is the end of the line, just print out your value on the prior and a space: "\n"
			bw.write(value.toString());
			bw.newLine();
		} else {
			// Set yourself to be written first
			for (CSV_Builder part : next) {
				part.writeCSV(bw, value.toString());
			}
		}
	}
	/**
	 * Writes out a CSV file triggered by this value being on top
	 * @param header - the header values of the CSV file
	 * @param path - the location of the file to get output
	 * @throws IOException
	 */
	public void writeCSV(String header, String path) throws IOException {
		BufferedWriter bw = FileSystem.createFile(path);
		bw.write(header);
		bw.newLine();
		writeCSV(bw);
		bw.close();
	}	
}