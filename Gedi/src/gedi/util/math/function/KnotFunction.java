package gedi.util.math.function;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.MathArrays.OrderDirection;

public abstract class KnotFunction implements DoubleUnaryOperator, BinarySerializable {

	protected double[] x;
	protected double[] y;


	/**
	 * Builds a knot function from a list of arguments and the corresponding
	 * values. Specifically, returns the function h(x) defined by <pre><code>
	 * h(x) = y[0] for all x < x[1]
	 *        y[1] for x[1] <= x < x[2]
	 *        ...
	 *        y[y.length - 1] for x >= x[x.length - 1]
	 * </code></pre>
	 * The value of {@code x[0]} is ignored, but it must be strictly less than
	 * {@code x[1]}.
	 * 
	 * x and y are not copied!
	 * if x and y are not strictly monotonically increasing, the mean of the y values is computed for equal x values
	 * It has to be increasing! 
	 *
	 * @param x Domain values where the function changes value.
	 * @param y Values of the function.
	 * @throws NullArgumentException if {@code x} or {@code y} are {@code null}.
	 * @throws NoDataException if {@code x} or {@code y} are zero-length.
	 * @throws DimensionMismatchException if {@code x} and {@code y} do not
	 * have the same length.
	 */
	public KnotFunction(double[] x,
			double[] y)
					throws NullArgumentException, NoDataException,
					DimensionMismatchException {
		if (x == null ||
				y == null) {
			throw new NullArgumentException();
		}
		if (x.length == 0 ||
				y.length == 0) {
			throw new NoDataException();
		}
		if (y.length != x.length) {
			throw new DimensionMismatchException(y.length, x.length);
		}
		MathArrays.checkOrder(x,OrderDirection.INCREASING,false);

		int index = 0;
		double sumy = 0;
		int ny = 0;
		for (int i=0; i<x.length; i++) {
			if (x[i]!=x[index]) {
				index++;
				ny = 1;
				sumy = y[i];
			} else {
				ny++;
				sumy+=y[i];
			}
			x[index]=x[i];
			y[index]=sumy/ny;
		}
		if (index+1!=x.length) {
			x = Arrays.copyOf(x, index+1);
			y = Arrays.copyOf(y, index+1);
		}

		this.x = x;
		this.y = y;
	}


	public int getKnotCount() {
		return x.length;
	}

	public double getX(int index) {
		return x[index];
	}

	public double getY(int index) {
		return y[index];
	}

	public double[] getX() {
		return x;
	}

	public double[] getY() {
		return y;
	}
	
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(x.length);
		for (int i=0; i<x.length; i++)
			out.putDouble(x[i]);
		for (int i=0; i<y.length; i++)
			out.putDouble(y[i]);
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		this.x = new double[in.getCInt()];
		this.y = new double[x.length];
		for (int i=0; i<x.length; i++)
			x[i] = in.getDouble();
		for (int i=0; i<y.length; i++)
			y[i] = in.getDouble();
	}
}
