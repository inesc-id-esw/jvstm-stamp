package jstamp.jvstm.yada;


public class List {
    private final ListNode head;

    public List () {
	head = new ListNode();
    }

    public ListNode getHead() {
	return head;
    }

    Element find (Element dataPtr) {
	ListNode nodePtr;
	ListNode prevPtr = findPrevious(dataPtr);

	nodePtr = prevPtr.getNextPtr();

	if ((nodePtr == null) || !nodePtr.getDataPtr().equals(dataPtr) ) {
	    return null;
	}

	return nodePtr.getDataPtr();
    }

    ListNode findPrevious (Element dataPtr) {
	ListNode prevPtr = getHead();
	ListNode nodePtr;
	nodePtr = prevPtr.getNextPtr();

	for (; nodePtr != null; nodePtr = nodePtr.getNextPtr()) {
	    if (nodePtr.getDataPtr().compare(dataPtr) >= 0) {
		return prevPtr;
	    }
	    prevPtr = nodePtr;
	}

	return prevPtr;
    }



    boolean insert (Element dataPtr) {
	ListNode prevPtr;
	ListNode nodePtr;
	ListNode currPtr;

	if(dataPtr==null)
	    throw new NullPointerException();

	prevPtr = findPrevious(dataPtr);
	currPtr = prevPtr.getNextPtr();

	if((currPtr != null) && currPtr.getDataPtr().equals(dataPtr)) {
	    return false;
	}

	nodePtr = new ListNode(dataPtr);

	nodePtr.setNextPtr(currPtr);
	prevPtr.setNextPtr(nodePtr);
	return true;
    }

    boolean remove(Element dataPtr) {

	ListNode prevPtr;
	ListNode nodePtr;

	prevPtr = findPrevious(dataPtr);

	nodePtr = prevPtr.getNextPtr();

	if((nodePtr != null) && nodePtr.getDataPtr().equals(dataPtr))
	{
	    prevPtr.setNextPtr(nodePtr.getNextPtr());

	    return true;
	}

	return false;
    }

}
