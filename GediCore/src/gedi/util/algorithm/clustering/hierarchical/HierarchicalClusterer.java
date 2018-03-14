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
package gedi.util.algorithm.clustering.hierarchical;

import java.util.ArrayList;

import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.DistanceMeasure;
import gedi.util.functions.Measure;
import gedi.util.functions.Measure.MeasureType;
import gedi.util.functions.SimilarityMeasure;


/**
 * Clustering in O(n^2) time and O(n^2) space
 * <p>
 * see Murtagh, F., "Complexities of hierarchic clustering algorithms: state of the art", Computational Statistics Quarterly, 1, 101-113, 1984.
 *  
 * @author erhard
 *
 */
public class HierarchicalClusterer<C> {
	
	
	public enum ClusteringMode {
		SingleLinkage {

			@Override
			public void mergeDistance(double[][] matrix,
					int index1, int index2, IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						matrix[index1][i] = matrix[i][index1] = Math.min(matrix[index1][i], matrix[index2][i]);
			}

			@Override
			public void mergeSimilarity(double[][] matrix, int index1,
					int index2, IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						matrix[index1][i] = matrix[i][index1] = Math.max(matrix[index1][i], matrix[index2][i]);
			}
			
			
			@Override
			public void mergeDistance(double[][] matrix, int[][] mergeIndices,
					int index1, int index2, IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						if (matrix[index2][i]<matrix[index1][i]) {
							matrix[index1][i] = matrix[i][index1] = matrix[index2][i];
							mergeIndices[index1][i] = mergeIndices[index2][i];
						}
			}

			@Override
			public void mergeSimilarity(double[][] matrix, int[][] mergeIndices, int index1,
					int index2, IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						if (matrix[index2][i]>matrix[index1][i]) {
							matrix[index1][i] = matrix[i][index1] = matrix[index2][i];
							mergeIndices[index1][i] = mergeIndices[index2][i];
						}
			}
			
			public boolean supportsIndices() {
				return true;
			}
			
		},
		CompleteLinkage {
			@Override
			public void mergeDistance(double[][] matrix, int index1, int index2,
					IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						matrix[index1][i] = matrix[i][index1] = Math.max(matrix[index1][i], matrix[index2][i]);

			}

			@Override
			public void mergeSimilarity(double[][] matrix, int index1, int index2,
					IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						matrix[index1][i] = matrix[i][index1] = Math.min(matrix[index1][i], matrix[index2][i]);
			}
			
			@Override
			public void mergeDistance(double[][] matrix, int[][] mergeIndices,
					int index1, int index2, IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						if (matrix[index2][i]>matrix[index1][i]) {
							matrix[index1][i] = matrix[i][index1] = matrix[index2][i];
							mergeIndices[index1][i] = mergeIndices[index2][i];
						}
			}

			@Override
			public void mergeSimilarity(double[][] matrix, int[][] mergeIndices, int index1,
					int index2, IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						if (matrix[index2][i]<matrix[index1][i]) {
							matrix[index1][i] = matrix[i][index1] = matrix[index2][i];
							mergeIndices[index1][i] = mergeIndices[index2][i];
						}
			}
			
			public boolean supportsIndices() {
				return true;
			}
		},
		UPGMA {
			@Override
			public void mergeDistance(double[][] matrix, int index1, int index2,
					IntArrayList[] clusterIndices) {
				int N = matrix.length;
				double w1 = clusterIndices[index1].size() / (double)(clusterIndices[index1].size()+clusterIndices[index2].size());
				double w2 = clusterIndices[index2].size() / (double)(clusterIndices[index1].size()+clusterIndices[index2].size());
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						matrix[index1][i] = matrix[i][index1] =  w1*matrix[index1][i] + w2*matrix[index2][i];
				
			}

			@Override
			public void mergeSimilarity(double[][] matrix, int index1, int index2,
					IntArrayList[] clusterIndices) {
				int N = matrix.length;
				double w1 = clusterIndices[index1].size() / (double)(clusterIndices[index1].size()+clusterIndices[index2].size());
				double w2 = clusterIndices[index2].size() / (double)(clusterIndices[index1].size()+clusterIndices[index2].size());
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						matrix[index1][i] = matrix[i][index1] =  w1*matrix[index1][i] + w2*matrix[index2][i];
				
			}
		},
		WPGMA {
			@Override
			public void mergeDistance(double[][] matrix, int index1, int index2,
					IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						matrix[index1][i] = matrix[i][index1] =  .5f*matrix[index1][i] + .5f*matrix[index2][i];
				
			}

			@Override
			public void mergeSimilarity(double[][] matrix, int index1, int index2,
					IntArrayList[] clusterIndices) {
				int N = matrix.length;
				for (int i=0; i<N; i++) 
					if (clusterIndices[i]!=null && i!=index1 && i!=index2) 
						matrix[index1][i] = matrix[i][index1] =  .5f*matrix[index1][i] + .5f*matrix[index2][i];
				
			}
		};
		
		
		public abstract void mergeDistance(double[][] matrix, int index1, int index2,
				IntArrayList[] clusterIndices);
		public abstract void mergeSimilarity(double[][] matrix, int index1, int index2,
				IntArrayList[] clusterIndices);
		
		public void mergeDistance(double[][] matrix, int[][] mergeIndices, int index1, int index2,
				IntArrayList[] clusterIndices){
			throw new UnsupportedOperationException();
		}
		public void mergeSimilarity(double[][] matrix, int[][] mergeIndices, int index1, int index2,
				IntArrayList[] clusterIndices){
			throw new UnsupportedOperationException();
		}
		
		public boolean supportsIndices() {
			return false;
		}

	}
	

	protected ClusteringMode mode = ClusteringMode.SingleLinkage;
	protected int representative;
	protected IntArrayList clusteredIndices;
	protected double[][] matrix;
	
	
	public void setMode(ClusteringMode mode) {
		this.mode = mode;
	}
	
	public ClusteringMode getMode() {
		return mode;
	}
	
	public HierarchicalCluster<C> cluster(C[] a, Measure<C> measure) {
		return cluster(a, measure.createMatrix(a), measure.type());
	}
	
	public HierarchicalCluster<C> clusterDistances(C[] a, double[][] distances) {
		return cluster(a,distances,MeasureType.Distance);
	}
	
	public HierarchicalCluster<C> clusterSimilarities(C[] a, double[][] similarities) {
		return cluster(a,similarities,MeasureType.Similarity);
	}

	public HierarchicalCluster<C> cluster(C[] a, double[][] matrix, MeasureType type) {
		return cluster(a,matrix,null,type);
	}
	

	public double[][] getMatrix() {
		return matrix;
	}
	
	/**
	 * Alters the matrix!! toCluster may be null, a may be null (when C is Integer), length may be -1
	 * @param a
	 * @param matrix
	 * @param distanceFlag
	 * @return
	 */
	public HierarchicalCluster<C> cluster(C[] a, double[][] matrix, int[] toCluster, MeasureType type) {
		return cluster(a,matrix,toCluster,type, getMode().supportsIndices());
	}
	
	protected int[][] mergeIndices;
	public HierarchicalCluster<C> cluster(C[] a, double[][] matrix, int[] toCluster, MeasureType type, boolean createIndices) {
		mergeIndices = null;
		int n = matrix.length;
		if (createIndices) {
			mergeIndices = new int[n][n];
			for (int i=0; i<n; i++)
				for (int j=0; j<n; j++)
					mergeIndices[i][j] = i;
		}
		
		return cluster(a,matrix,this.mergeIndices,toCluster,type);
	}
	
	@SuppressWarnings("unchecked")
	public HierarchicalCluster<C> cluster(C[] a, double[][] matrix, int[][] mergeIndices, int[] toCluster, MeasureType type) {
		this.matrix = matrix;
		this.clusteredIndices = null;
		this.representative = 0;
		if (toCluster!=null && toCluster.length==0)
			return null;
		
		
		final int N = matrix.length;
		
		IntArrayList[] clusterIndices = new IntArrayList[N];
		HierarchicalCluster<C>[] cluster = new HierarchicalCluster[N]; 
		if (toCluster==null)
			for (int i=0; i<N; i++) {
				clusterIndices[i] = new IntArrayList();
				clusterIndices[i].add(i);
				cluster[i] = new HierarchicalCluster<C>(a==null?(C)new Integer(i):a[i]);
				fireMergeEvent(cluster[i],-1,-1);
			}
		else
			for (int i : toCluster) {
				clusterIndices[i] = new IntArrayList();
				clusterIndices[i].add(i);
				cluster[i] = new HierarchicalCluster<C>(a==null?(C)new Integer(i):a[i]);
				fireMergeEvent(cluster[i],-1,-1);
			}
		
		
		// start
		int C = toCluster==null?N:toCluster.length;
		
		IntArrayList chain = new IntArrayList(N);
		chain.add(toCluster==null?0:toCluster[0]);
		
		while (C > 1) {
//			System.out.println(chain);
//			System.out.println("To cluster: "+C);
			
			// find NN
			int best=-1;
			double val;
			if (type==MeasureType.Distance){
				val = Double.POSITIVE_INFINITY;
				int last = chain.getLastInt();
				for (int i=0; i<N; i++) {
					if (clusterIndices[i] != null && i!=last) {
						if (matrix[last][i] < val) {
							val = matrix[last][i];
							best = i;
						}
					}
				}
			} else {
				val = Double.NEGATIVE_INFINITY;
				int last = chain.getLastInt();
				for (int i=0; i<N; i++) {
					if (clusterIndices[i] != null && i!=last) {
						if (matrix[last][i] > val) {
							val = matrix[last][i];
							best = i;
						}
					}
				}
			}
			
			// is RNN?
			if (chain.size()>=2 && best==chain.getLastInt(1)) {
				int index2 = chain.removeLast();
				int index1 = chain.removeLast();
				
				int nindex1 = -1;
				int nindex2 = -1;
				
				if (mergeIndices!=null) {
					nindex1 = mergeIndices[index1][index2];
					nindex2 = mergeIndices[index2][index1];
					
					if (type==MeasureType.Distance)
						mode.mergeDistance(matrix, mergeIndices, index1, index2, clusterIndices);
					else
						mode.mergeSimilarity(matrix, mergeIndices, index1, index2, clusterIndices);
				} else {
					if (type==MeasureType.Distance)
						mode.mergeDistance(matrix, index1, index2, clusterIndices);
					else
						mode.mergeSimilarity(matrix, index1, index2, clusterIndices);
				}
				
				clusterIndices[index1].addAll(clusterIndices[index2]);
				clusterIndices[index2] = null;
				cluster[index1] = new HierarchicalCluster<C>(cluster[index1],cluster[index2],val);
				cluster[index2] = null;
				
				fireMergeEvent(cluster[index1],nindex1,nindex2);
				
//				System.out.println(ArrayUtils.matrixToString(sim));
//				System.out.printf("Merged %s and %s\n",a==null?index1+"":a[index1],a==null?index2+"":a[index2]);
				
				if (chain.isEmpty()) {
					int arbi;
					for (arbi=0;cluster[arbi]==null; arbi++);
					chain.add(arbi);
				}
				C--; 
				representative = index1;
				
			} else {
				chain.add(best);
			}
		}
		this.clusteredIndices = clusterIndices[representative];
		return cluster[representative];
	}

	
	private ArrayList<MergeListener<C>> listeners = new ArrayList<MergeListener<C>>(); 
	private void fireMergeEvent(HierarchicalCluster<C> newCluster, int index1, int index2) {
		for (MergeListener<C> l : listeners)
			l.encounter(newCluster,index1,index2);
	}
	
	public void addMergeListener(MergeListener<C> l) {
		listeners.add(l);
	}
	
	public void removeMergeListener(MergeListener<C> l) {
		listeners.remove(l);
	}

	
	public interface MergeListener<C> {
		/**
		 * Fired for each initial singleton cluster and then for each merge event!
		 * @param newCluster
		 */
		void encounter(HierarchicalCluster<C> newCluster, int index1, int index2);
	}
	
	
}
