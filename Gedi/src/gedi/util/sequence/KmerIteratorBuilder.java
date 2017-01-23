package gedi.util.sequence;

import gedi.util.datastructure.collections.intcollections.IntIterator;

import java.util.Arrays;
import java.util.function.IntConsumer;


public class KmerIteratorBuilder {

	
	protected char[] sigma;
	protected int[] isigma;
	protected int q;
	protected int mask;
	
	
	public KmerIteratorBuilder(Alphabet alphabet, int k) {
		this.sigma = alphabet.toCharArray();
		
		if (sigma.length!=4)
			throw new RuntimeException("Implement it!");
		
		this.isigma = new int[265];
		Arrays.fill(isigma, -1);
		for (int i=0; i<sigma.length; i++)
			isigma[sigma[i]]=i;
		
		this.q = k;
		
		mask = (int) (Math.pow(4, k)-1);
	}
	
	
	public int indexOfChar(char c) {
		return isigma[c];
	}
	
	public int getNumKmers() {
		return (int)Math.pow(getAlphabetSize(),getK());
	}
	
	public int hash(CharSequence s) {
		int re = 0;
		for (int i=0; i<q; i++)
			if (isigma[s.charAt(i)]==-1)
				return -1;
			else
				re |= isigma[s.charAt(i)]<<(2*(q-i-1));
		return re;
	}
	
	public int hash(char[] s) {
		int re = 0;
		for (int i=0; i<q; i++)
			re |= isigma[s[i]]<<(2*(q-i-1));
		return re;
	}
	
	public char[] unhash(int index) {
		char[] re = new char[q];
		for (int i=0; i<q; i++)
			re[i] = sigma[(index>>(2*((q-i-1))))&3];
		return re;
	}
	

	public int getAlphabetSize() {
		return sigma.length;
	}
	
	public int getK() {
		return q;
	}
	
	public KMerHashIterator iterateSequence(CharSequence s) {
		return new KMerHashIterator(s);
	}
	
	public void iterateSequence(CharSequence s, IntConsumer consumer) {
		KMerHashIterator it = iterateSequence(s);
		while (it.hasNext()) 
			consumer.accept(it.nextInt());
	}
	
	public class KMerHashIterator implements IntIterator {

		private CharSequence sequence;
		private int hash = -1; // always the next hash value
		private int i=q-2;  // always the last position of the next kmer
		private int lastHash = -1;
		private int lastOffset;
		
		public KMerHashIterator(CharSequence sequence) {
			this.sequence = sequence;
			for (hash=-1; hash==-1 && i+1<sequence.length(); hash = hash(sequence.subSequence(++i-q+1,i+1)));
		}
		
		@Override
		public boolean hasNext() {
			return hash!=-1;
		}

		@Override
		public Integer next() {
			return nextInt();
		}

		@Override
		public void remove() {
		}

		@Override
		public int nextInt() {
			lastHash = hash;
			lastOffset = i-q+1;
			advance();
			return lastHash;
		}
		
		public String nextKmer() {
			return new String(unhash(nextInt()));
		}
		
		public CharSequence getSequence() {
			return sequence;
		}
		
		public int getLastOffset() {
			return lastOffset;
		}
		
		private void advance() {
			if (++i>=sequence.length()) {
				hash=-1;
				i++;
				return;
			}
			if (isigma[sequence.charAt(i)]==-1) {
				for (hash=-1; hash==-1 && i+1<sequence.length(); hash = hash(sequence.subSequence(++i-q+1,i+1)));
			} else {
				hash=((hash<<2)&mask)|isigma[sequence.charAt(i)];
			}
		}
		
	}

	
}
