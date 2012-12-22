/* =============================================================================
 *
 * data.java
 *
 * =============================================================================
 *
 * Copyright (C) Stanford University, 2006.  All Rights Reserved.
 * Author: Chi Cao Minh
 * Ported to Java June 2009 Alokika Dash
 * University of California, Irvine
 *
 * =============================================================================
 *
 * For the license of bayes/sort.h and bayes/sort.c, please see the header
 * of the files.
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

package jstamp.jvstm.bayes;


public class Data {
    public static final int DATA_PRECISION = 100;
    public static final int DATA_INIT = 2; /* not 0 or 1 */
    
    final int numVar;
    final int numRecord;
    final byte[] records; /* coordination of all records */
    final Random randomPtr;
    final Sort sort;

    /* =============================================================================
     * data_alloc
     * =============================================================================
     */
    public Data(int numVar, int numRecord, Random randomPtr)
    {
	int numDatum = numVar * numRecord;
	records = new byte[numDatum];
	for(int i = 0; i<numDatum; i++)
	    this.records[i] = (byte)DATA_INIT;

	this.numVar = numVar;
	this.numRecord = numRecord;
	this.randomPtr = randomPtr;
	this.sort=new Sort();
    }


    /* =============================================================================
     * data_generate
     * -- Binary variables of random PDFs
     * -- If seed is <0, do not reseed
     * -- Returns random network
     * =============================================================================
     */
    public Net data_generate (int seed, int maxNumParent, int percentParent)
    {
	if (seed >= 0) {
	    randomPtr.random_seed(seed);
	}

	/*
	 * Generate random Bayesian network
	 */

	Net netPtr = new Net(numVar);
	netPtr.net_generateRandomEdges(maxNumParent, percentParent, randomPtr);

	/*
	 * Create a threshold for each of the possible permutation of variable
	 * value instances
	 */

	int[][] thresholdsTable = new int[numVar][]; 
	int v;
	for (v = 0; v < numVar; v++) {
	    IntList parentIdListPtr = netPtr.net_getParentIdListPtr(v);
	    int numThreshold = 1 << parentIdListPtr.list_getSize();
	    int[] thresholds = new int[numThreshold];
	    for (int t = 0; t < numThreshold; t++) {
		int threshold = (int) (randomPtr.random_generate() % (DATA_PRECISION + 1));
		thresholds[t] = threshold;
	    }
	    thresholdsTable[v] = thresholds;
	}

	/*
	 * Create variable dependency ordering for record generation
	 */

	int[] order = new int[numVar];
	int numOrder = 0;

	Queue workQueuePtr = Queue.queue_alloc(-1);

	IntVector dependencyVectorPtr = IntVector.vector_alloc(1);

	BitMap orderedBitmapPtr = BitMap.bitmap_alloc(numVar);
	orderedBitmapPtr.bitmap_clearAll();

	BitMap doneBitmapPtr = BitMap.bitmap_alloc(numVar);
	doneBitmapPtr.bitmap_clearAll();

	v = -1;
	while ((v = doneBitmapPtr.bitmap_findClear(v + 1)) >= 0) {
	    IntList childIdListPtr = netPtr.net_getChildIdListPtr(v);
	    int numChild = childIdListPtr.list_getSize();

	    if (numChild == 0) {

		boolean status;

		/*
		 * Use breadth-first search to find net connected to this leaf
		 */

		workQueuePtr.queue_clear();
		status = workQueuePtr.queue_push(v);

		while (!(workQueuePtr.queue_isEmpty())) {
		    int id = workQueuePtr.queue_pop();
		    status = doneBitmapPtr.bitmap_set(id);

		    status = dependencyVectorPtr.vector_pushBack(id);

		    IntList parentIdListPtr = netPtr.net_getParentIdListPtr(id);
		    IntListNode it = parentIdListPtr.head;

		    while (it.nextPtr.get() !=null) {
			it = it.nextPtr.get();
			int parentId = it.dataPtr;
			status = workQueuePtr.queue_push(parentId);
		    }
		}

		/*
		 * Create ordering
		 */

		 int n = dependencyVectorPtr.vector_getSize();
		 for (int i = 0; i < n; i++) {
		     int id = dependencyVectorPtr.vector_popBack();
		     if (!(orderedBitmapPtr.bitmap_isSet(id))) {
			 orderedBitmapPtr.bitmap_set(id);
			 order[numOrder++] = id;
		     }
		 }
	    }
	}


	/*
	 * Create records
	 */

	int startindex = 0;
	for (int r = 0; r < numRecord; r++) {
	    for (int o = 0; o < numOrder; o++) {
		v = order[o];
		IntList parentIdListPtr = netPtr.net_getParentIdListPtr(v);
		int index = 0;
		IntListNode it = parentIdListPtr.head;

		while (it.nextPtr.get() != null) {
		    it = it.nextPtr.get();
		    int parentId = it.dataPtr;
		    int value = records[startindex + parentId];
		    index = (index << 1) + value;
		}
		int rnd = (int) (randomPtr.random_generate() % DATA_PRECISION);
		int threshold = thresholdsTable[v][index];
		records[startindex + v] = (byte) ((rnd < threshold) ? 1 : 0);
	    }
	    startindex += numVar;
	}

	return netPtr;
    }


    /* =============================================================================
     * data_sort
     * -- In place
     * =============================================================================
     */
    public void
    data_sort (int start,
	    int num,
	    int offset)
    {
	sort.sort(records, 
		start * numVar,
		num,
		numVar,
		numVar,
		offset);

    }


    /* =============================================================================
     * data_findSplit
     * -- Call data_sort first with proper start, num, offset
     * -- Returns number of zeros in offset column
     * =============================================================================
     */
    public int
    data_findSplit (int start, int num, int offset)
    {
	int low = start;
	int high = start + num - 1;

	while (low <= high) {
	    int mid = (low + high) / 2;
	    if (records[numVar * mid + offset] == 0) {
		low = mid + 1;
	    } else {
		high = mid - 1;
	    }
	}

	return (low - start);
    }
}

/* =============================================================================
 *
 * End of data.java
 *
 * =============================================================================
 */
