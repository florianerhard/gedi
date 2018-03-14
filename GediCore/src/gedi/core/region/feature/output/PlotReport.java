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
package gedi.core.region.feature.output;

public class PlotReport {
	public String section;
	public String id;
	public String title;
	public String description;
	public String img;
	public String script;
	public String csv;
	public PlotReport(String section,String id, String title, String description, String img, String script, String csv) {
		this.section = section;
		this.id = id;
		this.title = title;
		this.description = description;
		this.img = img;
		this.script = script;
		this.csv = csv;
	}
	
	@Override
	public String toString() {
		return title;
	}
	
}