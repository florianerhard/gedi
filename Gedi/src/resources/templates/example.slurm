#!/bin/bash
#SBATCH -D <?JS wd ?> 
#SBATCH -o <?JS logfolder ?>/<?JS name ?>.out
#SBATCH -e <?JS logfolder ?>/<?JS name ?>.err
#SBATCH -J <?JS print(name.substring(0,Math.min(name.length,10))); ?> 
#SBATCH --get-user-env 
#SBATCH --clusters=<?JS clusters ?> 
#SBATCH --partition=<?JS partition ?> 
#SBATCH --mail-type=end 
#SBATCH --mem=<?JS mem ?> 
#SBATCH --cpus-per-task=<?JS nthreads ?> 
#SBATCH --mail-user=<?JS email ?>
#SBATCH --export=NONE 
#SBATCH --time=24:00:00 

<?JS command ?>

<?JS 
var exec = function(script,dependencies) {
	var line;
	if (dependencies)
		line = RunUtils.output(['sbatch','--dependency=afterany:'+dependencies,script]);
	else
		line = RunUtils.output(['sbatch',script]);
	// the RunUtils.ouput function executes the given commands, redirecits input and error stream and returns everything from stdout as String.
	return line.split(" ")[3];
}


/* 
a function must be defined that calls the command submitting a job to the cluster; its parameters are the command to submit (a string)
and an array of dependencies (i.e. jobs that must be finished before this job is supposed to start), which can be null!
the function must return the job id (to construct dependencies for subsequent jobs)

the variables that must be given are completely up to you, the gedi -e Cluster example.slurm <name> <command> call constructs the parameters from 
1. the main Gedi config (in this example, this is where the email is supposed to come from)
2. the defaults from example.slurm.json
3. the parameters given to gedi -e Cluster via --key=value or -j <json-file>
4. name and command are obtained from the mandatory parameters to gedi -e Cluster
*/


?>
