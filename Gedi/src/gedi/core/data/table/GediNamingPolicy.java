package gedi.core.data.table;

import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;

public class GediNamingPolicy implements NamingPolicy {

	private String name;
	
	public GediNamingPolicy(String name) {
		this.name = name;
	}

	@Override
	public String getClassName(String prefix, String source, Object key,
			Predicate names) {
		if (prefix == null) {
            prefix = "gedi.generated";
        } else if (prefix.startsWith("java")) {
            prefix = "$" + prefix;
        }
        String base =
            prefix + "."+name+"$$" + 
            Integer.toHexString(key.hashCode());
        String attempt = base;
        int index = 2;
        while (names.evaluate(attempt))
            attempt = base + "_" + index++;
        return attempt;
	}

}
