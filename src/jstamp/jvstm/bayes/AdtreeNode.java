package jstamp.jvstm.bayes;

/**
 * Author: Alokika Dash
 * University of California, Irvine
 * adash@uci.edu
 *
 * - Helper class of Adtree.java
 **/
public class AdtreeNode {
  final int index;
  final int value;
  final int count;
  final AdtreeVary varyVectorPtr[];

  /* =============================================================================
   * allocNode
   * =============================================================================
   */
  public AdtreeNode(int index, int vecsize, int count, int value) {
    this.varyVectorPtr = new AdtreeVary[vecsize];
    this.index = index;
    this.value = value;
    this.count = count;
  }
}
