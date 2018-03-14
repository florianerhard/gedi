/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
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
