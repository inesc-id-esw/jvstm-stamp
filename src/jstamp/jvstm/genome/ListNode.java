package jstamp.jvstm.genome;

import jvstm.VBox;

public class ListNode {
    final Pair dataPtr;
    final VBox<ListNode> nextPtr;

    public ListNode () {
	dataPtr = null;
	nextPtr = new VBox<ListNode>(null);
    }

    public ListNode (Pair myDataPtr, ListNode nextPtr) {
	this.dataPtr = myDataPtr;
	this.nextPtr = new VBox<ListNode>(nextPtr);
    } 
}
