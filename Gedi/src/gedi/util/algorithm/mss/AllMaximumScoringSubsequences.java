package gedi.util.algorithm.mss;

import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.ExtendedIterator;

import java.util.AbstractCollection;
import java.util.LinkedList;


public class AllMaximumScoringSubsequences extends AbstractCollection<ScoreSubsequence> {
	
	private NumericArray a;
	private int size = -1;
	
	public AllMaximumScoringSubsequences(NumericArray a) {
		this.a = a;
	}
	
	@Override
	public ExtendedIterator<ScoreSubsequence> iterator() {
		return new MSSIterator(a);
	}


	@Override
	public int size() {
		if (size==-1)
			size = (int) iterator().count();
		return size;
	}
	
	
	private static class MSSIterator implements ExtendedIterator<ScoreSubsequence> {

		private LinkedList<ScoreSubsequence> buffer = new LinkedList<ScoreSubsequence>();

		NumericArray a;
		double cumSum = 0;
		Interval tail = null;
		int n;
		
		int m;
		
		public MSSIterator(NumericArray a) {
			this.a = a;
			this.n = a.length();
		}
			
		
		@Override
		public boolean hasNext() {
			if (buffer.isEmpty())
				fillBuffer();
			return !buffer.isEmpty();
		}

		@Override
		public ScoreSubsequence next() {
			if (buffer.isEmpty())
				fillBuffer();
			return buffer.pollFirst();
		}
		
		private void fillBuffer() {
			for (; buffer.isEmpty() && m<n; m++) {
				double v = a.getDouble(m);
				if (v<=0) {
					cumSum+=v;
					continue;
				}
				
				tail = new Interval(tail,m,m,cumSum, cumSum+=v);
				
				boolean check = true;
				while (check) {
					Interval I=tail.prev;
					for (; I!=null && I.L>=tail.L; I=I.prev);
					
					if (I==null) {
						// every interval in list is MSS
						for (I=tail.prev; I!=null && I.L>=tail.L; I=I.prev) 
							buffer.addFirst(new ScoreSubsequence(a, I.l, I.r+1));
						tail.prev = null;
						check = false;
					}
					
					else if (I.R<tail.R) {
						I.r = tail.r; 
						I.R = tail.R;
						tail = I;
					} else 
						check = false;
				}
			}
			
			if (m==n && buffer.isEmpty()) 
				for (; tail!=null; tail=tail.prev) 
					buffer.addFirst(new ScoreSubsequence(a, tail.l, tail.r+1));

			
		}
		
		

		@Override
		public void remove() {}
		
	}
	
	
	
	private static class Interval {
		
		private Interval prev;
		private int l;
		private int r;
		private double L;
		private double R;
		public Interval(Interval prev, int l, int r, double L, double R) {
			this.prev = prev;
			this.l = l;
			this.r = r;
			this.L = L;
			this.R = R;
		}
		@Override
		public String toString() {
			return l+":"+r+" ("+L+":"+R+")";
		}
	}

	

}
