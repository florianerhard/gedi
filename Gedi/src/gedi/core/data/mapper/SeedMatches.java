package gedi.core.data.mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.tree.Trie;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.io.text.fasta.DefaultFastaHeaderParser;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.fasta.FastaHeaderParser;

@GenomicRegionDataMapping(fromType=CharSequence.class,toType=IntervalTree.class)
public class SeedMatches implements GenomicRegionDataMapper<CharSequence,IntervalTree<GenomicRegion,NameAnnotation>>{

	private Trie<ArrayList<String>> seeds = new Trie<ArrayList<String>>();
	private boolean complement = false;
	
	
	public void setComplement(boolean complement) {
		this.complement = complement;
	}
	public void addMirnas(String fastaPath) throws IOException {
		FastaHeaderParser hp = new DefaultFastaHeaderParser(' ');
		new FastaFile(fastaPath).entryIterator(true).forEachRemaining(fe->
			seeds.computeIfAbsent(SequenceUtils.getDnaReverseComplement(fe.getSequence().substring(1,7)),k->new ArrayList<String>()).add(hp.getId(fe.getHeader()))
			);
	}
	
	@Override
	public IntervalTree<GenomicRegion, NameAnnotation> map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			CharSequence data) {
		IntervalTree<GenomicRegion, NameAnnotation> re = new IntervalTree<GenomicRegion, NameAnnotation>(reference.toStrand(!complement));
		
		if (data==null) return re;
				
		if (complement)
			data = SequenceUtils.getDnaReverseComplement(data);
		
		MutableReferenceGenomicRegion rgr = new MutableReferenceGenomicRegion()
										.setReference(reference.toStrand(!complement))
										.setRegion(region);
		
		
		seeds.iterateAhoCorasick(data.toString(), true).forEachRemaining(acr->{
			GenomicRegion reg = rgr.map(new ArrayGenomicRegion(acr.getStart(),acr.getEnd()));
			re.put(reg, new NameAnnotation(acr.getValue().toString()));
		});
		
		
		return re;
		
		
	}
	
	

}
