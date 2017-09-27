#!/usr/bin/env Rscript

suppressMessages(library(ggplot2))
suppressMessages(library(reshape2))

t<-read.delim('<?JS table ?>',check.names=F)
t$Category<-factor(t$Category,levels=t$Category)
g<-ggplot(t,aes(Category,Count))+geom_bar(stat="identity")+theme(axis.text.x=element_text(angle=90,hjust=1,vjust=0.5))
ggsave('<?JS png ?>',width=7,height=7)
