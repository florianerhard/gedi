
library(ggplot2)
library(reshape2)

t<-read.delim(paste(file,sep=''),check.names=F)

prep.tab<-function(aa,tab, stat) {
	a<-tab[ tab$`Amino acid`==aa & tab$Statistic==stat,]
	m<-a[,!(names(a) %in% c("Amino acid","Codon","Statistic"))]
#	m<-t(apply(m,1,function(v) v/colSums(m)))
	melt(data.frame(`Amino acid`=a$`Amino acid`,Codon=a$Codon,m,check.names=F),id.vars=c("Amino acid","Codon"),value.name="Occupancy",variable.name="Condition")
}

plot.aa<-function(aa, stat) {
	tab<-prep.tab(aa,t,stat)
	print(ggplot(tab,aes(Condition,Occupancy,fill=Condition))+geom_bar(stat="identity",position='dodge')+facet_wrap(~Codon)+ggtitle(sprintf("%s (%s)",aa,stat))+theme(axis.text.x = element_text(angle = 90, hjust = 1), legend.position="none"))
}

pdf(paste(file,".pdf",sep=''))

for (stat in unique(t$Statistic)) for (aa in unique(t$`Amino acid`)) plot.aa(aa,stat)
dev.off()
