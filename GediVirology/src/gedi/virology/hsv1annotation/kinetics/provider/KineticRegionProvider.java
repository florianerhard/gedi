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

package gedi.virology.hsv1annotation.kinetics.provider;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.functions.ExtendedIterator;


public interface KineticRegionProvider {

	
	String getType();	
	ExtendedIterator<ImmutableReferenceGenomicRegion<NameAnnotation>> ei(ReferenceGenomicRegion<?> region);

	default ImmutableReferenceGenomicRegion<NameAnnotation>[] arr(ReferenceGenomicRegion<?> region) {
		return ei(region).toArray(new ImmutableReferenceGenomicRegion[0]);
	}
}
