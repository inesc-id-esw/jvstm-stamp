package jstamp.jvstm.kmeans;

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

  /**
   * New centers of the clusters [nclusters][nfeatures]
   **/
  public VBoxFloat[][] new_centers;

  /**
    *
  **/
  public final VBoxInt global_i = new VBoxInt(0);

  public final VBoxFloat global_delta = new VBoxFloat(0.0f);

  long global_time;

}
