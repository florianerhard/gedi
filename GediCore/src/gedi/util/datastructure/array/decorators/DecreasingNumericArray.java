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
package gedi.util.datastructure.array.decorators;


import gedi.util.datastructure.array.NumericArray;

public class DecreasingNumericArray extends DecoratedNumericArray {
	
	
	public DecreasingNumericArray(NumericArray parent) {
		super(parent);
	}
	
	@Override
	public void setInfimum(int index) {
		parent.setSupremum(index);
	}
	
	@Override
	public void setSupremum(int index) {
		parent.setInfimum(index);
	}


	@Override
	public int compare(int index1, int index2) {
		return -parent.compare(index1, index2);
	}

	@Override
	public int compare(int index1, NumericArray a2, int index2) {
		return -parent.compare(index1, a2, index2);
	}

	@Override
	public int compareInCum(int index1, int index2) {
		return -parent.compareInCum(index1, index2);
	}

}
