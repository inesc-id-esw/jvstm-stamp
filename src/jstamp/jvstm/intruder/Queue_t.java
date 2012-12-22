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

package jstamp.jvstm.intruder;

import java.util.Iterator;

public class Queue_t {
    final VQueue queue;

    public Queue_t(int initCapacity) {
	this.queue = new VQueue();
    }

    public void queue_clear () {
	this.queue.clear();
    }

    public boolean queue_push (Object dataPtr) {
	return this.queue.add(dataPtr);
    }

    public Object queue_pop () {
	return this.queue.poll();
    }

    public void queue_shuffle (Random randomPtr) {
	int size = this.queue.size();
	Object[] elements = new Object[size];
	Iterator it = this.queue.iterator();
	for (int i = 0; i < size; i++) {
	    elements[i] = it.next();
	}
	
	for (int i = 0; i < size; i++) {
	    int r1 = (int) (randomPtr.random_generate() % size);
	    int r2 = (int) (randomPtr.random_generate() % size);
	    Object tmp = elements[r1];
	    elements[r1] = elements[r2];
	    elements[r2] = tmp;
	}
	
	this.queue.clear();
	for (Object el : elements) {
	    this.queue.add(el);
	}
    }

}
