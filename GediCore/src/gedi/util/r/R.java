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
package gedi.util.r;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RFactor;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class R extends RConnection {

	private static final Logger log = Logger.getLogger( R.class.getName() );
	private static final int BIG = 100;

	R() throws RserveException {
		super();
	}
	
	public void closeDevice() throws RserveException {
		eval("dev.off()");
	}

	public int getOpenDevices() {
		try {
			return eval("dev.cur()").asInteger()-1; // dont count null device!
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed getting devices!",e);
			throw new RuntimeException("Could not fetch open devices!",e);
		}
	}
	
	public R run(String... cmd) {
		for (String c : cmd)
			try {
				log.log(Level.FINE, "Running in R: "+c);
				eval(c);
			} catch (REngineException e) {
				throw new RuntimeException("Could not run: "+c,e);
			}
		return this;
	}
	
	public R run(URL script) {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(script.openStream()));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line=br.readLine())!=null) {
				sb.append(line).append("\n");
			}
			eval(sb.toString());
			return this;
		} catch (Exception e) {
			throw new RuntimeException("Could not run script!",e);
		}
	}
	
	public REXP evalUnchecked(String cmd) {
		log.log(Level.FINE,"R: "+cmd);
		try {
			return super.eval(cmd);
		} catch (RserveException e) {
			throw new RuntimeException("Cannot eval command "+cmd,e);
		}
	}
	
	
	@Override
	public REXP eval(String cmd) throws RserveException {
		log.log(Level.FINE,"R: "+cmd);
		try {
			return super.eval(cmd);
		} catch (RserveException e) {
			String msg = e.getMessage();
			try {
				msg = super.eval("geterrmessage()").asString();
			} catch (REXPMismatchException e1) {
			}
			throw new RserveException(this, msg, e.getRequestReturnCode());
		}
	}
	
	
	public REXP evalf(String format, Object...val) throws RserveException {
		return eval(String.format(Locale.US,format,val));
	}
	
	public R set(String symbol, double v) {
		try {
			assign(symbol, new REXPDouble(v));
		} catch (REngineException e) {
			throw new RuntimeException("Could not assign!",e);
		}
		return this;
	}
	
	public R set(String symbol, boolean v) {
		try {
			assign(symbol, new REXPLogical(v));
		} catch (REngineException e) {
			throw new RuntimeException("Could not assign!",e);
		}
		return this;
	}
	
	public R set(String symbol, double[] v) {
		try {
//			if (v.length>BIG) {
//				File f = File.createTempFile("Rcomfort", "symbol");
//				PageFileWriter b = new PageFileWriter(f.getAbsolutePath());
//				NumericArray.wrap(v).serialize(b);
//				b.close();
//				
//				run(symbol+".file = file(\""+f.getAbsolutePath()+"\", \"rb\")");
//				run(symbol+".size = readBin("+symbol+".file, \"integer\", endian=\"big\")");
//				run(symbol+" = readBin("+symbol+".file, \"double\", n="+symbol+".size)");
//				run("close("+symbol+".file)");
//				run("rm("+symbol+".file,"+symbol+".size)");
//				
//				
////				BufferedWriter bw = new BufferedWriter(new FileWriter(f));
////				for (double d : v)
////					bw.write(d+"\n");
////				bw.close();
////				run(symbol+"<-read.delim('"+f.getAbsolutePath()+"',header=F)$V1");
////				f.delete();
//			} else 
				assign(symbol, v);
		} catch (REngineException  e) {
			throw new RuntimeException("Could not assign!",e);
		}
		return this;
	}
	
	public R set(String symbol, int[] v) {
		try {
			assign(symbol, v);
		} catch (REngineException e) {
			throw new RuntimeException("Could not assign!",e);
		}
		return this;
	}
	
	public R set(String symbol, String v) {
		try {
			assign(symbol, v);
		} catch (REngineException e) {
			throw new RuntimeException("Could not assign!",e);
		}
		return this;
	}
	
	public double[] getDoubles(String symbol) {
		try {
			return eval(symbol).asDoubles();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}
	
	public double getDouble(String symbol) {
		try {
			return eval(symbol).asDouble();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}
	
	public int[] getIntegers(String symbol) {
		try {
			return eval(symbol).asIntegers();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}
	
	public int getInt(String symbol) {
		try {
			return eval(symbol).asInteger();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}
	
	public double[][] getMatrix(String symbol) {
		try {
			return eval(symbol).asDoubleMatrix();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}
	
	public RFactor getFactor(String symbol) {
		try {
			return eval(symbol).asFactor();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}

	public RList getList(String symbol) {
		try {
			return eval(symbol).asList();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}
	public String getString(String symbol) {
		try {
			return eval(symbol).asString();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}
	
	public Object get(String symbol) {
		try {
			return eval(symbol).asNativeJavaObject();
		} catch (Exception e) {
			throw new RuntimeException("Could not read!",e);
		}
	}
	
	
	
	
	private Stack<String> tmpFileMask = new Stack<String>();
	
	public R startPlots(int width, int height) {
		return startPlots(width, height, -1);
	}
	
	public R startPlots(int width, int height, int dpi) {
		try {
			String f = File.createTempFile("Rcomfort", "%05d.png").getAbsolutePath();
			tmpFileMask.push(f);
			log.log(Level.FINE,"Start plot into file "+f);
			if (dpi>0)
				evalf("png('%s',width=%d,height=%d,res=%d)",f,width,height,dpi);
			else
				evalf("png('%s',width=%d,height=%d)",f,width,height);
			return this;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed starting plots!",e);
			throw new RuntimeException("Could not start plot!",e);
		}
	}
	
	private String pdf = null;
	public R startPDF(String filename) {
		return startPDF(filename,7,7);
	}
	public R startPDF(String filename, int width, int height) {
		if (pdf!=null)
			throw new RuntimeException("Do not plot into more than one pdf!");
		try {
			log.log(Level.FINE,"Start plot into file "+filename);
			evalf("pdf('%s',width=%d,height=%d)",filename,width,height);
			this.pdf = filename;
			return this;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed starting plots!",e);
			throw new RuntimeException("Could not start plot!",e);
		}
	}
	
	public String finishPDF() {
		String pdf = this.pdf;
		this.pdf = null;
		try {
			eval("dev.off()");
			return pdf;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed finishing plots!",e);
			throw new RuntimeException("Could not finish plot!",e);
		}
	}
	
	
	public R startPNG(String filename) {
		return startPNG(filename,480,480);
	}
	public R startPNG(String filename, int width, int height) {
		if (pdf!=null)
			throw new RuntimeException("Do not plot into more than one png!");
		try {
			log.log(Level.FINE,"Start plot into file "+filename);
			evalf("png('%s',width=%d,height=%d)",filename,width,height);
			this.pdf = filename;
			return this;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed starting plots!",e);
			throw new RuntimeException("Could not start plot!",e);
		}
	}
	
	public String finishPNG() {
		String pdf = this.pdf;
		this.pdf = null;
		try {
			eval("dev.off()");
			return pdf;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed finishing plots!",e);
			throw new RuntimeException("Could not finish plot!",e);
		}
	}
	
	public ArrayList<BufferedImage> finishPlots() {
		ArrayList<BufferedImage> re = new ArrayList<BufferedImage>();
		finishPlots(re);
		return re;
	}
	
	public <C extends Collection<BufferedImage>> R finishPlots(C list) {
		try {
			eval("dev.off()");
			String mask = tmpFileMask.pop();
			int n = 1;
			File f;
			while ((f=new File(String.format(mask, n))).exists()) {
				log.log(Level.FINE,"Read file "+f);
				list.add(ImageIO.read(f));
				f.delete();
				n++;
			}
			return this;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed finishing plots!",e);
			throw new RuntimeException("Could not finish plot!",e);
		}
	}
	
	public BufferedImage finishPlot() throws RserveException, IOException {
		try {
			eval("dev.off()");
			String mask = tmpFileMask.pop();
			int n = 1;
			File f;
			BufferedImage re = null;
			while ((f=new File(String.format(mask, n))).exists()) {
				if (re==null) {
					log.log(Level.FINE,"Read file "+f);
					re = ImageIO.read(f);
				}
				f.delete();
				n++;
			}
			return re;
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed finishing plots!",e);
			throw new RuntimeException("Could not finish plot!",e);
		}
	}

	public void eps(File f) throws RserveException {
		eval("postscript(file = \""+f.getAbsolutePath()+"\", horizontal = FALSE, onefile = FALSE, paper = \"special\", height=7,width=7)");
	}
	public void svg(File f) throws RserveException {
		eval("svg(file = \""+f.getAbsolutePath()+"\")");
	}

	public boolean requirePackage(String pck) throws RserveException {
		try {
			return eval("require("+pck+")").asInteger()==1;
		} catch (REXPMismatchException e) {
			throw new RuntimeException(e);
		}
	}

	
}
