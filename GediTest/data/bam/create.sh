/home/proj/software/bowtie2/bowtie2-2.1.0/bowtie2-build genome.fasta genome

/home/proj/software/bowtie2/bowtie2-2.1.0/bowtie2 -f --end-to-end -D 20 -R 3 -N 1 -L 4 -i S,1,0.50 --rdg 3,1 --rfg 3,1 --score-min L,-0.9,-0.9 -x genome -U reads.fasta -S reads.sam
sed -i s/12D/12N/ reads.sam 
sed -i s/\\^AAAAAAAAAAAA/^/ reads.sam
/home/proj/software/samtools/samtools-0.1.18/samtools view -b -S reads.sam > reads.bam
/home/proj/software/samtools/samtools-0.1.18/samtools sort reads.bam reads
/home/proj/software/samtools/samtools-0.1.18/samtools index reads.bam 

/home/proj/software/bowtie2/bowtie2-2.1.0/bowtie2 -f --end-to-end -D 20 -R 3 -N 1 -L 4 -i S,1,0.50 --rdg 3,1 --rfg 3,1 --score-min L,-0.9,-0.9 -x genome -U reads2.fasta -S reads2.sam
sed -i s/12D/12N/ reads2.sam 
sed -i s/\\^AAAAAAAAAAAA/^/ reads2.sam
/home/proj/software/samtools/samtools-0.1.18/samtools view -b -S reads2.sam > reads2.bam
/home/proj/software/samtools/samtools-0.1.18/samtools sort reads2.bam reads2
/home/proj/software/samtools/samtools-0.1.18/samtools index reads2.bam 

/home/proj/software/bowtie2/bowtie2-2.1.0/bowtie2 -f --end-to-end -D 20 -R 3 -N 1 -L 4 -i S,1,0.50 --rdg 3,1 --rfg 3,1 --score-min L,-0.9,-0.9 -x genome -U reads3.fasta -S reads3.sam
/home/proj/software/samtools/samtools-0.1.18/samtools view -b -S reads3.sam > reads3.bam
/home/proj/software/samtools/samtools-0.1.18/samtools sort reads3.bam reads3
/home/proj/software/samtools/samtools-0.1.18/samtools index reads3.bam 


