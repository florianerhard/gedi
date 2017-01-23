package gedi.util.math.function;

import java.util.Arrays;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;

public class PiecewiseLinearFunction extends KnotFunction {
	   

    public PiecewiseLinearFunction(double[] x, double[] y) throws NullArgumentException,
			NoDataException, DimensionMismatchException {
		super(x, y);
	}

	public double applyAsDouble(double x) {
        int index = Arrays.binarySearch(this.x, x);
        double fx = 0;

        if (index < -1) {
            // "x" is between "abscissa[-index-2]" and "abscissa[-index-1]".
        	if (-index-1==this.x.length)
        		fx = y[-index-2];
        	else {
        		double frac = (x-this.x[-index-2])/(this.x[-index-1]-this.x[-index-2]);
        		fx = (1-frac)*y[-index-2] + frac*y[-index-1];
        	}
        } else if (index >= 0) {
            // "x" is exactly "abscissa[index]".
            fx = y[index];
        } else {
            // Otherwise, "x" is smaller than the first value in "abscissa"
            // (hence the returned value should be "ordinate[0]").
            fx = y[0];
        }

        return fx;
    }
    
       
}
