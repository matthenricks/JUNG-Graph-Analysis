package Main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class TestRunner {

	private static class writeInts implements Runnable {

		Integer counter = 0;
		
		public writeInts(int i) {
			counter = i;
		}
		
		@Override
		public void run() {
			try {
				TestRunner.writeOut(counter++, counter++);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	static void writeOut(Integer a, Integer b) throws InterruptedException {
		
		int i = 0;
		for (int y = 0; y < 1000000; y++) {
			i = i + y;
			if (i < 50000)
				Thread.sleep(10);
		}
		System.out.println("DONE: " + a);
	}
	
	public static void main(String[] args) throws InterruptedException, IOException, Error {

		// Create the graph
		UndirectedSparseGraph<String, String> dGraph = new UndirectedSparseGraph<String, String>();
		dGraph.addVertex("A");
		dGraph.addVertex("B");
		dGraph.addVertex("C");

		dGraph.addEdge("e1", "A", "B");
		dGraph.addEdge("e2", "B", "A");
		
		// Run the degree algorithm
		DegreeScorer<String> ds = new DegreeScorer<String>(dGraph);
		Integer degree;
		for (String vertex : dGraph.getVertices()) {
			degree = ds.getVertexScore(vertex);
			System.out.println(vertex + "," + degree.toString());
		}
	
		
		
		
//		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("export.csv")));
//		bw.write(
//				Double.POSITIVE_INFINITY + "," +
//						Double.NEGATIVE_INFINITY + "," +
//						Double.NaN + "\n"
//				+
//				(new Utils.CSV_Builder_Objects.CSV_Percent(Double.POSITIVE_INFINITY)).toString() + "," +
//						(new Utils.CSV_Builder_Objects.CSV_Percent(Double.NEGATIVE_INFINITY)).toString() + "," +
//						(new Utils.CSV_Builder_Objects.CSV_Percent(Double.NaN)).toString() + "\n"
//				+
//				(new Utils.CSV_Builder_Objects.CSV_Double(Double.POSITIVE_INFINITY)).toString() + "," +
//						(new Utils.CSV_Builder_Objects.CSV_Double(Double.NEGATIVE_INFINITY)).toString() + "," +
//						(new Utils.CSV_Builder_Objects.CSV_Double(Double.NaN)).toString()
//				);
//		bw.flush();
//		bw.close();
		
		System.out.println(Runtime.getRuntime().availableProcessors());
		System.out.println(Runtime.getRuntime().maxMemory() * 9.53674e-7);
		
		
		ExecutorService eS = Executors.newCachedThreadPool(new Utils.HardCode.PriorityThreadFactory(Thread.MIN_PRIORITY));
		ExecutorService eS2 = Executors.newCachedThreadPool(new Utils.HardCode.PriorityThreadFactory(Thread.MAX_PRIORITY));
		
		
		for (int i = 0; i < 50; i+=1) {
			eS.execute(new writeInts(0));
			eS2.execute(new writeInts(2));
		}
		eS.shutdown();
		eS2.shutdown();
		eS.awaitTermination(100, TimeUnit.SECONDS);
		eS2.awaitTermination(100, TimeUnit.SECONDS);
	}
	
}