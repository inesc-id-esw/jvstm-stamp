package jstamp.jvstm.yada;

import jvstm.VBox;

public class ListNode {
    private final Element dataPtr;
    private final VBox<ListNode> nextPtr;

    public ListNode () {
	dataPtr = null;
	nextPtr = new VBox<ListNode>(null);
    }

    public ListNode (Element myDataPtr) {
	dataPtr = myDataPtr;
	nextPtr = new VBox<ListNode>(null);
    }

    public Element getDataPtr() {
	return dataPtr;
    }

    public ListNode getNextPtr() {
	return nextPtr.get();
    }

    public void setNextPtr(ListNode nextPtr) {
	this.nextPtr.put(nextPtr);
    } 
}
