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

import gedi.app.classpath.ClassPathCache;
import gedi.commandline.CommandLineCommands;
import gedi.util.StringUtils;
import gedi.util.datastructure.tree.Trie;
import gedi.util.nashorn.JS;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import jline.console.completer.Completer;
import jline.console.completer.EnumCompleter;
import jline.console.completer.FileNameCompleter;

public class ExpectedTokenCompleter implements Completer {

	private Trie<String> classTrie;
	private JS js;
	
	private FileNameCompleter fileCompleter = new FileNameCompleter();
	private EnumCompleter commandsCompleter = new EnumCompleter(CommandLineCommands.class);
	
	public ExpectedTokenCompleter(JS js) {
		this.js = js;
		classTrie = new Trie<String>();
		classTrie.putAll(ClassPathCache.getInstance().getNameToFullNameMap());
		classTrie.sort();
	}
	
	
	@Override
	public int complete(String buffer, int cursor, List<CharSequence> candidates) {
		if (buffer==null || cursor==0) {
			return -1;
		}
		
		if (buffer.startsWith(".help ")) 
			return commandsCompleter.complete(buffer.substring(6), cursor-1, candidates)+6;
		if (buffer.startsWith(".") && buffer.substring(0, cursor).indexOf(' ')==-1) 
			return commandsCompleter.complete(buffer.substring(1), cursor-1, candidates)+1;

		int quotes = Math.max(buffer.substring(0, cursor).lastIndexOf('\''), buffer.substring(0, cursor).lastIndexOf('"'));
		if (quotes!=-1) {
			quotes++;
			int size = candidates.size();
			int pos = fileCompleter.complete(buffer.substring(quotes, cursor), cursor-quotes, candidates);
			if (size!=candidates.size()) return pos+quotes;
		}
		
		int start = StringUtils.getLongestSuffixPosition(buffer.substring(0,cursor),s->StringUtils.isJavaIdentifier(s));
		if (start==-1) start = cursor-1;

//		int start = cursor-1;
//		for (; start>0 && StringUtils.isJavaIdentifier(buffer.substring(start-1,cursor)); start--);

		
		String prefix = buffer.substring(start,cursor);
		if (prefix.equals(".")) {
			start++;
			prefix = "";
		} else
			if (!StringUtils.isJavaIdentifier(prefix))
				return -1;
		
		
		// test chain of invocations
		if (start>0 && buffer.charAt(start-1)=='.')  {
			
			TokenType type = TokenType.infer(js, buffer,start-2);
			if (type!=null) {
				TreeSet<String> re = new TreeSet<String>();
				type.handle(prefix,m->re.add(m.getName()), f->re.add(f.getName()), o->re.add(o));
				candidates.addAll(re);
				
				return start;
			}
			return -1;
		}
	
		
		
		if (Character.isLowerCase(prefix.charAt(0))) {
			handleVariable(prefix,candidates);
			return start;
		} else {
			handleClass(prefix,candidates);
			return start;
		}
		
	}

	

	private void handleClass(String prefix, List<CharSequence> candidates) {
		for (String cand : classTrie.getKeysByPrefix(prefix, new ArrayList<String>())) {
			if (!cand.substring(prefix.length()).contains("$"))
				candidates.add(cand);
		}
	}

	private void handleVariable(String prefix, List<CharSequence> candidates) {
		for (String v : js.getVariables(true).keySet()) {
			if (v.startsWith(prefix))
				candidates.add(v);
		}
	}
	
}
