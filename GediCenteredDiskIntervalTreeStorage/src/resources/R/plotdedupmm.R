#!/usr/bin/env Rscript

library(ggplot2)
library(reshape2)

t=read.delim(file)
t$RetainedDup=t$RetainedDup/t$Total
t$RetainedDedup=t$RetainedDedup/t$Total
t=melt(t[,1:4],value.name="Fraction",id.vars=c("Genomic","Read"))

png(out)
ggplot(t,aes(paste0(Genomic,">",Read),Fraction))+geom_bar(stat="identity")+facet_wrap(~variable,nrow=2)+xlab(NULL)+ylab(NULL)+theme(axis.text.x = element_text(angle = 90, vjust = 0.5, hjust=1))
dev.off()
