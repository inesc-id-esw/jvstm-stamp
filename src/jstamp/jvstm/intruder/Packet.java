package jstamp.jvstm.intruder;

public class Packet {
    final int flowId;
    final int fragmentId;
    final int numFragment;
    final int length;
    final byte[] data;

    public Packet(int numDataBytes, int numPacket, int flowId, int fragmentId) {
	data = new byte[numDataBytes];
	this.length = numDataBytes;
	this.numFragment = numPacket;
	this.flowId = flowId;
	this.fragmentId = fragmentId;
    }

    public static int compareFlowID(Packet aPtr, Packet bPtr) {
	return aPtr.flowId - bPtr.flowId;
    }

    public static int compareFragmentID(Packet aPtr, Packet bPtr) {
	return aPtr.fragmentId - bPtr.fragmentId;
    }
}
