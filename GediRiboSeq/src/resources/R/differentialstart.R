#!/usr/bin/env Rscript



library(ggplot2)
library(plyr)



t=read.delim(input)
pdf(output)

for (c in c(0.1,0.5,1)) {
for (contrast in unique(t$Contrast)) {
	box=ddply(t[t$Variance<c & t$Contrast==contrast,],.(Position),function(s) c(boxplot.stats(s$Mean)$stats,wilcox.test(s$Mean)$p.value))
	print(ggplot(box,aes(Position,group=factor(Position),ymin=V1,lower=V2,middle=V3,upper=V4,ymax=V5,color=V6*dim(box)[1]<0.01))+geom_boxplot(stat="identity")+theme_bw()+theme(text=element_text(size=24))+scale_color_manual(values=c('grey','black'),guide=FALSE)+ggtitle(paste(contrast,"with var <",c)))
}
}


dev.off()


