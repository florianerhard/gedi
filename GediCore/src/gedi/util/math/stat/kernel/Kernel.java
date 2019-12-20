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

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

public interface Kernel extends DoubleUnaryOperator {
	double halfSize();
	default String name() {
		String n = getClass().getSimpleName();
		if (n.endsWith("Kernel")) n = n.substring(0, n.length()-"Kernel".length());
		return n;
	}
	
	default PreparedIntKernel prepare() {
		return new PreparedIntKernel(this,false);
	}
	
	default PreparedIntKernel prepare(boolean normalize) {
		return new PreparedIntKernel(this,true);
	}
	
	/**
	 * first operand is the kernel operand, second is the result of the parent kernel
	 * @param parent
	 * @param op
	 */
	default Kernel decorate(DoubleBinaryOperator op) {
		return new DecoratedKernel(this, op);
	}
	
	
}
