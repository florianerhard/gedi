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

package gedi.util.userInteraction.log;

import gedi.util.mutable.MutablePair;

import java.util.HashSet;

public abstract class UniqueErrorProtokoll implements ErrorProtokoll {

	private HashSet<MutablePair<String,Object>> occurred = new HashSet<MutablePair<String,Object>>();
	private boolean makeUnique;
	
	public UniqueErrorProtokoll(boolean makeUnique) {
		this.makeUnique = makeUnique;
	}
	
	@Override
	public void addError(String errorType, Object object, String message) {
		if (!makeUnique || occurred.add(new MutablePair<String, Object>(errorType,object)))
			report(errorType,object,message);
		
	}
	
	public boolean isMakeUnique() {
		return makeUnique;
	}

	public void setMakeUnique(boolean makeUnique) {
		this.makeUnique = makeUnique;
	}

	protected abstract void report(String errorType, Object object, String message) ;

}
