#! /bin/sh

#1 minMem
#2 maxMem
#3 number of threads
#4 attempt number
#5 the folder of the results
#6 the stm used for labelling the results

javac -Xlint:none -cp jvstm.jar:. jstamp/jvstm/yada/Yada.java
echo "Yada: stm $6, threads $3, attempt $4 low"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.yada.Yada -a 20 -i jstamp/jvstm/yada/633.2 -t $3 >> $5/$6-$3-$4-l.data
echo "Yada: stm $6, threads $3, attempt $4 medium"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.yada.Yada -a 10 -i jstamp/jvstm/yada/ttimeu10000.2 -t $3 >> $5/$6-$3-$4-m.data
echo "Yada: stm $6, threads $3, attempt $4 high"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.yada.Yada -a 15 -i jstamp/jvstm/yada/ttimeu10000.2 -t $3 >> $5/$6-$3-$4-h.data
rm jstamp/jvstm/yada/*.class
rm jstamp/jvstm/*.class
