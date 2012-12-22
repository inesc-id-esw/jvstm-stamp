package jstamp.jvstm.genome;

public class List {
    final ListNode head;

    public List () {
	head = new ListNode(null, null);
    }

    Pair find (Pair dataPtr) {
	ListNode nodePtr;
	ListNode prevPtr = findPrevious(dataPtr);

	nodePtr = prevPtr.nextPtr.get();

	if ((nodePtr == null) || nodePtr.dataPtr.firstPtr.compareTo(dataPtr.firstPtr) != 0) {
	    return null;
	}

	return nodePtr.dataPtr;
    }

    ListNode findPrevious (Pair dataPtr) {
	ListNode prevPtr = head;
	ListNode nodePtr;
	nodePtr = prevPtr.nextPtr.get();

	for (; nodePtr != null; nodePtr = nodePtr.nextPtr.get()) {
	    if (nodePtr.dataPtr.firstPtr.compareTo(dataPtr.firstPtr) >= 0) {
		return prevPtr;
	    }
	    prevPtr = nodePtr;
	}

	return prevPtr;
    }

    boolean insert (Pair dataPtr) {
	ListNode prevPtr;
	ListNode nodePtr;
	ListNode currPtr;

	prevPtr = findPrevious(dataPtr);
	currPtr = prevPtr.nextPtr.get();

	if ((currPtr != null) && (currPtr.dataPtr.firstPtr.compareTo(dataPtr.firstPtr)==0)) {
	    return false;
	}

	nodePtr = new ListNode(dataPtr, null);

	nodePtr.nextPtr.put(currPtr);
	prevPtr.nextPtr.put(nodePtr);

	size++;
	
	return true;
    }
    
    int size = 0;
    
    public int size() {
	ListNode ptr = head;
	int sum = 0;
	if (ptr != null && ptr.dataPtr != null) {
	    sum  = 1;
	}
	while (ptr != null) {
	    if (ptr.dataPtr != null) {
		sum++;
	    }
	    ptr = ptr.nextPtr.get();
	}
	return sum;
    }
}
