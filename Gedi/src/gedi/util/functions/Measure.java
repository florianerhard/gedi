package gedi.util.functions;

import java.util.function.ToDoubleBiFunction;



public interface Measure<C> extends ToDoubleBiFunction<C, C> {

	
	public enum MeasureType {
		Similarity,Distance, Unknown
	}
	
	default MeasureType type() {
		return MeasureType.Unknown;
	}
	
	default double[][] createMatrix(C[] a) {
		return createMatrix(a,null);
	}
	default double[][] createMatrix(C[] a, double[][] re) {
		if (re==null || re.length!=a.length || re[0].length!=a.length)
			re = new double[a.length][a.length];
		for (int i=0; i<a.length; i++) 
			for (int j=i+1; j<a.length; j++) 
				re[j][i] = re[i][j] = applyAsDouble(a[i], a[j]);
		return re;
	}
	
	
}
