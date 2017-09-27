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

import gedi.app.Gedi;
import gedi.app.classpath.ClassPathCache;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.nashorn.JS;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


public class Nashorn {

	
	public static void main(String[] args) throws ScriptException, IOException {
		Gedi.startup();
		
		if (args.length==2 && args[0].equals("-e")) {
			JS js = new JS();
			String prep = js.prepareSource(args[1]);
			js.eval(prep);
		}
		else {
			JS js = new JS();
			js.addParam(args, true);
			
			String prep = js.prepareScript(args[0]);
			FileUtils.writeAllText(prep, new File(args[0]+".replaced"));
			
			js.eval(prep);
		}
		
	}
	
	public static void main2(String[] args) throws ScriptException, IOException {
		Gedi.startup();
		
		String path = args[0];
		String[] param = new String[args.length-1];
		System.arraycopy(args, 1, param, 0, param.length);
		
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        
        HashMap<String, String> argsMap = parseParameter(param);
        
        Bindings bindings = engine.createBindings();
        bindings.put("args", param);
        bindings.put("argsMap", argsMap);
        engine.setBindings(bindings,ScriptContext.GLOBAL_SCOPE);
        
        StringBuilder sb = new StringBuilder();
        new LineOrientedFile(path).lineIterator().forEachRemaining(l->sb.append(l.startsWith("#")?"\n":(l+"\n")));
        
        String src = replaceClassesPackages(argsMap,sb.toString());
        FileUtils.writeAllText(src, new File(path+".replaced"));
        engine.eval(src);
	}
	
	public static String replaceClassesPackages(HashMap<String,String> param, String src) {
		ClassPathCache classes = ClassPathCache.getInstance();
		
		Pattern word = Pattern.compile("[A-Z][A-Za-z0-9_]+");
		
		Matcher m = word.matcher(src);
		StringBuffer pre = new StringBuffer();
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, "");
			String rep = m.group();
			if (isStart(sb) && 
					classes.containsName(m.group())&& 
					!m.group().equals("Java")) {
				
				String c = classes.getFullName(m.group());
				if (c!=null) 
//					rep = "Packages."+c.replace('/', '.');
					pre.append("var "+m.group()+" = Java.type(\""+c+"\"); ");
			}
			sb.append(rep);
		}
		m.appendTail(sb);
		for (String k: param.keySet())
			pre.append("var "+k+" = "+param.get(k)+"; ");
			
		return pre.toString()+sb.toString();
	}

	private static boolean isStart(StringBuffer sb) {
		if (sb.length()==0) return true;
		return !Character.isDigit(sb.charAt(sb.length()-1)) && 
		!Character.isLowerCase(sb.charAt(sb.length()-1)) && 
		sb.charAt(sb.length()-1)!='.';
	}
	
	private static HashMap<String,String> parseParameter(String[] args) {
		
		HashMap<String,String> re = new HashMap<String, String>();
		for (int i=0; i<args.length;i++) {
			if (args[i].startsWith("-") && args.length>1) {
				String name = args[i].substring(1);
				if (i+1<args.length && !args[i+1].startsWith("-")) 
					re.put(name, convertType(args[++i]));
				else
					re.put(name,"true");
			}
		}
		return re;
	}

	private static String convertType(String s) {
		if (StringUtils.isNumeric(s)) return s;
		return "\""+s+"\"";
	}
	
}
