package jstamp.jvstm.genome;

import jvstm.VBox;
import jvstm.VBoxInt;

  public class endInfoEntry {
      final VBox<Boolean> isEnd;
      final VBoxInt jumpToNext;
      
      public endInfoEntry() {
        isEnd = new VBox<Boolean>(false);
        jumpToNext = new VBoxInt(0);
      }
      public endInfoEntry(boolean myEnd, int myNext) {
        isEnd = new VBox<Boolean>(myEnd);
        jumpToNext = new VBoxInt(myNext);
      }
  }
