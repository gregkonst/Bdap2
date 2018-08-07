import java.util.HashMap;
import java.util.HashSet;

/**
 * SimilarPair contains the ids of two objects and their similarity.
 * 
 * @author Toon Van Craenendonck
 *
 */

/*
 * Class edited to be immutable
 */

final public class SimilarPair implements Comparable<SimilarPair>{
	private int id1;
	private int id2;
	private double sim;
	
	/**
	 * Construct a SimilarPair object
	 * @param id1 id of object 1
	 * @param id2 id of object 2
	 * @param sim their similarity
	 */
	public SimilarPair(int id1, int id2, double sim){
		this.id1 = id1;
		this.id2 = id2;
		this.sim = sim;
	}

	public void setFields(int id1, int id2, double sim) {
		this.id1 = id1;
		this.id2 = id2;
		this.sim = sim;
	}

	/**
	 * Comparing a SimilarPair object to another SimilarPair object.
	 */
	@Override
	public int compareTo(SimilarPair c) {
		if (sim < c.getSimilarity()){
			return -1;
		}else if (sim == c.getSimilarity()){
			return 0;
		}else{
			return 1;
		}
	}
	
	/**
	 * Returns the id of object 1.
	 */
	public int getId1() {
		return id1;
	}

	/**
	 * Returns the id of object 2.
	 */
	public int getId2() {
		return id2;
	}

	/**
	 * Returns the similarity between the objects.
	 */
	public double getSimilarity(){
		return sim;
	}

	public void setSimilarity(double sim){
		this.sim = sim;
	}

    @Override
    public String toString() {
        return id1+","+id2+","+sim;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SimilarPair that = (SimilarPair) o;

		if (id1 != that.id1) return false;
		return id2 == that.id2;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = id1;
		result = 31 * result + id2;
		temp = Double.doubleToLongBits(sim);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
