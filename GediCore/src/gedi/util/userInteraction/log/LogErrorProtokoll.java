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

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogErrorProtokoll extends UniqueErrorProtokoll {

	private static final Logger log = Logger.getLogger( ErrorProtokoll.class.getName() );
	private Level level = Level.WARNING;
	
	public LogErrorProtokoll() {
		super(true);
	}
	
	public void setLevel(Level level) {
		this.level = level;
	}

	@Override
	protected void report(String errorType, Object object, String message) {
		log.log(level, message);
	}
	
}
