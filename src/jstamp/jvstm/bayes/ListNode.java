package jstamp.jvstm.bayes;

import jvstm.VBox;

public class ListNode {
    final public LearnerTask dataPtr;
    public final VBox<ListNode> nextPtr;

    public ListNode(LearnerTask dataPtr) {
	this.dataPtr = dataPtr;
	this.nextPtr = new VBox<ListNode>();
    }
}
