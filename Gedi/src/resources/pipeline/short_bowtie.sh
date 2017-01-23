#!/bin/bash

<?JS0

varin("wd","Working directory",true);
varin("tmp","Temp directory",true);
varin("tokens","Array of pipeline tokens (to resolve dependencies of programs)",true);
varin("name","Name for output files",true);
varin("datasets","Dataset definitions",true);
varin("references","Definitions of reference sequences",true);
varin("test","Test with the first 10k sequences",false);
varin("keeptrimmed","Keep the trimmed fastq files",false);

varout("reads","File name containing read mappings");

?>

<?JS

output.setExecutable(true);

var srrPat = Pattern.compile("SRR\\d+");
var nameToModeAndFiles = {};
var names = [];

var adapter;
for each (var d in datasets) {
	if (d.hasOwnProperty("gsm")) {
		var jsson = new LineIterator(new URL("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=sra&term="+d.gsm+"&retmode=json").openStream())
				.concat("");
		var json2 = DynamicObject.parseJson(jsson);
		var arr = json2.get(".esearchresult.idlist").asArray();
		if (arr.length!=1) throw new RuntimeException("Did not get a unique id for "+d.gsm+": "+json2);
		var xml = new LineIterator(new URL("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=sra&id="+arr[0].asString()+"&rettype=docsum").openStream())
			.concat("");
		var srrs = EI.wrap(srrPat.matcher(xml)).sort().toArray(String.class);
		log.info("SRA entry: Name: "+d.name+" gsm: "+d.gsm+" id: "+arr[0].asString()+" SRR: "+Arrays.toString(srrs));
		nameToModeAndFiles[d.name] = ["SRR",srrs,d.hasOwnProperty("adapter")?d.adapter:adapter];
	} else if (d.hasOwnProperty("sra")) {
		var srrs = StringUtils.split(d.sra.asString(), ',');
		log.info("SRA entry: Name: "+d.name+" srr: "+Arrays.toString(srrs));
		nameToModeAndFiles[d.name] = ["SRR",srrs,d.hasOwnProperty("adapter")?d.adapter:adapter];
	} else if (d.hasOwnProperty("fastq")) {
		var fastq = d.fastq;
		log.info("Fastq entry: Name: "+d.name+" fastq: "+fastq);
		nameToModeAndFiles[d.name] = ["FASTQ",[fastq],d.hasOwnProperty("adapter")?d.adapter:adapter];
	}
	names.push(d.name);
}

var id = name;
var tokens = tokens?tokens:[];

for (var name in nameToModeAndFiles) {
	var mode = nameToModeAndFiles[name][0];
	var files = EI.wrap(nameToModeAndFiles[name][1]).concat(" ");
	adapter = nameToModeAndFiles[name][2];
	processTemplate("short_bowtie1.sh",output.file.getParent()+"/"+name+".bash");
	prerunner(name); print(output.file.getParent()+"/"+name+".bash"); tokens.push(postrunner(name)); println(""); 
	
}
?>
	
<?JS prerunner(id,tokens) ?>gedi -e MergeCIT -c <?JS wd ?>/<?JS id ?>.cit <?JS print(EI.wrap(JS.array(names)).map(function(f) wd+"/"+f+".cit").concat(" ")); ?> <?JS var end=postrunner(id) ?> 

<?JS

name = id;
tokens = [end];
var reads = wd+"/"+id+".cit";


?> 
