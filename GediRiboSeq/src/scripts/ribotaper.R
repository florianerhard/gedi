# ada[ted from Calviello L, Mukherjee N, Wyler E, Zauber H, Hirsekorn A, Selbach M, et al. Detecting actively translated open reading frames in ribosome profiling data. Nat Meth. 2016 Feb;13(2):165–70. 

# x is the vector of codon activities for all three frames, from start to stop codon
ribotaper<-function(x,n_tapers=24,time_bw=12){
    length=length(x)
if(length<25){slepians<-dpss(n=length+(50-length),k=n_tapers,nw=time_bw)}
    if(length>=25){slepians<-dpss(n=length,k=24,nw=12)}
    if(length(x)<25){
                remain<-50-length(x)
x<-c(rep(0,as.integer(remain/2)),x,rep(0,remain%%2+as.integer(remain/2)))
        }
        if(length(x)<1024/2){padding<-1024}
        if(length(x)>=1024/2){padding<-"default"}
        resSpec1 <- spec.mtm(as.ts(x), k=n_tapers, nw=time_bw, nFFT = padding, Ftest = TRUE, plot=F,dpssIN=slepians)

Fmax_3nt<-resSpec1$mtm$Ftest[which(abs((resSpec1$freq-(1/3)))==min(abs((resSpec1$freq-(1/3)))))]
P_3nt<-(pf(q=Fmax_3nt,df1=2,df2=(2*n_tapers)-2,lower.tail=F))
Spec_3nt<-resSpec1$spec[which(abs((resSpec1$freq-(1/3)))==min(abs((resSpec1$freq-(1/3)))))]

        return(list(pval=P_3nt,spec=Spec_3nt))
}

