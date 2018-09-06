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
package gedi.util.io.randomaccess.serialization;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.Orm;

import java.io.IOException;
import java.util.function.Supplier;

public class ReferenceGenomicRegionSerializer<T> implements BinarySerializer<ImmutableReferenceGenomicRegion<T>> {

	private ReferenceSequence reference;
	private Supplier<T> creator;
	private DynamicObject globalInfo;

	public ReferenceGenomicRegionSerializer(ReferenceSequence reference, DynamicObject globalInfo,Supplier<T> creator) {
		this.reference = reference;
		this.creator = creator;
		this.globalInfo = globalInfo;
	}

	@Override
	public Class<ImmutableReferenceGenomicRegion<T>> getType() {
		return (Class)ImmutableReferenceGenomicRegion.class;
	}

	@Override
	public void beginSerialize(BinaryWriter out) throws IOException {
		out.getContext().setGlobalInfo(globalInfo);
	}
	@Override
	public void beginDeserialize(BinaryReader in) throws IOException {
		in.getContext().setGlobalInfo(globalInfo);
	}
	
	@Override
	public void serialize(BinaryWriter out, ImmutableReferenceGenomicRegion<T> r) throws IOException {
		FileUtils.writeGenomicRegion(out, r.getRegion());
		FileUtils.serialize(r.getData(),out);
	}

	@Override
	public ImmutableReferenceGenomicRegion<T> deserialize(BinaryReader in) throws IOException {
		return new ImmutableReferenceGenomicRegion<>(reference, FileUtils.readGenomicRegion(in),FileUtils.deserialize(creator.get(), in));
	}

	@Override
	public void serializeConfig(BinaryWriter out) throws IOException {
	}

	@Override
	public void deserializeConfig(BinaryReader in) throws IOException {
	}
	
	
}
