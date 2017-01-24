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
