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

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.util.SequenceUtils;
import gedi.util.io.text.fasta.DefaultFastaHeaderParser;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.FastaHeaderParser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

public class SixFrame {
	
	
	public static void main(String[] args) throws IOException {
		
		if (args.length!=2 || !new File(args[1]).exists()) {
			System.err.println("SixFrame -l/a/m/mm genome.fasta");
			System.exit(1);
		}
		
		boolean all = args[0].equals("-a")||args[0].equals("-m")||args[0].equals("-mm");
		String[] start = args[0].equals("-m")||args[0].equals("-mm")?mismatchStart():new String[] {"ATG"};
		String[] stop = {"TAG","TGA","TAA"};
		FastaHeaderParser parser = new DefaultFastaHeaderParser(' ');
		int[] frames = {-3,-2,-1,1,2,3};
		boolean alsoToM = args[0].equals("-mm");
		
		
		
		HashSet<String> startSet = new HashSet<String>(Arrays.asList(start));
		HashSet<String> stopSet = new HashSet<String>(Arrays.asList(stop));
		
		
		
		Iterator<FastaEntry> it = new FastaFile(args[1]).entryIterator(true);
		int on = 0;
		
		while (it.hasNext()) {
			FastaEntry fe = it.next();
			
			for (int frame : frames) {
				Strand str = frame>0?Strand.Plus:Strand.Minus;
				int offset = Math.abs(frame)%3;
				ReferenceSequence ref = Chromosome.obtain(parser.getId(fe.getHeader()), str);
				
				
				String dna = str==Strand.Minus?SequenceUtils.getDnaReverseComplement(fe.getSequence()):fe.getSequence();
				
				int prevStop3 = 0;
				for (int i=offset; i<dna.length()-2; i+=3) {
					if (stopSet.contains(dna.substring(i, i+3))) {
						
						int n = 0;
						for (int s=prevStop3; s<i; s+=3) {
							if (startSet.contains(dna.substring(s,s+3))) {
								
								String protein = SequenceUtils.translate(dna.substring(s, i));
								writeOrf(ref,frame,str==Strand.Minus?dna.length()-i:s,str==Strand.Minus?dna.length()-s:i,n++,on++,dna.substring(s, s+3),protein);
								
								if (alsoToM && !protein.startsWith("M"))
									writeOrf(ref,frame,str==Strand.Minus?dna.length()-i:s,str==Strand.Minus?dna.length()-s:i,n++,on++,dna.substring(s, s+3),"M"+protein.substring(1));
								
								if (!all) 
									break;
							}
						}
						
						prevStop3=i+3;
					}
				}
				
			}

		}
		
	}
	
	

	private static void writeOrf(ReferenceSequence ref, int frame, int start,
			int end, int nin, int ntotal, String startCodon, String protein) {
		System.out.printf(">%s_%d Frame %d Start %s OrfForStop %d Location %s:%d-%d\n%s\n",
				ref.getName(),ntotal, frame,startCodon,nin,ref,start,end,
				protein);
		
	}



	private static String[] mismatchStart() {
		String[] re = new String[1+3*3];
		re[0] = "ATG";
		int index = 1;
		for (int i=0; i<3; i++) 
			for (int b=0; b<4; b++)
				if ("ATG".charAt(i)!=SequenceUtils.nucleotides[b])
					re[index++]="ATG".substring(0, i)+String.valueOf(SequenceUtils.nucleotides[b])+"ATG".substring(i+1);
		return re;
	}

}
