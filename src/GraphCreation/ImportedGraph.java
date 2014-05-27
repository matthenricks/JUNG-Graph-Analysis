package GraphCreation;

import java.io.File;

/**
 * Abstraction on a normal graph loader that the path is a graph already exists
 * @author MOREPOWER
 *
 */
public abstract class ImportedGraph extends GraphLoader {

	protected String path;
	public ImportedGraph(String myPath) {
		path = myPath;
		if ((new File(path)).exists() == false)
			throw new Error("File Doesn't Exist For Import: " + myPath);
	}
	
	public String getPath() {
		return path;
	}
}
