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
package gedi.gui.gtracks.rendering;

import java.awt.Color;
import java.io.IOException;

import gedi.util.io.randomaccess.BinaryWriter;

public interface GTracksRenderTarget {

	void rect(double x1, double x2, double y1, double y2, Color border, Color background);
	void text(String text, double x1, double x2, double y1, double y2, Color color, Color background);
	

	int writeRaw(BinaryWriter out, int width, int height) throws IOException;
	String getFormat();
}
