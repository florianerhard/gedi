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
package gedi.util.nashorn;
import gedi.app.classpath.ClassPathCache;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.datastructure.charsequence.MutableCharSequence;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.mutable.MutableMonad;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import jdk.nashorn.api.scripting.ScriptObjectMirror;



public class JS {

	
	
	private ScriptEngine engine;
//	private HashMap<String, String> para = new HashMap<String, String>();
	private boolean replaceInPlace = false;
	private boolean interpolateStrings = true;
	
	private HashSet<String> systemVariables = new HashSet<String>(); 
	
	public JS()  {
		engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine.setBindings(engine.createBindings(),ScriptContext.ENGINE_SCOPE);
		
		try {
			engine.eval(new StringReader("function print(arg) { context.getWriter().write(Java.type(\"gedi.util.StringUtils\").toString(arg)); context.getWriter().flush(); }"));
			engine.eval(new StringReader("function printf(format,arg) { context.getWriter().write(Java.type(\"java.lang.String\").format(Java.type(\"java.util.Locale\").US,format,arg)); context.getWriter().flush(); }"));
			engine.eval(new StringReader("function println(arg) { context.getWriter().write(Java.type(\"gedi.util.StringUtils\").toString(arg)+\"\\n\"); context.getWriter().flush(); }"));
			
			engine.eval(new StringReader("function echo(obj) { print(obj.getClass().getSimpleName()+\":\\n\"+Java.type(\"gedi.util.StringUtils\").toString(obj)+\"\\n\");}"));
			engine.eval(new StringReader("function load(path) { "
					+ "var loader = Java.type(\"gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint\").getInstance().get(Java.type(\"java.nio.file.Paths\").get(path));"
					+ "if (loader==null) return null;"
					+ "return loader.load(Java.type(\"java.nio.file.Paths\").get(path));"
					+ "}"));
			
			
			
			
			systemVariables.add("print");
			systemVariables.add("printf");
			systemVariables.add("println");
			systemVariables.add("echo");
			
			setStderr(new OutputStreamWriter(System.err));
			setStdout(new OutputStreamWriter(System.out));
			
			
		} catch (ScriptException e) {
			throw new RuntimeException("Error initializing nashorn!",e);
		}
		
	}
	
	
	public void setInterpolateStrings(boolean interpolateStrings) {
		this.interpolateStrings = interpolateStrings;
	}
	
	public void setReplaceInPlace(boolean replaceInPlace) {
		this.replaceInPlace = replaceInPlace;
	}
	
	public Map<String,Object> getVariables(boolean systemAlso) {
		if (systemAlso)
			return engine.getBindings(ScriptContext.ENGINE_SCOPE);
		LinkedHashMap<String, Object> re = new LinkedHashMap<String, Object>(engine.getBindings(ScriptContext.ENGINE_SCOPE));
		re.keySet().removeAll(systemVariables);
		return re;
	}
	
	public HashMap<String,Object> saveState() {
		HashMap<String,Object> re = new HashMap<>();
		Bindings b = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		for (String r : b.keySet())
			if (r!=null && b.get(r)!=null)
				re.put(r, b.get(r));
		return re;
	}
	
	public void restoreState(HashMap<String,Object> saved) {
		engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();
		engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(saved);
	}
	
	public <T> T getVariable(String name) {
		return (T) engine.getBindings(ScriptContext.ENGINE_SCOPE).get(name);
	}
	
	public JS addParam(HashMap<String,Object> parsed) {	
		putVariable("argsMap",parsed);
		putVariables(parsed);
		return this;
	}
	
	public JS addParam(String[] args, boolean skipFirst) {
		if (skipFirst) {
			String[] param = new String[args.length-1];
			System.arraycopy(args, 1, param, 0, param.length);
			args = param;
		}


		putVariable("args", args);
		return addParam(parseParameter(args, false));
	}
	
	public JS putVariable(String name, Object var) {
		engine.getBindings(ScriptContext.ENGINE_SCOPE).put(name, var);
		return this;
	}
	
	
	public JS putSystemVariable(String name, Object var) {
		engine.getBindings(ScriptContext.ENGINE_SCOPE).put(name, var);
		systemVariables.add(name);
		return this;
	}

	
	public JS putVariables(Map<String,Object> vars) {
		for (String n : vars.keySet())
			engine.getBindings(ScriptContext.ENGINE_SCOPE).put(n, vars.get(n));
		return this;
	}
	public JS putSystemVariables(Map<String,Object> vars) {
		for (String n : vars.keySet())
			engine.getBindings(ScriptContext.ENGINE_SCOPE).put(n, vars.get(n));
		systemVariables.addAll(vars.keySet());
		return this;
	}
	
	public JS setStdout(Writer out) {
		engine.getContext().setWriter(out);
		return this;
	}
	
	public JS setStderr(Writer err) {
		engine.getContext().setErrorWriter(err);
		return this;
	}
	
	public JS setStdin(Reader in) {
		engine.getContext().setReader(in);
		return this;
	}
	
	public Writer getStdout() {
		return engine.getContext().getWriter();
	}
	
	public Writer getStderr() {
		return engine.getContext().getErrorWriter();
	}
	
	public Reader getStdin() {
		return engine.getContext().getReader();
	}
	
	/**
	 * Puts all non overloaded public and static functions and variables of the given class into javascript context.
	 * @param cls
	 * @throws ScriptException 
	 */
	public void injectStatics(Class<?> cls) throws ScriptException {
		HashMap<String,Method> methods = new HashMap<>();
		for (Method m : cls.getMethods()) {
			int mod = m.getModifiers();
			if (Modifier.isPublic(mod) && Modifier.isStatic(mod)) {
				if (methods.containsKey(m.getName()))
					methods.put(m.getName(), null);
				else
					methods.put(m.getName(), m);
			}
		}
		
		for (Method m : methods.values()) {
			if (m!=null) {
				String name = m.getName();
				eval("var "+name+" = Java.type(\""+cls.getName()+"\")."+name);
			}
		}
		
		for (Field f : cls.getFields()) {
			int mod = f.getModifiers();
			if (Modifier.isPublic(mod) && Modifier.isStatic(mod)) {
				String name = f.getName();
				try {
					engine.getBindings(ScriptContext.ENGINE_SCOPE).put(name, f.get(null));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("This should never happen!");
				}
			}
		}
	}
	
	/**
	 * Puts all non overloaded public functions and variables of the given object into javascript context.
	 * @param cls
	 * @throws ScriptException 
	 */
	public void injectObject(Object o) throws ScriptException {
		if (o instanceof ScriptObjectMirror) {
			ScriptObjectMirror m = (ScriptObjectMirror) o;
			engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(m);
		} else {
		
			HashMap<String,Method> methods = new HashMap<>();
			for (Method m : o.getClass().getMethods()) {
				int mod = m.getModifiers();
				if (Modifier.isPublic(mod) && !Modifier.isStatic(mod) && m.getDeclaringClass()!=Object.class) {
					if (methods.containsKey(m.getName()))
						methods.put(m.getName(), null);
					else
						methods.put(m.getName(), m);
				}
			}
			
			String oname = "I"+StringUtils.sha1(""+Objects.hashCode(o));
			try {
				engine.getBindings(ScriptContext.ENGINE_SCOPE).put(oname,o);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("This should never happen!");
			}
			
			for (Method m : methods.values()) {
				if (m!=null) {
					String name = m.getName();
					StringBuilder sb = new StringBuilder();
					sb.append("var ").append(name).append(" = function(");
					for (int i=0; i<m.getParameterCount(); i++) {
						if (i>0) sb.append(",");
						sb.append("V").append(i);
					}
					sb.append(") ").append(oname).append(".").append(name).append("(");
					for (int i=0; i<m.getParameterCount(); i++) {
						if (i>0) sb.append(",");
						sb.append("V").append(i);
					}
					sb.append(")");
					eval(sb.toString());
				}
			}
			
			for (Field f : o.getClass().getFields()) {
				int mod = f.getModifiers();
				if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
					String name = f.getName();
					try {
						engine.getBindings(ScriptContext.ENGINE_SCOPE).put(name, f.get(o));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new RuntimeException("This should never happen!");
					}
				}
			}
		}
		
	}

	
	public String prepareScript(String path) throws IOException, ScriptException {
		StringBuilder sb = new StringBuilder();
		new LineOrientedFile(path).lineIterator().forEachRemaining(l->sb.append(l.startsWith("#")?"\n":(l+"\n")));

		return prepareSource(sb.toString());
	}
	
	
	public <T> T execScript(String path) throws IOException, ScriptException {
		StringBuilder sb = new StringBuilder();
		new LineOrientedFile(path).lineIterator().forEachRemaining(l->sb.append(l.startsWith("#")?"\n":(l+"\n")));

		return execSource(sb.toString());
	}
	
	public <T> T execScript(InputStream stream) throws IOException, ScriptException {
		StringBuilder sb = new StringBuilder();
		try (LineIterator it = new LineIterator(stream)) {
			it.forEachRemaining(l->sb.append(l.startsWith("#")?"\n":(l+"\n")));	
		}
		return execSource(sb.toString());
	}
	
	public String prepareSource(String src) throws ScriptException {
		String re = src;
		if (interpolateStrings) {
			MutableMonad<ScriptException> ex = new MutableMonad<ScriptException>();
			re = StringUtils.replaceVariablesInQuotes(re,name->{
				try {
					return StringUtils.toString(eval(name));
				} catch (ScriptException e) {
					ex.Item=e;
					return null;
				}
			});
			if (ex.Item!=null) throw ex.Item;
		}
		re = replaceClassesPackages(re);
		
		return re;
	}
	

	public CompiledScript compileSource(String src) throws ScriptException {
		return compilePreparedSource(prepareSource(src));
	}
	
	public CompiledScript compilePreparedSource(String src) throws ScriptException {
		return ((Compilable)engine).compile(src);
	}
	
	public <T> T execSource(String src) throws ScriptException {
		return eval(prepareSource(src));
	}
	
	public void setSelf(DynamicObject json) throws ScriptException {
		if (json.isObject()) {
			for (String p : json.getProperties())
				eval("var "+p+"="+json.getEntry(p).toJson());
		}
	}
	
	public <T> T eval(String src) throws ScriptException {
		src = src.replace("_\n", "\n");
		try {
			return (T) engine.eval(src);
		} catch (Throwable e) {
			try {
				Map<String, Object> vars = getVariables(true);
				StringBuilder sb = new StringBuilder();
				for (String k : vars.keySet())
					sb.append(k).append(": ").append(StringUtils.toString(vars.get(k))).append("\n");
				FileUtils.writeAllText(sb.toString(), new File("dumb.table"));
				FileUtils.writeAllText(src, new File("dumb.js"));
			} catch (IOException e1) {
			}
			throw new RuntimeException("Could not execute JS, dumb.js saved!",e);
		}
	}
	
	public <T> T invokeFunction(String fun, Object...para) throws ScriptException {
		return (T) ((ScriptObjectMirror)engine.eval(fun)).call(null, para);
	}
	
	public <T> T invokeFunctionOn(String fun, Object on, Object...para) throws ScriptException {
		return (T) ((ScriptObjectMirror)engine.eval(fun)).call(on, para);
	}
	
	
	private String replaceClassesPackages(String src) {
		ClassPathCache classes = ClassPathCache.getInstance();
		src = StringUtils.removeComments(src);
		
		String m1 = MaskedCharSequence.maskEscaped(src, '\0', '"','\'').toString();
		MaskedCharSequence masked = MaskedCharSequence.maskQuotes(m1, ' ');
		MutableCharSequence seq = new MutableCharSequence(masked);

		Pattern word = Pattern.compile("[A-Z][A-Za-z0-9_]+(\\[\\])*");

		Matcher m = word.matcher(seq);
		StringBuffer pre = new StringBuffer();
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			seq.setSequence(src);
			m.appendReplacement(sb, "");
			seq.setSequence(masked);
			
			String rep = m.group();
			String woarr = StringUtils.trim(rep, '[',']');
			String arr = rep.substring(woarr.length());
			String Larr = StringUtils.repeat("L", arr.length()/2);
			
			if (isStart(sb) && 
					classes.containsName(woarr)&& 
					!rep.equals("Java")) {
				
				String c = classes.getFullName(woarr);
				if (c!=null) {
					if (!replaceInPlace) {
						pre.append("var "+Larr+woarr+" = Java.type(\""+c+arr+"\"); ");
						rep = Larr+woarr;
					}
					else
						rep = "(Java.type(\""+c+arr+"\"))";
				}
			}
			sb.append(rep);
		}
		seq.setSequence(src);
		m.appendTail(sb);
//		for (String k: para.keySet())
//			pre.append("var "+k+" = "+para.get(k)+"; ");

		return pre.toString()+sb.toString();
	}

	private static boolean isStart(StringBuffer sb) {
		if (sb.length()==0) return true;
		return !Character.isDigit(sb.charAt(sb.length()-1)) && 
				!Character.isLowerCase(sb.charAt(sb.length()-1)) && 
				sb.charAt(sb.length()-1)!='.';
	}

	public static HashMap<String,Object> parseParameter(String[] args, boolean quote) {

		HashMap<String,Object> re = new HashMap<String, Object>();
		for (int i=0; i<args.length;i++) {
			if (args[i].startsWith("-")) {
				String name = args[i].substring(1);
				if (i+1<args.length && !args[i+1].startsWith("-")) 
					re.put(name, quote?convertType(args[++i]):args[++i]);
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


	public static boolean[] booleanArray(boolean...a) {
		return a;
	}
	public static byte[] byteArray(byte...a) {
		return a;
	}
	
	
	public static short[] shortArray(short...a) {
		return a;
	}
	
	public static int[] intArray(int...a) {
		return a;
	}
	
	public static long[] longArray(long...a) {
		return a;
	}
	
	public static char[] charArray(char...a) {
		return a;
	}

	public static float[] floatArray(float...a) {
		return a;
	}
	
	public static double[] doubleArray(double...a) {
		return a;
	}
	public static <T> T[] array(T...a) {
		return a;
	}
	public static <T> T[] array(Class<T> cls, T...a) {
		T[] re = (T[]) Array.newInstance(cls, a.length);
		System.arraycopy(a, 0, re, 0, a.length);
		return re;
	}
	
	public static boolean[] booleanBuffer(int l) {
		return new boolean[l];
	}
	public static byte[] byteBuffer(int l) {
		return new byte[l];
	}
	
	
	public static short[] shortBuffer(int l) {
		return new short[l];
	}
	
	public static int[] intBuffer(int l) {
		return new int[l];
	}
	
	public static long[] longBuffer(int l) {
		return new long[l];
	}
	
	public static char[] charBuffer(int l) {
		return new char[l];
	}

	public static float[] floatBuffer(int l) {
		return new float[l];
	}
	
	public static double[] doubleBuffer(int l) {
		return new double[l];
	}
	public static <T> T[] buffer(int l, Class<T> cls) {
		return (T[]) Array.newInstance(cls, l);
	}


	


	


}
