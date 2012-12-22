package jstamp.jvstm.bayes;

/* =============================================================================
 *
 * adtree.java
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
 * Reference:
 *
 * A. Moore and M.-S. Lee. Cached sufficient statistics for efficient machine
 * learning with large datasets. Journal of Artificial Intelligence Research 8
 * (1998), pp 67-91.
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

public class Adtree {
    final int numVar;
    final int numRecord;
    final AdtreeNode rootNodePtr;

    /* =============================================================================
     * adtree_alloc
     * =============================================================================
     */
    public Adtree(Data dataPtr) {
	numVar = dataPtr.numVar;
	numRecord = dataPtr.numRecord;
	dataPtr.data_sort(0, numRecord, 0);
	rootNodePtr = makeNode(-1, -1, 0, numRecord, dataPtr, -1);
    }


    /* =============================================================================
     * makeVary
     * =============================================================================
     */
    public AdtreeVary makeVary (int parentIndex, int index, int start, int numRecord, Data dataPtr) {

	if ((parentIndex + 1 != index) && (numRecord > 1)) {
	    dataPtr.data_sort(start, numRecord, index);
	}

	int num0 = dataPtr.data_findSplit(start, numRecord, index);
	int num1 = numRecord - num0;

	int mostCommonValue = ((num0 >= num1) ? 0 : 1);

	AdtreeNode zeroNodePtr = null;
	AdtreeNode oneNodePtr = null;
	
	if (num0 == 0 || mostCommonValue == 0) {
	} else {
	    zeroNodePtr = makeNode(index, index, start, num0, dataPtr, 0);
	}

	if (num1 == 0 || mostCommonValue == 1) {
	} else {
	    oneNodePtr = makeNode(index, index, (start + num0), num1, dataPtr, 1);
	}
	AdtreeVary varyPtr = new AdtreeVary(index, mostCommonValue, zeroNodePtr, oneNodePtr);

	return varyPtr;
    }


    /* =============================================================================
     * makeNode
     * =============================================================================
     */
    public AdtreeNode makeNode (int parentIndex, int index, int start, int numRecord, Data dataPtr, int value) {
	int numVar = dataPtr.numVar;
	AdtreeNode nodePtr = new AdtreeNode(index, numVar-index-1, numRecord, value);

	AdtreeVary varyVectorPtr[] = nodePtr.varyVectorPtr;
	int i=0;

	for (int v = (index + 1); v < numVar; v++) {
	    AdtreeVary varyPtr = makeVary(parentIndex, v, start, numRecord, dataPtr);
	    varyVectorPtr[i++]=varyPtr;
	}

	return nodePtr;
    }




    /* =============================================================================
     * getCount
     * =============================================================================
     */
    public int
    getCount (AdtreeNode nodePtr,
	    int i,
	    int q,
	    Vector_t queryVectorPtr,
	    int lastQueryIndex)
    {
	if (nodePtr == null) {
	    return 0;
	}

	int nodeIndex = nodePtr.index;
	if (nodeIndex >= lastQueryIndex) {
	    return nodePtr.count;
	}

	int count = 0;

	Query queryPtr = (Query)(queryVectorPtr.vector_at(q));

	if (queryPtr == null) {
	    return nodePtr.count;
	}
	int queryIndex = queryPtr.index;

	AdtreeVary varyPtr = nodePtr.varyVectorPtr[queryIndex - nodeIndex - 1];

	int queryValue = queryPtr.value;

	if (queryValue == varyPtr.mostCommonValue) {

	    /*
	     * We do not explicitly store the counts for the most common value.
	     * We can calculate it by finding the count of the query without
	     * the current (superCount) and subtracting the count for the
	     * query with the current toggled (invertCount).
	     */
	    int numQuery = queryVectorPtr.vector_getSize();
	    Vector_t superQueryVectorPtr = new Vector_t(numQuery - 1);

	    for (int qq = 0; qq < numQuery; qq++) {
		if (qq != q) {
		    boolean status = superQueryVectorPtr.vector_pushBack(queryVectorPtr.vector_at(qq));
		}
	    }

	    int superCount = adtree_getCount(superQueryVectorPtr);
	    superQueryVectorPtr.clear();

	    int invertCount;
	    if (queryValue == 0) {
		queryPtr.value = 1;
		invertCount = getCount(nodePtr,
			i,
			q,
			queryVectorPtr,
			lastQueryIndex);
		queryPtr.value = 0;
	    } else {
		queryPtr.value = 0;
		invertCount = getCount(nodePtr,
			i,
			q,
			queryVectorPtr,
			lastQueryIndex);
		queryPtr.value = 1;
	    }
	    count += superCount - invertCount;

	} else {

	    if (queryValue == 0) {
		count += getCount(varyPtr.zeroNodePtr,
			(i + 1),
			(q + 1),
			queryVectorPtr,
			lastQueryIndex);
	    } else if (queryValue == 1) {
		count += getCount(varyPtr.oneNodePtr,
			(i + 1),
			(q + 1),
			queryVectorPtr,
			lastQueryIndex);
	    }

	}

	return count;
    }


    /* =============================================================================
     * adtree_getCount
     * -- queryVector must consist of queries sorted by id
     * =============================================================================
     */
    public int
    adtree_getCount (Vector_t queryVectorPtr)
    {
	if (rootNodePtr == null) {
	    return 0;
	}

	int lastQueryIndex = -1;
	int numQuery = queryVectorPtr.vector_getSize();
	if (numQuery > 0) {
	    Query lastQueryPtr = (Query)(queryVectorPtr.vector_at(numQuery - 1));
	    lastQueryIndex = lastQueryPtr.index;
	}

	return getCount(rootNodePtr,
		-1,
		0,
		queryVectorPtr,
		lastQueryIndex);
    }
}
/* =============================================================================
 *
 * End of adtree.java
 *
 * =============================================================================
 */
