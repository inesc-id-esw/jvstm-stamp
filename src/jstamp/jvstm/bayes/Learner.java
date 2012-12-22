package jstamp.jvstm.bayes;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import jstamp.jvstm.CallableCollectAborts;
import jstamp.jvstm.CommandCollectAborts;
import jvstm.Transaction;
import jvstm.TransactionalCommand;
import jvstm.VBoxFloat;
import jvstm.VBoxInt;

/* =============================================================================
 *

 * learner.java
 * -- Learns structure of Bayesian net from data
 *
 * =============================================================================
 *
 * Copyright (C) Stanford University, 2006.  All Rights Reserved.
 * Author: Chi Cao Minh
 * Ported to Java June 2009 Alokika Dash
 * University of California, Irvine
 *
 *
 * =============================================================================
 *
 * The penalized log-likelihood score (Friedman & Yahkani, 1996) is used to
 * evaluated the "goodness" of a Bayesian net:
 *
 *                             M      n_j
 *                            --- --- ---
 *  -N_params * ln(R) / 2 + R >   >   >   P((a_j = v), X_j) ln P(a_j = v | X_j)
 *                            --- --- ---
 *                            j=1 X_j v=1
 *
 * Where:
 *
 *     N_params     total number of parents across all variables
 *     R            number of records
 *     M            number of variables
 *     X_j          parents of the jth variable
 *     n_j          number of attributes of the jth variable
 *     a_j          attribute
 *
 * The second summation of X_j varies across all possible assignments to the
 * values of the parents X_j.
 *
 * In the code:
 *
 *    "local log likelihood" is  P((a_j = v), X_j) ln P(a_j = v | X_j)
 *    "log likelihood" is everything to the right of the '+', i.e., "R ... X_j)"
 *    "base penalty" is -ln(R) / 2
 *    "penalty" is N_params * -ln(R) / 2
 *    "score" is the entire expression
 *
 * For more notes, refer to:
 *
 * A. Moore and M.-S. Lee. Cached sufficient statistics for efficient machine
 * learning with large datasets. Journal of Artificial Intelligence Research 8
 * (1998), pp 67-91.
 *
 * =============================================================================
 *
 * The search strategy uses a combination of local and global structure search.
 * Similar to the technique described in:
 *
 * D. M. Chickering, D. Heckerman, and C. Meek.  A Bayesian approach to learning
 * Bayesian networks with local structure. In Proceedings of Thirteenth
 * Conference on Uncertainty in Artificial Intelligence (1997), pp. 80-89.
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



public class Learner {
    public static final int CACHE_LINE_SIZE = 64;
    public static final int  QUERY_VALUE_WILDCARD = -1;
    public static final int  OPERATION_INSERT = 0;
    public static final int  OPERATION_REMOVE = 1;
    public static final int  OPERATION_REVERSE = 2;
    public static final int  NUM_OPERATION = 3;

    public static final AtomicInteger aborts = new AtomicInteger(0);

    Adtree adtreePtr;
    Net netPtr;
    final VBoxFloat[] localBaseLogLikelihoods;
    final VBoxFloat baseLogLikelihood;
    LearnerTask[] tasks;
    List taskListPtr;
    final VBoxInt numTotalParent;
    int global_insertPenalty;
    int global_maxNumEdgeLearned;
    float global_operationQualityFactor;

    public Learner(Data dataPtr, 
	    Adtree adtreePtr, 
	    int numThread, 
	    int global_insertPenalty,
	    int global_maxNumEdgeLearned,
	    float global_operationQualityFactor) {
	this.adtreePtr = adtreePtr;
	this.netPtr = new Net(dataPtr.numVar);
	this.localBaseLogLikelihoods = new VBoxFloat[dataPtr.numVar];
	for (int i = 0; i < localBaseLogLikelihoods.length; i++) {
	    this.localBaseLogLikelihoods[i] = new VBoxFloat(0.0f);
	}
	this.baseLogLikelihood = new VBoxFloat(0.0f);
	this.tasks = new LearnerTask[dataPtr.numVar];
	this.taskListPtr = List.list_alloc();
	this.numTotalParent = new VBoxInt(0);
	this.global_insertPenalty = global_insertPenalty;
	this.global_maxNumEdgeLearned = global_maxNumEdgeLearned;
	this.global_operationQualityFactor = global_operationQualityFactor;
    }

    public void learner_free() {
	adtreePtr=null;
	netPtr=null;
	tasks=null;
	taskListPtr=null;
    }


    /* =============================================================================
     * computeSpecificLocalLogLikelihood
     * -- Query vectors should not contain wildcards
     * =============================================================================
     */
    public float
    computeSpecificLocalLogLikelihood (Adtree adtreePtr,
	    Vector_t queryVectorPtr,
	    Vector_t parentQueryVectorPtr)
    {
	int count = adtreePtr.adtree_getCount(queryVectorPtr);
	if (count == 0) {
	    return 0.0f;
	}

	double probability = (double)count / (double)adtreePtr.numRecord;
	int parentCount = adtreePtr.adtree_getCount(parentQueryVectorPtr);


	float fval = (float)(probability * (Math.log((double)count/ (double)parentCount)));

	return fval;
    }


    /* =============================================================================
     * createPartition
     * =============================================================================
     */
    public void
    createPartition (int min, int max, int id, int n, LocalStartStop lss)
    {
	int range = max - min;
	int chunk = Math.max(1, ((range + n/2) / n)); // rounded 
	int start = min + chunk * id;
	int stop;
	if (id == (n-1)) {
	    stop = max;
	} else {
	    stop = Math.min(max, (start + chunk));
	}

	lss.i_start = start;
	lss.i_stop = stop;
    }

    /* =============================================================================
     * createTaskList
     * -- baseLogLikelihoods and taskListPtr are updated
     * =============================================================================
     */
    public static void
    createTaskList (int myId, int numThread, final Learner learnerPtr)
    {
	boolean status;

	Query[] queries = new Query[2];
	queries[0] = new Query();
	queries[1] = new Query();

	Vector_t queryVectorPtr = new Vector_t(2);

	status = queryVectorPtr.vector_pushBack(queries[0]);

	Query parentQuery = new Query();
	Vector_t parentQueryVectorPtr = new Vector_t(1); 

	int numVar = learnerPtr.adtreePtr.numVar;
	int numRecord = learnerPtr.adtreePtr.numRecord;
	float baseLogLikelihood = 0.0f;
	float penalty = (float)(-0.5f * Math.log((double)numRecord)); // only add 1 edge 

	LocalStartStop lss = new LocalStartStop();
	learnerPtr.createPartition(0, numVar, myId, numThread, lss);

	/*
	 * Compute base log likelihood for each variable and total base loglikelihood
	 */

	for (int v = lss.i_start; v < lss.i_stop; v++) {

	    float localBaseLogLikelihood = 0.0f;
	    queries[0].index = v;

	    queries[0].value = 0;
	    localBaseLogLikelihood +=
		    learnerPtr.computeSpecificLocalLogLikelihood(learnerPtr.adtreePtr,
			    queryVectorPtr,
			    parentQueryVectorPtr);

	    queries[0].value = 1;
	    localBaseLogLikelihood +=
		    learnerPtr.computeSpecificLocalLogLikelihood(learnerPtr.adtreePtr,
			    queryVectorPtr,
			    parentQueryVectorPtr);

	    learnerPtr.localBaseLogLikelihoods[v].put(localBaseLogLikelihood);
	    baseLogLikelihood += localBaseLogLikelihood;

	} // for each variable 

	final float baseLogLikelihoodFinal = baseLogLikelihood;
	CommandCollectAborts cmd = new CommandCollectAborts() {
	    public void runTx() {
		float globalBaseLogLikelihood = learnerPtr.baseLogLikelihood.getFloat();
		learnerPtr.baseLogLikelihood.put((baseLogLikelihoodFinal + globalBaseLogLikelihood));
	    }
	};
	Transaction.transactionallyDo(cmd);
	if (cmd.getAborts() > 0) {
	    aborts.addAndGet(cmd.getAborts());
	}


	/*
	 * For each variable, find if the addition of any edge _to_ it is better
	 */

	status = parentQueryVectorPtr.vector_pushBack(parentQuery);

	for (int v = lss.i_start; v < lss.i_stop; v++) {

	    //Compute base log likelihood for this variable

	    queries[0].index = v;
	    int bestLocalIndex = v;
	    float bestLocalLogLikelihood = learnerPtr.localBaseLogLikelihoods[v].getFloat();

	    status = queryVectorPtr.vector_pushBack(queries[1]);

	    for (int vv = 0; vv < numVar; vv++) {

		if (vv == v) {
		    continue;
		}
		parentQuery.index = vv;
		if (v < vv) {
		    queries[0].index = v;
		    queries[1].index = vv;
		} else {
		    queries[0].index = vv;
		    queries[1].index = v;
		}

		float newLocalLogLikelihood = 0.0f;

		queries[0].value = 0;
		queries[1].value = 0;
		parentQuery.value = 0;
		newLocalLogLikelihood +=
			learnerPtr.computeSpecificLocalLogLikelihood(learnerPtr.adtreePtr,
				queryVectorPtr,
				parentQueryVectorPtr);

		queries[0].value = 0;
		queries[1].value = 1;
		parentQuery.value = ((vv < v) ? 0 : 1);
		newLocalLogLikelihood +=
			learnerPtr.computeSpecificLocalLogLikelihood(learnerPtr.adtreePtr,
				queryVectorPtr,
				parentQueryVectorPtr);

		queries[0].value = 1;
		queries[1].value = 0;
		parentQuery.value = ((vv < v) ? 1 : 0);
		newLocalLogLikelihood +=
			learnerPtr.computeSpecificLocalLogLikelihood(learnerPtr.adtreePtr,
				queryVectorPtr,
				parentQueryVectorPtr);

		queries[0].value = 1;
		queries[1].value = 1;
		parentQuery.value = 1;
		newLocalLogLikelihood +=
			learnerPtr.computeSpecificLocalLogLikelihood(learnerPtr.adtreePtr,
				queryVectorPtr,
				parentQueryVectorPtr);

		if (newLocalLogLikelihood > bestLocalLogLikelihood) {
		    bestLocalIndex = vv;
		    bestLocalLogLikelihood = newLocalLogLikelihood;
		}

	    } // foreach other variable 

	    queryVectorPtr.vector_popBack();

	    if (bestLocalIndex != v) {
		float logLikelihood = numRecord * (baseLogLikelihood +
			+ bestLocalLogLikelihood
			- learnerPtr.localBaseLogLikelihoods[v].getFloat());
		float score = penalty + logLikelihood;

		learnerPtr.tasks[v] = new LearnerTask();
		final LearnerTask taskPtr = learnerPtr.tasks[v];
		taskPtr.op = OPERATION_INSERT;
		taskPtr.fromId = bestLocalIndex;
		taskPtr.toId = v;
		taskPtr.score = score;
		try {
		    CallableCollectAborts<Boolean> cmdC = new CallableCollectAborts<Boolean>() {
			public Boolean runTx() {
			    return learnerPtr.taskListPtr.list_insert(taskPtr);
			}
		    };
		    status = Transaction.doIt(cmdC);
		    if (cmdC.getAborts() > 0) {
			aborts.addAndGet(cmdC.getAborts());
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		    System.exit(-1);
		}


	    }

	} // for each variable 


	queryVectorPtr.clear();
	parentQueryVectorPtr.clear();

	ListNode it = learnerPtr.taskListPtr.head;

	while (it.nextPtr.get() != null) {
	    it = it.nextPtr.get();
	    LearnerTask taskPtr = it.dataPtr;
	    //	    System.out.println("[task] op= "+ taskPtr.op +" from= "+taskPtr.fromId+" to= " +taskPtr.toId+
	    //		    " score= " + taskPtr.score);
	}

    }

    /* =============================================================================
     * TMpopTask
     * -- Returns null is list is empty
     * =============================================================================
     */
    public LearnerTask TMpopTask (List taskListPtr)
    {
	LearnerTask taskPtr = null;

	ListNode it = taskListPtr.head;
	if (it.nextPtr.get() != null) {
	    it = it.nextPtr.get();
	    taskPtr = it.dataPtr;
	    boolean status = taskListPtr.list_remove(taskPtr);
	}

	return taskPtr;
    }


    /* =============================================================================
     * populateParentQuery
     * -- Modifies contents of parentQueryVectorPtr
     * =============================================================================
     */
    public void
    populateParentQueryVector (Net netPtr,
	    int id,
	    Query[] queries,
	    Vector_t parentQueryVectorPtr)
    {
	parentQueryVectorPtr.vector_clear();

	IntList parentIdListPtr = netPtr.net_getParentIdListPtr(id);
	IntListNode it = parentIdListPtr.head;
	while (it.nextPtr.get() != null) {
	    it = it.nextPtr.get();
	    int parentId = it.dataPtr;
	    boolean status = parentQueryVectorPtr.vector_pushBack(queries[parentId]);
	}
    }


    /* =============================================================================
     * TMpopulateParentQuery
     * -- Modifies contents of parentQueryVectorPtr
     * =============================================================================
     */
    public void
    TMpopulateParentQueryVector (Net netPtr,
	    int id,
	    Query[] queries,
	    Vector_t parentQueryVectorPtr)
    {
	parentQueryVectorPtr.vector_clear();

	IntList parentIdListPtr = netPtr.net_getParentIdListPtr(id);
	IntListNode it = parentIdListPtr.head;

	while (it.nextPtr.get() !=null) {
	    it = it.nextPtr.get();
	    int parentId = it.dataPtr;
	    boolean status = parentQueryVectorPtr.vector_pushBack(queries[parentId]);
	}
    }


    /* =============================================================================
     * populateQueryVectors
     * -- Modifies contents of queryVectorPtr and parentQueryVectorPtr
     * =============================================================================
     */
    public void
    populateQueryVectors (Net netPtr,
	    int id,
	    Query[] queries,
	    Vector_t queryVectorPtr,
	    Vector_t parentQueryVectorPtr)
    {
	populateParentQueryVector(netPtr, id, queries, parentQueryVectorPtr);

	boolean status;
	status = Vector_t.vector_copy(queryVectorPtr, parentQueryVectorPtr);
	status = queryVectorPtr.vector_pushBack(queries[id]);


	queryVectorPtr.vector_sort();
    }


    /* =============================================================================
     * TMpopulateQueryVectors
     * -- Modifies contents of queryVectorPtr and parentQueryVectorPtr
     * =============================================================================
     */
    public void
    TMpopulateQueryVectors (Net netPtr,
	    int id,
	    Query[] queries,
	    Vector_t queryVectorPtr,
	    Vector_t parentQueryVectorPtr)
    {
	TMpopulateParentQueryVector(netPtr, id, queries, parentQueryVectorPtr);

	boolean status;
	status = Vector_t.vector_copy(queryVectorPtr, parentQueryVectorPtr);
	status = queryVectorPtr.vector_pushBack(queries[id]);

	queryVectorPtr.vector_sort();
    }

    /* =============================================================================
     * computeLocalLogLikelihoodHelper
     * -- Recursive helper routine
     * =============================================================================
     */
    public float
    computeLocalLogLikelihoodHelper (int i,
	    int numParent,
	    Adtree adtreePtr,
	    Query[] queries,
	    Vector_t queryVectorPtr,
	    Vector_t parentQueryVectorPtr)
    {
	if (i >= numParent) {
	    return computeSpecificLocalLogLikelihood(adtreePtr,
		    queryVectorPtr,
		    parentQueryVectorPtr);
	}

	float localLogLikelihood = 0.0f;

	Query parentQueryPtr = (Query) (parentQueryVectorPtr.vector_at(i));
	int parentIndex = parentQueryPtr.index;

	queries[parentIndex].value = 0;
	localLogLikelihood += computeLocalLogLikelihoodHelper((i + 1),
		numParent,
		adtreePtr,
		queries,
		queryVectorPtr,
		parentQueryVectorPtr);

	queries[parentIndex].value = 1;
	localLogLikelihood += computeLocalLogLikelihoodHelper((i + 1),
		numParent,
		adtreePtr,
		queries,
		queryVectorPtr,
		parentQueryVectorPtr);

	queries[parentIndex].value = QUERY_VALUE_WILDCARD;

	return localLogLikelihood;
    }


    /* =============================================================================
     * computeLocalLogLikelihood
     * -- Populate the query vectors before passing as args
     * =============================================================================
     */
    public float
    computeLocalLogLikelihood (int id,
	    Adtree adtreePtr,
	    Net netPtr,
	    Query[] queries,
	    Vector_t queryVectorPtr,
	    Vector_t parentQueryVectorPtr)
    {
	int numParent = parentQueryVectorPtr.vector_getSize();
	float localLogLikelihood = 0.0f;

	queries[id].value = 0;
	localLogLikelihood += computeLocalLogLikelihoodHelper(0,
		numParent,
		adtreePtr,
		queries,
		queryVectorPtr,
		parentQueryVectorPtr);

	queries[id].value = 1;
	localLogLikelihood += computeLocalLogLikelihoodHelper(0,
		numParent,
		adtreePtr,
		queries,
		queryVectorPtr,
		parentQueryVectorPtr);

	queries[id].value = QUERY_VALUE_WILDCARD;

	return localLogLikelihood;
    }


    /* =============================================================================
     * TMfindBestInsertTask
     * =============================================================================
     */
    public LearnerTask
    TMfindBestInsertTask (FindBestTaskArg argPtr)
    {
	int       toId                      = argPtr.toId;
	Learner   learnerPtr                = argPtr.learnerPtr;
	Query[]   queries                   = argPtr.queries;
	Vector_t  queryVectorPtr            = argPtr.queryVectorPtr;
	Vector_t  parentQueryVectorPtr      = argPtr.parentQueryVectorPtr;
	int       numTotalParent            = argPtr.numTotalParent;
	float     basePenalty               = argPtr.basePenalty;
	float     baseLogLikelihood         = argPtr.baseLogLikelihood;
	BitMap    invalidBitmapPtr          = argPtr.bitmapPtr;
	Queue     workQueuePtr              = argPtr.workQueuePtr;
	Vector_t  baseParentQueryVectorPtr  = argPtr.aQueryVectorPtr;
	Vector_t  baseQueryVectorPtr        = argPtr.bQueryVectorPtr;

	boolean status;
	Adtree adtreePtr               = learnerPtr.adtreePtr;
	Net    netPtr                  = learnerPtr.netPtr;

	TMpopulateParentQueryVector(netPtr, toId, queries, parentQueryVectorPtr);

	/*
	 * Create base query and parentQuery
	 */

	status = Vector_t.vector_copy(baseParentQueryVectorPtr, parentQueryVectorPtr);

	status = Vector_t.vector_copy(baseQueryVectorPtr, baseParentQueryVectorPtr);

	status = baseQueryVectorPtr.vector_pushBack(queries[toId]);

	queryVectorPtr.vector_sort();

	/*
	 * Search all possible valid operations for better local log likelihood
	 */

	int bestFromId = toId; // flag for not found 
	float oldLocalLogLikelihood = learnerPtr.localBaseLogLikelihoods[toId].getFloat();
	float bestLocalLogLikelihood = oldLocalLogLikelihood;

	status = netPtr.net_findDescendants(toId, invalidBitmapPtr, workQueuePtr);

	int fromId = -1;

	IntList parentIdListPtr = netPtr.net_getParentIdListPtr(toId);

	int maxNumEdgeLearned = global_maxNumEdgeLearned;

	if ((maxNumEdgeLearned < 0) ||
		(parentIdListPtr.list_getSize() <= maxNumEdgeLearned))
	{

	    IntListNode it = parentIdListPtr.head;

	    while(it.nextPtr.get() != null) {
		it = it.nextPtr.get();
		int parentId = it.dataPtr;
		invalidBitmapPtr.bitmap_set(parentId); // invalid since already have edge 
	    }

	    while ((fromId = invalidBitmapPtr.bitmap_findClear((fromId + 1))) >= 0) {

		if (fromId == toId) {
		    continue;
		}

		status = Vector_t.vector_copy(queryVectorPtr, baseQueryVectorPtr);

		status = queryVectorPtr.vector_pushBack(queries[fromId]);

		queryVectorPtr.vector_sort();

		status = Vector_t.vector_copy(parentQueryVectorPtr, baseParentQueryVectorPtr);
		status = parentQueryVectorPtr.vector_pushBack(queries[fromId]);

		parentQueryVectorPtr.vector_sort();

		float newLocalLogLikelihood =
			computeLocalLogLikelihood(toId,
				adtreePtr,
				netPtr,
				queries,
				queryVectorPtr,
				parentQueryVectorPtr);

		if (newLocalLogLikelihood > bestLocalLogLikelihood) {
		    bestLocalLogLikelihood = newLocalLogLikelihood;
		    bestFromId = fromId;
		}

	    } // foreach valid parent 

	} // if have not exceeded max number of edges to learn 

	/*
	 * Return best task; Note: if none is better, fromId will equal toId
	 */

	LearnerTask bestTask = new LearnerTask();
	bestTask.op     = OPERATION_INSERT;
	bestTask.fromId = bestFromId;
	bestTask.toId   = toId;
	bestTask.score  = 0.0f;

	if (bestFromId != toId) {
	    int numRecord = adtreePtr.numRecord;
	    int numParent = parentIdListPtr.list_getSize() + 1;
	    float penalty =
		    (numTotalParent + numParent * global_insertPenalty) * basePenalty;
	    float logLikelihood = numRecord * (baseLogLikelihood +
		    + bestLocalLogLikelihood
		    - oldLocalLogLikelihood);
	    float bestScore = penalty + logLikelihood;
	    bestTask.score  = bestScore;
	}

	return bestTask;
    }

    /* =============================================================================
     * TMfindBestRemoveTask
     * =============================================================================
     */
    public LearnerTask
    TMfindBestRemoveTask (FindBestTaskArg argPtr)
    {
	int       toId                     = argPtr.toId;
	Learner   learnerPtr               = argPtr.learnerPtr;
	Query[]   queries                  = argPtr.queries;
	Vector_t  queryVectorPtr           = argPtr.queryVectorPtr;
	Vector_t  parentQueryVectorPtr     = argPtr.parentQueryVectorPtr;
	int       numTotalParent           = argPtr.numTotalParent;
	float     basePenalty              = argPtr.basePenalty;
	float     baseLogLikelihood        = argPtr.baseLogLikelihood;
	Vector_t  origParentQueryVectorPtr = argPtr.aQueryVectorPtr;

	boolean status;
	Adtree adtreePtr = learnerPtr.adtreePtr;
	Net netPtr = learnerPtr.netPtr;
	VBoxFloat[] localBaseLogLikelihoods = learnerPtr.localBaseLogLikelihoods;

	TMpopulateParentQueryVector(netPtr, toId, queries, origParentQueryVectorPtr);
	int numParent = origParentQueryVectorPtr.vector_getSize();

	/*
	 * Search all possible valid operations for better local log likelihood
	 */

	int bestFromId = toId; // flag for not found 
	float oldLocalLogLikelihood = localBaseLogLikelihoods[toId].getFloat();
	float bestLocalLogLikelihood = oldLocalLogLikelihood;

	int i;
	for (i = 0; i < numParent; i++) {

	    Query queryPtr = (Query) (origParentQueryVectorPtr.vector_at(i));
	    int fromId = queryPtr.index;

	    /*
	     * Create parent query (subset of parents since remove an edge)
	     */

	    parentQueryVectorPtr.vector_clear();

	    for (int p = 0; p < numParent; p++) {
		if (p != fromId) {
		    Query tmpqueryPtr = (Query) (origParentQueryVectorPtr.vector_at(p));
		    status = parentQueryVectorPtr.vector_pushBack(queries[tmpqueryPtr.index]);
		}
	    } // create new parent query 

	    /*
	     * Create query
	     */

	    status = Vector_t.vector_copy(queryVectorPtr, parentQueryVectorPtr);
	    status = queryVectorPtr.vector_pushBack(queries[toId]);
	    queryVectorPtr.vector_sort();

	    /*
	     * See if removing parent is better
	     */

	    float newLocalLogLikelihood =
		    computeLocalLogLikelihood(toId,
			    adtreePtr,
			    netPtr,
			    queries,
			    queryVectorPtr,
			    parentQueryVectorPtr);

	    if (newLocalLogLikelihood > bestLocalLogLikelihood) {
		bestLocalLogLikelihood = newLocalLogLikelihood;
		bestFromId = fromId;
	    }

	} // for each parent 

	/*
	 * Return best task; Note: if none is better, fromId will equal toId
	 */

	LearnerTask bestTask = new LearnerTask();
	bestTask.op     = OPERATION_REMOVE;
	bestTask.fromId = bestFromId;
	bestTask.toId   = toId;
	bestTask.score  = 0.0f;

	if (bestFromId != toId) {
	    int numRecord = adtreePtr.numRecord;
	    float penalty = (numTotalParent - 1) * basePenalty;
	    float logLikelihood = numRecord * (baseLogLikelihood +
		    + bestLocalLogLikelihood
		    - oldLocalLogLikelihood);
	    float bestScore = penalty + logLikelihood;
	    bestTask.score  = bestScore;
	}

	return bestTask;
    }


    /* =============================================================================
     * TMfindBestReverseTask
     * =============================================================================
     */
    public LearnerTask
    TMfindBestReverseTask (FindBestTaskArg argPtr)
    {
	int       toId                         = argPtr.toId;
	Learner   learnerPtr                   = argPtr.learnerPtr;
	Query[]   queries                      = argPtr.queries;
	Vector_t  queryVectorPtr               = argPtr.queryVectorPtr;
	Vector_t  parentQueryVectorPtr         = argPtr.parentQueryVectorPtr;
	int       numTotalParent               = argPtr.numTotalParent;
	float     basePenalty                  = argPtr.basePenalty;
	float     baseLogLikelihood            = argPtr.baseLogLikelihood;
	BitMap    visitedBitmapPtr             = argPtr.bitmapPtr;
	Queue     workQueuePtr                 = argPtr.workQueuePtr;
	Vector_t  toOrigParentQueryVectorPtr   = argPtr.aQueryVectorPtr;
	Vector_t  fromOrigParentQueryVectorPtr = argPtr.bQueryVectorPtr;

	boolean status;
	Adtree adtreePtr = learnerPtr.adtreePtr;
	Net netPtr = learnerPtr.netPtr;
	VBoxFloat[] localBaseLogLikelihoods = learnerPtr.localBaseLogLikelihoods;

	TMpopulateParentQueryVector(netPtr, toId, queries, toOrigParentQueryVectorPtr);
	int numParent = toOrigParentQueryVectorPtr.vector_getSize();

	/*
	 * Search all possible valid operations for better local log likelihood
	 */

	int bestFromId = toId; // flag for not found 
	float oldLocalLogLikelihood = localBaseLogLikelihoods[toId].getFloat();
	float bestLocalLogLikelihood = oldLocalLogLikelihood;
	int fromId = 0;

	for (int i = 0; i < numParent; i++) {

	    Query queryPtr = (Query) (toOrigParentQueryVectorPtr.vector_at(i));
	    fromId = queryPtr.index;

	    bestLocalLogLikelihood =
		    oldLocalLogLikelihood + localBaseLogLikelihoods[fromId].getFloat();

	    TMpopulateParentQueryVector(netPtr,
		    fromId,
		    queries,
		    fromOrigParentQueryVectorPtr);

	    /*
	     * Create parent query (subset of parents since remove an edge)
	     */

	    parentQueryVectorPtr.vector_clear();

	    for (int p = 0; p < numParent; p++) {
		if (p != fromId) {
		    Query tmpqueryPtr = (Query) (toOrigParentQueryVectorPtr.vector_at(p));
		    status = parentQueryVectorPtr.vector_pushBack(queries[tmpqueryPtr.index]);
		}
	    } // create new parent query 

	    /*
	     * Create query
	     */

	    status = Vector_t.vector_copy(queryVectorPtr, parentQueryVectorPtr);
	    status = queryVectorPtr.vector_pushBack(queries[toId]);

	    queryVectorPtr.vector_sort();

	    /*
	     * Get log likelihood for removing parent from toId
	     */

	    float newLocalLogLikelihood =
		    computeLocalLogLikelihood(toId,
			    adtreePtr,
			    netPtr,
			    queries,
			    queryVectorPtr,
			    parentQueryVectorPtr);

	    /*
	     * Get log likelihood for adding parent to fromId
	     */

	    status = Vector_t.vector_copy(parentQueryVectorPtr, fromOrigParentQueryVectorPtr);
	    status = parentQueryVectorPtr.vector_pushBack(queries[toId]);

	    parentQueryVectorPtr.vector_sort();

	    status = Vector_t.vector_copy(queryVectorPtr, parentQueryVectorPtr);

	    status = queryVectorPtr.vector_pushBack(queries[fromId]);

	    queryVectorPtr.vector_sort();

	    newLocalLogLikelihood +=
		    computeLocalLogLikelihood(fromId,
			    adtreePtr,
			    netPtr,
			    queries,
			    queryVectorPtr,
			    parentQueryVectorPtr);

	    /*
	     * Record best
	     */

	    if (newLocalLogLikelihood > bestLocalLogLikelihood) {
		bestLocalLogLikelihood = newLocalLogLikelihood;
		bestFromId = fromId;
	    }

	} // for each parent 

	/*
	 * Check validity of best
	 */

	if (bestFromId != toId) {
	    boolean isTaskValid = true;
	    netPtr.net_applyOperation(OPERATION_REMOVE, bestFromId, toId);
	    if (netPtr.net_isPath(bestFromId,
		    toId,
		    visitedBitmapPtr,
		    workQueuePtr))
	    {
		isTaskValid = false;
	    }
	    netPtr.net_applyOperation(OPERATION_INSERT, bestFromId, toId);
	    if (!isTaskValid) {
		bestFromId = toId;
	    }
	}

	/*
	 * Return best task; Note: if none is better, fromId will equal toId
	 */

	LearnerTask bestTask = new LearnerTask();
	bestTask.op     = OPERATION_REVERSE;
	bestTask.fromId = bestFromId;
	bestTask.toId   = toId;
	bestTask.score  = 0.0f;

	if (bestFromId != toId) {
	    float fromLocalLogLikelihood = localBaseLogLikelihoods[bestFromId].getFloat();
	    int numRecord = adtreePtr.numRecord;
	    float penalty = numTotalParent * basePenalty;
	    float logLikelihood = numRecord * (baseLogLikelihood +
		    + bestLocalLogLikelihood
		    - oldLocalLogLikelihood
		    - fromLocalLogLikelihood);
	    float bestScore = penalty + logLikelihood;
	    bestTask.score  = bestScore;
	}

	return bestTask;
    }



    /* =============================================================================
     * learnStructure
     *
     * Note it is okay if the score is not exact, as we are relaxing the greedy
     * search. This means we do not need to communicate baseLogLikelihood across
     * threads.
     * =============================================================================
     */
    public static void
    learnStructure (int myId, int numThread, final Learner learnerPtr)
    {

	final int numRecord = learnerPtr.adtreePtr.numRecord;

	float operationQualityFactor = learnerPtr.global_operationQualityFactor;

	BitMap visitedBitmapPtr = BitMap.bitmap_alloc(learnerPtr.adtreePtr.numVar);

	Queue workQueuePtr = Queue.queue_alloc(-1);

	int numVar = learnerPtr.adtreePtr.numVar;
	final Query[] queries = new Query[numVar];

	for (int v = 0; v < numVar; v++) {
	    queries[v] = new Query();
	    queries[v].index = v;
	    queries[v].value = QUERY_VALUE_WILDCARD;
	}

	final float basePenalty = (float)(-0.5 * Math.log((double)numRecord));

	final Vector_t queryVectorPtr = new Vector_t(1);
	final Vector_t parentQueryVectorPtr = new Vector_t(1);
	Vector_t aQueryVectorPtr = new Vector_t(1);
	Vector_t bQueryVectorPtr = new Vector_t(1);

	final FindBestTaskArg arg = new FindBestTaskArg();
	arg.learnerPtr           = learnerPtr;
	arg.queries              = queries;
	arg.queryVectorPtr       = queryVectorPtr;
	arg.parentQueryVectorPtr = parentQueryVectorPtr;
	arg.bitmapPtr            = visitedBitmapPtr;
	arg.workQueuePtr         = workQueuePtr;
	arg.aQueryVectorPtr      = aQueryVectorPtr;
	arg.bQueryVectorPtr      = bQueryVectorPtr;

	while (true) {

	    LearnerTask taskPtr;
	    try {
		CallableCollectAborts<LearnerTask> cmdL = new CallableCollectAborts<LearnerTask>() {
		    public LearnerTask runTx() {
			return learnerPtr.TMpopTask(learnerPtr.taskListPtr);
		    }
		};
		taskPtr = Transaction.doIt(cmdL);
		if (cmdL.getAborts() > 0) {
		    aborts.addAndGet(cmdL.getAborts());
		}
	    } catch (Exception e) {
		e.printStackTrace();
		System.exit(-1);
		taskPtr = null;
	    }

	    if (taskPtr == null) {
		break;
	    }

	    final int op = taskPtr.op;
	    final int fromId = taskPtr.fromId;
	    final int toId = taskPtr.toId;

	    boolean isTaskValid = atomicCheckIfValid(learnerPtr, visitedBitmapPtr, workQueuePtr, op, fromId, toId);

	    float deltaLogLikelihood = 0.0f;

	    if (isTaskValid) {
		if(op == OPERATION_INSERT) {
		    try {
			CallableCollectAborts<Float> cmdF = new CallableCollectAborts<Float>() {
			    public Float runTx() {
				learnerPtr.TMpopulateQueryVectors(learnerPtr.netPtr, toId, queries, queryVectorPtr, parentQueryVectorPtr);
				float newBaseLogLikelihood = learnerPtr.computeLocalLogLikelihood(toId, learnerPtr.adtreePtr, learnerPtr.netPtr, queries, queryVectorPtr, parentQueryVectorPtr);
				float toLocalBaseLogLikelihood = learnerPtr.localBaseLogLikelihoods[toId].getFloat();
				learnerPtr.localBaseLogLikelihoods[toId].put(newBaseLogLikelihood);
				return toLocalBaseLogLikelihood - newBaseLogLikelihood;
			    }
			};
			deltaLogLikelihood += Transaction.doIt(cmdF);
			if (cmdF.getAborts() > 0) {
			    aborts.addAndGet(cmdF.getAborts());
			}
		    } catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		    }

		    CommandCollectAborts cmd = new CommandCollectAborts() {
			public void runTx() {
			    int numTotalParent = learnerPtr.numTotalParent.getInt();
			    learnerPtr.numTotalParent.put(numTotalParent + 1);
			}
		    };
		    Transaction.transactionallyDo(cmd);
		    if (cmd.getAborts() > 0) {
			aborts.addAndGet(cmd.getAborts());
		    }

		} else if(op == OPERATION_REMOVE) {
		    try {
			CallableCollectAborts<Float> cmdF = new CallableCollectAborts<Float>() {
			    public Float runTx() {
				learnerPtr.TMpopulateQueryVectors(learnerPtr.netPtr,fromId,queries,queryVectorPtr,parentQueryVectorPtr);
				float newBaseLogLikelihood =learnerPtr. computeLocalLogLikelihood(fromId,learnerPtr.adtreePtr,learnerPtr.netPtr,queries,queryVectorPtr,parentQueryVectorPtr);
				float fromLocalBaseLogLikelihood =learnerPtr.localBaseLogLikelihoods[fromId].getFloat();
				learnerPtr.localBaseLogLikelihoods[fromId].put(newBaseLogLikelihood);
				return fromLocalBaseLogLikelihood - newBaseLogLikelihood;
			    }
			};
			deltaLogLikelihood += Transaction.doIt(cmdF);
			if (cmdF.getAborts() > 0) {
			    aborts.addAndGet(cmdF.getAborts());
			}
		    } catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		    }

		    CommandCollectAborts cmd = new CommandCollectAborts() {
			public void runTx() {
			    int numTotalParent = learnerPtr.numTotalParent.getInt();
			    learnerPtr.numTotalParent.put(numTotalParent - 1);
			}
		    };
		    Transaction.transactionallyDo(cmd);
		    if (cmd.getAborts() > 0) {
			aborts.addAndGet(cmd.getAborts());
		    }

		} else if(op == OPERATION_REVERSE) {
		    try {
			CallableCollectAborts<Float> cmdF = new CallableCollectAborts<Float>() {
			    public Float runTx() {
				learnerPtr.TMpopulateQueryVectors(learnerPtr.netPtr,fromId,queries,queryVectorPtr,parentQueryVectorPtr);
				float newBaseLogLikelihood =learnerPtr.computeLocalLogLikelihood(fromId,learnerPtr.adtreePtr,learnerPtr.netPtr,queries,queryVectorPtr,parentQueryVectorPtr);
				float fromLocalBaseLogLikelihood =learnerPtr.localBaseLogLikelihoods[fromId].getFloat();
				learnerPtr.localBaseLogLikelihoods[fromId].put(newBaseLogLikelihood);
				return fromLocalBaseLogLikelihood - newBaseLogLikelihood;
			    }
			};
			deltaLogLikelihood += Transaction.doIt(cmdF);
			if (cmdF.getAborts() > 0) {
			    aborts.addAndGet(cmdF.getAborts());
			}
		    } catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		    }

		    try {
			CallableCollectAborts<Float> cmdF = new CallableCollectAborts<Float>() {
			    public Float runTx() {
				learnerPtr.TMpopulateQueryVectors(learnerPtr.netPtr,toId,queries,queryVectorPtr,parentQueryVectorPtr);
				float newBaseLogLikelihood =learnerPtr.computeLocalLogLikelihood(toId,learnerPtr.adtreePtr,learnerPtr.netPtr,queries,queryVectorPtr,parentQueryVectorPtr);
				float toLocalBaseLogLikelihood =learnerPtr.localBaseLogLikelihoods[toId].getFloat();
				learnerPtr.localBaseLogLikelihoods[toId].put(newBaseLogLikelihood);
				return toLocalBaseLogLikelihood - newBaseLogLikelihood;
			    }
			};
			deltaLogLikelihood += Transaction.doIt(cmdF);
			if (cmdF.getAborts() > 0) {
			    aborts.addAndGet(cmdF.getAborts());
			}
		    } catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		    }

		}

	    } //if isTaskValid

	    /*
	     * Update/read globals
	     */

	    final float deltaLogLikelihoodFinal = deltaLogLikelihood;
	    LearnerTask bestTask;
	    try {
		CallableCollectAborts<LearnerTask> cmdL = new CallableCollectAborts<LearnerTask>() {
		    public LearnerTask runTx() {
			float oldBaseLogLikelihood = learnerPtr.baseLogLikelihood.getFloat();
			float newBaseLogLikelihood = oldBaseLogLikelihood + deltaLogLikelihoodFinal;
			learnerPtr.baseLogLikelihood.put(newBaseLogLikelihood);
			float baseLogLikelihood = newBaseLogLikelihood;
			int numTotalParent = learnerPtr.numTotalParent.getInt();

			float baseScore = ((float)numTotalParent * basePenalty)
				+ (numRecord * baseLogLikelihood);
			LearnerTask bestTask = new LearnerTask();
			bestTask.score  = baseScore;
			arg.numTotalParent    = numTotalParent;
			arg.baseLogLikelihood = baseLogLikelihood;
			return bestTask;
		    }
		};
		bestTask = Transaction.doIt(cmdL);
		if (cmdL.getAborts() > 0) {
		    aborts.addAndGet(cmdL.getAborts());
		}
	    } catch (Exception e) {
		e.printStackTrace();
		System.exit(-1);
		bestTask = null;
	    }

	    bestTask.op     = NUM_OPERATION;
	    bestTask.toId   = -1;
	    bestTask.fromId = -1;

	    LearnerTask newTask = new LearnerTask();

	    arg.toId              = toId;
	    arg.basePenalty       = basePenalty;

	    try {
		CallableCollectAborts<LearnerTask> cmdL = new CallableCollectAborts<LearnerTask>() {
		    public LearnerTask runTx() {
			return learnerPtr.TMfindBestInsertTask(arg);
		    }
		};
		newTask = Transaction.doIt(cmdL);
		if (cmdL.getAborts() > 0) {
		    aborts.addAndGet(cmdL.getAborts());
		}
	    } catch (Exception e) {
		e.printStackTrace();
		System.exit(-1);
	    }

	    if ((newTask.fromId != newTask.toId) &&
		    (newTask.score > (bestTask.score / operationQualityFactor)))
	    {
		bestTask = newTask;
	    }

	    try {
		CallableCollectAborts<LearnerTask> cmdL = new CallableCollectAborts<LearnerTask>() {
		    public LearnerTask runTx() {
			return learnerPtr.TMfindBestRemoveTask(arg);
		    }
		};
		newTask = Transaction.doIt(cmdL);
		if (cmdL.getAborts() > 0) {
		    aborts.addAndGet(cmdL.getAborts());
		}
	    } catch (Exception e) {
		e.printStackTrace();
		System.exit(-1);
	    }

	    if ((newTask.fromId != newTask.toId) &&
		    (newTask.score > (bestTask.score / operationQualityFactor)))
	    {
		bestTask = newTask;
	    }

	    try {
		CallableCollectAborts<LearnerTask> cmdL = new CallableCollectAborts<LearnerTask>() {
		    public LearnerTask runTx() {
			return learnerPtr.TMfindBestReverseTask(arg);
		    }
		};
		newTask = Transaction.doIt(cmdL);
		if (cmdL.getAborts() > 0) {
		    aborts.addAndGet(cmdL.getAborts());
		}
	    } catch (Exception e) {
		e.printStackTrace();
		System.exit(-1);
	    }

	    if ((newTask.fromId != newTask.toId) &&
		    (newTask.score > (bestTask.score / operationQualityFactor)))
	    {
		bestTask = newTask;
	    }

	    if (bestTask.toId != -1) {
		final LearnerTask[] tasks = learnerPtr.tasks;
		tasks[toId] = bestTask;

		CommandCollectAborts cmd = new CommandCollectAborts() {
		    public void runTx() {
			learnerPtr.taskListPtr.list_insert(tasks[toId]);
		    }
		};
		Transaction.transactionallyDo(cmd);
		if (cmd.getAborts() > 0) {
		    aborts.addAndGet(cmd.getAborts());
		}
	    }

	} // while (tasks) 

	visitedBitmapPtr.bitmap_free();
	workQueuePtr.queue_free();
	bQueryVectorPtr.clear();
	aQueryVectorPtr.clear();
	queryVectorPtr.clear();
	parentQueryVectorPtr.clear();
    }

    private static boolean atomicCheckIfValid(final Learner learnerPtr, final BitMap visitedBitmapPtr, final Queue workQueuePtr, final int op,
	    final int fromId, final int toId) {
	try {
	    CallableCollectAborts<Boolean> cmdL = new CallableCollectAborts<Boolean>() {
		public Boolean runTx() {
		    boolean isTaskValid = true;
		    if(op == OPERATION_INSERT) {
			if(learnerPtr.netPtr.net_hasEdge(fromId, toId) || learnerPtr.netPtr.net_isPath(toId, fromId, visitedBitmapPtr, workQueuePtr)) {
			    isTaskValid = false;
			}
		    } else if (op == OPERATION_REVERSE) {
			// Temporarily remove edge for check
			learnerPtr.netPtr.net_applyOperation(OPERATION_REMOVE, fromId, toId);
			if(learnerPtr.netPtr.net_isPath(fromId, toId, visitedBitmapPtr, workQueuePtr)) {
			    isTaskValid = false;
			}
			learnerPtr.netPtr.net_applyOperation(OPERATION_INSERT, fromId, toId);
		    }

		    if (isTaskValid) {
			learnerPtr.netPtr.net_applyOperation(op, fromId, toId);
		    }
		    return isTaskValid;
		}
	    };
	    boolean b =Transaction.doIt(cmdL);
	    if (cmdL.getAborts() > 0) {
		aborts.addAndGet(cmdL.getAborts());
	    }
	    return b;
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(-1);
	    return false;
	}
    }


    /* =============================================================================
     * learner_run
     * -- Call adtree_make before this
     * =============================================================================
     */
    //Is not called anywhere now parallel code
    public void
    learner_run (int myId, int numThread, Learner learnerPtr)
    {
	{
	    createTaskList(myId, numThread, learnerPtr);
	}
	{
	    learnStructure(myId, numThread, learnerPtr);
	}
    }

    /* =============================================================================
     * learner_score
     * -- Score entire network
     * =============================================================================
     */
    public float
    learner_score ()
    {

	Vector_t queryVectorPtr = new Vector_t(1);
	Vector_t parentQueryVectorPtr = new Vector_t(1);

	int numVar = adtreePtr.numVar;
	Query[] queries = new Query[numVar];

	for (int v = 0; v < numVar; v++) {
	    queries[v] = new Query();
	    queries[v].index = v;
	    queries[v].value = QUERY_VALUE_WILDCARD;
	}

	int numTotalParent = 0;
	float logLikelihood = 0.0f;

	for (int v = 0; v < numVar; v++) {

	    IntList parentIdListPtr = netPtr.net_getParentIdListPtr(v);
	    numTotalParent += parentIdListPtr.list_getSize();

	    populateQueryVectors(netPtr,
		    v,
		    queries,
		    queryVectorPtr,
		    parentQueryVectorPtr);
	    float localLogLikelihood = computeLocalLogLikelihood(v,
		    adtreePtr,
		    netPtr,
		    queries,
		    queryVectorPtr,
		    parentQueryVectorPtr);
	    logLikelihood += localLogLikelihood;
	}

	queryVectorPtr.clear();
	parentQueryVectorPtr.clear();
	queries = null;


	int numRecord = adtreePtr.numRecord;
	float penalty = (float)(-0.5f * (double)numTotalParent * Math.log((double)numRecord));
	float score = penalty + (float)numRecord * logLikelihood;

	return score;
    }
}

/* =============================================================================
 *
 * End of learner.java
 *
 * =============================================================================
 */
