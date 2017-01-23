package gedi.util.orm.special;

import gedi.core.reference.ReferenceSequence;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;

public class IntervalTreeSpecialSerializer implements SpecialBinarySerializer<IntervalTree<?,?>> {

	@Override
	public void serialize(OrmSerializer parent, BinaryWriter out, IntervalTree<?,?> object) throws IOException {
		parent.serialize(out, object.getReference());
		out.putCInt(object.size());
		for (Object k : object.keySet()) {
			parent.serialize(out, k);
			parent.serialize(out, object.get(k));
		}
	}

	@Override
	public IntervalTree<?,?> deserialize(OrmSerializer parent, BinaryReader in) throws IOException {
		ReferenceSequence  ref = parent.deserialize(in);
		int c = in.getCInt();
		IntervalTree re = new IntervalTree(ref);
		for (int i=0; i<c; i++)
			re.put(parent.deserialize(in),parent.deserialize(in));
		return re;
	}

}
