package gedi.util.algorithm.clustering.hierarchical;

import gedi.util.functions.Measure.MeasureType;

public class IndexHierarchicalClusterer extends HierarchicalClusterer<Integer> {

	
	public HierarchicalCluster<Integer> clusterDistances(double[][] distances) {
		return cluster(distances,MeasureType.Distance);
	}
	
	public HierarchicalCluster<Integer> clusterSimilarities(double[][] similarities) {
		return cluster(similarities,MeasureType.Similarity);
	}

	public HierarchicalCluster<Integer> cluster(double[][] matrix, MeasureType type) {
		return cluster(null,matrix,null,type, getMode().supportsIndices());
	}
	
}
