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

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.r.RRunner;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


public abstract class AbstractFeature<O> implements GenomicRegionFeature<O>{

	private String id = null;


	protected int minValues = 0;
	protected int maxValues = Integer.MAX_VALUE;	
	protected int minInputs = 0;
	protected int maxInputs = Integer.MAX_VALUE;
	
	protected String[] inputNames;
	protected HashMap<String,Integer> inputNamesIndex;
	protected GenomicRegionFeatureProgram program;
	protected ArrayList<Function> postprocessors = new ArrayList<Function>();
	protected Predicate<GenomicRegionFeature<O>> condition;

	protected Set[] inputs;
	protected MutableReferenceGenomicRegion<Object> referenceRegion = new MutableReferenceGenomicRegion<Object>();
	
	protected ArrayList<Consumer<GenomicRegionFeature<O>>> finishActions = new ArrayList<Consumer<GenomicRegionFeature<O>>>();
	
	protected boolean dependsOnData = false;
	
		
	public MutableReferenceGenomicRegion<Object> getReferenceRegion() {
		return referenceRegion;
	}
	
	protected void copyProperties(AbstractFeature<O> from) {
		this.id = from.id;
		this.minValues = from.minValues;
		this.maxValues = from.maxValues;
		this.minInputs = from.minInputs;
		this.maxInputs = from.maxInputs;
		this.inputNames = from.inputNames;
		this.inputNamesIndex = from.inputNamesIndex;
		this.program = from.program;
		this.postprocessors = from.postprocessors;
		this.condition = from.condition;
		this.inputs = new Set[from.inputs.length];
		this.dependsOnData = from.dependsOnData;
	}
	
	@Override
	public String toString() {
		return getId();
	}
	
	@Override
	public boolean dependsOnData() {
		return dependsOnData;
	}
	
	@Override
	public GenomicRegionFeature<O> addCondition(Predicate<GenomicRegionFeature<O>> condition) {
		if (this.condition==null)
			this.condition = condition;
		else 
			this.condition = this.condition.and(condition);
		return this;
	}
	
	@Override
	public boolean hasCondition() {
		return condition!=null;
	}
	
	@Override
	public final void accept(Set<O> t) {
		if (this.condition==null || this.condition.test(this))
			accept_internal(t);
	}
	
	protected abstract void accept_internal(Set<O> t);

	@Override
	public void setProgram(GenomicRegionFeatureProgram program) {
		if (this.program!=null) 
			throw new RuntimeException("This feature is already part of a program!");
		
		this.program = program;
	}
	
	public void addFinishAction(Consumer<GenomicRegionFeature<O>> action) {
		finishActions.add(action);
	}
	
	public void addRscript(String url) {
		
		addFinishAction(f->{
			try {
				URL u = new URL(url);
				RRunner r = new RRunner(getId()+"."+FileUtils.getNameWithoutExtension(u.getPath())+".R");
				r.set("id",getId());
				r.set("file",getId());
				r.addSource(u.openStream());
				r.run(true);
			} catch (IOException e) {
				throw new RuntimeException("Could not run R script "+url,e);
			}
		});
		
	}
	
	
	@Override
	public GenomicRegionFeatureProgram getProgram() {
		return program;
	}
	
	@Override
	public void setInputNames(String[] inputNames) {
		int inputs = inputNames.length;
		if (inputs<minInputs || inputs>maxInputs) throw new RuntimeException("Illegal number of inputs for "+getId()+" min="+minInputs+" max="+maxInputs+" inputs="+inputs);
		this.inputNames = inputNames;
		this.inputNamesIndex = ArrayUtils.createIndexMap(inputNames);
		this.inputs = new Set[inputs];
	}
	
	@Override
	public <I> Set<I> getInput(String name) {
		return inputs[inputNamesIndex.get(name)];
	}
	
	public <I> Set<I> getInput(int i) {
		return inputs[i];
	}
	
	@Override
	public String getInputName(int index) {
		return inputNames[index];
	}

	@Override
	public <T> GenomicRegionFeature<O> addFunction(Function<O, T> function) {
		Objects.requireNonNull(function, "Function is null in "+getId());
		postprocessors.add(function);
		return this;
	}
	
	public int getInputLength() {
		return inputs.length;
	}
	
	@Override
	public void setData(Object data) {
		this.referenceRegion.setData(data);
	}
	
	
	@Override
	public void begin() {
	}
	
	@Override
	public void end() {
		for (Consumer<GenomicRegionFeature<O>> a : finishActions)
			a.accept(this);
	}
	
	@Override
	public GenomicRegionFeature<O> setId(String id) {
		this.id = id;
		return this;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int getMinValues() { 
		return minValues; 
	}
	@Override
	public int getMaxValues() { 
		return maxValues; 
	}
	
	@Override
	public boolean setGenomicRegion(ReferenceSequence reference,
			GenomicRegion region) {
		if (this.referenceRegion.getReference()==reference && this.referenceRegion.getRegion()==region) return false;
		
		this.referenceRegion.setReference(reference).setRegion(region);
		return true;
	}
	
	@Override
	public <I> void setInput(int index, Set<I> input) {
		inputs[index] = input;
	}
	
	
	private Object[] commandBuffer = new Object[0];
		
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void applyCommands(Set output) {
		commandBuffer = output.toArray(commandBuffer);
		int size = output.size();
		output.clear();
		final Object[] a = commandBuffer;
		
		for (int i=0; i<size; i++) {
			for (Function f : postprocessors) {
				a[i] = f.apply(a[i]);
				if (a[i]==null) 
					break;
			}
			if (a[i]!=null) output.add(a[i]);
		}
		
	}

}
