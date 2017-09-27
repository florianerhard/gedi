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

package gedi.app.extension;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;



public class DefaultExtensionPoint<C,T> implements ExtensionPoint<C,T> {

	
	private Class<T> cls;
	protected HashMap<C,Class<? extends T>> ext = new HashMap<C, Class<? extends T>>(); 
	
	protected DefaultExtensionPoint(Class<T> cls) {
		this.cls = cls;
	}
	
	@Override
	public Class<T> getExtensionPointClass() {
		return cls;
	}

	@Override
	public void addExtension(Class<? extends T> extension, C key) {
		ext.put(key,extension);
	}

	protected ExtensionContext empty = new ExtensionContext();
	
	public T get(ExtensionContext context, C key) {
		try {
			Class<? extends T> recls = ext.get(key);
			if (recls==null) return null;
			if (context==null) context = empty;
			return context.newInstance(recls);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get extension for "+key,e);
			throw new RuntimeException("Could not get extension for "+key,e);
		}
	}
	
	public ExtendedIterator<T> getExtensions(ExtensionContext ctx) {
		return EI.wrap(ext.keySet()).map(k->get(ctx,k));
	}
	
	
}
