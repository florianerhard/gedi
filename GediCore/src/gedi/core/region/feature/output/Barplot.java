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
package gedi.core.region.feature.output;

import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.r.RConnect;
import gedi.util.r.RRunner;
import gedi.util.userInteraction.results.ImageResult;
import gedi.util.userInteraction.results.Result;
import gedi.util.userInteraction.results.ResultConsumer;
import gedi.util.userInteraction.results.ResultProducer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import executables.Template;
import javafx.scene.image.Image;

/**
 * spaces in facets do not work in ggplot2 1.0.1 (will work in a future release)
 * TODO: interaction for multiple things; special commands
 * @author erhard
 *
 */
public class Barplot implements ResultProducer {
	private static final Logger log = Logger.getLogger( Template.class.getName() );
	
	
	private String name;
	private String title;
	private String description;
	
	private String[] aes;
	private String position;
	private String label = "Library";
	private String file = null;
	private boolean keepScript = true;
	private String facet = "";
	
	
	private HashSet<ResultConsumer> consumers = new HashSet<ResultConsumer>();
	private String pfile;
	private boolean isFinal;
	
	private ArrayList<String> add = new ArrayList<String>();
	private double minimalFraction = 0.01;
	private boolean sort = false;
	private LineOrientedFile csvFile;
	private String section;
	
	public Barplot(String[] aes, String position) {
		this.aes = aes;
		this.position = position;
	}
	
	public void add(String t) {
		add.add(t);
	}
	
	public void setFile(String file) {
		this.file = file;
	}
	
	public void setFacet(String facet) {
		this.facet = facet;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void setMinimalFraction(double minimalFraction) {
		this.minimalFraction  = minimalFraction;
	}
	
	public void setSort() {
		this.sort = true;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public void rotateLabels() {
		add("theme(axis.text.x = element_text(angle = 90, vjust = 0.5, hjust=1))");
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTitle() {
		return title==null?FileUtils.getNameWithoutExtension(pfile):title;
	}

	public void setKeepScript(boolean keepScript) {
		this.keepScript = keepScript;
	}
	
	public void plot(LineOrientedFile file,boolean isFinal, String[] inputs, boolean needsMelt) throws IOException {
		log.info("Plotting "+name);
		String[] aes = this.aes;
		int cols = needsMelt?inputs.length+2:inputs.length+1;
		if (!needsMelt && aes.length==cols+1) {
			// remove the entry before the last from aes
			aes = ArrayUtils.removeItemFromArray(aes, aes.length-2);
			needsMelt = false;
		}
		if (aes.length!=cols) throw new RuntimeException("Cannot plot, given aestetics have wrong length (id="+name+" aes="+aes.length+", expected="+cols+")");
		
		LineOrientedFile script = file.getExtensionSibling("R");
		script.startWriting();
		script.writef("#!/usr/bin/env Rscript\n\n");
		script.writef("suppressMessages(library(ggplot2))\n");
		script.writef("suppressMessages(library(reshape2))\n\n");
		script.writef("t<-read.delim('%s',check.names=F)\n",file.getPath());
		if (minimalFraction>0) {
			script.writef("rowmax<-apply(as.data.frame(t[,-1:-%d]),2,max)\n",inputs.length);
			script.writef("t<-t[apply(as.data.frame(t[,-1:-%d]),1,function(v) sum(v>=rowmax*%.4f)>0),]\n",inputs.length,minimalFraction);
		}
		
		if (needsMelt) {
			script.writef("t<-melt(t,id=1:%d)\n",inputs.length);
			script.writef("names(t)[(dim(t)[2]-1):dim(t)[2]]<-c('%s','Count')\n",label);
		} else {
			script.writef("names(t)[dim(t)[2]]<-'Count'\n");
		}
		
		// if one of the columns is not used in any aestetics, integrate it out!
		StringBuilder list = new StringBuilder();
		for (int i=0; i<inputs.length; i++) {
			if (isAes(aes,i)) {
				if (list.length()>0) list.append(",");
				list.append("`").append(inputs[i]).append("`=t$`").append(inputs[i]).append("`");
			}
		}
		boolean usedlabel= false;
		boolean iterateover = false;
		if (needsMelt && isAes(aes,inputs.length)) {
			if (list.length()>0) list.append(",");
			list.append("`").append(label).append("`=t$`").append(label).append("`");
			usedlabel = true;
		}
		
		if (list.length()>0) {
			if(!usedlabel) {
				script.writef("ot=t;\nfor (lab in c('',levels(t$`%s`))) {\n",label);
				script.writef("if (lab=='') {\n\tt<-aggregate(t$Count,list(%s),sum)\n\tnames(t)[dim(t)[2]]<-'Count'\n} ",list);
				script.writef("else t=t[t$`%s`==lab,]\n",label);
				iterateover = true;
			} else {
				script.writef("t<-aggregate(t$Count,list(%s),sum)\n",list);
				script.writef("names(t)[dim(t)[2]]<-'Count'\n");
			}
		}
		
		String x = null;
		list = new StringBuilder();
		for (int i=0; i<inputs.length; i++) {
			if (isAes(aes,i) && !isSpecialAes(aes,i)) {
				if (list.length()>0) list.append(",");
				if (aes[i].equals("dfill"))
					list.append("fill=factor(`").append(inputs[i]).append("`)");
				else
					list.append(aes[i]).append("=`").append(inputs[i]).append("`");
				if (aes[i].equals("x"))
					x = inputs[i];
			}
		}
		if (needsMelt && isAes(aes,inputs.length) && !isSpecialAes(aes,inputs.length)) {
			if (list.length()>0) list.append(",");
			list.append(aes[inputs.length]).append("=`").append(label).append("`");
		}
		if (isAes(aes,aes.length-1) && !isSpecialAes(aes,aes.length-1)) {
			if (list.length()>0) list.append(",");
			list.append(aes[aes.length-1]).append("=`Count`");
		}
		
		if (sort) {
			script.writef("agg<-aggregate(t$Count,list(t$`%s`),sum)\n",x);
			script.writef("t$`%s` = factor(t$`%s`,levels=agg$Group.1[order(agg$x,decreasing=T)])\n",x,x);
		}
		
		script.writef("g<-ggplot(t,aes(%s))",list);
		script.writef("+geom_bar(stat='identity',position='%s')",position);
		
		
		list = new StringBuilder();
		for (int i=0; i<inputs.length; i++) {
			if (isAes(aes,i) && isSpecialAes(aes,i)) {
				if (list.length()>0) list.append("+");
				list.append("`").append(inputs[i]).append("`");
			}
		}
		if (needsMelt && isAes(aes,inputs.length) && isSpecialAes(aes,inputs.length)) {
			if (list.length()>0) list.append("+");
			list.append("`").append(label).append("`");
		}
		if (isAes(aes,aes.length-1) && isSpecialAes(aes,aes.length-1)) {
			if (list.length()>0) list.append("+");
			list.append("Count");
		}
		if (list.length()>0) {
			if (facet!=null && facet.length()>0)
				script.writef("+facet_wrap(~%s,%s)",list,facet);
			else
				script.writef("+facet_wrap(~%s)",list);
		}
		
		for (String a : add)
			script.writef(" + %s", a);
		
		script.writeLine();
		
		this.csvFile = file;
		pfile = this.file!=null?this.file:file.getExtensionSibling("png").getPath();
		
		
		if (iterateover){
			script.writef("if (lab!='') lab=paste0('.',lab);\n");
			script.writef("ggsave(sprintf('%s%%s.%s',lab),width=7,height=7)\n\n",FileUtils.getFullNameWithoutExtension(pfile),FileUtils.getExtension(pfile));
			script.writef("t=ot;\n}\n");
		} else
			script.writef("ggsave('%s',width=7,height=7)\n\n",pfile);
		script.finishWriting();
		
		try {
			RRunner r = new RRunner(script.getPath());
			r.run(false);
//			RConnect.R().run(script.toURI().toURL());
		} catch (Exception e) {
			GenomicRegionFeatureProgram.log.log(Level.WARNING,"Could not plot results in "+pfile+"!",e);
		}
		
		this.isFinal = isFinal;
		
		if (!keepScript && isFinal)
			script.delete();
		
		
		for (ResultConsumer cons : consumers)
			cons.newResult(this);
	}

	

	@Override
	public Result getCurrentResult() {
		return new ImageResult() {
			@Override
			public Image getImage() {
				try {
					return new Image(new File(pfile).toURI().toURL().toString());
				} catch (MalformedURLException e) {
					return null;
				}
			}
		};
	}
	private static boolean isSpecialAes(String[] aes, int i) {
		return aes[i].startsWith("facet");
	}

	private static boolean isAes(String[] aes, int i) {
		return aes[i]!=null && aes[i].length()!=0;
	}
	
	@Override
	public boolean isFinalResult() {
		return isFinal;
	}

	@Override
	public String getName() {
		return name;
	}
	

	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public void registerConsumer(ResultConsumer consumer) {
		consumers.add(consumer);
	}
	
	@Override
	public void unregisterConsumer(ResultConsumer consumer) {
		consumers.remove(consumer);
	}

	public String getImageFile() {
		return pfile;
	}
	
	public String getScriptFile() {
		return csvFile.getExtensionSibling("R").getPath();
	}
	
	public String getCsvFile() {
		return csvFile.getPath();
	}
	
	public void setSection(String section) {
		this.section = section;
	}
	
	public String getSection() {
		return section;
	}
	
}
