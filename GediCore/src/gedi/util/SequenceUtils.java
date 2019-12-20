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
package gedi.util;

import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.StringUtils.ReversedCharSequence;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.datastructure.tree.Trie;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.fasta.index.FastaIndexFile.FastaIndexEntry;
import gedi.util.mutable.MutableInteger;
import gedi.util.oml.cps.CpsReader;
import gedi.util.sequence.WithFlankingSequence;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;
import java.util.regex.Pattern;

import cern.colt.bitvector.BitVector;


public class SequenceUtils {

	public enum DnaRnaMode {
		Dna,Rna
	}

	public static final char[] rna_nucleotides = {'A','C','G','U','N','-'};
	public static final char[] nucleotides = {'A','C','G','T','N','-'};
	public static final char[] valid_nucleotides = {'A','C','G','T'};
	public static final char[] compl_nucleotides = {'T','G','C','A','N','-'};
	public static final char[] compl_rna_nucleotides = {'U','G','C','A','N','-'};
	public static final int[] inv_nucleotides = new int[256];
	public static final char[][] dna_iupac = new char[256][];
	public static final Trie<String> code = new Trie<String>();
	public static final String STOP_CODON = "*";
	static {
		code.put("GCT", "A");
		code.put("GCC", "A");
		code.put("GCA", "A");
		code.put("GCG", "A");
		code.put("GCN", "A");
		code.put("TTA", "L");
		code.put("TTG", "L");
		code.put("CTT", "L");
		code.put("CTC", "L");
		code.put("CTA", "L");
		code.put("CTG", "L");
		code.put("CTN", "L");
		code.put("CGT", "R");
		code.put("CGC", "R");
		code.put("CGA", "R");
		code.put("CGG", "R");
		code.put("CGN", "R");
		code.put("AGA", "R");
		code.put("AGG", "R");
		code.put("AAA", "K");
		code.put("AAG", "K");
		code.put("AAT", "N");
		code.put("AAC", "N");
		code.put("ATG", "M");
		code.put("GAT", "D");
		code.put("GAC", "D");
		code.put("TTT", "F");
		code.put("TTC", "F");
		code.put("TGT", "C");
		code.put("TGC", "C");
		code.put("CCT", "P");
		code.put("CCC", "P");
		code.put("CCA", "P");
		code.put("CCG", "P");
		code.put("CCN", "P");
		code.put("CAA", "Q");
		code.put("CAG", "Q");
		code.put("TCT", "S");
		code.put("TCC", "S");
		code.put("TCA", "S");
		code.put("TCG", "S");
		code.put("TCN", "S");
		code.put("AGT", "S");
		code.put("AGC", "S");
		code.put("GAA", "E");
		code.put("GAG", "E");
		code.put("ACT", "T");
		code.put("ACC", "T");
		code.put("ACA", "T");
		code.put("ACG", "T");
		code.put("ACN", "T");
		code.put("GGT", "G");
		code.put("GGC", "G");
		code.put("GGA", "G");
		code.put("GGG", "G");
		code.put("GGN", "G");
		code.put("TGG", "W");
		code.put("CAT", "H");
		code.put("CAC", "H");
		code.put("TAT", "Y");
		code.put("TAC", "Y");
		code.put("ATT", "I");
		code.put("ATC", "I");
		code.put("ATA", "I");
		code.put("GTT", "V");
		code.put("GTC", "V");
		code.put("GTA", "V");
		code.put("GTG", "V");
		code.put("GTN", "V");
		code.put("TAG", STOP_CODON);
		code.put("TGA", STOP_CODON);
		code.put("TAA", STOP_CODON);

		Arrays.fill(inv_nucleotides,4);
		inv_nucleotides['A'] = inv_nucleotides['a'] = 0;
		inv_nucleotides['C'] = inv_nucleotides['c'] = 1;
		inv_nucleotides['G'] = inv_nucleotides['g'] = 2;
		inv_nucleotides['T'] = inv_nucleotides['t'] = 3;
		inv_nucleotides['N'] = inv_nucleotides['n'] = 4;
		inv_nucleotides['-'] = inv_nucleotides['-'] = 5;

		inv_nucleotides['U'] = inv_nucleotides['u'] = 3;

		dna_iupac['A'] = new char[] {'A'};
		dna_iupac['C'] = new char[] {'C'};
		dna_iupac['G'] = new char[] {'G'};
		dna_iupac['T'] = new char[] {'T'};
		dna_iupac['U'] = new char[] {'T'};
		dna_iupac['W'] = new char[] {'A','T'};
		dna_iupac['S'] = new char[] {'C','G'};
		dna_iupac['M'] = new char[] {'A','C'};		
		dna_iupac['K'] = new char[] {'G','T'};
		dna_iupac['R'] = new char[] {'A','G'}; 	
		dna_iupac['Y'] = new char[] {'C','T'};
		dna_iupac['B'] = new char[] {'C','G','T'};
		dna_iupac['D'] = new char[] {'A','G','T'};
		dna_iupac['H'] = new char[] {'A','C','T'};
		dna_iupac['V'] = new char[] {'A','C','G'}; 	
		dna_iupac['N'] = new char[] {'A','C','G','T'};

	}




	public static String translate(CharSequence dna) {
		StringBuilder sb = new StringBuilder();
		dna = toDna(dna);
		for (int i=0; i<dna.length()-2; i+=3) {
			String aa = code.get(dna.subSequence(i,i+3).toString());
			if (aa==null)
				aa="X";
			sb.append(aa);
		}
		return sb.toString();
	}

	

	public static boolean isWobble(char a, char b) {
		a = Character.toUpperCase(a);
		b = Character.toUpperCase(b);
		return (a=='G' && b=='U') || (a=='U' && b=='G');
	}

	public final static boolean isComplementary(char a, char b) {
		int bindex = inv_nucleotides[b];
		return a==compl_nucleotides[bindex] || a==compl_rna_nucleotides[bindex];
	}
	
	public final static boolean canPair(char a, char b) {
		return isComplementary(a, b)||isWobble(a, b);
	}

	public static String toRna(CharSequence dnaSequence) {
		StringBuilder sb = new StringBuilder(dnaSequence.length());
		for (int i=0; i<dnaSequence.length(); i++)
			sb.append(getDnaToRna(dnaSequence.charAt(i)));
		return sb.toString();
	}

	public static String toDna(CharSequence rnaSequence) {
		StringBuilder sb = new StringBuilder(rnaSequence.length());
		for (int i=0; i<rnaSequence.length(); i++)
			sb.append(getRnaToDna(rnaSequence.charAt(i)));
		return sb.toString();
	}

	public static String getDnaReverseComplement(CharSequence dnaSequence) {
		StringBuilder sb = new StringBuilder(dnaSequence.length());
		for (int i=dnaSequence.length()-1; i>=0; i--)
			sb.append(getDnaComplement(dnaSequence.charAt(i)));
		return sb.toString();
	}
	
	public static char[] getDnaReverseComplementInplace(char[] dnaSequence) {
		for (int i=0; i<dnaSequence.length/2; i++) {
			char tmp=getDnaComplement(dnaSequence[i]);
			dnaSequence[i]=getDnaComplement(dnaSequence[dnaSequence.length-1-i]);
			dnaSequence[dnaSequence.length-1-i]=tmp;
		}
		if (dnaSequence.length%2==1)
			dnaSequence[dnaSequence.length/2]=getDnaComplement(dnaSequence[dnaSequence.length/2]);
		return dnaSequence;
	}
	
	public static char[] getRnaReverseComplementInplace(char[] rnaSequence) {
		for (int i=0; i<rnaSequence.length/2; i++) {
			char tmp=getRnaComplement(rnaSequence[i]);
			rnaSequence[i]=getRnaComplement(rnaSequence[rnaSequence.length-1-i]);
			rnaSequence[rnaSequence.length-1-i]=tmp;
		}
		if (rnaSequence.length%2==1)
			rnaSequence[rnaSequence.length/2]=getRnaComplement(rnaSequence[rnaSequence.length/2]);
		return rnaSequence;
	}
	
	public static class SixFrameTranslatedSequence implements CharSequence {
		
		private CharSequence dna;
		private int l;
		private int offset;
		private boolean reverse;
		public SixFrameTranslatedSequence(CharSequence dna, int offset, boolean reverse) {
			this.dna = dna;
			this.l = dna.length();
			this.offset = offset;
			this.reverse = reverse;
		}

		@Override
		public char charAt(int index) {
			index = index*3;
			if (reverse) {
				index = l-index-3;
				index = index-offset;
			}
			else
				index = index+offset;
				
			CharSequence codon = dna.subSequence(index, index+3);
			if (reverse)
				codon = getDnaReverseComplement(codon);
			return translate(codon).charAt(0);
		}

		@Override
		public int length() {
			return (l-offset)/3;
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return StringUtils.toString(this,start,end);
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(this);
			return new String(sb);
		}
		
	}

	public static String getRnaReverseComplement(CharSequence dnaSequence) {
		StringBuilder sb = new StringBuilder(dnaSequence.length());
		for (int i=dnaSequence.length()-1; i>=0; i--)
			sb.append(getRnaComplement(dnaSequence.charAt(i)));
		return sb.toString();
	}

	public static String getDnaComplement(CharSequence dnaSequence) {
		StringBuilder sb = new StringBuilder(dnaSequence.length());
		for (int i=0; i<dnaSequence.length(); i++)
			sb.append(getDnaComplement(dnaSequence.charAt(i)));
		return sb.toString();
	}

	public static String getRnaComplement(CharSequence dnaSequence) {
		StringBuilder sb = new StringBuilder(dnaSequence.length());
		for (int i=0; i<dnaSequence.length(); i++)
			sb.append(getRnaComplement(dnaSequence.charAt(i)));
		return sb.toString();
	}


	public static char getDnaComplement(char nucleotide) {
		char re = compl_nucleotides[inv_nucleotides[nucleotide]];
		if (Character.isLowerCase(nucleotide))
			return Character.toLowerCase(re);
		return re;
	}
	public static char getRnaComplement(char nucleotide) {
		char re = compl_rna_nucleotides[inv_nucleotides[nucleotide]];
		if (Character.isLowerCase(nucleotide))
			return Character.toLowerCase(re);
		return re;
	}
	public static char getRnaToDna(char nucleotide) {
		char re = nucleotides[inv_nucleotides[nucleotide]];
		if (Character.isLowerCase(nucleotide))
			return Character.toLowerCase(re);
		return re;
	}
	public static char getDnaToRna(char nucleotide) {
		char re = rna_nucleotides[inv_nucleotides[nucleotide]];
		if (Character.isLowerCase(nucleotide))
			return Character.toLowerCase(re);
		return re;
	}



	public static Pattern getIUPACPattern(String iupac) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<iupac.length(); i++) {
			sb.append("(");
			for (char c : dna_iupac[iupac.charAt(i)])
				sb.append(c).append("|");
			sb.replace(sb.length()-1, sb.length(), ")");
		}
		return Pattern.compile(sb.toString());
	}

	public static <T> Trie<T> getIUPACTrie(String iupac, T value) {
		Trie<T> re = new Trie<T>();
		int[] ind = new int[iupac.length()];
		char[] word = new char[iupac.length()];
		
		do {
			for (int i=0; i<word.length; i++)
				word[i] = dna_iupac[iupac.charAt(i)][ind[i]];
			re.put(new String(word), value);
		} while(ArrayUtils.increment(ind, i->dna_iupac[iupac.charAt(i)].length));
		return re;
	}



	public static char normalizeBase(char base) {
		return nucleotides[inv_nucleotides[base]];
	}


	public static double getGcContent(CharSequence sequence) {
		int c = 0;
		int l = sequence.length();
		for (int i=0; i<l; i++) {
			char b = Character.toUpperCase(sequence.charAt(i));
			if (b=='G' || b=='C')
				c++;
		}
		return (double)c/l;
	}

	public static String scramble(String sequence) {
		char[] s = sequence.toCharArray();
		ArrayUtils.shuffleSlice(s, 0, s.length);
		return new String(s);
	}


	public static WithFlankingSequence scramble(WithFlankingSequence sequence) {
		return new WithFlankingSequence(
				scramble(sequence.get5Flank()).toLowerCase()+
				scramble(sequence.getActualSequence()).toUpperCase()+
				scramble(sequence.get3Flank()).toLowerCase()
				);
	}


	private static BitVector rnaBv = new BitVector(256);
	static {
		rnaBv.set('a');rnaBv.set('A');
		rnaBv.set('c');rnaBv.set('C');
		rnaBv.set('u');rnaBv.set('U');
		rnaBv.set('g');rnaBv.set('G');
	}
	public static boolean isRna(CharSequence sequence) {
		int n = sequence.length();
		for (int i=0; i<n; i++)
			if (!rnaBv.getQuick(sequence.charAt(i)))
				return false;
		return true;
	}


	/**
	 * if open is true, only the stop codon is checked!
	 * @param dna
	 * @param open
	 * @return
	 */
	public static boolean isOrf(String dna, boolean open) {
		dna = toDna(dna);
		if (!open && dna.length()%3!=0) return false;
		if (!open && !dna.startsWith("ATG")) return false;
		if (!STOP_CODON.equals(code.get(dna.substring(dna.length()-3)))) return false;
		return true;
	}




	public static Function<Character, Color> getNucleotideColorizer() {
		DynamicObject d = new CpsReader().parse(SequenceUtils.class.getResourceAsStream("/resources/colors.cps")).getForClasses(new HashSet<>(Arrays.asList("basecolors")));
		HashMap<Character, Color> map = EI.wrap(d.get("styles").asArray()).index(m->m.getEntry("name").asString().charAt(0),m->PaintUtils.parseColor(m.getEntry("fill").asString()));
		map.put('U', map.get('T'));
		return c->map.containsKey(c)?map.get(c):Color.gray;
	}


	public static String extractSequence(GenomicRegion coord,
			FastaIndexEntry index) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<coord.getNumParts(); i++)
			sb.append(index.getSequence(coord.getStart(i), coord.getEnd(i)));
		return sb.toString();
	}
	public static String extractSequence(GenomicRegion coord,
			CharSequence seq) {
		coord = coord.intersect(new ArrayGenomicRegion(0,seq.length()));
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<coord.getNumParts(); i++)
			sb.append(seq.subSequence(coord.getStart(i), coord.getEnd(i)));
		return sb.toString();
	}
	public static char[] extractSequence(GenomicRegion coord,
			char[] seq, char[] re) {
		if (re==null || re.length<coord.getTotalLength()) re = new char[coord.getTotalLength()];
		int c = 0;
		for (int i=0; i<coord.getNumParts(); i++) {
			System.arraycopy(seq, coord.getStart(i), re, c, coord.getLength(i));
			c+=coord.getLength(i);
		}
		return re;
	}
	public static char[] extractSequence(GenomicRegion coord,
			char[] seq) {
		char[] re = new char[coord.getTotalLength()];
		int off = 0;
		for (int i=0; i<coord.getNumParts(); i++) {
			System.arraycopy(seq, coord.getStart(i), re, off, coord.getLength(i));
			off+=coord.getLength(i);
		}
		return re;
	}
	public static String extractSequenceSave(GenomicRegion coord,
			String seq, char r) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<coord.getNumParts(); i++)
			sb.append(StringUtils.saveSubstring(seq, coord.getStart(i), coord.getEnd(i),r));
		return sb.toString();
	}
	
	public static ArrayGenomicRegion getAlignedRegion(String aliLine) {
		return MaskedCharSequence.maskChars(aliLine,'-','-').getUnmaskedRegion();
	}


	/**
	 * Many transcripts in ensembl are not complete...
	 * @param genomic
	 * @param t
	 * @return
	 */
	public static boolean checkCompleteCodingTranscript(Genomic genomic,
			ReferenceGenomicRegion<Transcript> t) {
		return checkCompleteCodingTranscript(genomic, t, 0, 0,false);
	}
	public static boolean checkCompleteCodingTranscript(Genomic genomic,
			ReferenceGenomicRegion<Transcript> t, int min5utr, int min3utr, boolean checkInternalStop) {
		if (!t.getData().isCoding()) return false;
		MutableReferenceGenomicRegion<Transcript> cds = t.getData().getCds(t);
		if (cds.getRegion().getTotalLength()%3!=0) return false;
		GenomicRegion stop = cds.map(new ArrayGenomicRegion(cds.getRegion().getTotalLength()-3,cds.getRegion().getTotalLength()));
		if (!translate(genomic.getSequence(t.getReference(), stop)).equals("*")) return false;
		if (checkInternalStop && StringUtils.removeFooter(translate(genomic.getSequence(cds).toString()),"*").contains("*")) return false;
		if (t.getData().get5Utr(t).getRegion().getTotalLength()<min5utr || t.getData().get3Utr(t).getRegion().getTotalLength()<min3utr) return false;
		return true;
	}


	public static ArrayList<GenomicRegion> getPolyAStretches(String seq) {
		return getPolyAStretches(seq, 10, 1);
	}

	public static ArrayList<GenomicRegion> getPolyAStretches(String seq, int minlen, int maxctcount) {
		
		ArrayList<GenomicRegion> re = new ArrayList<>();
		
		int[] cumA = new int[seq.length()];
		int[] cumG = new int[seq.length()];
		for (int i=0; i<seq.length(); i++) {
			if (seq.charAt(i)=='A')
				cumA[i]++;
			else if (seq.charAt(i)=='G')
				cumG[i]++;
		}
		ArrayUtils.cumSumInPlace(cumA, 1);
		ArrayUtils.cumSumInPlace(cumG, 1);
		for (int i=minlen; i<cumA.length; i++) {
			int na=cumA[i]-cumA[i-minlen];
			int ng=cumG[i]-cumG[i-minlen];
			if (na+ng>=minlen-maxctcount && na>ng) {
				// extend
				int s;
				for (s=i+1; s<cumA.length; s++) {
					na=cumA[s]-cumA[i-minlen];
					ng=cumG[s]-cumG[i-minlen];
					if (na+ng<s-i+minlen-maxctcount || na<=ng) 
						break;
				}
				re.add(new ArrayGenomicRegion(i-minlen,s));
			}
		}
		
		return re;
	}
	
	
}
