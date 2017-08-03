#!/usr/bin/env Rscript



library(ggplot2)

b2="#3182BD"
price='#9fda18'



t=read.delim(paste(prefix,".estimateData",sep=''))
pr=read.delim(paste(prefix,".signal.tsv",sep=''))

m=rbind(as.matrix(t[,3:5]),as.matrix(t[,6:8]))
sn=data.frame(Signal=apply(m,1,function(v) max(v)),Noise=apply(m,1,function(v) sum(v)-max(v)),Class=c(t$Length,paste(t$Length,"L",sep="")))
sn$STN=sn$Signal/sn$Noise
sn=sn[order(sn$STN,decreasing=T),]

svg(paste(prefix,".signaltonoise.svg",sep=''))
ggplot(sn,aes(cumsum(Signal),cumsum(Signal)/cumsum(Noise)))+geom_line(color=b2)+geom_point(size=2,color=b2)+geom_point(data=pr,size=3,color=price)+theme(text=element_text(size=22))+xlab("Signal")+ylab("Signal to noise")+geom_label(data=pr,label='PRICE',vjust=1.5,hjust=1,fill=price,color='white')+geom_label(aes(label=ifelse(Signal>quantile(Signal,0.9),as.character(Class),NA)),vjust=0,hjust=0,fill=b2,color='white')
dev.off()
