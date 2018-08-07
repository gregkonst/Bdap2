import java.util.*;

public class LocalitySensitiveHashing2Pass extends LocalitySensitiveHashing{

    int batchSize = 475000;

    final Shingler shingler = new Shingler(super.shingleLength, super.nShingles);
    final TwitterReader reader = new TwitterReader(super.maxFiles, shingler, super.inputPath);


    public LocalitySensitiveHashing2Pass(int shingleLength, int nShingles, String inputPath, int b, int r,
                           int sizeOfBuckets, int maxFiles, double threshold, String outputFile) {
        super(shingleLength, nShingles, inputPath, b, r, sizeOfBuckets, maxFiles, threshold, outputFile);
    }

    
    @Override
    public Set<SimilarPair> DoLSH() {
        Set<SimilarPair> originalSimilarPairs =  super.DoLSH();
        //If they are 0 just end the program, nothing to do here
        if(originalSimilarPairs.size() == 0) return originalSimilarPairs;


        //I want to re-read the files only once, so read the files and keep them in the memory
        List<SimilarPair> listOriginalSimilarPairs = new ArrayList<>(originalSimilarPairs);
        originalSimilarPairs = null;
        int batchCounter = 0;

        Set<SimilarPair> newSimilarPairs = new HashSet<SimilarPair>();

        while(batchCounter < listOriginalSimilarPairs.size()){
            if(listOriginalSimilarPairs.size() - batchCounter > batchSize){
                Set<Integer> uniqueIDs = getUniqueIds(listOriginalSimilarPairs, batchCounter, batchSize+batchCounter);
                List<Integer> listUniqueIDs = new ArrayList<>(uniqueIDs);
                Collections.sort(listUniqueIDs);
                Map <Integer, Set<Integer>> map = readFilesByID(listUniqueIDs);

                for(int i=batchCounter; i<batchCounter+batchSize; i++){
                    double similarity = jaccardSimilarity(map.get(listOriginalSimilarPairs.get(i).getId1()),
                                                        map.get(listOriginalSimilarPairs.get(i).getId2()));
                    if(similarity >= super.threshold){
                        newSimilarPairs.add(listOriginalSimilarPairs.get(i));
                    }
                }

                batchCounter+=batchSize;
            }else{
                Set<Integer> uniqueIDs = getUniqueIds(listOriginalSimilarPairs, batchCounter, listOriginalSimilarPairs.size());
                List<Integer> listUniqueIDs = new ArrayList<>(uniqueIDs);
                Collections.sort(listUniqueIDs);
                Map <Integer, Set<Integer>> map = readFilesByID(listUniqueIDs);

                for(int i=batchCounter; i<listOriginalSimilarPairs.size(); i++){
                    double similarity = jaccardSimilarity(map.get(listOriginalSimilarPairs.get(i).getId1()),
                            map.get(listOriginalSimilarPairs.get(i).getId2()));
                    if(similarity >= super.threshold){
                        newSimilarPairs.add(listOriginalSimilarPairs.get(i));
                    }
                }

                batchCounter = listOriginalSimilarPairs.size();
            }
        }
        return newSimilarPairs;
    }

    //Read and save in the memory only the IDs required for the second pass
    public Map<Integer, Set<Integer>> readFilesByID(List<Integer> listUniqueIDs){

        reader.reset();

        Map<Integer, Set<Integer>> map = new HashMap<Integer, Set<Integer>>();
        int idCounter = 0;
        for(int i=0; i<listUniqueIDs.size(); i++){
            int uniqueID = listUniqueIDs.get(i);
            while(uniqueID != idCounter){
                idCounter++;
                reader.skipNext();
            }
            map.put(uniqueID, reader.next());
            idCounter++;
        }
        return map;
    }

    /*
     * After the first LSH the program will have  a few similar pairs, this method
     * will record and return all the IDs appearing in those pairs, so that perform a
     * second pass on the tweets.
     */
    private Set<Integer> getUniqueIds(List<SimilarPair> similarPairs, int start, int batchSize) {
        Set<Integer> uniqueInts = new HashSet<Integer>();
        for (int i=start; i<batchSize; i++) {
            uniqueInts.add(similarPairs.get(i).getId1());
            uniqueInts.add(similarPairs.get(i).getId2());
        }
        return uniqueInts;
    }


//    /**
//     * Very fast algorithm for calculating the Jaccard Similarity between 2 HashSets
//     *
//     * @param set1 the first set
//     * @param set2 the second set
//     * @return the Jaccard similarity between the two sets
//     */
    public static double jaccardSimilarity(final Set<Integer> set1, final Set<Integer> set2){
        int common = 0; //counts the common elements in the two sets

        //Loop for the small set and check if its elements are in the big set
        if(set1.size() > set2.size()){
            for(Integer i: set2){
                if(set1.contains(i)) {
                    common++;
                }
            }
        }else{
            for(Integer i: set1){
                if(set2.contains(i)) {
                    common++;
                }
            }
        }

        //The total of the numbers of the two sets is the union minus the common
        final int total = set1.size() + set2.size() - common;

        //Avoid division by 0!
        if(total == 0) return 0;

        //return the similarity common over total
        return (double)common / total;
    }


}

