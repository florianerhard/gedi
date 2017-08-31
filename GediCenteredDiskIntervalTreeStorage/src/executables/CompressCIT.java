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

package executables;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class CompressCIT {

	public static void main(String[] args) throws IOException {
		if (args.length!=2) {
			usage();
			System.exit(1);
		}
		
		if (!new File(args[0]).exists()) {
			System.out.println("File "+args[0]+" does not exist!");
			usage();
			System.exit(1);
		}
		
		
		CenteredDiskIntervalTreeStorage in = new CenteredDiskIntervalTreeStorage(args[0]);
		CenteredDiskIntervalTreeStorage out = new CenteredDiskIntervalTreeStorage(args[1],in.getType(),true);
		out.fill(in);
		out.setMetaData(in.getMetaData());
		
	}

	private static void usage() {
		System.out.println("CompressCIT <in> <out>");
	}
	
}
