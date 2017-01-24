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

package gedi.ambiguity.clustering;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.mutable.MutableMonad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClusterInfo implements BinarySerializable {

	public static int[][] empty = new int[0][0];
	
	
	private int totalUniqueMappingReadCount;
	private double totalReadCountDivided;
	private int totalReadCountSum;
	private int regionCount;
	
	/**
	 * Oriented in genomic direction (NOT 5' to 3')
	 */
	/**
	 * index,0 -> pos(lastBaseInExon)
	 * index,1 -> pos(firstBaseInOtherExon)
	 * index,2 -> count
	 * sorted by 0 and then by 1
	 */
	/**
	 * Start is last base of exon, end is first base of other exon
	 */
	private IntervalTree<SimpleInterval, Double> splits = new  IntervalTree<SimpleInterval, Double>(null);
	
	/**
	 * index,0->pos; index,1->count
	 * sorted by 0
	 */
	private int[] neighborsIndex;
	private double[] neighborsCount;
	
	public ClusterInfo(int regionCount, double totalReadCountDivided, int totalReadCountSum, int totalUniqueMappingReadCount) {
		this.regionCount = regionCount;
		this.totalReadCountDivided = totalReadCountDivided;
		this.totalReadCountSum = totalReadCountSum;
		this.totalUniqueMappingReadCount = totalUniqueMappingReadCount;
	}
	
	
	public void setCounts(int[] neighborsIndex, double[] neigborsCount, int[] splitCountIndex1, int[] splitCountIndex2, double[] splitCount) {
		this.neighborsIndex = neighborsIndex;
		this.neighborsCount = neigborsCount;
		for (int i=0; i<splitCount.length; i++)
			splits.put(new SimpleInterval(splitCountIndex1[i], splitCountIndex2[i]-1), splitCount[i]);
	}
	
	public double[] getContextData(GenomicRegion reg, int context) {
		ArrayGenomicRegion down = new ArrayGenomicRegion(reg.getEnd(),reg.getEnd()+context);
		double[] rd = new double[context-1];
		updateContext(down, down.getStart(), 1, rd);
		
		ArrayGenomicRegion up = new ArrayGenomicRegion(reg.getStart()-context,reg.getStart());
		double[] ru = new double[context-1];
		updateContext(up, up.getStop(), -1, ru);
		
		return ArrayUtils.concat(ru,rd);
	}
	
	/**
	 * pos is first/last position to consider, in genomic space
	 * @param context
	 * @param pos
	 * @param direction
	 * @param current
	 */
	public void updateContext(ArrayGenomicRegion context, int pos, int direction, double[] current) {
		double[] c = getNeighborData(context);
		if (ArrayUtils.median(c.clone())>ArrayUtils.median(current.clone())) 
			System.arraycopy(c, 0, current, 0, current.length);
		
		// look right or left from pos for splits within context and call recursively
		if (direction==1) {
			for (SimpleInterval intron : splits.getIntervalsIntersecting(pos, context.getStop(), new ArrayList<SimpleInterval>()))  {
				if (intron.getStart()+1>=pos && intron.getStart()+1<context.getEnd())
					updateContext(
							context
							.subtract(new ArrayGenomicRegion(intron.getStart()+1, context.getEnd()))
							.union(new ArrayGenomicRegion(intron.getEnd(),intron.getEnd()+context.getEnd()-(intron.getStart()+1))),
							intron.getEnd(), direction, current);
			}
		} else {
			for (SimpleInterval intron : splits.getIntervalsIntersecting(context.getStart(), pos, new ArrayList<SimpleInterval>()))  {
				if (intron.getStop()<=pos && intron.getStop()>=context.getStart())
					updateContext(
							context
							.subtract(new ArrayGenomicRegion(context.getStart(),intron.getEnd()))
							.union(new ArrayGenomicRegion(intron.getStart()+1-(intron.getEnd()-context.getStart()),intron.getStart()+1)),
							intron.getStart(), direction, current);
			}
		}
	}
	
	public double[] getNeighborData(GenomicRegion reg) {
		double[] re = new double[reg.getTotalLength()-1];
		List<SimpleInterval> singleton = new MutableMonad<SimpleInterval>().asMonadList();  
		
		int l = reg.getStart();
		for (int i=0; i<re.length; i++) {
			int p = reg.map(i+1); // p is in genomic-space
			// determine the count for l->p
			if (l+1==p) {
				// no split, look into neighbors
				int index = Arrays.binarySearch(neighborsIndex,l);
				if (index<0) index = -index-2;
				if (index>=0 && index<neighborsIndex.length) {
					re[i] = neighborsCount[index];
				}
			} else {
				splits.getIntervalsEqual(l, p-1, singleton);
				if (!singleton.isEmpty()) {
					re[i] = splits.get(singleton.get(0));
					singleton.clear();
				}
			}
			
			l = p;
		}
		
		return re;
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
		

		out.putCInt(neighborsCount.length);
		for (int i=0; i<neighborsCount.length; i++) {
			out.putCInt(neighborsIndex[i]);
			out.putDouble(neighborsCount[i]);
		}
		
		out.putCInt(splits.size());
		for (SimpleInterval s : splits.keySet()) {
			out.putCInt(s.getStart());
			out.putCInt(s.getStop()+1);
			out.putDouble(splits.get(s));
		}
		
		
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		totalUniqueMappingReadCount = in.getCInt();
		totalReadCountDivided = in.getDouble();
		totalReadCountSum = in.getCInt();
		regionCount = in.getCInt();
		
		neighborsIndex = new int[in.getCInt()];
		neighborsCount = new double[neighborsIndex.length];
		for (int i=0; i<neighborsIndex.length; i++) {
			neighborsIndex[i] = in.getCInt();
			neighborsCount[i] = in.getDouble();
		}
		int size = in.getCInt();
		splits = new IntervalTree<SimpleInterval, Double>(null);
		for (int i=0; i<size; i++) {
			splits.put(new SimpleInterval(in.getCInt(),in.getCInt()-1), in.getDouble());
		}
		
	}

	@Override
	public String toString() {
		return "ClusterInfo [totalUniqueMappingReadCount="
				+ totalUniqueMappingReadCount + ", totalReadCountDivided="
				+ totalReadCountDivided + ", totalReadCountSum="
				+ totalReadCountSum + ", regionCount=" + regionCount + "]";
	}
	
	

}
