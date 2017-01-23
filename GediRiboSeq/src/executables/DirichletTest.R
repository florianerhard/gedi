ddirichlet<-function (x, alpha,log.p=F) 
{
    logD <- sum(lgamma(alpha)) - lgamma(sum(alpha))
    s <- (alpha - 1) * log(x)
    re=sum(s) - logD
	if (!log.p) re = exp(re)
	re
}
dstat1<-function(t,pseudo=0) {
	L1<-ddirichlet((t+pseudo)/sum(t+pseudo),t+pseudo,T)
	L2<-ddirichlet((t+pseudo)/sum(t+pseudo),estb(t+pseudo),T)
	2*(L1-L2)
}
dstat2<-function(t,pseudo=0) {
	L1<-ddirichlet((t+pseudo)/sum(t+pseudo),t+pseudo+1,T)
	L2<-ddirichlet(estb((t+pseudo)/sum(t+pseudo)),(t+pseudo+1),T)
	2*(L1-L2)
}
dtest1<-function(t,pseudo=0) {
	pchisq(dstat1(t,pseudo),df=7,lower.tail=F)
}
dtest2<-function(t,pseudo=0) {
	pchisq(dstat2(t,pseudo),df=7,lower.tail=F)
}
estb<-function(t) { 
	a<-t[1:(length(t)/2)]
	b<-t[(length(t)/2+1):length(t)]
	c<-(a+b)/sum(a+b)
	c(sum(a)*c,sum(b)*c)
}


layout(t(1:5))

p<-runif(8); p<-c(p,p); p<-p/sum(p)
d<-t(rmultinom(10000,prob=p,size=100))
plot(ecdf(apply(d,1,dtest1,pseudo=1))); abline(0,1,col='red',lty=2)
lines(ecdf(apply(d,1,dtest2,pseudo=1E-8)),col='red')


p<-runif(16)
d<-t(rmultinom(10000,prob=p,size=100))
plot(ecdf(apply(d,1,dtest1,pseudo=1))); abline(0,1,col='red',lty=2)
lines(ecdf(apply(d,1,dtest2,pseudo=1E-8)),col='red')


p<-runif(8); p<-c(p,p); p[1]<-p[1]*2; p<-p/sum(p)
d<-t(rmultinom(10000,prob=p,size=1000))
plot(ecdf(apply(d,1,dtest1,pseudo=1))); abline(0,1,col='red',lty=2)
lines(ecdf(apply(d,1,dtest2,pseudo=1E-8)),col='red')

p<-runif(8)
d<-cbind(t(rmultinom(10000,prob=p,size=1000)),t(rmultinom(10000,prob=p,size=100)))
plot(ecdf(apply(d,1,dtest1,pseudo=1))); abline(0,1,col='red',lty=2)
lines(ecdf(apply(d,1,dtest2,pseudo=1E-8)),col='red')

d<-cbind(t(rmultinom(10000,prob=runif(8),size=1000)),t(rmultinom(10000,prob=runif(8),size=100)))
plot(ecdf(apply(d,1,dtest1,pseudo=1))); abline(0,1,col='red',lty=2)
lines(ecdf(apply(d,1,dtest2,pseudo=1E-8)),col='red')
