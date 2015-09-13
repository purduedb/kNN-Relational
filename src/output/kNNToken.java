package output;

import java.util.Comparator;
import java.util.PriorityQueue;

import data.Tuple;

public class kNNToken extends PriorityQueue<Tuple> {

	public int numIO;
	private int k;
	
	public Tuple.Location focalPoint;
	
	public kNNToken(){
		numIO = 0;
	}
	
	public kNNToken(int initialSize, Comparator<Tuple> descComparer, int k, Tuple.Location focalPoint) {
		
		super(initialSize, descComparer);
		numIO = 0;
		this.k = k;
		this.focalPoint = focalPoint;
	}
	
	public void insert(Tuple tuple) {
		tuple.setDistance(focalPoint);
		
		if (this.size() < k)
			this.add(tuple);
		else {
			if (this.peek().distance > tuple.distance) {
				this.remove();
				this.add(tuple);
			}
		}
	}
	
}
