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

import gedi.app.classpath.ClassPathCache;
import gedi.app.classpath.ClassPathFactory;
import gedi.app.extension.ExtensionContext;
import gedi.core.workspace.action.WorkspaceItemActionExtensionPoint;
import gedi.fx.FX;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.r.RDataWriter;
import gedi.util.r.RProcess;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TreeSet;

import jline.console.history.History.Entry;

public enum CommandLineCommands {
	
	
	exit{
		@Override
		public boolean exec(CommandLineSession context, String param) {
			return false;
		}

		@Override
		public String usage() {
			return "Exit command line";
		}
		
	},
	
	quit {
		@Override
		public boolean exec(CommandLineSession context, String param) {
			return false;
		}

		@Override
		public String usage() {
			return "Exit command line";
		}
		
	},
	
	cls {
		@Override
		public boolean exec(CommandLineSession context, String param) throws Exception {
			context.reader.clearScreen();
			return true;
		}
		@Override
		public String usage() {
			return "Clear the console";
		}
	},
	
	time {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			long nanos = (Long) context.js.getVariables(true).get("nanos");
			context.reader.println(StringUtils.getHumanReadableTimespanNano(nanos));
			return true;
		}

		@Override
		public String usage() {
			return "Displays the time spent on executing the last command.";
		}
	},
	
	out {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			
			if (context.js.getStdout()!=context.reader.getOutput())
				context.js.getStdout().close();
			
			if (param.length()>0) {
				Writer out = new BufferedWriter(new FileWriter(param));
				context.js.setStdout(out);
			} else {
				PrintWriter out = new PrintWriter(context.reader.getOutput());
				context.js.setStdout(out);
			}
			return true;
		}

		@Override
		public String usage() {
			return "Redirects the output stream into a file (param) or stdout (no param).";
		}
	},
	
	err {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			
			if (context.js.getStderr()!=context.reader.getOutput())
				context.js.getStderr().close();
			
			if (param.length()>0) {
				Writer out = new BufferedWriter(new FileWriter(param));
				context.js.setStderr(out);
			} else {
				PrintWriter out = new PrintWriter(context.reader.getOutput());
				context.js.setStderr(out);
			}
			return true;
		}

		@Override
		public String usage() {
			return "Redirects the error stream into a file (param) or stdout (no param).";
		}
	},
		
	action {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			
			Object[] params = EI.wrap(StringUtils.split(param, ' ')).map(v->context.js.getVariable(v)).toArray();
			
			
			ExtensionContext econtext = new ExtensionContext();
			for (int i=1; i<params.length; i++)
				econtext.add(params[i]);
			WorkspaceItemActionExtensionPoint.getInstance().get(econtext, params[0].getClass()).accept(params[0]);
			
			return true;
		}

		@Override
		public String usage() {
			return "Executes an action on an object with given parameters (as if clicked in the main window).";
		}
	},
	
	exec {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			
			context.reader.getTerminal().restore();
			try {
				ProcessBuilder b = new ProcessBuilder();
				b.inheritIO();
				b.command(StringUtils.split(param, ' '));
				Process p = b.start();
				p.waitFor();
			} finally {
				context.reader.getTerminal().init();
			}
			
			return true;
		}

		@Override
		public String usage() {
			return "Executes a command.";
		}
	},
	
	
	sh {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			
			CommandLineUtils.shellProcess(context, param);
			return true;
		}

		@Override
		public String usage() {
			return "Executes a command in a shell (interactive mode).";
		}
		
		
	},

	R {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			
			if (param.trim().length()>0) {
				RDataWriter setter = context.getRProcess().startSetting();
				for (String n : param.split("\\s+"))
					if (n.length()>0)
						setter.write(n,(Object)context.js.getVariable(n));
				setter.finish();
			}
			
			context.blockTermHandler = true;
			context.getRProcess().read();
			context.blockTermHandler = false;
			context.reader.println();
			
			return true;
		}

		@Override
		public String usage() {
			return "Switch to R, switch back with Ctrl-J; specify variables to transfer";
		}
		
		
	},
	

	gc {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			
			Runtime.getRuntime().gc();
			return true;
		}

		@Override
		public String usage() {
			return "Run garbage collection.";
		}
		
		
	},
	
	
	history {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			
			LinkedList<CharSequence> hist = new LinkedList<CharSequence>();
			int c = param.length()==0?context.reader.getHistory().size():Integer.parseInt(param);
			
			ListIterator<Entry> it = context.reader.getHistory().entries(context.reader.getHistory().index());
			if (it.hasPrevious()) it.previous(); // skip the .history command
			while (it.hasPrevious() && hist.size()<c) 
				hist.addFirst(it.previous().value());
			
			
			CommandLineUtils.shellProcess(context, "less -IR +G", hist);
			
			return true;
		}

		@Override
		public String usage() {
			return "Displays the command history.";
		}
		
		
	},
	
	
	help {

		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			if (param.length()==0) {
				context.reader.println("Available commands:");
				context.reader.printColumns(EI.wrap(CommandLineCommands.values()).map(c->c.name()).list());
			} else {
				CommandLineCommands cmd = ReflectionUtils.valueOf(CommandLineCommands.class, param);
				if (cmd==null) 
					context.reader.println("Command "+param+" unknown!");
				else
					context.reader.println(cmd.usage());
			}
			return true;
		}

		@Override
		public String usage() {
			return "Displays help messages; Without parameter, all commands are listed, with parameter, the usage of the command is printed.";
		}
		
	},
	
	ls {

		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			TreeSet<String> vars = new TreeSet<String>(context.js.getVariables(param.equals("all")).keySet());
			context.reader.printColumns(vars);
			return true;
		}

		@Override
		public String usage() {
			return "Display all variable names.";
		}
		
	},
	
	classpath {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			context.reader.println(ClassPathFactory.getInstance().getJVMClasspath());
			return true;
		}

		@Override
		public String usage() {
			return "Display all elements in the classpath";
		}
	},
	
	script {
		@Override
		public boolean exec(CommandLineSession context, String param) throws Exception {
			context.setScriptFile(StringUtils.trim(param,'\'','"'));
			return true;
		}
		
		@Override
		public String usage() {
			return "Sets the script file to write all commands to (after .s)";
		}
	},
	s {
		@Override
		public boolean exec(CommandLineSession context, String param) throws Exception {
			context.lastCommandToScript();
			return true;
		}
		
		@Override
		public String usage() {
			return "Writes the last command to the script file";
		}
	},
	
	save {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			if (param.length()>0)
				context.save(param);
			else 
				context.save();
			return true;
		}

		@Override
		public String usage() {
			return "Saves the current environment to the given file, or the session file";
		}
		
	},
	
	load {
		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			if (param.length()>0)
				context.load(param);
			else 
				context.load();
			return true;
		}

		@Override
		public String usage() {
			return "Loads an environment from the given file, or the session file";
		}
		
	},
	
	resolve {

		@Override
		public boolean exec(CommandLineSession context, String param)
				throws Exception {
			context.reader.println(ClassPathCache.getInstance().getFullName(param));
			return true;
		}

		@Override
		public String usage() {
			return "Given a class name, resolve its package.";
		}
		
	},
	
	inspect {
		@Override
		public boolean exec(CommandLineSession context, String param) throws IOException {
			Object o = context.js.getVariables(true).get(param);
			if (o==null && ClassPathCache.getInstance().containsName(param))
				o = ClassPathCache.getInstance().getClass(param);
			
			if (o==null) 
				context.reader.print("Not an object: "+param);
			else
				FX.inspect(o);
			return true;
		}

		@Override
		public String usage() {
			return "Open the object inspector window for a given object or class";
		}
	};
	
	
	public abstract boolean exec(CommandLineSession context, String param) throws Exception;
	public abstract String usage();

}
