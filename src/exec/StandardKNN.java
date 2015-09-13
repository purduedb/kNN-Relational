package exec;

import index.QuadTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import data.Tuple;

public class StandardKNN {
	
	// No index (Only for verification purposes)
	public PriorityQueue<Tuple> kNNSelect(int k, Tuple.Location pointLocation, ArrayList<Tuple> arr) {
		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		PriorityQueue<Tuple> kNNQueue = new PriorityQueue<Tuple>(50, descComparer);


		for (Tuple entry : arr) {
			entry.setDistance(pointLocation);
			if (kNNQueue.size() < k)
				kNNQueue.add(entry);
			else {
				if (kNNQueue.peek().distance > entry.distance) {
					kNNQueue.remove();
					kNNQueue.add(entry);
				}
			}
		}

		return kNNQueue;
		//return Common.flushOutput(pointLocation, kNNQueue);
	}

	// No index (Only for verification purposes)
	public ArrayList<String> kNNJoin(int k, ArrayList<Tuple> outerArr, ArrayList<Tuple> innerArr) {

		ArrayList<String> output = new ArrayList<String>();

		for (Tuple outerTuple : outerArr)
			output.add(Common.flushOutput(outerTuple.location, kNNSelect(k, outerTuple.location, innerArr)));

		return output;
	}

	// With Quad Tree
	public ArrayList<String> kNNJoin(int k, QuadTree outerQTree, QuadTree innerQTree) {
		ArrayList<String> output = new ArrayList<String>();

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outerQTree);

		while (!queue.isEmpty()) {
			QuadTree outerNode = queue.remove(0);
			if (outerNode.isLeaf) {
				ArrayList<QuadTree> locality = Common.getLocality(k, outerNode, innerQTree);
				output.addAll(Common.getkNNFromLocality(k, outerNode, locality));
			}
			else {
				for (QuadTree child : outerNode.subTrees)					
					queue.add(child);				
			}
		}

		return output;
	}
	
	// With Quad Tree
	public PriorityQueue<Tuple> kNNSelect(int k, Tuple.Location pointLocation, QuadTree qtree) {
		ArrayList<QuadTree> locality = Common.getLocality(k, pointLocation, qtree);

		return Common.getkNNFromLocality(k, pointLocation, locality);
	}
}
