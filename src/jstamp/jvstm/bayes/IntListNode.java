package jstamp.jvstm.bayes;

import jvstm.VBox;

/**
 * Author: Alokika Dash
 * University of California, Irvine
 * adash@uci.edu
 *
 * - Helper class for IntList.java
 **/

public class IntListNode {
    public final int dataPtr;
    public final VBox<IntListNode> nextPtr;

    public IntListNode(int dataPtr) {
	this.dataPtr = dataPtr;
	this.nextPtr = new VBox<IntListNode>();
    }
}
