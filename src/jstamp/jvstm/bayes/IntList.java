package jstamp.jvstm.bayes;

import jvstm.VBoxInt;

/* =============================================================================
 *
 * Intlist.java
 * -- Sorted singly linked list
 * -- Options: -DLIST_NO_DUPLICATES (default: allow duplicates)
 *
 * =============================================================================
 *
 * Copyright (C) Stanford University, 2006.  All Rights Reserved.
 * Author: Chi Cao Minh
 *
 * Ported to Java June 2009, Alokika Dash
 * adash@uci.edu
 * University of California, Irvine
 *
 * =============================================================================
 *
 * For the license of bayes/sort.h and bayes/sort.c, please see the header
 * of the files.
 * 
 * ------------------------------------------------------------------------
 * 
 * Unless otherwise noted, the following license applies to STAMP files:
 * 
 * Copyright (c) 2007, Stanford University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 * 
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 * 
 *     * Neither the name of Stanford University nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY STANFORD UNIVERSITY ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL STANFORD UNIVERSITY BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * =============================================================================
 */

public class IntList {
    final public IntListNode head;
    public final VBoxInt size;

    public IntList() {
	this.head = new IntListNode(0);
	this.size = new VBoxInt(0);
    }

    public IntListNode
    allocNode (int dataPtr)
    {
	IntListNode nodePtr = new IntListNode(dataPtr);
	return nodePtr;
    }

    public static IntList list_alloc ()
    {
	IntList listPtr = new IntList();
	return listPtr;
    }


    /* =============================================================================
     * freeNode
     * =============================================================================
     */
    public void
    freeNode (IntListNode nodePtr)
    {
	nodePtr = null;
    }

    public int list_getSize () {
	return size.getInt();
    }

    public IntListNode findPrevious (int dataPtr) {
	IntListNode prevPtr = head;
	IntListNode nodePtr = prevPtr.nextPtr.get();

	for (; nodePtr != null; nodePtr = nodePtr.nextPtr.get()) {
	    if (compareId(nodePtr.dataPtr, dataPtr) >= 0) {
		return prevPtr;
	    }
	    prevPtr = nodePtr;
	}

	return prevPtr;
    }


    public boolean list_insert (int dataPtr) {
	IntListNode prevPtr;
	IntListNode nodePtr;
	IntListNode currPtr;

	prevPtr = findPrevious(dataPtr);
	currPtr = prevPtr.nextPtr.get();

	nodePtr = allocNode(dataPtr);

	nodePtr.nextPtr.put(currPtr);
	prevPtr.nextPtr.put(nodePtr);
	size.put(size.get() + 1);

	return true;
    }

    public static int compareId(int a, int b) {
	return (a - b);
    }

    public boolean list_remove (int dataPtr) {
	IntListNode prevPtr;
	IntListNode nodePtr;

	prevPtr = findPrevious(dataPtr);

	nodePtr = prevPtr.nextPtr.get();
	if ((nodePtr != null) &&
		(compareId(nodePtr.dataPtr, dataPtr) == 0))
	{
	    prevPtr.nextPtr.put(nodePtr.nextPtr.get());
	    nodePtr.nextPtr.put(null);
	    freeNode(nodePtr);
	    size.put(size.get() - 1);

	    return true;
	}

	return false;
    }


}