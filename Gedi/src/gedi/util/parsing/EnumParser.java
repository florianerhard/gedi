package gedi.util.parsing;

import gedi.core.reference.Strand;

import java.util.HashMap;

public class EnumParser<E> implements Parser<E> {
	
	private Class<E> cls;
	private HashMap<String,E> enumConst = new HashMap<String,E>();
	private HashMap<String,E> enumConstNoCase = new HashMap<String,E>();
	
	public EnumParser(Class<E> cls) {
		if (!cls.isEnum()) throw new RuntimeException("Not possible!");
		
		this.cls = cls;
		for (E e : cls.getEnumConstants()) {
			enumConst.put(e.toString(), e);
			if (!enumConstNoCase.containsKey(e.toString().toLowerCase()))
				enumConstNoCase.put(e.toString().toLowerCase(), e);
			else
				enumConstNoCase.put(e.toString().toLowerCase(), null);
		}
	}

	@Override
	public E apply(String s) {
		E re = enumConst.get(s);
		if (re!=null) return re;
		re = enumConstNoCase.get(s.toLowerCase());
		if (re!=null) return re;
		throw new RuntimeException("Enum constant "+s+" unknown: "+enumConst.keySet());
	}

	@Override
	public Class<E> getParsedType() {
		return cls;
	}
	
}