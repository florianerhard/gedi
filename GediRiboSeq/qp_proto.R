m<-matrix(c(0.7,0.2,0,0.1,0.7,0.2,0,0.1,0.7),3)
c=c(0,1000,0)
o<-apply(sapply(1:3,function(i) rmultinom(1,prob=m[,i],size=c[i])),1,sum)
library(quadprog)
C=rbind(c(0,0,0,1,1,1),cbind(diag(3),m),cbind(matrix(0,3,3),diag(3)))
D<-matrix(0,6,6)
diag(D[1:3,1:3])=1
diag(D[4:6,4:6])=0.000001
b=c(sum(o),o,0,0,0)
solve.QP(D,rep(0,6),t(C),b,3)



codonexp=c(1,1,1)
for (i in 1:100) codonexp<-apply(t(apply(t(t(m)*codonexp),1,function(v) v/sum(v)))*o,2,sum)
codonexp
