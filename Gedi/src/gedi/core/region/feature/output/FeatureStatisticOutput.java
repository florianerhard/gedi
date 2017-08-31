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
import gedi.core.region.feature.special.UnfoldGenomicRegionStatistics;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.formats.CsvReaderFactory;
import gedi.util.mutable.MutableTuple;
import gedi.util.nashorn.JSToDoubleFunction;
import gedi.util.userInteraction.results.ResultProducer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;

import javax.script.ScriptException;



@GenomicRegionFeatureDescription(toType=Void.class)
public class FeatureStatisticOutput extends AbstractFeature<Void> {

	private String multiSeparator = ",";
	private BiFunction<Object,NumericArray,NumericArray> dataToCounts;
	private int countAdapterIndex = -1;
	private UnaryOperator<NumericArray> countAdapter;
	private double minimalFraction = 0;
	private int decimals=2;
	private ArrayList<Barplot> plots = new ArrayList<Barplot>(); 
			
	
	private ToDoubleFunction<NumericArray> aggregateFun;
	private int[] aggregateInputs;
	
	public FeatureStatisticOutput(String file) {
		minValues = maxValues = 0;
		setFile(file);
	}
	
	@Override
	protected void copyProperties(AbstractFeature<Void> from) {
		super.copyProperties(from);
		FeatureStatisticOutput f = (FeatureStatisticOutput) from;
		this.countAdapter = f.countAdapter;
		this.countAdapterIndex = f.countAdapterIndex;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void begin() {
		super.begin();
		if (countAdapterIndex>=0)
			this.countAdapter = program.getFeature(getInputName(countAdapterIndex)).getCountAdapter();
	}
	
	/**
	 * Can be: One of the fields of {@link NumericArrayFunction}, the name of a {@link NumericArrayFunction} class or a JS function NumericArray->double.
	 * @param by
	 * @param function
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws ScriptException 
	 */
	public void aggregate(int[] by, String function) throws IllegalArgumentException, IllegalAccessException, ScriptException {
		this.aggregateInputs = by;
		try {
			Field f = NumericArrayFunction.class.getField(function);
			if (!Modifier.isStatic(f.getModifiers()) || f.getType()!=NumericArrayFunction.class) throw new RuntimeException("Not a static field of NumericArrayFunction!");
			aggregateFun = (NumericArrayFunction) f.get(null);
			return;
		} catch (NoSuchFieldException e){}
		
		try {
			aggregateFun = (NumericArrayFunction) Class.forName(function).newInstance();
		} catch (Exception e) {}
		
		aggregateFun = new JSToDoubleFunction<NumericArray>(function);
		
	}
	
	public void add(Barplot plot) {
		plot.setName(getId());
		plots.add(plot);
	}
	
	public void setFile(String path) {
		setId(path);
	}

	public void setMinimalFraction(double minimalFraction) {
		this.minimalFraction = minimalFraction;
	}
	
	public void setMultiSeparator(String multiSeparator) {
		this.multiSeparator = multiSeparator;
	}
	
	private NumericArray total;
	private HashMap<MutableTuple,NumericArray> counter = new HashMap<MutableTuple, NumericArray>();
	private MutableTuple key;
	private NumericArray buffer;
	
	public void setDecimals(int decimals) {
		this.decimals = decimals;
	}
	
	public int getDecimals() {
		return decimals;
	}
	
	public int getNumTuples() {
		return counter.keySet().size();
	}
	
	public void setDataToCounts(
			BiFunction<Object, NumericArray, NumericArray> dataToCounts) {
		this.dataToCounts = dataToCounts;
	}
	
	public void setCountAdapter(UnaryOperator<NumericArray> countAdapter) {
		this.countAdapter = countAdapter;
		this.countAdapterIndex = -1;
	}
	
	public void setCountAdapter(int fromFeature) {
		this.countAdapterIndex = fromFeature;
		this.countAdapter = null;
	}
	
	@Override
	public void setInputNames(String[] inputs) {
		super.setInputNames(inputs);
		Class[] types = new Class[inputs.length];
		Arrays.fill(types, Set.class);
		key = new MutableTuple(types);
	}
	
	@Override
	public <I> void setInput(int index, Set<I> input) {
		super.setInput(index, input);
		key.setQuick(index, input);
	}

	
	private boolean mustUnfold(MutableTuple key) {
		for (int i=0; i<key.size(); i++) {
			Set<?> e = key.get(i);
			if (e.size()==1 && e.iterator().next() instanceof UnfoldGenomicRegionStatistics)
				return true;
		}
		return false;
	}
	
	private ExtendedIterator<MutableTuple> unfold(MutableTuple key) {
		IntArrayList ind = new IntArrayList();
		ArrayList<Iterator<Object>> unf = new ArrayList<>();
		for (int i=0; i<key.size(); i++) {
			Set<?> e = key.get(i);
			Iterator<?> it = e.iterator();
			int n = 0;
			if (it.hasNext()) {
				Object o = it.next();
				if (o instanceof UnfoldGenomicRegionStatistics) {
					UnfoldGenomicRegionStatistics unfolder = (UnfoldGenomicRegionStatistics)o;
					unf.add(unfolder.iterator());
					ind.add(i);
					if (it.hasNext() || n!=0)
						throw new RuntimeException("If a feature wants to return an UnfoldGenomicRegionStatistics, it must be the only result value!");
				}
				n++;
			}
		}
		if (unf.isEmpty()) return EI.singleton(key);
		
		MutableTuple re = key.clone();
		for (int i:ind.toIntArray())
			re.set(i, new HashSet());

		return EI.wrap(unf.get(0)).map(e0->{
			
			Set s = re.get(ind.getInt(0));
			s.clear();
			s.add(e0);
			
			for (int i=1; i<ind.size(); i++) {
				s = re.get(ind.getInt(i));
				s.clear();
				if (!unf.get(i).hasNext())
					throw new RuntimeException("All UnfoldGenomicRegionStatistics must return the same number of entries!");
				s.add(unf.get(i).next());
			}
			return re;
		}).endAction(()->{
			for (int i=1; i<ind.size(); i++) 
				if (unf.get(i).hasNext())
					throw new RuntimeException("All UnfoldGenomicRegionStatistics must return the same number of entries!");
		});
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected void accept_internal(Set<Void> values) {
		buffer = dataToCounts==null?program.dataToCounts(referenceRegion.getData(), buffer):dataToCounts.apply(referenceRegion.getData(), buffer);
		if (countAdapter!=null)
			buffer = countAdapter.apply(buffer);
		
		if (mustUnfold(key)) {
			unfold(key).forEachRemaining(this::doCount);
		}
		else 
			doCount(key);
		
//		if (getId().equals("stats/5p15.pos_unique.stat") && getInput(0).contains(85)) {
//			System.out.println(NumericArrayFunction.Sum.applyAsDouble(mi)+"\t"+ getId()+"\t"+key+"\t"+mi+"\t"+referenceRegion);
//		}
		if (total==null) total = NumericArray.createMemory(buffer.length(), buffer.getType());
		total.add(buffer);
		buffer.clear();
	}
	
	private void doCount(MutableTuple key) {
		NumericArray mi = counter.get(key);
		if (mi==null) 
			counter.put(newKey(key),mi = NumericArray.createMemory(buffer.length(), buffer.getType()));
		mi.add(buffer);		
	}

	public boolean dependsOnData() {
		return true;
	}
	
	
	private MutableTuple newKey(MutableTuple k) {
		MutableTuple re = new MutableTuple(k.getTypes());
		for (int i=0; i<re.size(); i++)
			re.set(i, new HashSet(k.get(i)));
		return re;
	}

	@Override
	public void end() {
	}
	
	public boolean needsMelt() {
		return total.length()>1;
	}
	
	@Override
	public void addResultProducers(ArrayList<ResultProducer> re) {
		re.addAll(plots);
	}

	@Override
	public void produceResults(GenomicRegionFeature<Void>[] o){
		if (o!=null) {
			counter.clear();
			if (total!=null) total.clear();
			
			for (GenomicRegionFeature<Void> a : o) {
				FeatureStatisticOutput x = (FeatureStatisticOutput)a;
				
				MutableTuple[] tuples = null;
				while (tuples==null) {
					try {
						tuples = x.counter.keySet().toArray(new MutableTuple[0]);
					} catch (ConcurrentModificationException e) {
						tuples = null;
					}
				}
				for (MutableTuple key : tuples) {
					NumericArray ot = x.counter.get(key);
					if (ot!=null){
						NumericArray mi = counter.get(key);
						if (mi==null) 
							counter.put(newKey(key),mi = NumericArray.createMemory(ot.length(), ot.getType()));
						mi.add(ot);
					}
				}
				if (x.total!=null) {
					if (total==null) total = NumericArray.createMemory(x.total.length(), x.total.getType());
					total.add(x.total);
				}
			}
			
		}
		produceResult(!program.isRunning());
	}
	
	public ArrayList<Barplot> getPlots() {
		return plots;
	}
	
	public DataFrame getResults() {
		return new CsvReaderFactory().createReader(getId()).readDataFrame();
	}
	
	public void produceResult(boolean isFinal) {
		if (counter.size()==0) return;
		
		LineOrientedFile out = new LineOrientedFile(getId());
		try {
			out.startWriting();
			if (aggregateFun!=null) {
				for (int i=0; i<aggregateInputs.length; i++) 
					out.writef(i==0?"%s":"\t%s",inputNames[aggregateInputs[i]]);
			} else {
				for (int i=0; i<inputs.length; i++) 
					out.writef(i==0?"%s":"\t%s",inputNames[i]);
			}
			
			int l = total.length();
			if (program.getLabels()!=null && program.getLabels().length==l)
				for (int i=0; i<program.getLabels().length; i++) 
					out.writef("\t%s",program.getLabels()[i]);
			else {
				for (int i=0; i<l; i++) 
					out.writef("\t%d",i);
			}
			out.writeLine();
			
			NumericArray other = NumericArray.createMemory(total.length(), total.getType());
			MutableTuple[] tuples = null;
			while (tuples==null) {
				try {
					tuples = counter.keySet().toArray(new MutableTuple[0]);
				} catch (ConcurrentModificationException e) {
					tuples = null;
				}
			}
			
			HashMap<MutableTuple,NumericArray> counter = this.counter;
			
			if (aggregateFun!=null) {
				

				HashMap<MutableTuple,HashMap<MutableTuple,NumericArray>> agg = new HashMap<MutableTuple, HashMap<MutableTuple,NumericArray>>();
				for (MutableTuple t : counter.keySet()) 
					agg.computeIfAbsent(t.restrictTo(aggregateInputs), s->new HashMap<>()).put(t.exclude(aggregateInputs), counter.get(t));

				counter = new HashMap<MutableTuple, NumericArray>();
				
				for (MutableTuple t : agg.keySet()) {
					HashMap<MutableTuple,NumericArray> a = agg.get(t);
					NumericArray buff = NumericArray.createMemory(a.size(), NumericArrayType.Integer);
					int d = a.values().iterator().next().length();
					
					NumericArray put = NumericArray.createMemory(d, NumericArrayType.Double);
					for (int i=0; i<d; i++) {
						int j=0;
						for (MutableTuple agt : a.keySet()) {
							buff.copy(a.get(agt), i, j++);
						}
						double stat = aggregateFun.applyAsDouble(buff);
						put.setDouble(i, stat);
					}
					counter.put(t,put);
				}
				
				tuples = counter.keySet().toArray(new MutableTuple[0]);
				
			}
			
			Arrays.sort(tuples);
			for (MutableTuple t : tuples) {
				NumericArray a = counter.get(t);
				if (isOutput(a)) {
					for (int i=0; i<t.size(); i++) {
						Set<?> s = t.get(i);
						out.writef(i==0?"%s":"\t%s",StringUtils.concat(multiSeparator, s));
					}
					for (int i=0; i<a.length(); i++)
						out.writef("\t%s",a.formatDecimals(i,decimals));
					out.writeLine();
				} else {
					other.add(a);
				}
			}
			
			if (isOutput(other)) {
				for (int i=0; i<inputs.length; i++) {
					out.writef(i==0?"%s":"\t%s","Other");
				}
				for (int i=0; i<other.length(); i++)
					out.writef("\t%s",other.formatDecimals(i,decimals));
				out.writeLine();
			}
			
			out.finishWriting();

			String[] inputs = new String[aggregateFun==null?getInputLength():aggregateInputs.length];
			if (aggregateFun!=null) {
				for (int i=0; i<aggregateInputs.length; i++)
					inputs[i] = getInputName(aggregateInputs[i]);
			} else {
				for (int i=0; i<inputs.length; i++)
					inputs[i] = getInputName(i);
			}
			
			for (Barplot plot : plots) {
				plot.setName(getId());
				plot.plot(out,isFinal,inputs,needsMelt());
			}
			
		} catch (IOException e) {
			throw new RuntimeException("Cannot write output file!",e);
		}
	}

	private boolean isOutput(NumericArray a) {
		if (a==null) return false;
		for (int i=0; i<a.length(); i++)
			if (a.getDouble(i)>total.getDouble(i)*minimalFraction)
				return true;
		return false;
	}
	
	@Override
	public GenomicRegionFeature<Void> copy() {
		FeatureStatisticOutput re = new FeatureStatisticOutput(getId());
		re.copyProperties(this);
		re.multiSeparator = multiSeparator;
		re.dataToCounts = dataToCounts;
		re.minimalFraction = minimalFraction;
		re.plots = plots;
		re.decimals = decimals;
		Class[] types = new Class[inputs.length];
		Arrays.fill(types, Set.class);
		re.key = new MutableTuple(types);
		
		return re;
	}

	
	

}

