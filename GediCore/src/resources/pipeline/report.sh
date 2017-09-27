#!/bin/bash

<?JS0

varin("wd","Working directory",true);
varin("reads","File containing reads",true);
varin("tokens","Array of pipeline tokens (to resolve dependencies of programs)",true);
varin("references","Definitions of reference sequences",true);

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

<?JS prerunner(id+".report",tokens) ?>gedi -e Stats -prefix <?JS wd ?>/report/<?JS print(FileUtils.getNameWithoutExtension(reads)); ?>. -g <?JS genomes ?> <?JS reads?><?JS var end=postrunner(id+".report") ?>
