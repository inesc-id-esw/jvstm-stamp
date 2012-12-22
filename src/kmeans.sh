#! /bin/sh

#1 minMem
#2 maxMem
#3 number of threads
#4 attempt number
#5 the folder of the results
#6 the stm used for labelling the results

javac -Xlint:none -cp jvstm.jar:. jstamp/jvstm/kmeans/KMeans.java
echo "KMeans: stm $6, threads $3, attempt $4 low"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.kmeans.KMeans -m 15 -n 15 -t 0.00001 -i jstamp/jvstm/kmeans/random-n2048-d16-c16.txt -nthreads $3 >> $5/$6-$3-$4-l.data
echo "KMeans: stm $6, threads $3, attempt $4 medium"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.kmeans.KMeans -m 40 -n 40 -t 0.001 -i jstamp/jvstm/kmeans/random-n16384-d24-c16.txt -nthreads $3 >> $5/$6-$3-$4-m.data
echo "KMeans: stm $6, threads $3, attempt $4 high"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.kmeans.KMeans -m 40 -n 40 -t 0.000001 -i jstamp/jvstm/kmeans/random-n16384-d24-c16.txt -nthreads $3 >> $5/$6-$3-$4-h.data
rm jstamp/jvstm/kmeans/*.class
rm jstamp/jvstm/*.class
