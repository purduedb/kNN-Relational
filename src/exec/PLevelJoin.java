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

public class PLevelJoin {

	public ArrayList<String> relationalFirst(int k, double priceThreshold, int countThreshold, BPTree<Double, ArrayList<Order>> orderBPTree, 
			HashMap<Integer, Customer> customerHashMap, QuadTree supplierQTree, QuadTree customerQTree) {

		ArrayList<String> output = new ArrayList<String>();
		Comparator<Tuple> descComparer = new Common.DataDescComparer();


		// Determine the qualifyingList (materialized relation of qualifying tuples)
		ArrayList<Customer> qualifyingList = new ArrayList<Customer>();
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
			qualifyingList.add(customerHashMap.get(custID));
		}

		ArrayList<QuadTree> traversalQ = new ArrayList<QuadTree>();
		traversalQ.add(supplierQTree);

		while (!traversalQ.isEmpty()) {
			QuadTree outerNode = traversalQ.remove(0);
			if (outerNode.isLeaf) {
				for (Tuple outerTuple : outerNode.tuples) {
					kNNToken tempToken = new kNNToken(50, descComparer, k, outerTuple.location);
					for (Customer customer : qualifyingList) {
						double radius = Math.sqrt(Math.pow((customer.location.xCoord - outerTuple.location.xCoord), 2)
								+ Math.pow((customer.location.yCoord - outerTuple.location.yCoord), 2));
						customer.distance = radius;
						tempToken.insert(customer);
					}
					kNNToken outputToken = new kNNToken(50, descComparer, k, outerTuple.location);
					while (tempToken.size() > 0) {
						Customer customer = (Customer)tempToken.remove();
						if (!Common.kInCircle(k, outerTuple.location, customer.distance, customerQTree)) {
							outputToken.insert(customer);
							while(tempToken.size() > 0)
								outputToken.insert(tempToken.remove());
						}
						if (outputToken.size() > 0)
							output.add(Common.flushOutput(outerTuple.location, outputToken));
					}
				}
			}
			else {
				for (QuadTree child : outerNode.subTrees) {					
					traversalQ.add(child);
				}
			}
		}

		return output;		
	}

	public ArrayList<String> kNNFirst(int k, double priceThreshold, int countThreshold, HashMap<Integer, ArrayList<Order>> orderHashMap, 
			QuadTree supplierQTree, QuadTree customerQTree) {
		ArrayList<String> output = new ArrayList<String>();
		Comparator<Tuple> descComparer = new Common.DataDescComparer();

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(supplierQTree);

		HashMap<Tuple, Boolean> cache = new HashMap<Tuple, Boolean>();

		while (!queue.isEmpty()) {
			QuadTree outerNode = queue.remove(0);
			if (outerNode.isLeaf) {
				ArrayList<QuadTree> locality = Common.getLocality(k, outerNode, customerQTree);
				//System.out.println(locality.size() / (double)Common.getNumLeafs(customerQTree));
				for (Tuple outerTuple: outerNode.tuples) {
					PriorityQueue<Tuple> kNNQueue = Common.getkNNFromLocality(k, outerTuple.location, locality);

					// Filter out tuples that do not qualify the relation join predicate
					PriorityQueue<Tuple> filteredQueue = new PriorityQueue<Tuple>(50, descComparer);				
					for (Tuple innerTuple : kNNQueue) {
						// OPTIMIZE HERE
						if (cache.containsKey(innerTuple)) {
							if (cache.get(innerTuple))
								filteredQueue.add(innerTuple);
						}
						else {
							boolean qualifies = checkPredicate(orderHashMap, priceThreshold, countThreshold, (Customer) innerTuple);
							if (qualifies)
								filteredQueue.add(innerTuple);
							cache.put(innerTuple, qualifies);
						}
					}

					if (filteredQueue.size() > 0)
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

	private static boolean checkPredicate(HashMap<Integer, ArrayList<Order>> orderHashMap, double priceThreshold, int countThreshold, Customer customer) {
		int count = 0;
		if (orderHashMap.containsKey(customer.custKey)) {
			for (Order order : orderHashMap.get(customer.custKey)) {
				if (order.totalPrice < priceThreshold)
					count++;
			}
		}
		if (count > countThreshold )
			return true;
		return false;
	}

	public ArrayList<String> optimized(int k, ArrayList<CatalogEntry> catalog, double priceThreshold, int countThreshold, BPTree<Double, ArrayList<Order>> orderBPTree, 
			HashMap<Integer, Customer> customerHashMap, HashMap<Integer, ArrayList<Order>> orderHashMap, QuadTree supplierQTree, QuadTree customerQTree,
			double priceSelectivity, double countSelectivity) {

		int kNNFirstCost = 0;
		kNNFirstCost += CostEstimator.searchInCatalog(catalog, k).numBlocks;
		//kNNFirstCost += k * Common.getNumLeafs(supplierQTree);

		//423-9222
		
		int numOrdersPerCustomer = orderBPTree.size() / customerHashMap.size();
		int numTuplesQualifyingPrice = (int)(orderBPTree.size() * (1 - priceSelectivity));
		int overallRelationallyQualifying = (int)(numTuplesQualifyingPrice/numOrdersPerCustomer * (1 - countSelectivity));
		
		
		int relFirstCost = 0;
		relFirstCost = (int)(Common.getNumLeafs(supplierQTree) * (overallRelationallyQualifying) / 500);
		relFirstCost += (double)(Common.getNumLeafs(supplierQTree) * Math.log(k) /Math.log(2) * 2);
		
		System.out.println(kNNFirstCost + "  vs.  " + relFirstCost);
		if (kNNFirstCost < relFirstCost)
			return kNNFirst(k, priceThreshold, countThreshold, orderHashMap, supplierQTree, customerQTree);
		else
			return relationalFirst(k, priceThreshold, countThreshold, orderBPTree, customerHashMap, supplierQTree, customerQTree);

	}
	
}
