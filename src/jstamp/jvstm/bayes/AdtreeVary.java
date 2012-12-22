package jstamp.jvstm.bayes;
/**
 * Author: Alokika Dash
 * University of California, Irvine
 * adash@uci.edu
 *
 * - Helper class of Adtree.java
 **/

public class AdtreeVary {
    final int index;
    final int mostCommonValue;
    final AdtreeNode zeroNodePtr;
    final AdtreeNode oneNodePtr;

    public AdtreeVary(int index, int mostCommonValue, AdtreeNode zeroNodePtr, AdtreeNode oneNodePtr) {
	this.index = index;
	this.mostCommonValue = mostCommonValue;
	this.zeroNodePtr = zeroNodePtr;
	this.oneNodePtr = oneNodePtr;
    }

}
