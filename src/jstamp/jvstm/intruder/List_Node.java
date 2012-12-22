package jstamp.jvstm.intruder;

import jvstm.VBox;

public class List_Node {
    final Object dataPtr;
    final VBox<List_Node> nextPtr;

    public List_Node(Object dataPtr, List_Node nextPtr) {
	this.dataPtr = dataPtr;
	this.nextPtr = new VBox<List_Node>(nextPtr);
    }
}
