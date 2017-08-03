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

package gedi.commandline;

import gedi.app.Gedi;
import gedi.commandline.completer.ExpectedTokenCompleter;
import gedi.commandline.completer.ParameterHelper;
import gedi.util.ArrayUtils;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.nashorn.JS;
import gedi.util.userInteraction.progress.ConsoleProgress;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptException;

import jline.console.ConsoleReader;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.history.FileHistory;

public class GediCommandline {
	private static final Logger log = Logger.getLogger( GediCommandline.class.getName() );
	
	public static final String prompt = "\u001B[44m\u001B[1mGedi\u001B[0m> ";
	

	public static <T> void main(String[] args) throws IOException, ScriptException {
		Gedi.startup(true);
		GediCommandline cmd = new GediCommandline();
		cmd.addParam(args);
		cmd.read();
	}

	private CommandLineSession context;
	private char exitOperation = '\0';
	
	public GediCommandline() throws IOException, ScriptException {
		context = new CommandLineSession();
		context.progress = new ConsoleProgress();
		
		context.js = new JS();
		
		context.js.setReplaceInPlace(true);
		
		context.js.putSystemVariable("js", context.js);
		context.js.putSystemVariable("session", context);
		
		context.prevBuffer = new StringBuilder();
		
		initSignalHandlers(context);
		
		
		context.history = new FileHistory(new File(".ncl.history"));
		context.history.setMaxSize(10000);

		if (new File(".ncl.session").exists()) {
			log.log(Level.INFO, "Loading session");
			context.load();
		}
		
	}
	
	public synchronized void addParam(String name, Object value) {
		context.js.putVariable(name,value);
	}
	
	public synchronized void addParam(String[] args) {
		context.js.addParam(args,false);
	}

	
	public synchronized void addParam(HashMap<String,Object> param) {
		context.js.addParam(param);
	}
	
	public synchronized void setExitOperation(char c) {
		this.exitOperation  = c;
	}
	
	public synchronized void read() throws IOException {
		
		CandidateListCompletionHandler complHandler = new CandidateListCompletionHandler();
		complHandler.setPrintSpaceAfterFullCompletion(false);

		context.reader = new ConsoleReader();
		context.reader.setExpandEvents(false);
//		context.reader.setHandleUserInterrupt(true);
		context.reader.setCompletionHandler(complHandler );
		context.reader.addHelper(new ParameterHelper(context.js));
		context.reader.addCompleter(new ExpectedTokenCompleter(context.js));
		context.reader.setPrompt(prompt);
		context.reader.setHistory(context.history);
		
		context.js.setStdout(context.reader.getOutput());
		context.js.setStderr(context.reader.getOutput());
		
		context.jsThread = new CancelableJSThread(context.js);
		
		String line;
		for (;;) {
			try {
				while ((line = context.reader.readLine()) != null) {
		//			out.println("\u001B[33m======>\u001B[0m\"" + line + "\"");
		//			out.flush();
					if (line.trim().startsWith("//") || line.trim().startsWith("#")) continue;
					
					if (line.endsWith("\\")){
						context.prevBuffer.append(line.substring(0, line.length()-1)+"\n");
						context.reader.setPrompt("\t");
					}
					else if (line.startsWith(".") && context.prevBuffer.length()==0) {
						int space = line.indexOf(' ');
						if (space==-1) space = line.length();
						String cmdName = line.substring(1, space);
						CommandLineCommands cmd = ReflectionUtils.valueOf(CommandLineCommands.class,cmdName);
						if (cmd==null) 
							context.reader.println("Command "+cmdName+" unknown! \n Type .help for a list of commands and\n .help <command> for a usage of the commands!");
						else
							try {
								if (!cmd.exec(context, space<line.length()?line.substring(space+1).trim():""))
									break;
							} catch (Exception e) {
								e.printStackTrace(new PrintWriter(context.js.getStderr()));
//								context.js.getStderr().write(e.getMessage()+"\n");
							}
					} else if (StringUtils.trim(line).length()>0){
						
						if (line.endsWith("_")) {
							context.prevBuffer.append(line.substring(0,line.length()-1)).append("\n");
							context.reader.setPrompt("\t");
						}
						else {
							try {
								
								long nano = System.nanoTime();
								JSResult re = context.jsThread.execute(context.prevBuffer.toString()+line);
								if (re.getException()!=null) throw re.getException();
		
								nano = System.nanoTime()-nano;
								context.js.putSystemVariable("nanos",nano);
								
								
								if (re.getResult()!=null) {
									context.js.putSystemVariable("result",re.getResult());
									re = context.jsThread.execute("echo(result);");
									if (re.getException()!=null) throw re.getException();
								}
								context.lastResult = re;
								context.reader.setPrompt(prompt);
								context.prevBuffer.delete(0, context.prevBuffer.length());
								
							} catch (ScriptException e) {
								if (e.getMessage().contains("found eof")) {
									context.prevBuffer.append(line+"\n");
									context.reader.setPrompt("\t");
								}
								else
									context.js.getStderr().write(e.getMessage()+"\n");
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
						
		
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
			
			char[] exitChars = "hsbcn".toCharArray();
			int read = exitOperation;
			context.reader.setHistoryEnabled(false);
			if (ArrayUtils.find(exitChars, exitOperation)==-1) {
				String readl = "";
				do {
					readl = context.reader.readLine("Save (h)istory, (s)ession, (b)oth, (n)one, or (c)ancel exit? ");
				}
				while (readl!=null && (readl.length()!=1 || -1==ArrayUtils.find(exitChars, readl.charAt(0))));
				read = readl==null?'n':exitChars[ArrayUtils.find(exitChars, readl.charAt(0))];
			}
			context.reader.setHistoryEnabled(true);
			context.reader.flush();
			context.reader.println();
			if (read=='b' || read=='h')
				context.history.flush();
			if (read=='b' || read=='s')
				context.save();
			if (read!='c') break;
			context.reader.setPrompt(prompt);
		}
		
		context.reader = null;
		context.jsThread.shutdown();
	}

	@SuppressWarnings("restriction")
	private static void initSignalHandlers(final CommandLineSession context) {
		sun.misc.Signal.handle(new sun.misc.Signal("INT"), new sun.misc.SignalHandler() {
			@Override
			public void handle(sun.misc.Signal s) {
				if (context.reader==null) System.exit(1);
				try {
					if (context.blockTermHandler) {
						return;
					}
					if (!context.jsThread.isRunning()) {
						context.reader.setPrompt(prompt);
						context.prevBuffer.delete(0,context.prevBuffer.length());
						context.reader.abort();
						context.reader.flush();
						context.reader.getHistory().moveToEnd();
					}
					else {
						if (context.jsThread.cancelCurrent())
							context.reader.println();
					}
				} catch (IOException e) {
				}
			}
		});
		
		
		sun.misc.Signal.handle(new sun.misc.Signal("TSTP"), new sun.misc.SignalHandler() {
			@Override
			public void handle(sun.misc.Signal s) {
				if (context.reader==null) return;
				try {
					context.reader.getTerminal().restore();
				} catch (Exception e) {
					throw new RuntimeException("Could not restore terminal!");
				}
			}
		});
		
		
		sun.misc.Signal.handle(new sun.misc.Signal("CONT"), new sun.misc.SignalHandler() {
			@Override
			public void handle(sun.misc.Signal s) {
				if (context.reader==null) return;
				try {
					context.reader.getTerminal().init();
				} catch (Exception e) {
					throw new RuntimeException("Could not restore terminal!");
				}
			}
		});
	} 

	
}
