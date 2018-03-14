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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.logging.Logger;

import javax.script.ScriptException;

import gedi.app.Config;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.StringLineWriter;

/**
 * Use {@link Jhp} for a template engine. Parameters can be supplied via json or directly as java objects. Templates can either be obtained from files
 * or are built-in. First, the output is written to a buffer (access it via {@link #toString()}). However, calling the {@link #push(String)} and {@link #pop()}
 * methods, output can be redirected to files. By default, a {@link TemplateEngine} objects creates new files at the beginning, but appends during runtime
 * (i.e. push("x"), template("y"), pop(), template("z"), push("x"), template("a") will lead to templates y and a in file x, and template z in the buffer)
 * Importantly, templates themselves can issue all these {@link TemplateEngineCommands}!  
 * 
 * Don't forget to call {@link #finish()} in the end, to close all readers
 * 
 * @author erhard
 *
 */
public class TemplateEngine {
	private static final Logger log = Logger.getLogger( TemplateEngine.class.getName() );
	
	private Jhp jhp = new Jhp();
	
	private HashMap<String,Object> variables = new HashMap<>();
	private DynamicObject param;
	
	private HashMap<String,StringLineWriter> buffers = new HashMap<>();
	private StringLineWriter buffer = new StringLineWriter();
	private TemplateOutput currentOutput;
	
	private HashMap<String,TemplateOutput> outputs = new HashMap<>();
	private Stack<String> ostack = new Stack<>();

	private ArrayList<String> additionalTemplateSearchURLs = new ArrayList<>();  
	
	public TemplateEngine() {
		this(Config.getInstance().getConfig());
	}
	
	public TemplateEngine(DynamicObject param) {
		this.param = param;
		
		init();
	}
	
	/**
	 * ${name} in this url is replaced by the name, ${wd} by the working directory
	 * @param url
	 */
	public void addTemplateSearchURL(String url) {
		additionalTemplateSearchURLs.add(url);
	}

	
	private void init() {
		outputs.put(null, currentOutput = new TemplateOutput(buffer));
		ostack.push(null);
	}
	
	public TemplateEngine setBuffer(String content) {
		buffer.setContent(content);
		return this;
	}
	
	public void declareBuffer(String id) {
		buffers.put(id, new StringLineWriter());
	}
	
	public String getBuffer(String id) {
		return !buffers.containsKey(id)?"":buffers.get(id).toString();
	}
	
	public String getAndDiscardBuffer(String id) {
		String re = getBuffer(id);
		buffers.remove(id);
		outputs.remove(id);
		return re;
	}
	
	private Jhp prepare() {
		
		try {
			jhp.resetJS();
			jhp.getJs().putVariable("log", log);
			jhp.getJs().setInterpolateStrings(false);
			jhp.getJs().injectObject(this);
			jhp.getJs().setSelf(param);
			jhp.getJs().putVariables(variables);
			jhp.getJs().putVariable("outputInfo", currentOutput);
		} catch (ScriptException e) {
			throw new TemplateException("Could not prepare JS", e);
		}
		return jhp;
	}
	
	/**
	 * The template generator can do anything with this engine. Typically, it adds certain parameters and executes a template.
	 * @param gen
	 * @return
	 */
	public <G extends TemplateGenerator<G>> TemplateEngine accept(G[] gen) {
		gen[0].accept(this,gen);
		return this;
	}
	
	/**
	 * Execute the given source and append it to the current writer
	 * @param src
	 * @return
	 */
	public TemplateEngine direct(String src)  {
		Jhp jhp = prepare();
		try {
			currentOutput.writer.write(jhp.apply(src));
		} catch (IOException e) {
			throw new TemplateException("Could not write!",e);
		}
		jhp.getParameters().appendOutputs(variables);
		jhp.getParameters().nextFile();
		return this;
	}
	
	/**
	 * Load the template with given name, execute it and append it to the current writer
	 * @param name
	 * @return
	 */
	public TemplateEngine template(String name) {
		try {
			return direct(readSrc(name));
		} catch (IOException e) {
			throw new TemplateException("Could not read template "+name,e);
		}
	}
	
	/**
	 * Execute only the given level of the given source.
	 * @param src
	 * @param level
	 * @return
	 */
	public TemplateEngine executeSource(String src, String level) {
		Jhp jhp = prepare();
		jhp.apply(src,level);
		jhp.getParameters().appendOutputs(variables);
		jhp.getParameters().nextFile();
		return this;
	}
	
	/**
	 * Load the template with given name, and execute only its given level.
	 * @param name
	 * @param level
	 * @return
	 */
	public TemplateEngine execute(String name, String level) {
		try {
			return executeSource(readSrc(name), level);
		} catch (IOException e) {
			throw new TemplateException("Could not read template "+name,e);
		}
	}
	
	/**
	 * Load the given json file and cascade its entries into the current variables.
	 * @param path
	 * @return
	 */
	public TemplateEngine json(String path) {
		try {
			DynamicObject param1 = DynamicObject.parseJson(FileUtils.readAllText(new File(path)));
			param = DynamicObject.cascade(param,param1);
			return this;
		} catch (IOException e) {
			throw new TemplateException("Could not read json file "+path,e);
		}
		
	}
	
	/**
	 * Set the given variable to the given value (parsed as json or used directly)
	 * @param variable
	 * @param value
	 * @return
	 */
	public TemplateEngine parameter(String variable, String value) {
		DynamicObject param1=DynamicObject.parseExpression(variable, DynamicObject.parseJsonOrString(value));
		param = DynamicObject.cascade(param,param1);
		return this;
	}
	
	public TemplateEngine parameter(String variable, DynamicObject value) {
		DynamicObject param1=DynamicObject.from(variable, value);
		param = DynamicObject.cascade(param,param1);
		return this;
	}
	
	
	/**
	 * Set the given variable to the given value
	 * @param variable
	 * @param value
	 * @return
	 */
	public TemplateEngine set(String variable, Object value) {
		variables.put(variable, value);
		return this;
	}
	
	public <T> T get(String variable) {
		if (variables.containsKey(variable))
			return (T) variables.get(variable);
		if (param.hasProperty(variable))
			return (T) param.getEntry(variable);
		return null;
	}
	
	/**
	 * Set the current writer to the given file and keep all other writers on the writer stack  
	 * @param path
	 * @return
	 */
	public TemplateEngine push(String path) {
		if (outputs.containsKey(path)) 
			outputs.get(path).index++;
		else if (buffers.containsKey(path))
			outputs.put(path, new TemplateOutput(buffers.get(path)));
		else 
			outputs.put(path, new TemplateOutput(new LineOrientedFile(path).write()));
		
		currentOutput = outputs.get(path);
		ostack.push(path);
		return this;
	}
	
	/**
	 * Reset the current writer to the previous one.
	 * @return
	 */
	public TemplateEngine pop() {
		ostack.pop();
		if (!ostack.isEmpty())
			currentOutput = outputs.get(ostack.peek());
		return this;
	}
	
	/**
	 * Finish all writers (i.e. close them) and reset the stack to the buffer only
	 * @return
	 */
	public TemplateEngine finish() {
		ostack.clear();
		for (TemplateOutput to : outputs.values())
			try {
				to.writer.close();
			} catch (IOException e) {
				throw new TemplateException("Could not close writer!",e);
			}
		outputs.clear();
		
		init();
		return this;
	}
	
	/**
	 * Return the help message for the given template
	 * @param name
	 * @return
	 */
	public String help(String name) {
		Jhp jhp = prepare();
		try {
			jhp.apply(readSrc(name), "0");
		} catch (IOException e) {
			throw new TemplateException("Could not read template "+name,e);
		}
		return "Template variables:\n\n"+jhp.getParameters().getUsage(EI.wrap(param.getProperties()).chain(variables.keySet().iterator()).toArray(new String[0]));
	}
	

	@Override
	public String toString() {
		return buffer.toString();
	}
	

	
	private URL stringToUrl(String s) {
		try {
			return new URL(s);
		} catch (MalformedURLException e1) {
			throw new RuntimeException("Cannot parse template search path "+s,e1);
		}
	}
	
	private String readSrc(String name) throws IOException {
		HashMap<String,String> mapper = new HashMap<>();
		mapper.put("wd", System.getProperty("user.dir"));
		mapper.put("name", name);
		
		for (URL u : EI.wrap(additionalTemplateSearchURLs)
					.chain(EI.wrap(Config.getInstance().getTemplateSearchURLs()))
						.map(s->StringUtils.replaceVariables(s, mapper::get))
						.map(this::stringToUrl)
						.chain(EI.singleton(TemplateEngine.class.getResource(name)))
						.loop()) {
			try {
				if (!new File(u.getPath()).isDirectory()) {
					InputStream str = u.openStream();
					return new LineIterator(str).readAllText();
				}
			} catch (FileNotFoundException | UnknownHostException e) {
			}
		}
		
		
		throw new TemplateException("Cannot find template "+name);
	}

	/**
	 * Save the content of the given vars (not considering the {@link DynamicObject}) and returns a Runnable to restore them
	 * @param vars
	 * @return
	 */
	public Runnable save(String... vars) {
		HashMap<String,Object> save = new HashMap<>();
		for (String v : vars) {
			if (variables.containsKey(v))
				save.put(v,variables.get(v));
				save.put(v, null);
		}
		return ()->{
			for (String k : save.keySet()) {
				Object a = save.get(k);
				if (a==null) 
					variables.remove(k);
				else
					variables.put(k, a);
			}
		};
	}

	
	
}
