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
