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

import gedi.util.mutable.MutableMonad;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FilteredSpliterator<T> implements Spliterator<T> {

	private Spliterator<T> ssplit;
	private Predicate<T> filter;
	private MutableMonad<T> box = new MutableMonad<T>();
	
	
	public FilteredSpliterator(Spliterator<T> ssplit, Predicate<T> filter) {
		this.ssplit = ssplit;
		this.filter = filter;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		while (ssplit.tryAdvance(o->{if (filter.test(o)) box.Item=o;}) && box.Item==null);
		if (box.Item==null) return false;
		action.accept(box.Item);
		box.Item=null;
		return true;
	}

	@Override
	public Spliterator<T> trySplit() {
		Spliterator<T> s = ssplit.trySplit();
		if (s==null) return null;
		return new FilteredSpliterator<T>(s,filter);
	}

	@Override
	public long estimateSize() {
		return ssplit.estimateSize();
	}

	@Override
	public int characteristics() {
		return ssplit.characteristics() & ~SIZED;
	}
	
	public Comparator<? super T> getComparator() {
        return ssplit.getComparator();
    }
	
}
