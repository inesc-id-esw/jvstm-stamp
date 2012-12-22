#! /bin/sh

#1 minMem
#2 maxMem
#3 number of threads
#4 attempt number
#5 the folder of the results
#6 the stm used for labelling the results

javac -Xlint:none -cp jvstm.jar:. jstamp/jvstm/vacation/Vacation.java
echo "Vacation: stm $6, threads $3, attempt $4 low"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.vacation.Vacation -n 400 -q 60 -u 90 -r 16384 -t 200000 -c $3 >> $5/$6-$3-$4-l.data
echo "Vacation: stm $6, threads $3, attempt $4 medium"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.vacation.Vacation -n 400 -q 90 -u 90 -r 16384 -t 200000 -c $3 >> $5/$6-$3-$4-m.data
echo "Vacation: stm $6, threads $3, attempt $4 high"
java $1 $2 -cp jvstm.jar:. jstamp.jvstm.vacation.Vacation -n 400 -q 60 -u 50 -r 16384 -t 200000 -c $3 >> $5/$6-$3-$4-h.data
rm jstamp/jvstm/vacation/*.class
rm jstamp/jvstm/*.class
