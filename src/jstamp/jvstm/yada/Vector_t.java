package jstamp.jvstm.yada;

public class Vector_t {
    private int size;
    private int capacity;
    private Object[] elements;


    /* =============================================================================
     * Vector_alloc
     * -- Returns null if failed
     * =============================================================================
     */
    public static Vector_t vector_alloc (int initCapacity) {
	int capacity = Math.max(initCapacity, 1);
	Vector_t vectorPtr = new Vector_t();
	vectorPtr.capacity = capacity;
	vectorPtr.elements = new Object[capacity];
	vectorPtr.size = 0;
	return vectorPtr;
    }

    public int size() {
	return size;
    }

    /* =============================================================================
     * Vector_at
     * -- Returns null if failed
     * =============================================================================
     */
    public Object vector_at (int i) {
	return elements[i];
    }


    /* =============================================================================
     * Vector_pushBack
     * -- Returns false if fail, else true
     * =============================================================================
     */
    public boolean vector_pushBack (Object dataPtr) {
	if (size >= capacity) {
	    int newCapacity = capacity * 2;
	    Object[] newElements = new Object[newCapacity];
	    for(int i=0;i<elements.length;i++)
		newElements[i] = elements[i];
	    capacity = newCapacity;
	    elements = newElements;
	}

	elements[size] = dataPtr;
	size++;
	return true;
    }

    /* =============================================================================
     * Vector_getSize
     * =============================================================================
     */
    public int vector_getSize () {
	return size;
    }

    /* =============================================================================
     * Vector_clear
     * =============================================================================
     */
    public void vector_clear () {
	size = 0;
    }
}