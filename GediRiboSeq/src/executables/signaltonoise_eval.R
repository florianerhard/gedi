#!/usr/bin/env Rscript

pdf(paste(prefix,".signaltonoise.pdf",sep=''))


s<-read.delim(paste(prefix,'.robustnoise.data',sep=''))
d<-read.delim(paste(prefix,'.signaltonoise.details.data',sep=''))
t<-read.delim(paste(prefix,'.signaltonoise.data',sep=''))
c<-read.delim(paste(prefix,'.codonsignaltonoise.data',sep=''))

main=gsub(".*rem2/(.+?)(_prio)?\\.merged","\\1",prefix)

h=t[t$Signal/t$Noise>=sort(t$Signal/t$Noise,decreasing=T)[min(5,length(t$Signal))] | t$Type %in% c("Simple","Codon"),]
plot(t$Signal,t$Signal/t$Noise,main=main,xlab='Total signal',ylab="Signal / Noise",pch=20,xlim=c(0,max(t$Signal)*1.2),ylim=c(0,max((t$Signal+1)/(t$Noise+1))*1.2))
text(h$Signal,h$Signal/h$Noise,labels=gsub('-','5pM ',h$Type),adj=c(-0.1,1.1),offset=2)

#h=c[c$Signal/c$Noise>=sort(c$Signal/c$Noise,decreasing=T)[min(5,length(c$Signal))] | c$Type %in% c("Simple","Codon"),]
#plot(c$Signal,c$Signal/c$Noise,main=main,xlab='Total signal',ylab="Signal / Noise",pch=20,xlim=c(0,max(c$Signal)*1.2),ylim=c(0,max((c$Signal+1)/(c$Noise+1))*1.2))
#text(h$Signal,h$Signal/h$Noise,labels=gsub('-','5pM ',h$Type),adj=c(-0.1,1.1),offset=2)


trimming=s[s$Type=="Codon","Trim"]
codonsnr=t$Signal[t$Type=="Codon"]/s[s$Type=="Codon","Noise"]
simplesnr=t$Signal[t$Type=="Simple"]/s[s$Type=="Simple","Noise"]
plot(trimming,codonsnr,type='l',log='y',xlim=c(0,0.25),ylim=range(c(codonsnr[trimming<0.25],simplesnr[trimming<0.25]),na.rm=T,finite=T),xlab="Outlier trimming", ylab="Signal / Noise",main=main,lwd=4)
lines(trimming,simplesnr,lty=2,lwd=4)
legend('topleft',c('Codon','Simple'),lty=c(1,2),lwd=2)

cols=c('#AAAAAA','#777777','#000000')
d$Abundance=cut(log10(d$Reads),breaks=quantile(log10(d$Reads),c(0,0.33,0.66,1)),labels=c("Low abundance genes","Medium abundance genes","High abundance genes"))

data=((d$Signal+1)/(d$Noise+1))
plot(NA,main=main,ylim=c(0,1),xlim=quantile(data,c(0.01,0.99),na.rm=T),xlab="Signal / Noise",ylab="Cumulative frequency")
for (i in 1:3) {
	lev=levels(d$Abundance)[i]
	data=((d$Signal+1)/(d$Noise+1))[d$Abundance==lev]
	lines(ecdf(data),lwd=4,col=cols[i])
}
legend('bottomright',legend=levels(d$Abundance),fill=cols,bg='white')


data=(((d$Signal+1)/(d$Noise+1))/((d$Simple.Signal+1)/(d$Simple.Noise+1)))
plot(NA,main=main,ylim=c(0,1),xlim=quantile(data,c(0.01,0.99),na.rm=T),xlab="Signal / Noise improvement factor",ylab="Cumulative frequency")
for (i in 1:3) {
	lev=levels(d$Abundance)[i]
	data=(((d$Signal+1)/(d$Noise+1))/((d$Simple.Signal+1)/(d$Simple.Noise+1)))[d$Abundance==lev]
	lines(ecdf(data),lwd=4,col=cols[i])
}
abline(v=1,lty=2,lwd=3)
legend('bottomright',legend=levels(d$Abundance),fill=cols,bg='white')


dev.off()
