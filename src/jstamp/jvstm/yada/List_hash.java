package jstamp.jvstm.yada;



class ListNodeHM {
    private final Object dataPtr;
    private ListNodeHM nextPtr;

    public ListNodeHM () {
	dataPtr = null;
	nextPtr = null; // conf
    }

    public ListNodeHM (Object myDataPtr) {
	dataPtr = myDataPtr;
	nextPtr = null;
    }

    public Object getDataPtr() {
	return dataPtr;
    }

    public ListNodeHM getNextPtr() {
	return nextPtr;
    }

    public void setNextPtr(ListNodeHM nextPtr) {
	this.nextPtr = nextPtr;
    }
}


public class List_hash {
    private final ListNodeHM head;
    private ListNodeHM tail;

    public List_hash () {
	head = new ListNodeHM();
	tail = null;
    }

    public ListNodeHM getHead() {
	return head;
    }

    public ListNodeHM getTail() {
	return tail;
    }

    Pair find (Object dataPtr) {
	ListNodeHM node = getHead().getNextPtr();

	while(node!=null) {
	    Pair p = (Pair) node.getDataPtr();
	    if(p.getFirst().equals(dataPtr))
		return p;
	    node = node.getNextPtr();
	}

	return null;
    }

    boolean insert (Pair dataPtr) {
	ListNodeHM node = getHead().getNextPtr();
	ListNodeHM newNode = new ListNodeHM(dataPtr);

	if(node==null) {
	    getHead().setNextPtr(newNode);
	    tail = newNode;
	    return true;
	}

	ListNodeHM prevNode = getTail();
	prevNode.setNextPtr(newNode);
	tail = newNode;
	return true;
    }

    boolean remove(Object dataPtr) {

	ListNodeHM nodePtr = getHead().getNextPtr();
	ListNodeHM prevPtr = getHead();

	while(nodePtr!=null) {
	    if(((Pair)nodePtr.getDataPtr()).getFirst().equals(dataPtr)) {
		if(nodePtr.equals(getTail()))
		    tail = prevPtr;
		prevPtr.setNextPtr(nodePtr.getNextPtr());
		return true;
	    }
	    prevPtr = nodePtr;
	    nodePtr = nodePtr.getNextPtr();
	}
	return false;
    }

    void clear() {
	getHead().setNextPtr(null);
	tail = null;
    }
}
