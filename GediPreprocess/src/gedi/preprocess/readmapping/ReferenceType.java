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

package gedi.preprocess.readmapping;

public enum ReferenceType {

	Genomic(3,false),Transcriptomic(2,true),Both(-1,false),rRNA(1,true);
	
	public int prio;
	public boolean norc;
	private ReferenceType(int prio, boolean norc) {
		this.prio = prio;
		this.norc = norc;
	}
	
}
