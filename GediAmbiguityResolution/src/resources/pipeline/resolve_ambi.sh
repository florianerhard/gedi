#!/bin/bash
<?JS0
varin("tokens","Array of pipeline tokens (to resolve dependencies of programs)",true);
varin("name","Name for output files",true);
varin("references","Definitions of reference sequences",true);
varin("reads","Filename containing read mappings",true);
?>

<?JS


output.setExecutable(true);

var cit = StringUtils.removeFooter(reads,".cit");
var keep;
var tokens;

var genomes = "";
for (var r in references) 
	genomes = genomes+" "+r;

?>
<?JS prerunner(name+".disambi", tokens) ?>gedi -t <?JS tmp ?> -e ResolveAmbiguities -r <?JS cit ?>.cit -s <?JS cit ?>.rescue.csv -o <?JS if (keep) print(cit+"_rescued"); else print(cit); ?>.cit -g <?JS genomes ?> -D<?JS var end = postrunner(name+".disambi")?>

<?JS
tokens = [end];
?>