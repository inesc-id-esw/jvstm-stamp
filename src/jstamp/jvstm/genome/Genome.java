package jstamp.jvstm.genome;

import jvstm.Transaction;

public class Genome extends Thread {
    final int geneLength;
    final int segmentLength;
    final int minNumSegment;
    final int numThread;

    final int threadid;

    // add segments, random, etc to member variables
    // include in constructor
    // allows for passing in thread run function
    final Random randomPtr;
    final Gene genePtr;
    final Segments segmentsPtr;
    final Sequencer sequencerPtr;

    Genome(String args[]) {
	int i = 0;
	String arg;
	threadid = -1;
	int gL = 0;
	int sL = 0;
	int mS = 0;
	int nT = 0;
	while (i < args.length && args[i].startsWith("-")) {
	    arg = args[i++];
	    //check options
	    if(arg.equals("-g")) {
		if(i < args.length) {
		    gL = new Integer(args[i++]).intValue();
		}
	    } else if(arg.equals("-s")) {
		if(i < args.length) {
		    sL = new Integer(args[i++]).intValue();
		}
	    } else if(arg.equals("-n")) {
		if(i < args.length) {
		    mS = new Integer(args[i++]).intValue();
		}
	    } else if(arg.equals("-t")) {
		if(i < args.length) {
		    nT = new Integer(args[i++]).intValue();
		}
	    } 
	}
	this.geneLength = gL;
	this.segmentLength = sL;
	this.minNumSegment = mS;
	if(nT == 0) {
	    numThread = 1;
	} else {
	    this.numThread = nT;
	}

	randomPtr = new Random();
	randomPtr.random_alloc();
	randomPtr.random_seed(0);

	genePtr = new Gene(geneLength, randomPtr);

	segmentsPtr = new Segments(segmentLength, minNumSegment);
	segmentsPtr.create(genePtr, randomPtr);

	sequencerPtr = new Sequencer(geneLength, segmentLength, segmentsPtr);
    }

    Genome(int myThreadid, int myGeneLength, int mySegLength, int myMinNumSegs, int myNumThread, Random myRandomPtr, Gene myGenePtr, Segments mySegmentsPtr, Sequencer mySequencerPtr) {
	threadid = myThreadid;
	geneLength = myGeneLength;
	segmentLength = mySegLength;
	minNumSegment = myMinNumSegs;
	numThread = myNumThread;

	randomPtr = new Random();;
	randomPtr.random_alloc();
	randomPtr.random_seed(myThreadid);
	genePtr = myGenePtr;
	segmentsPtr = mySegmentsPtr;
	sequencerPtr = mySequencerPtr;
    }

    public void run() {
	Barrier.enterBarrier();
	Sequencer.run(threadid, numThread, randomPtr, sequencerPtr); 
    }

    public static void main(String x[]) throws InterruptedException{

//	System.out.print("Creating gene and segments... ");
	Transaction.beginUnsafeSingleThreaded();
	Genome g = new Genome(x);
	Transaction.commit();

//	System.out.println("done.");
//	System.out.println("Gene length     = " + g.genePtr.length);
//	System.out.println("Segment length  = " + g.segmentsPtr.length);
//	System.out.println("Number segments = " + g.segmentsPtr.contentsPtr.size());
//	System.out.println("Number threads  = " + g.numThread);


	Barrier.setBarrier(g.numThread);

	/* Create and Start Threads */

	ByteString gene = g.genePtr.contents;
	Genome[] gn = new Genome[g.numThread];

	for(int i = 0; i<g.numThread; i++) {
	    gn[i] = new Genome(i, g.geneLength, g.segmentLength, g.minNumSegment, g.numThread, g.randomPtr, g.genePtr, g.segmentsPtr, g.sequencerPtr);
	}

//	System.out.print("Sequencing gene... ");    

	for(int i = 1; i<g.numThread; i++) {
	    gn[i].start();
	}

	long start=System.currentTimeMillis();
	gn[0].start();
	//Sequencer.run(0, g.numThread, g.randomPtr, g.sequencerPtr); 
	for (int i = 0; i < g.numThread; i++) {
	   gn[i].join(); 
	}
	long stop=System.currentTimeMillis();
	long diff=stop-start;
	System.out.println(diff);
//	System.out.println("done.");
Transaction.beginInevitable();
	/* Check result */
	{
	    ByteString sequence = g.sequencerPtr.sequence.get();
	    boolean result = gene.compareTo(sequence) == 0;
//	    System.out.println(gene + " \n" + sequence);
//	    System.out.println("Sequence matches gene: " + (result ? "yes" : "no"));
	    //DEBUG
	    //if (result) {
	    // System.out.println("gene     = " + gene);
	    // System.out.println("sequence = " + sequence);
	    //}
	}
Transaction.commit();

    }
}
