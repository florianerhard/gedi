package gedi.proteomics.digest;

import cern.colt.bitvector.BitVector;
import gedi.util.datastructure.collections.intcollections.IntArrayList;


public class FullAfterAADigester implements Digester{

	
	private int missed;
	private BitVector aa;
	

	public FullAfterAADigester(char...aa) {
		this(2,aa);
	}
	public FullAfterAADigester(int missed,char...aa) {
		this.missed = missed;
		this.aa = new BitVector(256);
		for (char a : aa)
			this.aa.putQuick(a, true);
	}
	
	
	public FullAfterAAPeptideIterator iteratePeptides(String protein) {
		return new FullAfterAAPeptideIterator(protein,missed);
	}
	
	@Override
	public int getPeptideCount(String seq) {
		int re = 2;
		for (int i=0; i<seq.length()-1; i++)
			if (aa.getQuick(seq.charAt(i)))
				re++;
		return re;
	}
	
	
	public class FullAfterAAPeptideIterator implements DigestIterator {

		private String protein;
		private int missed;
		private int[] krpos;
		private int[] next = {0,0};
		private String pep = null;
		private int start;
		private int end;

		public FullAfterAAPeptideIterator(String protein, int missed) {
			this.protein = protein;
			this.missed = missed;
			IntArrayList kr = new IntArrayList(protein.length()/2);
			kr.add(-1);
			if (protein.startsWith("M")) {
				kr.add(0);
				next[1] = 1;
			}
			
			for (int i=0; i<protein.length(); i++)
				if (aa.getQuick(protein.charAt(i)))
					kr.add(i);
			if (kr.getLastInt()!=protein.length()-1)
				kr.add(protein.length()-1);
			krpos = kr.toIntArray();
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return pep!=null;
		}

		@Override
		public String next() {
			lookAhead();
			start = krpos[next[0]]+1;
			end = krpos[next[1]]+1;
			String re = pep;
			pep = null;
			return re;
		}
		
		public int getStartPosition() {
			return start;
		}
		
		public int getEndPosition() {
			return end;
		}
		
		@Override
		public int getMissed() {
			return next[0]==0&&protein.startsWith("M")?next[1]-next[0]:next[1]-next[0]-1;
		}
		
		private void lookAhead() {
			if (pep==null) {
				next[1]++;
				int missed = next[0]==0&&protein.startsWith("M")?this.missed+1:this.missed;
					
				if (next[1]-next[0]-1>missed || next[1]==krpos.length) {
					next[0]++;
					missed = this.missed;
					next[1]=next[0]+1;
				}
				if (next[1]-next[0]-1<=missed && next[1]<krpos.length)
					pep = protein.substring(krpos[next[0]]+1,krpos[next[1]]+1);
			}
		}

		@Override
		public void remove() {
		}
		
	}
}
