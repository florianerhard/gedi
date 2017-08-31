#!/bin/bash

<?JS0



varin("id","Output name",false);
varin("geo","GEO GSM id",true);
varin("test","Test with the first 10k sequences",false);

varout("reads","File name containing read mappings");


var id = id?id:geo;
var test;

var srrPat = Pattern.compile("SRR\\d+");
var jsson = new LineIterator(new URL("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=sra&term="+geo+"&retmode=json").openStream())
		.concat("");
var json2 = DynamicObject.parseJson(jsson);
var arr = json2.get(".esearchresult.idlist").asArray();
if (arr.length!=1) throw new RuntimeException("Did not get a unique id for "+geo+": "+json2);
var xml = new LineIterator(new URL("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=sra&id="+arr[0].asString()+"&rettype=docsum").openStream())
	.concat("");
var srrs = EI.wrap(srrPat.matcher(xml)).sort().toArray(String.class);
log.info("SRA entry: Name: "+id+" gsm: "+geo+" id: "+arr[0].asString()+" SRR: "+Arrays.toString(srrs));

var files = EI.wrap(srrs).concat(" ");

?>
fastq-dump <?JS if(test) print("-X 10000"); ?> -Z <?JS files ?> > <?JS id ?>.fastq
