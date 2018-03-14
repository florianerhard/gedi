#!/usr/bin/env Rscript



library(ggplot2)
library(plyr)



t=read.delim(input)
png(output,width=800,height=480)

ggplot(t,aes(Position,log2(Value),color=Condition))+geom_line()+coord_cartesian(ylim=log2(range(t$Value[t$Position>2 | t$Position<0])))+theme_bw()+theme(text=element_text(size=24))+scale_color_brewer(palette="Dark2")

dev.off()


