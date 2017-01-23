package gedi.oml.remote.Pipeline;

import gedi.core.reference.Chromosome;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;

public class RemotePipelineData<D> implements BinarySerializable {

	private String id;
	private ReferenceGenomicRegion<D> data;
	
	public RemotePipelineData(String id, ReferenceGenomicRegion<D> data) {
		this.id = id;
		this.data = data;
	}
	
	public RemotePipelineData() {
	}

	public String getId() {
		return id;
	}
	
	public ReferenceGenomicRegion<D> getData() {
		return data;
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(id.length());
		out.putAsciiChars(id);
		out.putCInt(data.getReference().getName().length());
		out.putAsciiChars(data.getReference().getName());
		out.putCInt(data.getReference().getStrand().ordinal());
		out.putCInt(data.getRegion().getNumBoundaries());
		for (int i=0; i<data.getRegion().getNumBoundaries(); i++)
			out.putCInt(data.getRegion().getBoundaries()[i]);
		
		new OrmSerializer(true,true).serializeAll(out, EI.wrap(data.getData()));
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		id = in.getAsciiChars(in.getCInt());
		String chrname = in.getAsciiChars(in.getCInt());
		Strand strand = Strand.values()[in.getCInt()];
		int[] coords = new int[in.getCInt()];
		for (int i=0; i<coords.length; i++)
			coords[i] = in.getCInt();
		ArrayGenomicRegion region = new ArrayGenomicRegion(coords);

		try {
			D data = (D) new OrmSerializer(true,true).deserializeAll(in).next();

			this.data = new ImmutableReferenceGenomicRegion<D>(Chromosome.obtain(chrname,strand), region,data);
		} catch (Exception e) {
			throw new IOException("Cannot deserialize!",e);
		}
		
		
	}
	
	
}
