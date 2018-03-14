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

package net.sf.cglib.transform.impl;

import junit.framework.*;

/**
 *
 * @author  baliuka
 */
public class TestDemo extends TestCase{
    
     /** Creates a new instance of AbstractTransformTest */
    public TestDemo(String s) {
       super(s);
    }
    
    public void test()throws Exception{
    
        TransformDemo.main( null );
    
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return new TestSuite(TestDemo.class);
    }
    
}
