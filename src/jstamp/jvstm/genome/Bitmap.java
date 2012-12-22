package jstamp.jvstm.genome;

public class Bitmap {
  private int numBit;
  private int numWord;
  private int bits[];
  
  private int NUM_BIT_PER_BYTE;
  private int NUM_BIT_PER_WORD;

  Bitmap(int myNumBit) {

    NUM_BIT_PER_BYTE = 8;
    NUM_BIT_PER_WORD = ((8) * NUM_BIT_PER_BYTE);

    numBit = myNumBit;
    numWord = DIVIDE_AND_ROUND_UP(numBit, NUM_BIT_PER_WORD);

    bits = new int[numWord];
    
    int i = 0;
    for(i = 0; i < numWord; i++) {
      bits[i] = 0;
    }
  }

  Bitmap(Bitmap myBitMap) {
    NUM_BIT_PER_BYTE = 8;
    NUM_BIT_PER_WORD = ((8) * NUM_BIT_PER_BYTE);


    numBit = myBitMap.numBit;
    numWord = myBitMap.numWord;
    bits = new int[numWord];
    int i = 0;
    for(i = 0; i < numWord; i++) {
      bits[i] = myBitMap.bits[i];
    }
  }

  boolean set (int i) {
    if ((i < 0) || (i >= numBit)) {
      return false;
    }

    bits[i/NUM_BIT_PER_WORD] |= (1 << (i % NUM_BIT_PER_WORD));

    return true;
  }

  boolean clear (int i) {
      if ((i < 0) || (i >= numBit)) {
      return false;
    }

    bits[i/NUM_BIT_PER_WORD] &= ~(1 << (i % NUM_BIT_PER_WORD));

    return true;
  }
  
  void clearAll () {
    int i = 0;
    for(i = 0; i < numWord; i++) {
      bits[i] = 0;
    }
  }

  boolean isSet (int i) {
    int tempB = bits[i/NUM_BIT_PER_WORD];
    int tempC = (1 << (i % NUM_BIT_PER_WORD));
    boolean tempbool = ((tempB & tempC) > 0) ? true:false;
    //tempB /*bits[((int)i)/NUM_BIT_PER_WORD]*/ & tempC /*(1 << (i % NUM_BIT_PER_WORD))*/ 
    if ((i >= 0) && (i < numBit) && tempbool) {
        return true;
    }

    return false;
  }

  int findClear (int startIndex) {
    int i;
    for (i = (startIndex>0?startIndex:0);i < numBit; i++) {
    	boolean tempbool = ((bits[i/NUM_BIT_PER_WORD] & (1 << (i % NUM_BIT_PER_WORD))) > 0) ? true:false;        
    	if (!tempbool) {
            return i;
        }
    }

    return -1;
  }

  int findSet (int startIndex) {
    int i;

    for (i = (startIndex>0? startIndex: 0); i < numBit; i++) {
      boolean tempbool = ((bits[i/NUM_BIT_PER_WORD] & (1 << (i % NUM_BIT_PER_WORD))) > 0) ? true:false;
        if (tempbool) {
            return i;
        }
    }

    return -1;
  }

  int getNumClear () {
    return (numBit - getNumSet());
  }

  int getNumSet () {
    int i;
    int count = 0;
    for (i = 0; i < numBit; i++) {
        boolean tempbool = ((bits[i/NUM_BIT_PER_WORD] & (1 << (i % NUM_BIT_PER_WORD))) > 0) ? true:false;
        if (tempbool) {
            count++;
        }
    }

    return count;
  }

  void toggleAll () {
    int w;
    for (w = 0; w < numWord; w++) {
      bits[w] ^= -1;
    }
  }

  static int DIVIDE_AND_ROUND_UP(int a, int b) {
    return (a/b) + (((a % b) > 0) ? (1) : (0));
  }
}
