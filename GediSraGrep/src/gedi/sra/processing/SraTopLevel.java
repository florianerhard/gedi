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
package gedi.sra.processing;

import gedi.sra.schema.experiment.ExperimentSetType;
import gedi.sra.schema.run.RunSetType;
import gedi.sra.schema.sample.SampleSetType;
import gedi.sra.schema.study.StudySetType;
import gedi.sra.schema.submission.SubmissionType;

public enum SraTopLevel {
	
	Study {
		@Override
		public Class<?> getType() {
			return StudySetType.class;
		}
	},
	Experiment {
		@Override
		public Class<?> getType() {
			return ExperimentSetType.class;
		}
	},
	Run {
		@Override
		public Class<?> getType() {
			return RunSetType.class;
		}
	},
	Sample {
		@Override
		public Class<?> getType() {
			return SampleSetType.class;
		}
	},
	Submission {
		@Override
		public Class<?> getType() {
			return SubmissionType.class;
		}
	};
	
	public abstract Class<?> getType();
}