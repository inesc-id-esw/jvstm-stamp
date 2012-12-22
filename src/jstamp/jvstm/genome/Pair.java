package jstamp.jvstm.genome;

public class Pair {
    final ByteString firstPtr;
    final ByteString secondPtr;
    
    public Pair() {
      firstPtr = null;
      secondPtr = null;
    }
    
    public Pair(ByteString myFirstPtr, ByteString mySecondPtr) { 
      firstPtr = myFirstPtr;
      secondPtr = mySecondPtr;
    }
}
