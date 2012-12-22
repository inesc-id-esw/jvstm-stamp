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

public class STMBench7Results {

    public static final int[] POSSIBLE_THREADS = { 1, 2, 4, 8, 16 };
    public static final String[] TESTS = { "ro", "r", "rw", "w", "wo", "sms-ro", "sms-r", "sms-rw", "sms-w", "sms-wo" };
    public static final String[] STMS = { "lf-ssi", "lf-normal" };
    public static final int ATTEMPTS = 3;

    public static class Result {
	public double time = 0;
	public double aborts = 0;
	public double restarts = 0;
    }

    public static void main(String[] args) {
	// jvstm -> test -> threads -> results
	Map<String, Map<String, Map<Integer, Result>>> allData = new HashMap<String, Map<String, Map<Integer, Result>>>();
	for (String jvstm : STMS) {
	    Map<String, Map<Integer, Result>> perSTM = new HashMap<String, Map<Integer, Result>>();
	    for (String test : TESTS) {
		Map<Integer, Result> perTest = new HashMap<Integer, Result>();
		for (int threadCount : POSSIBLE_THREADS) {
		    Result result = new Result();
		    for (int i = 0; i < ATTEMPTS; i++) {
			// "88.00 RW = 1, RO = 9, Conflicts = 0 (0.000000%), Restarts = 0 (0.000000%)"
			String[] parts = getFileContent(args[0] + "/" + jvstm + "-" + threadCount + "-" + test + "-" + (i+1) + ".data").get(0).split(" ");
			result.time += Double.parseDouble(parts[0]);
			result.aborts += Double.parseDouble(parts[9]);
			result.restarts += Double.parseDouble(parts[13]);
		    }
		    perTest.put(threadCount, result);
		}
		perSTM.put(test, perTest);
	    }
	    allData.put(jvstm, perSTM);
	}

	for (String test : TESTS) {
	    String output = "- ssi lock";
	    String outputA = "- ssi lock";
	    String outputR = "- ssi lock";
	    for (int threadCount : POSSIBLE_THREADS) {
		output += "\n" + threadCount;
		outputA += "\n" + threadCount;
		outputR += "\n" + threadCount;
		for (String jvstm : STMS) {
		    Result result = allData.get(jvstm).get(test).get(threadCount);
		    double avg = (result.time / ATTEMPTS);
		    double aborts = (result.aborts / ATTEMPTS);
		    double restarts = (result.restarts / ATTEMPTS);
		    output += " " + roundTwoDecimals(avg);
		    outputA += " " + roundTwoDecimals(aborts);
		    outputR += " " + roundTwoDecimals(restarts);
		}
		writeToFile(args[0] + "/results/" + test + "-t.output", output);
		writeToFile(args[0] + "/results/" + test + "-a.output", outputA);
		writeToFile(args[0] + "/results/" + test + "-r.output", outputR);
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
