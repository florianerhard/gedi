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
package gedi.gui.genovis.tracks.boxrenderer;

import gedi.util.PaintUtils;

import java.awt.Color;

public class AnnotationRenderer<T> extends BoxRenderer<T> {

	
	
	public AnnotationRenderer() {
		setHeight(20);
		setFont("Arial", 14, true, false);
		setForeground(t->Color.WHITE);
		setBackground(t->PaintUtils.DARK_BLUE);
		stringer = x->String.valueOf(x.getData());
	}
	


}
