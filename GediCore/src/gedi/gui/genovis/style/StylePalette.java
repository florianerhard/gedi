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

package gedi.gui.genovis.style;

import gedi.util.gui.ColorPalettes;

import java.awt.Color;
import java.util.HashMap;
import java.util.function.IntFunction;

public class StylePalette {

	private IntFunction<String> namer = i->i+"";
	private IntFunction<Color> palette = ColorPalettes.Set1.getCircularDiscreteMapper();
	
	private HashMap<Integer,StyleObject> cache = new HashMap<Integer, StyleObject>(); 
	
	
	public void setNamer(IntFunction<String> namer) {
		this.namer = namer;
	}
	
	public void setPalette(IntFunction<Color> palette) {
		this.palette = palette;
	}
	
	public void setPalette(ColorPalettes palette) {
		this.palette = palette.getCircularDiscreteMapper();
	}
	
	
	public StyleObject get(int index) {
		if (!cache.containsKey(index)) {
			StyleObject re = new StyleObject();
			cache.put(index, re);
			
			re.setName(namer.apply(index));
			re.setColor(palette.apply(index));
		}
		return cache.get(index);
	}
	
}
