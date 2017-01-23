
library(ggplot2)
library(reshape2)

t2<-read.delim('orfstats/wt_2.merged.orfs.codonusage_cds.stat',check.names=F)
t3<-read.delim('orfstats/wt_3.merged.orfs.codonusage_cds.stat',check.names=F)
t2$Replicate="A"
t3$Replicate="B"    
t2<-t2[,!grepl("harr|ltm|hrt",names(t2))]
t3<-t3[,!grepl("harr|ltm|hrt",names(t3))]

prep.tab<-function(aa,tab, stat) {
	a<-tab[ tab$`Amino acid`==aa & tab$Statistic==stat,]
	m<-a[,!(names(a) %in% c("Amino acid","Codon","Replicate","Statistic"))]
#	m<-t(apply(m,1,function(v) v/colSums(m)))
	melt(data.frame(`Amino acid`=a$`Amino acid`,Codon=a$Codon,Replicate=a$Replicate,m,check.names=F),id.vars=c("Amino acid","Codon","Replicate"),value.name="Occupancy",variable.name="Condition")
}

plot.aa<-function(aa, stat) {
	tab<-rbind(prep.tab(aa,t2, stat),prep.tab(aa,t3, stat))
	print(ggplot(tab,aes(Replicate,Occupancy,fill=Condition))+geom_bar(stat="identity",position='dodge')+facet_wrap(~Codon)+ggtitle(sprintf("%s (%s)",aa,stat)))
}


pdf("orfstats/codon_usage.pdf")
for (stat in unique(rbind(t2,t3)$Statistic)) for (aa in unique(rbind(t2,t3)$`Amino acid`)) plot.aa(aa,stat)
dev.off()
