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
package gedi.core.data.annotation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.StringUtils;
import gedi.util.functions.TriConsumer;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.text.HeaderLine;

public class Gff3Element implements GenomicRegion, AttributesProvider,ScoreProvider {

	
	
	
	private String source;
	private String feature;
	private int start;
	private int end;
	private double score;
	private int frame;
	private HashMap<String,String> attributes = new HashMap<String, String>();
	

	@Override
	public int getNumParts() {
		return 1;
	}

	@Override
	public int getStart(int part) {
		return start;
	}

	@Override
	public int getEnd(int part) {
		return end;
	}

	@Override
	public double getScore() {
		return score;
	}
	

	public String getSource() {
		return source;
	}

	public String getFeature() {
		return feature;
	}

	public int getFrame() {
		return frame;
	}

	@Override
	public Set<String> getAttributeNames() {
		return Collections.unmodifiableSet(attributes.keySet());
	}


	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}
	
	
	
	public static class Gff3ElementParser implements TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<Gff3Element>> {

		
		private HashSet<String> features;
		
		public Gff3ElementParser() {
			
		}
		
		public Gff3ElementParser(String[] features) {
			this.features = new HashSet<String>(Arrays.asList(features));
		}
		
		@Override
		public void accept(HeaderLine a, String[] fields,
				MutableReferenceGenomicRegion<Gff3Element> box) {
			
			if (features!=null && !features.contains(fields[2])){
				box.set(null,null,(Gff3Element)null);
				return;
			}
			
			ReferenceSequence reference = Chromosome.obtain(fields[0],fields[6]);
			Gff3Element e = new Gff3Element();
			e.source = fields[1];
			e.feature = fields[2];
			e.start = Integer.parseInt(fields[3])-1;
			e.end = Integer.parseInt(fields[4]);
			e.score = fields[5].equals(".")?Double.NaN:Double.parseDouble(fields[5]);
			e.frame = fields[7].equals(".")?-1:Integer.parseInt(fields[7]);
			for (String p : StringUtils.split(fields[8], ';')) {
				p = StringUtils.trim(p);
				if (p.length()==0) continue;
				String[] kv = StringUtils.split(p, '=');
				if (kv.length!=2) throw new RuntimeException("Not a key-value pair: "+p);
				try {
					e.attributes.put(URLDecoder.decode(kv[0],"UTF-8"),URLDecoder.decode(kv[1],"UTF-8"));
				} catch (UnsupportedEncodingException ex) {
					throw new RuntimeException("Could not decode key-value pair: "+p,ex);
				}
			}
			box.set(reference, e, e);
		}
		
	}


	@Override
	public String toString() {
		StringBuilder attr = new StringBuilder();
		for (String k : attributes.keySet()) {
			if (attr.length()>0) attr.append(";");
			try {
				attr.append(URLEncoder.encode(k,"UTF8"));
				attr.append("=");
				attr.append(URLEncoder.encode(attributes.get(k),"UTF8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Could not encode key-value pair: "+k,e);
			}
		}
			
		return String.format(Locale.US, ".\t%s\t%s\t%d\t%d\t%.3f\t.\t%s\t%s",source,feature,start+1,end,score,frame==-1?".":frame,attr);
	}
	
	public String toString(ReferenceSequence reference) {
		StringBuilder attr = new StringBuilder();
		for (String k : attributes.keySet()) {
			if (attr.length()>0) attr.append(";");
			try {
				attr.append(URLEncoder.encode(k,"UTF8"));
				attr.append("=");
				attr.append(URLEncoder.encode(attributes.get(k),"UTF8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Could not encode key-value pair: "+k,e);
			}
		}
			
		return String.format(Locale.US, "%s\t%s\t%s\t%d\t%d\t%.3f\t%s\t%s\t%s",reference.getName(),source,feature,start+1,end,score,reference.getStrand().getGff(),frame==-1?".":frame,attr);
	}
	
}

