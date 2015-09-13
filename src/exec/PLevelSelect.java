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

public class PLevelSelect {

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
				
		kNNToken tempToken = new kNNToken(50, descComparer, k, qLocation);
		for (Integer custID : ordersGroupedByCustId.keySet()) {
			if (ordersGroupedByCustId.get(custID).size() <= countThreshold)
				continue;
			
			Customer customer = customerHashMap.get(custID);
			double radius = Math.sqrt(Math.pow((customer.location.xCoord - qLocation.xCoord), 2)
					+ Math.pow((customer.location.yCoord - qLocation.yCoord), 2));
			customer.distance = radius;
			tempToken.insert(customer);			
		}
		while (tempToken.size() > 0) {
			Customer customer = (Customer)tempToken.remove();
			if (!Common.kInCircle(k, qLocation, customer.distance, customerQTree)) {
				outputToken.insert(customer);
				while(tempToken.size() > 0)
					outputToken.insert(tempToken.remove());
			}
		}
		
		return outputToken;
	}

	public kNNToken kNNFirst(int k, double priceThreshold, int countThreshold, Tuple.Location qLocation,
			QuadTree customerQTree, HashMap<Integer, ArrayList<Order>> orderHashMap) {
		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		kNNToken outputToken = new kNNToken(50, descComparer, k, qLocation);

		ArrayList<QuadTree> locality = Common.getLocality(k, qLocation, customerQTree);
		PriorityQueue<Tuple> kNNTuples = Common.getkNNFromLocality(k, qLocation, locality);
		while (kNNTuples.size() > 0) {
			Customer customer = (Customer)kNNTuples.remove();
			int numQualifyingOrders = 0;
			if (orderHashMap.containsKey(customer.custKey)) {
				for (Order order : orderHashMap.get(customer.custKey)) {
					if (order.totalPrice < priceThreshold)
						numQualifyingOrders++;
				}
			}
			if (numQualifyingOrders > countThreshold) {
				customer.numOrders = numQualifyingOrders;
				outputToken.insert(customer);
			}
		}		
		
		return outputToken;
	}
	
	public kNNToken optimized(int k, double priceThreshold, int countThreshold, Tuple.Location qLocation, QuadTree customerQTree,
			HashMap<Integer, Customer> customerHashMap, BPTree<Double, ArrayList<Order>> orderBPTree, HashMap<Integer, ArrayList<Order>> orderHashMap,
			double priceSelectivity, double countSelectivity) {
		
		QuadTree enclosingLeafNode = customerQTree.searchEnclosingLeaf(qLocation);
		CatalogEntry catalogEntry = CostEstimator.searchInCatalog(enclosingLeafNode.centerCatalog, k);
		
		int kNNFirstCost = 0;
		int relFirstCost = 0;
		int numOrdersPerCustomer = orderBPTree.size() / customerHashMap.size();
		
		// Relational First
		int numTuplesQualifyingPrice = (int)(orderBPTree.size() * (1 - priceSelectivity));
		int overallRelationallyQualifying = (int)(numTuplesQualifyingPrice/numOrdersPerCustomer * (1 - countSelectivity));
		relFirstCost = 2 * numTuplesQualifyingPrice + (int)(overallRelationallyQualifying * (Math.log(k)/Math.log(2) + catalogEntry.numBlocks));
		
		// kNN First
		int kNNSelectionCost = (int)(catalogEntry.numPoints);
		kNNFirstCost = kNNSelectionCost + numOrdersPerCustomer * k;
		
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
