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

package gedi.util.orm;

import gedi.app.classpath.ClassPathCache;
import gedi.util.FileUtils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;



public class CompilerTool {

    /**
     * The "com.sun.tools.javac.Main" (if available).
     */
    static final JavaCompiler JAVA_COMPILER;

    private static final Class<?> JAVAC_SUN;

    private static final String COMPILE_DIR =
            System.getProperty("java.io.tmpdir", ".");


    static {
        JavaCompiler c;
        try {
            c = ToolProvider.getSystemJavaCompiler();
        } catch (Exception e) {
            // ignore
            c = null;
        }
        JAVA_COMPILER = c;
        Class<?> clazz;
        try {
            clazz = Class.forName("com.sun.tools.javac.Main");
        } catch (Exception e) {
            clazz = null;
        }
        JAVAC_SUN = clazz;
    }

    public static Class<?> compileClass(String packageAndClassName, String extendsFrom, String implementsFrom, final String classBody) {
        ClassLoader classLoader = new ClassLoader(CompilerTool.class.getClassLoader()) {

            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                String packageName = null;
                int idx = name.lastIndexOf('.');
                String className;
                if (idx >= 0) {
                    packageName = name.substring(0, idx);
                    className = name.substring(idx + 1);
                } else {
                    className = name;
                }
                String s = getCompleteSourceCode(packageName, className, extendsFrom, implementsFrom, classBody);
                Class<?> classInstance;
				if (JAVA_COMPILER != null) {
                    classInstance = javaxToolsJavac(packageName, className, s);
                } else {
                    byte[] data = javacCompile(packageName, className, s);
                    if (data == null) {
                        classInstance = findSystemClass(name);
                    } else {
                        classInstance = defineClass(name, data, 0, data.length);
                    }
                }
                return classInstance;
            }
        };
        try {
			return classLoader.loadClass(packageAndClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Could not build class!",e);
		}
    }


    /**
     * Get the complete source code (including package name, imports, and so
     * on).
     *
     * @param packageName the package name
     * @param className the class name
     * @param source the (possibly shortened) source code
     * @return the full source code
     */
    static String getCompleteSourceCode(String packageName, String className,
    		String extendsFrom, String implementsFrom, final String classBody) {
        StringBuffer buff = new StringBuffer();
        if (packageName != null) {
            buff.append("package ").append(packageName).append(";\n");
        }
        
        buff.append("public class ").append(className);
        if (extendsFrom!=null && extendsFrom.length()>0)
        	buff.append(" extends ").append(extendsFrom);
        if (implementsFrom!=null && implementsFrom.length()>0)
        	buff.append(" implements ").append(implementsFrom);
        buff.append(" {\n");
        
        if (classBody!=null && classBody.length()>0) {
	        ClassPathCache classes = ClassPathCache.getInstance();
			Pattern word = Pattern.compile("[A-Z][A-Za-z0-9_]+");
			Matcher m = word.matcher(classBody);
			while (m.find()) {
				m.appendReplacement(buff, "");
				String rep = m.group();
				if (isStart(buff) && 
						classes.containsName(m.group())) 
					buff.append(classes.getFullName(m.group()));
				else
					buff.append(rep);
			}
			m.appendTail(buff);
        }
			
		buff.append("}");
        
        return buff.toString();
    }
    
    private static boolean isStart(StringBuffer sb) {
		if (sb.length()==0) return true;
		return !Character.isDigit(sb.charAt(sb.length()-1)) && 
		!Character.isLowerCase(sb.charAt(sb.length()-1)) && 
		sb.charAt(sb.length()-1)!='.';
	}

    /**
     * Compile the given class. This method tries to use the class
     * "com.sun.tools.javac.Main" if available. If not, it tries to run "javac"
     * in a separate process.
     *
     * @param packageName the package name
     * @param className the class name
     * @param source the source code
     * @return the class file
     */
    static byte[] javacCompile(String packageName, String className, String source) {
        File dir = new File(COMPILE_DIR);
        if (packageName != null) {
            dir = new File(dir, packageName.replace('.', '/'));
            dir.mkdirs();
        }
        File javaFile = new File(dir, className + ".java");
        File classFile = new File(dir, className + ".class");
        try {
            Writer out = new BufferedWriter(new FileWriter(javaFile));
            classFile.delete();
            out.write(source);
            out.close();
            if (JAVAC_SUN != null) {
                javacSun(javaFile);
            } else {
                javacProcess(javaFile);
            }
            byte[] data = new byte[(int) classFile.length()];
            DataInputStream in = new DataInputStream(new FileInputStream(classFile));
            in.readFully(data);
            in.close();
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            javaFile.delete();
            classFile.delete();
        }
    }

    /**
     * Compile using the standard java compiler.
     *
     * @param packageName the package name
     * @param className the class name
     * @param source the source code
     * @return the class
     */
    static Class<?> javaxToolsJavac(String packageName, String className, String source) {
        String fullClassName = packageName + "." + className;
        StringWriter writer = new StringWriter();
        JavaFileManager fileManager = new
                ClassFileManager(JAVA_COMPILER
                    .getStandardFileManager(null, null, null));
        ArrayList<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();
        compilationUnits.add(new StringJavaFileObject(fullClassName, source));
        JAVA_COMPILER.getTask(writer, fileManager, null, null,
                null, compilationUnits).call();
        String err = writer.toString();
        throwSyntaxError(err);
        try {
            return fileManager.getClassLoader(null).loadClass(fullClassName);
        } catch (ClassNotFoundException e) {
        	throw new RuntimeException(e);
        }
    }

    private static void javacProcess(File javaFile) {
        exec("javac",
                "-sourcepath", COMPILE_DIR,
                "-d", COMPILE_DIR,
                "-encoding", "UTF-8",
                javaFile.getAbsolutePath());
    }

    private static int exec(String... args) {
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        try {
            ProcessBuilder builder = new ProcessBuilder();
            // The javac executable allows some of it's flags
            // to be smuggled in via environment variables.
            // But if it sees those flags, it will write out a message
            // to stderr, which messes up our parsing of the output.
            builder.environment().remove("JAVA_TOOL_OPTIONS");
            builder.command(args);

            Process p = builder.start();
            copyInThread(p.getInputStream(), buff);
            copyInThread(p.getErrorStream(), buff);
            p.waitFor();
            String err = new String(buff.toByteArray(), Charset.forName("UTF-8"));
            throwSyntaxError(err);
            return p.exitValue();
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }

    private static void copyInThread(final InputStream in, final OutputStream out) {
    	Thread thread = new Thread() {
    		@Override
    		public void run() {
    			try {
           		 long length = Long.MAX_VALUE;
                    int len = 4 * 1024;
                    byte[] buffer = new byte[len];
                    while (length > 0) {
                        len = in.read(buffer, 0, len);
                        if (len < 0) {
                            break;
                        }
                        if (out != null) {
                            out.write(buffer, 0, len);
                        }
                        length -= len;
                        len = (int) Math.min(length, 4 * 1024);
                    }
                } catch (Exception e) {
                	throw new RuntimeException(e);
                }
    		}
    	};
        thread.setDaemon(true);
        thread.start();
    }

    private static void javacSun(File javaFile) {
        PrintStream old = System.err;
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        PrintStream temp = new PrintStream(buff);
        try {
            System.setErr(temp);
            Method compile;
            compile = JAVAC_SUN.getMethod("compile", String[].class);
            Object javac = JAVAC_SUN.newInstance();
            compile.invoke(javac, (Object) new String[] {
                    "-sourcepath", COMPILE_DIR,
                    "-d", COMPILE_DIR,
                    "-encoding", "UTF-8",
                    javaFile.getAbsolutePath() });
            String err = new String(buff.toByteArray(), Charset.forName("UTF-8"));
            throwSyntaxError(err);
        } catch (Exception e) {
        	throw new RuntimeException(e);
        } finally {
            System.setErr(old);
        }
    }

    private static void throwSyntaxError(String err) {
        if (err.startsWith("Note:")) {
            // unchecked or unsafe operations - just a warning
        } else if (err.length() > 0) {
            err = err.replaceAll(COMPILE_DIR, "");
            throw new RuntimeException(err);
        }
    }



    /**
     * An in-memory java source file object.
     */
    static class StringJavaFileObject extends SimpleJavaFileObject {

        private final String sourceCode;

        public StringJavaFileObject(String className, String sourceCode) {
            super(URI.create("string:///" + className.replace('.', '/')
                + Kind.SOURCE.extension), Kind.SOURCE);
            this.sourceCode = sourceCode;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }

    }

    /**
     * An in-memory java class object.
     */
    static class JavaClassObject extends SimpleJavaFileObject {

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        public JavaClassObject(String name, Kind kind) {
            super(URI.create("string:///" + name.replace('.', '/')
                + kind.extension), kind);
        }

        public byte[] getBytes() {
            return out.toByteArray();
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return out;
        }
    }

    /**
     * An in-memory class file manager.
     */
    static class ClassFileManager extends
            ForwardingJavaFileManager<StandardJavaFileManager> {

        /**
         * The class (only one class is kept).
         */
        JavaClassObject classObject;

        public ClassFileManager(StandardJavaFileManager standardManager) {
            super(standardManager);
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            return new SecureClassLoader() {
                @Override
                protected Class<?> findClass(String name)
                        throws ClassNotFoundException {
                    byte[] bytes = classObject.getBytes();
                    return super.defineClass(name, bytes, 0,
                            bytes.length);
                }
            };
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location,
                String className, Kind kind, FileObject sibling) throws IOException {
            classObject = new JavaClassObject(className, kind);
            return classObject;
        }
    }

}

