#!/bin/bash

<?JS0

varin("references","Definitions of reference sequences",true);
varin("tmp","Temp directory",true);
varin("mode","SRR/FASTQ",true);
varin("test","Test with the first 10k sequences",false);
varin("nthreads","Number of threads (Default: 8)",false);
varin("minlength","Minimal length of reads to keep (default: 18)",false);
varin("keepUnmapped","Keep the unmapped reads in a fasta file",false);
varin("keepbams","Keep the bam files",false);
varin("keepfastq","Keep the fastq files",false);
varin("sharedMem","Use shared memory for STAR",false);
varin("extract","Extract part of read",false);
varin("nosoft","No softclipping",false);
varin("adapter","Only for single end!",false);
varin("introns","All/Annotated/None",false);
varin("smartseq","*Full length* smart seq library, i.e. trim first 3bp",false);
varin("umi","Reads start with umis (umi-len,spacer-len,umi2-len,spacer2-len; 6,4 for Lexogen UMIs!)",false);
varin("citparam","e.g. -novar -nosec",false);
varin("starindex","folder of a combined STAR index",false);
varin("starparam","additional parameter for STAR",false);
varin("adapter1","Forward adapter sequence",false);
varin("adapter2","Backward adapter sequence",false);


varout("reads","File name containing read mappings");

?>

<?JS
var novar;
var starparam = starparam?starparam:"";
var nosec;
var introns;
var keepUnmapped;
var minlength=minlength?minlength:18;
var nthreads = nthreads?nthreads:Math.min(8,Runtime.getRuntime().availableProcessors());
var sharedMem;
var pairedend;
var nosoft;
var smartseq;
var umi;

var extract;
var extractparam = extract?"-extract "+extract+" ":"";
output.executable=true;

var intronparam = "";
if (introns && introns.toLowerCase().startsWith("no")) intronparam = "--alignSJDBoverhangMin 9999 --alignSJoverhangMin 9999";
else if (introns && introns.toLowerCase().startsWith("ann")) intronparam = "--alignIntronMax 1";

var citparam = citparam?citparam:"";

var alimode="Local";
if (nosoft) alimode=nosoft;
if (nosoft==true) alimode="EndToEnd";
	


var getPrio=function(r) ParseUtils.parseEnumNameByPrefix(references[r], true, ReferenceType.class).prio;
var garray = EI.wrap(DynamicObject.from(references).getProperties()).filter(function(i) getPrio(i)>1).toArray();
var rrnaarray = EI.wrap(DynamicObject.from(references).getProperties()).filter(function(i) getPrio(i)==1).toArray();
var genomes = EI.wrap(garray).concat(" ");

//var rrna;
var starindex;
if (!starindex && garray.length==1) starindex=ReadMapper.STAR.getIndex(Genomic.get(garray[0]),null);	
if (!starindex) throw new RuntimeException("Specify a starindex! You can create one by gedi -e GenomicUtils -p -m star -g "+genomes);
//if (rrnaarray.length>1) throw new RuntimeException("More than 1 rRNA index given!");
//if (rrnaarray.length==1) rrna = ReadMapper.STAR.getIndex(Genomic.get(rrnaarray[0]),null);
	
var fastqname = name+".fastq";

var test;
var keepbams;
var keepfastq;

?>


mkdir -p <?JS tmp ?>/<?JS name ?>
cd <?JS tmp ?>/<?JS name ?>

<?JS if (mode=="SRR") { ?>
RETRY=1; until fastq-dump --split-files <?JS if(test) print("-X 100000"); ?> <?JS files ?>; do echo Retry after $RETRY; sleep $RETRY; RETRY=$((RETRY*2)); done
<?JS 
fastqname="*.fastq";
} else if (mode=="FASTQ") {  

var ff = [files];
var fqs = [fastqname]

if (pairedend) {
	if (ff.length!=1) throw new RuntimeException("Paired-end reads must be in two files!");
	var fp = FileUtils.findPartnerFile(ff[0],pairedend);
	log.info("Found partner files: "+ff[0]+" "+fp); 
	ff=[ff[0],fp];
	fqs=[name+"_1.fastq",name+"_2.fastq"];
	fastqname = fqs.join(" ");	
}

for (var find=0; find<ff.length; find++) {
	var f1 = ff[find];
	var fq1 = fqs[find];
?>
	<?JS if (!test && f1.endsWith(".gz")) {  ?>
	zcat <?JS f1 ?> > <?JS fq1 ?>
	<?JS } else if (test && f1.endsWith(".gz")) {  ?>
	zcat <?JS f1 ?> | head -n400000 > <? fq1 ?>
	<?JS } else if (!test && f1.endsWith(".bz2")) {  ?>
	bzcat <?JS f1 ?> > <?JS fq1 ?>
	<?JS } else if (test && f1.endsWith(".bz2")) {  ?>
	bzcat <?JS f1 ?> | head -n400000  > <?JS fq1 ?>
	<?JS } else if (!test) {  ?>
	cp <?JS f1 ?> <?JS fq1 ?>
	<?JS } else if (test) {  ?>
	head -n400000 <?JS f1 ?> > <?JS fq1 ?>
	<?JS }
}
} ?>


echo -e "Category\tCount" > <?JS name ?>.reads.tsv
echo -ne "All\t" >> <?JS name ?>.reads.tsv

L=`wc -l <?JS fastqname ?> | head -n1 | awk '{print $1}'`
leftreads=$((L / 4))
echo $leftreads >> <?JS name ?>.reads.tsv

<?JS
var adapter;
if (adapter) { ?>
reaper --nozip --noqc -3p-prefix 1/1/0/0 -swp 1/4/4 -geom no-bc -i <?JS fastqname ?> -basename <?JS name ?>  -3pa <? adapter ?>
mv <? name ?>.lane.clean <?JS fastqname ?>
rm <?JS name ?>.lint
<? } ?>



<?JS
var adapter1;
var adapter2;
if (adapter1 && adapter2) { ?>
TRIMMER=$( ls -1 `echo $PATH | sed -e 's/:/\/trimmomatic*.jar /'g ` 2>/dev/null | head -n1 )
if [ ! -f $TRIMMER ]; then
	(>&2 echo "Trimmomatic not found, did not trim reads!")
else
	echo -e ">a/1\n<? adapter1 ?>\n>a/2\n<? adapter2 ?>" > adapter.fasta
	java -jar $TRIMMER PE -threads <? nthreads ?> <?JS fastqname ?> -baseout trimmed ILLUMINACLIP:adapter.fasta:2:30:10:1:true
	mv trimmed_1P <? name ?>_1.fastq <? //print(fqs[0]) ?>
	mv trimmed_2P <? name ?>_2.fastq <? //print(fqs[1]) ?>
	<? fastqname=name+"_1.fastq "+name+"_2.fastq"; ?>
fi
<? } ?>


gedi -t . -e FastqFilter -D <?extractparam?>-overwrite<? if (smartseq) print(" -smartseq"); ?><? if (umi) print(" -umi "+umi); ?> -ld <?JS name ?>.readlengths.tsv -min <?JS minlength ?> <?JS fastqname ?>


	echo -ne "Trimmed\t" >> <?JS name ?>.reads.tsv
	L=`wc -l <?JS fastqname ?> | head -n1 | awk '{print $1}'`
	leftreads=$((L / 4))
	echo $leftreads >> <?JS name ?>.reads.tsv


<?JS for each (var index in rrnaarray) { 
rrna = ReadMapper.bowtie2.getIndex(Genomic.get(index),null);
?>


# <? index ?> removal
#STAR --runMode alignReads --runThreadN <? nthreads ?>  --seedPerWindowNmax 30 --alignSJDBoverhangMin 9999 --alignSJoverhangMin 9999 --genomeDir <? rrna ?> --readFilesIn <? fastqname ?> --outSAMmode NoQS --alignEndsType <? alimode ?> --outReadsUnmapped Fastx

<?
if (pairedend) { ?>
bowtie2 -p <? nthreads ?> --un-conc un --local -x <? rrna ?> -1 <? print(fqs[0]) ?> -2 <? print(fqs[1]) ?> > /dev/null  
mv un.1 <? print(fqs[0]) ?>
mv un.2 <? print(fqs[1]) ?>
<?
} else { ?>
bowtie2 -p <? nthreads ?> --un un --local -x <? rrna ?> -U <? print(fqs[0]) ?> > /dev/null  
mv un <? print(fqs[0]) ?>
<? } ?>

	echo -ne "filtered <? index ?>\t" >> <?JS name ?>.reads.tsv
	L=`wc -l <?JS fastqname ?> | head -n1 | awk '{print $1}'`
	leftreads=$((L / 4))
	echo $leftreads >> <?JS name ?>.reads.tsv
<? } ?>


# mapping
STAR --runMode alignReads --runThreadN <? nthreads ?>  <? intronparam ?> --genomeDir <? starindex ?> <?JS print(sharedMem?"--genomeLoad LoadAndKeep":""); ?> --limitBAMsortRAM 8000000000 <? starparam ?> --readFilesIn <? fastqname ?> --outSAMmode NoQS --outSAMtype BAM SortedByCoordinate --alignEndsType <? alimode ?> --outSAMattributes nM MD NH  <?JS print(keepUnmapped?"--outReadsUnmapped Fastx":""); ?>
mv Aligned.sortedByCoord.out.bam <? name ?>.bam
<?JS
			if (keepUnmapped) {
				if (pairedend) {
?>
mv Unmapped.out.mate1 <? name ?>.unmapped_1.fastq
mv Unmapped.out.mate2 <? name ?>.unmapped_2.fastq
<?JS			} else { ?>
mv Unmapped.out.mate1 <? name ?>.unmapped.fastq
<?JS			} 
			}
?>

echo -ne "Unique\t" >> <?JS name ?>.reads.tsv
grep "Uniquely mapped reads number"  Log.final.out | cut -f2 -d'|' | awk '{ print $1}' >> <?JS name ?>.reads.tsv
echo -ne "Multi\t" >> <?JS name ?>.reads.tsv
grep "Number of reads mapped to multiple loci"  Log.final.out | cut -f2 -d'|' | awk '{ print $1}' >> <?JS name ?>.reads.tsv

mkdir -p <?JS wd ?>/report

samtools index <? name ?>.bam

echo -ne "Mitochondrial\t" >> <?JS name ?>.reads.tsv
gedi -t . -e Bam2CIT<? if (umi) print(" -umi"); ?> <? citparam ?> <? name ?>.cit <? name ?>.bam | tail -n1 | awk '{ print $2 }'>> <?JS name ?>.reads.tsv
gedi -t . -e CorrectCIT <? name ?>.cit
echo -ne "CIT\t" >> <?JS name ?>.reads.tsv
gedi -t . -e ReadCount -g <? genomes ?> -m Weight <? name ?>.cit | tail -n1 | awk '{ print $2 }'>> <?JS name ?>.reads.tsv

mv *.readlengths.* <?JS wd ?>/report

<?JS if (umi) { ?>
gedi -t . -e DedupUMI -prefix <? name ?> -plot <? name ?>.cit
echo -ne "Dedup\t" >> <?JS name ?>.reads.tsv
gedi -t . -e ReadCount -g <? genomes ?> -m Weight <? name ?>.cit | tail -n1 | awk '{ print $2 }'>> <?JS name ?>.reads.tsv

mv <?JS name ?>.dedup* <?JS wd ?>/report
<?JS } ?>

mv <?JS name ?>.reads.tsv <?JS wd ?>/report

if [ -f <?JS name ?>.cit ]; then
   mv <?JS name ?>.cit* <?JS wd ?>
<?JS if (keepUnmapped) { ?>
   mkdir -p <?JS wd ?>/unmapped
   mv <?JS name ?>.unmapped*.fastq <?JS wd ?>/unmapped
<?JS } ?>
<?JS if (keepbams) { ?>
   mkdir -p <?JS wd ?>/bams
   mv <? name ?>.bam* <?JS wd ?>/bams
<?JS } ?>
<?JS if (keepfastq) { ?>
   mkdir -p <?JS wd ?>/fastq
   mv *.fastq <?JS wd ?>/fastq
   cd <?JS wd ?>/fastq
   gzip *.fastq
<?JS } ?>
   cd <?JS wd ?>
   rm -rf <?JS tmp ?>/<?JS name ?>
else
   (>&2 echo "There were some errors, did not delete temp directory!")
   cd <?JS wd ?>
fi



<?JS
var png = output.file.getParentFile().getParentFile()+"/report/"+name+".reads.png";
var table = output.file.getParentFile().getParentFile()+"/report/"+name+".reads.tsv"
processTemplate("plot_mappingstatistics.R",output.file.getParentFile().getParentFile()+"/report/"+name+".reads.R");
?>
Rscript report/<?JS name ?>.reads.R
echo '{"plots":[{"section":"Mapping statistics","id":"mapping<? print(StringUtils.toJavaIdentifier(name)) ?>","title":"<? name ?>","description":"How many reads are removed by adapter trimming and rRNA mapping, how many are mapped to which reference and retained overall.","img":"<? name ?>.reads.png","script":"<? name ?>.reads.R","csv":"<? name ?>.reads.tsv"}]}' > report/<? name ?>.reads.report.json 

