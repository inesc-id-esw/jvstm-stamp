package jstamp.jvstm.yada;


public class Hashtable {
    private final List_hash buckets[];
    private final int numBucket;

    public Hashtable (int initNumBucket) {
	numBucket = initNumBucket;
	int i;
	/* Allocate bucket: extra bucket is dummy for easier iterator code */
	buckets = new List_hash[numBucket+1];
	for (i = 0; i < (numBucket + 1); i++) {
	    buckets[i] = new List_hash();
	}
    }

    public boolean insert (Object key, Object dataPtr) {

	int index;
	if(key instanceof Edge) {
	    index = (int) (((Edge)key).hashForTable() % numBucket);
	} else if(key instanceof Element) {
	    index = (int) (((Element)key).hashForTable() % numBucket);
	} else {
	    index = key.hashCode() % numBucket;
	}

	if(!contains(key)) {
	    Pair p = new Pair(key,dataPtr);
	    if(buckets[index].insert(p)) {
		return true;
	    }
	}
	return false;
    }

    public boolean remove(Object key) {
	int index;
	if(key instanceof Edge) {
	    index = (int) (((Edge)key).hashForTable() % numBucket);
	} else if(key instanceof Element) {
	    index = (int) (((Element)key).hashForTable() % numBucket);
	} else {
	    index = key.hashCode() % numBucket;
	}
	return buckets[index].remove(key);
    }

    boolean contains(Object key) {
	int index;
	if(key instanceof Edge) {
	    index = (int) (((Edge)key).hashForTable() % numBucket);
	} else if(key instanceof Element) {
	    index = (int) (((Element)key).hashForTable() % numBucket);
	} else {
	    index = key.hashCode() % numBucket;
	}

	return buckets[index].find(key)!=null;
    }

    public Object get(Object key) {
	int index;
	if(key instanceof Edge) {
	    index = (int) (((Edge)key).hashForTable() % numBucket);
	} else if(key instanceof Element) {
	    index = (int) (((Element)key).hashForTable() % numBucket);
	} else {
	    index = key.hashCode() % numBucket;
	}

	return buckets[index].find(key).getSecond();
    }

    public void clear() {
	for(int i=0;i<buckets.length;i++)
	    buckets[i].clear();
    }
}
