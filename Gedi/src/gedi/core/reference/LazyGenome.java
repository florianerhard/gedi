package gedi.core.reference;

import gedi.core.data.annotation.ReferenceSequenceLengthProvider;
import gedi.core.data.annotation.ReferenceSequencesProvider;
import gedi.util.StringUtils;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.ToIntFunction;

public class LazyGenome implements ToIntFunction<String> {
	
	private ReferenceSequencesProvider refSeqs;
	private ReferenceSequenceLengthProvider lengths;
	
	public LazyGenome(ReferenceSequencesProvider refSeqs,
			ReferenceSequenceLengthProvider lengths) {
		this.refSeqs = refSeqs;
		this.lengths = lengths;
	}

	public LazyGenome(String path) throws IOException {
		refs = new ArrayList<String>();
		map = new HashMap<String, Integer>();
		for (String l : new LineOrientedFile(path).lineIterator("#").loop()) {
			String[] p = StringUtils.split(l, '\t');
			if (p.length!=2) throw new RuntimeException("Must contain chr\tlength lines!");
			refs.add(p[0]);
			map.put(p[0],Integer.parseInt(p[1]));
		}
	}
	
	private ArrayList<String> refs;
	private HashMap<String,Integer> map = new HashMap<String, Integer>();
	
	@Override
	public int applyAsInt(String value) {
		return Math.abs(map.computeIfAbsent(value, r->lengths.getLength(r)));
	}
	
	public int getLength(String name) {
		return applyAsInt(name);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); 
		for (String n : getReferenceSequences()) {
			sb.append(n).append("\t").append(applyAsInt(n)).append("\n");
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	public boolean isLengthOnlyBound(String value) {
		return map.computeIfAbsent(value, r->lengths.getLength(r))<0;
	}

	public List<String> getReferenceSequences() {
		if (refs==null) {
			HashSet<String> names = new HashSet<String>();
			for (ReferenceSequence ref : refSeqs.iterateReferenceSequences().loop()) 
				names.add(ref.getName());
			refs = new ArrayList<String>(names);
			Collections.sort(refs,ReferenceSequence::compareChromosomeNames);
		}
		return refs;
	}

	public ReferenceSequence getReferenceSequenceBefore(
			ReferenceSequence referenceSequence) {
		String name;
		if (referenceSequence.getStrand()==Strand.Minus) name = getNameAfter(referenceSequence.getName());
		else name = getNameBefore(referenceSequence.getName());
		return name==null?null:Chromosome.obtain(name,referenceSequence.getStrand());
	}
	
	
	public ReferenceSequence getReferenceSequenceAfter(
			ReferenceSequence referenceSequence) {
		String name;
		if (referenceSequence.getStrand()==Strand.Minus) name = getNameBefore(referenceSequence.getName());
		else name = getNameAfter(referenceSequence.getName());
		return name==null?null:Chromosome.obtain(name,referenceSequence.getStrand());
	}
	
	
	public String getNameBefore(
			String name) {
		int ind = getReferenceSequences().indexOf(name);
		if (ind==-1) throw new IllegalArgumentException(name+" is an unknown chromosome!");
		return ind==0?null:refs.get(ind-1);
	}
	
	
	public String getNameAfter(
			String name) {
		int ind = getReferenceSequences().indexOf(name);
		if (ind==-1) throw new IllegalArgumentException(name+" is an unknown chromosome!");
		return ind==refs.size()-1?null:refs.get(ind+1);
	}
	

}
