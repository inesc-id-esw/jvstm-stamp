package jstamp.jvstm.yada;

public class Edge implements Comparable<Edge> {
    protected Object firstPtr;
    protected Object secondPtr;

    public Edge() {
	this.firstPtr = null;
	this.secondPtr = null;
    }
    
    public Edge(Object firstPtr, Object secondPtr) {
	this.firstPtr = firstPtr;
	this.secondPtr = secondPtr;
    }

    public Object getFirstPtr() {
	return firstPtr;
    }

    public Object getSecondPtr() {
	return secondPtr;
    }

    public static int compare(Edge e1, Edge e2) {
	long diffFirst = Coordinate.coordinate_compare((Coordinate)e1.getFirstPtr(),
		(Coordinate)e2.getFirstPtr());
	return (int) ((diffFirst != 0) ? (diffFirst) : (Coordinate.coordinate_compare(
		(Coordinate)e1.getSecondPtr(), (Coordinate)e2.getSecondPtr())));		
    }

    @Override
    public boolean equals(Object obj) {
	if(obj instanceof Edge) {
	    Edge e2 = (Edge) obj; 
	    long diffFirst = Coordinate.coordinate_compare((Coordinate)this.getFirstPtr(),
		    (Coordinate)e2.getFirstPtr());
	    long diffSecond = Coordinate.coordinate_compare((Coordinate)this.getSecondPtr(), 
		    (Coordinate)e2.getSecondPtr());
	    return diffFirst==0 && diffSecond==0;
	} else {
	    return false;
	}
    }

    public long hashForTable() {
	return (long) Math.abs((((Coordinate)firstPtr).getX()+((Coordinate)firstPtr).getY()));
    }

    public void print() {
//	System.out.println(((Coordinate)firstPtr.get()).getX()+" "+((Coordinate)firstPtr.get()).getY()+" "+((Coordinate)secondPtr.get()).getX()+" "+((Coordinate)secondPtr.get()).getY());
    }

    @Override
    public int compareTo(Edge o) {
	return compare(this, o);
    }
}