package gedi.core.data.annotation;

import java.util.ArrayList;

import gedi.core.reference.ReferenceSequence;

public class CompositeReferenceSequenceLengthProvider extends ArrayList<ReferenceSequenceLengthProvider> implements ReferenceSequenceLengthProvider {

	public int getLength(String name) {
		int re = 0;
		for (int i=0; i<size(); i++) {
			int l = get(i).getLength(name);
			if (l>0) re = Math.max(re, l);
			else if (re<=0) re = Math.min(re, l);
		}
		return re;
	}
	
}
