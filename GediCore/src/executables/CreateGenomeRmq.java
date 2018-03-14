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

import gedi.app.Gedi;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.reference.Chromosome;
import gedi.core.reference.Strand;
import gedi.util.ArrayUtils;
import gedi.util.MathUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.sequence.Alphabet;
import gedi.util.sequence.KmerIteratorBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CreateGenomeRmq {

	public static void main(String[] args) throws IOException {
		
		if (args.length!=2 || !new File(args[1]).exists() || !StringUtils.isInt(args[0])) {
			System.err.println("CreateGenomeRmq <window> <fasta-index>");
			System.exit(1);
		}
		
		
		Gedi.startup();	
		

		int w = Integer.parseInt(args[0]);
		
		KmerIteratorBuilder bpIt = new KmerIteratorBuilder(Alphabet.getDna(), 1);
		KmerIteratorBuilder cpgIt = new KmerIteratorBuilder(Alphabet.getDna(), 2);
		FastaIndexFile fi = new FastaIndexFile(args[1]).open();
		
		DiskGenomicNumericBuilder bpOut = new DiskGenomicNumericBuilder(args[1].substring(0, args[1].length()-2)+"bp.rmq");
		int[] bpCount = new int[bpIt.getNumKmers()];
		DiskGenomicNumericBuilder cpgOut = new DiskGenomicNumericBuilder(args[1].substring(0, args[1].length()-2)+"CpG.rmq");
		int[] cpgCount = new int[1];
		
		int cpg = cpgIt.hash("CG");
		
		for (String ch : fi.getEntryNames()) {
			System.out.println(ch);
			int l = fi.getLength(ch);
			Chromosome reference = Chromosome.obtain(ch,Strand.Independent);
			for (int i=0; i<l; i+=w) {
				Arrays.fill(bpCount, 0);
				bpIt.iterateSequence(fi.getSequence(ch, i, Math.min(i+w+bpIt.getK()-1,l)),km->bpCount[km]++);
				bpOut.addValue(reference, i, bpCount);
				
				Arrays.fill(cpgCount, 0);
				cpgIt.iterateSequence(fi.getSequence(ch, i, Math.min(i+w+cpgIt.getK()-1,l)),km->{if (km==cpg) cpgCount[0]++;});
				cpgOut.addValue(reference, i, cpgCount);
			}
		}
		
		bpOut.build();
		cpgOut.build();
	}
	
}
