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

package gedi.util.io.text.jhp;

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
		setJS(js);
	}

	public void resetJS() {
		setJS(null);
	}
	public void setJS(JS js) {
		this.js = js==null?new JS():js;
		param = new JhpParameter(this);
		try {
			this.js.injectObject(new TemplateExtensions(this));
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
			for (Integer l : EI.wrap(levels).loop()) {
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
		if (level==null)
			level = "";
		// first replace simple expressions
		t = clean(t);
		
		// now transform to js program end execute
		StringBuilder re = new StringBuilder();
		while (t.length()>0) {
			int open = t.indexOf(level.length()==0?"<?":("<?JS"+level));
			int openlength = open>=0 && t.substring(open).startsWith("<?JS"+level)?4+level.length():2;
			int close = t.indexOf("?>",open);
			
			if(open==-1) 
				break;
			
			if (close<open) throw new RuntimeException("JS end tag before start tag!");
			
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

	private static Pattern printPattern = Pattern.compile("^[_A-Za-z][_A-Za-z0-9]*(\\.[_A-Za-z][_A-Za-z0-9]*)*(\\[[_A-Za-z0-9]+\\])?$");
	private String clean(String t) {
		StringBuilder re = new StringBuilder();
		while (t.length()>0) {
			int open = t.indexOf("<?");
			int openlength = open>=0 && t.substring(open).startsWith("<?JS")?4:2;
			int close = t.indexOf("?>",open);
			
			if(open==-1) 
				break;
			
			if (close==-1) throw new RuntimeException("No Jhp end tag at "+position(t, open));
			if (close<open) 
				throw new RuntimeException("Jhp end tag before start tag at "+position(t, close)+" and "+position(t,open));
			
			String before = t.substring(0, open);
			String in = t.substring(open+openlength, close);
			t = t.substring(close+2);

			// remove newline at JS only lines
			if ((before.isEmpty() || before.endsWith("\n")) && t.startsWith("\n"))
				t = t.substring(1);
			
			re.append(before);
//			if (StringUtils.isJavaIdentifier(in.trim()) || EI.split(in.trim(), '.').filter(s->!StringUtils.isJavaIdentifier(s)).count()==0)
			if (printPattern.matcher(in.trim()).find())
				re.append("<?JS print(").append(in).append(") ?>");
			else
				re.append("<?JS").append(in).append("?>");
			
		}
		re.append(t);
		return re.toString();
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
