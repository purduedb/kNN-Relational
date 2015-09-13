package exec;

import java.util.Comparator;
import java.util.PriorityQueue;

import index.QuadTree;
import data.Tuple;
import exec.Common.QTreeNodeAscComparer;

public class LocalityGuidedScan {

	private Tuple.Location focalPoint;
	private QuadTree focalBlock;
	
	private double minDistThreshold = Double.MAX_VALUE;
	private boolean thresholdExceeded;

	private Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
	private PriorityQueue<QuadTree> minDistQueue = new PriorityQueue<QuadTree>(50, comparer);

	public LocalityGuidedScan(QuadTree qTree, Tuple.Location focalPoint) {

		this.focalPoint = focalPoint;

		qTree.distance = Common.minDist(focalPoint, qTree);
		minDistQueue.add(qTree);
	}

	public LocalityGuidedScan() {
		
	}
	
	// used only in kNN join to switch between blocks.
	public void reset(QuadTree qTree, QuadTree outerNode) {
		minDistQueue = new PriorityQueue<QuadTree>(50, comparer);
		
		this.focalBlock = outerNode;
		
		
		Tuple t = new Tuple();
		t.location.xCoord = outerNode.bounds.x + outerNode.bounds.width / 2;
		t.location.yCoord = outerNode.bounds.y + outerNode.bounds.height / 2;
		this.focalPoint = t.location;
		qTree.distance = Common.minDist(this.focalPoint, qTree);
		
		
		//qTree.distance = Common.minDist(outerNode, qTree);
		
		minDistQueue.add(qTree);
		thresholdExceeded = false;
		minDistThreshold = Double.MAX_VALUE;
	}

	public QuadTree getNextBlock() {
		if (minDistQueue.isEmpty() || thresholdExceeded)
			return null;

		QuadTree node = minDistQueue.remove();
		if (node.distance > minDistThreshold) {
			thresholdExceeded = true;
			return null;
		}

		while (!node.isLeaf) {
			for (QuadTree subTree : node.subTrees) {
				subTree.distance = getMinDist(subTree);
				minDistQueue.add(subTree);
			}
			node = minDistQueue.remove();			
		}
		return node;
	}
	
	private double getMinDist(QuadTree node) {
		if (focalPoint != null)
			return Common.minDist(focalPoint, node);
		else
			return Common.minDist(focalBlock, node);
	}	
	
	public void setMinDistThreshold(double threshold) {
		this.minDistThreshold = threshold;
	}

}
