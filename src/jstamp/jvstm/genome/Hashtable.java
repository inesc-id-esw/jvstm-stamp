package jstamp.jvstm.genome;

public class Hashtable {
    final List buckets[];
    final int numBucket;

    public Hashtable (int initNumBucket, int resizeRatio, int growthFactor) {
	int i;
	/* Allocate bucket: extra bucket is dummy for easier iterator code */
	buckets = new List[initNumBucket+1];

	for (i = 0; i < (initNumBucket + 1); i++) {
	    List chainPtr = new List();
	    buckets[i] = chainPtr;
	}
	numBucket = initNumBucket;
	resizeRatio = ((resizeRatio < 0) ? 3 : resizeRatio);
	growthFactor = ((growthFactor < 0) ? 3 : growthFactor);
    }

    public boolean TMhashtable_insert (ByteString keyPtr, ByteString dataPtr) {
	int i = keyPtr.hashCode() % numBucket;

	Pair findPair = new Pair(keyPtr, null);
	Pair pairPtr = buckets[i].find(findPair);
	if (pairPtr != null) {
	    return false;
	}

	Pair insertPtr = new Pair(keyPtr, dataPtr);

	/* Add new entry  */
	if (buckets[i].insert(insertPtr) == false) {
	    return false;
	}

	return true;
    }
    
    public int size() {
	int sum = 0;
	for (List list : buckets) {
	    sum += list.size();
	}
	return sum;
    }
}
