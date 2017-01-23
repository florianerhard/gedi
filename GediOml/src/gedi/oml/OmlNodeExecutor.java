package gedi.oml;

import gedi.app.classpath.ClassPathCache;
import gedi.util.GeneralUtils;
import gedi.util.ReflectionUtils;
import gedi.util.parsing.ArrayParser;
import gedi.util.parsing.Parser;
import gedi.util.parsing.ParserCache;
import gedi.util.properties.PropertyProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

import javafx.beans.property.Property;

@SuppressWarnings({"unchecked","rawtypes"})
public class OmlNodeExecutor {
	
	
	public static final String INLINED_CALL = "INLINED_CALL";
	
	private OmlInterceptorStack interceptors = new OmlInterceptorStack();
	
	public OmlNodeExecutor() {
	}
	

	public OmlNodeExecutor addInterceptor(OmlInterceptor interceptor) {
		interceptors.push(interceptor);
		return this;
	}

	public <T> T execute(OmlNode node, HashMap<String,Object> context) {
		return execute(node,null, context);
	}
	
	public <T> T execute(OmlNode node) {
		return execute(node,null, new HashMap<String, Object>());
	}
	
	public <P,T> T execute(OmlNode node, P parent) {
		return execute(node,parent, new HashMap<String, Object>());
	}
	
	public <P,T> T execute(OmlNode node, P parent, HashMap<String,Object> context) {
		
		ArrayList<OmlNode> children = node.getChildren();
		LinkedHashMap<String, String> attributes = node.getAttributes();
		String name = node.getName();
		
		if (parent instanceof OmlInterceptor) 
			interceptors.push((OmlInterceptor)parent);
		
		T re = null;
		
	
		children = interceptors.getChildren(node,node.getChildren(),context);
		attributes = interceptors.getAttributes(node,node.getAttributes(),context);
		name = interceptors.getName(node,node.getName(),context);
		
		Caller caller = null;
		try {
			// check whether this is method node
			caller = checkMethod(parent,name,attributes, children, context);
			if (caller==null)
				caller = checkMethod(parent,"set"+name,attributes, children,context);
			if (caller==null)
				caller = checkMethod(parent,"add"+name,attributes, children,context);
			if (caller==null)
				caller = checkProperty(parent,name,attributes, children,context);
			// check whether this is constructor node
			if (caller==null)
				caller = checkConstructor(parent,name,attributes, children,context);
						
			if (caller==null)
				caller = checkMethod(parent,"get"+name,attributes, children,context);
			
		} catch (Exception e) {
			
			throw new RuntimeException("Could not execute OML node: "+node,e);
		}
		// call
		if (caller==null){
			throw new RuntimeException("Could not execute OML node: no matching method/constructor found: "+node);
		}
		
		
		try {
			re = (T) caller.call();
			String id = node.getId();
			String[] classes = node.getClasses();
			if (re!=null && id!=null) {
				Method idsetter = ReflectionUtils.findMethod(re.getClass(), "setId", String.class);
				if (idsetter!=null) idsetter.invoke(re, id);

				context.put(id, re);
//				interceptors.newId(id,node,re,context);
			}
			
			if (re!=null && id!=null) {
				Method clsetter = ReflectionUtils.findMethod(re.getClass(), "setClasses", String[].class);
				if (clsetter!=null) clsetter.invoke(re, classes);

//				interceptors.newClasses(classes,node,re,context);
			}
			
			interceptors.setObject(node, re, id, classes, context);
			
		} catch (Exception e) {
			throw new RuntimeException("Could not execute OML node: "+name,e);
		}
		
		// execute children (not inlined)
		if (parent instanceof OmlInterceptor && !((OmlInterceptor)parent).useForSubtree()) 
			interceptors.pop();
		
		for (OmlNode ch : children) {
			if (!caller.isInlined(ch)) {
				Object chob = execute(ch,re, context);
				interceptors.childProcessed(node, ch, re, chob, context);
			}
		}
		
		if (parent instanceof OmlInterceptor && ((OmlInterceptor)parent).useForSubtree()) 
			interceptors.pop();
			
		return re;
	}
	
	
	private MethodCaller checkMethod(Object o, String methodName, LinkedHashMap<String, String> attributes, ArrayList<OmlNode> children, HashMap<String,Object> context) throws Exception {
		if (o==null) return null;
		
		Method best = null;
		Supplier[] bestPara = null;
		boolean hasAnno = false;
		Method equallyGood = null;
		
		for (Method m : o.getClass().getMethods()) {
			if (m.getName().equalsIgnoreCase(methodName)){
				
				Param[] anno = toParamAnnotations(m.getParameterAnnotations());
				Supplier[] para = determineSuppliers(o, m.getParameterTypes(),anno, attributes,children, context);
				
				if (para!=null) {
					
					
					if (!hasAnno && anno!=null) {
						bestPara = para;
						hasAnno = true;
						best = m;
						equallyGood = null;
					} else {
						if (bestPara==null || para.length>bestPara.length) {
							bestPara = para;
							best = m;
							equallyGood = null;
						} else if (para.length==bestPara.length){
							// the one with more specified wins
							int specifiedParsersCurrent = countSpecifiedParsers(para);
							int specifiedParsersBest = countSpecifiedParsers(bestPara);
							if (specifiedParsersCurrent>specifiedParsersBest) {
								bestPara = para;
								best = m;
								equallyGood = null;
							}
							else if (specifiedParsersCurrent==specifiedParsersBest)
								equallyGood = m;
						}
					}
					
					
				}
			}
		}
		
		if (equallyGood!=null) throw new Exception("Could not determine method for tag, two methods are equally good: "+best.toGenericString()+" and "+equallyGood.toGenericString());
		
		if (best!=null)
			return new MethodCaller(best, o, bestPara);
		return null;
	}
	
	private PropertyCaller checkProperty(Object o, String methodName, LinkedHashMap<String, String> attributes, ArrayList<OmlNode> children, HashMap<String,Object> context) throws Exception {
		if (o==null || !(o instanceof PropertyProvider) || attributes.size()!=1) return null;
		
		PropertyProvider p = (PropertyProvider)o;
		
		
		if (p.getProperties().containsKey(methodName)) {
			Property<Object> prop = p.getProperty(methodName);
			Supplier[] para = determineSuppliers(o, new Class[] {p.getPropertyClass(methodName)},null, attributes,children, context);
			if (para!=null)
				return new PropertyCaller(prop, para[0]);
		}
		return null;
	}
	
	
	
	private ConstructorCaller checkConstructor(Object o, String className, LinkedHashMap<String, String> attributes, ArrayList<OmlNode> children, HashMap<String,Object> context) throws Exception {
		
		Class cls = null;
		
		if (o!=null && ClassPathCache.getInstance().containsName(o.getClass().getName()+"$"+className))
			cls = ClassPathCache.getInstance().getClass(o.getClass().getName()+"$"+className);
		else if (!ClassPathCache.getInstance().containsName(className)) 
			return null;
		else 
			cls = ClassPathCache.getInstance().getClass(className);
		
		Constructor best = null;
		Supplier[] bestPara = null;
		boolean hasAnno = false;
		Constructor equallyGood = null;
		
		
		for (Constructor m : cls.getConstructors()) {
				
			Param[] anno = toParamAnnotations(m.getParameterAnnotations());
			Supplier[] para = determineSuppliers(o, m.getParameterTypes(),anno, attributes, children, context);
			
			if (!hasAnno && anno!=null) {
				bestPara = para;
				hasAnno = true;
				best = m;
				equallyGood = null;
			} else if (para!=null){
				if (bestPara==null || para.length>bestPara.length) {
					bestPara = para;
					if (para!=null)
						best = m;
					equallyGood = null;
				} else if (para.length==bestPara.length){
					// the one with more specified wins
					int specifiedParsersCurrent = countSpecifiedParsers(para);
					int specifiedParsersBest = countSpecifiedParsers(bestPara);
					if (specifiedParsersCurrent>specifiedParsersBest) {
						bestPara = para;
						best = m;
						equallyGood = null;
					}
					else if (specifiedParsersCurrent==specifiedParsersBest)
						equallyGood = m;
				}
			}
		}
		
		if (equallyGood!=null) 
			throw new Exception("Could not determine method for tag, two methods are equally good: "+best.toGenericString()+" and "+equallyGood.toGenericString());
		
		
		if (best!=null){
			Method adder = o!=null?ReflectionUtils.findMethod(o, "add", cls):null;
			if (adder==null && o!=null) adder = ReflectionUtils.findMethod(o, "set", cls);
			return new ConstructorCaller(best, bestPara, o, adder);
		}
		
		return null;
	}
	
	

	private int countSpecifiedParsers(Supplier[] ss) {
		int re = 0;
		for (Supplier s : ss)
			if (s instanceof ParsedParameterValue)
				if (((ParsedParameterValue)s).isParserSpecified())
					re++;
		return re;
	}

	private Param[] toParamAnnotations(
			Annotation[][] parameterAnnotations) throws Exception {
		if (parameterAnnotations.length==0) return null;
		for (int i=0; i<parameterAnnotations[0].length; i++) {
			if (parameterAnnotations[0][i] instanceof Param) {
				Param[] re = new Param[parameterAnnotations.length];
				re[0] = (Param) parameterAnnotations[0][i];
				for (i=1; i<parameterAnnotations.length; i++) {
					for (int j=0; j<parameterAnnotations[i].length; j++) {
						if (parameterAnnotations[i][j] instanceof Param)
							re[i] = (Param) parameterAnnotations[i][j];
					}	
					if (re[i]==null) throw new Exception("Method has @Param annotations, but not all parameters!");
				}
				return re;
			}
		}
		return null;
	}

	private Supplier[] determineSuppliers(Object o, Class<?>[] types,
			Param[] param, LinkedHashMap<String, String> attributes, ArrayList<OmlNode> children, HashMap<String,Object> context) {
		
		if (attributes.size()==0 && children.size()==1 && types.length==1) {
			// special case: inlined, id and reference ommitted
			OmlNode inlined = children.get(0);	
			try {
				if (inlined!=null && types[0].isAssignableFrom(ClassPathCache.getInstance().getClass(inlined.getName()))) 
					return new Supplier[] {new InlinedParameterValue(inlined, o, context)};
			} catch (Throwable e) {} // silently skip
		}
		
		if (param!=null) {
			Supplier[] re = new Supplier[param.length];
			for (int i=0; i<re.length; i++) {
				String name = param[i].value();
				String value = attributes.get(name);
				re[i] = determineSupplier(types[i], o, name, value, param[i].parser(), param[i].defaultValue(), attributes,children,context);
				if (re[i]==null) return null;
			}
			// accept only if all specified attributes would be used for this method
			HashSet<String> specified = new HashSet<String>(attributes.keySet());
			for (Supplier s : re)
				if (s instanceof ParsedParameterValue)
					specified.remove(((ParsedParameterValue)s).attributeName);
			if (specified.size()>0)
				return null;
			return re;
		}
		
		// second case: no Annotations
		Supplier[] re;
		int index;
		
		// special case: first argument is parent object for inner classes
		if (types.length==attributes.size()+1 && o!=null && types[0].isAssignableFrom(o.getClass())) {
			re = new Supplier[types.length];
			re[0] = ()->o;
			index = 1;
		} else if (types.length!=attributes.size())
			return null;
		else {
			re = new Supplier[types.length];
			index = 0;
		}
		
		Iterator<String> it = attributes.keySet().iterator();
		while (it.hasNext()) {
			String name = it.next();
			String value = attributes.get(name);
			re[index] = determineSupplier(types[index], o, name, value, "", "", attributes,children,context);
			if (re[index]==null) return null;
			index++;
		}
		return re;
		
	}


	private Supplier determineSupplier(Class<?> type, Object parent, String name, String value, String parserName, String defaultValue, LinkedHashMap<String, String> attributes, ArrayList<OmlNode> children, HashMap<String,Object> context) {
		if (value!=null) {
			
			// first priority: inlined
			OmlNode inlined = getChildById(children,value);	
			if (inlined!=null && type.isAssignableFrom(ClassPathCache.getInstance().getClass(inlined.getName()))) 
				return new InlinedParameterValue(inlined, parent, context);
			
			// second: in context
			if (context.containsKey(value) && type.isAssignableFrom(context.get(value).getClass())) 
				return new ContextParameterValue(value,context);
		}
			
		Parser parser = parserName.length()==0?null:ClassPathCache.getInstance().createInstance(parserName);
		if (parser==null && value.contains(":")) {
			int cp = value.indexOf(':');
			String pname = value.substring(0,cp);
			if (ClassPathCache.getInstance().containsName(pname)) {
				Class<Object> pcls = ClassPathCache.getInstance().getClass(pname);
				parser = ParserCache.getInstance().get(pcls);
				if (parser!=null) 
					value = value.substring(cp+1);
			}
		}
		if (parser==null)
			parser = ParserCache.getInstance().get(type);
		
		if (parser==null && type.isArray()) {
			Parser<?> oneParser = ParserCache.getInstance().get(type.getComponentType());
			if (oneParser!=null)
				parser = new ArrayParser(oneParser);
		}
		
		if (parser==null) 
			return null;
				
		// third: parse given value
		if (value!=null) 
			return new ParsedParameterValue(attributes,name, value, parser);
		
		// fourth: parser default value
		if (defaultValue.length()>0) 
			return new DefaultParameterValue(defaultValue, parser);
		
		return null;
	}
	


	private OmlNode getChildById(ArrayList<OmlNode> l, String id) {
		for (OmlNode n : l)
			if (GeneralUtils.isEqual(n.getId(),id))
					return n;
		return null;
	}



	
	private class DefaultParameterValue<T> implements Supplier<T> {

		private String defaultValue;
		private Parser<T> parser;

		public DefaultParameterValue(String defaultValue, Parser<T> parser) {
			this.defaultValue = defaultValue;
			this.parser = parser;
		}

		@Override
		public T get() {
			return parser.apply(defaultValue);
		}
	}
	
	private class ParsedParameterValue<T> implements Supplier<T> {

		public String attributeName;
		public String attributeValue;
		private Parser<T> parser;
		private LinkedHashMap<String, String> attributes;
		
		public ParsedParameterValue(LinkedHashMap<String, String> attributes, String attributeName, String attributeValue, Parser<T> parser) {
			this.attributes = attributes;
			this.attributeName = attributeName;
			this.attributeValue = attributeValue;
			this.parser = parser;
		}
		
		public boolean isParserSpecified() {
			return !attributeValue.equals(attributes.get(attributeName));
		}

		@Override
		public T get() {
			return parser.apply(attributeValue);
		}
	}
	
	private class ContextParameterValue<T> implements Supplier<T> {

		private String attributeValue;
		private HashMap<String, Object> context;

		public ContextParameterValue (String attributeValue, HashMap<String, Object> context) {
			this.attributeValue = attributeValue;
			this.context = context;
		}

		@Override
		public T get() {
			return (T) context.get(attributeValue);
		}
	}
	
	private class InlinedParameterValue<T> implements Supplier<T> {

		private OmlNode node;
		private Object parent;
		private HashMap<String, Object> context;


		public InlinedParameterValue(OmlNode node, Object parent,
				HashMap<String, Object> context) {
			super();
			this.node = node;
			this.parent = parent;
			this.context = context;
		}



		@Override
		public T get() {
			context.put(INLINED_CALL, true);
			T re = execute(node,parent, context);
			context.remove(INLINED_CALL);
			return re;
		}
	}

	
	private abstract class Caller {
		private Supplier[] params;

		public Caller(Supplier[] params) {
			this.params = params;
		}

		public boolean isInlined(OmlNode n) {
			for (Supplier s : params) 
				if (s instanceof InlinedParameterValue && ((InlinedParameterValue)s).node==n)
					return true;
			return false;
		}

		public Object call() throws Exception {
			Object[] p = new Object[params.length];
			for (int i=0; i<p.length; i++)
				p[i] = params[i].get();
			return invoke(p);
		}

		protected abstract Object invoke(Object[] p) throws Exception ;
	}
	
	private class MethodCaller extends Caller {
		private Method method;
		private Object parent;

		public MethodCaller(Method method, Object parent, Supplier[] params) {
			super(params);
			this.method = method;
			this.parent = parent;
		}

		@Override
		protected Object invoke(Object[] p) throws Exception {
			OmlReader.log.log(Level.CONFIG,"Invoking method "+method.getName()+" on "+parent);
			return method.invoke(parent, p);
		}
		
	}
	
	private class PropertyCaller extends Caller {
		private Property property;

		public PropertyCaller(Property property, Supplier param) {
			super(new Supplier[]{param});
			this.property = property;
		}

		@Override
		protected Object invoke(Object[] p) throws Exception {
			OmlReader.log.log(Level.CONFIG,"Invoking setter "+property.getName()+" on "+property.getBean());
			property.setValue(p[0]);
			return null;
		}
		
	}
	
	private class ConstructorCaller extends Caller {
		private Constructor ctor;
		private Object parent;
		private Method adder;

		public ConstructorCaller(Constructor ctor,Supplier[] params, Object parent, Method adder) {
			super(params);
			this.ctor = ctor;
			this.parent = parent;
			this.adder = adder;
		}

		@Override
		protected Object invoke(Object[] p) throws Exception {
			OmlReader.log.log(Level.CONFIG,"Invoking constructor of class "+ctor.getDeclaringClass());
			Object re = ctor.newInstance(p);
			if (adder!=null) {
				OmlReader.log.log(Level.CONFIG,"Invoking adder on "+parent);
				
				adder.invoke(parent, new Object[] {re});
			}
			return re;
		}
		
	}
	
}
