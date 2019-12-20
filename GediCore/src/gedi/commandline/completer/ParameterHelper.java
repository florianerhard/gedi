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
package gedi.commandline.completer;

import gedi.app.Config;
import gedi.app.classpath.ClassPathCache;
import gedi.util.StringUtils;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.nashorn.JS;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.URISyntaxException;

import jline.console.helper.Helper;
import net.htmlparser.jericho.Source;

public class ParameterHelper implements Helper {

	private JS js;
	private URI[] javadocs;
	
	
	public ParameterHelper(JS js) throws IOException {
		this.js = js;
		
		LineOrientedFile file = new LineOrientedFile(Config.getInstance().getConfigFolder()+"/javadoc.locations");
		if (file.exists()) {
			javadocs = file.lineIterator("#").filter(l->l.length()>0).map(l->{
				try {
					return new URI(l);
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			}).toArray(new URI[0]);
			
			
		}
		
	}
	
	
	@Override
	public String help(String buffer, int cursor) {
		int parPos = 0;
		if (buffer==null || cursor==0)
			return null;
		else if (buffer.charAt(cursor-1)!='(') {
			if (buffer.charAt(cursor-1)==',') {
				MaskedCharSequence chr = MaskedCharSequence.maskQuotes(buffer.substring(0, cursor-1), ' ');
				chr = MaskedCharSequence.maskRightToLeft(chr, ' ', new char[] {'('}, new char[] {')'});
				int ind = chr.toString().lastIndexOf('(');
				if (ind==-1)
					return null;
				cursor = ind+1;
				parPos = StringUtils.countChar(chr, ',')+1;
			}
			else
				return null;
		}
		cursor--;
		
		int start = StringUtils.getLongestSuffixPosition(buffer.substring(0,cursor),s->StringUtils.isJavaIdentifier(s));
		if (start==-1) start = 0;
		
		String prefix = buffer.substring(start,cursor);
		if (!StringUtils.isJavaIdentifier(prefix))
			return null;
		
		int uParpos = parPos;
		
		// test chain of invocations
		if (start>0 && buffer.charAt(start-1)=='.')  {
			
			TokenType type = TokenType.infer(js, buffer,start-2);
			if (type!=null) {
				StringBuilder sb = new StringBuilder();
				type.handle(prefix,
						m->{
							if (m.getName().equals(prefix)) 
								appendHelp(sb,uParpos, m.getDeclaringClass(),m.getName(),m.getParameters(),m.getReturnType());
						},
						f->{},
						o->{});
				if (sb.length()==0) return null;
				sb.deleteCharAt(sb.length()-1);
				return sb.toString();
			}
			return null;
		}
		
//		if (start>0 && buffer.charAt(start-1)=='.')  {
//			int start2 = start-2;
//			for (; start2>0 && StringUtils.isJavaIdentifier(buffer.substring(start2-1,start-1)); start2--);
//			String on = buffer.substring(start2,start-1);
//			if (StringUtils.isJavaIdentifier(on)) {
//				return handleMember(on,prefix);
//			}
//			return null;
//		}
	
		if (start>=4 && buffer.substring(start-4, start).equals("new ")) {
			return handleCtor(prefix, parPos);
		}
		
		return null;
		
	}


//	private String handleMember(String on, String method) {
//		if (ClassPathCache.getInstance().containsName(on)) {
//			StringBuilder sb = new StringBuilder();
//			Class<?> cls = ClassPathCache.getInstance().getClass(on);
//			for (Method m : cls.getMethods()) {
//				if (Modifier.isStatic(m.getModifiers()) &&
//						Modifier.isPublic(m.getModifiers()) &&  m.getName().equals(method))
//					appendHelp(sb,m.getParameters(),m.getReturnType());
//			}
//			return sb.length()>0?sb.toString():null;
//		} else if (js.getVariables(true).containsKey(on)) {
//			StringBuilder sb = new StringBuilder();
//			Object val = js.getVariables(true).get(on);
//			Class<?> cls = val.getClass();
//			for (Method m : cls.getMethods()) {
//				if (!Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()) &&  m.getName().startsWith(method))
//					appendHelp(sb,m.getParameters(),m.getReturnType());
//			}
//			return sb.length()>0?sb.toString():null;
//		}
//		return null;
//	}

	
	private String handleCtor(String clsname, int parPos) {
		if (ClassPathCache.getInstance().containsName(clsname)) {
			StringBuilder sb = new StringBuilder();
			Class<?> cls = ClassPathCache.getInstance().getClass(clsname);
			for (Constructor<?> c : cls.getConstructors()) {
				if (Modifier.isPublic(c.getModifiers()))
					appendHelp(sb,parPos,cls,cls.getSimpleName(),c.getParameters(),cls);
			}
			return sb.length()>0?sb.toString():null;
		}
		return null;
	}


	private void appendHelp(StringBuilder sb, int parPos, Class<?> cls, String methodName, Parameter[] paras, Class<?> returnType) {
		sb.append("(");
		for (int i = 0; i < paras.length; i++) {
			if (i>0) sb.append(", ");
			if (i==parPos)
				sb.append("<");
			if (paras[i].isNamePresent()) {
				sb.append(paras[i].getType().getSimpleName()).append(" ").append(paras[i].getName());
			} else
				sb.append(paras[i].getType().getSimpleName());
			if (i==parPos)
				sb.append(">");
		}
		sb.append(") -> ");
		sb.append(returnType.getSimpleName());
		sb.append("\n");
		
//		// check javadocs
//		String html = loadHtml(cls);
//	
//		if (html!=null) {
//			String help = extractHelp(html, methodName, paras);
//			if (help!=null)
//				sb.append(help);
//		}
	}

	private String extractHelp(String html,String methodName, Parameter[] paras) {
		StringBuilder parmlist = new StringBuilder();
		for (Parameter p : paras) {
			parmlist.append(p.getParameterizedType().getTypeName()).append("-");
		}
		String search = "<a name=\""+methodName+"-"+parmlist+"\">";
		int pos = html.indexOf(search);
		if (pos==-1) return null;
		
		html = html.substring(pos);
		
		search = "</ul>";
		pos = html.indexOf(search);
		if (pos==-1) return null;
		
		html = html.substring(0,pos+search.length());
		
		
		
		net.htmlparser.jericho.Config.LoggerProvider = net.htmlparser.jericho.LoggerProvider.DISABLED;
		return new Source(html).getRenderer().toString();
	}

	private String loadHtml(Class<?> cls) {
		for (URI jd : javadocs) {
			try {
				return  new LineIterator(jd.resolve(cls.getName().replace('.','/')+".html").toURL().openStream()).concat("\n");
			} catch (IOException e) {
			}
		}
		return null;
	}
	


}
