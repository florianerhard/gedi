package gedi.util.job;


import java.util.HashMap;
import java.util.function.Function;

public class AnnotationMap<F,T> extends HashMap<F,T> { 

	private String name;
	private Class<T> cls;
	
	public AnnotationMap(String name, Class<T> cls) {
		this.name = name;
		this.cls = cls;
	}

	
	public String getName() {
		return name;
	}
	
	public Class<T> getAnnotationClass() {
		return cls;
	}
	
	public AnnotationMap<F,T> clone(Function<F,F> keyMap,Function<T,T> valueMap) {
		AnnotationMap<F, T> re = new AnnotationMap<F, T>(name, cls);
		for (F k : keySet()) {
			F nk = keyMap.apply(k);
			if (nk!=null)
				re.put(nk,valueMap.apply(get(k)));
		}
		return re;
	}
	
}
