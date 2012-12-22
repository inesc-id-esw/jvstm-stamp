package jstamp.jvstm.yada;

import jstamp.jvstm.CallableCollectAborts;
import jstamp.jvstm.CommandCollectAborts;
import jvstm.CommitException;
import jvstm.Transaction;

public class Region {

    private final Queue_t expandQueuePtr;
    private final List_t beforeListPtr; /* before retriangulation; list to avoid duplicates */
    private final List_t borderListPtr; /* edges adjacent to region; list to avoid duplicates */
    private final Vector_t badVectorPtr;

    public Region() {
	expandQueuePtr = new Queue_t(-1);
	beforeListPtr = new List_t();
	borderListPtr = new List_t();
	badVectorPtr = Vector_t.vector_alloc(1);
    }

    void Pregion_clearBad() {
	badVectorPtr.vector_clear();
    }

    long region_refine(final Element elementPtr, final Mesh meshPtr) {
	try {
	    CallableCollectAborts<Long> cmd = new CallableCollectAborts<Long>() {
		public Long runTx() {
		    long numDelta = 0L;
		    Hashtable edgeMapPtr = null;
		    Element encroachElementPtr = null;
		    edgeMapPtr = new Hashtable(Yada.numBuckets);
		    while (true) {
			encroachElementPtr = growRegion(elementPtr, meshPtr, edgeMapPtr);
			if (encroachElementPtr!=null) {
			    numDelta += region_refine(encroachElementPtr, meshPtr);
			    if (elementPtr.element_isGarbage()) {
				break;
			    }
			} else {
			    break;
			}
			edgeMapPtr.clear();
		    }

		    /*
		     * Perform retriangulation.
		     */

		    if (!elementPtr.element_isGarbage()) {
			numDelta += retriangulate(elementPtr,meshPtr,edgeMapPtr);
		    }
		    return numDelta;
		}
	    };
	    Long r = Transaction.doIt(cmd);
	    if (cmd.getAborts() > 0) {
		Yada.aborts.addAndGet(cmd.getAborts());
	    }
	    return r;
	} catch (Exception e) {
	    if (e instanceof RuntimeException) {
		throw (RuntimeException)e;
	    }
	    e.printStackTrace();
	    System.exit(-1);
	    return -1L;
	}
    }

    Element growRegion(Element centerElementPtr, Mesh meshPtr, Hashtable edgeMapPtr) {

	boolean isBoundary = false;
	if (centerElementPtr.element_getNumEdge() == 1) {
	    isBoundary = true;
	}

	beforeListPtr.clear();
	borderListPtr.clear();
	expandQueuePtr.queue_clear();
	Coordinate centerCoordinatePtr = centerElementPtr.element_getNewPoint();
	expandQueuePtr.queue_push(centerElementPtr);

	while(!expandQueuePtr.queue_isEmpty()) {
	    Element currentElementPtr = (Element) expandQueuePtr.queue_pop();

	    beforeListPtr.insert(currentElementPtr); /* no duplicates */
	    List neighborListPtr = currentElementPtr.element_getNeighborListPtr();

	    ListNode node = neighborListPtr.getHead().getNextPtr();

	    while (node!=null) {
		Element neighborElementPtr = (Element)node.getDataPtr();
		if (beforeListPtr.find(neighborElementPtr)==null) {
		    if (neighborElementPtr.element_isInCircumCircle(centerCoordinatePtr)) {
			//						/* This is part of the region */
			if (!isBoundary && (neighborElementPtr.element_getNumEdge() == 1)) {
			    /* Encroached on mesh boundary so split it and restart */
			    return neighborElementPtr;
			} else {
			    /* Continue breadth-first search */
			    boolean isSuccess;
			    isSuccess = expandQueuePtr.queue_push(neighborElementPtr);
			    assert(isSuccess);
			}
		    } else {
			/* This element borders region; save info for retriangulation */
			Edge borderEdgePtr = Element.element_getCommonEdge(neighborElementPtr, currentElementPtr);
			if (borderEdgePtr==null) {
			    throw new CommitException();
			}
			borderListPtr.insert(borderEdgePtr); /* no duplicates */
			if (!edgeMapPtr.contains(borderEdgePtr)) {
			    edgeMapPtr.insert(borderEdgePtr, neighborElementPtr);
			}
		    }
		} /* not visited before */
		node = node.getNextPtr();
	    } /* for each neighbor */

	} /* breadth-first search */
	return null;
    }

    long retriangulate (Element elementPtr, Mesh meshPtr, Hashtable edgeMapPtr) {
	long numDelta = 0L;

	Coordinate centerCoordinate = elementPtr.element_getNewPoint();

	/*
	 * Remove the old triangles
	 */

	ListNodeL node = beforeListPtr.getHead().getNextPtr();
	while(node!=null) {
	    meshPtr.mesh_remove((Element) node.getDataPtr());
	    node = node.getNextPtr();
	}

	numDelta -= beforeListPtr.size();
	/*
	 * If segment is encroached, split it in half
	 */

	if (elementPtr.element_getNumEdge() == 1) {

	    Coordinate coordinates[] = new Coordinate[2];

	    Edge edgePtr = elementPtr.element_getEdge(0);
	    coordinates[0] = centerCoordinate;

	    coordinates[1] = (Coordinate)(edgePtr.getFirstPtr());
	    Element aElementPtr = new Element(coordinates, 2);

	    meshPtr.mesh_insert(aElementPtr, edgeMapPtr);

	    coordinates[1] = (Coordinate)(edgePtr.getSecondPtr());
	    Element bElementPtr = new Element(coordinates, 2);
	    meshPtr.mesh_insert(bElementPtr, edgeMapPtr);

	    boolean status;
	    status = meshPtr.mesh_removeBoundary(elementPtr.element_getEdge(0));
	    assert(status);
	    meshPtr.mesh_insertBoundary(aElementPtr.element_getEdge(0));
	    meshPtr.mesh_insertBoundary(bElementPtr.element_getEdge(0));

	    numDelta += 2;
	}

	/*
	 * Insert the new triangles. These are contructed using the new
	 * point and the two points from the border segment.
	 */

	ListNodeL nodeE = borderListPtr.getHead().getNextPtr();
	while(nodeE!=null) {
	    Coordinate coordinates[] = new Coordinate[3];
	    coordinates[0] = centerCoordinate;
	    coordinates[1] = (Coordinate)(((Edge)nodeE.getDataPtr()).getFirstPtr());
	    coordinates[2] = (Coordinate)(((Edge)nodeE.getDataPtr()).getSecondPtr());

	    Element afterElementPtr = new Element(coordinates, 3);
	    meshPtr.mesh_insert(afterElementPtr, edgeMapPtr);
	    if (afterElementPtr.element_isBad()) {
		addToBadVector(afterElementPtr);
	    }
	    nodeE = nodeE.getNextPtr();
	}

	numDelta += borderListPtr.size();
	return numDelta;
    }

    void addToBadVector (Element badElementPtr) {
	boolean status = badVectorPtr.vector_pushBack(badElementPtr);
	assert(status);
    }

    void region_transferBad(final Heap workHeapPtr) {
	CommandCollectAborts cmd = new CommandCollectAborts() {
	    public void runTx() {
		long numBad = badVectorPtr.size();
		long i;
		
		for (i = 0; i < numBad; i++) {
		    Element badElementPtr = (Element)badVectorPtr.vector_at((int) i);
		    if (!badElementPtr.element_isGarbage()) {
			boolean status = workHeapPtr.heap_insert(badElementPtr);
			assert(status);
		    }
		}
	    }
	};
	Transaction.transactionallyDo(cmd);
	if (cmd.getAborts() > 0) {
	    Yada.aborts.addAndGet(cmd.getAborts());
	}
    }
}
