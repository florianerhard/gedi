plotmatrix<-function(m,x,...) {
	plot(NA,xlim=range(x),ylim=range(m),ylab="Weighted total count",...)
	for (i in 1:dim(m)[1]) lines(x,m[i,],col=col[i])
}

s<-length(names)
col<-rainbow(s)


layout(rbind(c(1,2,3),c(4,4,4)),widths=c(1,1,2))
op<-par(mar=c(5,15,4,2))
barplot(unique,col=col,names.arg=names,horiz=T,las=1,main="Unique counts in clusters")
par(op)

barplot(w,col=col,horiz=T,main="Weights")

plotmatrix(graphs,x=1:dim(graphs)[2],main="Read coverage",xlab="Read base")

r<-1:(dim(contexts)[2]/2)
plotmatrix(contexts,x=c(-rev(r),r),main=paste("Best context coverage, id =",id),xlab="Context")
abline(v=0,lwd=3)
