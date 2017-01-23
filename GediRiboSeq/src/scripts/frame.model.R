library(ggplot2)
library(reshape2)


t<-read.delim('rem2/hsv1_wt2.merged.estimateData')
m=melt(t,id.vars=c("Condition","Length"),value.name="Count",variable.name="Type")

m$Frame=as.factor(substr(m$Type,2,2))
m$MM5p=factor(substr(m$Type,4,4),labels=c("No 5p mismatch","5p mismatch"))

postscript("hsv_wt2.framereads.eps",horizontal=F,onefile=T,height=7,width=7)
ggplot(m[m$Length %in% 27:30,],aes(Length,Count,fill=Frame)) + geom_bar(stat="identity") + facet_grid(~MM5p) + scale_fill_grey(end=0.6) + theme(text = element_text(size = 18))
dev.off()
 

 
postscript("hsv_wt2.readlengths.eps",horizontal=F,onefile=T,height=7,width=7)
ggplot(m[m$Length %in% 21:34,],aes(Length,Count,fill=MM5p)) + geom_bar(stat="identity") + scale_fill_grey(end=0.6) + theme(text = element_text(size = 18), legend.title=element_blank())
dev.off()
