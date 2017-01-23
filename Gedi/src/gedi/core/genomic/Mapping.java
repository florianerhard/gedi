package gedi.core.genomic;

import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.orm.Orm;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Mapping {

	private String id;
	
	private Genomic genomic;
	private String annoId;
	private String fieldKey;
	private HashMap<?,?> map;

	public Mapping(Genomic genomic, String id) {
		this.genomic = genomic;
		this.id = id;
		annoId = id;
		if (id.contains(".")) {
			annoId = id.substring(0, id.indexOf('.'));
			fieldKey = id.substring(id.indexOf('.')+1);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void merge(Mapping other) {
		get();
		other.get();
		Map m = map;
		if (m.keySet().removeAll(other.map.keySet()))
			throw new IllegalArgumentException("Cannot merge, mapping keys not unique: "+m.keySet());
		m.putAll(other.map);
	}
	

	public String getId() {
		return id;
	}
	
	public <F,T> Function<F,T> get() {
		if (map==null) {
			MemoryIntervalTreeStorage<T> itree = genomic.getAnnotation(annoId);
			Function<ReferenceGenomicRegion<T>,F> key = t->(F)t.getData();
			if (fieldKey!=null) {
				Function<T, F> getter = Orm.getFieldGetter(itree.getType(), fieldKey);
				key = t->getter.apply(t.getData());
			}
			map = ArrayUtils.index(itree.ei(),key);
		}
		return f->(T)map.get(f);
	}
}
