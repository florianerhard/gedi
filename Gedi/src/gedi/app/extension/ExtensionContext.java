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

import gedi.util.ReflectionUtils;
import gedi.util.dynamic.DynamicObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Context class, that holds a mapping of class to object.
 * The get method does not only work for the exact class, but
 * uses {@link ReflectionUtils#getImplementedInterfaces(Class)} to return
 * suitable object, even if the exact class is not present in the map.
 * <p>
 * Changes in the map can be tracked using a listener system.
 * 
 * 
 * @author Florian Erhard
 *
 */
public class ExtensionContext {

	private HashMap<Class<?>,Object> map = new HashMap<Class<?>, Object>();
	private DynamicObject globalInfo = DynamicObject.getEmpty();
	
	public <T> ExtensionContext add(T o) {
		return add((Class<T>)o.getClass(), o);
	}
	
	
	/**
	 * Adds a new object to the map
	 * @param cls the class
	 * @param o the object
	 * @return this extension context
	 */
	public <T> ExtensionContext add(Class<T> cls, T o) {
		T oldValue = get(cls);
		if (o==null)
			remove(cls);
		else
			map.put(cls, o);
		
		if (oldValue!=o)
			tryFireContextChange(getChangeEvent(cls, oldValue, o));
		
		return this;
	}
	
	/**
	 * Removes the best match of cls from the map, i.e. the object,
	 * that is returned by {@link #get(Class)}
	 * 
	 * @param cls the class
	 * @return this extension context
	 */
	public <T> ExtensionContext remove(Class<T> cls) {
		T oldValue = get(cls);
		map.remove(cls);
		tryFireContextChange(getChangeEvent(cls, oldValue, null));
		return this;
	}
	
	/**
	 * Removes all matches of cls from the map.
	 * 
	 * @param cls the class
	 * @return this extension context
	 */
	public ExtensionContext removeAllMatchting(Class<?> cls) {
		while (get(cls)!=null) 
			removeObject(get(cls));
		
		return this;
	}
	
	public DynamicObject getGlobalInfo() {
		return globalInfo;
	}
	
	
	public void setGlobalInfo(DynamicObject globalInfo) {
		this.globalInfo = globalInfo;
	}
	
	
	/**
	 * Removes the given object for each suitable key from the map.
	 * @param o the object to remove
	 * @return this extension context
	 */
	public ExtensionContext removeObject(Object o) {
		Set<Class<?>> keys = new HashSet<Class<?>>();
		for (Class<?> key : map.keySet())
			if (map.get(key)==o)
				keys.add(key);
		for (Class<?> key : keys)
			remove(key);
		return this;
	}
	
	/**
	 * Adds each element of the given extension context to this and
	 * overwrites objects, if specified.
	 * @param ec the extension context to add
	 * @param overwrite if to overwrite objects
	 * @return this extension context
	 */
	@SuppressWarnings("rawtypes")
	public ExtensionContext addAll(ExtensionContext ec, boolean overwrite) {
		for (Class cls : ec.map.keySet())
			if (overwrite || !map.containsKey(cls))
				add(cls,ec.map.get(cls));
		return this;
	}
	
	/**
	 * Gets the number of objects.
	 * @return the size
	 */
	public int size() {
		return map.size();
	}
	
	/**
	 * Returns, if this extension context contains objects for each
	 * of the given classes.
	 * 
	 * @param cls the classes
	 * @return if it conforms
	 */
	public boolean conformsTo(Class<?>[] cls) {
		for (Class<?> c : cls) 
			if (get(c)==null)
				return false;
		return true;
	}
	
	/**
	 * Gets a list of classes that are missing.
	 * @see #conformsTo(Class[])
	 * @param cls the classes
	 * @return list of classes
	 */
	public List<Class<?>> getMissing(Class<?>[] cls) {
		LinkedList<Class<?>> re = new LinkedList<Class<?>>();
		for (Class<?> c : cls) 
			if (get(c)==null)
				re.add(c);
		return re;
	}
	
	/**
	 * Get the objects for the classes (parallely)
	 * @param cls the classes
	 * @return the objects
	 */
	public Object[] getObjects(Class<?>[] cls) {
		Object[] re = new Object[cls.length];
		for (int i=0; i<re.length; i++)
			re[i] = get(cls[i]);
		return re;
	}
	
	
	/**
	 * Instantiates a class in the given context. If multiple constructors match, the one with the most parameters is taken; if this is not
	 * unique, the first one is taken.
	 * 
	 * @param <T> the class
	 * @param cls the class
	 * @return the instantiated class
	 * @throws InstantiationException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws ExtensionException
	 */
	@SuppressWarnings("unchecked")
	public <T> T newInstance(Class<? extends T> cls) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			
		Constructor<?>[] ctors = cls.getConstructors();
		int best = -1;
		for (int i=0; i<ctors.length; i++) {
			Constructor<?> ctor = ctors[i];
			if (Modifier.isPublic(ctor.getModifiers()) && conformsTo(ctor.getParameterTypes())) {
				if (best==-1 || ctor.getParameterCount()>ctors[best].getParameterCount())
					best = i;
			}
		}
		if (best==-1)
			for (int i=0; i<ctors.length; i++) {
				Constructor<?> ctor = ctors[i];
				if (!Modifier.isPublic(ctor.getModifiers()) && conformsTo(ctor.getParameterTypes())) {
					if (best==-1 || ctor.getParameterCount()>ctors[best].getParameterCount())
						best = i;
				}
			}
		
		if (best==-1) throw new InstantiationException("No matching constructor found!");
		if (!Modifier.isPublic(ctors[best].getModifiers()))
			ctors[best].setAccessible(true);
		
		return (T) ctors[best].newInstance(getObjects(ctors[best].getParameterTypes()));
	}
	
	
	/**
	 * Gets all objects of this extension context
	 * @return the collection
	 */
	public Collection<Object> getObjects() {
		return map.values();
	}

	/**
	 * Gets the object for the given class. If the class is present in the map,
	 * the value is returned directly. Otherwise, a more special class is search
	 * by {@link Class#isAssignableFrom(Class)} on each key of the map. If nothing has
	 * been found, a more general class is search using {@link ReflectionUtils#getImplementedInterfaces(Class)}
	 * and returned, if the respective object in the map is an instance of cls.
	 * 
	 * @param <T> the class
	 * @param cls the class
	 * @return a suitable object
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> cls) {
		if (cls==null) return null;
		
		T re = (T) map.get(cls);
		if (re==null) {
			// look for something more special
			for (Class<?> k : map.keySet())
				if (cls.isAssignableFrom(map.get(k).getClass()))
					return (T) map.get(k);
		}
		
		// look for something more general, which is a cls
		if (re==null) {
			for (Class<? super T> p : ReflectionUtils.getImplementedInterfaces(cls))
				if (map.containsKey(p) && cls.isInstance(map.get(p))) 
					return (T) map.get(p);
		}
		if (re==null && cls==ExtensionContext.class)
			return (T) this;
		
		return re; 
	}
	
	
	/**
	 * Similarly to {@link #get(Class)}, but returns a list of all matching items.
	 * 
	 * @param <T>
	 * @param cls
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getAll(Class<T> cls) {
		if (cls==null) return null;
		HashSet<T> re = new HashSet<T>();
		
		Object item = map.get(cls);
		if (item!=null)
			re.add((T)item);
	
		// look for something more special
		for (Class<?> k : map.keySet()) {
			item = map.get(k);
			if (item!=null && cls.isAssignableFrom(k))
				re.add((T)item);
		}
			
		
		// look for something more general, which is a cls
		for (Class<? super T> p : ReflectionUtils.getImplementedInterfaces(cls)) {
			item = map.get(p);
			if (item!=null && cls.isInstance(item)) 
				re.add((T)item);
		}

		if (cls==ExtensionContext.class)
			re.add((T)this);
		
		return re; 
	}
	
	public void clear() {
		map.clear();
	}


	@Override
	public ExtensionContext clone() {
		ExtensionContext re = new ExtensionContext();
		re.addAll(this,true);
		return re;
	}
	
	@Override
	public String toString() {
		return "Extension Context "+map.keySet();
	}
	
	private static ExtensionContext emptyContext = null;
	/**
	 * Gets an immutable empty {@link ExtensionContext}.
	 * @return the empts extension context
	 */
	public static ExtensionContext emptyContext() {
		// create lazily
		if (emptyContext==null)
			emptyContext = new ExtensionContext() {
				@Override
				public <T> ExtensionContext remove(Class<T> cls) {
					throw new RuntimeException("Not permitted");
				}
				@Override
				public ExtensionContext removeObject(Object o) {
					throw new RuntimeException("Not permitted");
				}
				@Override
				public <T> ExtensionContext add(Class<T> cls, T o) {
					throw new RuntimeException("Not permitted");
				}
			};
		return emptyContext;
	}

	/**
	 * Gets the keys of the map.
	 * @return the classes
	 */
	public Iterable<Class<?>> getClasses() {
		return map.keySet();
	}

	private ArrayList<Consumer<ContextChangeEvent<?>>> listeners = new ArrayList<Consumer<ContextChangeEvent<?>>>();
	
	/**
	 * Registers a listener at this extension context.
	 * @param l the listener
	 */
	public void addContextChangeListener(Consumer<ContextChangeEvent<?>> l) {
		listeners.add(l);
	}
	
	/**
	 * Removes a listener from this extension context.
	 * @param l the listener
	 */
	public void removeContextChangeListener(Consumer<ContextChangeEvent<?>> l)  {
		listeners.remove(l);
	}
	

	private <T> void tryFireContextChange(ContextChangeEvent<T> e) {
		for (Consumer<ContextChangeEvent<?>> l : listeners)
			l.accept(e);
	}
	
	private <T> ContextChangeEvent<T> getChangeEvent(Class<T> cls, T oldValue, T newValue) {
		return new ContextChangeEvent<>(this,cls,oldValue,newValue);
	}
	
	
	/**
	 * Parameters for an extension event.
	 * @see ContextChangeListener
	 * @see ExtensionContext
	 * 
	 * 
	 * @author Florian Erhard
	 *
	 */
	public static class ContextChangeEvent<T> {

		private ExtensionContext source;
		private Class<T> contextClass;
		private T oldValue;
		private T newValue;
		
		public ContextChangeEvent(ExtensionContext source, Class<T> contextClass, T oldValue, T newValue) {
			this.source = source;
			this.contextClass = contextClass;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
		
		public ExtensionContext getContext() {
			return source;
		}

		public Class<T> getContextClass() {
			return contextClass;
		}

		public T getOldValue() {
			return oldValue;
		}

		public T getNewValue() {
			return newValue;
		}
		
		@Override
		public String toString() {
			return "ContextChangeEvent [ContextClass="+contextClass+", oldValue="+oldValue+", newValue="+newValue+"]";
		}
	}


	

	
}
