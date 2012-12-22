
package jstamp.jvstm.bayes;
/* =============================================================================
 *
 * list.java
 * -- Sorted singly linked list
 * -- Options: -DLIST_NO_DUPLICATES (default: allow duplicates)
 *
 * =============================================================================
 *
 * Copyright (C) Stanford University, 2006.  All Rights Reserved.
 * Author: Chi Cao Minh
 *
 * Ported to Java June 2009 Alokika Dash
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

public class List {
  final public ListNode head;
  
  public List() {
      this.head = new ListNode(null);
  }

  /* =============================================================================
   * compareTask
   * -- Want greatest score first
   * -- For list
   * =============================================================================
   */
  public static int
    compareTask (LearnerTask aPtr, LearnerTask bPtr)
    {
      LearnerTask aTaskPtr = (LearnerTask) aPtr;
      LearnerTask bTaskPtr = (LearnerTask) bPtr;
      float aScore = aTaskPtr.score;
      float bScore = bTaskPtr.score;

      if (aScore < bScore) {
        return 1;
      } else if (aScore > bScore) {
        return -1;
      } else {
        return (aTaskPtr.toId - bTaskPtr.toId);
      }
    }
  
  /* =============================================================================
   * list_iter_reset
   * =============================================================================
   */
  public void
    list_iter_reset (ListIter itPtr)
    {
      itPtr.ptr = head;
    }

  /* =============================================================================
   * list_iter_hasNext
   * =============================================================================
   */
  public boolean
    list_iter_hasNext (ListIter itPtr)
    {
      return itPtr.ptr.nextPtr.get() != null;
    }

  /* =============================================================================
   * list_iter_next
   * =============================================================================
   */
  public LearnerTask
    list_iter_next (ListIter itPtr)
    {
      LearnerTask lt=itPtr.ptr.dataPtr;
      itPtr.ptr = itPtr.ptr.nextPtr.get();
      return lt;
    }

  /* =============================================================================
   * allocNode
   * -- Returns null on failure
   * =============================================================================
   */
  public ListNode
    allocNode (LearnerTask dataPtr)
    {
      ListNode nodePtr = new ListNode(dataPtr);

      return nodePtr;
    }

  /* =============================================================================
   * list_alloc
   * -- If 'compare' function return null, compare data pointer addresses
   * -- Returns null on failure
   * =============================================================================
   */
  public static List list_alloc ()
    {
      List listPtr = new List();

      return listPtr;
    }

  public ListNode
    findPrevious (LearnerTask dataPtr)
    {
      ListNode prevPtr = head;
      ListNode nodePtr = prevPtr.nextPtr.get();

      for (; nodePtr != null; nodePtr = nodePtr.nextPtr.get()) {
        if (compareTask(nodePtr.dataPtr, dataPtr) >= 0) {
          return prevPtr;
        }
        prevPtr = nodePtr;
      }

      return prevPtr;
    }


  /* =============================================================================
   * list_insert
   * -- Return true on success, else false
   * =============================================================================
   */
  public boolean
    list_insert (LearnerTask dataPtr)
    {
      ListNode prevPtr;
      ListNode nodePtr;
      ListNode currPtr;

      prevPtr = findPrevious(dataPtr);
      currPtr = prevPtr.nextPtr.get();

      nodePtr = allocNode(dataPtr);

      nodePtr.nextPtr.put(currPtr);
      prevPtr.nextPtr.put(nodePtr);

      return true;
    }

  /* =============================================================================
   * list_remove
   * -- Returns true if successful, else false
   * =============================================================================
   */
  public boolean
    list_remove (LearnerTask dataPtr)
    {
      ListNode prevPtr;
      ListNode nodePtr;

      prevPtr = findPrevious(dataPtr);

      nodePtr = prevPtr.nextPtr.get();
      if ((nodePtr != null) &&
          (compareTask(nodePtr.dataPtr, dataPtr) == 0))
      {
        prevPtr.nextPtr.put(nodePtr.nextPtr.get());
        nodePtr.nextPtr.put(null);

        return true;
      }

      return false;
    }

}
