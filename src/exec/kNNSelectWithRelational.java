package exec;

import index.QuadTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import output.kNNToken;

import data.Customer;
import data.LineItem;
import data.Order;
import data.Supplier;
import data.Tuple;

import exec.Common.QTreeNodeAscComparer;

public class kNNSelectWithRelational {

	// Can be used for Select Query on suppliers
	public PriorityQueue<Tuple> kNNSelectLocalityGuided(int k, int threshold, Tuple.Location pointLocation, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier, QuadTree qTree) {

		// do locality guided scanning

		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		kNNToken outputToken = new kNNToken(50, descComparer, k, pointLocation);

		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> minDistQueue = new PriorityQueue<QuadTree>(50, comparer);

		qTree.distance = Common.minDist(pointLocation, qTree);
		minDistQueue.add(qTree);
		int currentCount = 0;
		double highestMaxDist = 0;
		while (!minDistQueue.isEmpty()) {

			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				outputToken.numIO++;
				double maxDist = Common.maxDist(pointLocation, node);
				if (maxDist > highestMaxDist)
					highestMaxDist = maxDist;

				// currentCount += node.numTuples;
				for (Tuple tuple : node.tuples)
					if (Common.relPredicateQ21(outputToken, threshold, (Supplier)tuple, orders, itemsByOrder, itemsBySupplier)) {
						outputToken.insert(tuple);
						currentCount++;						
					}
				if (currentCount >= k)
					break;
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = Common.minDist(pointLocation, child);
					minDistQueue.add(child);
				}
			}
		}

		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				outputToken.numIO++;
				if (node.distance > highestMaxDist)
					break;
				for (Tuple tuple : node.tuples)
					if (Common.relPredicateQ21(outputToken, threshold, (Supplier)tuple, orders, itemsByOrder, itemsBySupplier))
						outputToken.insert(tuple);
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = Common.minDist(pointLocation, child);
					minDistQueue.add(child);
				}
			}
		}

		return outputToken;
	}

	// Used for Join query on Customers (Hybrid Join)
	public kNNToken kNNSelectLocalityGuided(int k, Supplier supplier, QuadTree customersQTree) {

		// do locality guided scanning
		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		kNNToken outputToken = new kNNToken(50, descComparer, k, supplier.location);

		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> minDistQueue = new PriorityQueue<QuadTree>(50, comparer);

		customersQTree.distance = Common.minDist(supplier.location, customersQTree);
		minDistQueue.add(customersQTree);
		int currentCount = 0;
		double highestMaxDist = 0;
		while (!minDistQueue.isEmpty()) {

			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				outputToken.numIO++;
				double maxDist = Common.maxDist(supplier.location, node);
				if (maxDist > highestMaxDist)
					highestMaxDist = maxDist;

				// currentCount += node.numTuples;
				for (Tuple tuple : node.tuples)
					if (supplier.acctBalance > ((Customer)tuple).acctBalance) {
						outputToken.insert(tuple);
						currentCount++;						
					}
				if (currentCount >= k)
					break;
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = Common.minDist(supplier.location, child);
					minDistQueue.add(child);
				}
			}
		}

		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				outputToken.numIO++;
				if (node.distance > highestMaxDist)
					break;
				for (Tuple tuple : node.tuples)
					if (supplier.acctBalance > ((Customer)tuple).acctBalance)
						outputToken.insert(tuple);
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = Common.minDist(supplier.location, child);
					minDistQueue.add(child);
				}
			}
		}

		return outputToken;
	}

	public PriorityQueue<Tuple> kNNSelectRelationalOperator(int k, int threshold, Tuple.Location pointLocation, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier, QuadTree qTree) {
		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		kNNToken outputToken = new kNNToken(50, descComparer, k, pointLocation);

		LocalityGuidedScan scan = new LocalityGuidedScan(qTree, pointLocation);
		double highestMaxDist = 0;
		int count = 0;
		QuadTree node;
		while ((node = scan.getNextBlock()) != null) {
			outputToken.numIO++;
			double maxDist = Common.maxDist(pointLocation, node);
			if (maxDist > highestMaxDist)
				highestMaxDist = maxDist;
			for (Tuple tuple : node.tuples)
				if (Common.relPredicateQ21(outputToken,threshold, (Supplier)tuple, orders, itemsByOrder, itemsBySupplier)) {
					outputToken.insert(tuple);
					count++;						
				}
			if (count >= k) {
				scan.setMinDistThreshold(highestMaxDist);
				break;
			}			
		}

		while ((node = scan.getNextBlock()) != null) {
			outputToken.numIO++;
			for (Tuple tuple : node.tuples)
				if (Common.relPredicateQ21(outputToken, threshold, (Supplier)tuple, orders, itemsByOrder, itemsBySupplier))
					outputToken.insert(tuple);						
		}

		return outputToken;
	}

	public PriorityQueue<Tuple> kNNSelectRelFirst(int k, int threshold, Tuple.Location pointLocation, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier, QuadTree qTree) {

		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		kNNToken outputToken = new kNNToken(50, descComparer, k, pointLocation);

		ArrayList<QuadTree> scanner = new ArrayList<QuadTree>();
		scanner.add(qTree);

		while (!scanner.isEmpty()) {
			QuadTree node = scanner.remove(0);
			if (node.isLeaf) {
				outputToken.numIO++;
				for (Tuple tuple : node.tuples) {
					if (Common.relPredicateQ21(outputToken, threshold, (Supplier)tuple, orders, itemsByOrder, itemsBySupplier)) {						
						outputToken.insert(tuple);
					}
				}
			}
			else {
				for (QuadTree childNode : node.subTrees)
					scanner.add(childNode);
			}
		}

		return outputToken;
	}

}
