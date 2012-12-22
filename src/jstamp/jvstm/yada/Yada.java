package jstamp.jvstm.yada;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jvstm.Transaction;

public class Yada {

    public static final AtomicInteger aborts = new AtomicInteger(0);

    public static int numBuckets = 10000;
    static String global_inputPrefix = "";
    static long global_numThread = 1;
    static double global_angleConstraint = 20.0;
    static Mesh global_meshPtr;
    static Heap global_workheapPtr;
    static AtomicLong global_totalNumAdded = new AtomicLong(0);
    static AtomicLong global_numProcess    = new AtomicLong(0);

    static long initializeWork (Heap workheapPtr, Mesh meshPtr) {
	Random randomPtr = new Random(); 
	randomPtr.random_alloc();
	randomPtr.random_seed(0);
	meshPtr.mesh_shuffleBad(randomPtr);

	long numBad = 0;

	while (true) {
	    Element elementPtr = meshPtr.mesh_getBad();
	    if (elementPtr==null) {
		break;
	    }
	    numBad++;
	    boolean status = workheapPtr.heap_insert(elementPtr);
	    assert(status);
	}

	return numBad;
    }

    static void process () {

	Heap workheapPtr = global_workheapPtr;
	Mesh meshPtr = global_meshPtr;
	Region regionPtr;
	AtomicLong totalNumAdded = new AtomicLong(0);
	AtomicLong numProcess = new AtomicLong(0);

	regionPtr = new Region();

	while (true) {
	    Element elementPtr;

	    elementPtr = workheapPtr.heap_remove();

	    if(elementPtr == null) {
		break;
	    }

	    Boolean isGarbage = elementPtr.element_isGarbage();

	    if(isGarbage) continue;

	    long numAdded;

	    regionPtr.Pregion_clearBad();
	    numAdded = regionPtr.region_refine(elementPtr, meshPtr);

	    totalNumAdded.addAndGet(numAdded);
	    regionPtr.region_transferBad(workheapPtr);
	}
	global_totalNumAdded.addAndGet(totalNumAdded.get());
	global_numProcess.addAndGet(numProcess.get());
    }

    static void parseArgs (String[] args) {
	for(int i =0;i<args.length;i+=2) {
	    //	    System.out.println(args[i]);
	    if(args[i].equals("-a"))
		global_angleConstraint = Double.parseDouble(args[i+1]);
	    if(args[i].equals("-i"))
		global_inputPrefix = args[i+1];
	    if(args[i].equals("-t"))
		global_numThread = Integer.parseInt(args[i+1]);
	}
    }	

    public static void main(String[] args) {
	/*
	 * Initialization
	 */
	Transaction.beginInevitable();
	parseArgs(args);
	//	System.out.println("Threads: "+global_numThread);
	global_meshPtr = new Mesh();
	//	System.out.println("Angle constraint = "+global_angleConstraint);
	//	System.out.println("Reading input... ");
	long initNumElement = global_meshPtr.mesh_read(global_inputPrefix);
	//	System.out.println("done.");
	global_workheapPtr = new Heap(1);
	long initNumBadElement = initializeWork(global_workheapPtr, global_meshPtr);

	Thread[] th = new Thread[(int) global_numThread];
	Runnable[] rn = new Runnable[(int) global_numThread];
	for(int i=0;i<rn.length;i++)
	    rn[i] = new Runnable() {

    	    @Override
    	    public void run() {
    		process();
    	    }
	};

	for(int i=0;i<th.length;i++) {
	    th[i] = new Thread(rn[i]);
	}

	//	System.out.println("Initial number of mesh elements = "+initNumElement);
	//	System.out.println("Initial number of bad elements  = "+initNumBadElement);
	//	System.out.println("Starting triangulation...");

	/*
	 * Run benchmark
	 */
	Transaction.commit();
	long initTime = System.currentTimeMillis();
	for(int i=0;i<th.length;i++)
	    th[i].start();

	try {
	    for(int i=0;i<th.length;i++)
		th[i].join();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}

	//	System.out.println(" done.");

	System.out.println(System.currentTimeMillis() - initTime + " " + aborts.get());

	/*
	 * Check solution
	 */
	Transaction.beginInevitable();
	long finalNumElement = initNumElement + global_totalNumAdded.get();
	//	boolean isSuccess = global_meshPtr.mesh_check(finalNumElement);
	//	System.out.println("Final mesh is "+(isSuccess ? "valid." : "INVALID!"));
	Transaction.commit();
    }

}
