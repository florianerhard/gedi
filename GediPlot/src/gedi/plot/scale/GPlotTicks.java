package gedi.plot.scale;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

public class GPlotTicks {

	private String[] labels;
	private double[] preValues;
	private DoubleUnaryOperator transformer;

	public GPlotTicks(String[] labels, double[] preValues, DoubleUnaryOperator transformer) {
		this.labels = labels;
		this.preValues = preValues;
		this.transformer = transformer;
	}
	
	public int count() {
		return labels.length;
	}
	

	public String label(int index) {
		return labels[index];
	}
	
	public double pre(int index) {
		return preValues[index];
	}
	
	public double transformed(int index) {
		return transformer.applyAsDouble(preValues[index]);
	}
	
	public double subtransformed(int index) {
		if (count()<2) return Double.NaN;
		if (index==0)
			return transformed(0)-(transformed(1)-transformed(0))/2;
		if (index==count())
			return transformed(count()-1)+(transformed(count()-1)-transformed(count()-2))/2;
		return (transformed(index-1)+transformed(index))/2;
	}
	
	public double[] allsubs() {
		double[] re = new double[labels.length+1];
		for (int i=0; i<re.length; i++)
			re[i] = subtransformed(i);
		return re;
	}
	
}
