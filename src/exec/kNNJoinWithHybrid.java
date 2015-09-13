package exec;

import index.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedMap;

import output.kNNToken;

import data.Supplier;
import data.Tuple;

public class kNNJoinWithHybrid {

	// Predicate Level (Makes sense here only)

	public ArrayList<kNNToken> hybridJoin(int k, ArrayList<Tuple> outerTable, QuadTree innerQTree, BPTree<Double, ArrayList<Tuple>> innerBPTree) {
		ArrayList<kNNToken> output = new ArrayList<kNNToken>();
		Comparator<Tuple> descComparer = new Common.DataDescComparer();
		kNNSelectWithRelational localityguidedkNN = new kNNSelectWithRelational();
		
		for (Tuple outerTuple : outerTable) {
			double selectivity = getSelectivity(((Supplier)outerTuple), innerBPTree);
			double numTuplesPerBlock = QuadTree.maxNumObjects * selectivity;
			double numLocalityBlocks = k / numTuplesPerBlock;

			double numRelQualifyingPoints = getSelectivity(((Supplier)outerTuple), innerBPTree) * innerBPTree.size();
			//double numRelQualifyingPoints = innerBPTree.size()*(((Supplier)outerTuple).acctBalance - innerBPTree.firstKey())/(innerBPTree.lastKey() - innerBPTree.firstKey());
			double numRelQualifyingBlocks = numRelQualifyingPoints / QuadTree.MAX_OBJECTS;			
			
			if (numRelQualifyingBlocks > numLocalityBlocks) {			
				// do locality guided scanning
				output.add(localityguidedkNN.kNNSelectLocalityGuided(k, ((Supplier)outerTuple), innerQTree));			
			}
			else {
				//System.out.println(numRelQualifyingBlocks + "  ||  " + numLocalityBlocks);
				// do rel first
				SortedMap<Double, ArrayList<Tuple>> sm = innerBPTree.subMap(innerBPTree.firstKey(), ((Supplier)outerTuple).acctBalance);
				kNNToken outputToken = new kNNToken(50, descComparer, k, outerTuple.location);
				outputToken.numIO = 1+ (int) numRelQualifyingBlocks;
				for (ArrayList<Tuple> arr : sm.values())
					for (Tuple tuple : arr)
						outputToken.insert(tuple);
				output.add(outputToken);
			}
		}
		
		return output;
	}

	public ArrayList<kNNToken> localityOnlyJoin(int k, ArrayList<Tuple> outerTable, QuadTree innerQTree) {
		ArrayList<kNNToken> output = new ArrayList<kNNToken>();

		kNNSelectWithRelational localityguidedkNN = new kNNSelectWithRelational();

		for (Tuple outerTuple : outerTable) {
			kNNToken token = localityguidedkNN.kNNSelectLocalityGuided(k, ((Supplier)outerTuple), innerQTree);
			output.add(token);
		}

		return output;
	}

	public ArrayList<kNNToken> relationalOnlyJoin(int k, ArrayList<Tuple> outerTable, BPTree<Double, ArrayList<Tuple>> innerBPTree) {
		ArrayList<kNNToken> output = new ArrayList<kNNToken>();
		Comparator<Tuple> descComparer = new Common.DataDescComparer();

		int i = 0;
		for (Tuple outerTuple : outerTable) {

			if (i!= 0 && i % 100 == 0)
				break;
			
			i++;

			double numRelQualifyingPoints = getSelectivity(((Supplier)outerTuple), innerBPTree) * innerBPTree.size();
			double numRelQualifyingBlocks = numRelQualifyingPoints/QuadTree.MAX_OBJECTS;			
			kNNToken outputToken = new kNNToken(50, descComparer, k, outerTuple.location);
			outputToken.numIO = 1+ (int) numRelQualifyingBlocks;

			SortedMap<Double, ArrayList<Tuple>> sm = innerBPTree.subMap(innerBPTree.firstKey(), ((Supplier)outerTuple).acctBalance);			
			for (ArrayList<Tuple> arr : sm.values())
				for (Tuple tuple : arr)
					outputToken.insert(tuple);

			output.add(outputToken);

		}

		return output;
	}

	private double getSelectivity(Supplier outerTuple, BPTree<Double, ArrayList<Tuple>> innerBPTree){

		//return (outerTuple.acctBalance + 999.99)/(9999.99 + 999.99);
		return (outerTuple.acctBalance + 999.99)/10999.98;
	}

}
