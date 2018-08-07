import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

public class LocalitySensitiveHashing {

    final int nShingles;
    final String inputPath;
    final int b;
    final int r;
    private final int sizeOfBuckets;
    final int maxFiles;
    final double threshold;
    final String outputFile;
    final int shingleLength;
    final int signatureSize;


    public LocalitySensitiveHashing(int shingleLength, int nShingles, String inputPath, int b, int r, int sizeOfBuckets, int maxFiles, double threshold, String outputFile) {
        this.shingleLength = shingleLength;
        this.nShingles = nShingles;
        this.inputPath = inputPath;
        this.b = b;
        this.r = r;
        this.sizeOfBuckets = sizeOfBuckets;
        this.maxFiles = maxFiles;
        this.threshold = threshold;
        this.outputFile = outputFile;
        this.signatureSize = b*r;
    }

    /**
     * Compute the candidate pairs from the minHash signatures of documents for a given band.
     * @param docSignatures the minHash signatures of the documents
     * @param band the band for which we want to find the similar pairs. 0 <= band < this.b
     * @return the candidate pairs of this band
     */
    private MyPrimitiveArrayList[] computeCandidatePairsForBand(int[] docSignatures, int band){

        /*
         * I need b buckets where I will keep track of all the documents that have been hashed there.
         * The size of each bucket b is given as input sizeOfBuckets.
         *
         * Because I do not know beforehand how many documents will have rows that hash to the same cell in the bucket,
         * I need to create an arrayList for each cell. The arrayList will keep the IDs of the tweets that hash to that
         * cell in that bucket.
         *
         * Therefore I need an array of ArrayLists. (Each cell of the array is an ArrayList).
         * 
         * Every time I call this method i pass an argument band b. Then I find for that band only all candidate pairs.
         * I do not find the candidante pairs for all bands at the same time because that would be more expensive memory wise.
         *
         * The code for the above follows.
         */


        //This array keeps the candidate pairs for the band given as input
        MyPrimitiveArrayList[] candidatePairs = new MyPrimitiveArrayList[this.sizeOfBuckets];

        //Declare a temp byte array with size equal to signature*4 (cause 1 int = 4bytes)
        byte[] oneSignatureAsByte = new byte[signatureSize*4];

        //Do LSH
        for(int currentDocumentId=0; currentDocumentId<maxFiles; currentDocumentId++){
            //for all b partitions of the rows (buckets)

            // Get the byte representation of a signature
            // Avoid creating and initializing dummy arrays all the time,
            // just initialize one array beforehand, and always use the same array for all signatures
            integersToBytes(docSignatures,
                    currentDocumentId*signatureSize, signatureSize, oneSignatureAsByte);


            // Get the MurmurHash of part of the array,
            // Note: we use *4 cause we are not working with int any more but with byte
            // Part of the array cause, we split the signature in b parts
            int rRowsHash = MurmurHash.myMurmurHash32(oneSignatureAsByte, (band*r*4), ((band+1)*r*4) -1);

            //bring the size of the hash to the size of the buckets
            rRowsHash = rRowsHash%sizeOfBuckets;

            /*
             * if hash is negative bring it to positive
             * NOTE: there is no bug for hash=Math.MIN_VALUE
             * cannot happen cause size of buckets = Math.MAX_VALUE therefore max value I have is Math.abs(Math.MAX_VALUE)
             * therefore Math.MIN_VALUE impossible
             */

            if(rRowsHash < 0){
                rRowsHash = - rRowsHash;
            }

            /*
             * If the list is null (never visited that bucket before) create a new ArrayList here
             * and later add that element to the newly created ArrayList
             *
             * If the array list is not null it means is was already created before and it already has
             * some elements in it (at least one). All these elements are candidate pairs!
             * Just add the current element to that list.
             */

            if(candidatePairs[rRowsHash] == null){
                candidatePairs[rRowsHash] = new MyPrimitiveArrayList(4);
            }

            //Add to the list of the bucket where the r rows hash, the current document ID!
            candidatePairs[rRowsHash].add(currentDocumentId);
        }
        return candidatePairs;
    }

    public Set<SimilarPair> DoLSH(){

        //Compute the MinHash signatures for all documents and store them in a 1d int array
        int[] docSignatures = computeMinHashSignatures();

        //HashSet that keeps all similarPairs. I am using a Set cause I do not want duplicates
        Set<SimilarPair> similarPairs = new HashSet<SimilarPair>();

        for(int band=0; band<b; band++){
            //Compute candidate pairs for a band
            MyPrimitiveArrayList[] candidatePairsForBand = computeCandidatePairsForBand(docSignatures, band);

            //Add all similar pairs, i.e. pairs of documents that have been identified as having a similarity bigger
            //than the threshold, from that band to the set
            getSimilarPairs(candidatePairsForBand, docSignatures, threshold, similarPairs);

        }
        docSignatures = null;
        return similarPairs;
    }

    private int[] computeMinHashSignatures(){
        //Get all the data from the files
        final Shingler shingler = new Shingler(this.shingleLength, this.nShingles);
        final TwitterReader reader = new TwitterReader(this.maxFiles, shingler, this.inputPath);


        //the size of the signature, or number of different hash functions, is b*r
        //First cell is ID of the document!
        //Array of arrays. Each cell contain 1 array which is the MinHash signature of each document.

        //Single dimensional array has advantage of locality + lower overhead over 2D array

        final int[] docSignatures = new int[signatureSize * this.maxFiles]; //array max sie is Integer.MAX_VALUE - 5!
        final MinHash minHash = new MinHash(signatureSize, nShingles);

        int idCounter = 0;
        while(reader.hasNext()) { //while not end of documents
            final Set<Integer> document = reader.next(); //read next document
            minHash.getSignature(document, docSignatures, idCounter*signatureSize);
            idCounter++;

        }
        return docSignatures;
    }

    /**
     * @param candidatePairs The pairs identified as candidates for being similar
     * @param docSignatures an Array that has the MinHash signatures of the documents
     * @param threshold the threshold we need to have bigger than in order to classify 2 different tweets as similar
     * @param similarPairs the Set where similar pairs are saved
     * @return returns the set with all pairs identified as having a Jaccard Similarity >= than the threshold
     */
    private Set<SimilarPair> getSimilarPairs(MyPrimitiveArrayList[] candidatePairs,
                                             int[] docSignatures,
                                             double threshold,
                                             Set<SimilarPair> similarPairs){

        //for every cell in the bucket
        for (int cellIndex = 0; cellIndex < candidatePairs.length; cellIndex++) {
            //if there is no list there (null) or if the size is 1 continue the loop
            //We check for size==1 cause an element cannot be similar to another one if another one does not exist
            if (candidatePairs[cellIndex] == null || candidatePairs[cellIndex].size() == 1) {
                candidatePairs[cellIndex] = null; // free memory?
                continue;
            }

            //Do brute force for all elements in this list
            for (int i = 0; i < candidatePairs[cellIndex].size(); i++) {
                final int id1 = candidatePairs[cellIndex].get(i);
                final int sigIndex1 = id1*signatureSize;
                for (int j = (i + 1); j < candidatePairs[cellIndex].size(); j++) {

                    final int id2 = candidatePairs[cellIndex].get(j);
                    final double docSimilarity =  jaccardSimilarityFast(docSignatures, sigIndex1,
                                                            id2*signatureSize, signatureSize);

                    //If the Similarity is over the threshold!
                    if (docSimilarity >= threshold) {
                        similarPairs.add(new SimilarPair(id1, id2, docSimilarity));
                    }
                }
            }
            candidatePairs[cellIndex] = null; //free memory
        }
        return similarPairs;
    }

    /**
     * Prints pairs and their similarity.
     * @param similarItems the set of similar items to print
     * @param outputFile the path of the file to which they will be printed
     */
    void printPairs(Set<SimilarPair> similarItems, String outputFile){
        outputFile = outputFile + "LSHS";
        try {
            File fout = new File(outputFile);
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

           // TODO that kanw add all kappa
           List<SimilarPair> sim = new ArrayList<SimilarPair>(similarItems);
           Collections.sort(sim, Collections.reverseOrder());

            String comma = ",";
            StringBuilder sb = new StringBuilder(24);
            for(SimilarPair sp : sim){
                sb.append(sp.getId1());
                sb.append(comma);
                sb.append(sp.getId2());
                sb.append(comma);
                sb.append(sp.getSimilarity());
                bw.write(sb.toString());
                bw.newLine();
                sb.delete(0, sb.length());
            }
            bw.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void integersToBytes(int[] sourceArray, int startPos, int length, byte[] targetArray) {
        int targetArrayIndex;
        for(int i=0; i<length; i++){
            targetArrayIndex = i*4;
            targetArray[targetArrayIndex] = (byte) (sourceArray[startPos] >>> 24); //always 0 in this program
            targetArray[targetArrayIndex + 1] = (byte) (sourceArray[startPos] >>> 16);
            targetArray[targetArrayIndex + 2] = (byte) (sourceArray[startPos] >>> 8);
            targetArray[targetArrayIndex + 3] = (byte) (sourceArray[startPos++]);
        }
    }

    private static double jaccardSimilarityFast(final int[] docSignatures,
                                                final int doc1Index, final int doc2Index, final int signatureSize){
        int common = 0;
        for(int i=0; i<signatureSize; i++){
            if(docSignatures[doc1Index + i] == docSignatures[doc2Index + i]){
                common++;
            }
        }
        return (double)common/signatureSize;
    }

    private void analyzeMemoryBucketsBands32Bit(MyPrimitiveArrayList[] candidatePairs){
        double memoryInBytes = 16;
        memoryInBytes += candidatePairs.length * 4;
        for(int i=0; i<candidatePairs.length; i++){
            MyPrimitiveArrayList list = candidatePairs[i];
            if(list == null) continue;
            memoryInBytes += 8;
            memoryInBytes = memoryInBytes + (4*list.size());
        }
        double MB = 1024*1024;
        System.out.println("32 bit " + memoryInBytes/MB + " Mbytes");
    }

    private void analyzeMemoryBucketsBands64bit(MyPrimitiveArrayList[] candidatePairs){
        double memoryInBytes = 28;
        memoryInBytes += candidatePairs.length * 8;
        for(int i=0; i<candidatePairs.length; i++){
            MyPrimitiveArrayList list = candidatePairs[i];
            if(list == null) continue;
            memoryInBytes += 16;
            memoryInBytes = memoryInBytes + (4*list.size());
        }
        double MB = 1024*1024;
        System.out.println("64 bit " + memoryInBytes/MB + " Mbytes");
    }


}