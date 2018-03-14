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
package gedi.util.algorithm.string.alignment.pairwise.scoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class QuasarMatrix extends SubstitutionMatrix {

	public QuasarMatrix(File file) throws IOException {
		float[][] matrix = null;
		int sig = 0;
		
		int nMatrix = 0;
		boolean full = false;
		String chars = null;
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while ((line=br.readLine())!=null) {
			
			if (line.startsWith("ROWINDEX") || line.startsWith("COLINDEX")) {
				if (chars==null) {
					chars = line.split("\\s+")[1];
					matrix = new float[chars.length()][chars.length()];
				} else {
					if (!chars.equals(line.split("\\s+")[1]))
						throw new IOException("ROWINDEX and COLINDEX must be equal!");
				}
			}
			else if (line.startsWith("MATRIX")) {
				if (chars==null)
					throw new IOException("ROWINDEX and COLINDEX be specified before MATRIX!");
				
				String[] numbers = line.split("\\s+");
				full |= nMatrix==0 && numbers.length-1>1;
				for (int i=0; i<numbers.length-1; i++) {
					
					sig = Math.max(sig, numbers[i+1].indexOf('.')>=0 ? numbers[i+1].length()-numbers[i+1].indexOf('.')-1: 0);
					
					matrix[nMatrix][i] = Float.parseFloat(numbers[i+1]);
					if (!full)
						matrix[i][nMatrix] = matrix[nMatrix][i]; 
				}
				
				nMatrix++;
			}
		}
		
		build(chars.toCharArray(), matrix, sig);
	}
	
}
