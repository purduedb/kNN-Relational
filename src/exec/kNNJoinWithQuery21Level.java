package exec;

import index.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import output.kNNToken;

import data.LineItem;
import data.Order;
import data.Supplier;
import data.Tuple;

public class kNNJoinWithQuery21Level {
	
	public int numIO;
	

	// IO Cost = numBlocksOuter * k / (QuadTree.MAX_OBJECTS * selectivity) 
	public ArrayList<String> localityGuided(int k, int threshold, QuadTree outerQTree, QuadTree innerQTree, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier) {
		numIO = 0;
		ArrayList<String> output = new ArrayList<String>();

		ArrayList<QuadTree> outerQueue = new ArrayList<QuadTree>();
		outerQueue.add(outerQTree);
		LocalityGuidedScan scan = new LocalityGuidedScan();
		
		while (!outerQueue.isEmpty()) {
			
			QuadTree outerNode = outerQueue.remove(0);
			if (outerNode.isLeaf) {				
				if (outerNode.numTuples == 0)
					continue;
				numIO++;
				ArrayList<Tuple> qualifyingTuples = localityGuidedQualTuples(scan, k, threshold, outerNode, innerQTree, orders, itemsByOrder, itemsBySupplier);				
				for (Tuple outerTuple : outerNode.tuples) {
					PriorityQueue<Tuple> kNNQueue = Common.getkNN(k, outerTuple.location, qualifyingTuples);
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
	
	private ArrayList<Tuple> localityGuidedQualTuples(LocalityGuidedScan scan, int k, int threshold, QuadTree outerNode, QuadTree innerQTree, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier) {
		
		scan.reset(innerQTree, outerNode);
		
		ArrayList<Tuple> qualifyingTuples = new ArrayList<Tuple>();
		double highestMaxDist = 0;
		int count = 0;
		QuadTree innerNode;
		while ((innerNode = scan.getNextBlock()) != null) {
			numIO++;
			double maxDist = Common.maxDist(outerNode, innerNode);
			if (maxDist > highestMaxDist)
				highestMaxDist = maxDist;
			
			kNNToken token = new kNNToken();
			ArrayList<Tuple> qualifyingFromInnerNode = Common.processPredicateQ21(token, threshold, innerNode, orders, itemsByOrder, itemsBySupplier);
			numIO+=token.numIO;
			qualifyingTuples.addAll(qualifyingFromInnerNode);
			count += qualifyingFromInnerNode.size();
			
			if (count >= k) {
				scan.setMinDistThreshold(highestMaxDist);
				break;
			}
		}

		while ((innerNode = scan.getNextBlock()) != null) {
			numIO++;
			kNNToken token = new kNNToken();
			ArrayList<Tuple> qualifyingFromInnerNode = Common.processPredicateQ21(token, threshold, innerNode, orders, itemsByOrder, itemsBySupplier);
			qualifyingTuples.addAll(qualifyingFromInnerNode);
			numIO+=token.numIO;
		}

		return qualifyingTuples;
	}

	// IO Cost = numInnerBlocks + numInnerBlocks*selectivity + numBlocksOuter * k / (QuadTree.MAX_OBJECTS)
	
	public ArrayList<String> kNNMAterialized(int k, int threshold, QuadTree outerQTree, QuadTree innerQTree, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier) {

		numIO = 0;
		// Build a materialized view
		ArrayList<Tuple> qualifyingTuples = new ArrayList<Tuple>();
		ArrayList<QuadTree> innerQueue = new ArrayList<QuadTree>();
		innerQueue.add(innerQTree);
		while (!innerQueue.isEmpty()) {
			QuadTree innerNode = innerQueue.remove(0);

			if (innerNode.isLeaf) {
				numIO++;
				for (Tuple innerTuple : innerNode.tuples) {
					kNNToken token = new kNNToken();
					if (Common.relPredicateQ21(token, threshold, (Supplier)innerTuple, orders, itemsByOrder, itemsBySupplier))
						qualifyingTuples.add(innerTuple);
					numIO+=token.numIO;
				}
			}
			else {
				for (QuadTree child : innerNode.subTrees) 					
					innerQueue.add(child);
			}
		}

		QuadTree qualifiedQTree = new QuadTree(innerQTree.bounds);
		qualifiedQTree.insert(qualifyingTuples);
		numIO+=qualifiedQTree.getNumLeaves();


		// Do a plain kNN join
		ArrayList<String> output = new ArrayList<String>();

		ArrayList<QuadTree> outerQueue = new ArrayList<QuadTree>();
		outerQueue.add(outerQTree);

		while (!outerQueue.isEmpty()) {
			QuadTree outerNode = outerQueue.remove(0);
			if (outerNode.isLeaf) {
				if (outerNode.numTuples == 0)
					continue;
				numIO++;
				ArrayList<QuadTree> locality = Common.getLocality(k, outerNode, qualifiedQTree);
				numIO+=locality.size();
				for (Tuple outerTuple: outerNode.tuples) {
					PriorityQueue<Tuple> kNNQueue = Common.getkNNFromLocality(k, outerTuple.location, locality);
					output.add(Common.flushOutput(outerTuple.location, kNNQueue));					
				}
			}
			else {
				for (QuadTree child : outerNode.subTrees) {					
					outerQueue.add(child);
				}
			}
		}

		return output;
	}

	
}
