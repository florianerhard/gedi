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

package gedi.util.mutable;

import gedi.util.orm.CompilerTool;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MutableFactory {

	
	public static final Logger log = Logger.getLogger( MutableFactory.class.getName() );

	/**
	 * Without that, the type of the generic field is not visible! 
	 * @param cls
	 * @return
	 */
	public static <T> MutableMonad<T> create(Class<T> cls) {
		String name = MutableMonad.class.getName();
		
		try {
			StringBuilder sb = new StringBuilder();
	    	sb.append(name);
	   		sb.append("<");
	   		sb.append(cls.getName());
	    	sb.append(">");
			String fullClsName = generateClassName(name,cls);
			return (MutableMonad<T>) CompilerTool.compileClass(fullClsName, sb.toString(), null, null).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Could not create Mutable!", e);
			throw new RuntimeException("Could not create Mutable!", e);
		}
	}
	
	/**
	 * Without that, the type of the generic field is not visible! 
	 * @param cls
	 * @return
	 */
	public static <T1,T2> MutablePair<T1,T2> create(Class<T1> cls1, Class<T2> cls2) {
		String name = MutablePair.class.getName();
		
		try {
			StringBuilder sb = new StringBuilder();
	    	sb.append(name);
	   		sb.append("<");
	   		sb.append(cls1.getName());
	   		sb.append(",");
	   		sb.append(cls2.getName());
	    	sb.append(">");
			String fullClsName = generateClassName(name,cls1,cls2);
			return (MutablePair<T1,T2>) CompilerTool.compileClass(fullClsName, sb.toString(), null, null).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Could not create Mutable!", e);
			throw new RuntimeException("Could not create Mutable!", e);
		}
	}
	
	
	/**
	 * Without that, the type of the generic field is not visible! 
	 * @param cls
	 * @return
	 */
	public static <T1,T2,T3> MutableTriple<T1,T2,T3> create(Class<T1> cls1, Class<T2> cls2, Class<T3> cls3) {
		String name = MutableTriple.class.getName();
		
		try {
			StringBuilder sb = new StringBuilder();
	    	sb.append(name);
	   		sb.append("<");
	   		sb.append(cls1.getName());
	   		sb.append(",");
	   		sb.append(cls2.getName());
	   		sb.append(",");
	   		sb.append(cls3.getName());
	    	sb.append(">");
			String fullClsName = generateClassName(name,cls1,cls2,cls3);
			return (MutableTriple<T1,T2,T3>) CompilerTool.compileClass(fullClsName, sb.toString(), null, null).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Could not create Mutable!", e);
			throw new RuntimeException("Could not create Mutable!", e);
		}
	}
	
	
	/**
	 * Without that, the type of the generic field is not visible! 
	 * @param cls
	 * @return
	 */
	public static <T1,T2,T3,T4> MutableQuadruple<T1,T2,T3,T4> create(Class<T1> cls1, Class<T2> cls2, Class<T3> cls3, Class<T4> cls4) {
		String name = MutableQuadruple.class.getName();
		
		try {
			StringBuilder sb = new StringBuilder();
	    	sb.append(name);
	   		sb.append("<");
	   		sb.append(cls1.getName());
	   		sb.append(",");
	   		sb.append(cls2.getName());
	   		sb.append(",");
	   		sb.append(cls3.getName());
	   		sb.append(",");
	   		sb.append(cls4.getName());
	    	sb.append(">");
			String fullClsName = generateClassName(name,cls1,cls2,cls3,cls4);
			return (MutableQuadruple<T1,T2,T3,T4>) CompilerTool.compileClass(fullClsName, sb.toString(), null, null).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Could not create Mutable!", e);
			throw new RuntimeException("Could not create Mutable!", e);
		}
	}
	
	/**
	 * Without that, the type of the generic field is not visible! 
	 * @param cls
	 * @return
	 */
	public static <T1,T2,T3,T4,T5> MutableQuintuple<T1,T2,T3,T4,T5> create(Class<T1> cls1, Class<T2> cls2, Class<T3> cls3, Class<T4> cls4, Class<T5> cls5) {
		String name = MutableQuintuple.class.getName();
		
		try {
			StringBuilder sb = new StringBuilder();
	    	sb.append(name);
	   		sb.append("<");
	   		sb.append(cls1.getName());
	   		sb.append(",");
	   		sb.append(cls2.getName());
	   		sb.append(",");
	   		sb.append(cls3.getName());
	   		sb.append(",");
	   		sb.append(cls4.getName());
	   		sb.append(",");
	   		sb.append(cls5.getName());
	    	sb.append(">");
			String fullClsName = generateClassName(name,cls1,cls2,cls3,cls4,cls5);
			return (MutableQuintuple<T1,T2,T3,T4,T5>) CompilerTool.compileClass(fullClsName, sb.toString(), null, null).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Could not create Mutable!", e);
			throw new RuntimeException("Could not create Mutable!", e);
		}
	}
	
	/**
	 * Without that, the type of the generic field is not visible! 
	 * @param cls
	 * @return
	 */
	public static <T1,T2,T3,T4,T5,T6> MutableSextuple<T1,T2,T3,T4,T5,T6> create(Class<T1> cls1, Class<T2> cls2, Class<T3> cls3, Class<T4> cls4, Class<T5> cls5, Class<T6> cls6) {
		String name = MutableSextuple.class.getName();
		
		try {
			StringBuilder sb = new StringBuilder();
	    	sb.append(name);
	   		sb.append("<");
	   		sb.append(cls1.getName());
	   		sb.append(",");
	   		sb.append(cls2.getName());
	   		sb.append(",");
	   		sb.append(cls3.getName());
	   		sb.append(",");
	   		sb.append(cls4.getName());
	   		sb.append(",");
	   		sb.append(cls5.getName());
	   		sb.append(",");
	   		sb.append(cls6.getName());
	    	sb.append(">");
			String fullClsName = generateClassName(name,cls1,cls2,cls3,cls4,cls5,cls6);
			return (MutableSextuple<T1,T2,T3,T4,T5,T6>) CompilerTool.compileClass(fullClsName, sb.toString(), null, null).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Could not create Mutable!", e);
			throw new RuntimeException("Could not create Mutable!", e);
		}
	}
	
	/**
	 * Without that, the type of the generic field is not visible! 
	 * @param cls
	 * @return
	 */
	public static <T1,T2,T3,T4,T5,T6,T7> MutableHeptuple<T1,T2,T3,T4,T5,T6,T7> create(Class<T1> cls1, Class<T2> cls2, Class<T3> cls3, Class<T4> cls4, Class<T5> cls5, Class<T6> cls6, Class<T7> cls7) {
		String name = MutableHeptuple.class.getName();
		
		try {
			StringBuilder sb = new StringBuilder();
	    	sb.append(name);
	   		sb.append("<");
	   		sb.append(cls1.getName());
	   		sb.append(",");
	   		sb.append(cls2.getName());
	   		sb.append(",");
	   		sb.append(cls3.getName());
	   		sb.append(",");
	   		sb.append(cls4.getName());
	   		sb.append(",");
	   		sb.append(cls5.getName());
	   		sb.append(",");
	   		sb.append(cls6.getName());
	   		sb.append(",");
	   		sb.append(cls7.getName());
	    	sb.append(">");
			String fullClsName = generateClassName(name,cls1,cls2,cls3,cls4,cls5,cls6,cls7);
			return (MutableHeptuple<T1,T2,T3,T4,T5,T6,T7>) CompilerTool.compileClass(fullClsName, sb.toString(), null, null).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Could not create Mutable!", e);
			throw new RuntimeException("Could not create Mutable!", e);
		}
	}
	
	/**
	 * Without that, the type of the generic field is not visible! 
	 * @param cls
	 * @return
	 */
	public static <T1,T2,T3,T4,T5,T6,T7,T8> MutableOctuple<T1,T2,T3,T4,T5,T6,T7,T8> create(Class<T1> cls1, Class<T2> cls2, Class<T3> cls3, Class<T4> cls4, Class<T5> cls5, Class<T6> cls6, Class<T7> cls7, Class<T8> cls8) {
		String name = MutableOctuple.class.getName();
		
		try {
			StringBuilder sb = new StringBuilder();
	    	sb.append(name);
	   		sb.append("<");
	   		sb.append(cls1.getName());
	   		sb.append(",");
	   		sb.append(cls2.getName());
	   		sb.append(",");
	   		sb.append(cls3.getName());
	   		sb.append(",");
	   		sb.append(cls4.getName());
	   		sb.append(",");
	   		sb.append(cls5.getName());
	   		sb.append(",");
	   		sb.append(cls6.getName());
	   		sb.append(",");
	   		sb.append(cls7.getName());
	   		sb.append(",");
	   		sb.append(cls8.getName());
	    	sb.append(">");
			String fullClsName = generateClassName(name,cls1,cls2,cls3,cls4,cls5,cls6,cls7,cls8);
			return (MutableOctuple<T1,T2,T3,T4,T5,T6,T7,T8>) CompilerTool.compileClass(fullClsName, sb.toString(), null, null).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Could not create Mutable!", e);
			throw new RuntimeException("Could not create Mutable!", e);
		}
	}
	
	
	
	private static String generateClassName(String name, Object...key) {
		String prefix = "gedi.generated";
        String base =
            prefix + "."+name+"$$" + 
            Integer.toHexString(Arrays.hashCode(key));
        String attempt = base;
        int index = 2;
        
        try {
        	while (true) {
	        Class.forName(attempt);
	        attempt = base + "_" + index++;
        	}
        } catch (ClassNotFoundException e) {
        	return attempt;
        }
	}
}
