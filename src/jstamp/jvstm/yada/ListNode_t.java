package jstamp.jvstm.yada;

import jvstm.VBox;

public class ListNode_t {
    private final Object dataPtr;
    private final VBox<ListNode_t> nextPtr;

    public ListNode_t () {
	dataPtr = null;
	nextPtr = new VBox<ListNode_t>(); // conf
    }

    public ListNode_t (Object myDataPtr) {
	dataPtr = myDataPtr;
	nextPtr = new VBox<ListNode_t>();
    }

    public Object getDataPtr() {
	return dataPtr;
    }

    public ListNode_t getNextPtr() {
	return nextPtr.get();
    }

    public void setNextPtr(ListNode_t nextPtr) {
	this.nextPtr.put(nextPtr);
    }
}
