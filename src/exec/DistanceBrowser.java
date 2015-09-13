package exec;

import index.QuadTree;

import java.util.Comparator;
import java.util.PriorityQueue;

import data.Tuple;

public class DistanceBrowser {

	private PriorityQueue<Tuple> tupleQueue;	
	private PriorityQueue<QuadTree> blockQueue;
	private Tuple.Location qLocation;

	public DistanceBrowser(Tuple.Location qLocation, QuadTree qTree) {
		this.qLocation = qLocation;

		Comparator<Tuple> descComparer = new Common.DataAscComparer();		
		tupleQueue = new PriorityQueue<Tuple>(50, descComparer);

		Comparator<QuadTree> ascComparer = new Common.QTreeNodeAscComparer();
		blockQueue = new PriorityQueue<QuadTree>(50, ascComparer);

		qTree.distance = Common.minDist(qLocation, qTree);
		blockQueue.add(qTree);	
	}
	
	public int numExploredLeaves = 0;

	public Tuple getNext() {

		while (tupleQueue.isEmpty())
		{
			if (blockQueue.isEmpty())
				return null;

			exploreMoreBlocks();			
		}

		while (!blockQueue.isEmpty() && tupleQueue.peek().distance >= blockQueue.peek().distance)
			exploreMoreBlocks();

		return tupleQueue.remove();
	}

	private void exploreMoreBlocks() {
		if (blockQueue.isEmpty())
			return;

		QuadTree block = blockQueue.remove();
		while (!block.isLeaf) {
			for (QuadTree subTree : block.subTrees) {
				subTree.distance = Common.minDist(qLocation, subTree);
				blockQueue.add(subTree);
			}
			block = blockQueue.remove();
		}
		numExploredLeaves++;
		for (Tuple tuple : block.tuples) {
			tuple.setDistance(qLocation);
			tupleQueue.add(tuple);
		}
	}


}
