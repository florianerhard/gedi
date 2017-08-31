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

package gedi.util.plotting;

import gedi.app.extension.ExtensionContext;
import gedi.core.workspace.action.WorkspaceItemActionExtensionPoint;
import gedi.gui.WindowType;
import gedi.util.StringUtils;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.mutable.MutableMonad;
import gedi.util.r.R;
import gedi.util.r.RConnect;
import gedi.util.r.RDataWriter;
import gedi.util.r.RProcess;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

import org.rosuda.REngine.Rserve.RserveException;

public class GGPlot {

	private DataFrame df;
	private StringBuilder cmd = new StringBuilder();
	
	public GGPlot(DataFrame df, Aes... aes) {
		this.df = df;
		this.cmd.append("ggplot(ggplot.tab");
		Aes.append(this.cmd,df,aes,true);
		this.cmd.append(")");
	}
	
	private GGPlot(DataFrame df, String cmd) {
		this.df = df;
		this.cmd.append(cmd);
	}
	
	@Override
	public String toString() {
		return cmd.toString();
	}
	
	public void display(String label) throws RserveException, IOException {
		display(label,WindowType.Reuse,2100,2100,300);
	}
	public void display(String label, WindowType type, int width, int height, int dpi) throws RserveException, IOException {
		ExtensionContext context = new ExtensionContext();
		context.add(String.class, label);
		context.add(WindowType.class, type);
		WorkspaceItemActionExtensionPoint.getInstance().get(context, BufferedImage.class).accept(plot(width,height,dpi));
	}
	
	public void pdf(String filename) throws RserveException, IOException {
		R r = prepareR();
		r.startPDF(filename);
		try {
			r.eval("print("+toString()+")");
		} catch (RserveException e) {
			System.err.println(toString());
			throw e;
		}
		r.finishPDF();
//		finishR(r);
	}
	
	public void png(String filename) throws RserveException, IOException {
		R r = prepareR();
		r.startPNG(filename);
		try {
			r.eval("print("+toString()+")");
		} catch (RserveException e) {
			System.err.println(toString());
			throw e;
		}
		r.finishPNG();
//		finishR(r);
	}
	
	public BufferedImage plot() throws RserveException, IOException {
		return plot(2100,2100,300);
	}
	
	private R prepareR() throws RserveException, IOException {
		R r = RConnect.R();
		r.eval("suppressMessages(library(ggplot2))");
		MutableMonad<RserveException> tex = new MutableMonad<>();
		Thread listenThread = new Thread() {
			@Override
			public void run() {
				try {
					r.eval(RProcess.LISTEN_CMD);
				} catch (RserveException e) {
					tex.Item = e;
				}
			}
		};
		listenThread.start();

		Socket socket = null;
		while (socket==null) {
			try {
				socket = new Socket("localhost",RProcess.port);
			} catch (ConnectException e) {
			}
		}
		RDataWriter re = new RDataWriter(socket.getOutputStream());
		re.writeHeader();
		re.write("ggplot.tab", df);
		re.finish();
		
		try {
			listenThread.join();
		} catch (InterruptedException e) {
			return null;
		}
		if (tex.Item!=null) throw tex.Item;
		
//		r.eval("for (col in which(sapply(ggplot.tab,class)=='character')) ggplot.tab[,col]<-factor(ggplot.tab[,col],levels=unique(ggplot.tab[,col]))");
		return r;
	}
	
//	private void finishR(R r) throws RserveException {
//		r.eval("dbDisconnect(con)");
//	}
	
	public BufferedImage plot(int width, int height, int dpi) throws RserveException, IOException {
		R r = prepareR();
		r.startPlots(width, height,dpi);
		r.eval("print("+toString()+")");
		BufferedImage re = r.finishPlot();
//		finishR(r);
		return re;
	}
	

	private GGPlot copy() {
		return new GGPlot(df, cmd.toString());
	}
	
	public GGPlot geom_area() {
		GGPlot re = copy();
		re.cmd.append(" + geom_area()");
		return re;
	}
	public GGPlot geom_area(Stat stat) {
		GGPlot re = copy();
		re.cmd.append(" + geom_area(\""+stat.toString()+"\")");
		return re;
	}
	public GGPlot geom_area_bin() {
		return geom_area(Stat.bin);
	}
	
	public GGPlot geom_density() {
		GGPlot re = copy();
		re.cmd.append(" + geom_density()");
		return re;
	}
	public GGPlot geom_density(double adjust) {
		GGPlot re = copy();
		re.cmd.append(" + geom_density(adjust="+adjust+")");
		return re;
	}
	
	public GGPlot geom_dotplot() {
		GGPlot re = copy();
		re.cmd.append(" + geom_dotplot()");
		return re;
	}

	public GGPlot geom_freqpoly() {
		GGPlot re = copy();
		re.cmd.append(" + geom_freqpoly()");
		return re;
	}
	

	public GGPlot geom_histogram() {
		GGPlot re = copy();
		re.cmd.append(" + geom_histogram()");
		return re;
	}
	public GGPlot geom_histogram(double binwidth) {
		GGPlot re = copy();
		re.cmd.append(" + geom_histogram(binwidth="+binwidth+")");
		return re;
	}
	public GGPlot geom_histogram(double...breaks) {
		GGPlot re = copy();
		re.cmd.append(" + geom_histogram(breaks=c("+StringUtils.concat(",", breaks)+"))");
		return re;
	}
	

	public GGPlot geom_bar() {
		GGPlot re = copy();
		re.cmd.append(" + geom_bar()");
		return re;
	}
	
	public GGPlot geom_polygon() {
		GGPlot re = copy();
		re.cmd.append(" + geom_polygon()");
		return re;
	}
	
	public GGPlot geom_path() {
		GGPlot re = copy();
		re.cmd.append(" + geom_path()");
		return re;
	}
	
	public GGPlot geom_ribbon() {
		GGPlot re = copy();
		re.cmd.append(" + geom_ribbon()");
		return re;
	}
	
	public GGPlot geom_segment() {
		GGPlot re = copy();
		re.cmd.append(" + geom_segment()");
		return re;
	}
	
	public GGPlot geom_rect() {
		GGPlot re = copy();
		re.cmd.append(" + geom_rect()");
		return re;
	}
	
	public GGPlot geom_blank() {
		GGPlot re = copy();
		re.cmd.append(" + geom_blank()");
		return re;
	}

	public GGPlot geom_jitter() {
		GGPlot re = copy();
		re.cmd.append(" + geom_jitter()");
		return re;
	}

	public GGPlot geom_point() {
		GGPlot re = copy();
		re.cmd.append(" + geom_point()");
		return re;
	}

	public GGPlot geom_quantile() {
		GGPlot re = copy();
		re.cmd.append(" + geom_quantile()");
		return re;
	}
	
	public GGPlot geom_rug() {
		GGPlot re = copy();
		re.cmd.append(" + geom_rug()");
		return re;
	}
	
	public GGPlot geom_rug(String side_trbl) {
		GGPlot re = copy();
		re.cmd.append(" + geom_rug(side=\""+side_trbl+"\")");
		return re;
	}

	public GGPlot geom_smooth() {
		GGPlot re = copy();
		re.cmd.append(" + geom_smooth()");
		return re;
	}
	
	public GGPlot geom_text(Aes...aes) {
		GGPlot re = copy();
		re.cmd.append(" + geom_smooth(");
		Aes.append(re.cmd, df, aes,false);
		re.cmd.append(")");
		return re;
	}
	
	public GGPlot geom_barxy() {
		GGPlot re = copy();
		re.cmd.append(" + geom_bar(stat=\"identity\")");
		return re;
	}
	
	public GGPlot geom_ecdf() {
		GGPlot re = copy();
		re.cmd.append(" + geom_line(stat=\"ecdf\")");
		return re;
	}
	
	public GGPlot geom_barxy(String position) {
		GGPlot re = copy();
		re.cmd.append(" + geom_bar(stat=\"identity\", position=\"").append(position).append("\")");
		return re;
	}
	
	public GGPlot geom_boxplot() {
		GGPlot re = copy();
		re.cmd.append(" + geom_boxplot()");
		return re;
	}
	
	public GGPlot geom_dotplotxy() {
		GGPlot re = copy();
		re.cmd.append(" + geom_dotplot(binaxis=\"y\",stackdir=\"center\")");
		return re;
	}
	
	public GGPlot geom_violin() {
		GGPlot re = copy();
		re.cmd.append(" + geom_violin(scale=\"area\")");
		return re;
	}
	

	public GGPlot geom_bin2d(double binwidthx,double binwidthy) {
		GGPlot re = copy();
		re.cmd.append(" + geom_bin2d(binwidth=c("+binwidthx+","+binwidthy+"))");
		return re;
	}	
	

	public GGPlot geom_density2d() {
		GGPlot re = copy();
		re.cmd.append(" + geom_density2d()");
		return re;
	}
	
	public GGPlot geom_hex() {
		GGPlot re = copy();
		re.cmd.append(" + geom_hex()");
		return re;
	}
	
	public GGPlot geom_line() {
		GGPlot re = copy();
		re.cmd.append(" + geom_line()");
		return re;
	}
	
	public GGPlot geom_step() {
		GGPlot re = copy();
		re.cmd.append(" + geom_step()");
		return re;
	}
	
	
	public GGPlot geom_crossbar() {
		GGPlot re = copy();
		re.cmd.append(" + geom_crossbar()");
		return re;
	}

	public GGPlot geom_crossbar(double fatten) {
		GGPlot re = copy();
		re.cmd.append(" + geom_crossbar(fatten="+fatten+")");
		return re;
	}


	public GGPlot geom_errorbar() {
		GGPlot re = copy();
		re.cmd.append(" + geom_errorbar()");
		return re;
	}
	
	public GGPlot geom_errorbarh() {
		GGPlot re = copy();
		re.cmd.append(" + geom_errorbarh()");
		return re;
	}
	
	public GGPlot geom_linerange() {
		GGPlot re = copy();
		re.cmd.append(" + geom_linerange()");
		return re;
	}
	
	public GGPlot geom_pointrange() {
		GGPlot re = copy();
		re.cmd.append(" + geom_pointrange()");
		return re;
	}
	

	public GGPlot coord_cartesian(NamedParameter... param) {
		GGPlot re = copy();
		re.cmd.append(" + coord_cartesian(");
		appendParam(re.cmd, param);
		re.cmd.append(")");
		return re;
	}
	
	
	
	
	private static void appendParam(StringBuilder sb, NamedParameter[] param) {
		for (int i=0; i<param.length; i++) {
			if (i>0) sb.append(", ");
			sb.append(param[i].name).append("=").append(param[i].value);
		}
	}

	public static NamedParameter xlim(double from, double to) {
		return new NamedParameter("xlim", "c("+from+","+to+")");
	}
	
	public static NamedParameter ylim(double from, double to) {
		return new NamedParameter("ylim", "c("+from+","+to+")");
	}
	
	public static class NamedParameter {
		private String name;
		private String value;
		public NamedParameter(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}
	}

	public GGPlot rotateLabelsX() {
		GGPlot re = copy();
		re.cmd.append("+theme(axis.text.x=element_text(angle=90,hjust=1,vjust=0.5))");
		return re;
	}
	
	
}
