#! /bin/sh

#1 minMem
#2 maxMem
#3 number of threads
#4 attempt number
#5 the folder of the results
#6 the stm used for labelling the results

javac -Xlint:none -cp jvstm.jar:. jstamp/jvstm/ssca2/SSCA2.java
echo "SSCA2: stm $6, threads $3, attempt $4 low"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.ssca2.SSCA2 -s 20 -i 1.0 -u 1.0 -l 9 -p 9 -t $3 >> $5/$6-$3-$4-l.data
echo "SSCA2: stm $6, threads $3, attempt $4 medium"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.ssca2.SSCA2 -s 20 -i 1.0 -u 1.0 -l 5 -p 5 -t $3 >> $5/$6-$3-$4-m.data
echo "SSCA2: stm $6, threads $3, attempt $4 high"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.ssca2.SSCA2 -s 20 -i 1.0 -u 1.0 -l 1 -p 1 -t $3 >> $5/$6-$3-$4-h.data
rm jstamp/jvstm/ssca2/*.class
rm jstamp/jvstm/*.class
