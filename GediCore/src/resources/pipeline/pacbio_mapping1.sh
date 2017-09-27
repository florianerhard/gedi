#!/bin/bash

<?JS0

varin("references","Definitions of reference sequences",true);
varin("tmp","Temp directory",true);
varin("mode","SRR/FASTQ",true);
varin("test","Test with the first 10k sequences",false);
varin("nthreads","Number of threads (Default: 8)",false);
varin("minlength","Minimal length of reads to keep (default: 18)",false);
varin("keepUnmapped","Keep the unmapped reads in a fasta file",false);
varin("poly","Only look for polyA (or T) instead of looking for the linker sequence",false);
varin("nofilter","Do not look for polyA/T nor linker",false);

varout("reads","File name containing read mappings");



?>

<?JS
var poly;
var nofilter;
var keepUnmapped;
var minlength=minlength?minlength:18;
var nthreads = nthreads?nthreads:Math.min(8,Runtime.getRuntime().availableProcessors());

output.executable=true;

var infos = ReadMappingReferenceInfo.writeTable(output.file.getParent()+"/"+name+".prio.csv",references,true,true,ReadMapper.STAR);
processTemplate("merge_priority.oml",output.file.getParent()+"/"+name+".prio.oml");

var test;

?>

mkdir -p <?JS tmp ?>/<?JS name ?>
cd <?JS tmp ?>/<?JS name ?>


<?JS if (mode=="SRR") { ?>
fastq-dump <?JS if(test) print("-X 10000"); ?> -Z <?JS files ?> > <?JS name ?>.fastq
<?JS } else if (mode=="FASTQ" && !test && files.endsWith(".gz")) {  ?>
zcat <?JS files ?> > <?JS name ?>.fastq
<?JS } else if (mode=="FASTQ" && test && files.endsWith(".gz")) {  ?>
zcat <?JS files ?> | head -n40000  > <?JS name ?>.fastq
<?JS } else if (mode=="FASTQ" && !test && files.endsWith(".bz2")) {  ?>
bzcat <?JS files ?> > <?JS name ?>.fastq
<?JS } else if (mode=="FASTQ" && test && files.endsWith(".bz2")) {  ?>
bzcat <?JS files ?> | head -n40000  > <?JS name ?>.fastq
<?JS } else if (mode=="FASTQ" && !test) {  ?>
cp <?JS files ?> <?JS name ?>.fastq
<?JS } else if (mode=="FASTQ" && test) {  ?>
head -n40000 <?JS files ?> > <?JS name ?>.fastq
<?JS } ?>


echo -e "Category\tCount" > <?JS name ?>.reads.tsv
echo -ne "All\t" >> <?JS name ?>.reads.tsv

L=`wc -l <?JS name ?>.fastq | cut -f1 -d' '`
leftreads=$((L / 4))
echo $leftreads >> <?JS name ?>.reads.tsv

<?JS if (!nofilter) { ?>
gedi -e PacBioRnaSeq <?JS if (poly) { print("-poly");} ?> <?JS name ?>.fastq
mv <?JS name ?>.trimmed.fastq <?JS name ?>.fastq
<?JS } ?>

gedi -e FastqFilter -D -ld <?JS name ?>.readlengths.tsv -min <?JS minlength ?> <?JS name ?>.fastq > <?JS name ?>_filtered.fastq
mv <?JS name ?>_filtered.fastq <?JS name ?>.fastq

# rRNA removal
<?JS for (var i=0; i<infos.length; i++)  
if (infos[i].priority==1) {
	println(ReadMapper.STAR.getPacBioCommand(infos[i],name+".fastq","/dev/null",name+"_unmapped.fastq",nthreads));
?>
mv <?JS name ?>_unmapped.fastq <?JS name ?>.fastq
echo -ne "rRNA removal\t" >> <?JS name ?>.reads.tsv
leftreads=$( grep -c @ <?JS name ?>.fastq )
echo $leftreads >> <?JS name ?>.reads.tsv
<?JS } ?>


# mapping
<?JS for (var i=0; i<infos.length; i++)  
if (infos[i].priority!=1){ 
	println(ReadMapper.STAR.getPacBioCommand(infos[i],name+".fastq",infos[i].type+".sam",null,nthreads));
	println("");
?>

<?JS if (nthreads>1) { ?>
samtools sort -o <?JS print(infos[i].type) ?>.sort.sam -n -T ./sort -@ <?JS nthreads ?> <?JS print(infos[i].type) ?>.sam 
mv <?JS print(infos[i].type) ?>.sort.sam <?JS print(infos[i].type) ?>.sam
unali=$( grep -v @ <?JS print(infos[i].type) ?>.sam | cut -f2 | grep -c "^4$" )
echo -ne "<?JS print(infos[i].name) ?>\t" >> <?JS name ?>.reads.tsv
echo $((leftreads-unali)) >> <?JS name ?>.reads.tsv


<?JS } ?>
<?JS } ?>

mkdir -p <?JS wd ?>/report

# Merging
gedi -t . -e MergeSam -D -t <?JS print(output.file.getParent()); ?>/<?JS name ?>.prio.csv -prio <?JS print(output.file.getParent()); ?>/<?JS name ?>.prio.oml -chrM -o <?JS name ?>.cit <?JS if (keepUnmapped) { print("-unmapped");} ?>
echo -ne "Merged\t" >> <?JS name ?>.reads.tsv
gedi Nashorn -e "println(EI.wrap(DynamicObject.parseJson(FileUtils.readAllText(new File('<?JS name ?>.cit.metadata.json'))).getEntry('conditions').asArray()).mapToDouble(function(d) d.getEntry('total').asDouble()).sum())" >> <?JS name ?>.reads.tsv


mv *.readlengths.* <?JS wd ?>/report
mv <?JS name ?>.reads.tsv <?JS wd ?>/report

if [ -f <?JS name ?>.cit ]; then
   mv <?JS name ?>.cit* <?JS wd ?>
<?JS if (keepUnmapped) { ?>
   mv <?JS name ?>.unmapped.fasta <?JS wd ?>
<?JS if (!nofilter) { ?>
   mv <?JS name ?>.untrimmed.fastq <?JS wd ?>
<?JS } } ?>
   rm -rf <?JS tmp ?>/<?JS name ?>
else
   (>&2 echo "There were some errors, did not delete temp directory!")
fi

cd <?JS wd ?>

<?JS
var png = output.file.getParentFile().getParentFile()+"/report/"+name+".reads.png";
var table = output.file.getParentFile().getParentFile()+"/report/"+name+".reads.tsv"
processTemplate("plot_mappingstatistics.R",output.file.getParentFile().getParentFile()+"/report/"+name+".reads.R");
?>
Rscript report/<?JS name ?>.reads.R
echo '{"plots":[{"section":"Mapping statistics","id":"mapping<? print(StringUtils.toJavaIdentifier(name)) ?>","title":"<? name ?>","description":"How many reads are removed by adapter trimming and rRNA mapping, how many are mapped to which reference and retained overall.","img":"<? name ?>.reads.png","script":"<? name ?>.reads.R","csv":"<? name ?>.reads.tsv"}]}' > report/<? name ?>.reads.report.json 

