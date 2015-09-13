package exec;

import index.BPTree;
import index.QuadTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.SortedMap;

import optimizer.CatalogEntry;
import optimizer.CostEstimator;

import output.kNNToken;

import data.Customer;
import data.Order;
import data.Tuple;
import exec.Common.QTreeNodeAscComparer;

public class QLevelSelect {

	public kNNToken relationalFirst(int k, double priceThreshold, int countThreshold, Tuple.Location qLocation, BPTree<Double, ArrayList<Order>> orderBPTree, 
			HashMap<Integer, Customer> customerHashMap, QuadTree customerQTree) {

		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		kNNToken outputToken = new kNNToken(50, descComparer, k, qLocation);

		HashMap<Integer, ArrayList<Order>> ordersGroupedByCustId = new HashMap<Integer, ArrayList<Order>>();
		SortedMap<Double, ArrayList<Order>> sm = orderBPTree.subMap(orderBPTree.firstKey(), priceThreshold);
		for (ArrayList<Order> arr : sm.values())
			for (Order order : arr) {
				ArrayList<Order> orders = ordersGroupedByCustId.get(order.custKey);
				if (orders == null) {
					orders = new ArrayList<Order>();
					ordersGroupedByCustId.put(order.custKey, orders);
				}
				orders.add(order);
			}

		for (Integer custID : ordersGroupedByCustId.keySet()) {
			if (ordersGroupedByCustId.get(custID).size() <= countThreshold)
				continue;
			Customer customer = customerHashMap.get(custID);
			double radius = Math.sqrt(Math.pow((customer.location.xCoord - qLocation.xCoord), 2)
					+ Math.pow((customer.location.yCoord - qLocation.yCoord), 2));
			customer.distance = radius;
			outputToken.insert(customer);			
		}

		return outputToken;
	}

	public kNNToken kNNFirst(int k, double priceThreshold, int countThreshold, Tuple.Location qLocation,
			QuadTree customerQTree, HashMap<Integer, ArrayList<Order>> orderHashMap) {
		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		kNNToken outputToken = new kNNToken(50, descComparer, k, qLocation);

		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> minDistQueue = new PriorityQueue<QuadTree>(50, comparer);

		customerQTree.distance = Common.minDist(qLocation, customerQTree);
		minDistQueue.add(customerQTree);
		double minDistThreshold = 0;
		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.isLeaf) {
				outputToken.numIO++;
				for (Tuple tuple : node.tuples)
					if (Common.relPredicateQ13(countThreshold, priceThreshold, (Customer)tuple, orderHashMap))
						outputToken.insert(tuple);

				if (outputToken.size() == k) {
					minDistThreshold = outputToken.peek().distance;
					break;
				}
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = Common.minDist(qLocation, child);
					minDistQueue.add(child);
				}
			}
		}

		while (!minDistQueue.isEmpty()) {
			QuadTree node = minDistQueue.remove();
			if (node.distance > minDistThreshold)
				break;
			if (node.isLeaf) {
				outputToken.numIO++;
				for (Tuple tuple : node.tuples)
					if (Common.relPredicateQ13(countThreshold, priceThreshold, (Customer)tuple, orderHashMap))
						outputToken.insert(tuple);
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = Common.minDist(qLocation, child);
					minDistQueue.add(child);
				}
			}
		}
		
		System.out.println("VISITED " + outputToken.numIO);
		return outputToken;
	}

	public kNNToken distBrowsing(int k, double priceThreshold, int countThreshold, Tuple.Location qLocation,
			QuadTree customerQTree, HashMap<Integer, ArrayList<Order>> orderHashMap) {
		Comparator<Tuple> descComparer = new Common.DataDescComparer();

		DistanceBrowser browser = new DistanceBrowser(qLocation, customerQTree);		
		kNNToken outputToken = new kNNToken(50, descComparer, k, qLocation);

		while (true) {
			Tuple next = browser.getNext();
			if (next == null) 				
				break;

			else if (Common.relPredicateQ13(countThreshold, priceThreshold, (Customer)next, orderHashMap)) {
				outputToken.insert(next);
				if (outputToken.size() == k)
					break;				
			}
		}

		System.out.println("EXPLORED " + browser.numExploredLeaves);
		outputToken.numIO = browser.numExploredLeaves;
				
		return outputToken;
	}

	public kNNToken optimized(int k, double priceThreshold, int countThreshold, Tuple.Location qLocation, QuadTree customerQTree,
			HashMap<Integer, Customer> customerHashMap, BPTree<Double, ArrayList<Order>> orderBPTree, HashMap<Integer, ArrayList<Order>> orderHashMap,
			double priceSelectivity, double countSelectivity) {

		QuadTree enclosingLeafNode = customerQTree.searchEnclosingLeaf(qLocation);
		double rho = 1 - (priceSelectivity * countSelectivity);
		CatalogEntry catalogEntry = CostEstimator.searchInCatalog(enclosingLeafNode.centerCatalog, (int)(k/rho));

		int kNNFirstCost = 0;
		int relFirstCost = 0;
		int numOrdersPerCustomer = orderBPTree.size() / customerHashMap.size();

		// Relational First
		int numTuplesQualifyingPrice = (int)(orderBPTree.size() * (1 - priceSelectivity));
		int overallRelationallyQualifying = (int)(numTuplesQualifyingPrice/numOrdersPerCustomer * (1 - countSelectivity));
		relFirstCost = 2 * numTuplesQualifyingPrice + (int)(overallRelationallyQualifying * (Math.log(k)/Math.log(2)));

		// kNN First
		int kNNSelectionCost = (int)(catalogEntry.numPoints);
		kNNFirstCost = kNNSelectionCost * numOrdersPerCustomer;

		//System.out.println("kNNFirst Cost " + kNNFirstCost);
		//System.out.println("RelationalFirst Cost " + relFirstCost);

		if (kNNFirstCost < relFirstCost) {
			//System.err.println("Choosing kNN First");
			return kNNFirst(k, priceThreshold, countThreshold, qLocation, customerQTree, orderHashMap);
		}
		else {
			//System.err.println("Choosing Relational First");
			return relationalFirst(k, priceThreshold, countThreshold, qLocation, orderBPTree, customerHashMap, customerQTree);
		}

	}

}
