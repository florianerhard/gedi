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
package gedi.util.orm;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.FileUtils;
import gedi.util.ReflectionUtils;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutableLong;
import gedi.util.mutable.MutableMonad;
import gedi.util.orm.Orm.OrmInfo;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;

import sun.misc.Unsafe;


/**
 * Extremely fast serialization/deserialization for simple objects (i.e. no cycles and no polymorphism are allowed; polymorphism means that the objects in the object tree must be from the same class as the initial object in the ctor)
 * 
 * Usage should be as follows: You have many objects of the same type you want to serialize. 
 * 1. Create a class tree for the first object
 * 2. call {@link #toBuffer(Object, MutableInteger)} for the first object
 * 3. save the returned buffer from 0 to the size in the MutableInteger
 * 4. call {@link #toBuffer(Object, byte[], MutableInteger)} for the next object reusing the buffer
 * 5. and so on
 * 
 * To deserialize, use the same ClassTree
 * 1. Create a large enough buffer (byte[])
 * 2. read into the buffer
 * 2. call {@link #fromBuffer(byte[])} to get the first object
 * 
 * You may also consider to gzip the buffer ({@link gedi.util.ArrayUtils#packRaw(byte[])})
 * 
 * @author erhard
 *
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class ClassTree<T> implements BinarySerializable {
	
	private static int ao = Orm.getUnsafe().arrayBaseOffset(byte[].class);
	
	private Class<T> cls;
	private boolean isenum;
	private boolean isprimitive;
	private boolean isarray;
	private boolean isVariableSize;
	
	private ClassTree[] children;
	private long[] start;
	private long[] end; // end-start > 0 for primitives only
	private GenomicRegion primitiveRegion = new ArrayGenomicRegion();
	
	private ClassTree arrayElementTree;
	
	
	
	
	
	public ClassTree(T root) {
		this(root,false, new  IdentityHashMap());
	}
	
	private ClassTree(T root, boolean isPrimitve, IdentityHashMap circleDetection) {
		if (circleDetection.put(root, root)!=null) 
			throw new RuntimeException("Circle detected: "+root);
		
		
		cls = (Class<T>) root.getClass();
		isenum = cls.isEnum();
		isprimitive = isPrimitve;
		isarray = cls.isArray();
		
		if (this.cls.isSynthetic())
			throw new RuntimeException("Cannot compute class trees with synthetic classes!");
		if (isPrimitve) cls = ReflectionUtils.toPrimitveClass(cls);
		
		if (isarray) {
			if (Array.getLength(root)>0) {
				arrayElementTree = new ClassTree(Array.get(root, 0), cls.getComponentType().isPrimitive(), circleDetection);
				// check for polymorphic array
				for (int i=1; i<Array.getLength(root); i++)
					if (Array.get(root,i).getClass()!=Array.get(root,0).getClass())
						throw new RuntimeException("Polymorphic arrays are not allowed!");
			}
			else if (cls.getComponentType().isPrimitive())
				arrayElementTree = new ClassTree(ReflectionUtils.PrimitiveDefaults[ReflectionUtils.PrimitiveTypeIndex.get(cls.getComponentType())], cls.getComponentType().isPrimitive(), circleDetection);
			else
				throw new RuntimeException("Cannot compute class tree with an empty non-primitive array!");
			isVariableSize = true;
		}
		
		OrmInfo info = Orm.getInfo(cls);
		Field[] f = info.getFields();
		children = new ClassTree[f.length];
		start = new long[f.length];
		end = new long[f.length];
		
		for (int i=0; i<f.length; i++) {
			f[i].setAccessible(true);
			try {
				Object o = f[i].get(root);
				if (o==null)  
					o = createPrimitiveArray(f[i].getType());
				if (o==null)
					throw new RuntimeException("Cannot compute class tree with a null pointer!");
				children[i] = new ClassTree(o,f[i].getType().isPrimitive(), circleDetection);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException("Cannot compute class tree!",e);
			}
			start[i] = info.getPointerOffsets()[i];
			end[i] = start[i] + children[i].getBytes();
			ArrayGenomicRegion tr = new ArrayGenomicRegion((int)start[i],(int)end[i]);
			primitiveRegion = primitiveRegion.union(tr);
			isVariableSize |= children[i].isVariableSize;
		}
		
	}

	private boolean isPrimitiveArray(Class<?> cls) {
		if (!cls.isArray()) return false;
		if (cls.getComponentType().isPrimitive()) return true;
		return isPrimitiveArray(cls.getComponentType());
	}
	
	private Object createPrimitiveArray(Class<?> cls) {
		if (!cls.isArray()) return null;
		if (cls.getComponentType().isPrimitive()) {
			int i=ReflectionUtils.PrimitiveTypeIndex.get(cls.getComponentType());
			return ReflectionUtils.PrimitiveEmptyArrays[i];
		};
		return Array.newInstance(cls.getComponentType(), 0);
	}

	public boolean isVariableSize() {
		return isVariableSize;
	}
	
	public Class<T> getType() {
		return cls;
	}
	
	public T fromBuffer(byte[] buffer, MutableInteger offset) {
		MutableLong off = new MutableLong(offset.N+Orm.getUnsafe().arrayBaseOffset(byte[].class));
		T re = fromBuffer(buffer, off);
		offset.N = (int)off.N;
		return re;
	}
	
	public T fromBuffer(byte[] buffer, int offset) {
		return fromBuffer(buffer, new MutableLong(offset+Orm.getUnsafe().arrayBaseOffset(byte[].class)));
	}
	
	public T fromBuffer(byte[] buffer) {
		return fromBuffer(buffer, new MutableLong(Orm.getUnsafe().arrayBaseOffset(byte[].class)));
	}
	private T fromBuffer(byte[] buffer, MutableLong offset) {
		Unsafe unsafe = Orm.getUnsafe();
		
		int bytes = getBytes();
		if (bytes>0) {
			T re = null;
			if (isprimitive) {
//				if (cls==boolean.class) re = unsafe.getBoolean(buffer, offset.N);
//				else if (cls==byte.class) re = unsafe.getByte(buffer, offset.N);
//				else if (cls==short.class) re = unsafe.getShort(buffer, offset.N);
//				else if (cls==char.class) re = unsafe.getChar(buffer, offset.N);
//				else if (cls==int.class) re = unsafe.getInt(buffer, offset.N);
//				else if (cls==long.class) re = unsafe.getLong(buffer, offset.N);
//				else if (cls==float.class) re = unsafe.getFloat(buffer, offset.N);
//				else if (cls==double.class) re = unsafe.getDouble(buffer, offset.N);
//				else throw new RuntimeException();
				throw new RuntimeException("Cannot do this for primitives!");
			}
			offset.N+=bytes;
			return re;
		}
		
		if (isenum) {
			T re = cls.getEnumConstants()[unsafe.getInt(checkGetSize(buffer,offset.N,Integer.BYTES), offset.N)];
			offset.N+=Integer.BYTES;
			return re;
		}
		
		if (isarray) {
			int l = unsafe.getInt(checkGetSize(buffer,offset.N,Integer.BYTES), offset.N);
			offset.N+=Integer.BYTES;
			
			if (l==-1) {
				return null;
			}
			T o = (T) Array.newInstance(cls.getComponentType(), l);
			
			if (cls.getComponentType().isPrimitive()) {
				int abytes = unsafe.arrayIndexScale(cls)*l; 
				unsafe.copyMemory(checkGetSize(buffer,offset.N,abytes), offset.N, o, unsafe.arrayBaseOffset(cls), abytes);
				offset.N+=abytes;
			} else {
				for (int i=0; i<l; i++) {
					Array.set(o, i, arrayElementTree.fromBuffer(buffer, offset));
				}
			}
			return o;
		}
		// normal class with primitives and pointers
		T re = Orm.create(cls);
		checkGetSize(buffer,offset.N,primitiveRegion.getTotalLength());
		for (int i=0; i<primitiveRegion.getNumParts(); i++) {
			for (int w=0; w<primitiveRegion.getLength(i); w+=Long.BYTES)
				if (w+Long.BYTES<=primitiveRegion.getLength(i)) {
					unsafe.putLong(re, (long)(primitiveRegion.getStart(i)+w), unsafe.getLong(buffer, offset.N));
					offset.N+=Long.BYTES;
				} else if (w+Integer.BYTES<=primitiveRegion.getLength(i)) {
					unsafe.putInt(re, (long)(primitiveRegion.getStart(i)+w), unsafe.getInt(buffer, offset.N));
					offset.N+=Integer.BYTES;
				} else
					throw new RuntimeException();
		}
		for (int i=0; i<start.length; i++) {
			if (end[i]-start[i]==0)
				unsafe.putObject(re, start[i], children[i].fromBuffer(buffer, offset));
		}
		return re;
		
	}
	private byte[] checkGetSize(byte[] buffer, long offset, int bytes) {
		if (offset-ao+bytes>buffer.length) {
			throw new IndexOutOfBoundsException("Buffer is too small!");
		}
		return buffer;
	}
	
	public byte[] toBuffer(T o) {
		return toBuffer(o, null, null);
	}
	
	public byte[] toBuffer(T o, byte[] buffer) {
		return toBuffer(o, buffer, null);
	}
	public byte[] toBuffer(T o, MutableInteger size) {
		return toBuffer(o, null, size);
	}
	
	public byte[] toBuffer(T o, byte[] buffer, MutableInteger size) {
		if (buffer==null) buffer = new byte[64*1024];
		MutableMonad<byte[]> box = new MutableMonad<byte[]>(buffer);
		
		long pos = toBuffer(o, box,ao);
		if (size!=null)
			size.N = (int)(pos-ao);
		return box.Item;
	}
	private long toBuffer(T o, MutableMonad<byte[]> buffer, long offset) {
		if (o!=null && o.getClass()!=cls) 
			throw new RuntimeException("No polymorphic objects are allowed; expected: "+cls+", got: "+o.getClass());
		
		Unsafe unsafe = Orm.getUnsafe();
		
		int bytes = getBytes();
		if (bytes>0) {
			if (isprimitive) {
//				if (cls==boolean.class) unsafe.putBoolean(buffer, offset, (Boolean)o);
//				else if (cls==byte.class) unsafe.putByte(buffer, offset, (Byte)o);
//				else if (cls==short.class) unsafe.putShort(buffer, offset, (Short)o);
//				else if (cls==char.class) unsafe.putChar(buffer, offset, (Character)o);
//				else if (cls==int.class) unsafe.putInt(buffer, offset, (Integer)o);
//				else if (cls==long.class) unsafe.putLong(buffer, offset, (Long)o);
//				else if (cls==float.class) unsafe.putFloat(buffer, offset, (Float)o);
//				else if (cls==double.class) unsafe.putDouble(buffer, offset, (Double)o);
//				else 
				throw new RuntimeException("Cannot do this for primitives!");
			}
			return offset+bytes;
		}
		
		if (isenum) {
			unsafe.putInt(checkPutSize(buffer,offset,Integer.BYTES), offset, ((Enum<?>)o).ordinal());
			return offset+Integer.BYTES;
		}
		
		if (isarray) {
			int l = o==null?-1:Array.getLength(o);
			unsafe.putInt(checkPutSize(buffer,offset,Integer.BYTES), offset, l);
			offset+=Integer.BYTES;
			
			if (o!=null) {
				if (cls.getComponentType().isPrimitive()) {
					int abytes = unsafe.arrayIndexScale(cls)*l; 
					unsafe.copyMemory(o, unsafe.arrayBaseOffset(cls), checkPutSize(buffer,offset,abytes), offset, abytes);
					offset+=abytes;
				} else {
					for (int i=0; i<l; i++) {
						Object e = Array.get(o,i);
						offset = arrayElementTree.toBuffer(e, buffer, offset);
						if (arrayElementTree.cls!=ReflectionUtils.toPrimitveClass(e.getClass())) 
							throw new RuntimeException("You cannot mix classes in arrays!");
					}
				}
			}
			return offset;
		}
		
		// normal class with primitives and pointers
		byte[] buff = checkPutSize(buffer,offset,primitiveRegion.getTotalLength());
		for (int i=0; i<primitiveRegion.getNumParts(); i++) {
			for (int w=0; w<primitiveRegion.getLength(i); w+=Long.BYTES)
				if (w+Long.BYTES<=primitiveRegion.getLength(i)) {
					unsafe.putLong(buff, offset, unsafe.getLong(o,(long)primitiveRegion.getStart(i)+w));
					offset+=Long.BYTES;
				} else if (w+Integer.BYTES<=primitiveRegion.getLength(i)) {
					unsafe.putInt(buff, offset, unsafe.getInt(o,(long)primitiveRegion.getStart(i)+w));
					offset+=Integer.BYTES;
				} else
					throw new RuntimeException();
		}
		
		// incompatible with openjdk 11 
//		for (int i=0; i<primitiveRegion.getNumParts(); i++) {
//			unsafe.copyMemory(o, primitiveRegion.getStart(i), checkPutSize(buffer,offset,primitiveRegion.getLength(i)), offset, primitiveRegion.getLength(i));
//			offset+=primitiveRegion.getLength(i);
//		}
		for (int i=0; i<start.length; i++) {
			if (end[i]-start[i]==0)
				offset = children[i].toBuffer(unsafe.getObject(o, start[i]),buffer,offset);
		}
		return offset;
	}
	
	private byte[] checkPutSize(MutableMonad<byte[]> buffer, long offset, int bytes) {
		while (offset-ao+bytes>buffer.Item.length) {
			byte[] nb = new byte[buffer.Item.length*2];
			System.arraycopy(buffer.Item, 0, nb, 0, buffer.Item.length);
			buffer.Item = nb;
		}
		return buffer.Item;
	}


	public int computeSize(T o) {
		if (getBytes()>0) return getBytes();
		if (cls.isArray()) {
			int re = Integer.BYTES;
			int l = Array.getLength(o);
			if (cls.getComponentType().isPrimitive()) {
				re += Orm.getUnsafe().arrayIndexScale(cls)*l;
			} else {
				for (int i=0; i<l; i++) {
					re += arrayElementTree.computeSize(Array.get(o,i));
				}
			}
			return re;
		}
		int re = 0;
		for (int i=0; i<start.length; i++) {
			re+=end[i]-start[i];
			if (end[i]-start[i]==0)
				re+=children[i].computeSize(Orm.getUnsafe().getObject(o, start[i]));
		}
		return re;
	}
	
	
	/**
	 * Bytes to store this, i.e. nothing for objects/arrays, size(int) for enum 
	 * @return
	 */
	private int getBytes() {
		if (cls.isPrimitive()) {
			if (cls==boolean.class) return Byte.BYTES;
			if (cls==byte.class) return Byte.BYTES;
			if (cls==short.class) return Short.BYTES;
			if (cls==char.class) return Character.BYTES;
			if (cls==int.class) return Integer.BYTES;
			if (cls==long.class) return Long.BYTES;
			if (cls==float.class) return Float.BYTES;
			if (cls==double.class) return Long.BYTES;
			throw new RuntimeException();
		}
		return 0;
	}
	
	
	@Override
	public String toString() {
		return toString(new StringBuilder(), 0).toString();
	}
	
	public StringBuilder toString(StringBuilder sb, int indent) {
		for (int i=0; i<indent; i++) sb.append(" ");
		sb.append(cls.getName());
		sb.append("\n");
		for (int c=0; c<children.length; c++) {
			for (int i=0; i<indent; i++) sb.append(" ");
			sb.append(" ").append(start[c]).append("-").append(end[c]).append("\n");
			children[c].toString(sb, indent+2);
		}
		return sb;
	}

	
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(cls.getName());
		int flags = (isenum?1<<3:0) |
				(isprimitive?1<<2:0) |
				(isarray?1<<1:0) |
				(isVariableSize?1<<0:0);
		out.putByte(flags);
		if (isarray) {
			arrayElementTree.serialize(out);
		} else if (!isprimitive && !isenum){
			FileUtils.writeGenomicRegion(out, primitiveRegion);
			out.putCInt(children.length);
			for (int i=0; i<children.length; i++) {
				out.putCLong(start[i]);
				out.putCLong(end[i]);
			}
			for (int i=0; i<children.length; i++)
				children[i].serialize(out);
			
		} else {
			if (children.length!=0 || primitiveRegion.getTotalLength()!=0) throw new RuntimeException("There should be no fields here!");
		}
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		try {
			cls = (Class<T>) Class.forName(in.getString());
		} catch (ClassNotFoundException e) {
			throw new IOException("Could not read class!",e);
		}
		int flags = in.getByte();
		isenum = (flags & (1<<3))!=0;
		isprimitive = (flags & (1<<2))!=0;
		isarray = (flags & (1<<1))!=0;
		isVariableSize = (flags & (1<<0))!=0;
		
		if (isarray) {
			arrayElementTree = Orm.create(ClassTree.class);
			arrayElementTree.deserialize(in);
		} else {
			primitiveRegion = FileUtils.readGenomicRegion(in);
			children = new ClassTree[in.getCInt()];
			start = new long[children.length];
			end = new long[children.length];
			for (int i=0; i<children.length; i++) {
				start[i] = in.getCLong();
				end[i] = in.getCLong();
			}
			for (int i=0; i<children.length; i++) {
				children[i] = Orm.create(ClassTree.class);
				children[i].deserialize(in);
			}
		}
	}
	
	
	
	
}

