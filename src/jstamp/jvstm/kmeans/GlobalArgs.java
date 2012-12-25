package jstamp.jvstm.kmeans;

import jvstm.PerTxBox;
import jvstm.VBoxFloat;
import jvstm.VBoxInt;

/* ==============================================================================
 *
 * GlobalArgs.java
 * -- Class that holds all the global parameters used by each thread
 *    during parallel execution
 *
 * =============================================================================
 * Author:
 *
 * Alokika Dash
 * University of California, Irvine
 * email adash@uci.edu
 *
 * =============================================================================
 */

public class GlobalArgs {

    public GlobalArgs() {

    }

    /**
     * Number of threads
     **/
    public int nthreads;

    /**
     * List of attributes
     **/
    public float[][] feature;

    /**
     * Number of attributes per Object
     **/
    public int nfeatures;

    /**
     * Number of Objects
     **/
    public int npoints;


    /**
     * Iteration id between min_nclusters to max_nclusters 
     **/
    public int nclusters;


    /**
     * Array that holds change index of cluster center per thread 
     **/
    public int[] membership;

    /**
     *
     **/
    public float[][] clusters;


    /**
     * Number of points in each cluster [nclusters]
     **/
    public VBoxInt[] new_centers_len;
    
    public PerTxBox<Integer>[] new_centers_len_inc;
    
    private class NewCentersLenPerTxBox extends PerTxBox<Integer> {
	private final int index;
	public NewCentersLenPerTxBox(int index, Integer initial) {
	    super(initial);
	    this.index = index;
	}

	@Override
	public void commit(Integer value) {
	    new_centers_len[index].put(new_centers_len[index].get() + value);
	}
    }

    public PerTxBox<Integer> createNewCentersLenPerTxBox(int index) {
	return new NewCentersLenPerTxBox(index, 0);
    }
    
    /**
     * New centers of the clusters [nclusters][nfeatures]
     **/
    public VBoxFloat[][] new_centers;

    public PerTxBox<Float>[][] new_centers_inc;
    
    private class NewCentersPerTxBox extends PerTxBox<Float> {
	private final int index1;
	private final int index2;
	public NewCentersPerTxBox(int index1, int index2, Float initial) {
	    super(initial);
	    this.index1 = index1;
	    this.index2 = index2;
	}

	@Override
	public void commit(Float value) {
	    new_centers[index1][index2].put(new_centers[index1][index2].get() + value);
	}
    }
    
    public PerTxBox<Float> createNewCentersPerTxBox(int index1, int index2) {
	return new NewCentersPerTxBox(index1, index2, 0.0f);
    }
    
    /**
     *
     **/
    public final VBoxInt global_i = new VBoxInt(0);

    public final PerTxBox<Integer> global_i_inc = new PerTxBox<Integer>(0) {
	@Override
	public void commit(Integer value) {
	    global_i.put(global_i.get() + value);
	}
    };
    
    public final VBoxFloat global_delta = new VBoxFloat(0.0f);

    public final PerTxBox<Float> global_delta_inc = new PerTxBox<Float>(0.0f) {
	@Override
	public void commit(Float value) {
	    global_delta.put(global_delta.get() + value);
	}
    };

    long global_time;

}
