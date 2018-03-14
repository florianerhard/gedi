/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
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
	
	
	public double integral(double from, double to) {
		int fi = Arrays.binarySearch(x, from);
		int ti = Arrays.binarySearch(x, to);
		
		double re = 0;
		if (-fi-1>=0 && -fi-1<x.length) // if inside: linear interpolated value to 1
			re+=noKnotBetweenintegral(from,x[-fi-1]);
		
		if (-ti-2>=0 && -ti-2<x.length) // if inside: 0 to linear interpolated value
			re+=noKnotBetweenintegral(x[-ti-2],to);
		
		if (fi<0) fi = -fi-1;
		if (ti<0) ti = -ti-2;
		for (int k=fi; k<ti; k++)
			re+=noKnotBetweenintegral(x[k],x[k+1]);
		
		return re;
	}
	
	protected abstract double noKnotBetweenintegral(double from, double to);


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
