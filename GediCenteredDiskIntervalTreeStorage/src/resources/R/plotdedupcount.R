#!/usr/bin/env Rscript

library(ggplot2)

t=read.delim(file)
t$Count=(t$Duplication*t$Count)/sum(t$Duplication*t$Count)
t=t[t$Duplication>1,]
t=t[t$Count>0.01*max(t$Count),]

png(out)
ggplot(t,aes(Duplication,Count))+geom_bar(stat="identity")+scale_x_continuous(breaks=t$Duplication)
dev.off()
