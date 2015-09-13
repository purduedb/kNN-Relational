package exec;

import index.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import data.Customer;
import data.Order;
import data.Tuple;

public class kNNJoinWithQuery13Level {
	
	public int numIO;
	

	// IO Cost = numBlocksOuter * k / (QuadTree.MAX_OBJECTS * selectivity) 
	public ArrayList<String> localityGuided(int k, int threshold, QuadTree outerQTree, QuadTree innerQTree, HashMap<Integer, ArrayList<Order>> orders) {
		numIO = 0;
		ArrayList<String> output = new ArrayList<String>();

		ArrayList<QuadTree> outerQueue = new ArrayList<QuadTree>();
		outerQueue.add(outerQTree);
		LocalityGuidedScan scan = new LocalityGuidedScan();
		
		while (!outerQueue.isEmpty()) {
			
			QuadTree outerNode = outerQueue.remove(0);
			if (outerNode.isLeaf) {
				numIO++;
				if (outerNode.numTuples == 0)
					continue;
				ArrayList<Tuple> qualifyingTuples = localityGuidedQualTuples(scan, k, threshold, outerNode, innerQTree, orders);				
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
	
	private ArrayList<Tuple> localityGuidedQualTuples(LocalityGuidedScan scan, int k, int threshold, QuadTree outerNode, QuadTree innerQTree, HashMap<Integer, ArrayList<Order>> orders) {
		
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
			
			if (!innerNode.processed) numIO+=innerNode.numTuples;
			ArrayList<Tuple> qualifyingFromInnerNode = Common.processPredicateQ13(threshold, innerNode, orders);
			qualifyingTuples.addAll(qualifyingFromInnerNode);
			count += qualifyingFromInnerNode.size();
			
			if (count >= k) {
				scan.setMinDistThreshold(highestMaxDist);
				break;
			}
		}

		while ((innerNode = scan.getNextBlock()) != null) {
			numIO++;
			if (!innerNode.processed) numIO+=innerNode.tuples.size();
			ArrayList<Tuple> qualifyingFromInnerNode = Common.processPredicateQ13(threshold, innerNode, orders);
			qualifyingTuples.addAll(qualifyingFromInnerNode);					
		}

		return qualifyingTuples;
	}

	// IO Cost = numInnerBlocks + numInnerBlocks*selectivity + numBlocksOuter * k / (QuadTree.MAX_OBJECTS)
	
	public ArrayList<String> kNNMAterialized(int k, int threshold, QuadTree outerQTree, QuadTree innerQTree, HashMap<Integer, ArrayList<Order>> orders) {

		numIO = 0;
		// Build a materialized view
		ArrayList<Tuple> qualifyingTuples = new ArrayList<Tuple>();
		ArrayList<QuadTree> innerQueue = new ArrayList<QuadTree>();
		innerQueue.add(innerQTree);
		while (!innerQueue.isEmpty()) {
			QuadTree innerNode = innerQueue.remove(0);

			if (innerNode.isLeaf) {
				numIO++;
				numIO+=innerNode.numTuples;
				for (Tuple innerTuple : innerNode.tuples)
					if (Common.relPredicateQ13(threshold, (Customer)innerTuple, orders))
						qualifyingTuples.add(innerTuple);
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
