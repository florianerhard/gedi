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

package gedi.oml.petrinet;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMappingJob;
import gedi.core.data.mapper.MutableDemultiplexMapper;
import gedi.gui.genovis.VisualizationTrack;
import gedi.oml.OmlInterceptor;
import gedi.oml.OmlNode;
import gedi.oml.OmlNodeExecutor;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.job.PetriNet;
import gedi.util.job.Place;
import gedi.util.job.Transition;
import gedi.util.mutable.Mutable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

@SuppressWarnings({"unchecked","rawtypes"})
public class Pipeline implements OmlInterceptor {

	
	private static final String INPUT_ATTRIBUTE = "input";
	private static final String INPUT_SETTER = "setInput";
	
	private ArrayList<VisualizationTrack> tracks = new ArrayList<VisualizationTrack>();
	private PetriNet pn;
	
//	private Place lastPlace;
	private HashMap<String, Place> idToPlace = new HashMap<String, Place>();

	private Place[] inputs;
	private String inputIds;
	
	public Pipeline() {
		pn = new PetriNet();
	}

	public PetriNet getPetriNet() {
		if (!pn.isPrepared())
			pn.prepare();
		return pn;
	}
	
	
	public ArrayList<VisualizationTrack> getTracks() {
		return tracks;
	}
	
	@Override
	public boolean useForSubtree() {
		return true;
	}
	
	private HashMap<Class,ArrayList> objects = new HashMap<Class, ArrayList>();
	
	public <T> ArrayList<T> getObjects(Class<T> cls) {
		if (!objects.containsKey(cls)) return new ArrayList();
		return objects.get(cls);
	}

	public void add(GenomicRegionDataMapper o) {
		if (o instanceof VisualizationTrack)
			tracks.add((VisualizationTrack<?,?>) o);
	}
	
	@Override
	public void setObject(OmlNode node, Object obj, String id, String[] classes,
			HashMap<String, Object> context) {
		
		
		
		if (obj instanceof GenomicRegionDataMapper) {
			
			GenomicRegionDataMapper o = (GenomicRegionDataMapper) obj;
			
			ArrayList l = objects.get(o.getClass());
			if (l==null) objects.put(o.getClass(), l = new ArrayList());
			l.add(o);
			
			GenomicRegionDataMappingJob job = new GenomicRegionDataMappingJob(o);
			
			if (job.isTupleInput()) 
				job.setTupleSize(inputs.length);
			
			Transition t = pn.createTransition(job);
			Place p = pn.createPlace(job.getOutputClass());
			pn.connect(t, p);
			
			idToPlace.put(id,p);
			((GenomicRegionDataMappingJob) p.getProducer().getJob()).setId(id);
			
			if (inputs!=null) {
				for (int i=0; i<inputs.length; i++)
					pn.connect(inputs[i], t, i);
			}
			if (inputIds!=null) {
				Method setter = ReflectionUtils.findMethod(o, INPUT_SETTER,String.class);
				if (setter!=null)
					try {
						setter.invoke(o, inputIds);
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						throw new RuntimeException("Could not execute setter: "+setter,e);
					}
			}
		}
	}
	
	
	
	@Override
	public LinkedHashMap<String, String> getAttributes(OmlNode node, LinkedHashMap<String, String> attributes,
			HashMap<String, Object> context) {
		
		if (context.containsKey(OmlNodeExecutor.INLINED_CALL)) return attributes;
		
		inputs = null;
		inputIds = null;
		
		LinkedHashMap<String, String> re = attributes;
		if (re.containsKey(INPUT_ATTRIBUTE)) {
			re = new LinkedHashMap<String, String>(re);
			this.inputIds = re.get(INPUT_ATTRIBUTE);
			String[] inputIds = StringUtils.split(this.inputIds,',');
			inputs = new Place[inputIds.length];
			for (int i=0; i<inputs.length; i++) {
				inputs[i] = idToPlace.get(inputIds[i]);
				int mi = getMutableIndex(inputIds[i]);
				if (inputs[i]==null && mi!=-1) {
					
					// add <MutableDemultiplexMapper input="pref" index="mi" id="pref[mi]" />
					
					String pref = inputIds[i].substring(0, inputIds[i].lastIndexOf('['));
					String full = inputIds[i];
					if (idToPlace.containsKey(pref) && Mutable.class.isAssignableFrom(idToPlace.get(pref).getTokenClass())) {
						MutableDemultiplexMapper m = new MutableDemultiplexMapper(mi);
						
						Place[] inputssave = this.inputs;
						String inputIdssave = this.inputIds;
						this.inputIds = pref;
						this.inputs = new Place[] {idToPlace.get(pref)};
						
						setObject(null, m, full, null, null);
						
						this.inputIds = inputIdssave;
						this.inputs = inputssave;
						this.inputs[i] = idToPlace.get(inputIds[i]);
						
					}
				}
				if (inputs[i]==null)
					throw new RuntimeException("Input with id "+inputIds[i]+" unknown!");
			}
			
			
			
			re.remove(INPUT_ATTRIBUTE);
		}
		
		return re;
	}

	private int getMutableIndex(String id) {
		if (!id.endsWith("]")) return -1;
		int s = id.lastIndexOf('[');
		if (s==-1) return -1;
		String n = id.substring(s+1, id.length()-1);
		if (StringUtils.isInt(n))
			return Integer.parseInt(n);
		return -1;
	}

	

	
		
		
}
