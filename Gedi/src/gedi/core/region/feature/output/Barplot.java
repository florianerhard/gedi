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
import gedi.util.io.text.LineOrientedFile;
import gedi.util.r.RConnect;
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

import javafx.scene.image.Image;

/**
 * spaces in facets do not work in ggplot2 1.0.1 (will work in a future release)
 * TODO: interaction for multiple things; special commands
 * @author erhard
 *
 */
public class Barplot implements ResultProducer {
	private String name;
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
	
	public void setName(String name) {
		this.name = name;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public void setKeepScript(boolean keepScript) {
		this.keepScript = keepScript;
	}
	
	public void plot(LineOrientedFile file,boolean isFinal, String[] inputs, boolean needsMelt) throws IOException {
		
		
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
		if (needsMelt && isAes(aes,inputs.length)) {
			if (list.length()>0) list.append(",");
			list.append("`").append(label).append("`=t$`").append(label).append("`");
		}
		
		if (list.length()>0) {
			script.writef("t<-aggregate(t$Count,list(%s),sum)\n",list);
			script.writef("names(t)[dim(t)[2]]<-'Count'\n");
		}
		
		list = new StringBuilder();
		for (int i=0; i<inputs.length; i++) {
			if (isAes(aes,i) && !isSpecialAes(aes,i)) {
				if (list.length()>0) list.append(",");
				if (aes[i].equals("dfill"))
					list.append("fill=factor(`").append(inputs[i]).append("`)");
				else
					list.append(aes[i]).append("=`").append(inputs[i]).append("`");
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
		
		pfile = this.file!=null?this.file:file.getExtensionSibling("png").getPath();
		script.writef("ggsave('%s',width=7,height=7)\n\n",pfile);
		
		script.finishWriting();
		
		try {
			RConnect.R().run(script.toURI().toURL());
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
}
