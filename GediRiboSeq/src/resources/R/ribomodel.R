#!/usr/bin/env Rscript



library(ggplot2)



t=read.delim(paste(prefix,".model.tsv",sep=''))
t$Position[t$Parameter=="Upstream"]=-t$Position[t$Parameter=="Upstream"]


c=t[t$Parameter=="Upstream" & t$Value>0.001,]
png(paste(prefix,".model.upstream.png",sep=''),width=460,height=800)
ggplot(c,aes(Position,Value))+geom_bar(stat="identity")+theme(text=element_text(size=20))+xlab("Position relative to P site")+ylab("Probability")
dev.off()

c=t[t$Parameter=="Downstream" & t$Value>0.001,]
png(paste(prefix,".model.downstream.png",sep=''),width=460,height=800)
ggplot(c,aes(Position,Value))+geom_bar(stat="identity")+theme(text=element_text(size=20))+xlab("Position relative to P site")+ylab("Probability")
dev.off()

png(paste(prefix,".model.cleavage.png",sep=''),width=800,height=800)
c=t[t$Parameter!="Untemplated addition" & t$Value>0.001,]
c$Position[c$Position>0]=c$Position[c$Position>0]+3
ggplot(c,aes(Position,Value))+geom_bar(stat="identity")+theme(text=element_text(size=20))+xlab("Position relative to P site")+ylab("Probability")
dev.off()

c=t[t$Parameter=="Untemplated addition",]
png(paste(prefix,".model.addition.png",sep=''),width=130,height=800)
ggplot(c,aes(Position,Value))+geom_bar(stat="identity")+theme(text=element_text(size=20))+ylab("Probability")+scale_x_continuous(breaks=c())+xlab("")
dev.off()

