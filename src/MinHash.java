import java.util.*;

final class MinHash {

    /**
     * Computes the minHash signature for a set
     *
     * @param numOfHashFunctions size of the signature to be returned or number of hash function to use
     * @param shingles the set of shingles
     * @param nShingles the total number of unique shingles
     * @return a signature as an integer array
     */

    private final int numOfHashFunctions;
    private final int N;

    /*
     * Memoize the results of MinHash
     */

    private final int[][] memoizedMinHash;

    public MinHash(int numOfHashFunctions, int nShingles) {
        this.numOfHashFunctions = numOfHashFunctions;
        this.N = nShingles;

        /*
         * As explained later the hash functions we use are of type h_{a,b}(x) = ((a*x + b) mod p) mod N.
         * p: is always the same for all hash functions.
         * a: is different for all hash functions.
         * b: is different for all hash functions.
         *
         * I plan to generate these numbers once and then re-use them for all documents.
         */


        int a[] = new int[numOfHashFunctions];
        int b[] = new int[numOfHashFunctions];
        int p = Primes.findLeastPrimeNumber(this.N+1);

        Random r = new Random(1234); //set a seed to guarantee reproducibility of results.


        /*
         * Normally the combination of a[i] and b[i] should be unique for all i.
         * i.e. a[5]=5 b[5]=5 , a[15]=5 b[15]=5 should not exist cause in that case I have the same hash function twice
         *
         * In spite of that I  do not check for such correctness cause there are 2^31 - 1 positive integers
         * and therefore the probability of that happening is extremely small.
         * For 2 different i to have same a[i] and b[i], the probability is 1 - ((2^31 - 1)*(2^31 - 1))
         *
         * Note: r.nextInt(Integer.MAX_VALUE) generates a int between 0 inclusive and Integer.MAX_VALUE exclusive,
         * therefore generates an int between 0 and 2^31 - 2, therefore there are 2^31-1 different int.
         */
        for (int i = 0; i < a.length; i++) {
            a[i] = r.nextInt(Integer.MAX_VALUE-1) + 1; //a =/=0
            b[i] = r.nextInt(Integer.MAX_VALUE);
        }

        this.memoizedMinHash = new int[numOfHashFunctions][nShingles];

        for(int i=0; i<numOfHashFunctions; i++){
            for(int j=0; j<nShingles; j++){
                long temp = (((long) a[i]) * j) + b[i];
                this.memoizedMinHash[i][j] = (int) (temp % p) % this.N;
            }
        }
    }

    /**
     * This method gets as an input a Set of shingles and returns the MinHash signature
     * in int[] format.
     *
     * @param shingles the set of shingles I want to get the signature of
     * @return the signature in int[]
     */
    public int[] getSignature(Set<Integer> shingles) {

        /*
         * An array of integers that keeps the generated signature from the set. Each cell in this array
         * represents 1 different hash function. The whole array is the signature.
         *
         * Note: in Java an int array is initialized as an array of 0s
         */
        int[] signature = new int[numOfHashFunctions];

        Iterator<Integer> it = shingles.iterator();

        /*
         * I assume the rows of my shingles x docs matrix are the results of the MurMurHash.
         * For example if the shingles in a set after getting hashed in the Shingler class is the following
         * {-84734, 92323, 123, 90, 823}
         * I assume the shingles x docs matrix has rows from Math.MIN_VALUE ... Math.MAX_VALUE
         * All the rows in that matrix are 0, except for the rows -84734, 92323, 123, 90, 823 which are 1.
         *
         * Therefore for this set I only need to compute the result of the rows -84734, 92323, 123, 90, 823 since
         * the other rows have 0 and they do not affect the result of MinHash.
         *
         *
         * Get for example the  scenario my set consists of 4 shingles that MurmurHash hashed to the values
         * {-1, 0, 1, 2}
         * My Shingle x Doc matrix will be the following
         *
         *  -----------------------
         * |       Row      | Doc1 |
         * |----------------|------|
         * |   -2147483648  |   0  |
         * |   -2147483647  |   0  |
         * |   -2147483646  |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |       -2       |   0  |
         * |       -1       |   1  |
         * |        0       |   1  |
         * |        1       |   1  |
         * |        2       |   1  |
         * |        3       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |    2147483646  |   0  |
         * |    2147483646  |   0  |
         *  -----------------------
         *
         *  Therefore to get the signature of Doc1 I only need to compute the rows where Doc1 = 1, those are the rows
         *  {-1, 0, 1, 2}, those are the members of the Set.
         *
         *  The only different between here and the slides, is that in the slide we start from row 0 till row nShingles,
         *  here I start from row -Math.MIN_VALUE up to Math.MAX_VALUE because that is the range of MurmurHash.
         *  The changed range does not affect the correctness of the algorithm at all.
         */

        /*
         * We need sizeOfSignature different hash functions, these hash functions should simulate matrix permutations.
         * The hash function I will use are of type h(x) = ((a*x + b) mod p) mod N
         *
         * a, b: are random integers
         * N: is the number of hash functions
         * p: is a prime number and p > N
         */

        /*
         * As I noted the array is at first initialized with 0s and not with infinite value.
         * A naive approach is looping the array to change the initialization, that is very expensive O(n).
         *
         * The way I deal with it is to initialize the array with the hashes of the first shingle. I also take care
         * of the special case where the set is empty, in that case I need to initialize the array to infinity, since
         * infinity cannot be represented as an integer I choose a special value to represent it, I choose the value -1
         * since it is negative and cannot be the result of any of hash functions.
         */


        /*
         * This variable keeps the prime number we will use for the hash functions. Since we need to use
         * many hash functions that all use the same prime number, just compute it once and keep it.
         */

        //Simply compute the first signature if the set is not empty, and that is our initial value (instead of infinity)
        if (it.hasNext()) {
            Integer hashedSingle = it.next();
            for (int i = 0; i < this.numOfHashFunctions; i++) {
                signature[i] = this.memoizedMinHash[i][hashedSingle];
            }
        } else {
            //If the set is empty then all values should be infinite,
            //then I need to choose a special value to represent infinity but shingles from Murmurhash are between
            //Integer.MIN_VALUE and Integer.MAX_VALUE
            //Change the values
            for (int i = 0; i < this.numOfHashFunctions; i++) {
                signature[i] = -1;
                System.out.println("Empty set at MinHash!!"); //TODO
            }
        }

        //Compute for all n-Shingles in set
        while (it.hasNext()) {
            Integer hashedSingle = it.next();
            for (int i = 0; i < this.numOfHashFunctions; i++) {
                signature[i] = Math.min(signature[i], this.memoizedMinHash[i][hashedSingle]);
            }
        }

        return signature;
    }


    /**
     * This method gets as an input a Set of shingles and a int[] array to store the results of the method.
     * It implicitly returns the MinHash signature of the shingles in int[] format
     *
     * @param shingles the set of shingles I want to get the signature of
     * @param arrayToStoreSignature the array to store the signature
     */
    public void getSignature(Set<Integer> shingles, int[] arrayToStoreSignature) {

        /*
         * An array of integers that keeps the generated signature from the set. Each cell in this array
         * represents 1 different hash function. The whole array is the signature.
         *
         * Note: in Java an int array is initialized as an array of 0s
         */

        Iterator<Integer> it = shingles.iterator();

        /*
         * I assume the rows of my shingles x docs matrix are the results of the MurMurHash.
         * For example if the shingles in a set after getting hashed in the Shingler class is the following
         * {-84734, 92323, 123, 90, 823}
         * I assume the shingles x docs matrix has rows from Math.MIN_VALUE ... Math.MAX_VALUE
         * All the rows in that matrix are 0, except for the rows -84734, 92323, 123, 90, 823 which are 1.
         *
         * Therefore for this set I only need to compute the result of the rows -84734, 92323, 123, 90, 823 since
         * the other rows have 0 and they do not affect the result of MinHash.
         *
         *
         * Get for example the  scenario my set consists of 4 shingles that MurmurHash hashed to the values
         * {-1, 0, 1, 2}
         * My Shingle x Doc matrix will be the following
         *
         *  -----------------------
         * |       Row      | Doc1 |
         * |----------------|------|
         * |   -2147483648  |   0  |
         * |   -2147483647  |   0  |
         * |   -2147483646  |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |       -2       |   0  |
         * |       -1       |   1  |
         * |        0       |   1  |
         * |        1       |   1  |
         * |        2       |   1  |
         * |        3       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |    2147483646  |   0  |
         * |    2147483646  |   0  |
         *  -----------------------
         *
         *  Therefore to get the signature of Doc1 I only need to compute the rows where Doc1 = 1, those are the rows
         *  {-1, 0, 1, 2}, those are the members of the Set.
         *
         *  The only different between here and the slides, is that in the slide we start from row 0 till row nShingles,
         *  here I start from row -Math.MIN_VALUE up to Math.MAX_VALUE because that is the range of MurmurHash.
         *  The changed range does not affect the correctness of the algorithm at all.
         */

        /*
         * We need to sizeOfSignature different hash function, these hash functions should simulate matrix permutations.
         * The hash function I will use are of type h(x) = ((a*x + b) mod p) mod N
         *
         * a, b: are random integers
         * N: is the number of hash functions
         * p: is a prime number and p > N
         */

        /*
         * As I noted the array is at first initialized with 0s and not with infinite value.
         * A naive approach is looping the array to change the initialization, that is very expensive O(n).
         *
         * The way I deal with it is to initialize the array with the hashes of the first shingle. I also take care
         * of the special case where the set is empty, in that case I need to initialize the array to infinity, since
         * infinity cannot be represented as an integer I choose a special value to represent it, I choose the value -1
         * since it is negative and cannot be the result of any of hash functions.
         */


        //Simply compute the first signature if the set is not empty, and that is our initial value (instead of infinity)
        if (it.hasNext()) {
            Integer hashedSingle = it.next();
            for (int i = 0; i < this.numOfHashFunctions; i++) {
                arrayToStoreSignature[i] = this.memoizedMinHash[i][hashedSingle];
            }
        } else {
            //If the set is empty then all values should be infinite,
            //then I need to choose a special value to represent infinity but shingles from MurmurHash are between
            //Integer.MIN_VALUE and Integer.MAX_VALUE
            //Change the values
            for (int i = 0; i < this.numOfHashFunctions; i++) {
                arrayToStoreSignature[i] = -1;
                System.out.println("Empty set at MinHash!!");
            }
        }

        //Compute for all n-Shingles in set
        while (it.hasNext()) {
            Integer hashedSingle = it.next();
            for (int i = 0; i < this.numOfHashFunctions; i++) {
                arrayToStoreSignature[i] = Math.min(arrayToStoreSignature[i], this.memoizedMinHash[i][hashedSingle]);
            }
        }

    }



    /**
     * This method gets as an input a Set of shingles and a int[] array to store the results of the method,
     * and an index from which cell of the array the saving of the signature should start.
     * It implicitly returns the MinHash signature of the shingles in int[] format
     *
     * @param shingles the set of shingles I want to get the signature of
     * @param targetSaveArray the array to store the signature
     * @
     */

    public void getSignature(final Set<Integer> shingles,
                              final int[] targetSaveArray, final int targetStartIndex){

        final Iterator<Integer> it = shingles.iterator();

        /*
         * I assume the rows of my shingles x docs matrix are the results of the MurMurHash.
         * For example if the shingles in a set after getting hashed in the Shingler class is the following
         * {-84734, 92323, 123, 90, 823}
         * I assume the shingles x docs matrix has rows from Math.MIN_VALUE ... Math.MAX_VALUE
         * All the rows in that matrix are 0, except for the rows -84734, 92323, 123, 90, 823 which are 1.
         *
         * Therefore for this set I only need to compute the result of the rows -84734, 92323, 123, 90, 823 since
         * the other rows have 0 and they do not affect the result of MinHash.
         *
         *
         * Get for example the  scenario my set consists of 4 shingles that MurmurHash hashed to the values
         * {-1, 0, 1, 2}
         * My Shingle x Doc matrix will be the following
         *
         *  -----------------------
         * |       Row      | Doc1 |
         * |----------------|------|
         * |   -2147483648  |   0  |
         * |   -2147483647  |   0  |
         * |   -2147483646  |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |       -2       |   0  |
         * |       -1       |   1  |
         * |        0       |   1  |
         * |        1       |   1  |
         * |        2       |   1  |
         * |        3       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |        .       |   0  |
         * |    2147483646  |   0  |
         * |    2147483646  |   0  |
         *  -----------------------
         *
         *  Therefore to get the signature of Doc1 I only need to compute the rows where Doc1 = 1, those are the rows
         *  {-1, 0, 1, 2}, those are the members of the Set.
         *
         *  The only different between here and the slides, is that in the slide we start from row 0 till row nShingles,
         *  here I start from row -Math.MIN_VALUE up to Math.MAX_VALUE because that is the range of MurmurHash.
         *  The changed range does not affect the correctness of the algorithm at all.
         */

        /*
         * We need to sizeOfSignature different hash function, these hash functions should simulate matrix permutations.
         * The hash function I will use are of type h(x) = ((a*x + b) mod p) mod N
         *
         * a, b: are random integers
         * N: is the number of hash functions
         * p: is a prime number and p > N
         */

        /*
         * As I noted the array is at first initialized with 0s and not with infinite value.
         * A naive approach is looping the array to change the initialization, that is very expensive O(n).
         *
         * The way I deal with it is to initialize the array with the hashes of the first shingle. I also take care
         * of the special case where the set is empty, in that case I need to initialize the array to infinity, since
         * infinity cannot be represented as an integer I choose a special value to represent it, I choose the value -1
         * since it is negative and cannot be the result of any of hash functions.
         */


        //Simply compute the first signature if the set is not empty, and that is our initial value (instead of infinity)
        if (it.hasNext()) {
            final Integer hashedSingle = it.next();
            for (int i = 0; i < this.numOfHashFunctions; i++) {
                targetSaveArray[targetStartIndex + i] = this.memoizedMinHash[i][hashedSingle];
            }
        } else {
            //If the set is empty then all values should be infinite,
            //then I need to choose a special value to represent infinity but shingles from Murmurhash are between
            //Integer.MIN_VALUE and Integer.MAX_VALUE
            //Change the values
            final int max = this.numOfHashFunctions + targetStartIndex;
            for (int i = targetStartIndex; i < max; i++) {
                targetSaveArray[i] = -1;
            }
        }

        //Compute for all n-Shingles in set
        while (it.hasNext()) {
            final Integer hashedSingle = it.next();
            for (int i = 0; i < this.numOfHashFunctions; i++) {
                targetSaveArray[targetStartIndex + i] =
                        Math.min(targetSaveArray[targetStartIndex + i], this.memoizedMinHash[i][hashedSingle]);
            }
        }

    }

}
