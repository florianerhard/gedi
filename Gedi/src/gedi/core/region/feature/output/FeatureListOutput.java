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

package gedi.core.region.feature.output;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.core.region.feature.features.AbstractFeature;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.function.BiFunction;


@GenomicRegionFeatureDescription(toType=Void.class)
public class FeatureListOutput extends AbstractFeature<Void> {

	private String multiSeparator = ",";
	private BiFunction<Object,NumericArray,NumericArray> dataToCounts;
	private int decimals=2;
	private double minimalCount = 0;

	private LineOrientedFile out;
	private NumericArray buffer;
	
	
	public FeatureListOutput(String file) {
		minValues = maxValues = 0;
		setFile(file);
	}
	
	public void setFile(String path) {
		setId(path);
	}
	
	public boolean dependsOnData() {
		return true;
	}
	
	
	@Override
	public GenomicRegionFeature<Void> copy() {
		FeatureListOutput re = new FeatureListOutput(getId());
		re.copyProperties(this);
		re.multiSeparator = multiSeparator;
		re.dataToCounts = dataToCounts;
		re.decimals = decimals;
		re.minimalCount = minimalCount;
		return re;
	}
	

	public void setMultiSeparator(String multiSeparator) {
		this.multiSeparator = multiSeparator;
	}
	
	
	public void setDecimals(int decimals) {
		this.decimals = decimals;
	}
	
	public int getDecimals() {
		return decimals;
	}
	
	public void setMinimalCount(double minimalCount) {
		this.minimalCount = minimalCount;
	}
	
	public void setDataToCounts(
			BiFunction<Object, NumericArray, NumericArray> dataToCounts) {
		this.dataToCounts = dataToCounts;
	}
	
	@Override
	public void produceResults(GenomicRegionFeature<Void>[] o){
	
		if (program.isRunning()) return;
		
		try {
			out = new LineOrientedFile(getId());
			out.startWriting();
			writeHeader(out);
			for (GenomicRegionFeature<Void> a :o) {
				FeatureListOutput x = (FeatureListOutput) a;
				LineIterator it = x.out.lineIterator();
				while (it.hasNext()) 
					out.writeLine(it.next());
			}
			out.finishWriting();
		} catch (IOException e) {
			throw new RuntimeException("Could not merge output files!",e);
		}
		
	}
	
	@Override
	protected void accept_internal(Set<Void> values) {
		try {
			
			buffer = dataToCounts==null?program.dataToCounts(referenceRegion.getData(), buffer):dataToCounts.apply(referenceRegion.getData(), buffer);
			
			
			if (out==null) {
				File main = new File(getId());
				
				out = new LineOrientedFile(File.createTempFile(main.getName(), ".tmp", main.getParentFile()).getPath());
				out.startWriting();
			}
			
			if (isOutput(buffer)) {
				out.writef("%s:%s",referenceRegion.getReference().toPlusMinusString(),referenceRegion.getRegion().toRegionString());
				for (int i=0; i<inputs.length; i++) {
					Set<?> s = getInput(i);
					out.writef("\t%s",StringUtils.concat(multiSeparator, s));
				}
				for (int i=0; i<buffer.length(); i++)
					out.writef("\t%s",buffer.formatDecimals(i,decimals));
				
				out.writeLine();
			}
			
		} catch (IOException e) {
			throw new RuntimeException("Cannot write output file!",e);
		}
	}
	
	private void writeHeader(LineOrientedFile out) throws IOException {
		out.writef("Genomic position");
		for (int i=0; i<inputs.length; i++) 
			out.writef("\t%s",inputNames[i]);
		
		int l = buffer.length();
		if (program.getLabels()!=null && program.getLabels().length==l)
			for (int i=0; i<program.getLabels().length; i++) 
				out.writef("\t%s",program.getLabels()[i]);
		else {
			
			for (int i=0; i<l; i++) 
				out.writef("\t%d",i);
		}
		out.writeLine();
	}

	private boolean isOutput(NumericArray a) {
		for (int i=0; i<a.length(); i++)
			if (a.getDouble(i)>=minimalCount)
				return true;
		return false;
	}

	
	@Override
	public void end() {
		super.end();

		try {
			out.finishWriting();
		} catch (IOException e) {
			throw new RuntimeException("Cannot write output file!",e);
		}
		
		
	}


}

