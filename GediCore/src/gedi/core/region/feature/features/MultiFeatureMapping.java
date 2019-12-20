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
package gedi.core.region.feature.features;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


@GenomicRegionFeatureDescription(fromType=String.class,toType=Void.class)
public class MultiFeatureMapping extends AbstractFeature<Void> {

	private ArrayList<Predicate<Set<String>>[]> predicates = new ArrayList<Predicate<Set<String>>[]>();
	private ArrayList<BiConsumer<MultiFeatureMapping,Set<Object>>[]> actions = new ArrayList<BiConsumer<MultiFeatureMapping,Set<Object>>[]>();

	private boolean applyAll = false;
	
	/**
	 * from Syntax:
	 * Example: ['Human'],['rRNA'],[*]
	 * 
	 * I.e. comma separated list of [] entries describing the input sets; either containing a ; separated list of ' delimited strings
	 * or one of the following: nothing, U, N, ?, +, * (corresponding to 0 elements, 1 element, more than one elements, 0 or 1 element, at least 1 element
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
		BiConsumer<MultiFeatureMapping,Set<Object>>[] a = new BiConsumer[t.length];
		
		
		
		for (int i=0; i<t.length; i++) {
			if (!t[i].startsWith("[") || !t[i].endsWith("]")) throw new RuntimeException("Entries must be enclosed in []");
			String ff = t[i].substring(1, t[i].length()-1).trim();
			if (ff.equals("*")) {
				a[i] = (f,s)->{};
			}
			else {
				
				HashSet<Function<MultiFeatureMapping,ExtendedIterator<Object>>> ts = new HashSet<>();
				
				for (String e : MaskedCharSequence.maskQuotes(ff, ' ').splitAndUnmask(';') ) {//StringUtils.splitAtUnquoted(ff,';')) {
					if (!e.startsWith("'") || !e.endsWith("'")) {
						ts.add((f)->EI.wrap(f.getInput(e)));
					} else {
						ts.add((f)->EI.singleton(e.substring(1, e.length()-1)));
					}
				}
				a[i] = (f,s)->{
						s.clear(); 
						for (Function<MultiFeatureMapping,ExtendedIterator<Object>> tss : ts) 
							tss.apply(f).toCollection(s);
						};
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
		re.applyAll = applyAll;
		return re;
	}
	
	public void setApplyAll() {
		this.applyAll = true;
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
				BiConsumer<MultiFeatureMapping,Set<Object>>[] a = actions.get(i);
				for (int j=0; j<a.length; j++)
					a[j].accept(this,getInput(j));
				if (!applyAll)
					return;
			}
		}
		
		
	}

	
}
