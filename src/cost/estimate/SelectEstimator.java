package cost.estimate;

import index.QuadTree;
import index.Rectangle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import output.kNNToken;

import data.Constants;
import data.Tuple;

import optimizer.CatalogEntry;
import exec.Common;
import exec.Common.*;

public class SelectEstimator {

	public static double estimateWithCorners(Tuple.Location queryPoint, QuadTree quadTree, int k) {
		QuadTree leafNode = quadTree.searchEnclosingLeaf(queryPoint);
		Tuple.Location center = Common.getCenter(leafNode);
		
		double distance = Math.sqrt(Math.pow(center.xCoord - queryPoint.xCoord, 2) + Math.pow(center.yCoord - queryPoint.yCoord, 2));
		double diag = Math.sqrt(Math.pow(leafNode.bounds.width, 2) + Math.pow(leafNode.bounds.height, 2)) / 2;
		double ratio = distance/diag;
		
		int centerCost = Helper.searchInCatalog(leafNode.centerCatalog, k).numBlocks;
		int cornerCost = Helper.searchInCatalog(leafNode.cornerCatalog, k).numBlocks;
		
		int diff = Math.abs(cornerCost - centerCost);
		
		return (int)(centerCost + diff * ratio);
	}
	
	public static double estimateWithoutCorners(Tuple.Location queryPoint, QuadTree quadTree, int k) {
		QuadTree leafNode = quadTree.searchEnclosingLeaf(queryPoint);		
		return Helper.searchInCatalog(leafNode.centerCatalog, k).numBlocks;		
	}

	// Tao et. al
	public static int estimateDensityBased(Tuple.Location queryPoint, QuadTree quadTree, int k) {

		QuadTree leafNode = quadTree.searchEnclosingLeaf(queryPoint);

		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> scan = new PriorityQueue<QuadTree>(50, comparer);

		double radius = 0;

		// MINDIST Scanning
		quadTree.distance = Common.minDist(leafNode, quadTree);
		quadTree.mDistance = Common.maxDist(leafNode, quadTree);
		scan.add(quadTree);

		int numTuples = 0;
		// bounds
		double left, right, top, bottom;
		left = leafNode.bounds.x; bottom = leafNode.bounds.y;
		right = leafNode.bounds.x + leafNode.bounds.width; top = leafNode.bounds.y + leafNode.bounds.height;

		// pi r^2 * density = k
		// r^2 = k/(pi * density)


		boolean loop = true;
		while (loop) {

			QuadTree node = Helper.getNextBlock(leafNode, scan);
			if (node == null)
				break;

			if (node.bounds.x < left)
				left = node.bounds.x;
			if (node.bounds.y < bottom)
				bottom = node.bounds.y;
			if (node.bounds.x + node.bounds.width > right)
				right = node.bounds.x + node.bounds.width;
			if (node.bounds.y + node.bounds.height > top)
				top = node.bounds.y + node.bounds.height;

			numTuples += node.numTuples;

			double density = (double)numTuples / ((top-bottom) * (right-left));
			radius = Math.sqrt(k / (Math.PI * density));

			if (right - queryPoint.xCoord < radius)
				loop = true;
			else if (queryPoint.xCoord - left < radius)
				loop = true;
			else if (top - queryPoint.yCoord < radius)
				loop = true;
			else if (queryPoint.yCoord - bottom < radius)
				loop = true;
			else
				loop = false;
		}

		int cost = 0;
		scan = new PriorityQueue<QuadTree>(50, comparer);
		quadTree.distance = Common.minDist(leafNode, quadTree);
		quadTree.mDistance = Common.maxDist(leafNode, quadTree);
		scan.add(quadTree);
		while (true) {
			QuadTree node = Helper.getNextBlock(leafNode, scan);
			if (node == null)
				break;
			if (node.distance > radius)
				break;
			cost++;
		}

		return cost;

	}

	// Preprocess the center catalog of all blocks in the quadtree
	public static void preprocessCenter(QuadTree quadTree) {
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(quadTree);
		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				node.centerCatalog = getPivotCatalogOnePass(Common.getCenter(node), quadTree);
			}
			else {
				for (QuadTree child : node.subTrees) {
					queue.add(child);
				}
			}
		}
	}
	
	// Preprocess the corners catalog of all blocks in the quadtree
	public static void preprocessCorners(QuadTree quadTree) {
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(quadTree);
		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				node.cornerCatalog = getCornersCatalog(node, quadTree);
			}
			else {
				for (QuadTree child : node.subTrees) {
					queue.add(child);
				}
			}
		}
	}
	
	public static long computeStorage(boolean withCorner, QuadTree quadTree) {
		long total = 0;
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(quadTree);
		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				total += node.centerCatalog.size();
				if (withCorner)
					total += node.cornerCatalog.size();
			}
			else {
				for (QuadTree child : node.subTrees) {
					queue.add(child);
				}
			}
		}
		return total;
	}

	public static int getActualCost(Tuple.Location queryPoint, QuadTree quadTree, int k) {
		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> blocksQueue = new PriorityQueue<QuadTree>(50, comparer);

		Comparator<Tuple> ascComparer = new Common.DataAscComparer();
		PriorityQueue<Tuple> tuplesQueue = new PriorityQueue<Tuple>(50, ascComparer);

		quadTree.distance = Common.minDist(queryPoint, quadTree);
		blocksQueue.add(quadTree);

		int cost = 0;
		int nnOutputed = 0;

		while (nnOutputed < k) {
			QuadTree node = Helper.getNextBlock(queryPoint, blocksQueue);
			cost++;
			for (Tuple t : node.tuples) {
				t.setDistance(queryPoint);
				tuplesQueue.add(t);
			}
			
			while (!tuplesQueue.isEmpty() && tuplesQueue.peek().distance < blocksQueue.peek().distance) {
				nnOutputed++;
				tuplesQueue.remove();
			}
		}
		return cost;
	}

	public static ArrayList<CatalogEntry> getPivotCatalogOnePass(Tuple.Location pivot, QuadTree inputQTree) {
		ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();
		
		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> blocksQueue = new PriorityQueue<QuadTree>(50, comparer);

		Comparator<Tuple> ascComparer = new Common.DataAscComparer();
		PriorityQueue<Tuple> tuplesQueue = new PriorityQueue<Tuple>(50, ascComparer);

		inputQTree.distance = Common.minDist(pivot, inputQTree);
		blocksQueue.add(inputQTree);

		int cost = 0;
		int currentK = 0;

		while (currentK < Constants.maxK) {
			QuadTree node = Helper.getNextBlock(pivot, blocksQueue);
			cost++;
			for (Tuple t : node.tuples) {
				t.setDistance(pivot);
				tuplesQueue.add(t);
			}
			int startK = currentK;
			while (!tuplesQueue.isEmpty() && tuplesQueue.peek().distance < blocksQueue.peek().distance) {
				currentK++;
				if (currentK >= Constants.maxK)
					break;
				tuplesQueue.remove();
			}
			if (startK == currentK)
				continue;
			CatalogEntry entry = new CatalogEntry();
			entry.startK = startK + 1; entry.endK = currentK;
			entry.numBlocks = cost;
			catalog.add(entry);
		}
		//System.out.println("Done " + catalog.size());
		return catalog;		
	}
	
	public static ArrayList<CatalogEntry> getCornersCatalog(QuadTree node, QuadTree inputQTree) {
		Tuple topLeft = new Tuple();
		topLeft.location.xCoord = node.bounds.x;
		topLeft.location.yCoord = node.bounds.y + node.bounds.height;
		
		Tuple topRight = new Tuple();
		topRight.location.xCoord = node.bounds.x + node.bounds.width;
		topRight.location.yCoord = node.bounds.y + node.bounds.height;
		
		Tuple bottomLeft = new Tuple();
		bottomLeft.location.xCoord = node.bounds.x;
		bottomLeft.location.yCoord = node.bounds.y;
		
		Tuple bottomRight = new Tuple();
		bottomRight.location.xCoord = node.bounds.x + node.bounds.width;
		bottomRight.location.yCoord = node.bounds.y;
		
		ArrayList<CatalogEntry> topLeftCatalog = getPivotCatalogOnePass(topLeft.location, inputQTree);
		ArrayList<CatalogEntry> topRightCatalog = getPivotCatalogOnePass(topRight.location, inputQTree);
		ArrayList<CatalogEntry> bottomLeftCatalog = getPivotCatalogOnePass(bottomLeft.location, inputQTree);
		ArrayList<CatalogEntry> bottomRightCatalog = getPivotCatalogOnePass(bottomRight.location, inputQTree);
		
		ArrayList<ArrayList<CatalogEntry>> allCatalogs = new ArrayList<ArrayList<CatalogEntry>>();
		allCatalogs.add(topLeftCatalog); allCatalogs.add(topRightCatalog); 
		allCatalogs.add(bottomLeftCatalog); allCatalogs.add(bottomRightCatalog);
		
		return Helper.mergeCatalogs(allCatalogs);
	}

	public static ArrayList<CatalogEntry> getPivotCatalogNaive(Tuple.Location pivot, QuadTree inputQTree) {
		ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();

		int lastCost = getActualCost(pivot, inputQTree, 1);
		int lastK = 1;

		for (int k = 2; k < Constants.maxK; k++) {
			int currentCost = getActualCost(pivot, inputQTree, k);
			if (currentCost != lastCost) {
			//if (Math.abs(currentCost - lastCost) > 0) {
				CatalogEntry entry = new CatalogEntry();
				entry.startK = lastK;
				entry.endK = k - 1;
				entry.numBlocks = lastCost;
				catalog.add(entry);

				lastCost = currentCost;
				lastK = k;
			}
		}
		CatalogEntry entry = new CatalogEntry();
		entry.startK = lastK;
		entry.endK = Constants.maxK;
		entry.numBlocks = lastCost;
		catalog.add(entry);

		return catalog;
	}
}