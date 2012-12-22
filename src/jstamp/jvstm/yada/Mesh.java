package jstamp.jvstm.yada;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import jvstm.VBox;

public class Mesh {

    private final VBox<Element> rootElementPtr;
    private final Queue initBadQueuePtr;
    private final RBTree boundarySetPtr;

    public Mesh() {
	this.rootElementPtr = new VBox<Element>(null);
	this.initBadQueuePtr = new Queue(-1);
	this.boundarySetPtr = new RBTree();
    }

    void mesh_insert (Element elementPtr, Hashtable edgeMapPtr) {
	/*
	 * Assuming fully connected graph, we just need to record one element.
	 * The root element is not needed for the actual refining, but rather
	 * for checking the validity of the final mesh.
	 */
	if (rootElementPtr.get()==null) {
	    this.rootElementPtr.put(elementPtr);
	}

	/*
	 * Record existence of each of this element's edges
	 */
	long i;
	long numEdge = elementPtr.element_getNumEdge();

	for (i = 0; i < numEdge; i++) {
	    Edge edgePtr = elementPtr.element_getEdge(i);

	    if (!edgeMapPtr.contains(edgePtr)) {
		/*
		 * Record existence of this edge
		 */
		edgeMapPtr.insert(edgePtr, elementPtr);
	    } else {

		/*
		 * Shared edge; update each element's neighborList
		 */
		boolean isSuccess;
		Element sharerPtr = (Element)edgeMapPtr.get(edgePtr);

		elementPtr.element_addNeighbor(sharerPtr);
		sharerPtr.element_addNeighbor(elementPtr);

		isSuccess = edgeMapPtr.remove(edgePtr);
		assert(isSuccess);
	    }
	}

	/*
	 * Check if really encroached
	 */

	Edge encroachedPtr = elementPtr.element_getEncroachedPtr();
	if (encroachedPtr!=null) {

	    if (!this.boundarySetPtr.contains(encroachedPtr)) {
		elementPtr.element_clearEncroached();
	    }
	}
    }

    void mesh_remove(Element elementPtr) {

	assert(!elementPtr.element_isGarbage());
	/*
	 * If removing root, a new root is selected on the next mesh_insert, which
	 * always follows a call a mesh_remove.
	 */
	if (this.rootElementPtr.get() == elementPtr) {
	    this.rootElementPtr.put(null);
	}

	/*
	 * Remove from neighbors
	 */
	List neighborListPtr = elementPtr.element_getNeighborListPtr();
	ListNode node = neighborListPtr.getHead().getNextPtr();
	while(node!=null) {
	    Element neighborPtr = node.getDataPtr();
	    List neighborNeighborListPtr = neighborPtr.element_getNeighborListPtr();
	    boolean status = neighborNeighborListPtr.remove(elementPtr);
	    assert(status);
	    node = node.getNextPtr();
	}

	elementPtr.element_setIsGarbage(true);
    }

    boolean mesh_removeBoundary (Edge boundaryPtr) {
	return this.boundarySetPtr.remove(boundaryPtr);
    }

    void mesh_insertBoundary (Edge boundaryPtr) {
	this.boundarySetPtr.insert(boundaryPtr,null);
    }

    long mesh_read (String fileNamePrefix) {
	Hashtable edgeMapPtr = new Hashtable(Yada.numBuckets);
	long numElement = 0;

	try {
	    /*
	     * Read .node file
	     */
	    //	    System.out.println("Node");
	    FileInputStream fstream = new FileInputStream(fileNamePrefix+".node");
	    DataInputStream in = new DataInputStream(fstream);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));

	    String strLine = br.readLine();
	    String[] line = strLine.split(" ");
	    line = tretaDeInput(line,2);
	    long numEntry = Integer.parseInt(line[0]);
	    long numDimension = Integer.parseInt(line[1]);
	    assert(numDimension == 2); /* must be 2-D */
	    long numCoordinate = numEntry+1;
	    Coordinate[] coordinates = new Coordinate[(int) (numCoordinate+1)];
	    while ((strLine = br.readLine()) != null)   {
		line = strLine.trim().split(" "); 
		if(line[0].trim().equals("#")) break;
		line = tretaDeInput(line,3);
		long id = Long.parseLong(line[0].trim());
		double x = Double.parseDouble(line[1].trim());
		double y = Double.parseDouble(line[2].trim());
		coordinates[(int) id] = new Coordinate(x, y);
	    }
	    br.close();
	    in.close();
	    fstream.close();

	    /*
	     * Read .poly file, which contains boundary segments
	     */
	    //	    System.out.println("Poly");
	    fstream = new FileInputStream(fileNamePrefix+".poly");
	    in = new DataInputStream(fstream);
	    br = new BufferedReader(new InputStreamReader(in));

	    strLine = br.readLine();
	    line = strLine.split(" ");
	    line = tretaDeInput(line,2);
	    numEntry = Integer.parseInt(line[0]);
	    numDimension = Integer.parseInt(line[1]);
	    assert(numEntry == 0); /* .node file used for vertices */
	    assert(numDimension == 2); /* must be 2-D */
	    strLine = br.readLine();
	    line = strLine.split(" ");
	    line = tretaDeInput(line,1);
	    numEntry = Integer.parseInt(line[0]);

	    while ((strLine = br.readLine()) != null)   {
		line = strLine.trim().split(" ");
		Coordinate insertCoordinates[] = new Coordinate[2];
		if(line[0].trim().equals("#")) break;
		line = tretaDeInput(line,3);
		double a = Double.parseDouble(line[1].trim());
		double b = Double.parseDouble(line[2].trim());
		assert(a >= 0 && a < numCoordinate);
		assert(b >= 0 && b < numCoordinate);
		insertCoordinates[0] = coordinates[(int) a];
		insertCoordinates[1] = coordinates[(int) b];
		createElement(insertCoordinates, 2, edgeMapPtr);
	    }

	    br.close();
	    in.close();
	    fstream.close();
	    numElement += numEntry;

	    /*
	     * Read .ele file, which contains triangles
	     */
	    //	    System.out.println("Ele");
	    fstream = new FileInputStream(fileNamePrefix+".ele");
	    in = new DataInputStream(fstream);
	    br = new BufferedReader(new InputStreamReader(in));

	    strLine = br.readLine();
	    line = strLine.split(" ");
	    line = tretaDeInput(line,2);
	    numEntry = Integer.parseInt(line[0]);
	    numDimension = Integer.parseInt(line[1]);
	    assert(numDimension == 3); /* must be triangle */

	    while ((strLine = br.readLine()) != null) {
		line = strLine.trim().split(" "); 
		Coordinate insertCoordinates[] = new Coordinate[3];
		if(line[0].trim().equals("#")) break;
		line = tretaDeInput(line,4);
		long a = Long.parseLong(line[1].trim());
		long b = Long.parseLong(line[2].trim());
		long c = Long.parseLong(line[3].trim());
		assert(a >= 0 && a < numCoordinate);
		assert(b >= 0 && b < numCoordinate);
		assert(c >= 0 && c < numCoordinate);
		insertCoordinates[0] = coordinates[(int) a];
		insertCoordinates[1] = coordinates[(int) b];
		insertCoordinates[2] = coordinates[(int) c];
		createElement(insertCoordinates, 3, edgeMapPtr);
	    }

	    br.close();
	    in.close();
	    fstream.close();
	    numElement += numEntry;
	} catch(IOException e) {
	    System.out.println(e);
	    System.exit(-1);
	}

	return numElement;
    }

    private String[] tretaDeInput(String[] line, int size) {
	String[] out = new String[size];
	int index=0;
	for(int i=0;i<line.length;i++) {
	    if(line[i].trim().matches("[-.0-9]+")) {
		out[index] = line[i];
		index++;
	    }
	    if(index==size)
		return out;
	}
	return out;
    }

    Element mesh_getBad () {
	return (Element)this.initBadQueuePtr.queue_pop();
    }

    void mesh_shuffleBad (Random randomPtr) {
	this.initBadQueuePtr.queue_shuffle(randomPtr);
    }

    boolean mesh_check (long expectedNumElement) {
	Queue_t searchQueuePtr;
	Hashtable visitedMapPtr;
	long numBadTriangle = 0;
	long numFalseNeighbor = 0;
	long numElement = 0;

	//	System.out.println("Checking final mesh:");

	searchQueuePtr = new Queue_t(-1);
	visitedMapPtr = new Hashtable(Yada.numBuckets);

	/*
	 * Do breadth-first search starting from rootElementPtr
	 */
	searchQueuePtr.queue_push(this.rootElementPtr.get());
	while (!searchQueuePtr.queue_isEmpty()) {

	    Element currentElementPtr;
	    List neighborListPtr;

	    currentElementPtr = (Element)searchQueuePtr.queue_pop();
	    if (visitedMapPtr.contains(currentElementPtr)) {
		continue;
	    }
	    visitedMapPtr.insert(currentElementPtr, null);

	    if (!currentElementPtr.element_checkAngles()) {
		numBadTriangle++;
	    }
	    neighborListPtr = currentElementPtr.element_getNeighborListPtr();

	    ListNode node = neighborListPtr.getHead().getNextPtr();
	    while (node!=null) {
		/*
		 * Continue breadth-first search
		 */
		if (!visitedMapPtr.contains(node.getDataPtr())) {
		    boolean isSuccess;
		    isSuccess = searchQueuePtr.queue_push(node.getDataPtr());
		    assert(isSuccess);
		}
		node = node.getNextPtr();
	    } /* for each neighbor */

	    numElement++;

	} /* breadth-first search */

	//	System.out.println("Number of elements      = "+numElement);
	//	System.out.println("Number of bad triangles = "+numBadTriangle);

	return ((numBadTriangle > 0 || 	numFalseNeighbor > 0 || numElement != expectedNumElement) ? false : true);
    }

    void createElement(Coordinate[] coordinates, long numCoordinate, Hashtable edgeMapPtr) {

	Element elementPtr = new Element(coordinates, numCoordinate);

	if (numCoordinate == 2) {
	    Edge boundaryPtr = elementPtr.element_getEdge(0);

	    boundarySetPtr.insert(boundaryPtr,null);
	}

	mesh_insert(elementPtr, edgeMapPtr);

	if (elementPtr.element_isBad()) {
	    boolean status = initBadQueuePtr.queue_push(elementPtr);
	    assert(status);
	}
    }
}