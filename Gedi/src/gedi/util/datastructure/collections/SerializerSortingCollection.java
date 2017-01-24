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

package gedi.util.datastructure.collections;

import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Not thread-safe!
 * @author erhard
 *
 * @param <T>
 */
public class SerializerSortingCollection<T> implements Collection<T>, Closeable {

	private int memoryCapacity;
	private ArrayList<T> mem;
	private PageFileWriter buf = null;
	private ArrayList<Long> offsets = new ArrayList<Long>(); 
	private Comparator<? super T> comp;
	private int size;
	
	private BinarySerializer<T> serializer;
	
	public SerializerSortingCollection(BinarySerializer<T> serializer, Comparator<? super T> comp, int memoryCapacity) {
		this.serializer = serializer;
		this.comp = comp;
		this.memoryCapacity = memoryCapacity;
		mem = new ArrayList<T>(memoryCapacity);
	}
	
	
	public boolean add(T e) {
		size++;
		if (mem.size()>=memoryCapacity) 
			spillToDisk();
		mem.add(e);
		return true;
	}

	
	public ExtendedIterator<T> iterator() {
		if (buf==null) {
			Collections.sort(mem, comp);
			return EI.wrap(mem);
		}
		
		spillToDisk();
		
		try {
			serializer.endSerialize(buf);
			
			PageFile f = buf.read(false);
			f.setUnmap(false);
			ExtendedIterator<T>[] iter = new ExtendedIterator[offsets.size()];
			serializer.beginDeserialize(f);
			for (int i=0; i<iter.length; i++) {
				iter[i] = (ExtendedIterator<T>) f.view(i==0?0:offsets.get(i-1), offsets.get(i)).iterator(p->{
					try {
						T re = serializer.deserialize(p);
						return re;
					} catch (Exception e) {
						throw new RuntimeException("Could not deserialize object!",e);
					}
				}
				);
			}
			serializer.endDeserialize(f);
			
			return EI.merge(comp, iter);
		
		} catch (IOException e) {
			throw new RuntimeException("Cannot iterate temp file!",e);
		}
	}
	
	
	@Override
	public void close() throws IOException {
		if (buf!=null) {
			buf.close();
			new File(buf.getPath()).delete();
			buf = null;
		}
		mem.clear();
	}

	private void spillToDisk() {
		Collections.sort(mem, comp);
		if (buf==null)
			try {
				buf = new PageFileWriter(Files.createTempFile("serializercollection", ".tmp").toString());
				new File(buf.getPath()).deleteOnExit();
				serializer.beginSerialize(buf);
			} catch (IOException e) {
				throw new RuntimeException("Cannot create temp file!",e);
			}
		
				
		for (T e : mem)
			try {
				serializer.serialize(buf, e);
			} catch (IOException e1) {
				throw new RuntimeException("Cannot write entry !",e1);
			}
		
		offsets.add(buf.position());
		
		mem.clear();
	}


	@Override
	public int size() {
		return size;
	}


	@Override
	public boolean isEmpty() {
		return size==0;
	}


	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}


	@Override
	public Object[] toArray() {
		return iterator().toArray(serializer.getType());
	}


	@Override
	public <E> E[] toArray(E[] a) {
		return (E[]) iterator().toArray((T[]) a);
	}


	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}


	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}


	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T t : c) add(t);
		return true;
	}


	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}


	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}


	@Override
	public void clear() {
		try {
			close();
		} catch (IOException e) {
			throw new RuntimeException("Could not clear!",e);
		}
	}
	
	
}
