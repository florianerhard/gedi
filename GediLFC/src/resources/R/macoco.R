
suppressMessages(library(glmnet))
suppressMessages(library(MASS))

exx=read.delim(input)
exx=exx[exx$len>0,]

X=as.matrix(exx[,1:(dim(exx)[2]-2)])
y=exx$cov
w=rep(1,length(y))

for (i in 1:10) {
	b=coef(glmnet(X,y,lambda=0,intercept=FALSE,lower.limits=0,weights=w))[-1,1]
	fx=log(exx$cov+0.001)
	fy=log((y-X%*%b)^2)[,1]
	use=is.finite(fy) & is.finite(fx)
	fy=fy[use]; fx=fx[use]
	rfit=rlm(fy~fx)
	w=1/exp(predict(rfit,data.frame(fx=log(exx$cov+0.001))))
}


write.table(data.frame(Gene=names(exx)[1:(dim(exx)[2]-2)],b=b),output,sep="\t",quote=FALSE,row.names=FALSE,col.names=TRUE)

