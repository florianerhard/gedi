package gedi.centeredDiskIntervalTree;

import java.io.IOException;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.FixedSizeBinarySerializable;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class CenteredDiskIntervalTreeNode implements BinarySerializable {

	private int node;
	long ptr;

	public CenteredDiskIntervalTreeNode() {
	}
	
	public CenteredDiskIntervalTreeNode(int node, long ptr) {
		this.node = node;
		this.ptr = ptr;
	}

	public int getNode() {
		return node;
	}
	
	public long getPtr() {
		return ptr;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(node);
		out.putCLong(ptr);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		node = in.getCInt();
		ptr = in.getCLong();
	}

	
	@Override
	public String toString() {
		return node+"->"+ptr;
	}
}
