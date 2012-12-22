package jstamp.jvstm.yada;

import jvstm.VBox;


class ListNodeL {
    private final Object dataPtr;
    private final VBox<ListNodeL> nextPtr;

    public ListNodeL () {
	dataPtr = null;
	nextPtr = new VBox<ListNodeL>(); // conf
    }

    public ListNodeL (Object myDataPtr) {
	dataPtr = myDataPtr;
	nextPtr = new VBox<ListNodeL>(); // conf
    }

    public Object getDataPtr() {
	return dataPtr;
    }

    public ListNodeL getNextPtr() {
	return nextPtr.get();
    }

    public void setNextPtr(ListNodeL nextPtr) {
	this.nextPtr.put(nextPtr);
    }
}

public class List_t {
    private ListNodeL head;
    private int size;

    public List_t () {
	head = new ListNodeL();
	size = 0;
    }

    public ListNodeL getHead() {
	return head;
    }

    Object find (Object dataPtr) {
	ListNodeL nodePtr;
	ListNodeL prevPtr = findPrevious(dataPtr);

	nodePtr = prevPtr.getNextPtr();

	if ((nodePtr == null) || !nodePtr.getDataPtr().equals(dataPtr) ) {
	    return null;
	}

	return nodePtr.getDataPtr();
    }

    ListNodeL findPrevious (Object dataPtr) {
	ListNodeL prevPtr = head;
	ListNodeL nodePtr = prevPtr.getNextPtr();

	if(dataPtr instanceof Edge) {
	    Edge e = (Edge) dataPtr;
	    for (; nodePtr != null; nodePtr = nodePtr.getNextPtr()) {
		if (Edge.compare(((Edge)nodePtr.getDataPtr()),e) >= 0) {
		    return prevPtr;
		}
		prevPtr = nodePtr;
	    }
	}

	if(dataPtr instanceof Element) {
	    Element e = (Element) dataPtr;
	    for (; nodePtr != null; nodePtr = nodePtr.getNextPtr()) {
		if (((Element)nodePtr.getDataPtr()).compare(e) >= 0) {
		    return prevPtr;
		}
		prevPtr = nodePtr;
	    }
	}

	return prevPtr;
    }

    boolean insert (Object dataPtr) {
	ListNodeL prevPtr;
	ListNodeL nodePtr;
	ListNodeL currPtr;

	prevPtr = findPrevious(dataPtr);
	currPtr = prevPtr.getNextPtr();

	if((currPtr != null) && currPtr.getDataPtr().equals(dataPtr)) {
	    return false;
	}

	nodePtr = new ListNodeL(dataPtr);

	nodePtr.setNextPtr(currPtr);
	prevPtr.setNextPtr(nodePtr);
	size = size + 1;
	return true;
    }

    boolean remove(Object dataPtr) {

	ListNodeL prevPtr = findPrevious(dataPtr);
	if(prevPtr==null) {
	    return false;
	}
	prevPtr.setNextPtr(prevPtr.getNextPtr().getNextPtr());
	return true;
    }

    void clear() {
	head.setNextPtr(null);
	size = 0;
    }

    public long size() {
	return size;
    }
}
