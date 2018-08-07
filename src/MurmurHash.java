 import java.util.Arrays;

/**
 * murmur hash 2.0.
 * 
 * The murmur hash is a relatively fast hash function from
 * http://murmurhash.googlepages.com/ for platforms with efficient
 * multiplication.
 * 
 * This is a re-implementation of the original C code plus some
 * additional features.
 * 
 * Public domain.
 * 
 * @author Viliam Holub
 * @version 1.0.2
 *
 */
public final class MurmurHash {
    
    // all methods static; private constructor. 
    private MurmurHash() {}


    /**
     * Generates 32 bit hash from byte array of the given length and
     * seed.
     *
     * @param data byte array to hash
     * @param length length of the array to hash
     * @param seed initial seed value
     * @return 32 bit hash of the given array
     */
    public static int hash32(final byte[] data, int length, int seed) {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        // Initialize the hash to a random value
        int h = seed^length;
        int length4 = length/4;

        for (int i=0; i<length4; i++) {
            final int i4 = i*4;
            int k = (data[i4+0]&0xff) +((data[i4+1]&0xff)<<8)
                    +((data[i4+2]&0xff)<<16) +((data[i4+3]&0xff)<<24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        // Handle the last few bytes of the input array
        switch (length%4) {
            case 3: h ^= (data[(length&~3) +2]&0xff) << 16;
            case 2: h ^= (data[(length&~3) +1]&0xff) << 8;
            case 1: h ^= (data[length&~3]&0xff);
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    /**
     * Modified MurmurHash that accepts as input a startIndex and an endIndex
     * Generates 32 bit hash from byte array of given start index and end index
     *
     * @param data byte array to hash
     * @param startIndex the index I will start considering from for the hashing
     * @param endIndex the index I will stop for the hashing
     * @return 32 bit hash of the given array
     */
    public static int myMurmurHash32(final byte[] data, int startIndex, int endIndex) {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        final int seed = 0x9747b28c;
        //the length of the data is the end - start +1
        int length = 1 + endIndex - startIndex;

        // Initialize the hash to a random value
        int h = seed^length;
        int length4 = length/4;

        //I start loop at startIndex
        for (int i=0; i<length4; i++) {
            final int i4 = i*4;
            int k = (data[i4+startIndex]&0xff) +((data[i4+startIndex+1]&0xff)<<8)
                    +((data[i4+startIndex+2]&0xff)<<16) +((data[i4+startIndex+3]&0xff)<<24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        // Handle the last few bytes of the input array
        switch (length%4) {
            case 3: h ^= (data[((length&~3) +2) + startIndex]&0xff) << 16;
            case 2: h ^= (data[((length&~3) +1) + startIndex]&0xff) << 8;
            case 1: h ^= (data[(length&~3) + startIndex]&0xff);
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }


    /** 
     * Generates 32 bit hash from byte array with default seed value.
     * 
     * @param data byte array to hash
     * @param length length of the array to hash
     * @return 32 bit hash of the given array
     */
    public static int hash32(final byte[] data, int length) {
        return hash32(data, length, 0x9747b28c);
    }

    /** 
     * Generates 32 bit hash from a string.
     * 
     * @param text string to hash
     * @param seed initial seed value
     * @return 32 bit hash of the given string
     */
    public static int hash32(final String text, int seed) {
        final byte[] bytes = text.getBytes(); 
        return hash32(bytes, bytes.length, seed);
    }

    public static int hash32(final int[] data, int start, int end){
        byte[] tempArray = convertIntegersToBytes(data);
        byte[] tempArray2 = Arrays.copyOfRange(tempArray, start, end);
        return hash32(tempArray2, tempArray2.length);
    }

    public static byte[] convertIntegersToBytes (int[] integers) {
        if (integers != null) {
            byte[] outputBytes = new byte[integers.length * 4];

            for(int i = 0, k = 0; i < integers.length; i++) {
                int integerTemp = integers[i];
                for(int j = 0; j < 4; j++, k++) {
                    outputBytes[k] = (byte)((integerTemp >> (8 * j)) & 0xFF);
                }
            }
            return outputBytes;
        } else {
            return null;
        }
    }

    /** 
     * Generates 64 bit hash from byte array of the given length and seed.
     * 
     * @param data byte array to hash
     * @param length length of the array to hash
     * @param seed initial seed value
     * @return 64 bit hash of the given array
     */
    public static long hash64(final byte[] data, int length, int seed) {
        final long m = 0xc6a4a7935bd1e995L;
        final int r = 47;

        long h = (seed&0xffffffffl)^(length*m);

        int length8 = length/8;

        for (int i=0; i<length8; i++) {
            final int i8 = i*8;
            long k =  ((long)data[i8+0]&0xff)      +(((long)data[i8+1]&0xff)<<8)
                    +(((long)data[i8+2]&0xff)<<16) +(((long)data[i8+3]&0xff)<<24)
                    +(((long)data[i8+4]&0xff)<<32) +(((long)data[i8+5]&0xff)<<40)
                    +(((long)data[i8+6]&0xff)<<48) +(((long)data[i8+7]&0xff)<<56);
            
            k *= m;
            k ^= k >>> r;
            k *= m;
            
            h ^= k;
            h *= m; 
        }
        
        switch (length%8) {
        case 7: h ^= (long)(data[(length&~7)+6]&0xff) << 48;
        case 6: h ^= (long)(data[(length&~7)+5]&0xff) << 40;
        case 5: h ^= (long)(data[(length&~7)+4]&0xff) << 32;
        case 4: h ^= (long)(data[(length&~7)+3]&0xff) << 24;
        case 3: h ^= (long)(data[(length&~7)+2]&0xff) << 16;
        case 2: h ^= (long)(data[(length&~7)+1]&0xff) << 8;
        case 1: h ^= (long)(data[length&~7]&0xff);
                h *= m;
        };
     
        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        return h;
    }
    
    /** 
     * Generates 64 bit hash from byte array with default seed value.
     * 
     * @param data byte array to hash
     * @param length length of the array to hash
     * @return 64 bit hash of the given string
     */
    public static long hash64(final byte[] data, int length) {
        return hash64(data, length, 0xe17a1465);
    }

    /** 
     * Generates 64 bit hash from a string.
     * 
     * @param text string to hash
     * @param seed initial seed value
     * @return 64 bit hash of the given string
     */
    public static long hash64(final String text, int seed) {
        final byte[] bytes = text.getBytes(); 
        return hash64(bytes, bytes.length, seed);
    }

}