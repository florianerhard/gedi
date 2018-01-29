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

import gedi.core.data.annotation.AttributesProvider;
import gedi.core.data.annotation.NameProvider;
import gedi.util.PaintUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;

import java.awt.Color;

public class AttributesRenderer extends BoxRenderer<Object> {

	
	private DynamicObject features;
	private String attribute = "name";

	public AttributesRenderer() {
		stringer = (rgr)->{
			if (features==null) return "";
			String t = getType(rgr.getData());
			if (t==null) return "";
			String s = features.getEntry(t).getEntry("label").asString();
			return StringUtils.replaceVariables(s,p->{
				Object r = rgr.getData() instanceof AttributesProvider?((AttributesProvider)rgr.getData()).getAttribute(p):null;
				return r==null?p:r.toString();
			});
		};
		background = (rgr)->{
			if (features==null) return Color.gray;
			String t = getType(rgr.getData());
			if (t==null) return Color.gray;
			String s = features.getEntry(t).getEntry("color").asString();
			return PaintUtils.parseColor(s);
		};
	}
	
	
	private String getType(Object a) {
		if (attribute.equals("name") && a instanceof NameProvider)
			return ((NameProvider)a).getName();
		if (a instanceof AttributesProvider)
			return ((AttributesProvider) a).getStringAttribute(attribute);
		return null;
	}


	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	public void setFeatures(DynamicObject features) {
		this.features = features;
	}
	
	


}
