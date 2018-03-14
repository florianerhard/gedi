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
package gedi.util.math.stat.kernel;

import org.apache.commons.math3.distribution.NormalDistribution;

public class GaussianKernel extends RealDistributionKernel {

	
	public GaussianKernel() {
		super(new NormalDistribution(0,1), 1E-4);
	}

	
	public GaussianKernel(double sd) {
		super(new NormalDistribution(0,sd), 1E-4);
	}

	
	public GaussianKernel(double sd, double maxMassOutside) {
		super(new NormalDistribution(0,sd), maxMassOutside);
	}

	
	
	
	public void setParameter(String name, String value) {
		if (name.equals("sd")) setSd(Double.parseDouble(value));
		throw new IllegalArgumentException("Parameter "+name+" unknown!");
	}
	public void setSd(double sd) {
		dist= new NormalDistribution(0, sd);
		updateDistribution();
	}
	
	public double getSd() {
		return ((NormalDistribution)dist).getStandardDeviation();
	}


	public void setParameter(String name, double value) {
		if (name.equals("sd")) setSd(value);
		throw new IllegalArgumentException("Parameter "+name+" unknown!");
	}
	public void setParameter(String name, int value) {
		if (name.equals("sd")) setSd(value);
		throw new IllegalArgumentException("Parameter "+name+" unknown!");
	}
	
	private String[] parameterNames = {"sd"};
	
	public String[] parameterNames() { 
		return parameterNames;
	}
	
	public String getParameter(String name) {
		if (name.equals("sd")) return ""+getSd();
		throw new IllegalArgumentException("Parameter "+name+" unknown!"); 
	}
	
	
	
}
