#!/bin/bash

<?JS0

varin("wd","Working directory",true);
varin("tmp","Temp directory",true);
varin("tokens","Array of pipeline tokens (to resolve dependencies of programs)",true);
varin("name","Name for output files",true);
varin("references","Definitions of reference sequences",true);
varin("reads","Filename containing read mappings",true);
varin("startcodon","Treatment pairs for start codon prediction",false);

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
	


var startcodon;
if (startcodon) 
	startcodon = "-t "+startcodon.map(function(a) a.join("/")).join(" ");
else 
	startcodon = "";

?>
	
<?JS prerunner(id+"model",tokens) ?>gedi -t <?JS tmp ?> -e EstimateRiboModel -r <?JS reads ?> -g <?JS genomes ?>  -o <?JS wd ?>/price/<?JS name ?> -D <?JS var model = postrunner(id+"model") ?> 
<?JS prerunner(id+"err",[model]) ?>gedi -t <?JS tmp ?> -e EstimateModelError -r <?JS reads ?> -o <?JS wd ?>/price/<?JS name ?>.merged -D <?JS startcodon ?> -m <?JS wd ?>/price/<?JS name ?>.merged.model -g <?JS genomes ?> <?JS var err = postrunner(id+"err") ?> 
<?JS prerunner(id+"orfs",[err]) ?>gedi -t <?JS tmp ?> -e InferOrfs -r <?JS reads ?> -o <?JS wd ?>/price/<?JS name ?>.merged -D -m <?JS wd ?>/price/<?JS name ?>.merged.model -g <?JS genomes ?> <?JS var end = postrunner(id+"orfs") ?> 

<?JS

name = id;
tokens = [end];

?> 
