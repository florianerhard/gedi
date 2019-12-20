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
package gedi.util.io.text.genbank;

import gedi.core.data.annotation.Gff3Element;
import gedi.core.reference.Strand;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GenbankFeature implements Comparable<GenbankFeature> {

	private GenbankFile file;
	private String featureName;
	private GenbankFeaturePosition position;
	private HashMap<String,ArrayList<String>> stringMap;
	private HashMap<String,ArrayList<GenbankFeaturePosition>> posMap;
	private String[] mapLines;

	public GenbankFeature(ReferenceGenomicRegion<Gff3Element> r) {
		this.file = null;
		this.mapLines = null;
		this.featureName = r.getData().getFeature();
		String descr = (1+r.getData().getStart())+".."+r.getData().getEnd();
		this.position = r.getReference().getStrand().equals(Strand.Minus)?new ComplementFeaturePosition(this, "complement("+descr+")"):new SpanFeaturePosition(this, descr);
		this.stringMap = new HashMap<String,ArrayList<String>>();
		this.posMap = new HashMap<String,ArrayList<GenbankFeaturePosition>>();
		for (String s : r.getData().getAttributeNames()) 
			stringMap.put(s,new ArrayList<String>(Arrays.asList(r.getData().getAttribute(s).toString())));
	}
	public GenbankFeature(GenbankFile file, String featureName,
			String position, String[] mapLines) {
		this.file = file;
		this.mapLines = mapLines;
		this.featureName = featureName;
		this.position = parsePosition(position);
		this.stringMap = new HashMap<String,ArrayList<String>>();
		this.posMap = new HashMap<String,ArrayList<GenbankFeaturePosition>>();
		
		for (int i=0; i<mapLines.length; i++) {
			String line = mapLines[i].trim();
			int equalIndex = line.indexOf('=');
			
			if (!line.startsWith("/") || equalIndex<=0){
//				Logger.getLogger(LoggerConfig.DESIGN).log(Level.WARNING,"Not a map line!",line);
			} else {
				line = line.substring(1);
				String key = line.substring(0,equalIndex-1);
				String val = line.substring(equalIndex);
				if (val.startsWith("\"")) {
					StringBuilder sb = new StringBuilder();
					val = val.substring(1);
					for (; val.indexOf('"')<0; val = mapLines[++i].trim()) {
						sb.append(val);
					}
					if (!val.endsWith("\"")){
//						Logger.getLogger(LoggerConfig.DESIGN).log(Level.WARNING,"Could not parse multi line map element correctly!",line);
					}else
						sb.append(val.substring(0,val.length()-1));
					

					stringMap.computeIfAbsent(key, k->new ArrayList<>()).add(sb.toString());
				} else {
					GenbankFeaturePosition p = parsePosition(val);
					if (p!=null)
						posMap.computeIfAbsent(key, k->new ArrayList<>()).add(parsePosition(val));
					else
						stringMap.computeIfAbsent(key, k->new ArrayList<>()).add(val);
				}
				
			}
		}
	}

	public HashMap<String,String> toSimpleMap() {
		HashMap<String,String> re = new HashMap<String,String>();
		for (String k : stringMap.keySet())
			re.put(k,StringUtils.concat("; ", stringMap.get(k)));
		return re;
	}
	
	public Set<String> getKeys() {
		return stringMap.keySet();
	}

	public GenbankFile getFile() {
		return file;
	}

	public String getFeatureName() {
		return featureName;
	}

	public GenbankFeaturePosition getPosition() {
		return position;
	}

	public String getStringValue(String key) {
		return stringMap.containsKey(key)?stringMap.get(key).iterator().next():null;
	}
	
	/**
	 * keys are a prioritized list, i.e. the first matching key is used
	 * @param key
	 * @return
	 */
	public String getStringValue(String[] keys) {
		for (int i=0; i<keys.length; i++)
			if (stringMap.containsKey(keys[i])) 
				return stringMap.get(keys[i]).iterator().next();
		return null;
	}
	
	public GenbankFeaturePosition getPositionValue(String key) {
		return posMap.containsKey(key)?posMap.get(key).iterator().next():null;
	}
	
	public Collection<String> getStringValues(String key) {
		return stringMap.containsKey(key)?stringMap.get(key):null;
	}
	
	public Collection<GenbankFeaturePosition> getPositionValues(String key) {
		return posMap.containsKey(key)?posMap.get(key):null;
	}
	

	private static final Pattern posRegex = Pattern.compile("pos:([^,]*)(,|$)");

	GenbankFeaturePosition parsePosition(String position) {
		Matcher ma = posRegex.matcher(position);
		if (ma.find())
			position = ma.group(1);
		if (position.startsWith(ComplementFeaturePosition.prefix))
			return new ComplementFeaturePosition(this,position);
		else if (position.startsWith(JoinedFeaturePosition.prefix)||position.startsWith(JoinedFeaturePosition.prefix2))
			return new JoinedFeaturePosition(this,position);
		else if (position.contains(".."))
			return new SpanFeaturePosition(this,position);
		else if (StringUtils.isNumeric(position))
			return new SingleFeaturePosition(this,position);
		else
			return null;
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("FEATURE[");
		sb.append(featureName);
		sb.append(" ");
		sb.append(position.toString());
		sb.append("]");
		return sb.toString();
	}
	
	public String getGenbankEntry() {
		if (mapLines==null)
			throw new RuntimeException("Cannot generate genbank entry when gff3 was given!");
		return StringUtils.concat("\n", mapLines);
	}
	@Override
	public int compareTo(GenbankFeature o) {
		int re = position.getStrand().compareTo(o.position.getStrand());
		if (re==0)
			re = position.toGenomicRegion().compareTo(o.position.toGenomicRegion());
		if (re==0)
			re = featureName.compareTo(featureName);
		return re;
	}
	
}
