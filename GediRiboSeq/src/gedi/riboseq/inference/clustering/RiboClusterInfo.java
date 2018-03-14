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
package gedi.riboseq.inference.clustering;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;

public class RiboClusterInfo implements BinarySerializable {

	private int totalUniqueMappingReadCount;
	private double totalReadCountDivided;
	private int totalReadCountSum;
	private int regionCount;

	public RiboClusterInfo(int regionCount, double totalReadCountDivided, int totalReadCountSum, int totalUniqueMappingReadCount) {
		this.regionCount = regionCount;
		this.totalReadCountDivided = totalReadCountDivided;
		this.totalReadCountSum = totalReadCountSum;
		this.totalUniqueMappingReadCount = totalUniqueMappingReadCount;
	}
	
	


	public int getRegionCount() {
		return regionCount;
	}

	public double getTotalReadCountDivided() {
		return totalReadCountDivided;
	}
	
	public int getTotalReadCountSum() {
		return totalReadCountSum;
	}
	
	public int getTotalUniqueMappingReadCount() {
		return totalUniqueMappingReadCount;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(totalUniqueMappingReadCount);
		out.putDouble(totalReadCountDivided);
		out.putCInt(totalReadCountSum);
		out.putCInt(regionCount);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		totalUniqueMappingReadCount = in.getCInt();
		totalReadCountDivided = in.getDouble();
		totalReadCountSum = in.getCInt();
		regionCount = in.getCInt();
	}

	@Override
	public String toString() {
		return "ClusterInfo [totalUniqueMappingReadCount="
				+ totalUniqueMappingReadCount + ", totalReadCountDivided="
				+ totalReadCountDivided + ", totalReadCountSum="
				+ totalReadCountSum + ", regionCount=" + regionCount + "]";
	}
	
}
