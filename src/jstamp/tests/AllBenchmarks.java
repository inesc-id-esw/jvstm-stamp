package jstamp.tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllBenchmarks {

    public static final int[] POSSIBLE_THREADS = { 1, 2, 4, 8, 16 };
    public static final String[] TESTS = { /*"bayes", "intruder", "genome", "kmeans", "matrix", "ssca2",*/ "vacation"/*, "yada"*/ };
    public static final String[] STMS = { "lf-ssi", "lf-normal" };
    public static final String[] OPS = {"l", "m", "h"};
    public static final int ATTEMPTS = 3;

    public static class Result {
	public double time = 0;
	public double aborts = 0;
    }

    public static void main(String[] args) {
	// jvstm -> test -> ops -> threads -> results
	Map<String, Map<String, Map<String, Map<Integer, Result>>>> allData = new HashMap<String, Map<String, Map<String, Map<Integer, Result>>>>();
	for (String jvstm : STMS) {
	    Map<String, Map<String, Map<Integer, Result>>> perSTM = new HashMap<String, Map<String, Map<Integer, Result>>>();
	    for (String test : TESTS) {
		Map<String, Map<Integer, Result>> perTest = new HashMap<String, Map<Integer, Result>>();
		for (String op : OPS) {
		    Map<Integer, Result> perOp = new HashMap<Integer, Result>();
		    for (int threadCount : POSSIBLE_THREADS) {
			Result result = new Result();
			for (int i = 0; i < ATTEMPTS; i++) {
			    String[] parts = getFileContent(args[0] + "/" + test + "-results/" + jvstm + "-" + threadCount + "-" + (i+1) + "-" + op + ".data").get(0).split(" ");
			    result.time += Double.parseDouble(parts[0]);
			    result.aborts += Double.parseDouble(parts[1]);
			}
			perOp.put(threadCount, result);
		    }
		    perTest.put(op, perOp);
		}
		perSTM.put(test, perTest);
	    }
	    allData.put(jvstm, perSTM);
	}

	for (String test : TESTS) {
	    for (String op : OPS) {
		String output = "- ssi lock";
		String outputA = "- ssi lock";
		for (int threadCount : POSSIBLE_THREADS) {
		    output += "\n" + threadCount;
		    outputA += "\n" + threadCount;
		    for (String jvstm : STMS) {
			Result result = allData.get(jvstm).get(test).get(op).get(threadCount);
			double avg = (1200000 / ((result.time / ATTEMPTS) / 1000)) / 1000;
			double aborts = (result.aborts / ATTEMPTS) / 1000;
			output += " " + roundTwoDecimals(avg);
			outputA += " " + roundTwoDecimals(aborts);
		    }
		}
		writeToFile(args[0] + "/results/" + test + "-" + op + "-t.output", output);
		writeToFile(args[0] + "/results/" + test + "-" + op + "-a.output", outputA);
	    }
	}
    }

    private static void writeToFile(String filename, String content) {
	try {
	    FileWriter fstream = new FileWriter(filename);
	    BufferedWriter out = new BufferedWriter(fstream);
	    out.write(content);
	    out.close();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
    }

    private static List<String> getFileContent(String filename) {
	List<String> testLines1 = new ArrayList<String>();
	try {
	    FileInputStream is = new FileInputStream(filename);
	    DataInputStream in = new DataInputStream(is);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
	    String strLine;
	    while ((strLine = br.readLine()) != null) {
		if (strLine.equals("")) {
		    continue;
		}
		testLines1.add(strLine);
	    }
	    br.close();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
	return testLines1;
    }

    private static double roundTwoDecimals(double d) {
	DecimalFormat twoDForm = new DecimalFormat("#");
	return Double.valueOf(twoDForm.format(d));
    }

}
