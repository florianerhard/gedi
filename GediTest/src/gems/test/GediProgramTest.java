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
package gems.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import gedi.util.program.GediParameter;
import gedi.util.program.GediParameterSet;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.FileParameterType;
import gedi.util.program.parametertypes.StringParameterType;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

@RunWith(JUnit4.class)
public class GediProgramTest {
	
	@Test
	public void programTest() throws Exception {
		
		GediProgram pipeline = GediProgram.create("Test1",
				new Prog(0).in(0,1).out(2),
				new Prog(1).in(0,2).out(3)
				);
		p[0].set("0");
		p[1].set("1");
		
		assertNull(pipeline.execute(new GediProgramContext(Logger.getLogger("TEST"),()->new NoProgress(),false)));
		assertEquals("3", p[3].get());
	}
	
	@Test
	public void fileTest() throws Exception {
		GediProgram pipeline = GediProgram.create("File",
				new Prog(90).in(90,100).out(80),
				new Prog(1).in(80).out(81)
				);
		p[100].set("data/testfile");
		assertNull(pipeline.execute(new GediProgramContext(Logger.getLogger("TEST"),()->new NoProgress(),false)));
		assertEquals("81", p[81].get());
	}
	
	
	@Test
	public void programTest2() throws Exception {
		
		GediProgram pipeline = GediProgram.create("Test2",
				new Prog(2).in(10,11).out(12),
				new Prog(3).in(10,12).out(13,15),
				new Prog(4).in(13).out(14),
				new Prog(5).in(14,15).out(16)
				);
		p[10].set("10");
		p[11].set("11");
		p[13].set("13");
		p[15].set("15");
		
		assertNull(pipeline.execute(new GediProgramContext(Logger.getLogger("TEST"),()->new NoProgress(),false)));
		assertEquals("14", p[14].get());
		assertEquals("16", p[16].get());
	}
	
	@Before
	public void makeParams() {
		GediParameterSet set = new GediParameterSet();
		p = new GediParameter[101];
		for (int i=0; i<p.length-1; i++) {
			if (i<90) 
				p[i] = new GediParameter<String>(set, "p"+i, "", false, new StringParameterType());
			else
				p[i] = new GediParameter<File>(set, "${prefix}."+i, "", false, new FileParameterType());
		}
		p[p.length-1] = new GediParameter<>(set, "prefix", "", false, new StringParameterType());
		
	}
	
	private GediParameter[] p;


	private class Prog extends GediProgram {
		
		private int[] in;
		private int[] out;
		
		public Prog(int i) {
			super(i+"");
		}
		
		public Prog in(int...in) {
			for (int i : in)
				addInput(p[i]);
			this.in = in;
			return this;
		}
		public Prog out(int...out){
			for (int i : out)
				addOutput(p[i]);
			this.out = out;
			return this;
		}

		@Override
		public String execute(GediProgramContext context) throws Exception {
//			StringBuilder sb = new StringBuilder();
//			for (int i=0; i<in.length; i++)
//				sb.append(this.<String>getParameter(i));
			for (int i=0; i<out.length; i++) {
//				setOutput(i, sb.toString()+"#"+out[i]);	
				setOutput(i, ""+out[i]);
			}
			context.getLog().log(Level.INFO, "Executed "+getName());
			return null;
		}
		
	}
	
}
