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
package gedi.riboseq.codonprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableTuple;

public abstract class CodonProcessorCounter {

	
	private NumericArray total;
	private HashMap<MutableTuple, NumericArray> counter = new HashMap<MutableTuple, NumericArray>();
	private MutableTuple proto;
	private String prefix;
	
	private ArrayList<CodonProcessorOutput> outputs = new ArrayList<CodonProcessorOutput>();
	private String[] varNames;
	
	private ArrayList<Predicate<HashMap<String,Object>>> conditions = new ArrayList<Predicate<HashMap<String,Object>>>();
	
	
	public CodonProcessorCounter(String prefix, String[] varNames)  {
		this.prefix = prefix;
		this.varNames = varNames;
		proto = new MutableTuple(EI.repeat(varNames.length, ()->Object.class).toArray(new Class[0]));
	}

	public String[] getVariableNames() {
		return varNames;
	}

	
	public CodonProcessorCounter addCondition(Predicate<HashMap<String,Object>> cond) {
		this.conditions.add(cond);
		return this;
	}
	
	
	public CodonProcessorCounter addCondition(String var, Object val) {
		return addCondition(map->map.get(var).equals(val));
	}
	
	
	public CodonProcessorCounter addCondition(String var, int val) {
		return addCondition(map->map.get(var).equals(new Integer(val)));
	}
	
	public CodonProcessorCounter addCondition(String var, boolean val) {
		return addCondition(map->map.get(var).equals(new Boolean(val)));
	}
	
	
	public String getPrefix() {
		return prefix;
	}
	
	public NumericArray getTotal() {
		
		return total;
	}
	
	public Set<MutableTuple> keys() {
		return counter.keySet();
	}
	
	public NumericArray getCounts(MutableTuple key) {
		return counter.get(key);
	}
	
	@Override
	public String toString() {
		return prefix;
	}
	
	public abstract void count(HashMap<String,Object> vars, NumericArray count);

	
	protected boolean testConditions(HashMap<String,Object> vars) {
		for (Predicate<HashMap<String,Object>> cond : conditions)
			if (!cond.test(vars))
				return false;
		return true;
	}

	protected void setup(HashMap<String, Object> vars) {
		for (int i=0; i<getVariableNames().length; i++)
			proto.set(i, vars.get(getVariableNames()[i]));		
	}

	
	protected void count(NumericArray count) {
		NumericArray mi = counter.get(proto);
		if (mi==null) 
			counter.put(proto.clone(),mi = NumericArray.createMemory(count.length(), count.getType()));
		mi.add(count);
		
		if (total==null) total = NumericArray.createMemory(count.length(), count.getType());
		total.add(count);
	}

	

	public void merge(CodonProcessorCounter other) {
		if (total==null) total = other.total;
		else if (other.total!=null) total.add(other.total);
		
		for (MutableTuple key : other.counter.keySet()) {
			NumericArray count = other.counter.get(key);
			counter.computeIfAbsent(key, x->NumericArray.createMemory(count.length(), count.getType())).add(count);
		}
	}


	
	public CodonProcessorCounter addOutput(CodonProcessorOutput output) {
		this.outputs.add(output);
		output.setCounter(this);
		return this;
	}

	public ExtendedIterator<CodonProcessorOutput> outputs() {
		return EI.wrap(outputs);
	}



	
	
}
