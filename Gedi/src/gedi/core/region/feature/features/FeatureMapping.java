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

import gedi.core.genomic.Genomic;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.util.StringUtils;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@GenomicRegionFeatureDescription(fromType=String.class,toType=Object.class)
public class FeatureMapping extends AbstractFeature<Object> {

	public enum ChainMode {
		None, FirstForEach, FirstForAny
	}
	
	private ChainMode chainMode = ChainMode.None;
	// Each mapper must add all its result to the set and return the number of added elements
	private ArrayList<ToIntBiFunction<Set<Object>,String>> mapper = new ArrayList<ToIntBiFunction<Set<Object>,String>>();
	
	public FeatureMapping() {
		minInputs = maxInputs = 1;
	}

	@Override
	public GenomicRegionFeature<Object> copy() {
		FeatureMapping re = new FeatureMapping();
		re.copyProperties(this);
		re.chainMode = chainMode;
		re.mapper = mapper;
		return re;
	}
	
	public void setChainMode(ChainMode chainMode) {
		this.chainMode = chainMode;
	}
	
	public void addRegexMapping(String pattern, String to) {
		Pattern p = Pattern.compile(pattern);
		mapper.add((a,s)->{
			Matcher m = p.matcher(s);
			if (m.find()) {
				a.add(to);
				return 1;
			}
			return 0;
		});
	}
	
	public void addContainsMapping(String from, String to) {
		mapper.add((a,s)->{
			if (s.contains(from)) {
				a.add(to);
				return 1;
			}
			return 0;
		});
	}
	
	public void addEqualsMapping(String from, String to) {
		mapper.add((a,s)->{
			if (s.equals(from)) {
				a.add(to);
				return 1;
			}
			return 0;
		});
	}
	
	public void addDefault(String to) {
		mapper.add((a,s)->{
			if (a.isEmpty())
				a.add(to);
			return 1;
		});
	}
	
	
	public void addTable(String file, String separator, String from, String to) throws IOException {
		LineIterator it = new LineOrientedFile(file).lineIterator();
		HeaderLine header = new HeaderLine(it.next(),separator);
		addTable(it, separator, header.get(from), header.get(to));
	}
	
	public void addTable(String file, String separator, boolean header, int from, int to) throws IOException {
		LineIterator it = new LineOrientedFile(file).lineIterator();
		if (header) it.next();
		addTable(it, separator, from, to);
	}
	public void addTable(String file, String from, String to) throws IOException {
		addTable(file,"\t", from, to);
	}
	
	public void addTable(String file) throws IOException {
		addTable(file, "\t", false, 0, 1);
	}
	
	
	public void addTable(String file, boolean header, int from, int to) throws IOException {
		addTable(file, "\t", header, from, to);
	}
	
	private void addTable(ExtendedIterator<String> it, String separator, int from, int to) {
		HashMap<String,TreeSet<String>> map = new HashMap<String, TreeSet<String>>(); 
		
		for (String l : it.loop()) {
			String f = StringUtils.splitField(l, separator, from);
			String t = StringUtils.splitField(l, separator, to);
			TreeSet<String> set=map.computeIfAbsent(f, (x)->new TreeSet<String>());
			if (chainMode==ChainMode.None || set.isEmpty()) 
				set.add(t);
		}

		mapper.add((s,k)->{
			TreeSet<String> m = map.get(k);
			if (m==null) return 0;
			s.addAll(m);
			return m.size();
		});
	}
	
	public void addTranscripts(Genomic g, String targetProperty) {
		Function<String, String> tab = g.getTranscriptTable(targetProperty);
		mapper.add((s,k)->{
			Object val = tab.apply(k);
			if (val==null) return 0;
			s.add(val);
			return 1;
		});
	}
	
	public void addGenes(Genomic g, String targetProperty) {
		Function<String, String> tab = g.getGeneTable(targetProperty);
		mapper.add((s,k)->{
			Object val = tab.apply(k);
			if (val==null) return 0;
			s.add(val);
			return 1;
		});
	}
	

	@Override
	protected void accept_internal(Set<Object> values) {
		switch (chainMode) {
		case None:
			for (String inp : this.<String>getInput(0)) {
				for (ToIntBiFunction<Set<Object>,String> m : mapper) {
					m.applyAsInt(values,inp);
				}
			}
			break;
		case FirstForEach:
			for (String inp : this.<String>getInput(0)) {
				for (ToIntBiFunction<Set<Object>,String> m : mapper) {
					if (m.applyAsInt(values,inp)>0)
							break;
				}
			}
			break;
		case FirstForAny:
			for (ToIntBiFunction<Set<Object>,String> m : mapper) {
				for (String inp : this.<String>getInput(0)) {
					if (m.applyAsInt(values,inp)>0)
							return;
				}
			}
			break;
		}
		
		
		
	}

	
}
