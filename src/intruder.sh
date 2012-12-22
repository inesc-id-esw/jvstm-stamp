#! /bin/sh

#1 minMem
#2 maxMem
#3 number of threads
#4 attempt number
#5 the folder of the results
#6 the stm used for labelling the results

javac -Xlint:none -cp jvstm.jar:. jstamp/jvstm/intruder/Intruder.java
echo "Intruder: stm $6, threads $3, attempt $4 low"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.intruder.Intruder -a 10 -l 16 -n 20480 -s 1 -t $3 >> $5/$6-$3-$4-l.data
echo "Intruder: stm $6, threads $3, attempt $4 medium"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.intruder.Intruder -a 128 -l 16 -n 20480 -s 1 -t $3 >> $5/$6-$3-$4-m.data
echo "Intruder: stm $6, threads $3, attempt $4 high"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.intruder.Intruder -a 10 -l 128 -n 20480 -s 1 -t $3 >> $5/$6-$3-$4-h.data
rm jstamp/jvstm/intruder/*.class
rm jstamp/jvstm/*.class
