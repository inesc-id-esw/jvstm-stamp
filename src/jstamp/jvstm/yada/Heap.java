package jstamp.jvstm.yada;

import jstamp.jvstm.CallableCollectAborts;
import jvstm.Transaction;
import jvstm.VBox;
import jvstm.VBoxInt;
import jvstm.VBoxLong;

public class Heap {

    private final VBox<VBox<Element>[]> elements;
    private final VBoxInt size;
    private final VBoxLong capacity;

    public Heap(int initCapacity) {
	int capacity = ((initCapacity > 0) ? (initCapacity) : (1));
	VBox<Element>[] arr = new VBox[capacity];
	for(int i=0;i<capacity;i++)
	    arr[i] = new VBox<Element>();
	this.elements = new VBox<VBox<Element>[]>(arr);
	this.size = new VBoxInt(0);
	this.capacity = new VBoxLong(capacity);
    }

    private void siftUp (int long1) {
	long index = long1;
	VBox<Element>[] arr = elements.get();
	while ((index > 1)) {
	    long parentIndex = index/2;
	    Element parentPtr = arr[(int) parentIndex].get();
	    Element thisPtr = arr[(int) index].get();
	    if (Element.element_heapCompare(parentPtr, thisPtr) >= 0) {
		break;
	    }
	    Element tmpPtr = parentPtr;
	    arr[(int) parentIndex].put(thisPtr);
	    arr[(int) index].put(tmpPtr);
	    index = parentIndex;
	}
    }

    public boolean heap_insert (Element dataPtr) {
	VBox<Element>[] arr = elements.get();
	if ((size.get() + 1) >= capacity.get()) {
	    VBox<Element>[] newElements = new VBox[(int) (capacity.get()*2)];
	    for(int i=0;i<arr.length;i++)
		newElements[i] = arr[i];
	    for(int i=arr.length;i<newElements.length;i++)
		newElements[i] = new VBox<Element>();
	    capacity.put(capacity.get()*2);
	    elements.put(newElements);
	    arr = newElements;
	}

	size.put(size.get()+1);
	arr[size.getInt()].put(dataPtr);
	siftUp(size.get());
	return true;
    }

    private void heapify (int startIndex) {
	int index = startIndex;

	VBox<Element>[] arr = elements.get();
	while (true) {

	    int leftIndex = index*2;
	    int rightIndex = 2*index+1;
	    int maxIndex = -1;

	    if ((leftIndex <= size.get()) &&
		    (Element.element_heapCompare(arr[leftIndex].get(), arr[index].get()) > 0))
	    {
		maxIndex = leftIndex;
	    } else {
		maxIndex = index;
	    }

	    if ((rightIndex <= size.get()) &&
		    (Element.element_heapCompare(arr[rightIndex].get(), arr[maxIndex].get()) > 0))
	    {
		maxIndex = rightIndex;
	    }

	    if (maxIndex == index) {
		break;
	    } else {
		Element tmpPtr = arr[index].get();
		arr[index].put(arr[maxIndex].get());
		arr[maxIndex].put(tmpPtr);
		index = maxIndex;
	    }
	}
    }

    public Element heap_remove() {
	try {
	    CallableCollectAborts<Element> cmd = new CallableCollectAborts<Element>() {
		public Element runTx() {
		    if (size.get() < 1) {
			return null;
		    }
		    Element dataPtr = ret();
		    heapify(1);
		    return dataPtr;
		}
	    };
	    Element r = Transaction.doIt(cmd);
	    return r;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    private Element ret() {
	VBox<Element>[] arr = elements.get();
	Element dataPtr = arr[1].get();
	arr[1].put(arr[size.get()].get());
	size.put(size.get()-1);
	return dataPtr;
    }

}