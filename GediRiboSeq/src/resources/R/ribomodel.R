#!/usr/bin/env Rscript



library(ggplot2)


t=read.delim(paste(prefix,".model.tsv",sep=''))
t$Position[t$Parameter=="Upstream"]=-t$Position[t$Parameter=="Upstream"]


c=t[t$Parameter=="Upstream" & t$Value>0.001,]
svg(paste(prefix,".model.upstream.svg",sep=''),width=4)
ggplot(c,aes(Position,Value))+geom_bar(stat="identity")+theme(text=element_text(size=20))+xlab("Position relative to P site")+ylab("Probability")
dev.off()

c=t[t$Parameter=="Downstream" & t$Value>0.001,]
svg(paste(prefix,".model.downstream.svg",sep=''),width=4)
ggplot(c,aes(Position,Value))+geom_bar(stat="identity")+theme(text=element_text(size=20))+xlab("Position relative to P site")+ylab("Probability")
dev.off()

svg(paste(prefix,".model.cleavage.svg",sep=''),width=7)
c=t[t$Parameter!="Untemplated addition" & t$Value>0.001,]
c$Position[c$Position>0]=c$Position[c$Position>0]+3
ggplot(c,aes(Position,Value))+geom_bar(stat="identity")+theme(text=element_text(size=20))+xlab("Position relative to P site")+ylab("Probability")
dev.off()

c=t[t$Parameter=="Untemplated addition",]
svg(paste(prefix,".model.addition.svg",sep=''),width=2)
ggplot(c,aes(Position,Value))+geom_bar(stat="identity")+theme(text=element_text(size=20))+ylab("Probability")+scale_x_continuous(breaks=c())+xlab("")
dev.off()

