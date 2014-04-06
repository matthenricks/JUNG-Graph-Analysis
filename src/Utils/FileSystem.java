package Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileSystem {
	
	/**
	 * Finds the first open path by counting onto an existing path
	 * @param path - the base of the path
	 * @return an open path to write to
	 */
	public static File findOpenPath(File path) {
		int counter = 0;
		while(path.exists())
			path = new File(path.getPath() + counter);
		return path;
	}
	/**
	 * Finds the first open path by counting onto an existing path
	 * @param path - the base of the path
	 * @return an open path to write to
	 */
	public static File findOpenPath(String path) {
		return findOpenPath(new File(path));
	}
	
	/**
	 * Makes sure an empty folder exists at the specified path. If not, it creates one.
	 * This will error if a file exists in the place
	 * @param path
	 * @return
	 */
	public static void createFolder(File path) {
		if (path.exists()) {
			if (path.isFile()) throw new Error("output_folder must be a folder, not a file");
//			if (path.list().length != 0) throw new Error("The output folder is not empty");
		} else {
			if (!path.mkdir())
				throw new Error("Directory creation failed");
		}
	}
	
	/**
	 * Makes sure an empty folder exists at the specified path. If not, it creates one.
	 * This will error a file or partially filled folder exist
	 * @param path
	 * @return
	 */
	public static void createFolder(String path) {
		createFolder(new File(path));
	}
		
	/**
	 * Returns a writer that will overwrite the file
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static BufferedWriter createWriter(File path) throws IOException {
		return new BufferedWriter(new FileWriter(path, false));
	}
	
	// TODO: Change This Dude...
	
	/**
	 * Creates a file by returning a BufferedWriter used to write in it
	 * @param path
	 * @return
	 * @throws IOException 
	 */
	public static File createFile(String path) throws IOException {
		return path == null ? null : new File(path);
	}
	
	/**
	 * Creates a file if it hadn't existed before, otherwise it throws an error
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static BufferedWriter createNewFile(String path) throws IOException {
		File file = new File(path);
		if (file.exists())
			throw new Error("File already exists: " + path);
		return new BufferedWriter(new FileWriter(file));
	}
	
	/**
	 * Creates a file by returning a BufferedWriter used to write in it
	 * @param path
	 * @return
	 * @throws IOException 
	 */
	public static BufferedWriter createFileIfNonexistant(String path) throws IOException {
		File myFile = createFile(path);
		if (myFile.exists()) {
			if (myFile.isFile() == false)
				throw new Error("File loaded is not a file");
		}
		
		return createWriter(myFile);
	}
}