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

public class QLevelJoin {

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
					kNNToken outputToken = new kNNToken(50, descComparer, k, outerTuple.location);
					for (Customer customer : qualifyingList) {
						double radius = Math.sqrt(Math.pow((customer.location.xCoord - outerTuple.location.xCoord), 2)
								+ Math.pow((customer.location.yCoord - outerTuple.location.yCoord), 2));
						customer.distance = radius;
						outputToken.insert(customer);
					}

					if (outputToken.size() > 0)
						output.add(Common.flushOutput(outerTuple.location, outputToken));
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

		ArrayList<QuadTree> outerQueue = new ArrayList<QuadTree>();
		outerQueue.add(supplierQTree);
		LocalityGuidedScan scan = new LocalityGuidedScan();

		HashMap<Tuple, Boolean> cache = new HashMap<Tuple, Boolean>();

		while (!outerQueue.isEmpty()) {

			QuadTree outerNode = outerQueue.remove(0);
			if (outerNode.isLeaf) {
				if (outerNode.numTuples == 0)
					continue;
				ArrayList<Tuple> qualifyingTuples = localityGuidedQualTuples(cache, scan, k, priceThreshold, countThreshold, outerNode, customerQTree, orderHashMap);				
				for (Tuple outerTuple : outerNode.tuples) {
					PriorityQueue<Tuple> kNNQueue = Common.getkNN(k, outerTuple.location, qualifyingTuples);
					if (kNNQueue.size() > 0)
						output.add(Common.flushOutput(outerTuple.location, kNNQueue));
				}				
			}
			else {
				for (QuadTree child : outerNode.subTrees)
					outerQueue.add(child);
			}
		}

		return output;
	}

	private ArrayList<Tuple> localityGuidedQualTuples(HashMap<Tuple, Boolean> cache, LocalityGuidedScan scan, int k, double priceThreshold, int countThreshold, QuadTree outerNode, QuadTree innerQTree, HashMap<Integer, ArrayList<Order>> orders) {

		scan.reset(innerQTree, outerNode);

		ArrayList<Tuple> qualifyingTuples = new ArrayList<Tuple>();
		double highestMaxDist = 0;
		int count = 0;
		QuadTree innerNode;
		while ((innerNode = scan.getNextBlock()) != null) {
			double maxDist = Common.maxDist(outerNode, innerNode);							
			if (maxDist > highestMaxDist)
				highestMaxDist = maxDist;

			ArrayList<Tuple> qualifyingFromInnerNode = processBlock(cache, priceThreshold, countThreshold, innerNode, orders);
			qualifyingTuples.addAll(qualifyingFromInnerNode);
			count += qualifyingFromInnerNode.size();

			if (count >= k) {
				scan.setMinDistThreshold(highestMaxDist);
				break;
			}
		}

		while ((innerNode = scan.getNextBlock()) != null) {
			ArrayList<Tuple> qualifyingFromInnerNode = processBlock(cache, priceThreshold, countThreshold, innerNode, orders);
			qualifyingTuples.addAll(qualifyingFromInnerNode);					
		}

		return qualifyingTuples;
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

		double rho = 1 - (priceSelectivity * countSelectivity);
		int kNNFirstCost = 0;
		int kRho = (int)(k/rho);
		if (kRho > customerQTree.numTuples)
			kRho = customerQTree.numTuples -1;
		// THERE IS A BUG THAT NEEDS TO BE FIXED HERE
		kNNFirstCost += CostEstimator.searchInCatalog(catalog, kRho).numBlocks;
		//kNNFirstCost += k * Common.getNumLeafs(supplierQTree);

		//423-9222

		int numOrdersPerCustomer = orderBPTree.size() / customerHashMap.size();
		int numTuplesQualifyingPrice = (int)(orderBPTree.size() * (1 - priceSelectivity));
		int overallRelationallyQualifying = (int)(numTuplesQualifyingPrice/numOrdersPerCustomer * (1 - countSelectivity));


		int relFirstCost = 0;
		relFirstCost = (int)(Common.getNumLeafs(supplierQTree) * (overallRelationallyQualifying) / 500);
		//relFirstCost += (double)(Common.getNumLeafs(supplierQTree) * Math.log(k) /Math.log(2) * 2);

		System.out.println(kNNFirstCost + "  vs.  " + relFirstCost);
		if (kNNFirstCost < relFirstCost)
			return kNNFirst(k, priceThreshold, countThreshold, orderHashMap, supplierQTree, customerQTree);
		else
			return relationalFirst(k, priceThreshold, countThreshold, orderBPTree, customerHashMap, supplierQTree, customerQTree);

	}

	private static ArrayList<Tuple> processBlock(HashMap<Tuple, Boolean> cache, double priceThreshold, int countThreshold, QuadTree innerNode, HashMap<Integer, ArrayList<Order>> orders) {
		ArrayList<Tuple> qualifying = new ArrayList<Tuple>();
		for (Tuple t : innerNode.tuples) {
			if (cache.containsKey(t)) {
				if (cache.get(t))
					qualifying.add(t);					
			}
			else {
				boolean qualifies = checkPredicate(orders, priceThreshold, countThreshold, (Customer)t);
				if (qualifies)
					qualifying.add(t);
				cache.put(t, qualifies);
			}

		}
		return qualifying;
	}
}
