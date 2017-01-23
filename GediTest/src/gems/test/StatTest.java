package gems.test;

import static org.junit.Assert.*;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.math.function.StepFunction;
import gedi.util.math.stat.counting.RollingStatistics;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StatTest {
	
	@Test
	public void overlapTest() throws IOException {
		
		
		double[] x =   {0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11};
		double[] min = {0, 2, 4, 6, 8, 10};
		double[] max = {2, 4, 6, 8, 10,11};
		double[] y = new double[x.length];
		for (int i=0; i<x.length; i++)
			y[i] = i;
		
		
		RollingStatistics stat = new RollingStatistics();
		for (int i=0; i<x.length; i++)
			stat.add(x[i], y[i]);
		StepFunction cmin = stat.computeBinCovariate(2.1, 0.2, NumericArrayFunction.Min);
		StepFunction cmax = stat.computeBinCovariate(2.1, 0.2, NumericArrayFunction.Max);
		
		for (int k=0; k<cmin.getKnotCount(); k++) {
			assertEquals(min[k], cmin.applyAsDouble(cmin.getX(k)), 0);
			assertEquals(max[k], cmax.applyAsDouble(cmin.getX(k)), 0);
			
		}
		
	}
	
	@Test
	public void notieTest() throws IOException {
		
		
		double[] x =   {0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10};
		double[] min = {0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8};
		double[] max = {2, 3, 4, 5, 6, 7, 8, 9,10,10,10};
		double[] y = new double[x.length];
		for (int i=0; i<x.length; i++)
			y[i] = i;
		
		
		RollingStatistics stat = new RollingStatistics();
		for (int i=0; i<x.length; i++)
			stat.add(x[i], y[i]);
		for (int step : Arrays.asList(1,2,3,5)) {
			StepFunction cmin = stat.computeEquiSize(2, step, NumericArrayFunction.Min);
			StepFunction cmax = stat.computeEquiSize(2, step, NumericArrayFunction.Max);
			
			for (int k=0; k<cmin.getKnotCount(); k++) {
				int n = 0;
				double emin = 0;
				double emax = 0;
				for (int i=0; i<x.length; i++) {
					if (x[i]==cmin.getX(k)) {
						n++;
						emin+=min[i];
						emax+=max[i];
					}
				}
				assertEquals(emin/n, cmin.applyAsDouble(cmin.getX(k)), 0);
				assertEquals(emax/n, cmax.applyAsDouble(cmin.getX(k)), 0);
				
			}
		}
		
	}
	
	@Test
	public void tieTest() throws IOException {
		
		
		double[] x =   {0,0,0,0,0,0,0,0, 0, 1, 2, 3, 4, 5, 6, 7, 7, 7, 7, 7, 7, 8, 9,10,10,10,10,10,10,10};
		double[] min = {0,0,0,0,0,0,0,0, 0, 0, 0, 9,10,11,12,13,14,15,15,15,15,15,15,21,22,23,23,23,23,23};
		double[] max = {8,8,8,8,8,8,8,9,10,11,12,13,14,20,20,20,20,20,20,21,22,29,29,29,29,29,29,29,29,29};
		double[] y = new double[x.length];
		for (int i=0; i<x.length; i++)
			y[i] = i;
		
		
		RollingStatistics stat = new RollingStatistics();
		for (int i=0; i<x.length; i++)
			stat.add(x[i], y[i]);
		for (int step : Arrays.asList(1,2,3,5)) {
			StepFunction cmin = stat.computeEquiSize(2, step, NumericArrayFunction.Min);
			StepFunction cmax = stat.computeEquiSize(2, step, NumericArrayFunction.Max);
			
			for (int k=0; k<cmin.getKnotCount(); k++) {
				int n = 0;
				double emin = 0;
				double emax = 0;
				for (int i=0; i<x.length; i++) {
					if (x[i]==cmin.getX(k) && (i==0 || i==x.length-1 || (i>=2 && i<=x.length-3 && (i-2)%step==0))) {
						n++;
						emin+=min[i];
						emax+=max[i];
					}
				}
				assertEquals(emin/n, cmin.applyAsDouble(cmin.getX(k)), 0);
				assertEquals(emax/n, cmax.applyAsDouble(cmax.getX(k)), 0);
				
			}
		}
		
	}
	
	
}
