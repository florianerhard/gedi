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


import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 * Genbank file
 * 
 * @author erhard
 *
 */
public class GenbankFile extends LineOrientedFile {

	private Integer positionBase;
	private Integer originWidth;
	private String source;
	
	private String[] features;
	
	public GenbankFile(String path) throws IOException {
		super(path);
	}
	
	
	public String[] loadFeatures() throws IOException {
		if (features==null) {
			ExtendedIterator<String> it = lineIterator().skip(l->!l.startsWith("FEATURES")).skip(1);
			List<String> f = new ArrayList<String>();
			
			while (it.hasNext()) {
				String line = it.next();
				if (!Character.isWhitespace(line.charAt(0)))
					break;
				f.add(line);
			}
			features = f.toArray(new String[f.size()]);
		}
		return features;
	}
	
	@Override
	public void loadIntoMemory() throws IOException {
		super.loadIntoMemory();
		loadFeatures();
		getSource();
	}

	
	private void determineSequenceProperties() throws IOException {
		if (positionBase==null && originWidth==null) {
			ExtendedIterator<String> it = lineIterator().skip(l->!l.startsWith("ORIGIN")).skip(1);
			
			
			while (it.hasNext()) {
				SequenceLine current = new SequenceLine(it.next());
				
				if (positionBase!=null && originWidth==null && current.getStart()!=positionBase.intValue()) {
					originWidth = current.getStart()-positionBase;
					return;
				}
				if (positionBase==null)
					positionBase = current.getStart();
			}
		}
	}
	
	/**
	 * Equal to getSource().substring(start,stop) (but does not require that much memory!)
	 * @param start
	 * @param stop
	 * @return
	 * @throws IOException 
	 */
	public String getSource(int start, int stop) throws IOException {
		determineSequenceProperties();
		
		ExtendedIterator<String> it = lineIterator().skip(l->!l.startsWith("ORIGIN")).skip(1);
		
		for (int i=0; i<(start-positionBase)/originWidth; i++, it.next());

		StringBuilder sb = new StringBuilder();
		while (it.hasNext()) {
			SequenceLine sl = new SequenceLine(it.next());
			int cur = sl.getStart()-positionBase;
			int next = cur+originWidth;
			
			int from = start<=cur? 0 : start-cur;
			int to = stop>=next ? originWidth : stop-cur;
			if (to>sl.getSequence().length()) {
//				Logger.getLogger(LoggerConfig.DESIGN).log(Level.WARNING,"Feature is longer than sequence!",stop);
				sb.append(sl.getSequence().substring(from));
				break;
			}
			sb.append(sl.getSequence().substring(from,to));
			if (stop<=next)
				break;
		}
		
		return sb.toString();
	}
	
	

	public String getSource() throws IOException {
		if (source==null) {
			super.loadIntoMemory();
			int sourceLine = ArrayUtils.find(lines,l->l.startsWith("ORIGIN"))+1;
			String line;
			int numberIndex;
			StringBuilder sb = new StringBuilder((int) this.length());
			for (;sourceLine<lines.length && !lines[sourceLine].startsWith("//"); sourceLine++) {
				line = lines[sourceLine].trim();
				// remove leading number (=position) and warn if not matching!
				numberIndex = line.indexOf(' ');
				
				if (numberIndex==-1) {
//					Logger.getLogger(LoggerConfig.DESIGN).log(Level.WARNING,"Could not parse origin line, no space after sequence position!",line);
				}else {
					String number = line.substring(0,numberIndex);
//					if (!StringUtils.isNumeric(number))
//						Logger.getLogger(LoggerConfig.DESIGN).log(Level.WARNING,"Could not parse origin line, no sequence position found!",line);
//					else if (Integer.parseInt(number)-1 !=sb.length())
//						Logger.getLogger(LoggerConfig.DESIGN).log(Level.WARNING,"Origin position does not match the parsed characters!",line);
					
					if (positionBase==null)
						positionBase = Integer.parseInt(number);
					
					if (StringUtils.isNumeric(number))
						line = line.substring(numberIndex+1);
				}
				
				// cut out spaces
				int p = 0;
				for (int spaceIndex=line.indexOf(' '); spaceIndex>=0; spaceIndex = line.substring(p).indexOf(' ')) {
						sb.append(line.substring(p,p+spaceIndex));
					p += spaceIndex+1;
				}
				sb.append(line.substring(p));
			}
			source = sb.toString();
		}
		return source;
	}
	
	private static class SequenceLine {
		
		private int start=-1;
		private String sequence;
		
		public SequenceLine(String line) {
			line = line.trim();
			// remove leading number (=position) and warn if not matching!
			int numberIndex = line.indexOf(' ');
			
			if (numberIndex==-1) {
//				Logger.getLogger(LoggerConfig.DESIGN).log(Level.WARNING,"Could not parse origin line, no space after sequence position!",line);
			}else {
				String number = line.substring(0,numberIndex);
				if (!StringUtils.isNumeric(number)){
//					Logger.getLogger(LoggerConfig.DESIGN).log(Level.WARNING,"Could not parse origin line, no sequence position found!",line);
				}else
					start = Integer.parseInt(number);
				
				if (start>=0)
					line = line.substring(numberIndex+1);
			}
			
			StringBuilder sb = new StringBuilder();
			// cut out spaces
			int p = 0;
			for (int spaceIndex=line.indexOf(' '); spaceIndex>=0; spaceIndex = line.substring(p).indexOf(' ')) {
					sb.append(line.substring(p,p+spaceIndex));
				p += spaceIndex+1;
			}
			sb.append(line.substring(p));
			
			sequence = sb.toString();
		}
		
		public String getSequence() {
			return sequence;
		}
		
		public int getStart() {
			return start;
		}
		
		
	}
	
	public ExtendedIterator<GenbankFeature> featureIterator(String...featureNames) throws IOException {
		return new GenbankFeatureIterator(this,featureNames);
	}
	
	public GenbankFeature getFirstEntry(String featureName) throws IOException {
		Iterator<GenbankFeature> it = featureIterator(featureName);
		return it.hasNext() ? it.next() : null;
	}


	public String getOrganism() throws IOException {
		GenbankFeature s = getFirstEntry("source");
		if (s==null)
			return "";
		return s.getStringValue("organism");
	}
	
	public String getAccession() throws IOException {
		Iterator<String> it = lineIterator();
		while (it.hasNext()) {
			String line = it.next();
			if (line.startsWith("ACCESSION"))
				return line.substring("ACCESSION".length()).trim();
		}
		return "";
	}
}
