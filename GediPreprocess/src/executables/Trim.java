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
import gedi.util.SequenceUtils;
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

public class Trim {

	
	private static final int KMER_SIZE = 6;
	public static final double FREQ_CUTOFF = 0.4;
	private static final int DETERMINE = 100_000;

	public static void main(String[] args) throws IOException {
		
		Progress progress = new NoProgress();
		if (args[0].equals("-p")) {
			progress = new ConsoleProgress(System.err);
			args = ArrayUtils.slice(args, 1);
		}
		
		if (args.length==4 && new File(args[0]).exists()  && new File(args[1]).exists()) {
			paired(progress,args);
			return;
		}
		
		if (args.length==2 && new File(args[0]).exists() ) {
			single(progress,args);
			return;
		}
		
		System.err.println("gedi -e GediTrim fw.fq[.gz|.bz2] bw.fq[.gz|.bz2] tfw.fq[.gz|.bz2] tbw.fq[.gz|.bz2]");
		System.exit(1);
		
		
	}
	
	
	private static void single(Progress prog, String[] args) throws IOException {
		// Phase 1: Determine adapters
		KmerIteratorBuilder kmerer = new KmerIteratorBuilder(Alphabet.getDna(),KMER_SIZE);
		ParallelizedIterator<String, Object, int[]> para = EI.lines(args[0])
				.head(DETERMINE*4)//Runtime.getRuntime().availableProcessors()
				.parallelized(1, 8000,()->new int[kmerer.getNumKmers()], 
						(eit,state)->{
							while (eit.hasNext()) {
								eit.next(); // Header 1
								String s1 = eit.next();
								eit.next(); // inter 1
								eit.next(); // qual 1
								kmerer.iterateSequence(s1,hash->state[hash]++);
							}
							return EI.empty();
						}
						);
		para.drain();
		
		int[] histo = para.getState(0);
		for (int i=1; i<para.getNthreads(); i++)
			ArrayUtils.add(histo,para.getState(i));

		int argmax = ArrayUtils.argmax(histo);

		ParallelizedIterator<String, Object, DetermineState> para2 = EI.lines(args[0])
				.head(DETERMINE*4)//Runtime.getRuntime().availableProcessors()
				.parallelized(1, 8000,()->new DetermineState(), 
						(eit,state)->{
							while (eit.hasNext()) {
								eit.next(); // Header 1
								String s1 = eit.next();
								eit.next(); // inter 1
								eit.next(); // qual 1
								KMerHashIterator kit = kmerer.iterateSequence(s1);
								while (kit.hasNext()) {
									int hash = kit.nextInt();
									if (histo[hash]>histo[argmax]*FREQ_CUTOFF) {
										int m = kit.getLastOffset();
										for (int i=m; i<s1.length(); i++) {
											int ind = SequenceUtils.inv_nucleotides[s1.charAt(i)];
											if (ind>=0 && ind<5)
												state.fw[i-m][ind]++;
										}
										kit.drain();
									}
								}
								
								
							}
							return EI.empty();
						}
						);
		para2.drain();
		
		for (int i=1; i<para2.getNthreads(); i++)
			para2.getState(0).add(para2.getState(i));
		
		String fw = para2.getState(0).getAdapter(para2.getState(0).fw);
		System.out.println("Adapter prefix:  "+fw);

		
		
	}
	
	private static void paired(Progress prog, String[] args) throws IOException {
		// Phase 1: Determine adapters
		ParallelizedIterator<String, Object, DetermineState> para = EI.lines(args[0]).alternating(EI.lines(args[1]))
				.head(DETERMINE*8)//Runtime.getRuntime().availableProcessors()
				.parallelized(1, 8000,()->new DetermineState(), 
						(eit,state)->{
							while (eit.hasNext()) {
								eit.next(); // Header 1
								eit.next(); // Header 2
								String s1 = eit.next();
								String s2 = eit.next();
								eit.next(); // inter 1
								eit.next(); // inter 2
								eit.next(); // qual 1
								eit.next(); // qual 2
								int m = suffixPrefixMatch(s1, SequenceUtils.getDnaReverseComplement(s2));
//								System.out.println(m);
								for (int i=m; i<s1.length(); i++) {
									int ind = SequenceUtils.inv_nucleotides[s1.charAt(i)];
									if (ind>=0 && ind<5)
										state.fw[i-m][ind]++;
									ind = SequenceUtils.inv_nucleotides[s2.charAt(i)];
									if (ind>=0 && ind<5)
										state.bw[i-m][ind]++;
								}
							}
							return EI.empty();
						}
						);
		para.drain();
		
		for (int i=1; i<para.getNthreads(); i++)
			para.getState(0).add(para.getState(i));
		
		String fw = para.getState(0).getAdapter(para.getState(0).fw);
		String bw = para.getState(0).getAdapter(para.getState(0).bw);
		System.out.println("Forward adapter prefix:  "+fw);
		System.out.println("Backward adapter prefix: "+bw);
		
		// prepare kmers
		KmerIteratorBuilder kmerer = new KmerIteratorBuilder(Alphabet.getDna(),KMER_SIZE);
		int[] fwpos = new int[kmerer.getNumKmers()]; Arrays.fill(fwpos, -1);
		int[] bwpos = new int[kmerer.getNumKmers()]; Arrays.fill(bwpos, -1);
		KMerHashIterator it = kmerer.iterateSequence(fw);
		while (it.hasNext()) {
			int hash = it.nextInt();
			fwpos[hash] = it.getLastOffset();
		}
		it = kmerer.iterateSequence(bw);
		while (it.hasNext()) {
			int hash = it.nextInt();
			bwpos[hash] = it.getLastOffset();
		}
		
		// phase 2: trim
		ExtendedIterator<String> rit = EI.lines(args[0]).alternating(EI.lines(args[1]))
			.progress(prog,-1,a->"Processing reads")	
			.parallelized(Runtime.getRuntime().availableProcessors(), 8000,
				eit->{
					String[] re = new String[4000];
					int ind = 0;
					while (eit.hasNext()) {
						eit.next(); // Header 1
						eit.next(); // Header 2
						String s1 = eit.next();
						String s2 = eit.next();
						eit.next(); // inter 1
						eit.next(); // inter 2
						String q1 = eit.next(); // qual 1
						String q2 = eit.next(); // qual 2
						int m = findAdapter(s1,s2,fw,bw,fwpos,kmerer);
						if (m==-1)
							m = findAdapter(s2,s1,bw,fw, bwpos,kmerer);
						if (m==-1)
							m = expensiveMatch(s1, s2, fw, bw);
						
						if (m==-1) {
							re[ind++] = s1;
							re[ind++] = q1;
							re[ind++] = s2;
							re[ind++] = q2;
						}
						else {
							re[ind++] = s1.substring(0,m);
							re[ind++] = q1.substring(0,m);
							re[ind++] = s2.substring(0,m);
							re[ind++] = q2.substring(0,m);
						}
					}
					return EI.wrap(re);
				}
				);
		
		LineWriter wfw = new LineOrientedFile(args[2]).write();
		LineWriter wbw = new LineOrientedFile(args[3]).write();
		
		int index = 0;
		while (rit.hasNext()) {
			wfw.writef2("@%d\n%s\n+\n%s\n", index,rit.next(),rit.next());
			wbw.writef2("@%d\n%s\n+\n%s\n", index,rit.next(),rit.next());
			index++;
		}
		wfw.close();
		wbw.close();
		
	}
	
	private static int expensiveMatch(String a, String b, String aadapt, String badapt) {
		for (int m=0; m<a.length(); m++) {
			int maxmm = (m+aadapt.length()+badapt.length())/6;
			if (mismatches(a, b, aadapt, badapt, m, maxmm))
				return m;
		}
		return -1;
	}


	private static int findAdapter(String a, String b, String aadap, String badap, int[] pos, KmerIteratorBuilder kmerer) {
		KMerHashIterator it = kmerer.iterateSequence(a);
		int lastm = -1;
		while (it.hasNext()) {
			int hash = it.nextInt();
			if (pos[hash]!=-1) {
				// found it, check mismatches
				int m = it.getLastOffset()-pos[hash];
				int maxmm = (m+aadap.length()+badap.length())/6;
				if (m>=0 && m!=lastm && mismatches(a,b,aadap,badap,m,maxmm)) 
					return m;
				lastm = m;
			}
		}
		return -1;
	}

	// counts the mismatches
	private static boolean mismatches(String a, String b, String aadap, String badap, int m, int maxmm) { 
		int mm = 0;
		for (int i=0; i<m; i++)
			if (a.charAt(i)!=SequenceUtils.getDnaComplement(b.charAt(m-1-i))) {
				mm++;
				if (mm>maxmm) 
					return false;
			}
		
		for (int i=0; i<Math.min(a.length()-m, aadap.length()); i++)
			if (a.charAt(m+i)!=aadap.charAt(i)){
				mm++;
				if (mm>maxmm) 
					return false;
			}
		
		for (int i=0; i<Math.min(b.length()-m, badap.length()); i++)
			if (b.charAt(m+i)!=badap.charAt(i)){
				mm++;
				if (mm>maxmm) 
					return false;
			}
		
		return true;
	}

	private static class DetermineState {
		int[][] fw = new int[256][5];
		int[][] bw = new int[256][5];
		
		public void add(DetermineState state) {
			for (int i=0; i<fw.length; i++) ArrayUtils.add(fw[i], state.fw[i]);
			for (int i=0; i<bw.length; i++) ArrayUtils.add(bw[i], state.bw[i]);
		}
		
		public String getAdapter(int[][] s) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<s.length; i++) {
				int am = 0;
				int sum = s[i][0];
				for (int c=1; c<4; c++) {
					if (s[i][c]>s[i][am])
						am = c;
					sum+=s[i][c];
				}
				if (s[i][am]/(double)sum<FREQ_CUTOFF) {
//					System.out.println();
					return sb.toString();
				}
//				System.out.println(String.valueOf(SequenceUtils.nucleotides[am])+" "+s[i][am]/(double)sum);
				
				sb.append(SequenceUtils.nucleotides[am]);
			}
			return sb.toString();
		}
		
	}
	
	private static int suffixPrefixMatch(String a, String b) {
		assert a.length()==b.length();
		int p = 0;
		int s = 0;
		int re = 0;
		for (int i=0; i<a.length(); i++) {
			int hp=SequenceUtils.inv_nucleotides[a.charAt(i)];
			int hs=SequenceUtils.inv_nucleotides[b.charAt(b.length()-1-i)];
			p+=1<<(hp*4);
			s+=1<<(hs*4);
			if (p==s) {
				if (a.substring(0,i+1).equals(b.substring(b.length()-i-1)))
					re = i+1;
			}
		}
		return re;
	}
	
}
