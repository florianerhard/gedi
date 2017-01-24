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

package gedi.core.reference;

import gedi.util.StringUtils;

import java.util.function.UnaryOperator;

public enum ReferenceSequenceConversion implements UnaryOperator<ReferenceSequence> {

	none {
		@Override
		public ReferenceSequence apply(ReferenceSequence t) {
			return t;
		}
		
		@Override
		public boolean altersName() {
			return false;
		}
	},
	
	toEnsembl {

		@Override
		public ReferenceSequence apply(ReferenceSequence t) {
			String name = StringUtils.removeHeader(t.getName(), "chr");
			if (name.equals("M")) name = "MT";
			return Chromosome.obtain(name,t.getStrand());
		}

		@Override
		public boolean altersName() {
			return true;
		}
	},
	
	toUcsc {

		@Override
		public ReferenceSequence apply(ReferenceSequence t) {
			String name = t.getName();
			if (name.equals("MT")) name = "M";
			if (!name.startsWith("chr"))
				name = "chr"+name;
			return Chromosome.obtain(name,t.getStrand());
		}

		@Override
		public boolean altersName() {
			return true;
		}
	},
	
	toOpposite {

		@Override
		public ReferenceSequence apply(ReferenceSequence t) {
			return t.toOppositeStrand();
		}

		@Override
		public boolean altersName() {
			return false;
		}
	},
	
	toPlus {

		@Override
		public ReferenceSequence apply(ReferenceSequence t) {
			return t.toPlusStrand();
		}

		@Override
		public boolean altersName() {
			return false;
		}
	},
	
	toMinus {

		@Override
		public ReferenceSequence apply(ReferenceSequence t) {
			return t.toMinusStrand();
		}

		@Override
		public boolean altersName() {
			return false;
		}
	},
	
	toIndependent {

		@Override
		public ReferenceSequence apply(ReferenceSequence t) {
			return t.toStrandIndependent();
		}
		
		@Override
		public boolean altersName() {
			return false;
		}
		
	};
	
	public abstract boolean altersName();
	
	
}
