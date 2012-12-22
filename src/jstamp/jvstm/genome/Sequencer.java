package jstamp.jvstm.genome;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import jstamp.jvstm.CallableCollectAborts;
import jstamp.jvstm.CommandCollectAborts;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.util.VLinkedSet;

public class Sequencer {
    public final VBox<ByteString> sequence;

    public final Segments segmentsPtr;

    /* For removing duplicate segments */
    final Hashtable uniqueSegmentsPtr;

    /* For matching segments */
    final endInfoEntry endInfoEntries[];
    final Table startHashToConstructEntryTables[];

    /* For constructing sequence */
    final constructEntry constructEntries[];
    final Table hashToConstructEntryTable;

    /* For deallocation */
    final int segmentLength;


    public Sequencer (int myGeneLength, int mySegmentLength, Segments mySegmentsPtr) { 

	this.sequence = new VBox<ByteString>(null);

	int maxNumUniqueSegment = myGeneLength - mySegmentLength + 1;
	int i;

	uniqueSegmentsPtr = new Hashtable((int)myGeneLength, -1, -1);

	/* For finding a matching entry */
	endInfoEntries = new endInfoEntry[maxNumUniqueSegment];
	for (i = 0; i < maxNumUniqueSegment; i++) {
	    endInfoEntries[i] = new endInfoEntry(true, 1);
	}

	startHashToConstructEntryTables = new Table[mySegmentLength];
	for (i = 1; i < mySegmentLength; i++) { /* 0 is dummy entry */
	    startHashToConstructEntryTables[i] = new Table(myGeneLength);
	}
	segmentLength = mySegmentLength;

	/* For constructing sequence */
	constructEntries = new constructEntry[maxNumUniqueSegment];

	for (i= 0; i < maxNumUniqueSegment; i++) {
	    constructEntries[i] = new constructEntry(null, true, 0, null, null, null, 0, segmentLength);
	}
	hashToConstructEntryTable = new Table(myGeneLength);

	segmentsPtr = mySegmentsPtr;  
    }

    /* =============================================================================
     * sequencer_run
     * =============================================================================
     */

    public static void run (int threadNum, int numOfThreads, Random randomPtr, Sequencer sequencerPtr) {

	int threadId = threadNum;

	Segments segmentsPtr = sequencerPtr.segmentsPtr;

	Hashtable         uniqueSegmentsPtr = sequencerPtr.uniqueSegmentsPtr;
	endInfoEntry    endInfoEntries[] = sequencerPtr.endInfoEntries;
	Table         startHashToConstructEntryTables[] = sequencerPtr.startHashToConstructEntryTables;
	constructEntry  constructEntries[] = sequencerPtr.constructEntries;
	Table         hashToConstructEntryTable = sequencerPtr.hashToConstructEntryTable;

	ArrayList      segmentsContentsPtr = segmentsPtr.contentsPtr;
	int        numSegment          = segmentsContentsPtr.size();
	int        segmentLength       = segmentsPtr.length;

	int i;
	int j;
	int i_start;
	int i_stop;
	int numUniqueSegment;
	int substringLength;
	int entryIndex;

	int CHUNK_STEP1 = 12;

	/*
	 * Step 1: Remove duplicate segments
	 */
	int numThread = numOfThreads;
	{
	    /* Choose disjoint segments [i_start,i_stop) for each thread */
	    int partitionSize = (numSegment + numThread/2) / numThread; /* with rounding */
	    i_start = threadId * partitionSize;
	    if (threadId == (numThread - 1)) {
		i_stop = numSegment;
	    } else {
		i_stop = i_start + partitionSize;
	    }
	}

	for (i = i_start; i < i_stop; i+=CHUNK_STEP1) {
	    atomicMethodOne(uniqueSegmentsPtr, segmentsContentsPtr, i, i_stop,
		    CHUNK_STEP1);
	}

	Barrier.enterBarrier1();

	/*
	 * Step 2a: Iterate over unique segments and compute hashes.
	 *
	 * For the gene "atcg", the hashes for the end would be:
	 *
	 *     "t", "tc", and "tcg"
	 *
	 * And for the gene "tcgg", the hashes for the start would be:
	 *
	 *    "t", "tc", and "tcg"
	 *
	 * The names are "end" and "start" because if a matching pair is found,
	 * they are the substring of the end part of the pair and the start
	 * part of the pair respectively. In the above example, "tcg" is the
	 * matching substring so:
	 *
	 *     (end)    (start)
	 *     a[tcg] + [tcg]g  = a[tcg]g    (overlap = "tcg")
	 */

	/* uniqueSegmentsPtr is constant now */
	numUniqueSegment = uniqueSegmentsPtr.size();
	entryIndex = 0;

	{
	    /* Choose disjoint segments [i_start,i_stop) for each thread */
	    int num = uniqueSegmentsPtr.numBucket;
	    int partitionSize = (num + numThread/2) / numThread; /* with rounding */
	    i_start = threadId * partitionSize;
	    if (threadId == (numThread - 1)) {
		i_stop = num;
	    } else {
		i_stop = i_start + partitionSize;
	    }
	}

	{
	    /* Approximate disjoint segments of element allocation in constructEntries */
	    int partitionSize = (numUniqueSegment + numThread/2) / numThread; /* with rounding */
	    entryIndex = threadId * partitionSize;
	}

	for (i = i_start; i < i_stop; i++) {
	    List chainPtr = uniqueSegmentsPtr.buckets[i];
	    ListNode it = chainPtr.head;

	    while(it.nextPtr.get() != null) {
		it = it.nextPtr.get();    
		ByteString segment = it.dataPtr.firstPtr;
		int newj;
		int startHash;
		boolean status;

		/* Find an empty constructEntries entry */
		entryIndex = atomicMethodTwo(constructEntries, numUniqueSegment,
			entryIndex, segment);

		constructEntry constructEntryPtr = constructEntries[entryIndex];

		entryIndex = (entryIndex + 1) % numUniqueSegment;



		/*
		 * Save hashes (sdbm algorithm) of segment substrings
		 *
		 * endHashes will be computed for shorter substrings after matches
		 * have been made (in the next phase of the code). This will reduce
		 * the number of substrings for which hashes need to be computed.
		 *
		 * Since we can compute startHashes incrementally, we go ahead
		 * and compute all of them here.
		 */
		/* constructEntryPtr is local now */
		constructEntryPtr.endHash.put(segment.substring(1).hashCode()); // USE BYTE SUBSTRING FUNCTION

		startHash = 0;
		for (newj = 1; newj < segmentLength; newj++) {
		    startHash = segment.byteAt(newj-1) + (startHash << 6) + (startHash << 16) - startHash;
		    atomicMethodThree(startHashToConstructEntryTables, newj, startHash,
			    constructEntryPtr);
		}


		/*
		 * For looking up construct entries quickly
		 */
		startHash = segment.byteAt(newj-1) + (startHash << 6) + (startHash << 16) - startHash;
		atomicMethodFour(hashToConstructEntryTable, startHash,
			constructEntryPtr);
	    }
	}

	Barrier.enterBarrier2();

	/*
	 * Step 2b: Match ends to starts by using hash-based string comparison.
	 */
	for (substringLength = segmentLength-1; substringLength > 0; substringLength--) {

	    Table startHashToConstructEntryTablePtr = startHashToConstructEntryTables[substringLength];
	    VLinkedSet buckets[] = startHashToConstructEntryTablePtr.buckets;
	    int numBucket = startHashToConstructEntryTablePtr.numBucket;

	    int index_start;
	    int index_stop;


	    /* Choose disjoint segments [index_start,index_stop) for each thread */
	    int partitionSize = (numUniqueSegment + numThread/2) / numThread; /* with rounding */
	    index_start = threadId * partitionSize;
	    if (threadId == (numThread - 1)) {
		index_stop = numUniqueSegment;
	    } else {
		index_stop = index_start + partitionSize;
	    }


	    /* Iterating over disjoint itervals in the range [0, numUniqueSegment) */
	    for (entryIndex = index_start;
		    entryIndex < index_stop;
		    entryIndex += endInfoEntries[entryIndex].jumpToNext.getInt())
	    {
		if (!endInfoEntries[entryIndex].isEnd.get()) {
		    continue;
		}

		/*  ConstructEntries[entryIndex] is local data */
		constructEntry endConstructEntryPtr = constructEntries[entryIndex];
		ByteString endSegment = endConstructEntryPtr.segment.get();
		int endHash = endConstructEntryPtr.endHash.getInt();

		VLinkedSet chainPtr = buckets[(endHash % numBucket)]; /* buckets: constant data */
		Iterator it = chainPtr.iterator();
		while (it.hasNext()) {
		    constructEntry startConstructEntryPtr = (constructEntry)it.next();
		    ByteString startSegment = startConstructEntryPtr.segment.get();

		    /* endConstructEntryPtr is local except for properties startPtr/endPtr/length */
		    atomicMethodFive2(endInfoEntries, segmentLength, substringLength,
			    entryIndex, endConstructEntryPtr, endSegment,
			    startConstructEntryPtr, startSegment);

		    // atomicMethodFive2(segmentLength);

		    if (!endInfoEntries[entryIndex].isEnd.get()) { /* if there was a match */
			break;
		    }
		} /* iterate over chain */

	    } /* for (endIndex < numUniqueSegment) */

	    //	    Barrier.enterBarrier3();

	    /*
	     * Step 2c: Update jump values and hashes
	     *
	     * endHash entries of all remaining ends are updated to the next
	     * substringLength. Additionally jumpToNext entries are updated such
	     * that they allow to skip non-end entries. Currently this is sequential
	     * because parallelization did not perform better.
	     */

	    if (threadId == 0) {
		if (substringLength > 1) {
		    int index = segmentLength - substringLength + 1;
		    /* initialization if j and i: with i being the next end after j=0 */
		    for (i = 1; !endInfoEntries[i].isEnd.get(); i+=endInfoEntries[i].jumpToNext.getInt()) {
			/* find first non-null */
			;
		    }
		    /* entry 0 is handled seperately from the loop below */
		    endInfoEntries[0].jumpToNext.put(i);
		    if (endInfoEntries[0].isEnd.get()) {
			ByteString segment = constructEntries[0].segment.get();
			constructEntries[0].endHash.put(segment.subString(index).hashCode()); // USE BYTE SUBSTRING FUNCTION
		    }
		    /* Continue scanning (do not reset i) */
		    for (j = 0; i < numUniqueSegment; i+=endInfoEntries[i].jumpToNext.getInt()) {

			if (endInfoEntries[i].isEnd.get()) {
			    ByteString segment = constructEntries[i].segment.get();
			    constructEntries[i].endHash.put(segment.substring(index).hashCode()); // USE BYTE SUBSTRING FUNCTION
			    endInfoEntries[j].jumpToNext.put(Math.max(1, i - j));
			    j = i;
			}
		    }
		    endInfoEntries[j].jumpToNext.put(i - j);
		}
	    }


	    //	    Barrier.enterBarrier();

	} /* for (substringLength > 0) */

	Barrier.enterBarrier3();

	/*
	 * Step 3: Build sequence string
	 */
	if (threadId == 0) {
	    int totalLength = 0;
	    for (i = 0; i < numUniqueSegment; i++) {
		if (constructEntries[i].isStart.getBoolean()) {
		    totalLength += constructEntries[i].length.getInt();
		}
	    }

	    ByteString sequence = sequencerPtr.sequence.get();

	    ByteString copyPtr = sequence;
	    int sequenceLength = 0;

	    for (i = 0; i < numUniqueSegment; i++) {
		/* If there are several start segments, we append in arbitrary order  */
		constructEntry constructEntryPtr = constructEntries[i];
		if (constructEntryPtr.isStart.getBoolean()) {
		    int newSequenceLength = sequenceLength + constructEntryPtr.length.getInt();
		    int prevOverlap = 0;
		    do {
			int numChar = segmentLength - constructEntryPtr.overlap.getInt();
			copyPtr = constructEntryPtr.segment.get();
			if(sequencerPtr.sequence.get() == null) {
			    sequencerPtr.sequence.put(copyPtr);
			} else {
			    sequencerPtr.sequence.put(sequencerPtr.sequence.get().concat(copyPtr.substring(prevOverlap)));
			}
			prevOverlap = constructEntryPtr.overlap.getInt();
			constructEntryPtr = constructEntryPtr.nextPtr.get();
		    } while (constructEntryPtr != null);
		}
	    }
	}
    }

    //    @Atomic
    //    private static void atomicMethodFive(/*endInfoEntry[] endInfoEntries,*/
    //	    int segmentLength) {
    //	////trans2(null, null, null, null, segmentLength, 0, null, 0);
    //    }


    // @Atomic
    private static void atomicMethodFive2(final endInfoEntry[] endInfoEntries,
	    final int segmentLength, final int substringLength, final int entryIndex,
	    final constructEntry endConstructEntryPtr, final ByteString endSegment,
	    final constructEntry startConstructEntryPtr, final ByteString startSegment) {
	CommandCollectAborts cmd = new CommandCollectAborts() {
	    @Override
	    public void runTx() {
		trans2(startSegment, endSegment, startConstructEntryPtr, endConstructEntryPtr, segmentLength, substringLength, endInfoEntries, entryIndex);
		if(startConstructEntryPtr.isStart.getBoolean() &&
			(endConstructEntryPtr.startPtr.get() != startConstructEntryPtr) &&
			(startSegment.substring(0, (int)substringLength).compareTo(endSegment.substring((int)(segmentLength-substringLength))) == 0))
		{
		    startConstructEntryPtr.isStart.put(false);

		    /* Update endInfo (appended something so no inter end) */
		    endInfoEntries[entryIndex].isEnd.put(false);
		    /* Update segment chain construct info */
		    constructEntry startConstructEntry_endPtr = startConstructEntryPtr.endPtr.get();
		    constructEntry endConstructEntry_startPtr = endConstructEntryPtr.startPtr.get();
		    startConstructEntry_endPtr.startPtr.put(endConstructEntry_startPtr);
		    endConstructEntryPtr.nextPtr.put(startConstructEntryPtr);
		    endConstructEntry_startPtr.endPtr.put(startConstructEntry_endPtr);
		    endConstructEntryPtr.overlap.put(substringLength);
		    int newLength = endConstructEntry_startPtr.length.getInt() + startConstructEntryPtr.length.getInt() - substringLength;
		    endConstructEntry_startPtr.length.put(newLength);
		}
	    }
	};
	Transaction.transactionallyDo(cmd);
	if (cmd.getAborts() > 0) {
	    aborts5.addAndGet(cmd.getAborts());
	}
    }

    // @Atomic
    private static void atomicMethodFour(final Table hashToConstructEntryTable,
	    final int startHash, final constructEntry constructEntryPtr) {
	CommandCollectAborts cmd = new CommandCollectAborts() {
	    @Override
	    public void runTx() {
		hashToConstructEntryTable.table_insert(startHash, constructEntryPtr);
	    }
	};
	Transaction.transactionallyDo(cmd);
	if (cmd.getAborts() > 0) {
	    aborts4.addAndGet(cmd.getAborts());
	}
    }

    // @Atomic
    private static void atomicMethodThree(final Table[] startHashToConstructEntryTables,
	    final int newj, final int startHash, final constructEntry constructEntryPtr) {
	CommandCollectAborts cmd = new CommandCollectAborts() {
	    @Override
	    public void runTx() {
		boolean check = startHashToConstructEntryTables[newj].table_insert(startHash, constructEntryPtr);
	    }
	};
	Transaction.transactionallyDo(cmd);
	if (cmd.getAborts() > 0) {
	    aborts3.addAndGet(cmd.getAborts());
	}
    }

    // @Atomic
    private static int atomicMethodTwo(final constructEntry[] constructEntries,
	    final int numUniqueSegment, final int entryIndex, final ByteString segment) {
	try {
	    CallableCollectAborts<Integer> cmd = new CallableCollectAborts<Integer>() {
		@Override
		public Integer runTx() {
		    int eI = entryIndex;
		    while(constructEntries[eI].segment.get() != null) { 
			eI = (eI + 1) % numUniqueSegment; /* look for empty */
		    }
		    constructEntries[eI].segment.put(segment);
		    return eI;
		}
	    };
	    int t = Transaction.doIt(cmd);
	    if (cmd.getAborts() > 0) {
		aborts2.addAndGet(cmd.getAborts());
	    }
	    return t;
	} catch (Exception e) {
	    e.printStackTrace();
	    return -1;
	}
    }

    public static final AtomicInteger aborts1 = new AtomicInteger(0);
    public static final AtomicInteger aborts2 = new AtomicInteger(0);
    public static final AtomicInteger aborts3 = new AtomicInteger(0);
    public static final AtomicInteger aborts4 = new AtomicInteger(0);
    public static final AtomicInteger aborts5 = new AtomicInteger(0);

    // @Atomic
    private static void atomicMethodOne(final Hashtable uniqueSegmentsPtr,
	    final ArrayList segmentsContentsPtr, final int i, final int i_stop, final int CHUNK_STEP1) {
	{
	    CommandCollectAborts cmd = new CommandCollectAborts() {
		@Override
		public void runTx() {
		    int ii;
		    int ii_stop = Math.min(i_stop, (i+CHUNK_STEP1));
		    for (ii = i; ii < ii_stop; ii++) {
			ByteString segment = (ByteString)segmentsContentsPtr.get(ii);
			if(!uniqueSegmentsPtr.TMhashtable_insert(segment, segment)) {
			    ;
			}
		    } /* ii */
		}
	    };
	    Transaction.transactionallyDo(cmd);
	    if (cmd.getAborts() > 0) {
		aborts1.addAndGet(cmd.getAborts());
	    }
	}
    }


    static void trans2(ByteString startSegment, ByteString endSegment, constructEntry startConstructEntryPtr, constructEntry endConstructEntryPtr, int segmentLength, int substringLength, endInfoEntry endInfoEntries[], int entryIndex) {
	if(startConstructEntryPtr.isStart.getBoolean() &&
		(endConstructEntryPtr.startPtr.get() != startConstructEntryPtr) &&
		(startSegment.substring(0, substringLength).compareTo(endSegment.substring(segmentLength-substringLength)) == 0)) {
	    startConstructEntryPtr.isStart.put(false);

	    /* Update endInfo (appended something so no inter end) */
	    endInfoEntries[entryIndex].isEnd.put(false);
	    /* Update segment chain construct info */
	    constructEntry startConstructEntry_endPtr = startConstructEntryPtr.endPtr.get();
	    constructEntry endConstructEntry_startPtr = endConstructEntryPtr.startPtr.get();
	    startConstructEntry_endPtr.startPtr.put(endConstructEntry_startPtr);
	    endConstructEntryPtr.nextPtr.put(startConstructEntryPtr);
	    endConstructEntry_startPtr.endPtr.put(startConstructEntry_endPtr);
	    endConstructEntryPtr.overlap.put(substringLength);
	    int newLength = endConstructEntry_startPtr.length.getInt() + startConstructEntryPtr.length.getInt() - substringLength;
	    endConstructEntry_startPtr.length.put(newLength);
	}
    }
}
