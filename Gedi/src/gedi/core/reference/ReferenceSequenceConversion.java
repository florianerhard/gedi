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
