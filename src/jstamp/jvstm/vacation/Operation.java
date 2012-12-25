package jstamp.jvstm.vacation;

public abstract class Operation {

    public static boolean nestedParallelismOn;
    public static int numberParallelSiblings;
    public static boolean parallelizeUpdateTables;
    public static boolean usePerTxBoxes = false;

    public abstract void doOperation();

}
