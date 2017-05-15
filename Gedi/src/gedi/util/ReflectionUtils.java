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

package gedi.util;

import gedi.util.parsing.Parser;
import gedi.util.parsing.ParserCache;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;


public class ReflectionUtils {


	public static Class<?> getCommonSuperClass(Iterable<Class<?>> classes) {
		return getCommonSuperClass(classes.iterator());
	}
	/**
	 * Gets the first class, that is super class of each of the given classes, i.e. no subclass
	 * of the returned class is also a super class.
	 * @param classes
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Class<?> getCommonSuperClass(Iterator<Class<?>> classes) {
		Iterator<Class<?>> it = classes; 
		if (!it.hasNext())
			return Object.class;

		Class<?>[] first = getSuperClassPath(it.next());
		HashSet<Class<?>> set = new HashSet<Class<?>>(Arrays.asList(first));
		while (it.hasNext()) {
			HashSet<Class<?>> nextSet = new HashSet<Class<?>>(Arrays.asList(it.next()));
			set.retainAll(nextSet);
		}

		for (int i=0; i<first.length; i++)
			if (set.contains(first[i]))
				return first[i];

		throw new RuntimeException("Not possible, Object.class should at least be returned!");
	}

	public static Class<?> getCommonSuperClass(Class<?> class1, Class<?> class2) {
		if (class1==null) return class2;
		if (class2==null) return class1;

		Class<?>[] first = getSuperClassPath(class1);
		for (int i=0; i<first.length; i++)
			if (first[i].isAssignableFrom(class2))
				return first[i];
		throw new RuntimeException("Not possible, Object.class should at least be returned!");
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<? super T>[] getSuperClassPath(Class<T> cls) {
		List<Class<? super T>> parents = new ArrayList<Class<? super T>>();

		for (Class<? super T> parent = cls; parent!=null; parent=parent.getSuperclass()) 
			parents.add(parent);

		return parents.toArray((Class<T>[]) Array.newInstance(Class.class, parents.size()));
	}

	/**
	 * Gets a list of each super class and super interface of the given class.
	 * The order is: the class itself, the class hierarchy up to {@link Object},
	 * the declared interfaces of each class in the hierarchy in the hierarchy order
	 * 
	 * @param <T> the class
	 * @param cls the clas
	 * @return the implemented classes and interfaces
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<Class<? super T>> getImplementedInterfaces(Class<T> cls) {
		ArrayList<Class<? super T>> order = new ArrayList<>();
		HashSet<Class<? super T>> set = new HashSet<>();
		List<Class<? super T>> parents = new ArrayList<Class<? super T>>();
		List intf = new ArrayList();

		for (Class<? super T> parent = cls; parent!=null; parent=parent.getSuperclass()) {
			parents.add(parent);
			intf.addAll(Arrays.asList(parent.getInterfaces()));
		}
		for (Class<? super T> p : parents) {
			if (set.add(p))
				order.add(p);
		}

		while (!intf.isEmpty()) {
			Class<? super T> i = (Class<? super T>) intf.remove(0);
			if (set.add(i))
				order.add(i);
			intf.addAll(Arrays.asList(i.getInterfaces()));
		}


		return order;
	}

	public static Method getCorrespondingSetter(Method m) throws SecurityException, NoSuchMethodException {
		return m.getDeclaringClass().getMethod("s"+m.getName().substring(1), m.getReturnType());
	}

	public static Method getCorrespondingGetter(Method m) throws SecurityException, NoSuchMethodException {
		return m.getDeclaringClass().getMethod("g"+m.getName().substring(1), m.getReturnType());
	}


	/**
	 * Independent of parameters!
	 * @param o
	 * @param name
	 * @return
	 */
	public static Method findMethod(Object o, String name) {
		for (Method m : o.getClass().getMethods()) {
			if (m.getName().equals(name))
				return m;
		}
		return null;
	}
	

	/**
	 * Independent of parameters!
	 * @param o
	 * @param name
	 * @return
	 */
	public static Method findMethod(Class cls, String name) {
		for (Method m : cls.getMethods()) {
			if (m.getName().equals(name))
				return m;
		}
		return null;
	}
	
	/**
	 * Find the first method that matches and returns an executor for the unparser parameters
	 * @param o
	 * @param name
	 * @param unparsedParameters
	 * @return
	 */
	public static BiFunction<Object,String[],Object> findMethod(Object o, String name, String[] unparsedParameters) {
		methd:for (Method m : o.getClass().getMethods()) {
			if (m.getName().equals(name) && m.getParameterCount()==unparsedParameters.length) {
				Parser[] pars = new Parser[unparsedParameters.length];
				for (int i=0; i<unparsedParameters.length; i++) {
					pars[i] = ParserCache.getInstance().get(m.getParameterTypes()[i]);
					if (!pars[i].canParse(unparsedParameters[i]))
						continue methd;
				}
				m.setAccessible(true);
				return (o2,a)->{
					Object[] params = new Object[m.getParameterCount()];
					for (int i=0; i<params.length; i++)
						params[i] = pars[i].apply(a[i]);
					try {
						return m.invoke(o2, params);
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						throw new RuntimeException("Method "+name+" for "+o.getClass().getName()+" could not be invoked!",e);
					}
				};
			}
		}
		return null;
	}

	public static Method findMethod(Object o, String name,Class<?>... cls) {
		for (Method m : o.getClass().getMethods()) {
			Class<?>[] para = m.getParameterTypes();
			if (para.length!=cls.length || !m.getName().equals(name))
				continue;
			int i;
			for (i=0; i<cls.length; i++) {
				if (cls[i]!=null && !toBoxClass(para[i]).isAssignableFrom(toBoxClass(cls[i])))
					break;
			}
			if (i==cls.length)
				return m;
		}
		return null;
	}

	public static Method findMethod(Object o, String name,Object... param) {
		for (Method m : o.getClass().getMethods()) {
			Class<?>[] para = m.getParameterTypes();
			if (para.length!=param.length || !m.getName().equals(name))
				continue;
			int i;
			for (i=0; i<param.length; i++) {
				if (!toBoxClass(para[i]).isInstance(param[i]))
					break;
			}
			if (i==param.length)
				return m;
		}
		return null;
	}

	public static Method findMethod(Class<?> cls, String name,Object... param) {
		for (Method m : cls.getMethods()) {
			Class<?>[] para = m.getParameterTypes();
			if (para.length!=param.length || !m.getName().equals(name))
				continue;
			int i;
			for (i=0; i<param.length; i++) {
				if (!toBoxClass(para[i]).isInstance(param[i]))
					break;
			}
			if (i==param.length)
				return m;
		}
		return null;
	}
	
	/**
	 * Also non-public!
	 * @param cls
	 * @param name
	 * @param param
	 * @return
	 */
	public static Method findAnyMethod(Class<?> cls, String name,Object... param) {
		for (; cls!=null; cls = cls.getSuperclass())
			for (Method m : cls.getDeclaredMethods()) {
				Class<?>[] para = m.getParameterTypes();
				if (para.length!=param.length || !m.getName().equals(name))
					continue;
				int i;
				for (i=0; i<param.length; i++) {
					if (!toBoxClass(para[i]).isInstance(param[i]))
						break;
				}
				if (i==param.length)
					return m;
			}
		return null;
	}
	
	/**
	 * Also non-public!
	 * @param cls
	 * @param name
	 * @param param
	 * @return
	 */
	public static <T> Constructor<T> findAnyConstructor(Class<T> cls, Object... param) {
		for (Constructor<?> m : cls.getDeclaredConstructors()) {
			Class<?>[] para = m.getParameterTypes();
			if (para.length!=param.length)
				continue;
			int i;
			for (i=0; i<param.length; i++) {
				if (!toBoxClass(para[i]).isInstance(param[i]))
					break;
			}
			if (i==param.length)
				return (Constructor<T>) m;
		}
		return null;
	}

	
	/**
	 * Also non-public!
	 * @param cls
	 * @param name
	 * @param param
	 * @return
	 */
	public static Field findAnyField(Class<?> cls, String name, boolean staticField) {
		for (; cls!=null; cls = cls.getSuperclass())
			for (Field m : cls.getDeclaredFields()) {
				if (Modifier.isStatic(m.getModifiers())==staticField && m.getName().equals(name))
					return m;
			}
		return null;
	}

	public static Method findMethod(Class<?> cls, String name,Class<?>... param) {
		for (Method m : cls.getMethods()) {
			Class<?>[] para = m.getParameterTypes();
			if (para.length!=param.length || !m.getName().equals(name))
				continue;
			if (Arrays.equals(param, para))
				return m;
		}
		return null;
	}
	public static Method findMethodIgnoreCase(Class<?> cls, String name,Class<?>... param) {
		for (Method m : cls.getMethods()) {
			Class<?>[] para = m.getParameterTypes();
			if (para.length!=param.length || !m.getName().equalsIgnoreCase(name))
				continue;
			if (Arrays.equals(param, para))
				return m;
		}
		return null;
	}
	public static Method findMethodAllowSuper(Class<?> cls, String name,Class<?>... param) {
		for (Method m : cls.getMethods()) {
			Class<?>[] para = m.getParameterTypes();
			if (para.length!=param.length || !m.getName().equals(name))
				continue;
			if (areAssignable(para,param))
				return m;
		}
		return null;
	}

	private static boolean areAssignable(Class<?>[] superClasses, Class<?>[] extendingClasses) {
		if (superClasses.length!=extendingClasses.length)
			return false;
		for (int i=0; i<superClasses.length; i++)
			if (!superClasses[i].isAssignableFrom(extendingClasses[i]))
				return false;
		return true;
	}

	/**
	 * The java primitive types, parallel to {@link #PrimitiveBoxTypes} and
	 * {@link #PrimitiveDefaults}
	 */
	public static Class<?>[] PrimitiveTypes = {Byte.TYPE,Short.TYPE,Integer.TYPE,Long.TYPE,Float.TYPE,Double.TYPE,Boolean.TYPE,Character.TYPE};
	/**
	 * The java primitive sizes in bits, parallel to {@link #PrimitiveBoxTypes} and
	 * {@link #PrimitiveDefaults}
	 */
	public static int[] PrimitiveSizes = {Byte.SIZE,Short.SIZE,Integer.SIZE,Long.SIZE,Float.SIZE,Double.SIZE,1,Character.SIZE};
	/**
	 * The java primitive types, parallel to {@link #PrimitiveBoxTypes} and
	 * {@link #PrimitiveDefaults}
	 */
	public static String[] DefaultFormatting = {"%d","%d","%d","%d","%.1f","%.1f","%b","%s"};
	/**
	 * The java box types, parallel to {@link #PrimitiveTypes} and
	 * {@link #PrimitiveDefaults}
	 */
	public static Class<?>[] PrimitiveBoxTypes = {Byte.class,Short.class,Integer.class,Long.class,Float.class,Double.class,Boolean.class,Character.class};
	/**
	 * The java primitive default values, parallel to {@link #PrimitiveTypes} and
	 * {@link #PrimitiveBoxTypes}
	 */
	public static Object[] PrimitiveDefaults = {(byte) 0,(short) 0, 0 ,0L, 0.0f, 0.0, false, '\0'};
	public static Object[] PrimitiveEmptyArrays = {new byte[0],new short[0], new int[0], new long[0], new float[0], new double[0], new boolean[0], new char[0]};


	public static HashMap<Class<?>,Integer> PrimitiveTypeIndex;
	static {
		PrimitiveTypeIndex = ArrayUtils.createIndexMap(PrimitiveTypes);
	}

	/**
	 * Enhancement of {@link Class#forName(String)}, that also recognizes
	 * primitive types.
	 * @param name the class name
	 * @return the class
	 * @throws ClassNotFoundException
	 */
	public static Class<?> getClassByString(String name) throws ClassNotFoundException {
		for (int i=0; i<PrimitiveTypes.length; i++)
			if (name.equals(PrimitiveTypes[i].getName()))
				return PrimitiveTypes[i];
		return Class.forName(name);
	}

	/**
	 * Converts the primitive type to its boxed class version
	 * @see #toPrimitveClass(Class)
	 * @param type the type
	 * @return the box class
	 */
	public static <T> Class<T> toBoxClass(Class<T> type) {
		if (!type.isPrimitive()) return type;
		int primi = ArrayUtils.find(PrimitiveTypes, type);
		if (primi>=0)
			return (Class<T>) PrimitiveBoxTypes[primi];
		else 
			return type;
	}

	/**
	 * Converts the box class to the primitive type version
	 * @see #toBoxClass(Class)
	 * @param type the box class
	 * @return the primitive type
	 */
	public static <T> Class<T> toPrimitveClass(Class<T> type) {
		int primi = ArrayUtils.find(PrimitiveBoxTypes, type);
		if (primi>=0)
			return (Class<T>) PrimitiveTypes[primi];
		else 
			return type;
	}

	/**
	 * Converts the box class to the primitive type version
	 * @see #toBoxClass(Class)
	 * @param type the box class
	 * @return the primitive type
	 */
	public static String getDefaultFormatting(Class<?> type) {
		int primi = ArrayUtils.find(PrimitiveTypes, toPrimitveClass(type));
		if (primi>=0)
			return DefaultFormatting[primi];
		else 
			return "%s";
	}

	public static <T> T primitiveCast(double v, Class<T> cls) {
		if (cls==Integer.class || cls==Integer.TYPE)
			return (T)(new Integer((int)v));

		if (cls==Double.class || cls==Double.TYPE)
			return (T)(new Double((double)v));

		if (cls==Float.class || cls==Float.TYPE)
			return (T)(new Float((float)v));


		throw new RuntimeException("Implement me!");
	}

	public static <T> boolean isPrimitiveOrStruct(Class<T> cls) {
		if (cls.isPrimitive()) return true;

		HashSet<Class<?>> encountered = new HashSet<Class<?>>();
		Stack<Class<?>> search = new Stack<>();
		search.push(cls);
		while (!search.isEmpty()) {
			Class<?> c = search.pop();
			encountered.add(c);

			for (Field f : c.getFields()) {
				if (encountered.contains(f.getType())) return false; // recursive usage of a class
				if (f.getType().isArray()) return false; // arrays are no primitive or struct
				if (!f.getType().isPrimitive())
					search.push(f.getType());
			}
		}
		return true;
	}

	public static <T> int getPrimitiveOrStructSize(Class<T> cls) {
		if (cls.isPrimitive()) return PrimitiveTypeIndex.get(cls)*8;

		HashSet<Class<?>> encountered = new HashSet<Class<?>>();
		Stack<Class<?>> search = new Stack<>();
		search.push(cls);
		int re = 0;
		while (!search.isEmpty()) {
			Class<?> c = search.pop();
			encountered.add(c);

			for (Field f : c.getFields()) {
				if (encountered.contains(f.getType())) throw new UnsupportedOperationException(); // recursive usage of a class
				if (f.getType().isArray()) throw new UnsupportedOperationException(); // arrays are no primitive or struct
				if (!f.getType().isPrimitive())
					search.push(f.getType());
				else
					re+=PrimitiveTypeIndex.get(cls)*8;
			}
		}
		return re;
	}

	
	public static <T> T getPrivate(Object o, String field) throws NoSuchFieldException, SecurityException {
		return (T) getPrivate(o,(Class)o.getClass(),field);
	}
	
	public static <C,T> T getPrivate(C o, Class<? super C> cls, String field) throws NoSuchFieldException, SecurityException {
		try {
			Field f = o.getClass().getDeclaredField(field);
			f.setAccessible(true);
			try {
				return (T) f.get(o);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("Cannot be!",e);
			}
		} catch (NoSuchFieldException | SecurityException e) {
			if (cls==Object.class) throw e;
			return getPrivate(o,cls.getSuperclass(),field);
		}
	}

	public static <T,O> O invoke(T o, String method, Object...args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method m = findAnyMethod(o.getClass(), method, args);
		if (!Modifier.isPublic(m.getModifiers()))
			m.setAccessible(true);
		return (O) m.invoke(o, args);
	}
	
	public static <T,O> O invokeStatic(Class<T> cls, String method, Object...args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method m = findAnyMethod(cls, method, args);
		if (!Modifier.isPublic(m.getModifiers()))
			m.setAccessible(true);
		return (O) m.invoke(null, args);
	}
	
	public static <T> boolean has(T o, String field) {
		return findAnyField(o.getClass(), field, false)!=null;
	}
	
	public static <T> boolean hasStatic(Class<T> o, String field) {
		return findAnyField((Class<?>) o, field, true)!=null;
	}

	public static <T,O> O get(T o, String field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field f = findAnyField(o.getClass(), field, false);
		if (!Modifier.isPublic(f.getModifiers()))
			f.setAccessible(true);
		return (O) f.get(o);
	}
	
	public static <T,O> O getStatic(Class<T> o, String field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field f = findAnyField(o, field, true);
		if (!Modifier.isPublic(f.getModifiers()))
			f.setAccessible(true);
		return (O) f.get(null);
	}
	public static <T,O> void setStatic(Class<T> o, String field, O toset) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field f = findAnyField(o, field, true);
		if (!Modifier.isPublic(f.getModifiers()))
			f.setAccessible(true);
		f.set(null, toset);
	}
	
	public static <T,O> void set(T o, String field, O toset) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field f = findAnyField(o.getClass(), field, false);
		if (!Modifier.isPublic(f.getModifiers()))
			f.setAccessible(true);
		f.set(o,toset);
	}

	public static <T> T newInstance(Class<T> cls, Object...args) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException {
		Constructor<T> m = findAnyConstructor(cls, args);
		if (!Modifier.isPublic(m.getModifiers()))
			m.setAccessible(true);
		return m.newInstance(args);
	}

	/**
	 * Returns null if enum does not contain value!
	 * @param enumClass
	 * @param value
	 * @return
	 */
	public static <E extends Enum<E>> E valueOf(
			Class<E> enumClass, String value) {
		try {
			Map<String, E> map = invoke(enumClass, "enumConstantDirectory");
			return map.get(value);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return null;
		}
		
	}
}
