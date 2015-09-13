package exec;

import index.QuadTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

import output.kNNToken;

import data.Customer;
import data.LineItem;
import data.Order;
import data.Supplier;
import data.Tuple;

public class Common {

	public static Tuple.Location getCenter(QuadTree node) {
		Tuple t = new Tuple();
		t.location.xCoord = node.bounds.x + node.bounds.width/2;
		t.location.yCoord = node.bounds.y + node.bounds.height/2;
		return t.location;
	}
	
	public static double minDist(QuadTree R, QuadTree S) {
		double sumOfSquares = 0;

		if (R.bounds.x > (S.bounds.x + S.bounds.width))
			sumOfSquares += Math.pow(S.bounds.x + S.bounds.width - R.bounds.x, 2);
		else if (S.bounds.x > (R.bounds.x + R.bounds.width))
			sumOfSquares += Math.pow(S.bounds.x - (R.bounds.x + R.bounds.width), 2);

		if (R.bounds.y > (S.bounds.y + S.bounds.height))
			sumOfSquares += Math.pow(R.bounds.y - (S.bounds.y + S.bounds.height), 2);
		else if (S.bounds.y > (R.bounds.y + R.bounds.height))
			sumOfSquares += Math.pow(S.bounds.y - (R.bounds.y + R.bounds.height), 2);

		return Math.sqrt(sumOfSquares);
	}

	public static double maxDist(QuadTree R, QuadTree S) {
		double sumOfSquares = 0;

		if (Math.abs(R.bounds.x - (S.bounds.x + S.bounds.width)) > Math.abs(S.bounds.x - (R.bounds.x + R.bounds.width)))
			sumOfSquares += Math.pow(R.bounds.x - (S.bounds.x + S.bounds.width), 2);
		else 
			sumOfSquares += Math.pow(S.bounds.x - (R.bounds.x + R.bounds.width), 2);

		if (Math.abs(R.bounds.y - (S.bounds.y + S.bounds.height)) > Math.abs(S.bounds.y - (R.bounds.y + R.bounds.height))) 
			sumOfSquares += Math.pow(R.bounds.y - (S.bounds.y + S.bounds.height), 2);
		else
			sumOfSquares += Math.pow(S.bounds.y - (R.bounds.y + R.bounds.height), 2);

		return Math.sqrt(sumOfSquares);
	}

	public static double minDist (Tuple.Location pointLocation, QuadTree node){
		double sumOfSquares = 0;

		if (pointLocation.xCoord < node.bounds.x)
			sumOfSquares += Math.pow(node.bounds.x - pointLocation.xCoord, 2);
		else if (pointLocation.xCoord > (node.bounds.x + node.bounds.width))
			sumOfSquares += Math.pow(pointLocation.xCoord - node.bounds.x - node.bounds.width, 2);

		if (pointLocation.yCoord < node.bounds.y)
			sumOfSquares += Math.pow(node.bounds.y - pointLocation.yCoord, 2);
		else if (pointLocation.yCoord > (node.bounds.y + node.bounds.height))
			sumOfSquares += Math.pow(pointLocation.yCoord - node.bounds.y - node.bounds.height, 2);

		return Math.sqrt(sumOfSquares);
	}

	public static double maxDist (Tuple.Location pointLocation, QuadTree node){
		double sumOfSquares = 0;

		if (pointLocation.xCoord < (node.bounds.x + node.bounds.width/2.0))
			sumOfSquares += Math.pow(node.bounds.x + node.bounds.width - pointLocation.xCoord, 2);
		else
			sumOfSquares += Math.pow(pointLocation.xCoord - node.bounds.x, 2);

		if (pointLocation.yCoord < (node.bounds.y + node.bounds.height/2.0))
			sumOfSquares += Math.pow(node.bounds.y + node.bounds.height - pointLocation.yCoord, 2);
		else
			sumOfSquares += Math.pow(pointLocation.yCoord - node.bounds.y, 2);

		return Math.sqrt(sumOfSquares);
	}

	public static boolean kInCircle(int k, Tuple.Location center, double radius, QuadTree qtree) {
		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> minDistQueue = new PriorityQueue<QuadTree>(50, comparer);

		qtree.distance = minDist(center, qtree);
		minDistQueue.add(qtree);
		int count = 0;
		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.distance > radius) { // minDist
				break;
			}
			if (maxDist(center, node) < radius) {
				count += node.numTuples;
				if (count >= k)
					return true;
			}
			else if (node.isLeaf) {
				for (Tuple t : node.tuples) {
					double distance = Math.sqrt(Math.pow((t.location.xCoord - center.xCoord), 2)
							+ Math.pow((t.location.yCoord - center.yCoord), 2));
					if (distance < radius) {
						count++;
						if (count == k)
							return true;
					}
				}
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = minDist(center, child);
					minDistQueue.add(child);
				}
			}
		}

		return false;
	}

	public static ArrayList<QuadTree> getLocality(int k, Tuple.Location pointLocation, QuadTree qtree) {
		ArrayList<QuadTree> locality = new ArrayList<QuadTree>();

		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> minDistQueue = new PriorityQueue<QuadTree>(50, comparer);

		qtree.distance = minDist(pointLocation, qtree);
		minDistQueue.add(qtree);
		int currentCount = 0;
		double highestMaxDist = 0;
		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				currentCount += node.numTuples;
				locality.add(node);
				double maxDist = maxDist(pointLocation, node);
				if (maxDist > highestMaxDist  && node.numTuples > 0)
					highestMaxDist = maxDist;
				if (currentCount >= k)
					break;
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = minDist(pointLocation, child);
					minDistQueue.add(child);
				}
			}
		}

		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				if (node.distance > highestMaxDist)
					break;
				if (!locality.contains(node))
					locality.add(node);
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = minDist(pointLocation, child);
					minDistQueue.add(child);
				}
			}
		}

		return locality;
	}

//	public static ArrayList<QuadTree> getLocality(int k, QuadTree outerQTree, QuadTree innerQTree) {
//		ArrayList<QuadTree> locality = new ArrayList<QuadTree>();
//
//		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
//		PriorityQueue<QuadTree> minDistQueue = new PriorityQueue<QuadTree>(50, comparer);
//
//		Tuple t = new Tuple();
//		t.location.xCoord = outerQTree.bounds.x + outerQTree.bounds.width / 2;
//		t.location.yCoord = outerQTree.bounds.y + outerQTree.bounds.height / 2;
//
//
//		// MINDIST Scanning
//		innerQTree.distance = minDist(t.location, innerQTree);
//		minDistQueue.add(innerQTree);
//		int currentCount = 0;
//		double highestMaxDist = 0;
//		while (!minDistQueue.isEmpty()) {
//			QuadTree node = minDistQueue.remove();
//			if (node.isLeaf) {
//				currentCount += node.numTuples;
//				//locality.add(node);
//				double maxDist = maxDist(outerQTree, node);
//				if (maxDist > highestMaxDist && node.numTuples > 0)
//					highestMaxDist = maxDist;
//				if (currentCount >= k)
//					break;
//			}
//			else {
//				for (QuadTree child : node.subTrees) {
//					child.distance = minDist(t.location, child);
//					minDistQueue.add(child);					
//				}
//			}
//		}
//
//		minDistQueue.clear();
//		innerQTree.distance = minDist(outerQTree, innerQTree);
//		minDistQueue.add(innerQTree);
//		while (!minDistQueue.isEmpty()) {
//			QuadTree node = minDistQueue.remove();
//			if (node.isLeaf) {
//				if (node.distance > highestMaxDist)
//					break;
//				if (node.numTuples > 0)
//					locality.add(node);
//			}
//			else {
//				for (QuadTree child : node.subTrees) {
//					child.distance = minDist(outerQTree, child);
//					minDistQueue.add(child);
//				}
//			}
//		}
//
//		return locality;
//	}

	public static ArrayList<QuadTree> getLocality(int k, QuadTree outerQTree, QuadTree innerQTree) {
		ArrayList<QuadTree> locality = new ArrayList<QuadTree>();

		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> minDistQueue = new PriorityQueue<QuadTree>(50, comparer);

		// MINDIST Scanning
		innerQTree.distance = minDist(outerQTree, innerQTree);
		minDistQueue.add(innerQTree);
		int currentCount = 0;
		double highestMaxDist = 0;
		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				if (node.numTuples == 0)
					continue;
				currentCount += node.numTuples;
				locality.add(node);
				if (node.mDistance > highestMaxDist)
					highestMaxDist = node.mDistance;
				if (currentCount >= k)
					break;
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = minDist(outerQTree, child);
					child.mDistance = maxDist(outerQTree, child);
					minDistQueue.add(child);					
				}
			}
		}

		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				if (node.numTuples == 0)
					continue;
				if (node.distance > highestMaxDist)
					break;
					locality.add(node);
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = minDist(outerQTree, child);
					child.mDistance = maxDist(outerQTree, child);
					minDistQueue.add(child);
				}
			}
		}

		return locality;
	}
	
	public static String flushOutput(Tuple.Location pointLocation, PriorityQueue<Tuple> kNNQueue) {
		String output = pointLocation.xCoord + "," + pointLocation.yCoord + ": ";
		while (!kNNQueue.isEmpty())
			output += kNNQueue.remove().distance + " - ";

		return output;
	}

	public static PriorityQueue<Tuple> getkNNFromLocality(int k, Tuple.Location pointLocation, ArrayList<QuadTree> locality) {
		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		PriorityQueue<Tuple> kNNQueue = new PriorityQueue<Tuple>(50, descComparer);
		for (QuadTree node : locality) {
			if (kNNQueue.size()>k && node.distance > kNNQueue.peek().distance)
				break;
			for (Tuple entry : node.tuples) {
				entry.setDistance(pointLocation);

				if (kNNQueue.size() < k)
					kNNQueue.add(entry);
				else if (kNNQueue.peek().distance > entry.distance) {
					kNNQueue.remove();
					kNNQueue.add(entry);
				}					
			}
		}
		return kNNQueue;
	}

	public static ArrayList<Tuple> processPredicateQ13(int threshold, QuadTree innerNode, HashMap<Integer, ArrayList<Order>> orders) {
		ArrayList<Tuple> qualifyingTuples = new ArrayList<Tuple>();

		if (innerNode.processed) {
			if(!innerNode.relationallyQualifies)
				return qualifyingTuples;
			else {
				for (Tuple innerTuple : innerNode.tuples)
					if (innerTuple.relationallyQualifies)
						qualifyingTuples.add(innerTuple);					
			}
		}
		else {
			innerNode.processed = true;
			for (Tuple innerTuple : innerNode.tuples) {
				if (relPredicateQ13(threshold, (Customer)innerTuple, orders))
					qualifyingTuples.add(innerTuple);
			}
			if (qualifyingTuples.size() != 0)
				innerNode.relationallyQualifies = true;	
		}

		return qualifyingTuples;
	}

	public static ArrayList<Tuple> processPredicateQ21(kNNToken outputToken, int threshold, QuadTree innerNode, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier) {

		ArrayList<Tuple> qualifyingTuples = new ArrayList<Tuple>();

		if (innerNode.processed) {
			if(!innerNode.relationallyQualifies)
				return qualifyingTuples;
			else {
				for (Tuple innerTuple : innerNode.tuples)
					if (innerTuple.relationallyQualifies)
						qualifyingTuples.add(innerTuple);					
			}
		}
		else {
			innerNode.processed = true;
			for (Tuple innerTuple : innerNode.tuples) {
				if (relPredicateQ21(outputToken, threshold, (Supplier)innerTuple, orders, itemsByOrder, itemsBySupplier))
					qualifyingTuples.add(innerTuple);
			}
			if (qualifyingTuples.size() != 0)
				innerNode.relationallyQualifies = true;	
		}

		return qualifyingTuples;
	}

	public static boolean relPredicateQ13(int threshold, Customer customer, HashMap<Integer, ArrayList<Order>> orders) {

		customer.processed = true;

		ArrayList<Order> myOrders = orders.get(customer.custKey);
		if (myOrders != null && myOrders.size() > threshold) {
			customer.relationallyQualifies = true;
			return true;
		}

		customer.relationallyQualifies = false;

		return false;
	}

	public static boolean relPredicateQ13(int countThreshold, double priceThreshold, Customer customer, HashMap<Integer, ArrayList<Order>> orders) {

		int numQualifyingOrders = 0;
		if (orders.containsKey(customer.custKey)) {
			for (Order order : orders.get(customer.custKey)) {
				if (order.totalPrice < priceThreshold)
					numQualifyingOrders++;
			}
		}
		if (numQualifyingOrders > countThreshold)
			return true;

		return false;
	}

	public static boolean relPredicateQ21(kNNToken outputToken, int threshold, Supplier supplier, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier) {
		supplier.processed = true;

		boolean supplierFailedAlone;
		int count = 0;
		ArrayList<LineItem> supplierItems = itemsBySupplier.get(supplier.suppKey);
		outputToken.numIO++;

		for (LineItem item : supplierItems) {
			supplierFailedAlone = false;
			if (item.receiptDate > item.commitDate) {
				Order order = orders.get(item.orderKey);
				outputToken.numIO++;
				if (order.orderStatus == 'F') {
					ArrayList<LineItem> orderItems = itemsByOrder.get(item.orderKey);
					outputToken.numIO++;
					for (LineItem otherItem : orderItems) {
						if (otherItem.suppKey != item.suppKey) {
							supplierFailedAlone = true;
							break;
						}
					}
					if (supplierFailedAlone) {
						for (LineItem otherItem : orderItems) {
							if (otherItem.suppKey != item.suppKey) {
								if (otherItem.receiptDate <= otherItem.commitDate) {
									supplierFailedAlone = false;
									break;
								}
							}
						}
					}
				}
			}
			if(supplierFailedAlone) {
				count++;
			}
		}
		if (count < threshold) {
			supplier.relationallyQualifies = true;
			return true;
		}

		supplier.relationallyQualifies = false;

		return false;
	}

	public static PriorityQueue<Tuple> getkNN(int k, Tuple.Location pointLocation, ArrayList<Tuple> tuples) {
		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		PriorityQueue<Tuple> kNNQueue = new PriorityQueue<Tuple>(50, descComparer);
		for (Tuple entry : tuples) {
			entry.setDistance(pointLocation);

			if (kNNQueue.size() < k)
				kNNQueue.add(entry);
			else if (kNNQueue.peek().distance > entry.distance) {
				kNNQueue.remove();
				kNNQueue.add(entry);
			}
		}

		return kNNQueue;
	}

	public static QuadTree getRandomLeaf(QuadTree tree) {
		QuadTree leaf = tree;
		while (!leaf.isLeaf) {
			Random r = new Random();
			int i = r.nextInt(4);
			leaf = leaf.subTrees[i];
		}

		return leaf;
	}

	public static int getNumLeafs(QuadTree tree) {
		int total = 0;

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(tree);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);

			if (node.isLeaf) {
				total++;
			}
			else {
				queue.add(node.subTrees[0]);
				queue.add(node.subTrees[1]);
				queue.add(node.subTrees[2]);
				queue.add(node.subTrees[3]);
			}
		}

		return total;
	}

	public static ArrayList<String> getkNNFromLocality(int k, QuadTree outerNode, ArrayList<QuadTree> locality) {
		ArrayList<String> output = new ArrayList<String>();

		for (Tuple t : outerNode.tuples)
			output.add(Common.flushOutput(t.location, getkNNFromLocality(k, t.location, locality)));

		return output;
	}

	public static class DataDescComparer implements Comparator<Tuple>{

		@Override
		public int compare(Tuple a, Tuple b) {

			if (a.distance > b.distance)
				return -1;
			if (a.distance < b.distance)
				return 1;

			return 0;
		}
	}

	public static class DataAscComparer implements Comparator<Tuple>{

		@Override
		public int compare(Tuple a, Tuple b) {

			if (a.distance < b.distance)
				return -1;
			if (a.distance > b.distance)
				return 1;

			return 0;
		}
	}

	public static class QTreeNodeAscComparer implements Comparator<QuadTree>{

		@Override
		public int compare(QuadTree a, QuadTree b) {

			if (a.distance < b.distance)
				return -1;
			if (a.distance > b.distance)
				return 1;
			
			// Break Ties with maxDist
			if (a.mDistance < b.mDistance)
				return -1;
			if (a.mDistance > b.mDistance)
				return 1;
			
			return 0;
		}
	}
}
