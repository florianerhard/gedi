#!/usr/bin/env Rscript

library(ggplot2)
library(data.table)


#prefix=commandArgs(T)[1]

pdf(paste(prefix,".model.pdf",sep=''))

# data
t<-as.data.frame(fread(paste(prefix,'.readlengths',sep="")))
t=t[t$Length>15,]
barplot(t$`Total posterior`,col=ifelse(t$Use,'black','gray'),border=ifelse(t$Use,'black','gray'),names.arg=t$Length,las=2,main="Valid read length",xlab="Read length",ylab="Total posterior")




# internal fits
s<-read.delim(paste(prefix,'.internal.model.txt',sep=""))
for (frame in 1:2) {
	f<-s[s$Frame==frame,]
	plot(NA,xlab="Triplett sum",ylab="Error Fraction",xlim=quantile(f$Sum,c(0,0.999)),ylim=range(f$Value),main=sprintf("Error data (Frame=%d)",frame),log='x')
	for (qi in 1:length(unique(f$Quantile))) {
		quantile<-unique(f$Quantile)[qi]
		lines(f[f$Quantile==quantile,'Sum'],f[f$Quantile==quantile,'Value'])
		lines(f[f$Quantile==quantile,'Sum'],f[f$Quantile==quantile,'Smooth'],col=rainbow(length(unique(f$Quantile)))[qi])
	}
	legend('topright',legend=sprintf("%.2g quantile",unique(f$Quantile)),fill=rainbow(length(unique(f$Quantile))),bg='white')
}


# data
t<-as.data.frame(fread(paste(prefix,'.bfits.data',sep="")))
#t=t[t$`Mean coverage`>1,]
t$Mean=t$Sum/t$Length
	
	
# fits
#g<-read.delim(paste(prefix,'.Gaps.model.txt',sep=""))
#plot(g$Coverage, g$mean,xlim=c(0,quantile(g$Coverage,0.99)),xlab="Mean coverage",ylab="Mean",main="Gaps conditional beta fit",pch=20)
#lines(g$Coverage, g$mean.Smooth,col='red',lwd=3)
#plot(g$Coverage, g$var,xlim=c(0,quantile(g$Coverage,0.99)),xlab="Mean coverage",ylab="Variance",main="Gaps conditional beta fit",pch=20)
#lines(g$Coverage, g$var.Smooth,col='red',lwd=3)


g<-as.data.frame(fread(paste(prefix,'.gaps.model.txt',sep="")))

t$Frac=t$Gaps/t$Length
upper=ceiling(quantile(t$Frac,0.95))
binsize = upper/100
t$Frac.bin=cut(t$Frac,breaks=seq(0,upper,by=binsize),labels=seq(0,upper-binsize,by=binsize))

t$MedCov=log10(t$`Mean`)
t$MedCov.bin=cut(t$MedCov,breaks=seq(min(t$MedCov),max(t$MedCov),length.out=100),labels=sprintf("%.2g",10^seq(min(t$MedCov),max(t$MedCov),length.out=100)[1:99]))
m=table(MeanCoverage=t$MedCov.bin,GapFraction=t$Frac.bin)
m=m/apply(m,1,sum)
d=as.data.frame(m)

spl=smooth.spline(g$Sum,g$Value,df=10)
d$Spline=cut(pmin(1,pmax(0,predict(spl,log10(as.double(as.character(d$MeanCoverage))))$y)),breaks=seq(0,upper,by=binsize),labels=seq(0,upper-binsize,by=binsize))

print(ggplot(d, aes(MeanCoverage, GapFraction,fill = Freq)) + geom_tile()+scale_fill_gradient(trans="log10")+theme(axis.text.x = element_text(angle = 90, vjust=0.2, hjust = 1))+scale_x_discrete(breaks=levels(t$MedCov.bin)[seq(1,length(levels(t$MedCov.bin)),by=10)])+scale_y_discrete(breaks=levels(t$Frac.bin)[seq(1,length(levels(t$Frac.bin)),by=10)])+geom_point(aes(x=MeanCoverage,y=Spline),colour="red"))



##### GOF
t<-as.data.frame(fread(paste(prefix,'.gof.data',sep="")))
g<-as.data.frame(fread(paste(prefix,'.gof.model.txt',sep="")))

if (max(t$GOF)>0) {
	
	t$GOF=log10(t$GOF)
	upper=ceiling(quantile(t$GOF,0.95))
	binsize = upper/100
	t$GOF.bin=cut(t$GOF,breaks=seq(min(t$GOF),upper,by=binsize),labels=sprintf("%.4g",10^seq(min(t$GOF),upper-binsize,by=binsize)))
	
	t$Activity = log10(t$Activity)
	a.min = quantile(t$Activity,0.01)
	a.max = max(t$Activity)
	
	t$Activity.bin=cut(t$Activity,breaks=seq(a.min,a.max,length.out=100),labels=sprintf("%.4g",10^seq(a.min,a.max,length.out=100)[1:99]))
	m=table(Activity=t$Activity.bin,Gof=t$GOF.bin)
	m=m/apply(m,1,sum)
	d=as.data.frame(m)
	
	spl=smooth.spline(g$Sum,g$Value,df=10)
	d$Spline=cut((predict(spl,log10(as.double(as.character(d$Activity))))$y),breaks=seq(min(t$GOF),upper,by=binsize),labels=sprintf("%.4g",10^seq(min(t$GOF),upper-binsize,by=binsize)))
	
	print(ggplot(d, aes(Activity, Gof,fill = Freq)) + geom_tile()+scale_fill_gradient(trans="log10")+theme(axis.text.x = element_text(angle = 90, vjust=0.2, hjust = 1))+scale_x_discrete(breaks=levels(t$Activity.bin)[seq(1,length(levels(t$Activity.bin)),by=10)])+scale_y_discrete(breaks=levels(t$GOF.bin)[seq(1,length(levels(t$GOF.bin)),by=10)])+geom_point(aes(x=Activity,y=Spline),colour="red"))
	
	
	##### GOF
	t<-as.data.frame(fread(paste(prefix,'.gofcod.data',sep="")))
	g<-as.data.frame(fread(paste(prefix,'.gofcod.model.txt',sep="")))
	
	t$GOF=log10(t$GOF)
	upper=max(t$GOF)
	binsize = upper/100
	t$GOF.bin=cut(t$GOF,breaks=seq(min(t$GOF),upper,by=binsize),labels=sprintf("%.4g",10^seq(min(t$GOF),upper-binsize,by=binsize)))
	
	t$Activity = log10(t$Activity)
	a.min = quantile(t$Activity,0.01)
	a.max = max(t$Activity)
	
	t$Activity.bin=cut(t$Activity,breaks=seq(a.min,a.max,length.out=100),labels=sprintf("%.4g",10^seq(a.min,a.max,length.out=100)[1:99]))
	m=table(Activity=t$Activity.bin,Gof=t$GOF.bin)
	m=m/apply(m,1,sum)
	d=as.data.frame(m)
	
	spl=smooth.spline(g$Sum,g$Value,df=10)
	d$Spline=cut((predict(spl,log10(as.double(as.character(d$Activity))))$y),breaks=seq(min(t$GOF),upper,by=binsize),labels=sprintf("%.4g",10^seq(min(t$GOF),upper-binsize,by=binsize)))
	
	print(ggplot(d, aes(Activity, Gof,fill = Freq)) + geom_tile()+scale_fill_gradient(trans="log10")+theme(axis.text.x = element_text(angle = 90, vjust=0.2, hjust = 1))+scale_x_discrete(breaks=levels(t$Activity.bin)[seq(1,length(levels(t$Activity.bin)),by=10)])+scale_y_discrete(breaks=levels(t$GOF.bin)[seq(1,length(levels(t$GOF.bin)),by=10)])+geom_point(aes(x=Activity,y=Spline),colour="red"))
}


#estbeta<-function(vals) {
#		m = mean(vals)
#		v = var(vals)
#		c = m*(1-m)/v-1
#		list(shape1=m*c,shape2=(1-m)*c)
#}
#x<-seq(0,1,by=0.01)

#for (bin in levels(t$MedCov.bin)) {
#	if (sum(t$MedCov.bin==bin)>10) {
#		plot(ecdf(t$Frac[t$MedCov.bin==bin]),main=sprintf("Beta fit for Coverage (Bin %s)",bin),xlab="Gap fraction",ylab="Cumulative frequency")
#		est<-estbeta(t$Frac[t$MedCov.bin==bin])
#		lines(x,pbeta(x,shape1=est$shape1,shape2=est$shape2),col='red')
#		legend('bottomright',legend=c(sprintf("Data (n=%d)",sum(t$MedCov.bin==bin)),sprintf("Beta fit (p=%.2g)",ks.test(t$Frac[t$MedCov.bin==bin],pbeta,shape1=est$shape1,shape2=est$shape2)$p.value)),fill=c('black','red'),bg='white')
#	}
#}




dev.off()
