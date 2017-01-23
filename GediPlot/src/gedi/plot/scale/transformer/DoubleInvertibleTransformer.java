package gedi.plot.scale.transformer;

import gedi.util.datastructure.dataframe.DataColumn;
import jdistlib.disttest.NormalityTest;

public interface DoubleInvertibleTransformer {

	double transform(double v);
	double inverse(double v);
	String formatTransformed(double v, int digits);
	
	public static DoubleInvertibleTransformer identity = new DoubleInvertibleTransformer() {
		@Override
		public double transform(double v) {
			return v;
		}
		@Override
		public double inverse(double v) {
			return v;
		}
		@Override
		public String formatTransformed(double v, int digits) {
			return String.format("%."+digits+"f", v);
		}
	};

	public static DoubleInvertibleTransformer log10 = new DoubleInvertibleTransformer() {
		@Override
		public double transform(double v) {
			return Math.log10(v);
		}
		@Override
		public double inverse(double v) {
			return Math.pow(10, v);
		}
		@Override
		public String formatTransformed(double v, int digits) {
			return "$10^{"+String.format("%."+digits+"f", v)+"}$";
		}
	};

	public static DoubleInvertibleTransformer log2 = new DoubleInvertibleTransformer() {
		@Override
		public double transform(double v) {
			return Math.log(v)/Math.log(2);
		}
		@Override
		public double inverse(double v) {
			return Math.pow(2,v);
		}
		@Override
		public String formatTransformed(double v, int digits) {
			return "$2^{"+String.format("%."+digits+"f", v)+"}$";
		}
	};

	public static DoubleInvertibleTransformer iscale(double min, double max) {
		return new DoubleInvertibleTransformer() {
			@Override
			public double transform(double v) {
				return (1-v)*min+v*max;
			}
			@Override
			public double inverse(double v) {
				return (v-min)/(max-min);
			}
			@Override
			public String formatTransformed(double v, int digits) {
				return String.format("%."+digits+"f", inverse(v));
			}
		};
	}
	
	public static DoubleInvertibleTransformer scale(double min, double max) {
		return new DoubleInvertibleTransformer() {
			@Override
			public double inverse(double v) {
				return (1-v)*min+v*max;
			}
			@Override
			public double transform(double v) {
				return (v-min)/(max-min);
			}
			@Override
			public String formatTransformed(double v, int digits) {
				return String.format("%."+digits+"f", transform(v));
			}
		};
	}
	
	public static DoubleInvertibleTransformer margin(double min, double max, double margin) {
		double range = Math.abs(max-min);
		return new DoubleInvertibleTransformer() {
			@Override
			public double inverse(double v) {
				return ((v-Math.min(max, min)-range*margin)/(1-2*margin))+Math.min(max, min);
			}
			@Override
			public double transform(double v) {
				return Math.min(max, min)+range*margin+(1-2*margin)*(v-Math.min(max, min));
			}
			@Override
			public String formatTransformed(double v, int digits) {
				return String.format("%."+digits+"f", transform(v));
			}
		};
	}
	

}
