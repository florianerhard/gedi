package gedi.util.functions;

import java.util.function.Function;



public interface SimilarityMeasure<C> extends Measure<C> {

	@Override
	default MeasureType type() {
		return MeasureType.Similarity;
	}

	public static SimilarityMeasure<double[]> BHATTACHARYYA_COEFFICIENT = (a,b)->{
		double co = 0;
		for (int i=0; i<a.length; i++)
			co+=Math.sqrt(a[i]*b[i]);
		return co;
	};
	
	
	default <O> SimilarityMeasure<O> adapt(Function<O,C> map) {
		return new SimilarityMeasure<O>() {
			@Override
			public double applyAsDouble(O t, O u) {
				return SimilarityMeasure.this.applyAsDouble(map.apply(t),map.apply(u));
			}
		};
	}
	
}
