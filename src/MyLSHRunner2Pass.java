import java.io.*;
import java.util.*;

/**
 * The Runner can be ran from the commandline to find the most similar pairs of tweets.
 * Example command to run with brute force similarity search:
 * 				java Runner -threshold 0.5 -method bf -maxFiles 100 -inputPath ../data/tweets -outputPath myoutput -shingleLength 3
 * @author Toon Van Craenendonck
 */

public class MyLSHRunner2Pass {

	public static void main(String[] args) throws Exception{
		String inputPath = "";
		String outputPath = "";
		int maxFiles = -1;
		int shingleLength = -1;
		int nShingles = -1;
		float threshold = -1;
		int i = 0;
		int b = 0;
		int r = 0;
		int numberOfBuckets = 0;
		while (i < args.length && args[i].startsWith("-")) {
			String arg = args[i];
			if(arg.equals("-inputPath")) {
				inputPath = args[i + 1];
			}else if(arg.equals("-maxFiles")){
				maxFiles = Integer.parseInt(args[i+1]);
			}else if(arg.equals("-shingleLength")) {
				shingleLength = Integer.parseInt(args[i + 1]);
			}else if(arg.equals("-nShingles")){
				nShingles = Integer.parseInt(args[i+1]);
			}else if(arg.equals("-threshold")){
				threshold = Float.parseFloat(args[i+1]);
			}else if(arg.equals("-outputPath")) {
				outputPath = args[i + 1];
            }else if(arg.equals("-b")){
				b = Integer.parseInt(args[i+1]);
			}else if(arg.equals("-r")){
            	r = Integer.parseInt(args[i+1]);
			}else if(arg.equals("-numberOfBuckets")){
				numberOfBuckets = Integer.parseInt(args[i+1]);
			}
			i += 2;
		}
		LocalitySensitiveHashing2Pass lsh = new LocalitySensitiveHashing2Pass(shingleLength, nShingles,
                            inputPath, b, r, numberOfBuckets, maxFiles, threshold, outputPath);
		Set<SimilarPair> similarPairs = lsh.DoLSH();
		lsh.printPairs(similarPairs, outputPath);
	}

}
