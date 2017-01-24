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

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.tree.Trie;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.fasta.DefaultFastaHeaderParser;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.FastaHeaderParser;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutableMonad;
import gedi.util.parsing.ReferenceGenomicRegionParser;
import gedi.util.userInteraction.progress.ConsoleProgress;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.UnaryOperator;

public class OldFindPeptides {

	public static void main(String[] args) throws IOException {
		if (args.length<3 && !new File(args[0]).exists()) {
			System.err.println("FindPeptides evidence.txt -b/s/p reference1.fasta ...");
			System.exit(1);
		}
		
		ConsoleProgress pro = new ConsoleProgress(System.err);
		pro.init().setDescription("Reading evidences...");
		
		MutableMonad<HeaderLine> header = new MutableMonad<HeaderLine>();
		Trie<HitList> peptides = new Trie<HitList>();
		new LineOrientedFile(args[0]).lineIterator()
			.skip(1,l->header.Item = new HeaderLine(l))
			.map(s->StringUtils.split(s, '\t'))
//			.filter(a->Double.parseDouble(a[header.Item.get("PEP")])<0.05)
			.forEachRemaining(p->peptides.put(p[0],new HitList()));
		
		pro.setDescriptionf("Read %d peptides!",peptides.size());
		pro.finish();
		ReferenceGenomicRegionParser rparser = new ReferenceGenomicRegionParser();
		
		pro.init();
		FastaHeaderParser parser = new DefaultFastaHeaderParser(' ');
		
		for (int i=2; i<args.length; i+=2) {
			boolean bothstrands = args[i-1].equals("-b");
			boolean prot = args[i-1].equals("-p");
			
			UnaryOperator<ImmutableReferenceGenomicRegion<Void>> op = t->t;
			
			if (i+1<args.length && !args[i+1].startsWith("-")) {
				CenteredDiskIntervalTreeStorage<Transcript> anno = new CenteredDiskIntervalTreeStorage<Transcript>(args[i+1]);
				op = t->anno.getReferenceRegionsIntersecting(t.getReference(), t.getRegion()).size()>0?t:null;
			}
			
			int[] frames = bothstrands? new int[]{-3,-2,-1,1,2,3}:new int[]{1,2,3};
			if (prot) frames = new int[]{0};
			
			UnaryOperator<ImmutableReferenceGenomicRegion<Void>> fop = op;
			Iterator<FastaEntry> it = new FastaFile(args[i]).entryIterator(true);
			while (it.hasNext()) {
				FastaEntry fe = it.next();
				for (int frame : frames) {
					Strand s = frame>=0?Strand.Plus:Strand.Minus;
					int offset = Math.abs(frame)%3;
					
					pro.setDescriptionf("Searching for hits in %s - %s Frame %d",FileUtils.getNameWithoutExtension(args[i]),parser.getId(fe.getHeader()),frame);
						
					String seq = s==Strand.Minus?SequenceUtils.getDnaReverseComplement(fe.getSequence()):fe.getSequence();
					String pep = prot?seq:SequenceUtils.translate(seq.substring(offset));
					ReferenceSequence refa = Chromosome.obtain(parser.getId(fe.getHeader()), s);
					
					peptides.iterateAhoCorasick(pep, true).forEachRemaining(res->{
						int start, end;
						if (prot) {
							start = res.getStart();
							end = start+res.getLength();
						} else { 
							start = s==Strand.Plus?res.getStart()*3+offset:seq.length()-res.getStart()*3-offset-res.getLength()*3;
							end = start+res.getLength()*3;
						}
						
						ReferenceSequence ref = refa;
						GenomicRegion reg = new ArrayGenomicRegion(start,end);
						
						String loc=null;
						if ((loc=findLoc(fe,parser,rparser))!=null) {
							if (prot) {
								reg = new ArrayGenomicRegion(start*3,end*3);
							}
							MutableReferenceGenomicRegion refe = rparser.apply(loc);
							reg = refe.map(reg);
							ref = refe.getReference();
						}
						
						ImmutableReferenceGenomicRegion<Void> a = fop.apply(new ImmutableReferenceGenomicRegion<Void>(ref, reg));
						if (a!=null) {
							
							res.getValue().add(a);
							pro.incrementProgress();
						}
					});
					
					
				}
				
				
			}
			
			if (i+1<args.length && !args[i+1].startsWith("-"))
				i++;
		}
		pro.finish();
		
		MutableInteger id = new MutableInteger();
		
		pro.init().setDescription("Writing output...");
		new LineOrientedFile(args[0]).lineIterator().skip(1,l->System.out.println(l+"\tId\tk\tn\tLocation"))
			.forEachRemaining(s->{
				String p = StringUtils.splitField(s, '\t', 0);
				int n = 0;
				if (peptides.get(p)!=null){
					for (ImmutableReferenceGenomicRegion<Void> hit : peptides.get(p))
						System.out.printf("%s\t%d\t%d\t%d\t%s\n",s,id.N,n++,peptides.get(p).size(),hit);
					if (peptides.get(p).isEmpty())
						System.out.printf("%s\t%d\t%d\t%d\t\n",s,id.N,n++,peptides.get(p).size());
					id.N++;
					pro.incrementProgress();
				}
			});
		pro.finish();
		
	}
	
	
	
	private static String findLoc(FastaEntry fe, FastaHeaderParser parser, ReferenceGenomicRegionParser rparser) {
		if (rparser.canParse(parser.getId(fe.getHeader())))
			return parser.getId(fe.getHeader());
		if (rparser.canParse(fe.getHeader().substring(fe.getHeader().lastIndexOf(' ')+1)))
			return fe.getHeader().substring(fe.getHeader().lastIndexOf(' ')+1);
		return null;
	}



	private static class HitList extends HashSet<ImmutableReferenceGenomicRegion<Void>> {
	}
	
}
