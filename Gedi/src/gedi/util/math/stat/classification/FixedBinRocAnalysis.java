package gedi.util.math.stat.classification;

import gedi.util.ArrayUtils;

import java.util.Arrays;


public class FixedBinRocAnalysis extends RocAnalysis {

	private double min;
	private double max;
	private int[] t;
	private int[] f;
	
	private int[] tp;
	private int[] fp;
	
	private boolean computed = false;
	private boolean reverseFlag;
	
	public FixedBinRocAnalysis(int bins, double min, double max) {
		this(bins, min, max, false);
	}
	/**
	 * Wheter or not to invert meaning of score
	 * @param reverseFlag
	 */
	public FixedBinRocAnalysis(int bins, double min, double max, boolean reverseFlag) {
		this.reverseFlag = reverseFlag;
		t = new int[bins+2];
		f = new int[bins+2];
		this.min = min;
		this.max = max;
	}

	@Override
	/**
	 * The larger the score, the better!
	 */
	public void addScore(double score, boolean isTrue) {
		int bin = Math.max(0,Math.min(t.length-1,(int) ((score-min)/(max-min)*(t.length-2))));
		if (isTrue)
			t[bin]++;
		else
			f[bin]++;
		computed=false;
	}
	
	@Override
	public void switchDirection() {
		reverseFlag = !reverseFlag;
		computed = false;
	}
	
	public void clear() {
		computed=false;
		Arrays.fill(t, 0);
		Arrays.fill(f, 0);
	}
	
	@Override
	public int getNumPoints() {
		return t.length-1;
	}
	
	public int getNumPositives() {
		compute();
		return tp[reverseFlag?tp.length-1:0];
	}
	
	public int getNumNegatives() {
		compute();
		return fp[reverseFlag?fp.length-1:0];
	}
	
	protected void compute() {
		if (computed)return;
		computed = true;
		tp=ArrayUtils.cumSum(t, reverseFlag?1:-1);
		fp=ArrayUtils.cumSum(f, reverseFlag?1:-1);
	}

	@Override
	public double getCutoff(int index) {
		return (double)index/(getNumPoints()-1)*(max-min)+min;
	}

	@Override
	public int getFn(int index) {
		compute();
		return getNumPositives()-tp[index];
	}

	@Override
	public int getFp(int index) {
		compute();
		return fp[index];
	}

	@Override
	public int getTn(int index) {
		compute();
		return getNumNegatives()-fp[index]; 
	}

	@Override
	public int getTp(int index) {
		compute();
		return tp[index];
	}

	
}
