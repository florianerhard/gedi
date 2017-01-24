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

package gedi.lfc.foldchange;

import gedi.lfc.Log2FoldChange;

public class TransformedMLLog2FoldChange implements Log2FoldChange{

	@Override
	public double computeFoldChange(double a, double b) {
		return (Math.log(a)-Math.log(b))/Math.log(2);
	}

}
