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
package gedi.core.data.annotation;

import java.util.ArrayList;

import gedi.core.reference.ReferenceSequence;

public class CompositeReferenceSequenceLengthProvider extends ArrayList<ReferenceSequenceLengthProvider> implements ReferenceSequenceLengthProvider {

	public int getLength(String name) {
		int re = 0;
		for (int i=0; i<size(); i++) {
			int l = get(i).getLength(name);
			if (l>0) re = Math.max(re, l);
			else if (re<=0) re = Math.min(re, l);
		}
		return re;
	}
	
}
