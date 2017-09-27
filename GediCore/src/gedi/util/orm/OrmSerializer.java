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

import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.orm.Orm.OrmInfo;
import gedi.util.orm.special.SpecialSerializerExtensionPoint;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;

import sun.misc.Unsafe;

/**
 * Each record starts with two cints (class id and object id). This combination always determines, whether objects are cached, and whether the current object is cached:
 * 0,0: null
 * 0,n: is a cached object, no data follow, already read
 * n,0: no caching; data follow
 * n,n: first time this object, cached under this id; data follow
 * 
 * @author erhard
 *
 */

@SuppressWarnings({"unchecked","rawtypes"})
public class OrmSerializer {

	
	
	private ArrayList<Class> classtable = new ArrayList<Class>();
	private HashMap<Class,Integer> classindex = new HashMap<Class,Integer>();

	
	private ArrayList<Object> objecttable = new ArrayList<Object>();
	private IdentityHashMap<Object,Integer> objectindex = new IdentityHashMap<Object,Integer>();

	private boolean cacheObjects = false;
	private boolean useBinarySerializable = false;
	
	private SpecialSerializerExtensionPoint extPoint = SpecialSerializerExtensionPoint.getInstance();
	
	public OrmSerializer() {
		objecttable.add(null);
		classtable.add(null);
	}
	
	public OrmSerializer(boolean cacheObjects, boolean useBinarySerializable) {
		this.cacheObjects = cacheObjects;
		this.useBinarySerializable = useBinarySerializable;
		objecttable.add(null);
		classtable.add(null);
	}

	
	public OrmSerializer addClasses(Class... classes) {
		for (Class cls : classes)
			obtainClassId(cls);
		return this;
	}
	
	/**
	 * Starting from 1, 0 means that object is already known!
	 * @param cls
	 * @return
	 */
	private int obtainClassId(Class cls) {
		Integer id = classindex.get(cls);
		if (id==null) {
			classindex.put(cls, id = classindex.size()+1);
			classtable.add(cls);
		}
		return id;
	}
	private Class obtainClass(int id) {
		return classtable.get(id);
	}
	
	private Integer getObjectId(Object o) {
		return objectindex.get(o);
	}
	
	private int indexObject(Object o, boolean throwIfIndexed) {
		if (objectindex.containsKey(o)) {
			if (throwIfIndexed)
				throw new RuntimeException("Object already indexed!");
			return objectindex.get(o);
		}
		int re;
		objectindex.put(o, re = objectindex.size()+1);
		objecttable.add(o);
//		System.out.println("Indexing object "+o.getClass()+" "+re+" ");
		return re;
	}
	Object placeholder = new Object();
	private void replaceIndexObject(Object o, int index) {
		if (objecttable.size()<=index) throw new RuntimeException("Cannot update object index");
		objectindex.remove(objecttable.get(index));
		objectindex.put(o, index);
		objecttable.set(index, o);
	}
	private <T> T getObject(int oid) {
		return (T) objecttable.get(oid);
	}
	
	public OrmSerializer clearObjectCache() {
		objectindex.clear();
		objecttable.clear();
		objecttable.add(null);
		return this;
	}
	
	
	
	public void serializeAll(BinaryWriter writer, Iterator<?> objects) throws IOException {
		writer.putAsciiChars("ORM");
		long offset = writer.position();
		writer.putLong(0);
		writer.putLong(0);
		
		long count = 0;
		
		while (objects.hasNext()) { 
			serialize(writer, objects.next());
			count++;
		}
		
		long table = writer.position();
		serializeClasstable(writer);
		writer.putLong(offset, table);
		writer.putLong(offset+8, count);
	}
	
	
	public void serializeClasstable(BinaryWriter writer) throws IOException {
		writer.putCInt(classtable.size());
		for (int i=1; i<classtable.size(); i++)
			writer.putString(classtable.get(i).getName());
	}
	
	public void serialize(BinaryWriter writer, Object o) throws IOException {
		if (o==null) {
			writer.putCInt(0);
			writer.putCInt(0);
			return;
		}
		
		
		if (getObjectId(o)!=null) 
			writer.putCInt(0);
		else 
			writer.putCInt(obtainClassId(o.getClass()));

		serializeWithoutClass(writer, o);
	}
	
	public void serializeWithoutClass(BinaryWriter writer, Object o) throws IOException {
		Integer oid = getObjectId(o);
		if (oid!=null) {
			writer.putCInt(oid);
		} else {
			oid = 0;
			if (cacheObjects) oid = indexObject(o,true);
			writer.putCInt(oid);
			
			if (useBinarySerializable && o instanceof BinarySerializable)((BinarySerializable)o).serialize(writer);
			else if (extPoint.contains(o.getClass())) {
				extPoint.<Object>get(o.getClass()).serialize(this,writer, o);
			}
			else if (o.getClass().isArray()) {
				if (o.getClass()==boolean[].class) {
					boolean[] a = (boolean[]) o;
					int[] p = pack(a);
					writer.putCInt(a.length);
					for (int i=0; i<p.length; i++)
						writer.putByte(p[i]);
				} else if (o.getClass()==byte[].class) {
					byte[] a = (byte[]) o;
					writer.putCInt(a.length);
					for (int i=0; i<a.length; i++)
						writer.putByte(a[i]);
				} else if (o.getClass()==short[].class) {
					short[] a = (short[]) o;
					writer.putCInt(a.length);
					for (int i=0; i<a.length; i++)
						writer.putShort(a[i]);
				} else if (o.getClass()==int[].class) {
					int[] a = (int[]) o;
					writer.putCInt(a.length);
					for (int i=0; i<a.length; i++)
						writer.putInt(a[i]);
				} else if (o.getClass()==long[].class) {
					long[] a = (long[]) o;
					writer.putCInt(a.length);
					for (int i=0; i<a.length; i++)
						writer.putLong(a[i]);
				} else if (o.getClass()==char[].class) {
					char[] a = (char[]) o;
					writer.putCInt(a.length);
					for (int i=0; i<a.length; i++)
						writer.putChar(a[i]);
				} else if (o.getClass()==float[].class) {
					float[] a = (float[]) o;
					writer.putCInt(a.length);
					for (int i=0; i<a.length; i++)
						writer.putFloat(a[i]);
				} else if (o.getClass()==double[].class) {
					double[] a = (double[]) o;
					writer.putCInt(a.length);
					for (int i=0; i<a.length; i++)
						writer.putDouble(a[i]);
				}else {
					int l = Array.getLength(o);
					writer.putCInt(l);
					for (int i=0; i<l; i++) {
						serialize(writer,Array.get(o, i));
					}
				}
			}
			else if (o.getClass().isEnum()) {
				Enum a = (Enum) o;
				writer.putCInt(a.ordinal());
			}
			else {
				Unsafe unsafe = Orm.getUnsafe();
				OrmInfo info = Orm.getInfo(o.getClass());
				Field[] fields = info.getFields();
				long[] offset = info.getPointerOffsets();
				for (int fieldIndex=0; fieldIndex<fields.length; fieldIndex++) {
					if (fields[fieldIndex].getType().isPrimitive()) {
						if (fields[fieldIndex].getType()==boolean.class) 
							writer.putByte(unsafe.getBoolean(o, offset[fieldIndex])?1:0);
						else if (fields[fieldIndex].getType()==byte.class) 
							writer.putByte(unsafe.getByte(o, offset[fieldIndex]));
						else if (fields[fieldIndex].getType()==short.class) 
							writer.putShort(unsafe.getShort(o, offset[fieldIndex]));
						else if (fields[fieldIndex].getType()==int.class) 
							writer.putInt(unsafe.getInt(o, offset[fieldIndex]));
						else if (fields[fieldIndex].getType()==long.class) 
							writer.putLong(unsafe.getLong(o, offset[fieldIndex]));
						else if (fields[fieldIndex].getType()==char.class) 
							writer.putChar(unsafe.getChar(o, offset[fieldIndex]));
						else if (fields[fieldIndex].getType()==float.class) 
							writer.putFloat(unsafe.getFloat(o, offset[fieldIndex]));
						else if (fields[fieldIndex].getType()==double.class) 
							writer.putDouble(unsafe.getDouble(o, offset[fieldIndex]));
						else throw new RuntimeException();
					}
					else if (extPoint.contains(fields[fieldIndex].getType())) {
						extPoint.<Object>get(fields[fieldIndex].getType()).serialize(this,writer, unsafe.getObject(o, offset[fieldIndex]));
					}
					else if (fields[fieldIndex].getType().isArray()) {
						if (fields[fieldIndex].getType()==boolean[].class) {
							boolean[] a = (boolean[]) unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else {
								int[] p = pack(a);
								writer.putCInt(a.length+1);
								for (int i=0; i<p.length; i++)
									writer.putByte(p[i]);
							}
						} else if (fields[fieldIndex].getType()==byte[].class) {
							byte[] a = (byte[]) unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else {
								writer.putCInt(a.length+1);
								for (int i=0; i<a.length; i++)
									writer.putByte(a[i]);
							}
						} else if (fields[fieldIndex].getType()==short[].class) {
							short[] a = (short[]) unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else {
								writer.putCInt(a.length+1);
								for (int i=0; i<a.length; i++)
									writer.putShort(a[i]);
							}
						} else if (fields[fieldIndex].getType()==int[].class) {
							int[] a = (int[]) unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else {
								writer.putCInt(a.length+1);
								for (int i=0; i<a.length; i++)
									writer.putInt(a[i]);
							}
						} else if (fields[fieldIndex].getType()==long[].class) {
							long[] a = (long[]) unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else {
								writer.putCInt(a.length+1);
								for (int i=0; i<a.length; i++)
									writer.putLong(a[i]);
							}
						} else if (fields[fieldIndex].getType()==char[].class) {
							char[] a = (char[]) unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else {
								writer.putCInt(a.length+1);
								for (int i=0; i<a.length; i++)
									writer.putChar(a[i]);
							}
						} else if (fields[fieldIndex].getType()==float[].class) {
							float[] a = (float[]) unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else {
								writer.putCInt(a.length+1);
								for (int i=0; i<a.length; i++)
									writer.putFloat(a[i]);
							}
						} else if (fields[fieldIndex].getType()==double[].class) {
							double[] a = (double[]) unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else {
								writer.putCInt(a.length+1);
								for (int i=0; i<a.length; i++)
									writer.putDouble(a[i]);
							}
						} else {
							Object a = unsafe.getObject(o, offset[fieldIndex]);
							if (a==null) {
								writer.putCInt(0);
							} else if (getObjectId(a)!=null) {
								writer.putCInt(1);
								writer.putCInt(getObjectId(a));
							} else {
								writer.putCInt(2);
								
								int aoid = 0;
								if (cacheObjects) aoid = indexObject(a,true);
								writer.putCInt(aoid);
								
								int l = Array.getLength(a);
								writer.putCInt(l);
								for (int i=0; i<l; i++) {
									serialize(writer,Array.get(a, i));
								}
							}
						}
					} else if (fields[fieldIndex].getType().isEnum()) {
						Enum a = (Enum) unsafe.getObject(o, offset[fieldIndex]);
						if (a==null) {
							writer.putCInt(0);
						} else {
							writer.putCInt(a.ordinal()+1);
						}
					} else {
						Object a = unsafe.getObject(o, offset[fieldIndex]);
						serialize(writer, a);
					}
				}
			}
				
		}
			
	}

	
	
	public <T> ExtendedIterator<T> deserializeAll(BinaryReader reader) throws IOException {
		if (!reader.getAsciiChars(3).equals("ORM")) throw new IOException("Magic bytes are missing!");

		long tableOffset = reader.getLong();
		long count = reader.getLong();
		long dataOffset = reader.position();
		
		reader.position(tableOffset);
		deserializeClasstable(reader);
		final long after = reader.position();
		reader.position(dataOffset);
		
		return new ExtendedIterator() {

			long i = 0;
			@Override
			public boolean hasNext() {
				return i<count;
			}

			@Override
			public Object next() {
				try {
					i++;
					Object o = deserialize(reader);
					if (!hasNext()) // position the reader after the classtable 
						reader.position(after);
					return o;
				} catch (IOException e) {
					throw new RuntimeException("Could not deserialize!",e);
				}
			}
			
		};
	}
	
	
	public void deserializeClasstable(BinaryReader reader) throws IOException {
		int size = reader.getCInt()-1;
		for (int i=0; i<size; i++) {
			try {
				Class cls = Class.forName(reader.getString());
				classindex.put(cls, i+1);
				classtable.add(cls);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Cannot read class table!",e);
			}
		}
	}
	
	public <T> T deserialize(BinaryReader reader) throws IOException {
		Class<T> cls = obtainClass(reader.getCInt());
		return deserialize(reader, cls);
	}
	
	
	public <T> T deserialize(BinaryReader reader, Class<T> cls) throws IOException {
		if (cls==null) {// expect that this is a cached object
			int oid = reader.getCInt();
			if (oid==0) return null;
			return getObject(oid);
		}
		
		int oid = reader.getCInt();
		T re;
		boolean cached = false;
		
		if (extPoint.contains(cls)) {
			if (cacheObjects && indexObject(placeholder,true)!=oid) 
				throw new IOException("Object cache inconsistent for "+cls);
			cached = true;
			re = (T) extPoint.<Object>get(cls).deserialize(this,reader);
			replaceIndexObject(re, oid);
		} else if (cls==boolean[].class) {
			int alen = reader.getCInt();
			int plen = alen/8+((alen%8==0)?0:1);
			int[] p = new int[plen];
			for (int i=0; i<plen; i++) p[i] = reader.getByte();
			boolean[] a = unpack(alen, p);
			re = (T) a;
		} else if (cls==byte[].class) {
			int len = reader.getCInt();
			byte[] a = new byte[len];
			for (int i=0; i<len; i++) a[i] = (byte) reader.getByte();
			re = (T) a;
		} else if (cls==short[].class) {
			int len = reader.getCInt();
			short[] a = new short[len];
			for (int i=0; i<len; i++) a[i] = reader.getShort();
			re = (T) a;
		} else if (cls==int[].class) {
			int len = reader.getCInt();
			int[] a = new int[len];
			for (int i=0; i<len; i++) a[i] = reader.getInt();
			re = (T) a;
		} else if (cls==long[].class) {
			int len = reader.getCInt();
			long[] a = new long[len];
			for (int i=0; i<len; i++) a[i] = reader.getLong();
			re = (T) a;
		} else if (cls==char[].class) {
			int len = reader.getCInt();
			char[] a = new char[len];
			for (int i=0; i<len; i++) a[i] = reader.getChar();
			re = (T) a;
		} else if (cls==float[].class) {
			int len = reader.getCInt();
			float[] a = new float[len];
			for (int i=0; i<len; i++) a[i] = reader.getFloat();
			re = (T) a;
		} else if (cls==double[].class) {
			int len = reader.getCInt();
			double[] a = new double[len];
			for (int i=0; i<len; i++) a[i] = reader.getDouble();
			re = (T) a;
		} else if (cls.isEnum()) {
			int ordinal = reader.getCInt();
			Object v = cls.getEnumConstants()[ordinal];
			re = (T) v;
		} else if (cls.isArray()) {
			int len = reader.getCInt();
			Object a = Array.newInstance(cls.getComponentType(), len);
			if (cacheObjects && indexObject(a,true)!=oid) 
				throw new IOException("Object cache inconsistent!");
			cached = true;
			
			for (int i=0; i<len; i++) 
				Array.set(a, i, deserialize(reader));
			re = (T) a;
		}
		else {
		
			re = Orm.create(cls);
			if (cacheObjects && indexObject(re,true)!=oid) 
				throw new IOException("Object cache inconsistent for "+cls);
			cached = true;
			deserialize(reader,re);
		}
		
		if (!cached){
			if (cacheObjects && indexObject(re,true)!=oid) throw new IOException("Object cache inconsistent!");
		}
		
		return re;
		
	}
	
	public void deserializeWithoutClass(BinaryReader reader, Object re) throws IOException {
		int oid = reader.getCInt();
		if (cacheObjects && indexObject(re,true)!=oid) 
			throw new IOException("Object cache inconsistent for "+re);
		deserialize(reader,re);
	}
	private void deserialize(BinaryReader reader, Object re) throws IOException {
		
		if (useBinarySerializable && BinarySerializable.class.isAssignableFrom(re.getClass())) {
			((BinarySerializable)re).deserialize(reader);
		}
		else {
			Unsafe unsafe = Orm.getUnsafe();
			OrmInfo info = Orm.getInfo(re.getClass());
			Field[] fields = info.getFields();
			long[] offset = info.getPointerOffsets();
			for (int fieldIndex=0; fieldIndex<fields.length; fieldIndex++) {
				if (fields[fieldIndex].getType()==boolean.class) 
					unsafe.putBoolean(re, offset[fieldIndex], reader.getByte()!=0);
				else if (fields[fieldIndex].getType()==byte.class) 
					unsafe.putByte(re, offset[fieldIndex], (byte)reader.getByte());
				else if (fields[fieldIndex].getType()==short.class) 
					unsafe.putShort(re, offset[fieldIndex], reader.getShort());
				else if (fields[fieldIndex].getType()==int.class) 
					unsafe.putInt(re, offset[fieldIndex], reader.getInt());
				else if (fields[fieldIndex].getType()==long.class) 
					unsafe.putLong(re, offset[fieldIndex], reader.getLong());
				else if (fields[fieldIndex].getType()==char.class) 
					unsafe.putChar(re, offset[fieldIndex], reader.getChar());
				else if (fields[fieldIndex].getType()==float.class) 
					unsafe.putFloat(re, offset[fieldIndex], reader.getFloat());
				else if (fields[fieldIndex].getType()==double.class) 
					unsafe.putDouble(re, offset[fieldIndex], reader.getDouble());
				else if (extPoint.contains(fields[fieldIndex].getType())) 
					unsafe.putObject(re, offset[fieldIndex], extPoint.<Object>get(fields[fieldIndex].getType()).deserialize(this,reader));
				else if (fields[fieldIndex].getType()==boolean[].class) {
					int alen = reader.getCInt();
					if (alen==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						alen--;
						int plen = alen/8+((alen%8==0)?0:1);
						int[] p = new int[plen];
						for (int i=0; i<plen; i++) p[i] = reader.getByte();
						boolean[] a = unpack(alen, p);
						unsafe.putObject(re, offset[fieldIndex],a);
					}
				} else if (fields[fieldIndex].getType()==byte[].class) {
					int len = reader.getCInt();
					if (len==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						len--;
						byte[] a = new byte[len];
						for (int i=0; i<len; i++) a[i] = (byte) reader.getByte();
						unsafe.putObject(re, offset[fieldIndex],a);
					}
				} else if (fields[fieldIndex].getType()==short[].class) {
					int len = reader.getCInt();
					if (len==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						len--;
						short[] a = new short[len];
						for (int i=0; i<len; i++) a[i] = reader.getShort();
						unsafe.putObject(re, offset[fieldIndex],a);
					}
				} else if (fields[fieldIndex].getType()==int[].class) {
					int len = reader.getCInt();
					if (len==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						len--;
						int[] a = new int[len];
						for (int i=0; i<len; i++) a[i] = reader.getInt();
						unsafe.putObject(re, offset[fieldIndex],a);
					}
				} else if (fields[fieldIndex].getType()==long[].class) {
					int len = reader.getCInt();
					if (len==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						len--;
						long[] a = new long[len];
						for (int i=0; i<len; i++) a[i] = reader.getLong();
						unsafe.putObject(re, offset[fieldIndex],a);
					}
				} else if (fields[fieldIndex].getType()==char[].class) {
					int len = reader.getCInt();
					if (len==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						len--;
						char[] a = new char[len];
						for (int i=0; i<len; i++) a[i] = reader.getChar();
						unsafe.putObject(re, offset[fieldIndex],a);
					}
				} else if (fields[fieldIndex].getType()==float[].class) {
					int len = reader.getCInt();
					if (len==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						len--;
						float[] a = new float[len];
						for (int i=0; i<len; i++) a[i] = reader.getFloat();
						unsafe.putObject(re, offset[fieldIndex],a);
					}
				} else if (fields[fieldIndex].getType()==double[].class) {
					int len = reader.getCInt();
					if (len==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						len--;
						double[] a = new double[len];
						for (int i=0; i<len; i++) a[i] = reader.getDouble();
						unsafe.putObject(re, offset[fieldIndex],a);
					}
				} else if (fields[fieldIndex].getType().isEnum()) {
					int ordinal = reader.getCInt();
					if (ordinal==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else {
						ordinal--;
						Object v = fields[fieldIndex].getType().getEnumConstants()[ordinal];
						unsafe.putObject(re, offset[fieldIndex], v);
					}
				} else if (fields[fieldIndex].getType().isArray()) {
					int ty = reader.getCInt();
					if (ty==0) {
						unsafe.putObject(re, offset[fieldIndex],null);
					} else if (ty==1) {
						int oid = reader.getCInt();
						if (oid==0) throw new RuntimeException("Object cache inconsistent!");
						Object o = getObject(oid);
						unsafe.putObject(re, offset[fieldIndex],o);
								
					} else {
						int aoid = reader.getCInt();
						int len = reader.getCInt();
						Object a = Array.newInstance(fields[fieldIndex].getType().getComponentType(), len);
						if (cacheObjects && indexObject(a, true)!=aoid) throw new RuntimeException("Object cache inconsistent!");
						
						for (int i=0; i<len; i++) 
							Array.set(a, i, deserialize(reader));
						unsafe.putObject(re, offset[fieldIndex], a);
					}
				} else {
					Object a = deserialize(reader);
					unsafe.putObject(re, offset[fieldIndex], a);
				}
			}
		}
	}
	
	private int[] pack(boolean[] a) {
		int[] re = new int[a.length/8+(a.length%8!=0?1:0)];
		for (int i=0; i<a.length; i++) {
			int index = i/8;
			if (a[i])
				re[index] |= 1<<(i%8);
		}
		return re;
	}
	
	
	private boolean[] unpack(int length, int[] a) {
		boolean[] re = new boolean[length];
		int index = 0;
		for (int i=0; i<a.length; i++) {
			for (int b=0; index<re.length && b<8; b++)
				re[index++] = (a[i] & (1<<b)) !=0;
		}
		return re;
	}
	
}
