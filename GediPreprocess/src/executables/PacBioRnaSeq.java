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
package executables;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.algorithm.mss.MaximalScoringSubsequence;
import gedi.util.datastructure.array.computed.ComputedDoubleArray;
import gedi.util.datastructure.array.computed.ComputedIntegerArray;
import gedi.util.datastructure.graph.SimpleDirectedGraph;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.ParallelizedIterator;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.Ranking;
import gedi.util.sequence.Alphabet;
import gedi.util.sequence.DnaSequence;
import gedi.util.sequence.KmerIteratorBuilder;
import gedi.util.sequence.KmerIteratorBuilder.KMerHashIterator;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class PacBioRnaSeq {

	private final static double minFrac = 0.2;

	public static void main(String[] args) throws IOException {
		
		boolean poly = false;
		if (args[0].equals("-poly")) {
			poly = true;
			args = ArrayUtils.slice(args, 1);
		}
		
		String linker = "AAGCAGTGGTATCAACGCAGAGTAC";
		String overhang = "ATGGG";
		
		if (args.length>1) {
			linker = args[0];
			args = ArrayUtils.slice(args, 1);
		}
		
		if (args.length>1) {
			overhang = args[0];
			args = ArrayUtils.slice(args, 1);
		}
		
		if (args.length==1 && new File(args[0]).exists() ) {
			pacbio(linker, overhang, args[0],poly);
			return;
		}
		
		System.err.println("gedi -e PacBioRnaSeq [-notrim] [linker] [overhang] reads.fq[.gz|.bz2]");
		System.exit(1);
		
		
	}
	
	private static void pacbio(String linker, String overhang, String file, boolean poly) throws IOException {

		
		LineWriter trimmed = new LineOrientedFile(FileUtils.getNameWithoutExtension(file)+".trimmed.fastq").write();
		LineWriter untrimmed = new LineOrientedFile(FileUtils.getNameWithoutExtension(file)+".untrimmed.fastq").write();
		
		LineIterator lit = new LineOrientedFile(file).lineIterator();
		int[] buff = new int[2];
		while (lit.hasNext()) {
			String id = lit.next();
			String seq = lit.next();
			String b = lit.next();
			String qual = lit.next();
			
			if (!poly) {
				if (check(seq,buff, linker, overhang)) {
					trimmed.writeLine(id);
					trimmed.writeLine(seq.substring(buff[0],buff[1]));
					trimmed.writeLine(b);
					trimmed.writeLine(qual.substring(buff[0],buff[1]));
				} else if (check(SequenceUtils.getDnaReverseComplement(seq),buff, linker, overhang)) {
					seq = SequenceUtils.getDnaReverseComplement(seq);
					qual = StringUtils.reverse(qual).toString();
					trimmed.writeLine(id);
					trimmed.writeLine(seq.substring(buff[0],buff[1]));
					trimmed.writeLine(b);
					trimmed.writeLine(qual.substring(buff[0],buff[1]));
				} else {
					untrimmed.writeLine(id);
					untrimmed.writeLine(seq);
					untrimmed.writeLine(b);
					untrimmed.writeLine(qual);
				}
			} else {
				boolean a = checkPolyA(seq);
				boolean t = checkPolyA(SequenceUtils.getDnaReverseComplement(seq));
				if (a && !t) {
					trimmed.writeLine(id);
					trimmed.writeLine(seq);
					trimmed.writeLine(b);
					trimmed.writeLine(qual);
				} else if (t && !a) {
					seq = SequenceUtils.getDnaReverseComplement(seq);
					qual = StringUtils.reverse(qual).toString();
					trimmed.writeLine(id);
					trimmed.writeLine(seq);
					trimmed.writeLine(b);
					trimmed.writeLine(qual);
				} else {
					untrimmed.writeLine(id);
					untrimmed.writeLine(seq);
					untrimmed.writeLine(b);
					untrimmed.writeLine(qual);
				}
			}
			
		}
		
		trimmed.close();
		untrimmed.close();
	}

	private static boolean checkPolyA(String seq) {
		ComputedDoubleArray a = new ComputedDoubleArray(i->seq.charAt(i)=='A'?1:-4, seq.length());
		MaximalScoringSubsequence mss = new MaximalScoringSubsequence();
		return mss.getMss(a).getSum()>=10;
	}


	private static boolean check(String seq, int[] buff, String linker, String overhang) {
		int l = linker.length();
		int o = overhang.length();
		int lo = l+o;
		
		
		if (!fewMM(seq,0,linker+overhang)) return false;
		
		int nmer = 1;
		for (; l+nmer*lo+lo<seq.length(); nmer++) {
			if (!fewMM(seq,l+nmer*lo,overhang+linker)) break;
		}
		buff[0] = nmer*lo;
		
		if (fewMM(seq,nmer*lo-o,"TTTTTTTT")) return false;
		
		linker = SequenceUtils.getDnaReverseComplement(linker);
		overhang = SequenceUtils.getDnaReverseComplement(overhang);

		
		if (!fewMM(seq,seq.length()-linker.length(),linker)) return false;
		
		nmer = 1;
		for (; nmer*lo+l<seq.length(); nmer++) {
			if (!fewMM(seq,seq.length()-l-nmer*lo,linker+overhang)) break;
		}
		
		if (!fewMM(seq,seq.length()-(l+(nmer-1)*lo)-8,"AAAAAAAA")) return false;
		
		buff[1] = seq.length()-(l+(nmer-1)*lo);
		return true;
	}


	private static boolean fewMM(String seq, int start, String l) {
		return lessMM(seq, start, l,(int)Math.floor(l.length()*minFrac));
	}
	private static boolean lessMM(String seq, int start, String l, int max) {
		int re = 0;
		for (int i=0; i<l.length(); i++)
			if (seq.charAt(start+i)!=l.charAt(i)) {
				re++;
				if (re>max) return false;
			}
		return true;
	}
	
	
}
