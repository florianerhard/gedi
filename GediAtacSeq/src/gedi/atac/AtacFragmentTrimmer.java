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

package gedi.atac;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import gedi.util.FunctorUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.algorithm.string.search.BoyerMoore;
import gedi.util.datastructure.collections.SimpleHistogram;
import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.datastructure.tree.Trie;
import gedi.util.datastructure.tree.Trie.AhoCorasickResult;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.mutable.MutableInteger;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;
import gedi.util.userInteraction.progress.ProgressFactory;



/**
 * <p>
 * If two Tn5 attacks occur in close proximity, a fragment smaller than the sequencing length is produced. Then, the 19bp mosaic end sequences
 * are sequenced (and probably the first bp of the sequencing primer (i.e. transposon payload).
 * </p>
 * 
 * <p>
 * Thus, the paired end reads may look like this:
 * <pre>
 * >CD4p_ATACseq_Day1_Rep1.14592
 * GTTTAGAGGAGTGCAGCCTCCAAGAT CTGTCTCTTATACACATCT CCGAG
 * >CD4p_ATACseq_Day1_Rep1.14592 HWI-ST281:266:C1LTTACXX:6:1101:16653:29899 length=50
 * ATCTTGGAGGCTGCACTCCTCTAAAC CTGTCTCTTATACACATCT GACGC
 * </pre>
 * 
 * The first part is the fragment; thus, the prefixes of the mate pairs must be reverse complementary. Then, the Tn5 mosaic end is sequenced in 
 * both mates, followed by the prefix of the corresponding sequencing primer. 
 * </p>
 * 
 * <p>
 * To recognize this, the mosaic end prefix is identified as suffix of one of the two reads (assuming that a sequencing error is unlikely to occur
 * in both mates simultaneously), then the mismatches in the other read and of the reverse complementary fragment sequences are counted. If this 
 * number is smaller than the specified threshold, the me position is returned (such that reads sequences can be trimmed).
 * </p>
 * 
 * <p>
 * Since mosaic end suffixes are not necessarily unique, the longest suffix is taken that meets the threshold.
 * </p>
 * 
 * <p>
 * Don't use bowtie1 for truncated paired end reads, as bowtie won't report any alignments! Bowtie2 does, however.
 * </p>
 * 
 * @author erhard
 *
 */
public class AtacFragmentTrimmer {

	public static final String MOSAIC_END = "CTGTCTCTTATACACATCT";
	
	private int maxFragmentMismatch = 4;
	private int minLength = 1;
	private String mosaicEndSequence = MOSAIC_END;
	
	private Trie<Integer> trie;
	private BoyerMoore bm = new BoyerMoore();
	
	private LineOrientedFile stats = new LineOrientedFile(LineOrientedFile.STDERR);
	private Progress progress = new NoProgress();
	
	
	public AtacFragmentTrimmer() {
		prepare();
	}
	
	public AtacFragmentTrimmer setProgress(Progress progress) {
		this.progress = progress;
		return this;
	}

	public AtacFragmentTrimmer setMaxFragmentMismatch(int maxFragmentMismatch) {
		this.maxFragmentMismatch = maxFragmentMismatch;
		return this;
	}
	
	public AtacFragmentTrimmer setMosaicEndSequence(String mosaicEndSequence) {
		this.mosaicEndSequence = mosaicEndSequence;
		prepare();
		return this;
	}
	
	
	private void prepare() {
		trie = new Trie<Integer>();
		String rme = StringUtils.reverse(mosaicEndSequence).toString();
		for (int i=0; i<rme.length(); i++)
			trie.put(rme.substring(i), rme.length()-i);
		bm.setPattern(mosaicEndSequence);
	}

	public int findME(String a, String b) {
		if (a.length()!=b.length())
			throw new RuntimeException("Sequences do not have same length: \n"+a+"\n"+b);
		int re = checkExactMosaic(a, b);
		if (re==-1)
			re = checkExactMosaic(b, a);
		return re;
	}
	
	public int findMESimple(String a, String b) {
		int re = checkExactMosaicSimple(a, b);
		if (re==-1)
			re = checkExactMosaicSimple(b, a);
		return re;
	}
	
	private int checkExactMosaicSimple(String a, String b) {
		int o = a.indexOf(mosaicEndSequence);
		if (o!=-1 && checkPos(o, a, b))
			return o;
		
		for (int l=mosaicEndSequence.length()-1; l>0; l--)
			if (a.endsWith(mosaicEndSequence.substring(0, l)) && checkPos(a.length()-l, a, b))
					return a.length()-l;
		return -1;
	}
	
	/**
	 * Finds the longest exact match of mosaic sequence in a, with a match in b meeting the mismatch threshold
	 * @param a
	 * @param b
	 * @return
	 */
	private int checkExactMosaic(String a, String b) {
		bm.setText(a);
		IntIterator it = bm.searchIterate();
		while (it.hasNext()) {
			int p = it.nextInt();
			if (checkPos(p,a,b))
				return p;
		}
		
		CharSequence reva = StringUtils.reverse(a);
		Iterator<Integer> it2 = trie.iteratePrefixMatches(reva);
		int longest = -1;
		while (it2.hasNext()) {
			int p = a.length()-it2.next();
			if (checkPos(p,a,b))
				longest = p;
		}
		// make sure mismatches is setup correctly
		if (longest!=-1) checkPos(longest, a, b);
		return longest;
	}

	int mismatches = 0;
	private boolean checkPos(int p, String a, String b) {
		mismatches = 0;
		// count mosaic mismatches in b
		for (int i=p; i<b.length() && i-p<mosaicEndSequence.length(); i++)
			if (b.charAt(i)!=mosaicEndSequence.charAt(i-p)) {
				if (++mismatches>maxFragmentMismatch) return false;
			}
		
		// count reverse complementary mismatches
		for (int i=0; i<p; i++) {
			if (!SequenceUtils.isComplementary(a.charAt(i), b.charAt(p-1-i))) {
				if (++mismatches>maxFragmentMismatch) return false;
			}
		}
		
		return true;
	}

	
	public void trimFastq(String in1, String in2, String out1, String out2) throws IOException {
		Iterator<String> it1 = new LineOrientedFile(in1).lineIterator();
		Iterator<String> it2 = new LineOrientedFile(in2).lineIterator();
		LineOrientedFile o1 = new LineOrientedFile(out1);
		LineOrientedFile o2 = new LineOrientedFile(out2);
		o1.startWriting();
		o2.startWriting();
		
		String h1, h2;
		String l1, l2;
		String or1, or2;
		
		int[] mmHisto = new int[maxFragmentMismatch+1];
		SimpleHistogram<Integer> sizeHisto = new SimpleHistogram<Integer>().setComparator(FunctorUtils.naturalComparator());
		progress.init().setDescription("Adapter trimming...");
		while (it1.hasNext() || it2.hasNext()) {
			h1 = checkedNext(it1);
			h2 = checkedNext(it2);
			
			if (!checkPairHeader(h1,h2) || !h1.startsWith("@"))
				throw new IOException("Fastq headers do not match:\n"+h1+"\n"+h2);
			
			or1 = l1 = checkedNext(it1);
			or2 = l2 = checkedNext(it2);
			if (l1.length()!=l2.length()) {
				// pad shorter with N
				if (l1.length()<l2.length()) l1 = StringUtils.padRight(l1, l2.length(),'N');
				if (l2.length()<l1.length()) l2 = StringUtils.padRight(l2, l1.length(),'N');
			}
			
			int p = findME(l1,l2);
			if (p>=0) {
				mmHisto[mismatches]++;
				sizeHisto.put(p);
				if (p>=minLength) {
					o1.writeLine(h1);
					o2.writeLine(h2);
					o1.writeLine(l1.substring(0, p));
					o2.writeLine(l2.substring(0, p));
					checkedPipe(it1,o1);
					checkedPipe(it2,o2);
					o1.writeLine(checkedNextTrim(it1, p));
					o2.writeLine(checkedNextTrim(it2, p));
				} else {
					checkedNext(it1);
					checkedNext(it2);
					checkedNext(it1);
					checkedNext(it2);
				}
			} else {
				sizeHisto.put(-1);
				o1.writeLine(h1);
				o2.writeLine(h2);
				o1.writeLine(or1);
				o2.writeLine(or2);
				checkedPipe(it1,o1);
				checkedPipe(it2,o2);
				checkedPipe(it1,o1);
				checkedPipe(it2,o2);
			}
			
			
			progress.incrementProgress();
			
		}
		
		o1.finishWriting();
		o2.finishWriting();
		progress.finish();
		
		// write histos
		stats.startWriting();
		stats.writef("Mismatch histogram\n");
		for (int i=0; i<mmHisto.length; i++)
			stats.writef("%d\t%d\n",i,mmHisto[i]);
		stats.writef("Fragment size histogram (total="+sizeHisto.getTotal()+")\n");
		sizeHisto.writeHistogram(stats.getWriter());
		
		stats.finishWriting();
	}

	private boolean checkPairHeader(String h1, String h2) {
		if (h1.length()!=h2.length()) return false;
		for (int i=0; i<h1.length(); i++)
			if (h1.charAt(i)!=h2.charAt(i) && (h1.charAt(i)!='1' || h2.charAt(i)!='2'))
				return false;
		return true;
	}

	private String checkedNext(Iterator<String> it) throws IOException {
		if (!it.hasNext()) 
			throw new IOException("Unexpected end of file!"); 
		return it.next();
	}
	
	private String checkedNextTrim(Iterator<String> it, int p) throws IOException {
		if (!it.hasNext()) 
			throw new IOException("Unexpected end of file!"); 
		String re = it.next();
		if (p<re.length())
			re = re.substring(0, p);
		re = StringUtils.padRight(re, p, '#');
		return re;
	}
	
	
	private String checkedPipe(Iterator<String> it, LineOrientedFile o) throws IOException {
		String l = checkedNext(it);
		o.writeLine(l);
		return l;
	}
	
	
}
