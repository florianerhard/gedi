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
package gedi.util.parsing;

import gedi.core.data.reads.ReadCountMode;
import gedi.util.PaintUtils;

import java.awt.Color;
import java.awt.Paint;

public class ReadCountModeParser implements Parser<ReadCountMode> {

	@Override
	public ReadCountMode apply(String t) {
		return ReadCountMode.valueOf(t);
	}

	@Override
	public Class<ReadCountMode> getParsedType() {
		return ReadCountMode.class;
	}

}
