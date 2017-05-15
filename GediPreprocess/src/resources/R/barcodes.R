#!/usr/bin/env Rscript

suppressMessages(library(ggplot2))
suppressMessages(library(reshape2))

t<-read.delim('<?JS tsv ?>',check.names=F)
t$color=ifelse(nchar(as.character(t$Barcode))=='<?JS length ?>','gray','red')
t$Barcode=factor(t$Barcode,levels=t$Barcode[order(t$Count,decreasing=T)])

t<-t[t$Count>max(t$Count)/100,]

g<-ggplot(t,aes(Barcode,Count,fill=color))+geom_bar(stat="identity")+scale_fill_identity()+theme(axis.text.x=element_text(angle=90,hjust=1,vjust=0.5))
ggsave('<?JS png ?>',width=7,height=7)