#! /bin/sh

#1 minMem
#2 maxMem
#3 number of threads
#4 attempt number
#5 the folder of the results
#6 the stm used for labelling the results

javac -Xlint:none -cp jvstm.jar:. jstamp/jvstm/genome/Genome.java
echo "Genome: stm $6, threads $3, attempt $4 low"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.genome.Genome -g 16384 -s 64 -n 16777216 -t $3 >> $5/$6-$3-$4-l.data
echo "Genome: stm $6, threads $3, attempt $4 medium"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.genome.Genome -g 16384 -s 128 -n 16777216  -t $3 >> $5/$6-$3-$4-m.data
echo "Genome: stm $6, threads $3, attempt $4 high"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.genome.Genome -g 32768 -s 64 -n 16777216 -t $3 >> $5/$6-$3-$4-h.data
rm jstamp/jvstm/genome/*.class
rm jstamp/jvstm/*.class
