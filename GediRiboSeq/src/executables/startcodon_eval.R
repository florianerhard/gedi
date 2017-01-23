#!/usr/bin/env Rscript

library(ggplot2)
library(reshape2)
library(ROCR)
#require(grid)

#prefix=commandArgs(T)[1]

pdf(paste(prefix,".startstop.pdf",sep=''))

t<-read.delim(paste(prefix,'.startstop.estimateData',sep=''),stringsAsFactors=F)
t$sd[is.nan(t$sd)]=0

for (r in 1:dim(t)[1]) {
	x<-seq(min(t$mean[r]-4*t$sd[r]),max(t$mean[r]+4*t$sd[r]),by=0.01)
	plot(x,pnorm(x,mean=t$mean[r],sd=t$sd[r]),type='l',lwd=4,xlab="Value",ylab="Cumulative frequency",main=t$Name[r])
	abline(v=0,lty=2,lwd=3)
}

t<-read.delim(paste(prefix,'.changepoint.estimateData',sep=''),stringsAsFactors=F)
x<-seq(min(t$mean-4*t$sd),max(t$mean+4*t$sd),by=0.01)
plot(x,pnorm(x,mean=t$mean,sd=t$sd),type='l',lwd=4,xlab="Value",ylab="Cumulative frequency",main="Change point")
abline(v=0,lty=2,lwd=3)

t<-read.delim(paste(prefix,'.startpairs.eval.data',sep=''))
if (file.exists(paste(prefix,'.start.lfc.cv.eval',sep=''))) t<-rbind(t,read.delim(paste(prefix,'.start.lfc.cv.eval',sep='')))
t$Type=gsub("^(.*?)[0-9XYMIVM]+.*$","\\1",t$Location,perl=T)

for (type in c(NA,unique(t$Type))) {
	if (is.na(type)) a<-t else a=t[t$Type==type,]
	for (pair in unique(a$Pair)) {
		b<-a[a$Pair==pair,]
		auc=performance(prediction(b$Lod,b$Pos==0),measure='auc')@y.values[[1]]
		if (is.na(type)) title<-sprintf("%s\nAUC=%.2f",pair,auc) else title=sprintf("%s (%s)\nAUC=%.2f",pair,type,auc)
		
		perf=performance(prediction(b$Lod,b$Pos==0),measure='tpr',x.measure='fpr')
		df=data.frame(x=perf@x.values[[1]],y=perf@y.values[[1]])
		
		print(ggplot(df,aes(x,y))+geom_line(size=1.5)+geom_abline(slope=1,intercept=0,linetype=2,size=1.5)+labs(title=title,x=perf@x.name,y=perf@y.name))
		#print(ggplot(df,aes(x,y))+geom_line(size=1.5)+coord_cartesian(xlim=c(0,0.05))+theme(panel.margin=unit(c(0,0,0,0),"npc"),axis.title.x=element_blank(),axis.title.y=element_blank()), vp=viewport(.75, 0.3, .4, .4))
	}
}

dev.off()
