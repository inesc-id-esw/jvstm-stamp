

/* =============================================================================
 *
 * intruder.java
 *
 * =============================================================================
 *
 * Copyright (C) Stanford University, 2006.  All Rights Reserved.
 * Author: Chi Cao Minh
 *
 * =============================================================================
 *
 * For the license of bayes/sort.h and bayes/sort.c, please see the header
 * of the files.
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of kmeans, please see kmeans/LICENSE.kmeans
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of ssca2, please see ssca2/COPYRIGHT
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of lib/mt19937ar.c and lib/mt19937ar.h, please see the
 * header of the files.
 * 
 * ------------------------------------------------------------------------
 * 
 * For the license of lib/rbtree.h and lib/rbtree.c, please see
 * lib/LEGALNOTICE.rbtree and lib/LICENSE.rbtree
 * 
 * ------------------------------------------------------------------------
 * 
 * Unless otherwise noted, the following license applies to STAMP files:
 * 
 * Copyright (c) 2007, Stanford University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 * 
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 * 
 *     * Neither the name of Stanford University nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY STANFORD UNIVERSITY ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL STANFORD UNIVERSITY BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * =============================================================================
 */

package jstamp.jvstm.intruder;

import java.util.concurrent.atomic.AtomicInteger;

import jstamp.jvstm.CallableCollectAborts;
import jstamp.jvstm.CommandCollectAborts;
import jvstm.Transaction;

public class Intruder extends Thread {

    char PARAM_ATTACK = 'a';
    char  PARAM_LENGTH= 'l';
    char PARAM_NUM    ='n';
    char PARAM_SEED   ='s';
    char PARAM_THREAD ='t';

    int PARAM_DEFAULT_ATTACK =10;
    int PARAM_DEFAULT_LENGTH =16;
    int PARAM_DEFAULT_NUM =(1 << 16);
    int PARAM_DEFAULT_SEED =1;
    int PARAM_DEFAULT_THREAD =1;


    int percentAttack;
    int maxDataLength;
    int numFlow;
    int randomSeed;
    int numThread;

    int threadID;
    Arg argument;


    public Intruder(String[] argv) 
    {
	parseArg(argv);
    }

    public Intruder(int myID,Arg a)
    {
	argument = a;
	threadID = myID;
    }

    private void setDefaultParams() {
	percentAttack = PARAM_DEFAULT_ATTACK;
	maxDataLength = PARAM_DEFAULT_LENGTH;
	numFlow       = PARAM_DEFAULT_NUM;
	randomSeed    = PARAM_DEFAULT_SEED;
	numThread     = PARAM_DEFAULT_THREAD;
    }


    /* =============================================================================
     * displayUsage
     * =============================================================================
     */
    private void displayUsage()
    {   
	System.out.print  ("Usage: Intruder [options]\n");
	System.out.println("\nOptions:                            (defaults)\n");
	System.out.print  ("    a <UINT>   Percent [a]ttack     ");
	System.out.print  ("    l <UINT>   Max data [l]ength    ");
	System.out.print  ("    n <UINT>   [n]umber of flows    ");
	System.out.print  ("    s <UINT>   Random [s]eed        ");
	System.out.print  ("    t <UINT>   Number of [t]hreads  ");
	System.exit(1);
    }


    /* =============================================================================
     * parseArgs
     * =============================================================================
     */
    private void parseArg(String[] argv) 
    {
	int i=0;
	String arg;
	boolean opterr = false;

	setDefaultParams();

	while ( i< argv.length) {

	    if(argv[i].charAt(0) == '-') {
		arg = argv[i++];
		//check options
		if(arg.equals("-a")) {
		    percentAttack = Integer.parseInt(argv[i++]);
		}
		else if(arg.equals("-l")) {
		    maxDataLength = Integer.parseInt(argv[i++]);
		} 
		else if(arg.equals("-n")) {
		    numFlow = Integer.parseInt(argv[i++]);
		}
		else if(arg.equals("-s")) {
		    randomSeed = Integer.parseInt(argv[i++]);
		}
		else if(arg.equals("-t")) {
		    numThread = Integer.parseInt(argv[i++]);
		}
		else {
		    System.out.println("Non-option argument: " + argv[i]);
		    opterr = true;
		}
	    }
	}
	if(opterr) {
	    displayUsage();
	}
    }




    /* =============================================================================
     * processPackets
     * =============================================================================
     */
    public void processPackets(Arg argPtr)
    {
	// TM_THREAD_ENTER();

	Stream streamPtr = argPtr.streamPtr;
	Decoder decoderPtr = argPtr.decoderPtr;
	Vector_t[] errorVectors = argPtr.errorVectors;


	Detector detectorPtr = new Detector();
	detectorPtr.addPreprocessor(2);

	Vector_t errorVectorPtr = errorVectors[threadID];

	while(true) {
	    Packet packetPtr;


	    packetPtr = atomicGetPacket(streamPtr);


	    if(packetPtr == null) {
		break;
	    }
	    atomicProcess(decoderPtr, packetPtr);


	    byte[] data;
	    int[] decodedFlowId = new int[1];

	    data = atomicGetComplete(decoderPtr, decodedFlowId);

	    if(data != null) {
		int err = detectorPtr.process(data);

		if(err != 0) {
		    errorVectorPtr.vector_pushBack(new Integer(decodedFlowId[0]));
		}
	    }
	}

	// TM_THREAD_EXIT();

    }

    public static final AtomicInteger abortsPacket = new AtomicInteger(0);
    public static final AtomicInteger abortsProcess = new AtomicInteger(0);
    public static final AtomicInteger abortsComplete = new AtomicInteger(0);
    
    //    @Atomic
    private byte[] atomicGetComplete(final Decoder decoderPtr, final int[] decodedFlowId) {
	try {
	    CallableCollectAborts<byte[]> cmd = new CallableCollectAborts<byte[]>() {
		@Override
		public byte[] runTx() {
		    byte[] data;
		    data = decoderPtr.getComplete(decodedFlowId);
		    return data;
		}
	    };
	    byte[] r = Transaction.doIt(cmd);
	    if (cmd.getAborts() > 0) {
		abortsComplete.addAndGet(cmd.getAborts());
	    }
	    return r;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    //    @Atomic
    private void atomicProcess(final Decoder decoderPtr, final Packet packetPtr) {
	CommandCollectAborts cmd = new CommandCollectAborts() {
	    public void runTx() {
		decoderPtr.process(packetPtr);
	    }
	};
	Transaction.transactionallyDo(cmd);
	if (cmd.getAborts() > 0) {
	    abortsProcess.addAndGet(cmd.getAborts());
	}
    }

    //    @Atomic
    private Packet atomicGetPacket(final Stream streamPtr) {
	try {
	    CallableCollectAborts<Packet> cmd = new CallableCollectAborts<Packet>() {
		@Override
		public Packet runTx() {
		    return streamPtr.getPacket();
		}
	    };
	    Packet r = Transaction.doIt(cmd);
	    if (cmd.getAborts() > 0) {
		abortsPacket.addAndGet(cmd.getAborts());
	    }
	    return r;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    @Override
    public void run()
    {
	Barrier.enterBarrier();
	processPackets(argument);
	Barrier.enterBarrier();
    }

    /* =============================================================================
     * main
     * =============================================================================
     */

    public static void main(String[] argv) {
	/*
	 * Initialization
	 */

	ERROR er = new ERROR();

	Transaction.beginInevitable();
	Intruder in = new Intruder(argv);   // parsing argv

	Barrier.setBarrier(in.numThread + 1);

//	System.out.println("Percent attack  =   " + in.percentAttack);
//	System.out.println("Max data length =   " + in.maxDataLength);
//	System.out.println("Num flow        =   " + in.numFlow);
//	System.out.println("Random seed     =   " + in.randomSeed);
//	System.out.println("Thread count    =   " + in.numThread);

	Dictionary dictionaryPtr = new Dictionary();
	Stream streamPtr = new Stream(in.percentAttack);
	int numAttack = streamPtr.generate(dictionaryPtr,in.numFlow,in.randomSeed,in.maxDataLength);

//	System.out.println("Num Attack      =   " + numAttack);

	Decoder decoderPtr = new Decoder();
	Vector_t[] errorVectors = new Vector_t[in.numThread];

	int i;

	for(i =0;i< in.numThread;i++) {
	    errorVectors[i] = new Vector_t(in.numFlow);
	}

	Arg arg = new Arg();

	arg.streamPtr = streamPtr;
	arg.decoderPtr = decoderPtr;
	arg.errorVectors = errorVectors;

	in.argument = arg;

	// Run transactions

	Intruder[] intruders = new Intruder[in.numThread];

	for(i=0; i<in.numThread;i++) {
	    intruders[i] = new Intruder(i,arg);
	}
	in.threadID = 0;

	Transaction.commit();
	
	for(i = 0; i< in.numThread;i++) {
	    intruders[i].start();
	}

	long start = System.currentTimeMillis();

	Barrier.enterBarrier();
	start=System.currentTimeMillis();
	Barrier.enterBarrier();

	long finish = System.currentTimeMillis();
	long elapsed = finish - start;

	System.out.println(elapsed + " " + Intruder.abortsComplete.get() + " " + Intruder.abortsProcess.get() + " " + Intruder.abortsPacket.get());

	// finish
	//
	// Check solution

	Barrier.assertIsClear();

	int numFound = 0;
	Transaction.beginInevitable();
	for(i =0;i<in.numThread;i++) {
	    Vector_t errorVectorPtr = errorVectors[i];
	    int e;
	    int numError = errorVectorPtr.vector_getSize();
	    //System.out.println("numError = " + numError);
	    numFound += numError;
	    for (e = 0; e< numError; e++) {
		int flowId = ((Integer)errorVectorPtr.vector_at(e)).intValue();
		boolean status = streamPtr.isAttack(flowId);

		if(status == false) {
		    System.out.println("Assertion in check solution");
		    System.out.println(String.format("Problem at flowId = %d, status is false there. It's in errorVectorPtr position %d.", 
			    flowId, e));
		    System.exit(1);
		}
	    }
	}

//	System.out.println("Num found       = " + numFound);

	if(numFound != numAttack) {
	    System.out.println("Assertion in check solution");
	    System.out.println(String.format("Problem is that numFound (%d) is not equal to numAttack (%d).", 
		    numFound, numAttack));
	    System.exit(1);
	}

//	System.out.println("Finished");
	Transaction.commit();
    }

}

/* =============================================================================
 *
 * End of intruder.java
 *
 * =============================================================================
 */
