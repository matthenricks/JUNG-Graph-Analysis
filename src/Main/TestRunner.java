package Main;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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