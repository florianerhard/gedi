package gedi.util.io.text.jph;

import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.nashorn.JS;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javax.script.ScriptException;


/**
 * The given string must include <? ?> or <?JS ?> tags, which are executed in the given javascript environment.
 * Stdout of these are inserted into the string instead of the tags. Also possible: <? variable ?>, which is equivalent to
 * <? print(variable) ?> (instead of variable, use any valid identifier).  
 * 
 * It is also possible to include a ?> ... <? inside a 
 * JS statement ( <? for (var i=0; i<n; i++) { ?> Hello World <? i ?> <? } ?> )
 * 
 * 
 * @author erhard
 *
 */
public class Jhp implements UnaryOperator<String> {

	
	private JS js;
	private JhpParameter param;
	
	public Jhp() {
		this(new JS());
	}

	public Jhp(JS js) {
		this.js = js==null?new JS():js;
		param = new JhpParameter();
		try {
			this.js.injectObject(param);
		} catch (ScriptException e) {
			throw new RuntimeException("Could not set up jhp parameters!",e);
		}
	}

	
	public JS getJs() {
		return js;
	}
	

	private String position(String code, int pos) {
		return "Line "+StringUtils.countChar(code.substring(0, pos), '\n')+" Column "+(pos-code.substring(0, pos).lastIndexOf('\n')-1);
	}
	
	
	private static Pattern levelPat = Pattern.compile("<\\?JS(\\d+)");
	@Override
	public String apply(String t) {
		TreeSet<Integer> levels = new TreeSet<>();
		EI.wrap(levelPat.matcher(t),1).map(Jhp.this::parseInt).toCollection(levels);
		
		if (levels.isEmpty()) {
			return apply(t,"");
		} else {
			for (Integer l : EI.wrap(levels.descendingIterator()).loop()) {
				t = apply(t,l+"");
			}
			t=apply(t,"");
			return t;
		}
	
	}
	
	private Integer parseInt(String s) {
		if (!StringUtils.isInt(s))
			throw new RuntimeException(s+"  is not an int! <?JS"+s+" is not allowed!");
		return Integer.parseInt(s);
	}

	public String apply(String t, String level) {
		// first replace simple expressions
		StringBuilder re = new StringBuilder();
		while (t.length()>0) {
			int open = t.indexOf(level==null?"<?":("<?JS"+level));
			int openlength = open>=0 && t.substring(open).startsWith("<?JS"+level)?4+level.length():2;
			int close = t.indexOf("?>");
			
			if(open==-1) 
				break;
			
			if (close==-1) throw new RuntimeException("No Jhp end tag at "+position(t, open));
			if (close<open) throw new RuntimeException("Jhp end tag before start tag at "+position(t, close)+" and "+position(t,open));
			
			String before = t.substring(0, open);
			String in = t.substring(open+openlength, close);
			t = t.substring(close+2);
		
			re.append(before);
			if (StringUtils.isJavaIdentifier(in.trim())) {
				re.append("<?JS"+level+" print(").append(in).append(") ?>");
			} else
				re.append("<?JS"+level+"").append(in).append("?>");
			
		}
		re.append(t);
		t = re.toString();
		
		// now transform to js program end execute
		re = new StringBuilder();
		while (t.length()>0) {
			int open = t.indexOf(level==null?"<?":("<?JS"+level));
			int openlength = open>=0 && t.substring(open).startsWith("<?JS"+level)?4+level.length():2;
			int close = t.indexOf("?>");
			
			if(open==-1) 
				break;
			
			if (close<open) throw new RuntimeException("Jhp end tag before start tag!");
			
			String before = t.substring(0, open);
			String in = t.substring(open+openlength, close);
			t = t.substring(close+2);
		
			re.append(print(before));
			re.append(exec(in));
		}
		
		re.append(print(t));
		
		StringWriter sw=new StringWriter();
		Writer bak = js.getStdout();
		js.setStdout(sw);
		try {
			js.execSource(re.toString());
		} catch (ScriptException e) {
			if (e.getCause()!=null && e.getCause().getMessage().contains("is not defined")) {
				throw new JhpParameterException(StringUtils.splitField(e.getCause().getMessage(), '"', 1));
			}
			throw new RuntimeException("Could not execute Jhp!",e);
		}
		js.setStdout(bak);
		return sw.toString();
	}

	private String print(String before) {
		before = before.replace("\\", "\\\\");
		before = before.replace("\"", "\\\"");
		before = before.replace("\n", "\\n\"+\n\t\"");
		
		return "print(\""+before+"\");\n";
	}

	private String exec(String in) {
		return in+"\n";
	}

	public JhpParameter getParameters() {
		return param;
	}


}
