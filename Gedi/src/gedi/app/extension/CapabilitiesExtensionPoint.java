package gedi.app.extension;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;



public class CapabilitiesExtensionPoint<C extends Enum<C>,T> implements ExtensionPoint<C[],T> {

	
	private Class<T> cls;
	
	private LinkedList<HashSet<C>> caps = new LinkedList<HashSet<C>>(); 
	private LinkedList<Class<? extends T>> classes = new LinkedList<Class<? extends T>>();
	
	protected CapabilitiesExtensionPoint(Class<T> cls) {
		this.cls = cls;
	}
	
	@Override
	public Class<T> getExtensionPointClass() {
		return cls;
	}

	@Override
	public void addExtension(Class<? extends T> extension, C... capabilities) {
		caps.addFirst(new HashSet<C>(Arrays.asList(capabilities)));
		classes.addFirst(extension);
	}

	
	public T get(ExtensionContext context, C... capabilities) {
		HashSet<C> nec = new HashSet<C>(Arrays.asList(capabilities));
		
		Iterator<HashSet<C>> cit = caps.iterator();
		Iterator<Class<? extends T>> rit = classes.iterator();
		while (cit.hasNext()) {
			HashSet<C> set = cit.next();
			if (set.containsAll(nec))
				try {
					return context.newInstance(rit.next());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get extension for "+Arrays.toString(capabilities),e);
					throw new RuntimeException("Could not get extension for "+Arrays.toString(capabilities),e);
				}
			rit.next();
		}
		return null;
	}
	
}
