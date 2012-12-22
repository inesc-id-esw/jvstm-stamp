package jstamp.jvstm.genome;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Barrier {

private static CyclicBarrier barrier;
private static CyclicBarrier barrierOne;
private static CyclicBarrier barrierTwo;
private static CyclicBarrier barrierThree;
public static void enterBarrier() {
	try {
		barrier.await();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BrokenBarrierException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

public static void enterBarrier1() {
	try {
		barrierOne.await();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BrokenBarrierException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void enterBarrier2() {
	try {
		barrierTwo.await();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BrokenBarrierException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static void enterBarrier3() {
	try {
		barrierThree.await();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BrokenBarrierException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

  public static void setBarrier(int x) {
	  barrier = new CyclicBarrier(x);
	  barrierOne = new CyclicBarrier(x);
	  barrierTwo = new CyclicBarrier(x);
	  barrierThree = new CyclicBarrier(x);
  }

}