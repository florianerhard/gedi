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
package gedi.util.io.text.genbank;

public abstract class AbstractFeaturePosition implements GenbankFeaturePosition {

	private String descriptor;
	private GenbankFeature feature;
	protected int leftMost;
	protected int rightMost;
	
	public AbstractFeaturePosition(GenbankFeature feature, String descriptor) {
		this.feature = feature;
		this.descriptor = descriptor;
	}
	

	@Override
	public String getDescriptor() {
		return descriptor;
	}

	@Override
	public GenbankFeature getFeature() {
		return feature;
	}
	
	@Override
	public String toString() {
		return getDescriptor();
	}

	@Override
	public int getStartInSource() {
		return leftMost;
	}
	
	@Override
	public int getEndInSource() {
		return rightMost;
	}
}
