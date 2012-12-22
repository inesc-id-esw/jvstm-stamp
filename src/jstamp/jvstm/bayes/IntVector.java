package jstamp.jvstm.bayes;

/**
 * Author: Alokika Dash
 * University of California, Irvine
 * adash@uci.edu
 *
 * - Vector class for "ints" elements 
 **/

public class IntVector {
  int size;
  int capacity;
  int[] elements;

  public IntVector() {

  }

  /* =============================================================================
   * Vector_alloc
   * -- Returns null if failed
   * =============================================================================
   */
  public static IntVector vector_alloc (int initCapacity) {
    int capacity = Math.max(initCapacity, 1);
    IntVector vectorPtr = new IntVector();
    vectorPtr.size = 0;
    vectorPtr.capacity = capacity;
    vectorPtr.elements = new int[capacity];
    return vectorPtr;
  }

  /* =============================================================================
   * Vector_free
   * =============================================================================
   */
  public void
    vector_free ()
    {
      elements = null;
    }

  /* =============================================================================
   * Vector_at
   * -- Returns null if failed
   * =============================================================================
   */
  public int vector_at (int i) {
    if ((i < 0) || (i >= size)) {
      System.out.println("Illegal Vector.element\n");
      return -1;
    }
    return (elements[i]);
  }


  /* =============================================================================
   * Vector_pushBack
   * -- Returns false if fail, else true
   * =============================================================================
   */
  public boolean vector_pushBack (int dataPtr) {
    if (size == capacity) {
      int newCapacity = capacity * 2;
      int[] newElements = new int[newCapacity];

      capacity = newCapacity;
      for (int i = 0; i < size; i++) {
        newElements[i] = elements[i];
      }
      elements = null;
      elements = newElements;
    }

    elements[size++] = dataPtr;

    return true;
  }

  /* =============================================================================
   * Vector_popBack
   * -- Returns null if fail, else returns last element
   * =============================================================================
   */
  public int
    vector_popBack ()
    {
      if (size < 1) {
        return 0;
      }

      return (elements[--(size)]);
    }

  /* =============================================================================
   * Vector_getSize
   * =============================================================================
   */
  public int
    vector_getSize ()
    {
      return (size);
    }

  /* =============================================================================
   * Vector_clear
   * =============================================================================
   */
  public void
    vector_clear ()
    {
      size = 0;
    }
  
  /* =============================================================================
   * Vector_copy
   * =============================================================================
   */
  public static boolean
    vector_copy (IntVector dstVectorPtr, IntVector srcVectorPtr)
    {
      int dstCapacity = dstVectorPtr.capacity;
      int srcSize = srcVectorPtr.size;
      if (dstCapacity < srcSize) {
        int srcCapacity = srcVectorPtr.capacity;
        int[] elements = new int[srcCapacity];

        dstVectorPtr.elements = null;
        dstVectorPtr.elements = elements;
        dstVectorPtr.capacity = srcCapacity;
      }

      for(int i = 0; i< srcSize; i++) {
        dstVectorPtr.elements[i] = srcVectorPtr.elements[i];
      }

      dstVectorPtr.size = srcSize;

      return true;
    }
}
