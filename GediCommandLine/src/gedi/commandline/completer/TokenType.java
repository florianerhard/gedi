package gedi.commandline.completer;

import gedi.app.classpath.ClassPathCache;
import gedi.util.StringUtils;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.functions.EI;
import gedi.util.nashorn.JS;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

class TokenType {
	Class<?>[] classes;
	boolean isObject;

	public TokenType(boolean isObject, Class<?>... classes) {
		this.classes = classes;
		this.isObject = isObject;
	}

	public void handle(String prefix, Consumer<Method> ma, Consumer<Field> fa, Consumer<String> other) {
		for (Class cls : classes) {
			for (Method m : cls.getMethods()) {
				if ((Modifier.isStatic(m.getModifiers())!=isObject) 
						&& Modifier.isPublic(m.getModifiers()) &&  m.getName().startsWith(prefix))
					ma.accept(m);
			}
			for (Field f : cls.getFields()) {
				if ((Modifier.isStatic(f.getModifiers())!=isObject) 
						&& Modifier.isPublic(f.getModifiers()) &&  f.getName().startsWith(prefix))
					fa.accept(f);
			}
			if (cls.isArray())
				other.accept("length");
		}
		
	}
	
	public Class<?>[] getMethodsReturnTypes(String name, int paramcount) {
		HashSet<Class<?>> re = new HashSet<Class<?>>();
		for (Class cls : classes)
			for (Method m : cls.getMethods()) {
				if ((Modifier.isStatic(m.getModifiers())!=isObject) 
						&& Modifier.isPublic(m.getModifiers()) &&  m.getName().equals(name)) {
					
					int minParam = m.getParameterCount()==0?0:EI.wrap(m.getParameters()).map(p->p.isVarArgs()?0:1).collect((BinaryOperator<Integer>)((a,b)->a+b));
					int maxParam = m.getParameterCount()==0?0:EI.wrap(m.getParameters()).map(p->p.isVarArgs()?Integer.MAX_VALUE-m.getParameterCount():1).collect((BinaryOperator<Integer>)((a,b)->a+b));
					
					if (paramcount>=minParam && paramcount<=maxParam)
						re.add(m.getReturnType());
				}
			}
		return re.toArray(new Class[0]);
	}
	
	private Class<?>[] getInstanceVariableTypes(String name) {
		HashSet<Class<?>> re = new HashSet<Class<?>>();
		for (Class cls : classes)
			for (Field f : cls.getFields()) {
				if ((Modifier.isStatic(f.getModifiers())!=isObject) 
						&& Modifier.isPublic(f.getModifiers()) &&  f.getName().equals(name)) {
					
					re.add(f.getType());
				}
			}
		return re.toArray(new Class[0]);
	}
	
	@Override
	public String toString() {
		return "TokenType [classes=" + Arrays.toString(classes) + ", isObject="
				+ isObject + "]";
	}

	public static TokenType infer(JS js, String buffer, int lastPositionOfToken) {
		
		if (buffer.charAt(lastPositionOfToken)==')') {
			// ctor or method
			String masked = MaskedCharSequence.maskQuotes(buffer.substring(0, lastPositionOfToken), ' ').toString();
			masked = MaskedCharSequence.maskRightToLeft(masked, ' ', new char[]{'(','['}, new char[]{')',']'}).toString();
			int open = masked.lastIndexOf('(');
			if (open==-1) return null;
			
			int st = scanJavaIdent(buffer, open);
			if (st==-1) return null;
			
			// ctor?
			if (st>=4 && buffer.substring(st-4, st).equals("new "))
				return inferTokenType(buffer.substring(st, open),true);
			
			// method invocation
			if (st>=2 && buffer.substring(st-1, st).equals(".")) {
				TokenType before = infer(js, buffer, st-2);
				if (before==null) return null;
				
				String in = masked.substring(open+1);
				int paramcount = StringUtils.countChar(in,',')+(in.length()>0?1:0);
				Class<?>[] classes = before.getMethodsReturnTypes(buffer.substring(st, open), paramcount);
				return new TokenType(true, classes);
			}
		}
		
		if (buffer.charAt(lastPositionOfToken)==']') {
			// array
			int open = buffer.substring(0, lastPositionOfToken).lastIndexOf('[');
			if (open==-1) return null;
			TokenType before = infer(js, buffer, open-1);
			if (before==null || !before.isObject) return null;
			
			ArrayList<Class<?>> classes = new ArrayList<Class<?>>(); 
			for (Class<?> cls : before.classes) 
				if (cls.isArray()) 
					classes.add(cls.getComponentType());
			if (classes.size()==0) return null;
			
			return new TokenType(true, classes.toArray(new Class[0]));
		}

		// class or variable
		int st = scanJavaIdent(buffer, lastPositionOfToken+1);
		if (st==-1) return null;
		
		String on = buffer.substring(st, lastPositionOfToken+1);
		
		if (st>0 && buffer.charAt(st-1)=='.') {
			// instance variable
			TokenType before = infer(js, buffer, st-2);
			if (before==null) return null;
			
			Class<?>[] classes = before.getInstanceVariableTypes(buffer.substring(st, lastPositionOfToken+1));
			return new TokenType(true, classes);
		}
		
		// class
		if (ClassPathCache.getInstance().containsName(on)) 
			return new TokenType(false, ClassPathCache.getInstance().getClass(on));
		
		// variable
		if (js.getVariables(true).get(on)!=null)
			return new TokenType(true, js.getVariables(true).get(on).getClass());
		
		return null;
		
	}
	
	

	private static int scanJavaIdent(String buffer, int end) {
		int start = StringUtils.getLongestSuffixPosition(buffer.substring(0,end),s->StringUtils.isJavaIdentifier(s));
		if (start==-1) start = 0;
		if (StringUtils.isJavaIdentifier(buffer.substring(start,end)))
			return start;
		return -1;
	}
	
	private static TokenType inferTokenType(String className, boolean isObject) {
		Class<Object> cls = ClassPathCache.getInstance().getClass(className);
		if (cls!=null) return new TokenType(isObject,cls);
		return null;
	}
	
}
	