#! /bin/sh

prefix="/home/nmld/workspace/JStampJVSTM"

minMem="-Xms512M"
maxMem="-Xmx8G"

jvstm[1]="lb-ssi"
jvstm[2]="lb-normal"
jvstm[3]="lf-ssi"
jvstm[4]="lf-normal"

stamp[1]="bayes"
stamp[2]="genome"
stamp[3]="intruder"
stamp[4]="kmeans"
stamp[5]="matrix"
stamp[6]="ssca2"
stamp[7]="vacation"
stamp[8]="yada"


for benchmark in 7
do
	rm -rf $prefix/${stamp[$benchmark]}-results
	mkdir $prefix/${stamp[$benchmark]}-results
        for stm in 1 2
        do
                for t in 1 2 4 8 16
                do
                                for attempt in 1 2 3
                                do
					cp $prefix/${jvstm[$stm]}.jar $prefix/src/jvstm.jar
					cd $prefix/src/
					bash ${stamp[$benchmark]}.sh $minMem $maxMem $t $attempt $prefix/${stamp[$benchmark]}-results ${jvstm[$stm]}
					rm $prefix/src/jvstm.jar
                                done
                        done
        done
done
