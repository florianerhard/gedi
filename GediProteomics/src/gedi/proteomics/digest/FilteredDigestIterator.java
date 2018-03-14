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
package gedi.proteomics.digest;

import java.util.function.Predicate;


class FilteredDigestIterator implements DigestIterator {

	/**
	 * 
	 */
	private final Predicate<String> predicate;
	private String next;
	private DigestIterator it;
	
	public FilteredDigestIterator(Predicate<String> predicate, DigestIterator it) {
		this.predicate = predicate;
		this.it = it;
	}

	@Override
	public boolean hasNext() {
		lookAhead();
		return next!=null;
	}

	@Override
	public String next() {
		lookAhead();
		String re = next;
		next = null;
		return re;
	}
	
	@Override
	public int getMissed() {
		return it.getMissed();
	}

	@Override
	public void remove() {
	}
	
	private void lookAhead() {
		if (next==null && it.hasNext()) {
			next = it.next();
			boolean valid = predicate.test(next);
			while (!valid && it.hasNext()) 
				valid = predicate.test(next = it.next());
			if (!valid) next=null;
			
		}
	}

	@Override
	public int getStartPosition() {
		return it.getStartPosition();
	}

	@Override
	public int getEndPosition() {
		return it.getEndPosition();
	}
	
}