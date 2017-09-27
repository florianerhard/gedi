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

package gedi.util;

public class Checker {

	
	public static <T> T notNull(T check) {
		if (check==null) throw new RuntimeException("May not be null!");
		return check;
	}
	
	public static int not(int check, int not) {
		if (check==not) throw new RuntimeException("May not be "+not+"!");
		return check;
	}
	
	public static void equal(int a, int b) {
		if (a!=b) throw new RuntimeException("Not equal: "+a+" and "+b);
	}
	
}
