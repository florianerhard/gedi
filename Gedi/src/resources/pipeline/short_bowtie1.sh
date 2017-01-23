#!/bin/bash

<?JS0

varin("references","Definitions of reference sequences",true);
varin("tmp","Temp directory",true);
varin("mode","SRR/FASTQ",true);
varin("test","Test with the first 10k sequences",false);
varin("keeptrimmed","Keep the trimmed fastq files",false);

varout("reads","File name containing read mappings");



?>

<?JS

output.executable=true;

var infos = new ArrayList();
for (var r in references) {
	var genomic = Genomic.get(r);
	var type = ParseUtils.parseEnumNameByPrefix(references[r], true, ReferenceType.class);
			
	if (type==ReferenceType.Both) {
		var ri = new Bowtie1ReferenceInfo(ReferenceType.Genomic,genomic);
		if (ri.index!=null)
			infos.add(ri);
		ri = new Bowtie1ReferenceInfo(ReferenceType.Transcriptomic,genomic);
		if (ri.index!=null)
			infos.add(ri);
		
	} else {
		var ri = new Bowtie1ReferenceInfo(type,genomic);
		if (ri.index!=null)
			infos.add(ri);
	}

	
}

Bowtie1ReferenceInfo.writeTable(output.file.getParent()+"/"+name+".prio.csv",infos,true,true);
processTemplate("merge_priority.oml",output.file.getParent()+"/"+name+".prio.oml");

var test;

?>

mkdir -p <?JS tmp ?>/<?JS name ?>
cd <?JS tmp ?>/<?JS name ?>

<?JS if (mode=="SRR") { ?>

fastq-dump <?JS if(test) print("-X 10000"); ?> -Z <?JS files ?> > <?JS name ?>.fastq
<?JS } else if (mode=="FASTQ" && !test) {  ?>
cp <?JS files ?> <?JS name ?>.fastq
<?JS } else if (mode=="FASTQ" && test) {  ?>
head -n40000 <?JS files ?> > <?JS name ?>.fastq
<?JS } ?>

<?JS
var adapter; 
if (adapter) { ?>
ADAPT=<?JS adapter ?>
<?JS } else { ?>
minion search-adapter -i <?JS name ?>.fastq -show 1 -write-fasta adapter.fasta 
ADAPT=`head -n2 adapter.fasta | tail -n1`
<?JS } ?>

reaper --nozip --noqc -clean-length 18 -3p-prefix 1/1/0/0 -swp 1/4/4 -geom no-bc -i <?JS name ?>.fastq -basename <?JS name ?>  -3pa $ADAPT 
perl -ne '$_="@".($n++)."\n" if ($l++%4==0); print' < <?JS name ?>.lane.clean > <?JS name ?>.fastq
rm <?JS name ?>.lint <?JS name ?>.lane.clean

<?JS for (var i=0; i<infos.length; i++)  
if (infos[i].priority==1) {
?>
bowtie -a -m 100 -v 3 --best --strata <?JS if (infos[i].norc) print("--norc"); ?> --un <?JS name ?>_unmapped.fastq --sam <?JS print(infos[i].index); ?> <?JS name ?>.fastq /dev/null 
mv <?JS name ?>_unmapped.fastq <?JS name ?>.fastq 
<?JS } ?>

pids=""
<?JS for (var i=0; i<infos.length; i++)  
if (infos[i].priority!=1){ ?>
	
bowtie -a -m 100 -v 3 --best --strata <?JS if (infos[i].norc) print("--norc"); ?> --sam <?JS print(infos[i].index); ?> <?JS name ?>.fastq <?JS print(infos[i].type) ?>.sam & 
pids="$pids $!"
<?JS } ?>
wait $pids

gedi -t . -e MergeSam -D -t <?JS print(output.file.getParent()); ?>/<?JS name ?>.prio.csv -prio <?JS print(output.file.getParent()); ?>/<?JS name ?>.prio.oml -chrM -o <?JS name ?>.cit

<?JS 
var keeptrimmed;
if (keeptrimmed) { ?>
mv <?JS name ?>.fastq <?JS wd ?>
<?JS } ?>

mv *.cit <?JS wd ?>

rm -rf <?JS tmp ?>/<?JS name ?>


cd <?JS wd ?>
echo '{"conditions":[{"name":"<?JS name ?>"}]}' > <?JS name ?>.cit.metadata.json
