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
package gedi.core.region;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.orm.Orm;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class MutableReferenceGenomicRegion<D> implements ReferenceGenomicRegion<D>, BinarySerializable {

	private ReferenceSequence reference;
	private GenomicRegion region;
	private D data;
	private Supplier<D> dataSupplier;

	public MutableReferenceGenomicRegion<D> clear() {
		return set(null,null);
	}
	
	public MutableReferenceGenomicRegion<D> set(ReferenceSequence reference,
			GenomicRegion region) {
		this.reference = reference;
		this.region = region;
		this.data = null;
		this.dataSupplier = null;
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> set(ReferenceSequence reference,
			GenomicRegion region, D data) {
		this.reference = reference;
		this.region = region;
		this.data = data;
		this.dataSupplier = null;
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> set(MutableReferenceGenomicRegion<D> o) {
		if (o==null) {
			this.reference = null;
			this.region = null;
			this.data = null;
			this.dataSupplier = null;
			return this;	
		}
		this.reference = o.reference;
		this.region = o.region;
		this.data = o.data;
		this.dataSupplier = o.dataSupplier;
		return this;
	}
	
	@Override
	public boolean isMutable() {
		return true;
	}
	
	public MutableReferenceGenomicRegion<D> set(ReferenceSequence reference,
			GenomicRegion region, Supplier<D> dataSupplier) {
		this.reference = reference;
		this.region = region;
		this.data = null;
		this.dataSupplier = dataSupplier;
		return this;
	}
	
	@Override
	public MutableReferenceGenomicRegion<D> toMutable() {
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> setReference(ReferenceSequence reference) {
		this.reference = reference;
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> setRegion(GenomicRegion region) {
		this.region = region;
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> alterReference(UnaryOperator<ReferenceSequence> op) {
		reference = op.apply(reference);
		return this;
	}
	public MutableReferenceGenomicRegion<D> alterRegion(UnaryOperator<GenomicRegion> op) {
		region = op.apply(region);
		return this;
	}
	public MutableReferenceGenomicRegion<D> alterData(UnaryOperator<D> op) {
		data = op.apply(data);
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> setData(D data) {
		this.data = data;
		this.dataSupplier = null;
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> setDataSupplier(Supplier<D> dataSupplier) {
		this.data = null;
		this.dataSupplier = dataSupplier;
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> toStrandIndependent() {
		setReference(getReference().toStrandIndependent());
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> toPlusStrand() {
		setReference(getReference().toPlusStrand());
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> toOppositeStrand() {
		setReference(getReference().toOppositeStrand());
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> toMinusStrand() {
		setReference(getReference().toMinusStrand());
		return this;
	}
	

	public MutableReferenceGenomicRegion<D> toStrand(Strand strand) {
		setReference(getReference().toStrand(strand));
		return this;
	}
	
	public ReferenceSequence getReference() {
		return reference;
	}
	
	public GenomicRegion getRegion() {
		return region;
	}
	
	public D getData() {
		if (dataSupplier!=null)
			return dataSupplier.get();
		return data;
	}

	@Override
	public String toString() {
		return toString2();
	}
	
	@Override
	public int hashCode() {
		return hashCode2();
	}

	@Override
	public boolean equals(Object obj) {
		return equals2(obj);
	}
	
	@SuppressWarnings("unchecked")
	public void deserialize(BinaryReader in) throws IOException {
		reference = FileUtils.readReferenceSequence(in);
		region = FileUtils.readGenomicRegion(in);
		if (in.getByte()==1) {
			if (data==null){
				if (dataSupplier!=null)
					data = dataSupplier.get();
				else if (in.getContext().get(Class.class)!=null)
					data = Orm.create((Class<D>)in.getContext().get(Class.class));
				else
					throw new RuntimeException("Cannot create an instance of the data object!");
			}
			if (getData() instanceof BinarySerializable) {
				((BinarySerializable)getData()).deserialize(in);
			} 
			else if (getData() instanceof BinarySerializable[]) {
				BinarySerializable[] a = (BinarySerializable[])getData();
				int l = in.getCInt();
				if (a.length!=l)
					data = (D) Array.newInstance(data.getClass().getComponentType(), l);
				
				a = (BinarySerializable[])getData();
				for (BinarySerializable b : a)
					b.deserialize(in);
			}
			else if (getData() instanceof Number) {
				if (data instanceof Byte)
					data = (D) new Byte((byte) in.getByte());
				else if (data instanceof Short)
					data = (D) new Short(in.getShort());
				else if (data instanceof Integer)
					data = (D) new Integer(in.getInt());
				else if (data instanceof Long)
					data = (D) new Long(in.getLong());
				else if (data instanceof Float)
					data = (D) new Float(in.getFloat());
				else if (data instanceof Double)
					data = (D) new Double(in.getDouble());
			}
			else throw new RuntimeException("Cannot deserialize "+data);
			
		} else {
			data = null;
		}
	}
	
	public void serialize(BinaryWriter out) throws IOException{
		FileUtils.writeReferenceSequence(out, getReference());
		FileUtils.writeGenomicRegion(out, getRegion());
		if (getData() instanceof BinarySerializable) {
			out.putByte(1);
			((BinarySerializable)getData()).serialize(out);
		} 
		else if (getData() instanceof BinarySerializable[]) {
			BinarySerializable[] a = (BinarySerializable[])getData();
			out.putByte(1);
			out.putCInt(a.length);
			for (BinarySerializable b : a)
				b.serialize(out);
		}
		else if (getData() instanceof Number) {
			out.putByte(1);
			FileUtils.writeNumber(out,(Number)getData());
		}
		else {
			out.putByte(0);
		}
	}
	
	public MutableReferenceGenomicRegion<D> transform(UnaryOperator<MutableReferenceGenomicRegion<D>> fun) {
		return set(fun.apply(this));
	}

	public MutableReferenceGenomicRegion<D> transformRegion(UnaryOperator<GenomicRegion> fun) {
		region = fun.apply(region);
		return this;
	}

	
	public MutableReferenceGenomicRegion<D> extendRegion(int upstream, int downstream) {
		if (reference.getStrand().equals(Strand.Minus))
			region = region.extendFront(downstream).extendBack(upstream);
		else
			region = region.extendFront(upstream).extendBack(downstream);
		return this;
	}

	
	public MutableReferenceGenomicRegion<D> transformReference(UnaryOperator<ReferenceSequence> fun) {
		reference = fun.apply(reference);
		return this;
	}
	
	public MutableReferenceGenomicRegion<D> transformData(UnaryOperator<D> fun) {
		data = fun.apply(data);
		return this;
	}


	
	public MutableReferenceGenomicRegion<D> parse(String pos) {
		return parse(pos,(D)null);
	}

	public MutableReferenceGenomicRegion<D> parse(String pos, D data) {
		String[] p = StringUtils.split(pos,':');
		if (p.length!=2) return clear();
		ArrayGenomicRegion reg = GenomicRegion.parse(p[1]);
		if (reg==null) return clear();
		return set(Chromosome.obtain(p[0]), reg,data);
	}
	
	public MutableReferenceGenomicRegion<D> parse(String pos, Supplier<D> data) {
		String[] p = StringUtils.split(pos,':');
		if (p.length!=2) return clear();
		ArrayGenomicRegion reg = GenomicRegion.parse(p[1]);
		if (reg==null) return clear();
		return set(Chromosome.obtain(p[0]), reg,data);
	}

		
}
