package jstamp.jvstm.matrix;

import java.util.concurrent.atomic.AtomicInteger;

import jstamp.jvstm.CommandCollectAborts;
import jvstm.Transaction;
import jvstm.TransactionalCommand;

public class Matrix extends Thread{
    final MMul mmul;
    public final int x0;
    public final int y0;
    public final int x1;
    public final int y1;
    public Matrix(MMul mmul, int x0, int x1, int y0, int y1) {
	this.mmul = mmul;
	this.x0 = x0;
	this.y0 = y0;
	this.x1 = x1;
	this.y1 = y1;
    }

    // @Atomic
    public void run() {
	CommandCollectAborts cmd = new CommandCollectAborts() {
	    public void runTx() {
		int M = mmul.M;
		for(int i = x0; i< x1; i++){
		    for (int j = y0; j < y1; j++) {
			double innerProduct=0;
			for(int k = 0; k < M; k++) {
			    innerProduct += mmul.a[i][k] *mmul.btranspose[j][k];
			}
			mmul.c[i][j].put(innerProduct);
		    }
		}
	    }
	};
	Transaction.transactionallyDo(cmd);
    }

    public static void main(String[] args) {
	int NUM_THREADS = 4;
	int SIZE=600;
	if (args.length>0) {
	    NUM_THREADS=Integer.parseInt(args[0]);
	    if (args.length>1)
		SIZE=Integer.parseInt(args[1]);
	}

	int p, q, r;
	Matrix[] mm;
	Matrix tmp;
	MMul matrix;
Transaction.beginInevitable();
	matrix = new MMul(SIZE, SIZE, SIZE);
	matrix.setValues();
	matrix.transpose();
	mm = new Matrix[NUM_THREADS];
	int increment=SIZE/NUM_THREADS;
	int base=0;
	for(int i=0;i<NUM_THREADS;i++) {
	    if ((i+1)==NUM_THREADS)
		mm[i]=new Matrix(matrix,base, SIZE, 0, SIZE);
	    else
		mm[i]=new Matrix(matrix,base, base+increment, 0, SIZE);
	    base+=increment;
	}
	p = matrix.L;
	q = matrix.M;
	r = matrix.N;

	// print out the matrices to be multiplied
//	System.out.print("\n");
//	System.out.print("MatrixMultiply: L=");
//	System.out.print(p);
//	System.out.print("\t");
//	System.out.print("M=");
//	System.out.print(q);
//	System.out.print("\t");
//	System.out.print("N=");
//	System.out.print(r);
//	System.out.print("\n");

	Transaction.commit();
	
	long start=System.currentTimeMillis();

	// start a thread to compute each c[l,n]
	for (int i = 0; i < NUM_THREADS; i++) {
	    tmp = mm[i];
	    tmp.start();
	}


	// wait for them to finish
	for (int i = 0; i < NUM_THREADS; i++) {
	    tmp = mm[i];
	    try {
		tmp.join();
	    } catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}

	long stop=System.currentTimeMillis();

	// print out the result of the matrix multiply

//	System.out.print("Finished\n");
	long diff=stop-start;
	System.out.println(diff);
    }
}


