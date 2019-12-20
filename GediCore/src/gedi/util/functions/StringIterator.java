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
package gedi.util.functions;

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.io.text.HeaderLine;


public interface StringIterator extends ExtendedIterator<String> {

	
	default StringIterator filterString(Predicate<? super String> pred) {
		return new FilteredStringIterator(this, pred);
	}
	
	default StringIterator mapString(UnaryOperator<String> fun) {
		return new MappedStringIterator(this, fun);
	}
	
	default StringIterator startsWith(String prefix) {
		return filterString(s->s.startsWith(prefix));
	}
	
	default StringIterator endsWith(String prefix) {
		return filterString(s->s.endsWith(prefix));
	}
	
	default StringIterator contains(String infex) {
		return filterString(s->s.contains(infex));
	}
	
	default StringIterator matches(String regex) {
		return matches(Pattern.compile(regex));
	}
	
	default StringIterator matches(Pattern pattern) {
		return filterString(s->pattern.matcher(s).find());
	}
	
	default StringIterator match(String regex, int group) {
		return match(Pattern.compile(regex),group);
	}
	
	default StringIterator match(Pattern pattern, int group) {
		return mapString(s->{
			Matcher m = pattern.matcher(s);
			if (m.find()) return m.group(group);
			return null;
		});
	}
	
	default ExtendedIterator<String[]> matchAll(String regex) {
		return matchAll(Pattern.compile(regex));
	}
	
	default ExtendedIterator<String[]> matchAll(Pattern pattern) {
		return map(s->{
			Matcher m = pattern.matcher(s);
			if (m.find()) return EI.seq(1, 1+m.groupCount()).map(i->m.group(i)).toArray(String.class);
			return new String[0];
		});
	}

	default StringIterator match(String regex, String replacement) {
		return replace(Pattern.compile(regex),replacement);
	}
	
	
	
	default StringIterator replace(Pattern pattern, String replacement) {
		return mapString(s->{
			Matcher m = pattern.matcher(s);
			if (m.find()) return m.replaceAll(replacement);
			return s;
		});
	}
	
	default StringIterator trim(char...chars) {
		return new MappedStringIterator(this, s->StringUtils.trim(s, chars));
	}

	default StringIterator trim() {
		return new MappedStringIterator(this, s->StringUtils.trim(s));
	}

	default StringIterator header(HeaderLine h) {
		if (hasNext())
			h.set(next());
		return this;
	}

	
	default IntIterator countChar(char c) {
		return new StringToIntIterator(this, s->StringUtils.countChar(s,c));
	}

	default IntIterator indexOf(char c) {
		return new StringToIntIterator(this, s->s.indexOf(c));
	}

	default IntIterator indexOf(char c, int p) {
		return new StringToIntIterator(this, s->s.indexOf(c,p));
	}

	default IntIterator lastIndexOf(char c) {
		return new StringToIntIterator(this, s->s.lastIndexOf(c));
	}

	default IntIterator lastIndexOf(char c, int p) {
		return new StringToIntIterator(this, s->s.lastIndexOf(c,p));
	}

	default ExtendedIterator<String[]> split(char c) {
		return map(s->StringUtils.split(s, c));
	}

	default ExtendedIterator<String[]> split(String c) {
		return map(s->StringUtils.split(s, c));
	}
	
	default StringIterator substring(int start, int end) {
		return new MappedStringIterator(this,s->s.substring(start, end));
	}
	
	default StringIterator substring(int start) {
		return new MappedStringIterator(this,s->s.substring(start));
	}

	default StringIterator splitField(char c, int field) {
		return new MappedStringIterator(this,s->StringUtils.splitField(s, c, field));
	}
	default StringIterator splitField(String c, int field) {
		return new MappedStringIterator(this,s->StringUtils.splitField(s, c, field));
	}

	default IntIterator parseInt() {
		return new StringToIntIterator(this, s->Integer.parseInt(s));
	}

	default DoubleIterator parseDouble() {
		return new StringToDoubleIterator(this, s->Double.parseDouble(s));
	}



	
	public static class FilteredStringIterator implements StringIterator {
		private Iterator<String> it;
		private Predicate<? super String> predicate;
		private String next;
		private boolean isnull = true;
		
		public FilteredStringIterator(Iterator<String> it, Predicate<? super String> predicate) {
			this.it = it;
			this.predicate = predicate;
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return !isnull;
		}
		@Override
		public String next() {
			lookAhead();
			isnull = true;
			return next;
		}
		private void lookAhead() {
			if (isnull && it.hasNext()) { 
				boolean valid = false;
				isnull = false;
				for (next = it.next(); !(valid = predicate.test(next))&&it.hasNext(); next = it.next());
				if (!valid) isnull = true;
			}
		}
	}
	
	
	
	public static class MappedStringIterator implements StringIterator {
		private Iterator<String> it;
		private UnaryOperator<String> mapper;
		public MappedStringIterator(Iterator<String> it, UnaryOperator<String> mapper) {
			this.it = it;
			this.mapper = mapper;
		}
		@Override
		public boolean hasNext() {
			return it.hasNext();
		}
		@Override
		public String next() {
			return mapper.apply(it.next());
		}
		@Override
		public void remove() {
			it.remove();
		}
		
		public Iterator<String> getParent() {
			return it;
		}
		
	}
	
	
	public static class StringToIntIterator implements IntIterator {
		private Iterator<String> it;
		private ToIntFunction<String> mapper;
		public StringToIntIterator(Iterator<String> it, ToIntFunction<String> mapper) {
			this.it = it;
			this.mapper = mapper;
		}
		@Override
		public boolean hasNext() {
			return it.hasNext();
		}
		@Override
		public int nextInt() {
			return mapper.applyAsInt(it.next());
		}
		@Override
		public void remove() {
			it.remove();
		}
		
		public Iterator<String> getParent() {
			return it;
		}
		
	}
	
	public static class StringToDoubleIterator implements DoubleIterator {
		private Iterator<String> it;
		private ToDoubleFunction<String> mapper;
		public StringToDoubleIterator(Iterator<String> it, ToDoubleFunction<String> mapper) {
			this.it = it;
			this.mapper = mapper;
		}
		@Override
		public boolean hasNext() {
			return it.hasNext();
		}
		@Override
		public double nextDouble() {
			return mapper.applyAsDouble(it.next());
		}
		@Override
		public void remove() {
			it.remove();
		}
		
		public Iterator<String> getParent() {
			return it;
		}
		
	}

	public static StringIterator empty() {
		return new StringIterator() {

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public String next() {
				return null;
			}
			
		};
	}
}
