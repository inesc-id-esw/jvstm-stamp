package jstamp.jvstm.genome;

public class Gene {
  final public int length;
  final public ByteString contents;
  final public Bitmap startBitmapPtr; /* used for creating segments */
  
  Gene(int myLength, Random randomObj) {
    length = myLength;
    startBitmapPtr = new Bitmap(length);
    
    int i;
    byte[] nucleotides = new byte[4];
    byte[] arrayContents = new byte[length];
    nucleotides[0] = (byte) 'a';
    nucleotides[1] = (byte) 'c';
    nucleotides[2] = (byte) 'g';
    nucleotides[3] = (byte) 't';

    for (i = 0; i < length; i++) {
      arrayContents[i] = nucleotides[(int)(randomObj.random_generate() % 4)];
    }
    
    contents = new ByteString(arrayContents);
  }

}
