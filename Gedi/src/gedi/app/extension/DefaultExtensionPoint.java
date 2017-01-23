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
