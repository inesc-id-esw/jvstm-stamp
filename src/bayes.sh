#! /bin/sh

#1 minMem
#2 maxMem
#3 number of threads
#4 attempt number
#5 the folder of the results
#6 the stm used for labelling the results

javac -Xlint:none -cp jvstm.jar:. jstamp/jvstm/bayes/Bayes.java
echo "Bayes: stm $6, threads $3, attempt $4 low"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.bayes.Bayes -v 32 -r 4096 -n 10 -p 40 -i 10 -e 9 -s 1 -t $3 >> $5/$6-$3-$4-l.data
echo "Bayes: stm $6, threads $3, attempt $4 medium"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.bayes.Bayes -v 32 -r 4096 -n 10 -p 40 -i 2 -e 10 -s 1 -t $3 >> $5/$6-$3-$4-m.data
echo "Bayes: stm $6, threads $3, attempt $4 high"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.bayes.Bayes -v 32 -r 4096 -n 10 -p 40 -i 20 -e 8 -s 1 -t $3 >> $5/$6-$3-$4-h.data
rm jstamp/jvstm/bayes/*.class
rm jstamp/jvstm/*.class
