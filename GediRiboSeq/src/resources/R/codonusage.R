#!/usr/bin/env Rscript



library(ggplot2)
library(plyr)



c=read.delim(input)

pdf(output,width=12)
for (codon in unique(c$Codon)) {
	print(ggplot(c[c$Codon==codon,],aes(Offset,log2(Value),color=Condition))+geom_line()+theme_bw()+theme(text=element_text(size=24))+scale_color_brewer(palette="Dark2")+geom_hline(yintercept=0)+scale_x_continuous(breaks=c(seq(min(c$Offset),-5,by=5),-1,0,1,seq(5,max(c$Offset),by=5)),labels=c(seq(min(c$Offset),-5,by=5),"E","P","A",seq(5,max(c$Offset),by=5)))+xlab("Position relative to ribosome")+ylab("Normalized codon activity")+ggtitle(codon))
}
dev.off()

png(aoutput,width=1024,height=512)
ggplot(c[c$Offset==1,],aes(Codon,log2(Value),color=Condition))+geom_point()+theme_bw()+theme(text=element_text(size=24))+scale_color_brewer(palette="Dark2")+theme(axis.text.x = element_text(angle = 90, vjust = 0.5, hjust=1))+ggtitle("A site")+ylab("Normalized codon activity")
dev.off()


