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
package gedi.util.math.stat.testing;

import java.util.Arrays;

import gedi.util.ArrayUtils;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;

public class WilcoxonUnpaired {

	 /** Ranking algorithm. */
    private NaturalRanking naturalRanking;

    /**
     * Create a test instance where NaN's are left in place and ties get
     * the average of applicable ranks. Use this unless you are very sure
     * of what you are doing.
     */
    public WilcoxonUnpaired() {
        naturalRanking = new NaturalRanking(NaNStrategy.FIXED,
                TiesStrategy.AVERAGE);
    }

    /**
     * Create a test instance using the given strategies for NaN's and ties.
     * Only use this if you are sure of what you are doing.
     *
     * @param nanStrategy
     *            specifies the strategy that should be used for Double.NaN's
     * @param tiesStrategy
     *            specifies the strategy that should be used for ties
     */
    public WilcoxonUnpaired(final NaNStrategy nanStrategy,
                                  final TiesStrategy tiesStrategy) {
        naturalRanking = new NaturalRanking(nanStrategy, tiesStrategy);
    }
    
    private double xsum(double[] r, int nx) {
    	double re = 0;
    	for (int i=0; i<nx; i++)
    		re+=r[i];
    	return re;
    }
    private NormalDistribution norm = new NormalDistribution();
    
    public double computePval(double[] x, double[] y) {
    	return computePval(H1.NOT_EQUAL, x, y);
    }
    public double computePval(H1 h1, double[] x, double[] y) {
    	double[] conc = ArrayUtils.concat(x,y);
    	double[] r = naturalRanking.rank(conc);
    	double[] rsort = r.clone();
    	Arrays.sort(rsort);
    	
        int nx = x.length;
        int ny = y.length;
        double w = xsum(r,nx) - nx * (nx + 1) / 2;
        double tiesSum = 0;
        int s = 0;
        for (int i=1; i<rsort.length; i++) {
        	if (Double.compare(rsort[s], rsort[i])<0) {
        		double d=i-s;
        		if (d>1)
        			tiesSum+=d*d*d-d;
        		s=i;
        	}
        }
        int d=r.length-s;
		if (d>1)
			tiesSum+=d*d*d*-d;
        double z = w - nx * ny / 2;
        double SIGMA = Math.sqrt((nx * ny / 12) *
                      ((nx + ny + 1)
                       - tiesSum
                       / ((nx + ny) * (nx + ny - 1))));
        double CORRECTION = Math.signum(z)*0.5;
        if (h1==H1.GREATER_THAN) CORRECTION = 0.5;
        else if (h1==H1.LESS_THAN) CORRECTION = 0.5;
            
	    z = (z - CORRECTION) / SIGMA;
	    double PVAL = norm.cumulativeProbability(z);
	    if (h1==H1.GREATER_THAN) PVAL = 1-PVAL;
        else if (h1==H1.NOT_EQUAL) PVAL = 2*Math.min(PVAL,1-PVAL);
	    
	    return PVAL;
    }
    
}
