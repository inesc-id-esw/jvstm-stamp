package jstamp.jvstm.yada;

import jvstm.VBox;
import jvstm.VBoxBoolean;

public class Element {

    private final Coordinate coordinates[];
    private final long numCoordinate;
    private final Coordinate circumCenter;
    private final double circumRadius;
    private final Edge edges[];
    private final long numEdge;
    private final Coordinate midpoints[]; /* midpoint of each edge */
    private final boolean isSkinny;
    
    private final VBox<Edge> encroachedEdgePtr; /* opposite obtuse angle */
    private final List neighborListPtr;
    private final VBoxBoolean isGarbage;

    public Coordinate[] getCoordinates() {
	return coordinates;
    }

    public long getNumCoordinate() {
	return numCoordinate;
    }

    Element (Coordinate[] coordinates, long numCoordinate) {
	this.coordinates = new Coordinate[3];
	for (int i = 0; i < numCoordinate; i++) {
	    this.coordinates[i] = coordinates[i];
	}

	edges = new Edge[3];
	for(int i=0;i<edges.length;i++) {
	    edges[i] = new Edge();
	}

	midpoints = new Coordinate[3];

	this.numCoordinate = numCoordinate;
	neighborListPtr = new List();
	isGarbage = new VBoxBoolean(false);
	boolean skinny = false;
	encroachedEdgePtr = new VBox<Edge>(null);
	minimizeCoordinates();

	// check angles
	double minAngle = 180.0;
	assert(numCoordinate == 2 || numCoordinate == 3);

	skinny = false;
	this.encroachedEdgePtr.put(null);

	if (numCoordinate == 3) {
	    int i;
	    for (i = 0; i < 3; i++) {
		double angle = Coordinate.coordinate_angle(coordinates[i],coordinates[((i + 1) % 3)],
			coordinates[((i + 2) % 3)]);
		assert(angle > 0.0);
		assert(angle < 180.0);
		if (angle > 90.0) {
		    this.encroachedEdgePtr.put(edges[(int) ((i + 1) % 3)]);
		}
		if (angle < Yada.global_angleConstraint) {
		    skinny = true;
		}
		if (angle < minAngle) {
		    minAngle = angle;
		}
	    }
	    assert(minAngle < 180.0);
	}
	this.isSkinny = skinny;

	// calculateCircumCircle
	assert(numCoordinate == 2 || numCoordinate == 3);

	if (numCoordinate==2) {
	    circumCenter = new Coordinate((coordinates[0].getX() + coordinates[1].getX()) / 2.0, (coordinates[0].getY() + coordinates[1].getY()) / 2.0);
	} else {
	    double ax = coordinates[0].getX();
	    double ay = coordinates[0].getY();
	    double bx = coordinates[1].getX();
	    double by = coordinates[1].getY();
	    double cx = coordinates[2].getX();
	    double cy = coordinates[2].getY();
	    double bxDelta = bx - ax;
	    double byDelta = by - ay;
	    double cxDelta = cx - ax;
	    double cyDelta = cy - ay;
	    double bDistance2 = (bxDelta * bxDelta) + (byDelta * byDelta);
	    double cDistance2 = (cxDelta * cxDelta) + (cyDelta * cyDelta);
	    double xNumerator = (byDelta * cDistance2) - (cyDelta * bDistance2);
	    double yNumerator = (bxDelta * cDistance2) - (cxDelta * bDistance2);
	    double denominator = 2 * ((bxDelta * cyDelta) - (cxDelta * byDelta));
	    double rx = ax - (xNumerator / denominator);
	    double ry = ay + (yNumerator / denominator);

	    circumCenter = new Coordinate(rx, ry);
	}

	this.circumRadius = Coordinate.coordinate_distance(circumCenter,coordinates[0]);

	// init edges
	numEdge = ((numCoordinate * (numCoordinate - 1)) / 2);

	// set Edges
	for (long i = 0; i < numEdge; i++) {
	    Coordinate firstPtr = coordinates[(int) i];
	    Coordinate secondPtr = coordinates[(int) ((i + 1) % numCoordinate)];

	    Edge edgePtr = edges[(int) i];

	    long cmp = Coordinate.coordinate_compare(firstPtr, secondPtr);
	    assert(cmp != 0);
	    if (cmp < 0) {
		edgePtr.firstPtr = firstPtr;
		edgePtr.secondPtr = secondPtr;
	    } else {
		edgePtr.firstPtr = secondPtr;
		edgePtr.secondPtr = firstPtr;
	    }

	    midpoints[(int)i] = new Coordinate((firstPtr.getX() + secondPtr.getX()) / 2.0, (firstPtr.getY() + secondPtr.getY()) / 2.0);
	}
    }

    void minimizeCoordinates() {
	long minPosition = 0;

	for (long i = 1; i < numCoordinate; i++) {
	    if(Coordinate.coordinate_compare(coordinates[(int) i], coordinates[(int) minPosition]) < 0) {
		minPosition = i;
	    }
	}

	while (minPosition != 0) {
	    Coordinate tmp = coordinates[0];
	    long j;
	    for (j = 0; j < (numCoordinate - 1); j++) {
		coordinates[(int) j] = coordinates[(int) (j+1)];
	    }
	    coordinates[(int) (numCoordinate-1)] = tmp;
	    minPosition--;
	}
    }

    boolean element_checkAngles() {

	double angleConstraint = Yada.global_angleConstraint;

	if (numCoordinate == 3) {
	    long i;
	    for (i = 0; i < 3; i++) {
		double angle = Coordinate.coordinate_angle(coordinates[(int) i], coordinates[(int) ((i + 1) % 3)], coordinates[(int) ((i + 2) % 3)]);
		if (angle < angleConstraint) {
		    return false;
		}
	    }
	}
	return true;
    }

    static long element_compare (Element aElementPtr, Element bElementPtr) {
	long aNumCoordinate = aElementPtr.numCoordinate;
	long bNumCoordinate = bElementPtr.numCoordinate;
	Coordinate[] aCoordinates = aElementPtr.coordinates;
	Coordinate[] bCoordinates = bElementPtr.coordinates;

	if (aNumCoordinate < bNumCoordinate) {
	    return -1;
	} else if (aNumCoordinate > bNumCoordinate) {
	    return 1;
	}

	long i;
	for (i = 0; i < aNumCoordinate; i++) {
	    long compareCoordinate =
		    Coordinate.coordinate_compare(aCoordinates[(int) i], bCoordinates[(int) i]);
	    if (compareCoordinate != 0) {
		return compareCoordinate;
	    }
	}

	return 0;
    }

    static long element_listCompare (Element aPtr, Element bPtr) {
	return Element.element_compare(aPtr, bPtr);
    }

    long element_mapCompare (Edge aPtr, Edge bPtr) {
	Element aElementPtr = (Element) aPtr.getFirstPtr();
	Element bElementPtr = (Element) bPtr.getFirstPtr();

	return Element.element_compare(aElementPtr, bElementPtr);
    }

    boolean element_isGarbage() {
	return this.isGarbage.get();
    }

    void element_setIsGarbage(boolean status) {
	isGarbage.put(status);
    }

    long element_getNumEdge() {
	return numEdge;
    }

    Coordinate element_getNewPoint() {

	if (encroachedEdgePtr.get() != null) {
	    for (int e = 0; e < numEdge; e++) {
		if (Edge.compare(encroachedEdgePtr.get(), edges[e]) == 0) {
		    return midpoints[e];
		}
	    }
	}

	return circumCenter;
    }

    List element_getNeighborListPtr() {
	return neighborListPtr;
    }

    boolean element_isInCircumCircle(Coordinate coordinatePtr) {
	double distance = Coordinate.coordinate_distance(coordinatePtr, circumCenter);
	return ((distance <= circumRadius) ? true : false);
    }

    static Edge element_getCommonEdge(Element aElementPtr, Element bElementPtr) {
	Edge[] aEdges = aElementPtr.edges;
	Edge[] bEdges = bElementPtr.edges;
	long aNumEdge = aElementPtr.numEdge;
	long bNumEdge = bElementPtr.numEdge;
	long a;
	long b;

	for (a = 0; a < aNumEdge; a++) {
	    Edge aEdgePtr = aEdges[(int) a];
	    for (b = 0; b < bNumEdge; b++) {
		Edge bEdgePtr = bEdges[(int) b];
		if (Edge.compare(aEdgePtr, bEdgePtr) == 0) {
		    return aEdgePtr;
		}
	    }
	}
	return null;
    }

    Edge element_getEdge(long i) {
	if (i < 0 || i > numEdge) {
	    return null;
	}

	return edges[(int)i];
    }

    boolean element_isSkinny() {
	return ((isSkinny) ? true : false);
    }

    boolean isEncroached() {
	return ((this.encroachedEdgePtr.get() != null) ? true : false);
    }

    boolean element_isBad() {
	//    	if(Yada.DEBUG) System.out.println("isBad "+isEncroached()+" "+element_isSkinny());
	return ((this.isEncroached() || this.element_isSkinny()) ? true : false);
    }

    boolean element_addNeighbor(Element neighborPtr) {
	return neighborListPtr.insert(neighborPtr);
    }

    Edge element_getEncroachedPtr() {
	return encroachedEdgePtr.get();
    }

    void element_clearEncroached() {
	encroachedEdgePtr.put(null);
    }

    @Override
    public boolean equals(Object o) {

	if(o instanceof Element) {
	    Element e = (Element) o; 

	    if (numCoordinate < e.numCoordinate) {
		return false;
	    } else if (numCoordinate > e.numCoordinate) {
		return false;
	    }

	    long i;
	    for (i = 0; i < numCoordinate; i++) {
		long compareCoordinate = Coordinate.coordinate_compare(coordinates[(int) i], e.coordinates[(int) i]);
		if (compareCoordinate != 0) {
		    return false;
		}
	    }

	    return true;
	}
	return false;
    }

    void printElement() {

	//	int i;
	//	System.out.println("|---------------------------------------|");
	//	System.out.println(numCoordinate);
	//	for (i = 0; i < numCoordinate; i++) {
	//	    System.out.println("("+coordinates[i].getX()+" "+coordinates[i].getY()+") ");
	//	}
	//	System.out.println(" ("+circumCenter.getX()+" "+circumCenter.getY()+")");
	//	System.out.println((" | isBad = "+this.element_isBad()));
	//	System.out.println("|_______________________________________|");
    }

    public int compare(Element dataPtr) {

	if (numCoordinate < dataPtr.numCoordinate) {
	    return -1;
	} else if (numCoordinate > dataPtr.numCoordinate) {
	    return 1;
	}

	long i;
	for (i = 0; i < numCoordinate; i++) {
	    long compareCoordinate = Coordinate.coordinate_compare(coordinates[(int) i], dataPtr.coordinates[(int) i]);
	    if (compareCoordinate != 0) {
		return (int) compareCoordinate;
	    }
	}

	return 0;
    }

    static int element_heapCompare (Element aPtr, Element bPtr) {
	Element aElementPtr = aPtr;
	Element bElementPtr = bPtr;

	if (aElementPtr.encroachedEdgePtr.get()!=null) {
	    if (bElementPtr.encroachedEdgePtr.get()!=null) {
		return 0; /* do not care */
	    } else {
		return 1;
	    }
	}

	if (bElementPtr.encroachedEdgePtr.get()!=null) {
	    return -1;
	}

	return 0; /* do not care */
    }

    public long hashForTable() {
	return (long) Math.abs((circumCenter.getX()+circumCenter.getY()+circumRadius));
    }
}