package gedi.gui.genovis.pixelMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableTuple;

public class PixelBlockToValuesMap implements BinarySerializable {

	private PixelLocationMapping blocks;
	private NumericArray[] values;


	public PixelBlockToValuesMap() {

	}
	

	public PixelBlockToValuesMap(MutableTuple c) {
		int n = 0;
		for (int i=0; i<c.size(); i++) {
			PixelBlockToValuesMap p = c.get(i);
			n+=p.values[0].length();
		}
		int off = 0;
		for (int i=0; i<c.size(); i++) {
			PixelBlockToValuesMap p = c.get(i);
			if (blocks==null) {
				blocks = p.blocks;
				this.values = new NumericArray[blocks.size()];
				for (int r=0; r<values.length; r++)
					values[r] = NumericArray.createMemory(n, p.values[r].getType());
			}
			else if (!blocks.equals(p.blocks)) throw new RuntimeException("Blocks do not match!");

			for (int r = 0; r < values.length; r++) {
				p.values[r].copyRange(0, values[r], off, p.values[r].length());
			}
			off+=p.values[0].length();
		}
	}


	public PixelBlockToValuesMap(PixelBlockToValuesMap c, boolean copyValues) {
		blocks = c.blocks;
		values = new NumericArray[c.values.length];
		for (int i = 0; i < values.length; i++) {
			if (copyValues)
				values[i] = c.values[i].createMemoryCopy();
			else
				values[i] = NumericArray.createMemory(c.values[i].length(), c.values[i].getType());
		}
	}
	
	/**
	 * Don't even create the value arrays!
	 * @param c
	 */
	public PixelBlockToValuesMap(PixelBlockToValuesMap c, NumericArray[] values) {
		blocks = c.blocks;
		this.values = values;
	}


	public int getBlockIndex(ReferenceSequence ref, int bp) {
		int low = 0;
		int high = blocks.size() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int cmp = blocks.get(mid).getReference().compareTo(ref);
			if (cmp==0) cmp = blocks.get(mid).compareToBp(bp);
	
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}


	public PixelLocationMapping getBlocks() {
		return blocks;
	}

	public PixelBlockToValuesMap(PixelLocationMapping blocks, int rows, NumericArrayType type) {
		this.blocks = blocks;
		this.values = new NumericArray[blocks.size()];
		for (int i=0; i<values.length; i++)
			values[i] = NumericArray.createMemory(rows, type);
	}

	public PixelBlockToValuesMap(PixelLocationMapping blocks, int rows, double init) {
		this.blocks = blocks;
		this.values = new NumericArray[blocks.size()];
		for (int i=0; i<values.length; i++) {
			values[i] = NumericArray.createMemory(rows, NumericArrayType.Double);
			for (int j=0; j<values[i].length(); j++)
				values[i].setDouble(j, init);
		}
	}
	
	public int size() {
		return values==null?0:values.length;
	}

	public PixelLocationMappingBlock getBlock(int index) {
		return blocks.get(index);
	}

	public NumericArray getValues(int index) {
		return values[index];
	}

	public GenomicRegion getRegion() {
		return blocks.getRegion();
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(blocks.size());
		if (blocks.size()==0) return;

		out.putString(blocks.get(0).getReference().getName());
		out.putCInt(blocks.get(0).getReference().getStrand().ordinal());
		for (PixelLocationMappingBlock bl : blocks) {
			if (!bl.getReference().equals(blocks.get(0).getReference())) 
				throw new IOException("Inhomogeneous reference sequences not allowed!");
			out.putCInt(bl.getStartBp());
			out.putCInt(bl.getStopBp()-bl.getStartBp()); // may save some space!
		}

		out.putCInt(values[0].getType().ordinal());
		for (int i=0; i<values.length; i++)
			values[i].serialize(out);

	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		int size = in.getCInt();
		blocks = new PixelLocationMapping();
		values = new NumericArray[size];

		Chromosome ref = Chromosome.obtain(in.getString(), Strand.values()[in.getCInt()]);
		for (int i=0; i<size; i++) {
			int start = in.getCInt();
			int l1 = in.getCInt();
			blocks.addBlock(ref,start,start+l1);
		}

		NumericArrayType type = NumericArrayType.values()[in.getCInt()];
		for (int i=0; i<size; i++) {
			values[i] = NumericArray.createMemory(-1, type);
			values[i].deserialize(in);
		}

	}

}
