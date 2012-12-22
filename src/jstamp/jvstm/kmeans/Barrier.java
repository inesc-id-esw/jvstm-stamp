package jstamp.jvstm.kmeans;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Barrier {
	 private static CyclicBarrier barrier;
	 public static void enterBarrier() {
	 	try {
	 		barrier.await();
	 	} catch (InterruptedException e) {
	 		// TODO Auto-generated catch block
	 	} catch (BrokenBarrierException e) {
	 		// TODO Auto-generated catch block
//	 		e.printStackTrace();
	 	} catch (IllegalMonitorStateException e) {}
	   }
	   public static void setBarrier(int x) {
	 	  barrier = new CyclicBarrier(x);
	   }

}