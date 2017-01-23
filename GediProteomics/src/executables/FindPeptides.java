package executables;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.tree.Trie;
import gedi.util.functions.EI;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.fasta.DefaultFastaHeaderParser;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.FastaHeaderParser;
import gedi.util.mutable.MutableMonad;
import gedi.util.parsing.ReferenceGenomicRegionParser;
import gedi.util.userInteraction.progress.ConsoleProgress;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class FindPeptides {

	public static void main(String[] args) throws IOException {
		if (args.length<3 && !new File(args[0]).exists()) {
			System.err.println("FindPeptides evidence.txt [-u] protein_db.fasta ...\n\n -u undigested sample, do not report digested peptides\n protein_db fasta files must contain the genomic location of the peptide as the last entry (i.e. after the last space)");
			System.exit(1);
		}
		
		int off = 1;
		boolean[] undigested = {false};
		
		if (args[off].equals("-u")) {
			off = 2;
			undigested[0] = true;
		}
		
		ConsoleProgress pro = new ConsoleProgress(System.err);
		pro.init().setDescription("Reading evidences...");
		
		MutableMonad<HeaderLine> header = new MutableMonad<HeaderLine>();
		Trie<HitList> peptides = new Trie<HitList>();
		new LineOrientedFile(args[0]).lineIterator()
			.skip(1,l->header.Item = new HeaderLine(l))
			.map(s->StringUtils.split(s, '\t'))
//			.filter(a->Double.parseDouble(a[header.Item.get("PEP")])<0.05)
			.forEachRemaining(p->peptides.put(p[0].replace('I', 'L'),new HitList()));
		
		pro.setDescriptionf("Read %d peptides!",peptides.size());
		pro.finish();
		ReferenceGenomicRegionParser rparser = new ReferenceGenomicRegionParser();
		
		pro.init();
		FastaHeaderParser parser = new DefaultFastaHeaderParser(' ');
		
		for (int i=off; i<args.length; i++) {
			
			
			Iterator<FastaEntry> it = new FastaFile(args[i]).entryIterator(true);
			while (it.hasNext()) {
				FastaEntry fe = it.next();
				
				pro.setDescriptionf("Searching for hits in %s - %s",FileUtils.getNameWithoutExtension(args[i]),parser.getId(fe.getHeader()));
					
				String loc = findLoc(fe, parser, rparser);
				MutableReferenceGenomicRegion refe = rparser.apply(loc);
				
				String seq = fe.getSequence().replace('I', 'L');
				
				boolean isFirstToM = fe.getHeader().substring(1).startsWith("RPm");
				
				peptides.iterateAhoCorasick(seq, true).forEachRemaining(res->{
					int start = res.getStart();
					int end = start+res.getLength();
					
					ReferenceSequence ref = refe.getReference();
					GenomicRegion reg = new ArrayGenomicRegion(start*3,end*3);
					if (reg.getEnd()>refe.getRegion().getTotalLength() && start>0)
						reg = new ArrayGenomicRegion(start*3-1,end*3-1);
						
					if (reg.getEnd()>refe.getRegion().getTotalLength())
						System.err.println("Not contained: "+reg+" in "+fe);
					reg = refe.map(reg);
					
					String type="internal";
					String pref = end==seq.length()?"full":"Nterm";
					if (start==0) {
						if (isFirstToM) 
							type = pref+" noncanonical tRNAMet";
						else if (seq.charAt(0)=='M')
							type = pref+" canonical";
						else
							type = pref+" noncanonical";
					}
					else if (start==1) {
						if (seq.charAt(0)=='M' && !isFirstToM)
							type = pref+" canonical loss";
						else
							type = pref+" noncanonical loss";
					}
					
					if (!undigested[0] || type.startsWith("full"))
						res.getValue().add(new ImmutableReferenceGenomicRegion<String>(ref, reg, type));
					
					pro.incrementProgress();
				});
				
				
			}
				
				
			
		}
		pro.finish();
		
		
		pro.init().setDescription("Writing output...");
		new LineOrientedFile(args[0]).lineIterator().skip(1,l->System.out.println(l+"\tLocation\tType"))
			.forEachRemaining(s->{
				String p = StringUtils.splitField(s, '\t', 0).replace('I', 'L');
				String locs = EI.wrap(peptides.get(p)).map(rgr->rgr.toLocationString()).concat(";");
				String types = EI.wrap(peptides.get(p)).map(rgr->rgr.getData()).concat(";");
				System.out.printf("%s\t%s\t%s\n",s,locs,types);
				pro.incrementProgress();
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



	private static class HitList extends HashSet<ImmutableReferenceGenomicRegion<String>> {
	}
	
}
