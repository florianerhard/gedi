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
package gedi.util.oml;

import java.io.File;
import java.io.IOException;

/**
 * Each node in the OML file either refers to a (1) constructor or a (2) method. For (1), an object is created and for (2) the method is executed on the parent node. 

Each node may have attributes, that are used as parameters for the constructor/method call. Names may be specified in the corresponding classes using the OmlParam annotation. When the constructor/method does not have an annotation, the correct overloading constructor/method is tried to be inferred from the number and types of the oml attributes.
Attributes can either be ints, doubles, Strings or references to previously created (or programmatically added) objects. Other parsers can also be added.
The attribute named "id" is special, as it is used to store the created object (either from the constructor or the return value from a method) for referencing. Objects are available for subsequent siblings and their descendents.
The attribute named "class" is also special, as space separated names are stored in the class property of omlnodes. These can then be used to apply properties e.g. using a cps file. 
If the created object has an setId(String) or setOmlClasses(String[]) method, it is called accordingly. 
Constructor nodes may also be inlined: A node A may directly refer to its direct children. These are treated as if they were older siblings of A, i.e. inlined nodes cannot refer to children of A (nor to A).
Finally, the id and reference to inlined nodes may be omitted, if the inlined node is the only child and the parent has no attributes.

New (10/29/2015): Inner classes can be specified directly in an object of the outer class. It may even be a non-static inner class!
New (01/27/2017): Call static methods as <Genomic.get param=...
New (01/27/2017): Select methods and ctors based preferentially on argument names (that are now finally stored in class files!)
New (04/05/2017): Inner Text of tags can be used as single string argument for methods/ctors

Rules:
1. Child has id attribute, parent has attribute referring to it and class with specified name and matching overloading constructor found: child is constructor node is stored using id attribute
2. Method with the specified name and matching overloading found: child is method node and gets executed on the parent object; returned object can be stored using id attribute
3. Setter/Adder/Property with the specified name and matching overloading found: child is method node and gets executed on the parent object; returned object can be stored using id attribute
4. Getter with the specified name and matching overloading found: child is method node and gets executed on the parent object; returned object can be stored using id attribute
5. Class with specified name and matching overloading constructor found and parent class has add method with matching overloading: child is constructor node and is added; object can be stored using id attribute
Otherwise an error is produced.


Order:
DFS through the tree (which is the order in the OML file) and each node is executed immediately on encounter. Note the special treatment of inlined nodes (they are executed before their parent). 

 * @author erhard
 *
 */
public class Oml {

	public static <T> T create(String file) throws IOException {
		return new OmlNodeExecutor().execute(new OmlReader().parse(new File(file)));
	}
	
}
