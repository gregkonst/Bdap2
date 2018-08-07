public class MyPrimitiveArrayList {

    private int[] array;
    private int currentSize = 0;

    public MyPrimitiveArrayList(){
        array = new int[16];
    }

    public MyPrimitiveArrayList(int initialCapacity){
        array = new int[initialCapacity];
    }

    public void add(int element){
        if(currentSize == array.length){
            int newSize = (int)(array.length * 1.5);
            int[] biggerArray = new int[newSize];
            System.arraycopy(array, 0, biggerArray, 0, array.length);
            array = biggerArray;
        }
        array[currentSize++] = element;
    }

    public int get(int index){
        return array[index];
    }

    public int size(){
        return currentSize;
    }

}
