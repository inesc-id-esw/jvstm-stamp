package jstamp.jvstm.yada;


public class Coordinate {

    private final double x;
    private final double y;

    public Coordinate(double x, double y) {
	this.x = x;
	this.y = y;
    }
    
    public double getX() {
	return x;
    }

    public double getY() {
	return y;
    }

    static long coordinate_compare(Coordinate aPtr, Coordinate bPtr) {
	if (aPtr.getX() < bPtr.getX()) {
	    return -1;
	} else if (aPtr.getX() > bPtr.getX()) {
	    return 1;
	} else if (aPtr.getY() < bPtr.getY()) {
	    return -1;
	} else if (aPtr.getY() > bPtr.getY()) {
	    return 1;
	}

	return 0;
    }

    static double coordinate_distance(Coordinate coordinatePtr, Coordinate aPtr) {
	double delta_x = coordinatePtr.getX() - aPtr.getX();
	double delta_y = coordinatePtr.getY() - aPtr.getY();

	return Math.sqrt((delta_x * delta_x) + (delta_y * delta_y));
    }

    static double coordinate_angle (Coordinate aPtr, Coordinate bPtr, Coordinate cPtr) {

	Coordinate delta_b = new Coordinate(bPtr.getX() - aPtr.getX(), bPtr.getY() - aPtr.getY());
	Coordinate delta_c = new Coordinate(cPtr.getX() - aPtr.getX(), cPtr.getY() - aPtr.getY());
	double distance_b;
	double distance_c;
	double numerator;
	double denominator;
	double cosine;
	double radian;

	numerator = (delta_b.getX() * delta_c.getX()) + (delta_b.getY() * delta_c.getY());

	distance_b = coordinate_distance(aPtr, bPtr);
	distance_c = coordinate_distance(aPtr, cPtr);
	denominator = distance_b * distance_c;

	if(denominator==0) {
	    cosine = 0;
	} else {
	    cosine = numerator / denominator;
	}
	radian = Math.acos(cosine);

	return (180.0 * radian / Math.PI);
    }

    static void coordinate_print(Coordinate coordinatePtr) {
//	System.out.println(coordinatePtr.getX()+" "+coordinatePtr.getY());
    }


}
