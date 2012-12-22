package jstamp.jvstm.genome;

import jvstm.VBox;
import jvstm.VBoxBoolean;
import jvstm.VBoxInt;

public class constructEntry {
    final VBoxBoolean isStart;
    final VBox<ByteString> segment;
    final VBoxInt endHash;
    final VBox<constructEntry> startPtr;
    final VBox<constructEntry> nextPtr;
    final VBox<constructEntry> endPtr;
    final VBoxInt overlap;
    final VBoxInt length;
      
    constructEntry(ByteString mySegment, boolean myStart, int myEndHash, constructEntry myStartPtr, constructEntry myNextPtr, constructEntry myEndPtr, int myOverlap, int myLength) {
      segment = new VBox<ByteString>(mySegment);
      isStart = new VBoxBoolean(myStart);
      endHash = new VBoxInt(myEndHash);
      startPtr = new VBox<constructEntry>(this);
      nextPtr = new VBox<constructEntry>(myNextPtr);
      endPtr = new VBox<constructEntry>(this);
      overlap = new VBoxInt(myOverlap);
      length = new VBoxInt(myLength);
    }
    
    boolean equals(constructEntry copy) {
      return ((segment.get().compareTo(copy.segment.get()) == 0) && (isStart.getBoolean() == copy.isStart.getBoolean()) &&
	      (endHash.get() == copy.endHash.get()) && (startPtr.get() == copy.startPtr.get()) && (nextPtr.get() == copy.nextPtr.get()) && 
	      (endPtr.get() == copy.endPtr.get()) && (overlap.get() == copy.overlap.get()) && (length.get() == copy.length.get()));
    }
}
