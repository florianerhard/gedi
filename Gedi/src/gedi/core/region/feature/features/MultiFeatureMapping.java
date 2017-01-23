package gedi.core.region.feature.features;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.util.datastructure.charsequence.MaskedCharSequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;


@GenomicRegionFeatureDescription(fromType=String.class,toType=Void.class)
public class MultiFeatureMapping extends AbstractFeature<Void> {

	private ArrayList<Predicate<Set<String>>[]> predicates = new ArrayList<Predicate<Set<String>>[]>();
	private ArrayList<Consumer<Set<String>>[]> actions = new ArrayList<Consumer<Set<String>>[]>();

	/**
	 * from Syntax:
	 * Example: ['Human'],['rRNA'],[*]
	 * 
	 * I.e. comma separated list of [] entries describing the input sets; either containing a ; separated list of ' delimited strings
	 * or one of the following: U, N, ?, +, * (corresponding to 1 element, more than one elements, 0 or 1 element, at least 1 element
	 * and any number of elements
	 * 
	 * to Syntax:
	 * Example: ['rRNA'],[],[]
	 * 
	 * Same as from Syntax without these "following" things, except for [*] which does not change contents!
	 * 
	 * @param from
	 * @param to
	 */
	public void addMapping(String from, String to) {
		String[] t = MaskedCharSequence.maskQuotes(to, ' ').splitAndUnmask(',');
//		String[] t = StringUtils.splitAtUnquoted(to, ',');	
	
		Predicate<Set<String>>[] p = GenomicRegionFeature.parseSetConditions(from);
		Consumer<Set<String>>[] a = new Consumer[t.length];
		
				
		
		
		for (int i=0; i<t.length; i++) {
			if (!t[i].startsWith("[") || !t[i].endsWith("]")) throw new RuntimeException("Entries must be enclosed in []");
			String ff = t[i].substring(1, t[i].length()-1).trim();
			if (ff.equals("*")) {
				a[i] = s->{};
			} else {
				HashSet<String> ts = new HashSet<String>();
				for (String e : MaskedCharSequence.maskQuotes(ff, ' ').splitAndUnmask(';') ) {//StringUtils.splitAtUnquoted(ff,';')) {
					if (!e.startsWith("'") || !e.endsWith("'")) throw new RuntimeException("Entries must be quoted!");
					ts.add(e.substring(1, e.length()-1));
				}
				a[i] = s->{s.clear(); s.addAll(ts);};
			}
			 
		}
		
		
		predicates.add(p);
		actions.add(a);
	}
	
	@Override
	public GenomicRegionFeature<Void> copy() {
		MultiFeatureMapping re = new MultiFeatureMapping();
		re.copyProperties(this);
		re.predicates = predicates;
		re.actions = actions;
		return re;
	}
	
	
	@Override
	protected void accept_internal(Set<Void> values) {
		for (int i=0; i<predicates.size(); i++) {
			Predicate<Set<String>>[] p = predicates.get(i);
			boolean allmatch = true;
			for (int j=0; j<p.length; j++) 
				if (!p[j].test(getInput(j))) {
					allmatch = false;
					break;
				}
			
			if (allmatch) {
				Consumer<Set<String>>[] a = actions.get(i);
				for (int j=0; j<a.length; j++)
					a[j].accept(getInput(j));
			}
		}
		
		
	}

	
}
