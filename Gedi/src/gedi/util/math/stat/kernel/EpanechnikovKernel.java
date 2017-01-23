package gedi.util.math.stat.kernel;

public class EpanechnikovKernel implements Kernel {

	private double size;
	

	public EpanechnikovKernel() {
		this(1);
	}
	
	public EpanechnikovKernel(double size) {
		super();
		this.size = size;
	}


	@Override
	public double applyAsDouble(double u) {
		u/=size;
		if (u>=1||u<=-1) return 0;
		return 0.75*(1-u*u);
	}

	@Override
	public double halfSize() {
		return size;
	}

	
}
