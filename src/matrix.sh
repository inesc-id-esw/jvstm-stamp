#! /bin/sh

#1 minMem
#2 maxMem
#3 number of threads
#4 attempt number
#5 the folder of the results
#6 the stm used for labelling the results

javac -Xlint:none -cp jvstm.jar:. jstamp/jvstm/matrix/Matrix.java
echo "Matrix: stm $6, threads $3, attempt $4 low"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.matrix.Matrix $3 100 >> $5/$6-$3-$4-l.data
echo "Matrix: stm $6, threads $3, attempt $4 medium"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.matrix.Matrix $3 500 >> $5/$6-$3-$4-m.data
echo "Matrix: stm $6, threads $3, attempt $4 high"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.matrix.Matrix $3 1150 >> $5/$6-$3-$4-h.data
rm jstamp/jvstm/matrix/*.class
rm jstamp/jvstm/*.class
