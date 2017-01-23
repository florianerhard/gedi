package gedi.util.math.stat.kernel;

public class SingletonKernel implements Kernel {

	@Override
	public double applyAsDouble(double operand) {
		return operand==0?1:0;
	}

	@Override
	public double halfSize() {
		return 0;
	}

}
