package gedi.util.datastructure.collections;

import gedi.util.ArrayUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.mutable.MutableInteger;
import gedi.util.orm.ClassTree;

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
public class FastSortingCollection<T> implements Collection<T>, Closeable {

	private int memoryCapacity;
	private ArrayList<T> mem;
	private PageFileWriter buf = null;
	private ArrayList<Long> offsets = new ArrayList<Long>(); 
	private Comparator<? super T> comp;
	private int size;
	private ClassTree<T> tree;
	
	private boolean compress = true;
	
	public FastSortingCollection(T proto, Comparator<? super T> comp, int memoryCapacity) {
		tree = new ClassTree<T>(proto);
		this.comp = comp;
		this.memoryCapacity = 128;//memoryCapacity;
		mem = new ArrayList<T>(memoryCapacity);
	}
	
	public void setCompress(boolean compress) {
		this.compress = compress;
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
			PageFile f = buf.read(false);
			f.setUnmap(false);
			ExtendedIterator<T>[] iter = new ExtendedIterator[offsets.size()];
			for (int i=0; i<iter.length; i++) {
				iter[i] = (ExtendedIterator<T>) f.view(i==0?0:offsets.get(i-1), offsets.get(i)).iterator(p->{
					try {
						int size = p.getCInt();
						int rsize = size;
						if (compress) rsize = p.getCInt();
						
						p.get(buffer,0,rsize); // must always be large enough (has been used to write)
						byte[] in = buffer;
						if (compress) {
							ArrayUtils.decompress(buffer, 0, cbuffer, 0, size);
							in = cbuffer;
						}
						T re = (T)tree.fromBuffer(in);
						return re;
					} catch (Exception e) {
						throw new RuntimeException("Could not deserialize object!",e);
					}
				}
				);
			}
			return EI.merge(comp, iter).endAction(()->{
				try {
					f.close();
				} catch (IOException e){} 
			});
		
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

	private MutableInteger mi = new MutableInteger();
	private byte[] buffer = new byte[16*1024];
	private byte[] cbuffer = new byte[16*1024];
	private void spillToDisk() {
		if (mem.size()==0) return;
		
		Collections.sort(mem, comp);
		if (buf==null)
			try {
				buf = new PageFileWriter(Files.createTempFile("fastsortingcollection", ".tmp").toString());
				new File(buf.getPath()).deleteOnExit();
			} catch (IOException e) {
				throw new RuntimeException("Cannot create temp file!",e);
			}
		
		for (T e : mem)
			try {
				
				buffer = tree.toBuffer(e, buffer, mi);
				byte[] out = buffer;
				buf.putCInt(mi.N);
				if (compress) {
					while (cbuffer.length<ArrayUtils.getSaveCompressedSize(mi.N)) cbuffer = new byte[cbuffer.length*2];
					mi.N = ArrayUtils.compress(buffer.clone(), 0, mi.N, cbuffer, 0);
					buf.putCInt(mi.N);
					out=  cbuffer;
				}

				buf.put(out, 0, mi.N);
				
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
		return iterator().toArray(tree.getType());
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
