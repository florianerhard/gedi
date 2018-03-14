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
package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.Transcript;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.GeneralUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.GenomicExonsTsvFileReader;
import gedi.util.parsing.IntegerParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.ToIntBiFunction;

public class GffExonFileReader extends GenomicExonsTsvFileReader<Transcript> {

	
	
	

	public GffExonFileReader(String path) throws IOException {
		super(path,false,"\t",getRef(),getStart(),getEnd(),getData(),getComp(),init(), Transcript.class);
			
		idgetter = d->d.getTranscriptId();
		readCoordinates= (h,f)->f[2].equals("exon");
		mergeOverlap = true;
		
		lineChecker = check().fieldCount(9)
				.fieldType(3, new IntegerParser())
				.fieldType(4, new IntegerParser())
				.fieldContent(8, f->getGtfField("Parent",f)==null?"Cannot find Parent in attributes":null);
	}
	

	private static Consumer<HeaderLine> init() {
		return null;
	}


	private static BiPredicate<String[],String[]> getComp() {
		return (a,b)->GeneralUtils.isEqual(getGtfField("Parent",a),getGtfField("Parent",b));
	}


	private static ToIntBiFunction<HeaderLine, String[]> getEnd() {
		return (h,f)-> {
			return Integer.parseInt(f[4]);
		};
	}


	private static ToIntBiFunction<HeaderLine, String[]> getStart() {
		return (h,f)-> {
			return Integer.parseInt(f[3])-1;
		};
	}


	private static BiFunction<HeaderLine, String[], ReferenceSequence> getRef() {
		return (h,f)-> {
			return Chromosome.obtain(f[0],f[6]);
		};
	}
	
	private static final String getGtfField(String name, String[] f) {
		return getGtfField(name, f[8],null);
	}
	
	public static final String getGtfField(String name, String f8) {
		return getGtfField(name, f8, null);
	}
	
	private static final String getGtfField(String name, String[] f, String def) {
		return getGtfField(name, f[8],def);
	}
	
	public static final String getGtfField(String name, String f8, String def) {
		String[] f = StringUtils.split(f8, ';');
		for (int i=0; i<f.length; i++) {
			if (f[i].contains(name))
				return StringUtils.trim(f[i].substring(f[i].indexOf(name)+name.length()), ' ','"','=');
		}
		return def;
	}


	private static BiFunction<HeaderLine, String[][], Transcript> getData() {
		return (h,tr)->{
			int codingStart = -1;
			int codingEnd = -1;
			Transcript re = new Transcript(
					getGtfField("Parent", tr[0]),
					getGtfField("Parent", tr[0]),
					codingStart,codingEnd
					);
			return re;
		};
	}

}
