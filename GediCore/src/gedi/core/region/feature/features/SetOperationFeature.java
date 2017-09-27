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

package gedi.core.region.feature.features;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.util.functions.ExtendedIterator;

import java.util.ArrayList;
import java.util.Set;


public class SetOperationFeature extends AbstractFeature<Object> {

	public enum SetOperation {
		Union, Intersect, Subtract
	}
	
	
	private SetOperation op = SetOperation.Union;
	
	public SetOperationFeature() {
		minInputs = 2;
		maxInputs = Integer.MAX_VALUE;
	}
	
	public void setOp(SetOperation op) {
		this.op = op;
		maxInputs = Integer.MAX_VALUE;
		if (op==SetOperation.Subtract) {
			maxInputs = 2;
		}
	}

	@Override
	public GenomicRegionFeature<Object> copy() {
		SetOperationFeature re = new SetOperationFeature();
		re.copyProperties(this);
		re.op = op;
		return re;
	}
	
	@Override
	protected void accept_internal(Set<Object> values) {
		switch (op) {
		case Union:
			for (int i=0; i<getInputLength(); i++)
				values.addAll(getInput(i));
			break;
		case Intersect:
			values.addAll(getInput(0));
			for (int i=1; i<getInputLength(); i++)
				values.retainAll(getInput(i));
			break;
		case Subtract:
			values.addAll(getInput(0));
			values.removeAll(getInput(1));
			break;
		}
	}

}
