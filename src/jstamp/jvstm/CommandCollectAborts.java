package jstamp.jvstm;

import jvstm.TransactionalCommand;

public abstract class CommandCollectAborts implements TransactionalCommand {

    private int aborts = -1;
    
    @Override
    public void doIt() {
	this.aborts++;
	runTx();
    }
    
    public abstract void runTx();

    public int getAborts() {
	return aborts;
    }

}
