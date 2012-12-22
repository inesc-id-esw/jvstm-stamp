package jstamp.jvstm;

import java.util.concurrent.Callable;

public abstract class CallableCollectAborts<T> implements Callable<T> {

    private int aborts = -1;
    
    @Override
    public T call() {
	this.aborts++;
	return runTx();
    }
    
    public abstract T runTx();

    public int getAborts() {
	return aborts;
    }

}
