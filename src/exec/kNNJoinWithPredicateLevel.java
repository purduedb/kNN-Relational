package exec;

import index.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;


import data.Customer;
import data.Order;
import data.Tuple;

public class kNNJoinWithPredicateLevel {

	public ArrayList<String> SRJoin(int k, int threshold, QuadTree outerQTree, QuadTree innerQTree, HashMap<Integer, ArrayList<Order>> orders) {
		ArrayList<String> output = new ArrayList<String>();
		Comparator<Tuple> descComparer = new Common.DataDescComparer();

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outerQTree);

		while (!queue.isEmpty()) {
			QuadTree outerNode = queue.remove(0);

			if (outerNode.isLeaf) {
				ArrayList<QuadTree> locality = Common.getLocality(k, outerNode, innerQTree);
				ArrayList<Tuple> qualifyingLocalityTuples = new ArrayList<Tuple>();
				for (QuadTree innerNode : locality) {
					ArrayList<Tuple> qualifyingFromInnerNode = Common.processPredicateQ13(threshold, innerNode, orders);
					qualifyingLocalityTuples.addAll(qualifyingFromInnerNode);
				}

				for (Tuple outerTuple: outerNode.tuples) {
					double bestRadius = Double.MAX_VALUE;
					PriorityQueue<Tuple> filteredQueue = new PriorityQueue<Tuple>(50, descComparer);
					for (Tuple innerTuple : qualifyingLocalityTuples) {
						innerTuple.setDistance(outerTuple.location);
						if (innerTuple.distance < bestRadius) {
							if (!Common.kInCircle(k, outerTuple.location, innerTuple.distance, innerQTree)) {									
								filteredQueue.add(innerTuple);
							}
							else {
								bestRadius = innerTuple.distance;
							}
						}
					}
					output.add(Common.flushOutput(outerTuple.location, filteredQueue));
				}
			}
			else {
				for (QuadTree child : outerNode.subTrees) {					
					queue.add(child);
				}
			}
		}
		return output;
	}

	public ArrayList<String> kNNFirstWithCaching(int k, int threshold, QuadTree outerQTree, QuadTree innerQTree, HashMap<Integer, ArrayList<Order>> orders) {
		ArrayList<String> output = new ArrayList<String>();
		Comparator<Tuple> descComparer = new Common.DataDescComparer();

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outerQTree);

		while (!queue.isEmpty()) {
			QuadTree outerNode = queue.remove(0);
			if (outerNode.isLeaf) {
				ArrayList<QuadTree> locality = Common.getLocality(k, outerNode, innerQTree);
				for (Tuple outerTuple: outerNode.tuples) {
					PriorityQueue<Tuple> kNNQueue = Common.getkNNFromLocality(k, outerTuple.location, locality);

					// Filter out tuples that do not qualify the relation join predicate
					PriorityQueue<Tuple> filteredQueue = new PriorityQueue<Tuple>(50, descComparer);				
					for (Tuple innerTuple : kNNQueue)
						// First check if the tuple was already checked before
						if (innerTuple.processed) {
							if (innerTuple.relationallyQualifies)
								filteredQueue.add(innerTuple);
						}
						else if (Common.relPredicateQ13(threshold, (Customer)innerTuple, orders))
							filteredQueue.add(innerTuple);
				}
			}
			else {
				for (QuadTree child : outerNode.subTrees) {					
					queue.add(child);
				}
			}
		}

		return output;
	}

	public ArrayList<String> kNNFirst(int k, int threshold, QuadTree outerQTree, QuadTree innerQTree, HashMap<Integer, ArrayList<Order>> orders) {
		ArrayList<String> output = new ArrayList<String>();
		Comparator<Tuple> descComparer = new Common.DataDescComparer();

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outerQTree);

		while (!queue.isEmpty()) {
			QuadTree outerNode = queue.remove(0);
			if (outerNode.isLeaf) {
				ArrayList<QuadTree> locality = Common.getLocality(k, outerNode, innerQTree);
				for (Tuple outerTuple: outerNode.tuples) {
					PriorityQueue<Tuple> kNNQueue = Common.getkNNFromLocality(k, outerTuple.location, locality);

					// Filter out tuples that do not qualify the relation join predicate
					PriorityQueue<Tuple> filteredQueue = new PriorityQueue<Tuple>(50, descComparer);				
					for (Tuple innerTuple : kNNQueue)
						if (Common.relPredicateQ13(threshold, (Customer)innerTuple, orders))
							filteredQueue.add(innerTuple);
					
					output.add(Common.flushOutput(outerTuple.location, filteredQueue));
				}
			}
			else {
				for (QuadTree child : outerNode.subTrees) {					
					queue.add(child);
				}
			}
		}

		return output;
	}

}
