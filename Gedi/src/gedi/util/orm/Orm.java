package gedi.util.orm;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.value.Value;

import sun.misc.Unsafe;


/**
 * Either all (non static, non final, non transient) fields are taken (also from superclasses, those come first!) or only fields with the annotation
 *  {@link OrmField} (when there is at least one field with it)
 *  
 *  01/21/2016: Changed the behavior: final fields are not ignored anymore!!!!!!!! (Important, as the value of a final field may be set in the ctor, e.g. in Integer.)
 *  
 * @author erhard
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class Orm {


	private static final Logger log = Logger.getLogger( Orm.class.getName() );


	public static <T> T create(Class<T> cls) {
		try {
			return (T) getUnsafe().allocateInstance(cls);
		} catch (InstantiationException e) {
			log.log(Level.SEVERE, "Cannot create unsafe instance of "+cls,e);
			throw new RuntimeException("Cannot create unsafe instance of "+cls,e);
		}
	}
	
	/**
     * Get the name of the Java class for the given value type.
     *
     * @param type the value type
     * @return the class name
     */
    public static Class<?> getClassFromH2Type(int type) {
        switch(type) {
        case Value.BOOLEAN:
            // "java.lang.Boolean";
            return boolean.class;
        case Value.BYTE:
            // "java.lang.Byte";
            return byte.class;
        case Value.SHORT:
            // "java.lang.Short";
            return short.class;
        case Value.INT:
            // "java.lang.Integer";
            return int.class;
        case Value.LONG:
            // "java.lang.Long";
            return long.class;
        case Value.STRING:
        case Value.STRING_IGNORECASE:
        case Value.STRING_FIXED:
            return String.class;
        case Value.DOUBLE:
            // "java.lang.Double";
            return double.class;
        case Value.FLOAT:
            // "java.lang.Float";
            return float.class;
        default:
            return Object.class;
        }
    }
    
    
    public static void serialize(String path, Object o) throws IOException {
    	PageFileWriter out = new PageFileWriter(path);
    	new OrmSerializer(true,true).serializeAll(out, EI.wrap(o));
    	out.close();
    }
    
    public static <T> T deserialize(String path) throws IOException {
    	PageFile in = new PageFile(path);
    	ExtendedIterator<T> it = new OrmSerializer(true, true).deserializeAll(in);
    	T re = it.next();
    	if (it.hasNext()) throw new IOException("More data available!");
    	in.close();
    	return re;
    }
	
	public static <T> T fromArray(Object[] a, Class<T> cls) {
		OrmInfo info = getInfo(cls);
		
		Unsafe unsafe = getUnsafe();
		T re = create(cls);
		if (re instanceof OrmObject)
			((OrmObject)re).preOrmAction();
		
		Field[] fields = info.getFields();
		Class[] classes = info.getClasses();
		long[] offset = info.getPointerOffsets();
		for (int i=0; i<fields.length; i++) {
			if (classes[i]==boolean.class) 
				unsafe.putBoolean(re, offset[i], (boolean) a[i]);
			else if (classes[i]==byte.class) 
				unsafe.putByte(re, offset[i], (byte) a[i]);
			else if (classes[i]==short.class) 
				unsafe.putShort(re, offset[i], (short) a[i]);
			else if (classes[i]==int.class) 
				unsafe.putInt(re, offset[i], (int) a[i]);
			else if (classes[i]==long.class) 
				unsafe.putLong(re, offset[i], (long) a[i]);
			else if (classes[i]==float.class) 
				unsafe.putFloat(re, offset[i], (float) a[i]);
			else if (classes[i]==double.class) 
				unsafe.putDouble(re, offset[i], (double) a[i]);
			else 
				throw new RuntimeException("Only primitives allowed!");
		}
		if (re instanceof OrmObject)
			((OrmObject)re).postOrmAction();
		
		return re;
	}
	
	
	public static <T> T fromBinaryReader(BinaryReader in, T re) throws IOException {
		OrmInfo info = getInfo(re.getClass());
		
		Unsafe unsafe = getUnsafe();
		if (re instanceof OrmObject)
			((OrmObject)re).preOrmAction();
		
		Field[] fields = info.getFields();
		Class[] classes = info.getClasses();
		long[] offset = info.getPointerOffsets();
		for (int i=0; i<fields.length; i++) {
			if (classes[i]==boolean.class) 
				unsafe.putBoolean(re, offset[i], in.getCInt()==1);
			else if (classes[i]==byte.class) 
				unsafe.putByte(re, offset[i], (byte) in.getByte());
			else if (classes[i]==short.class) 
				unsafe.putShort(re, offset[i], in.getShort());
			else if (classes[i]==int.class) 
				unsafe.putInt(re, offset[i], in.getInt());
			else if (classes[i]==long.class) 
				unsafe.putLong(re, offset[i], in.getLong());
			else if (classes[i]==float.class) 
				unsafe.putFloat(re, offset[i], in.getFloat());
			else if (classes[i]==double.class) 
				unsafe.putDouble(re, offset[i], in.getDouble());
			else if (classes[i]==String.class) 
				unsafe.putObject(re, offset[i], in.getString());
			else 
				throw new RuntimeException("Only primitives allowed!");
		}
		if (re instanceof OrmObject)
			((OrmObject)re).postOrmAction();
		
		return re;
	}
	public static <T> T fromBinaryReader(BinaryReader in, Class<T> cls) throws IOException {
		return fromBinaryReader(in,create(cls));
	}
	
	public static <T> T fromFunctor(Class<T> cls, IntFunction<Object> fun) {
		OrmInfo info = getInfo(cls);
		
		Unsafe unsafe = getUnsafe();
		T re = create(cls);
		if (re instanceof OrmObject)
			((OrmObject)re).preOrmAction();
		
		Field[] fields = info.getFields();
		Class[] classes = info.getClasses();
		long[] offset = info.getPointerOffsets();
		for (int i=0; i<fields.length; i++) {
			if (classes[i]==boolean.class) 
				unsafe.putBoolean(re, offset[i], (boolean) fun.apply(i));
			else if (classes[i]==byte.class) 
				unsafe.putByte(re, offset[i], (byte) fun.apply(i));
			else if (classes[i]==short.class) 
				unsafe.putShort(re, offset[i], (short) fun.apply(i));
			else if (classes[i]==int.class) 
				unsafe.putInt(re, offset[i], (int) fun.apply(i));
			else if (classes[i]==long.class) 
				unsafe.putLong(re, offset[i], (long) fun.apply(i));
			else if (classes[i]==float.class) 
				unsafe.putFloat(re, offset[i], (float) fun.apply(i));
			else if (classes[i]==double.class) 
				unsafe.putDouble(re, offset[i], (double) fun.apply(i));
			else 
				unsafe.putObject(re, offset[i], fun.apply(i));
		}
		if (re instanceof OrmObject)
			((OrmObject)re).postOrmAction();
		
		return re;
	}
	
	public static <C,T> Function<C,T> getFieldGetter(Class<C> cls, String fieldname) {
		OrmInfo info = getInfo(cls);
		
		Unsafe unsafe = getUnsafe();
		
		Field[] fields = info.getFields();
		int fieldIndex = info.getIndex(fieldname);
		long[] offset = info.getPointerOffsets();
		
		if (fieldIndex==-1) throw new RuntimeException("Class "+cls+" does not have a field "+fieldname);
		
		if (fields[fieldIndex].getType()==boolean.class) 
			return o->(T) new Boolean(unsafe.getBoolean(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==byte.class) 
			return o->(T) new Byte(unsafe.getByte(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==short.class) 
			return o->(T) new Short(unsafe.getShort(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==int.class) 
			return o->(T) new Integer(unsafe.getInt(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==long.class) 
			return o->(T) new Long(unsafe.getLong(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==float.class) 
			return o->(T) new Float(unsafe.getFloat(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==double.class) 
			return o->(T) new Double(unsafe.getDouble(o, offset[fieldIndex]));
		else 
			return o->(T) unsafe.getObject(o, offset[fieldIndex]);
	}
	
	public static <C,T> BiConsumer<C,T> getFieldSetter(Class<C> cls, String fieldname) {
		OrmInfo info = getInfo(cls);
		
		Unsafe unsafe = getUnsafe();
		
		Field[] fields = info.getFields();
		int fieldIndex = info.getIndex(fieldname);
		long[] offset = info.getPointerOffsets();
		
		if (fields[fieldIndex].getType()==boolean.class) 
			return (o,v)->unsafe.putBoolean(o, offset[fieldIndex], (Boolean)v);
		else if (fields[fieldIndex].getType()==byte.class) 
			return (o,v)->unsafe.putByte(o, offset[fieldIndex], (Byte)v);
		else if (fields[fieldIndex].getType()==short.class) 
			return (o,v)->unsafe.putShort(o, offset[fieldIndex], (Short)v);
		else if (fields[fieldIndex].getType()==int.class) 
			return (o,v)->unsafe.putInt(o, offset[fieldIndex], (Integer)v);
		else if (fields[fieldIndex].getType()==long.class) 
			return (o,v)->unsafe.putLong(o, offset[fieldIndex], (Long)v);
		else if (fields[fieldIndex].getType()==float.class) 
			return (o,v)->unsafe.putFloat(o, offset[fieldIndex], (Float)v);
		else if (fields[fieldIndex].getType()==double.class) 
			return (o,v)->unsafe.putDouble(o, offset[fieldIndex], (Double)v);
		else 
			return (o,v)->unsafe.putObject(o, offset[fieldIndex], v);
	}
	
	public static <C> ToIntFunction<C> getIntFieldGetter(Class<C> cls, String fieldname) {
		OrmInfo info = getInfo(cls);
		
		Unsafe unsafe = getUnsafe();
		
		Field[] fields = info.getFields();
		int fieldIndex = info.getIndex(fieldname);
		long[] offset = info.getPointerOffsets();
		
		if (fields[fieldIndex].getType()!=boolean.class) 
			throw new RuntimeException("Not an int field!");
		return o->unsafe.getInt(o, offset[fieldIndex]);
	}
	
	public static <C> ToLongFunction<C> getLongFieldGetter(Class<C> cls, String fieldname) {
		OrmInfo info = getInfo(cls);
		
		Unsafe unsafe = getUnsafe();
		
		Field[] fields = info.getFields();
		int fieldIndex = info.getIndex(fieldname);
		long[] offset = info.getPointerOffsets();
		
		if (fields[fieldIndex].getType()!=long.class) 
			throw new RuntimeException("Not an long field!");
		return o->unsafe.getLong(o, offset[fieldIndex]);
	}	
	public static <C> ToDoubleFunction<C> getDoubleFieldGetter(Class<C> cls, String fieldname) {
		OrmInfo info = getInfo(cls);
		
		Unsafe unsafe = getUnsafe();
		
		Field[] fields = info.getFields();
		int fieldIndex = info.getIndex(fieldname);
		long[] offset = info.getPointerOffsets();
		
		if (fields[fieldIndex].getType()!=double.class) 
			throw new RuntimeException("Not an long field!");
		return o->unsafe.getDouble(o, offset[fieldIndex]);
	}
	
	
	public static <T> T getField(Object o, int fieldIndex) {
		OrmInfo info = getInfo(o.getClass());
		
		Unsafe unsafe = getUnsafe();
		
		Field[] fields = info.getFields();
		long[] offset = info.getPointerOffsets();
		
		if (fields[fieldIndex].getType()==boolean.class) 
			return (T) new Boolean(unsafe.getBoolean(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==byte.class) 
			return (T) new Byte(unsafe.getByte(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==short.class) 
			return (T) new Short(unsafe.getShort(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==int.class) 
			return (T) new Integer(unsafe.getInt(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==long.class) 
			return (T) new Long(unsafe.getLong(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==float.class) 
			return (T) new Float(unsafe.getFloat(o, offset[fieldIndex]));
		else if (fields[fieldIndex].getType()==double.class) 
			return (T) new Double(unsafe.getDouble(o, offset[fieldIndex]));
		else 
			return (T) unsafe.getObject(o, offset[fieldIndex]);
	}
	
	public static Object[] toArray(Object o, Object[] a) {
		OrmInfo info = getInfo(o.getClass());
		Field[] fields = info.getFields();
		if (a==null || a.length<fields.length) a = new Object[fields.length];

		Unsafe unsafe = getUnsafe();
		
		Class[] classes = info.getClasses();
		long[] offset = info.getPointerOffsets();
		for (int i=0; i<fields.length; i++) {
			if (classes[i]==boolean.class) 
				a[i] = unsafe.getBoolean(o, offset[i]);
			else if (classes[i]==byte.class) 
				a[i] = unsafe.getByte(o, offset[i]);
			else if (classes[i]==short.class) 
				a[i] = unsafe.getShort(o, offset[i]);
			else if (classes[i]==int.class) 
				a[i] = unsafe.getInt(o, offset[i]);
			else if (classes[i]==long.class) 
				a[i] = unsafe.getLong(o, offset[i]);
			else if (classes[i]==float.class) 
				a[i] = unsafe.getFloat(o, offset[i]);
			else if (classes[i]==double.class) 
				a[i] = unsafe.getDouble(o, offset[i]);
			else 
				a[i] = unsafe.getObject(o, offset[i]);
		}
		
		return a;
	}
	
	public static <T> T toBinaryWriter(T o, BinaryWriter out) throws IOException {
		OrmInfo info = getInfo(o.getClass());
		Field[] fields = info.getFields();

		Unsafe unsafe = getUnsafe();
		
		Class[] classes = info.getClasses();
		long[] offset = info.getPointerOffsets();
		for (int i=0; i<fields.length; i++) {
			if (classes[i]==boolean.class) 
				out.putCInt(unsafe.getBoolean(o, offset[i])?1:0);
			else if (classes[i]==byte.class) 
				out.putShort(unsafe.getShort(o,offset[i]));
			else if (classes[i]==short.class) 
				out.putShort(unsafe.getShort(o, offset[i]));
			else if (classes[i]==int.class) 
				out.putInt(unsafe.getInt(o, offset[i]));
			else if (classes[i]==long.class) 
				out.putLong(unsafe.getLong(o, offset[i]));
			else if (classes[i]==float.class) 
				out.putFloat(unsafe.getFloat(o, offset[i]));
			else if (classes[i]==double.class) 
				out.putDouble(unsafe.getDouble(o, offset[i]));
			else if (classes[i]==String.class) 
				out.putString((String)unsafe.getObject(o, offset[i]));
			else 
				throw new RuntimeException("Only primitives allowed!");
		}
		return o;
	}
	
	
	public static <T> T fromResultSet(ResultSet rs, Class<T> cls) throws SQLException {
		OrmInfo info = getInfo(cls);
		
		T re = create(cls);
		if (re instanceof OrmObject)
			((OrmObject)re).preOrmAction();
		
		Field[] fields = info.getFields();
		Class[] classes = info.getClasses();
		long[] offset = info.getPointerOffsets();
		for (int i=0; i<fields.length; i++) {
			if (classes[i]==boolean.class) 
				getUnsafe().putBoolean(re, offset[i], rs.getBoolean(fields[i].getName()));
			else if (classes[i]==byte.class) 
				getUnsafe().putByte(re, offset[i], rs.getByte(fields[i].getName()));
			else if (classes[i]==short.class) 
				getUnsafe().putShort(re, offset[i], rs.getShort(fields[i].getName()));
			else if (classes[i]==int.class) 
				getUnsafe().putInt(re, offset[i], rs.getInt(fields[i].getName()));
			else if (classes[i]==long.class) 
				getUnsafe().putLong(re, offset[i], rs.getLong(fields[i].getName()));
			else if (classes[i]==float.class) 
				getUnsafe().putFloat(re, offset[i], rs.getFloat(fields[i].getName()));
			else if (classes[i]==double.class) 
				getUnsafe().putDouble(re, offset[i], rs.getDouble(fields[i].getName()));
			else if (classes[i]==String.class) 
				getUnsafe().putObject(re, offset[i], rs.getString(fields[i].getName()));
			else if (BinarySerializable.class.isAssignableFrom(classes[i])){
				BinarySerializable o = (BinarySerializable) create(classes[i]);
				byte[] arr = rs.getBytes(fields[i].getName());
				BinaryBlob blob = new BinaryBlob(arr.length);
				blob.setBytes(0, arr);
				blob.finish(false);
				try {
					o.deserialize(blob);
				} catch (IOException e) {
				}
				getUnsafe().putObject(re, offset[i], o);
			}
			else if (classes[i]==Boolean.class) 
				getUnsafe().putObject(re, offset[i], rs.getBoolean(fields[i].getName()));
			else if (classes[i]==Byte.class) 
				getUnsafe().putObject(re, offset[i], rs.getByte(fields[i].getName()));
			else if (classes[i]==Short.class) 
				getUnsafe().putObject(re, offset[i], rs.getShort(fields[i].getName()));
			else if (classes[i]==Integer.class) 
				getUnsafe().putObject(re, offset[i], rs.getInt(fields[i].getName()));
			else if (classes[i]==Long.class) 
				getUnsafe().putObject(re, offset[i], rs.getLong(fields[i].getName()));
			else if (classes[i]==Float.class) 
				getUnsafe().putObject(re, offset[i], rs.getFloat(fields[i].getName()));
			else if (classes[i]==Double.class) 
				getUnsafe().putObject(re, offset[i], rs.getDouble(fields[i].getName()));
			else  
				throw new RuntimeException("Cannot set field "+fields[i]);
			
		}
		if (re instanceof OrmObject)
			((OrmObject)re).postOrmAction();
		
		return re;
	}
	
	
	/**
	 * Sets from 1 to n the parameters
	 * Returns the number of set parameters
	 * @param o
	 * @param pre
	 * @return
	 * @throws SQLException
	 */
	public static int toPreparedStatement(Object o, PreparedStatement pre) throws SQLException {
		OrmInfo info = getInfo(o.getClass());
		
		
		Field[] fields = info.getFields();
		Class[] classes = info.getClasses();
		long[] offset = info.getPointerOffsets();
		for (int i=0; i<fields.length; i++) {
			if (classes[i]==boolean.class) 
				pre.setBoolean(i+1, getUnsafe().getBoolean(o, offset[i]));
			else if (classes[i]==byte.class) 
				pre.setByte(i+1, getUnsafe().getByte(o, offset[i]));
			else if (classes[i]==short.class) 
				pre.setShort(i+1, getUnsafe().getShort(o, offset[i]));
			else if (classes[i]==int.class) 
				pre.setInt(i+1, getUnsafe().getInt(o, offset[i]));
			else if (classes[i]==long.class) 
				pre.setLong(i+1, getUnsafe().getLong(o, offset[i]));
			else if (classes[i]==float.class) 
				pre.setFloat(i+1, getUnsafe().getFloat(o, offset[i]));
			else if (classes[i]==double.class) 
				pre.setDouble(i+1, getUnsafe().getDouble(o, offset[i]));
			else if (classes[i]==String.class) 
				pre.setString(i+1, (String)getUnsafe().getObject(o, offset[i]));
			else if (BinarySerializable.class.isAssignableFrom(classes[i])){
				BinarySerializable f = (BinarySerializable)getUnsafe().getObject(o, offset[i]);
				BinaryBlob blob = new BinaryBlob();
				try {
					f.serialize(blob);
				} catch (IOException e) {
				}
				blob.finish(false);
				pre.setBytes(i+1, blob.toArray());
			} else {
				throw new IllegalArgumentException("Cannot set "+classes[i]);
			}
		}
		return fields.length;
	}

	private static HashMap<Class<?>,OrmInfo> cache = new HashMap<Class<?>, OrmInfo>(); 
	
	public static Field[] getOrmFields(Class<?> cls) {
		return getInfo(cls).getFields();
	}


	public static OrmInfo getInfo(Class<?> cls) {
		return cache.computeIfAbsent(cls, c->new OrmInfo(c));
	}


	public static String[] getOrmFieldNames(Class<?> cls) {
		return ArrayUtils.map(getOrmFields(cls),new String[0],m->m.getName());
	}

	public static Class<?>[] getOrmFieldClasses(Class<?> cls) {
		return ArrayUtils.map(getOrmFields(cls),new Class[0],m->m.getType());
	}

	public static boolean hasToStringDeclared(Class<?> cls) {
		try {
			cls.getDeclaredMethod("toString");
			return true;
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
		
	}
	
	
	public static String toString(Object o, String sep, boolean writeFieldNames, boolean writeClassName) {
		OrmInfo info = getInfo(o.getClass());
		Field[] fields = info.getFields();

		Unsafe unsafe = getUnsafe();
		
		StringBuilder sb = new StringBuilder();
		if (writeClassName)
			sb.append(o.getClass().getSimpleName()+" [");
		
		Class[] classes = info.getClasses();
		long[] offset = info.getPointerOffsets();
		for (int i=0; i<fields.length; i++) {
			if (i>0) sb.append(sep);
			if (writeFieldNames)
				sb.append(fields[i].getName()+"=");
			
			if (classes[i]==boolean.class) 
				sb.append(unsafe.getBoolean(o, offset[i]));
			else if (classes[i]==byte.class) 
				sb.append(unsafe.getByte(o, offset[i]));
			else if (classes[i]==short.class) 
				sb.append(unsafe.getShort(o, offset[i]));
			else if (classes[i]==int.class) 
				sb.append(unsafe.getInt(o, offset[i]));
			else if (classes[i]==long.class) 
				sb.append(unsafe.getLong(o, offset[i]));
			else if (classes[i]==float.class) 
				sb.append(unsafe.getFloat(o, offset[i]));
			else if (classes[i]==double.class) 
				sb.append(unsafe.getDouble(o, offset[i]));
			else 
				sb.append(unsafe.getObject(o, offset[i]));
		}
		
		if (writeClassName)
			sb.append("]");
		return sb.toString();
	}

	public static long sizeOf(Object object) {
		Unsafe unsafe = getUnsafe();
		
		return unsafe.getAddress( normalize( unsafe.getInt(object, 4L) ) + 12L );
	}

	private static long normalize(int value) {
		if(value >= 0) return value;
		return (~0L >>> 32) & value;
	}

	private static Unsafe unsafe;
	public static Unsafe getUnsafe() {
		if (unsafe==null) {
			try {
				Field f = Unsafe.class.getDeclaredField("theUnsafe");
				f.setAccessible(true);
				unsafe = (Unsafe)f.get(null);
			} catch (Exception e) { 
				throw new RuntimeException("Cannot get access to sun.misc.Unsafe!",e);
			}
		}
		return unsafe;
	}

	
	public static class OrmInfo {
		Class<?> cls;
		Field[] fields;
		Type[] genericDeclaringClasses;
		Class[] classes;
		String[] names;
		HashMap<String,Integer> nameToIndex;
		
		long[] pointerOffset;
		
		public OrmInfo(Class<?> cls) {
			this.cls = cls;
		}
		
		public synchronized int getIndex(String fieldName) {
			if (nameToIndex==null) {
				nameToIndex = new HashMap<String, Integer>();
				Field[] fields = getFields();
				for (int i=0; i<fields.length; i++)
					nameToIndex.put(fields[i].getName(), i);
			}
			Integer i =  nameToIndex.get(fieldName);
			if (i==null) return -1;
			return i;
		}
		
		public synchronized Field[] getFields() {
			if (fields==null) {
				if (cls == null) 
					fields =  new Field[0];
				else {
					LinkedList<Field> re = new LinkedList<Field>();
					LinkedList<Type> re2 = new LinkedList<Type>();
					Type gcls = null;
					for (Class<?> cls = this.cls ;cls!=null; gcls = cls.getGenericSuperclass(), cls = cls.getSuperclass()) {
						Field[] fields = cls.getDeclaredFields();
						ArrayUtils.reverse(fields);
						for (Field f : fields)
							if (f.getAnnotation(OrmField.class)!=null){
								re.addFirst(f);
								re2.addFirst(gcls);
							}
					}
					if (re.size()==0) {
						gcls = null;
						for (Class<?> cls = this.cls;cls!=null; gcls = cls.getGenericSuperclass(), cls = cls.getSuperclass()) {
							Field[] fields = cls.getDeclaredFields();
							ArrayUtils.reverse(fields);
							for (Field f : fields) {
								int m = f.getModifiers();
								if (!Modifier.isStatic(m) 
//										&& !Modifier.isFinal(m)
										&& !Modifier.isTransient(m)
										) {
									re.addFirst(f);
									re2.addFirst(gcls);
								}
							}
						}
					}
					fields = re.toArray(new Field[0]);
					genericDeclaringClasses = re2.toArray(new Type[0]);
				}
			}
			return fields;
		}
		
		public synchronized Class[] getClasses() {
			if (classes==null) {
				Field[] fields = getFields();
				classes = new Class[fields.length];
				
				for (int i=0; i<fields.length; i++) {
					classes[i] = fields[i].getType();
					if (classes[i]==Object.class) { // maybe a generic field, so try using superclass reflection!
						TypeVariable<?>[] genClasses = fields[i].getDeclaringClass().getTypeParameters();
						Type sup = genericDeclaringClasses[i];
						Type typ = fields[i].getGenericType();
						if (genClasses.length>0 && sup instanceof ParameterizedType && typ instanceof TypeVariable) {
							ParameterizedType gsup = (ParameterizedType)sup;
							TypeVariable tv = (TypeVariable) typ;
							for (int g=0; g<genClasses.length; g++) {
								if (genClasses[g].getName().equals(tv.getName())) {
									classes[i] = (Class) gsup.getActualTypeArguments()[g];
									break;
								}
									
							}
						}
					}
				}
				
				
				
			}
			return classes;
		}
		
		public synchronized long[] getPointerOffsets() {
			if (pointerOffset==null) {
				Field[] fields = getFields();
				pointerOffset = new long[fields.length];
				for (int i=0; i<fields.length; i++)
					pointerOffset[i] = getUnsafe().objectFieldOffset(fields[i]);
			}
			return pointerOffset;
		}

		public synchronized String[] getNames() {
			if (names==null) {
				names = new String[getFields().length];
				for (int i=0; i<names.length; i++) {
					names[i] = StringUtils.getUnCamelCase(getFields()[i].getName(), " ", true);
				}
			}
			return names;
		}
	}
		
}
