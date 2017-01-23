package gems.test;

import gedi.util.MathUtils;
import gedi.util.SequenceUtils;
import gedi.util.io.text.fasta.DefaultFastaHeaderParser;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.FastaHeaderParser;
import gedi.util.sequence.DnaSequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Mapping {

	
	private static class RefPos {
		String reference;
		int pos;
		int mm;
		
		public RefPos(String reference, int pos) {
			this.reference = reference;
			this.pos = pos;
		}
		
		public RefPos(String reference, int pos, int k, int mm, char r, char o) {
			this.reference = reference;
			this.pos = pos;
			
			mm = mm+1;
			mm |= SequenceUtils.inv_nucleotides[r]<<Integer.highestOneBit(k);
			mm |= SequenceUtils.inv_nucleotides[o]<<(Integer.highestOneBit(k)+2);
		}
		
		public RefPos(String reference, int pos, int k, int mm1, char r1, char o1,  int mm2, char r2, char o2) {
			this.reference = reference;
			this.pos = pos;
			
			mm = mm1+1;
			mm |= SequenceUtils.inv_nucleotides[r1]<<Integer.highestOneBit(k);
			mm |= SequenceUtils.inv_nucleotides[o1]<<(Integer.highestOneBit(k)+2);
			
			mm |= (mm1+1)<<(Integer.highestOneBit(k)+4);
			mm |= SequenceUtils.inv_nucleotides[r1]<<(Integer.highestOneBit(k)*2+4);
			mm |= SequenceUtils.inv_nucleotides[o1]<<(Integer.highestOneBit(k)*2+6);
			
		}
	}
	
	
	private HashMap<DnaSequence, ArrayList<RefPos>> index = new HashMap<DnaSequence, ArrayList<RefPos>>();
	private int k = 8;
	
	
	public void loadReference(String path) throws IOException {
		FastaHeaderParser p = new DefaultFastaHeaderParser(' ');
		FastaFile ff = new FastaFile(path);
		long[] l = new long[1];
//		ff.entryIterator(true).forEachRemaining(fe->addSequence(p.getId(fe.getHeader()),fe.getSequence()));
		ff.entryIterator(true).forEachRemaining(fe->{
			l[0]+=Math.max(0,fe.getSequence().length()/k+1);
		});
		System.out.println(l[0]);
		System.out.println(l[0]*(k*3+1));
		System.out.println((int)Math.pow(4, k));
	}
	
	public void addSequence(String id, String sequence) {
		System.out.println(id);
		for (int p=0; p<sequence.length()-k+1; p+=k) {
			char[] sub = sequence.substring(p, p+k).toCharArray();
			index.computeIfAbsent(new DnaSequence(sub), d->new ArrayList<RefPos>()).add(new RefPos(id,p));
			for (int i=0; i<k; i++) {
				char o = sub[i];
				for (char r : SequenceUtils.nucleotides) {
					if (r!=o) {
						sub[i] = r;
						index.computeIfAbsent(new DnaSequence(sub), d->new ArrayList<RefPos>()).add(new RefPos(id,p,k, i,r,o));
						sub[i] = o;
					}
				}
			}
			
//			for (int i=0; i<k; i++) {
//				char o = sub[i];
//				for (char r : SequenceUtils.nucleotides) {
//					if (r!=o) {
//						sub[i] = r;
//						for (int i2=i+1; i2<k; i2++) {
//							char o2 = sub[i];
//							for (char r2 : SequenceUtils.nucleotides) {
//								if (r2!=o2) {
//									sub[i2] = r2;
//									index.computeIfAbsent(new DnaSequence(sub), d->new ArrayList<RefPos>()).add(new RefPos(id,p,k, i,r,o, i2,r2,o2));
//									sub[i2] = o2;
//								}
//							}
//						}
//						sub[i] = o;
//					}
//				}
//			}
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		Mapping m = new Mapping();
		m.loadReference("/mnt/biostor1/Data/Databases/Ensembl/v75/sequences/Homo_sapiens.GRCh37.75.cdna.all.fa");
		
		
	}
	
}
