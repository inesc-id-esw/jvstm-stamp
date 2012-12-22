package jstamp.jvstm.intruder;

public class Decoded {
	final int flowId;
	final byte[] data;

	public Decoded(int flowId, byte[] data) {
	    this.flowId = flowId;
	    this.data = data;
	}
}
