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


import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.awt.Color;
import java.io.IOException;

public class NameColorAnnotation implements BinarySerializable, NameProvider, ColorProvider {
	
	private String name;
	private Color color;
	
	public NameColorAnnotation() {
	}
	
	public NameColorAnnotation(String name, Color color) {
		this.name = name;
		this.color = color;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(name);
		out.putInt(color.getRGB());
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		name = in.getString();
		color = new Color(in.getInt(),true);
	}
	
	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public void setName(String name) {
		this.name = name;
	}

}
