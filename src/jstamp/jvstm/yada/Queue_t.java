package jstamp.jvstm.yada;


/* =============================================================================
 *
 * queue.java
 *
 * =============================================================================
 *
 * Copyright (C) Stanford University, 2006.  All Rights Reserved.
 * Author: Chi Cao Minh
 *
 * Ported to Java
 * Author:Alokika Dash
 * University of California, Irvine
 *
 * =============================================================================
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


public class Queue_t {

    public static int QUEUE_GROWTH_FACTOR = 2;

    private Element[] elements;
    private int pop; /* points before element to pop */
    private int push;
    private int capacity;

    /* =============================================================================
     * queue_alloc
     * =============================================================================
     */
    public Queue_t (int initCapacity) {
	int capacity = ((initCapacity < 2) ? 2 : initCapacity);
	elements = new Element[capacity];
	pop = capacity - 1;
	push = 0;
	this.capacity = capacity;
    }

    /* =============================================================================
     * queue_isEmpty
     * =============================================================================
     */
    public boolean queue_isEmpty () {
	return (((pop + 1) % capacity == push) ? true : false);
    }


    /* =============================================================================
     * queue_clear
     * =============================================================================
     */
    public void queue_clear () {
	pop = capacity - 1;
	push = 0;
    }

    /* =============================================================================
     * queue_push
     * =============================================================================
     */
    public boolean queue_push (Element dataPtr) {
	int newPush = (push + 1) % capacity;
	if (newPush == pop) {

	    int newCapacity = capacity * QUEUE_GROWTH_FACTOR;
	    Element[] newElements = new Element[(int) newCapacity];

	    int dst = 0;
	    if (pop < push) {
		int src;
		for (src = (pop + 1); src < push; src++, dst++) {
		    newElements[dst] = elements[src];
		}
	    } else {
		int src;
		for (src = (pop + 1); src < capacity; src++, dst++) {
		    newElements[dst] = elements[src];
		}
		for (src = 0; src < push; src++, dst++) {
		    newElements[dst] = elements[src];
		}
	    }

	    elements = newElements;
	    pop = newCapacity - 1;
	    capacity = newCapacity;
	    push = dst;
	    newPush = push + 1; /* no need modulo */
	}

	elements[push] = dataPtr;
	push = newPush;
	return true;
    }

    /* =============================================================================
     * queue_pop
     * =============================================================================
     */
    public Element queue_pop () {

	int newPop = (pop + 1) % capacity;
	if (newPop == push) {
	    return null;
	}

	Element dataPtr = elements[newPop];
	pop = newPop;

	return dataPtr;
    }

    void queue_shuffle (Random randomPtr) {

	int numElement;
	if (pop < push) {
	    numElement = push - (pop + 1);
	} else {
	    numElement = capacity - (pop - push + 1);
	}

	int base = pop + 1;
	for (int i = 0; i < numElement; i++) {
	    int r1 = randomPtr.random_generate() % numElement;
	    int r2 = randomPtr.random_generate() % numElement;
	    int i1 = (base + r1) % capacity;
	    int i2 = (base + r2) % capacity;
	    Element tmp = elements[i1];
	    elements[i1] = elements[i2];
	    elements[i2] = tmp;
	}
    }
}
/* =============================================================================
 *
 * End of queue.java
 *
 * =============================================================================
 */