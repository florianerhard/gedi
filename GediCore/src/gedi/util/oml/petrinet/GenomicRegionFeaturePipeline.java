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
package gedi.util.oml.petrinet;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.oml.OmlInterceptor;
import gedi.util.oml.OmlNode;
import gedi.util.oml.OmlNodeExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

@SuppressWarnings("rawtypes")
public class GenomicRegionFeaturePipeline implements OmlInterceptor {

	
	private static final String INPUT_ATTRIBUTE = "input";
	
	
	private GenomicRegionFeature lastFeature;
	private HashMap<String, String[]> idToInputs = new HashMap<String, String[]>();

	private String[] inputIds;
	
	private LinkedHashMap<GenomicRegionFeature,String[]> features = new LinkedHashMap<GenomicRegionFeature,String[]>();


	private String conditionsFile;


	private int threads=-1;
	private boolean checkSorting = false;


	private String[] labels;
	
	
	public GenomicRegionFeatureProgram getProgram() throws IOException {
		GenomicRegionFeatureProgram re = new GenomicRegionFeatureProgram();
		for (GenomicRegionFeature f : features.keySet())
			re.add(f, features.get(f));
			
		if (conditionsFile!=null)
			re.setConditionFile(conditionsFile);
		if (labels!=null)
			re.setLabels(labels);
		
		if (threads>=0)
			re.setThreads(threads);
		re.setCheckSorting(checkSorting);
		
		return re;
	}
	
	public void setCheckSorting(boolean checkSorting) {
		this.checkSorting = checkSorting;
	}
	
	public void setConditions(String file) {
		this.conditionsFile = file;
	}
	public void setLabels(String[] labels) {
		this.labels = labels;
	}
	
	public void setThreads(int n) {
		this.threads = n;
	}
	
	@Override
	public boolean useForSubtree() {
		return true;
	}
	
	
	
	@Override
	public void setObject(OmlNode node, Object o, String id, String[] classes,
			HashMap<String, Object> context) {
		if (o instanceof GenomicRegionFeature) {
			id = ((GenomicRegionFeature)o).getId();
			if (id!=null && idToInputs.containsKey(id))
				return;
			idToInputs.put(id,inputIds);
			if (id==null) {
				do {
					id = StringUtils.createRandomIdentifier(10);
				} while (idToInputs.containsKey(id));
				idToInputs.put(id, inputIds);
				((GenomicRegionFeature)o).setId(id);
			}
			features.put((GenomicRegionFeature) o, inputIds);
		}
	}
	
	@Override
	public void childProcessed(OmlNode parentNode, OmlNode childNode,
			Object parent, Object child, HashMap<String, Object> context) {
		if (parent instanceof GenomicRegionFeature && child instanceof GenomicRegionFeature && !parentNode.getAttributes().containsKey("input") && parent!=child) {
			
			String id = ((GenomicRegionFeature)child).getId();
			
			// parent is already added so overwrite!
			String[] cinputs = features.remove(parent);
			if (cinputs!=null)
				features.put((GenomicRegionFeature)parent, ArrayUtils.concat(cinputs,new String[]{id}));
			else
				features.put((GenomicRegionFeature)parent, new String[]{id});
			
			
			
		}
	}
	
	@Override
	public LinkedHashMap<String, String> getAttributes(OmlNode node, LinkedHashMap<String, String> attributes,
			HashMap<String, Object> context) {
		
		if (context.containsKey(OmlNodeExecutor.INLINED_CALL)) return attributes;
		
		inputIds = null;
		
		LinkedHashMap<String, String> re = attributes;
		if (re.containsKey(INPUT_ATTRIBUTE)) {
			re = new LinkedHashMap<String, String>(re);
			String inputIdString = re.get(INPUT_ATTRIBUTE);
			inputIds = StringUtils.split(inputIdString,',');
			for (int i=0; i<inputIds.length; i++) {
				if (!idToInputs.containsKey(inputIds[i]))
					throw new RuntimeException("Input with id "+inputIds[i]+" unknown!");
			}
			
			
			
			re.remove(INPUT_ATTRIBUTE);
		}
		
		return re;
	}

	

	
		
		
}
