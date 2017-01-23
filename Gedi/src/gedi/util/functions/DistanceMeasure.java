package gedi.util.functions;

import java.util.function.Function;



public interface DistanceMeasure<C> extends Measure<C> {

	
	@Override
	default MeasureType type() {
		return MeasureType.Distance;
	}
	
	public static DistanceMeasure<double[]> MANHATTAN = (a,b)->{
		double re = 0;
		for (int i=0; i<a.length; i++)
			re+=Math.abs(a[i]-b[i]);
		return re;
	};
	
	public static DistanceMeasure<double[]> L2 = (a,b)->{
		double re = 0;
		for (int i=0; i<a.length; i++){
			double d =a[i]-b[i];
			re+=d*d;
		}
		return Math.sqrt(re);
	};
	
	public static DistanceMeasure<double[]> BHATTACHARYYA = (a,b)->{
		double co = 0;
		for (int i=0; i<a.length; i++)
			co+=Math.sqrt(a[i]*b[i]);
		return -Math.log(co);
	};
	
	
	default <O> DistanceMeasure<O> adapt(Function<O,C> map) {
		return new DistanceMeasure<O>() {
			@Override
			public double applyAsDouble(O t, O u) {
				return DistanceMeasure.this.applyAsDouble(map.apply(t),map.apply(u));
			}
		};
	}
	
}
