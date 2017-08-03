t=rep(0,2500)

model=c(0.22, 0.67, 0.11)

orf=function(t,start,end,n,model) {
	pos = start + floor(runif(n,min=0,max=(end-start)/3))*3+sample.int(3,n,replace=T,prob=model)-2
	tab = table(pos)
	tpos = as.integer(names(tab))
	t[tpos]=t[tpos]+tab
	t
}
