#!/bin/bash

<?JS0

varin("wd","Working directory",true);
varin("tmp","Temp directory",true);
varin("tokens","Array of pipeline tokens (to resolve dependencies of programs)",true);
varin("name","Name for output files",true);
varin("references","Definitions of reference sequences",true);
varin("reads","Filename containing read mappings",true);

?>

<?JS
output.setExecutable(true);

reads = new File(reads).getAbsolutePath();
var tokens;
var genomes = "";
if (typeof references === 'string' || references instanceof String)
	genomes= references;
else {
	for (var r in references) 
		genomes = genomes+" "+r;
}
	



?>

<?JS prerunner(id+"price",tokens) ?>gedi -t <?JS tmp ?> -e Price -reads <?JS reads ?> -genomic <?JS genomes ?> -prefix <?JS wd ?>/price/<?JS name ?> -D <?JS var price = postrunner(id+"price") ?> 


<?JS

name = id;
tokens = [price];

?> 