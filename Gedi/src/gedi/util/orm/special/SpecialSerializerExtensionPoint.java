package gedi.util.orm.special;

import gedi.app.extension.DefaultExtensionPoint;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

@SuppressWarnings("rawtypes")
public class SpecialSerializerExtensionPoint extends DefaultExtensionPoint<Class,SpecialBinarySerializer>{

	
	static {
		SpecialSerializerExtensionPoint.getInstance().addExtension(StringSpecialSerializer.class, String.class);
		SpecialSerializerExtensionPoint.getInstance().addExtension(ArrayListSpecialSerializer.class, ArrayList.class);
		SpecialSerializerExtensionPoint.getInstance().addExtension(HashMapSpecialSerializer.class, HashMap.class);
		SpecialSerializerExtensionPoint.getInstance().addExtension(TreeMapSpecialSerializer.class, TreeMap.class);
		SpecialSerializerExtensionPoint.getInstance().addExtension(IntervalTreeSpecialSerializer.class, IntervalTree.class);
	}

	
	protected SpecialSerializerExtensionPoint() {
		super(SpecialBinarySerializer.class);
	}

	private static SpecialSerializerExtensionPoint instance;

	public static SpecialSerializerExtensionPoint getInstance() {
		if (instance==null) 
			instance = new SpecialSerializerExtensionPoint();
		return instance;
	}

	public boolean contains(Class<?> cls) {
		return ext.containsKey(cls);
	}
	
	public <T> SpecialBinarySerializer<T> get(Class<? extends T> cls) {
		return get(null, cls);
	}

}
