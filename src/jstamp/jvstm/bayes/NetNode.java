package jstamp.jvstm.bayes;

/**
 * Author: Alokika Dash
 * University of California, Irvine
 * adash@uci.edu
 *
 * - Helper class for Net.java
 **/

public class NetNode {
  int mark;
  final IntList parentIdListPtr;
  final IntList childIdListPtr;
  final int NET_NODE_MARK_INIT;
  final int NET_NODE_MARK_DONE;
  final int NET_NODE_MARK_TEST;

  public NetNode() {
    mark = 0;
    parentIdListPtr = IntList.list_alloc(); 
    childIdListPtr = IntList.list_alloc();
    NET_NODE_MARK_INIT = 0;
    NET_NODE_MARK_DONE = 1;
    NET_NODE_MARK_TEST = 2;
  }

}
