
args=commandArgs(T)
t=read.delim(args[1])

pdf(args[2])
plot(ecdf(t$Posterior))
barplot(table(((t$Posterior>=0.9)+t$Before.in.posterior.bin+t$After.in.posterior.bin)>0,cut(t$Sinh.mean,breaks=c(0,0.1,0.5,1,5,Inf))),beside=T,main="Any >=0.9 per activity bin")
plot(ecdf(t$Before.in.posterior.bin[t$Posterior>=0.9]),main="Before detected start >=0.9")
plot(ecdf((t$After.in.posterior.bin/t$After.length)[t$Posterior>=0.9]),main="Fraction after detected start >=0.9")
plot(ecdf((t$After.in.posterior.bin)[t$Posterior>=0.9]),main="After detected start >=0.9")
dev.off()
