package gedi.cascadingProperty;

import java.util.HashSet;

public class CpsKey {

	private String id;
	private HashSet<String> classes;
	private Class<?> cls;
	
	public CpsKey(String id, HashSet<String> classes, Class<?> cls) {
		this.id = id;
		this.classes = classes;
		this.cls = cls;
	}

	/**
	 * Returns if the given key fulfills all restrictions induced by this (i.e. if this has id, key must have this id,
	 * if this has classes, key must have all those classes (possibly more); if this has cls, key must have a class that is a subclass of it (or it))
	 * @param key
	 * @return
	 */
	public boolean matches(CpsKey key) {
		if (id!=null && !id.equals(key.id)) return false;
		if (classes!=null && key.classes!=null && !key.classes.containsAll(classes)) return false;
		if (cls!=null && key.cls!=null && !cls.isAssignableFrom(key.cls)) return false;
		return true;
	}
	
	

	@Override
	public String toString() {
		return "CpsKey [id=" + id + ", classes=" + classes + ", cls=" + cls
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classes == null) ? 0 : classes.hashCode());
		result = prime * result + ((cls == null) ? 0 : cls.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CpsKey other = (CpsKey) obj;
		if (classes == null) {
			if (other.classes != null)
				return false;
		} else if (!classes.equals(other.classes))
			return false;
		if (cls == null) {
			if (other.cls != null)
				return false;
		} else if (!cls.equals(other.cls))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	
	
	
}
