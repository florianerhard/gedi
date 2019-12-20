#!/bin/bash

<?JS0

varin("wd","Working directory",true);
varin("reads","File containing reads",true);
varin("tokens","Array of pipeline tokens (to resolve dependencies of programs)",true);
varin("references","Definitions of reference sequences",true);
varin("referenceSequenceConversion","referenceSequenceConversion",true);
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

	var referenceSequenceConversion;
	
	var add = "";
	if (referenceSequenceConversion) {
			add=add+" --referenceSequenceConversion="+referenceSequenceConversion;
		}

?>

<?JS prerunner(id+".report",tokens) ?>gedi -e Stats -prefix <?JS wd ?>/report/<?JS print(FileUtils.getNameWithoutExtension(reads)); ?>. -g <?JS genomes ?><? add ?> <?JS reads?><?JS var end=postrunner(id+".report") ?>

mkdir -p <?JS wd ?>/counts
<?JS prerunner(id+".count",tokens) ?>gedi -e Stats -prefix <?JS wd ?>/counts/<?JS print(FileUtils.getNameWithoutExtension(reads)); ?>. -g <?JS genomes ?><? add ?> -count <?JS reads?><?JS var end2=postrunner(id+".count") ?>
