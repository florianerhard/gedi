#!/bin/bash

<?JS0

varin("references","Definitions of reference sequences",true);
varin("tmp","Temp directory",true);
varin("mode","SRR/FASTQ",true);
varin("mapper","Mapping method",false);
varin("test","Test with the first 10k sequences",false);
varin("keeptrimmed","Keep the trimmed fastq files",false);
varin("keepbams","Keep the bam files",false);
varin("nthreads","Number of threads (Default: 8)",false);
varin("minlength","Minimal length of reads to keep (default: 18)",false);
varin("maxMismatch","Maximal number of mismatches (default: 3)",false);
varin("adapter","Adapter sequence (default: infer with minion)",false);
varin("smartseq","Full length smart seq library, i.e. trim first 3bp",false);
varin("trimmed","Whether data is already adapter trimmed",false);
varin("barcodes","Json describing barcodes (Default: undef)",false);
varin("keepUnmapped","Keep the unmapped reads in a fasta file",false);
varin("keeptmp","Keep the tmp directory",false);

varout("reads","File name containing read mappings");



?>

<?JS
var keepUnmapped;
var keeptmp;
var minlength=minlength?minlength:18;
var nthreads = nthreads?nthreads:Math.min(8,Runtime.getRuntime().availableProcessors());
var barcodes;
var mapper = mapper?mapper:"bowtie";
var smartseq;
var maxMismatch=maxMismatch?maxMismatch:3;

if (mapper=="STAR")
	System.out.println("We strongly advice against using STAR for mapping short reads (very very bad things will happen)!");

var smapper = mapper;
mapper = ParseUtils.parseEnumNameByPrefix(mapper,true,ReadMapper.class);
if (mapper==null) throw new RuntimeException("Mapper "+smapper+" unknown!");
output.executable=true;

var infos = ReadMappingReferenceInfo.writeTable(output.file.getParent()+"/"+name+".prio.csv",references,true,true,mapper);
processTemplate("merge_priority.oml",output.file.getParent()+"/"+name+".prio.oml");


var genomes = EI.wrap(infos).filter(function(i) i.priority>1).map(function(i) i.getGenomic().getId()).unique(false).reduce(function(a,b) a+" "+b);

var test;

var keepbams;

if (keepbams) {
?>
mkdir -p <?JS wd ?>/bams
<? } ?>

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

<?JS
var trimmed;
if (!trimmed) {
var adapter; 
if (adapter) { ?>
ADAPT=<?JS adapter ?>
<?JS } else { ?>
head -n4000000 <?JS name ?>.fastq > <?JS name ?>.head.fastq
minion search-adapter -i <?JS name ?>.head.fastq -show 1 -write-fasta adapter.fasta
rm <?JS name ?>.head.fastq 
ADAPT=`head -n2 adapter.fasta | tail -n1`
cp adapter.fasta <?JS wd ?>/log/<? name ?>.adapter.fasta
<?JS } ?>

reaper --nozip --noqc -3p-prefix 8/2/0/2 -geom no-bc -i <?JS name ?>.fastq -basename <?JS name ?>  -3pa $ADAPT
gedi -e FastqFilter -D -ld <?JS name ?>.readlengths.tsv <? if (smartseq) print("-smartseq"); ?> -min <?JS minlength ?> <?JS name ?>.lane.clean > <?JS name ?>.fastq
rm <?JS name ?>.lint <?JS name ?>.lane.clean

echo -ne "Trimmed\t" >> <?JS name ?>.reads.tsv
L=`wc -l <?JS name ?>.fastq | cut -f1 -d' '`
leftreads=$((L / 4))
echo $leftreads >> <?JS name ?>.reads.tsv

<? } else {?>
gedi -e FastqFilter -D -ld <?JS name ?>.readlengths.tsv -min <?JS minlength ?> <?JS name ?>.fastq > <?JS name ?>_filtered.fastq
mv <?JS name ?>_filtered.fastq <?JS name ?>.fastq
<? } ?>


<?JS
if (barcodes) {
	FileUtils.writeAllText(JSON.stringify(barcodes),new File(output.file.getParent()+"/"+name+".barcodes.json"));
?>
gedi -t . -e ExtractBarcodes -D -json <?JS print(new File(output.file.getParent()+"/"+name+".barcodes.json")) ?> <?JS name ?>.fastq <?JS name ?>.collapsed
mv <?JS name ?>.collapsed.fastq <?JS name ?>.fastq
echo -ne "Collapsed\t" >> <?JS name ?>.reads.tsv
L=`wc -l <?JS name ?>.fastq | cut -f1 -d' '`
leftreads=$((L / 4))
echo $leftreads >> <?JS name ?>.reads.tsv
mv <?JS name ?>.collapsed.png <?JS name ?>.collapsed.R <?JS name ?>.collapsed.report.json <?JS name ?>.collapsed.tsv <?JS wd ?>/report
	
<?JS
}
?>

<?JS for (var i=0; i<infos.length; i++)  
if (infos[i].priority==1) {
	println(ReadMapper.bowtie.getShortReadCommand(new ReadMappingReferenceInfo(ReferenceType.rRNA,infos[i].getGenomic(),ReadMapper.bowtie),name+".fastq","/dev/null",name+"_unmapped.fastq",nthreads,3));
?>
mv <?JS name ?>_unmapped.fastq <?JS name ?>.fastq
echo -ne "rRNA removal\t" >> <?JS name ?>.reads.tsv
L=`wc -l <?JS name ?>.fastq | cut -f1 -d' '`
leftreads=$((L / 4))
echo $leftreads >> <?JS name ?>.reads.tsv

<?JS } ?>


<?JS for (var i=0; i<infos.length; i++)  
if (infos[i].priority!=1){ 
	if (infos[i].mapper==null) throw new RuntimeException("Mapper unknown!");
	println(infos[i].mapper.getShortReadCommand(infos[i],name+".fastq",infos[i].type+".sam",null,nthreads,maxMismatch));
	if (keepbams) {
		infos[i].type+".sam" 
		?>
samtools view -b <? print(infos[i].type+".sam") ?> > <? print(infos[i].type+".bam") ?>
samtools sort <? print(infos[i].type+".bam") ?> > <? print(name+"."+infos[i].type+".bam") ?>
samtools index <? print(name+"."+infos[i].type+".bam") ?>
rm <? print(infos[i].type+".bam") ?>
		<?JS

	}
?>
<?JS if (nthreads>1) { ?>
samtools sort -o <?JS print(infos[i].type) ?>.sort.sam -n -T ./sort -@ <?JS nthreads ?> <?JS print(infos[i].type) ?>.sam 
mv <?JS print(infos[i].type) ?>.sort.sam <?JS print(infos[i].type) ?>.sam
<?JS } ?>
ali=$( samtools view -F 4 <?JS print(infos[i].type) ?>.sam | cut -f1 | uniq | wc -l )
echo -ne "<?JS print(infos[i].name) ?>\t" >> <?JS name ?>.reads.tsv
echo $ali >> <?JS name ?>.reads.tsv
<?JS } ?>

mkdir -p <?JS wd ?>/report

<?JS if (barcodes) { 
var png = output.file.getParentFile().getParentFile()+"/report/"+name+".barcodecorrection.png";
var table = output.file.getParentFile().getParentFile()+"/report/"+name+".barcodecorrection.tsv"
processTemplate("plot_barcodes.R",output.file.getParentFile().getParentFile()+"/report/"+name+".barcodecorrection.R");
?>
gedi -t . -e MergeSam -D -genomic <?JS genomes ?> -t <?JS print(output.file.getParent()); ?>/<?JS name ?>.prio.csv -prio <?JS print(output.file.getParent()); ?>/<?JS name ?>.prio.oml -chrM -o <?JS name ?>.cit -bcjson <?JS print(new File(output.file.getParent()+"/"+name+".barcodes.json")) ?> -bcfile <?JS name ?>.collapsed.barcodes  <?JS if (keepUnmapped) { print("-unmapped");} ?>
cp <?JS name ?>.barcodecorrection.* <?JS wd ?>/report
<?JS } else { ?>
gedi -t . -e MergeSam -D -genomic <?JS genomes ?> -t <?JS print(output.file.getParent()); ?>/<?JS name ?>.prio.csv -prio <?JS print(output.file.getParent()); ?>/<?JS name ?>.prio.oml -chrM -o <?JS name ?>.cit  <?JS if (keepUnmapped) { print("-unmapped");} ?>
<?JS } ?>
echo -ne "Merged\t" >> <?JS name ?>.reads.tsv
gedi Nashorn -e "println(EI.wrap(DynamicObject.parseJson(FileUtils.readAllText(new File('<?JS name ?>.cit.metadata.json'))).getEntry('conditions').asArray()).mapToDouble(function(d) d.getEntry('total').asDouble()).sum())" >> <?JS name ?>.reads.tsv


mv *.readlengths.* <?JS wd ?>/report
mv <?JS name ?>.reads.tsv <?JS wd ?>/report

if [ -f <?JS name ?>.cit ]; then
   mv <?JS name ?>.cit* <?JS wd ?>
<?JS if (keepUnmapped) { ?>
   mv <?JS name ?>.unmapped.fasta <?JS wd ?>
<?JS } ?>
<?JS if (keepbams) { ?>
   mv *.bam* <?JS wd ?>/bams
<?JS } ?>
<?JS 
var keeptrimmed;
if (keeptrimmed) { ?>
   mv <?JS name ?>.fastq <?JS wd ?>
<?JS } ?>

<?JS if (!keeptmp) { ?>
   rm -rf <?JS tmp ?>/<?JS name ?>
<? } ?>
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

<?JS if (barcodes) { ?>
Rscript report/<?JS name ?>.barcodecorrection.R
echo '{"plots":[{"section":"Barcodes","id":"barcodes<? print(StringUtils.toJavaIdentifier(name)) ?>","title":"<? name ?>","description":"How many original reads correspond to the retained mappings, how many distict random barcodes, how many corrected barcodes and how many corrected barcodes mapping to one of the library barcodes.","img":"<? name ?>.barcodecorrection.png","script":"<? name ?>.barcodecorrection.R","csv":"<? name ?>.barcodecorrection.tsv"}]}' > report/<? name ?>.barcodecorrection.report.json 
<?JS } ?>
