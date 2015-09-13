package cost.estimate;

import index.QuadTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import optimizer.CatalogEntry;
import data.Constants;

import exec.Common;
import exec.Common.QTreeNodeAscComparer;

public class JoinEstimator {

	public static ArrayList<CatalogEntry> getMergedCatalog(QuadTree outer, QuadTree inner) {
		ArrayList<ArrayList<CatalogEntry>> allCatalogs = new ArrayList<ArrayList<CatalogEntry>>();
		
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outer);
		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				node.localityCatalog = getLocalityCatalogOnePass(node, inner);
				allCatalogs.add(node.localityCatalog);
			}
			else {
				for (QuadTree child : node.subTrees) {
					queue.add(child);
				}
			}
		}
		
		return Helper.aggregateMergeCatalogs(allCatalogs);
	}
	
	public static ArrayList<CatalogEntry> getMergedCatalog(int sampleSize, QuadTree outer, QuadTree inner) {
		ArrayList<ArrayList<CatalogEntry>> allCatalogs = new ArrayList<ArrayList<CatalogEntry>>();
		int numLeafsInOuter = Common.getNumLeafs(outer);
		int i = 0;
		int step = (int)Math.ceil(numLeafsInOuter / (double)sampleSize);
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outer);
		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				if (i++%step == 0) {
					node.localityCatalog = getLocalityCatalogOnePass(node, inner);
					allCatalogs.add(node.localityCatalog);
				}
			}
			else {
				for (QuadTree child : node.subTrees) {
					queue.add(child);
				}
			}
		}
		ArrayList<CatalogEntry> mergedCatalog = Helper.aggregateMergeCatalogs(allCatalogs);
		for (CatalogEntry entry : mergedCatalog) {
			// Scale each value
			entry.numBlocks = (int)(((double)entry.numBlocks * numLeafsInOuter) / allCatalogs.size());
		}
		return mergedCatalog;
	}
	
	public static int estimateCatalogMerge(int k, ArrayList<CatalogEntry> mergeCatalog) {
		CatalogEntry cEntry = Helper.searchInCatalog(mergeCatalog, k);
		return cEntry.numBlocks;
	}
	
	public static ArrayList<CatalogEntry> getLocalityCatalogOnePass(QuadTree leafNode, QuadTree inputQTree) {
		ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();

		Comparator<QuadTree> comparer = new QTreeNodeAscComparer();
		PriorityQueue<QuadTree> countScan = new PriorityQueue<QuadTree>(50, comparer);

		// MINDIST Scanning
		inputQTree.distance = Common.minDist(leafNode, inputQTree);
		inputQTree.mDistance = Common.maxDist(leafNode, inputQTree);
		countScan.add(inputQTree);

		int startK, endK, aggCost = 0;
		int currentCount = 0;
		double highestMaxDist = 0;
		while (currentCount < Constants.maxK) {
			// Get the first leaf 
			startK = currentCount + 1;
			QuadTree firstNode = Helper.getNextBlock(leafNode, countScan);
			if (firstNode == null)
				return catalog;
			currentCount += firstNode.numTuples;
			highestMaxDist = firstNode.mDistance;
			aggCost++;

			// Loop until highestMaxDist changes
			while (true) {
				QuadTree next = Helper.getNextBlock(leafNode, countScan);
				if (next == null)
					return catalog;
				if (next.mDistance > highestMaxDist) {
					countScan.add(next);
					break;
				}
				currentCount += next.numTuples;
				aggCost++;
			}

			endK = currentCount;

			// Loop till mindist > highestMaxDist
			ArrayList<QuadTree> encountered = new ArrayList<QuadTree>();
			while (true) {
				QuadTree next = Helper.getNextBlock(leafNode, countScan);
				if (next == null)
					return catalog;
				encountered.add(next);
				if (next.distance > highestMaxDist) {
					CatalogEntry entry = new CatalogEntry();
					entry.startK = startK; entry.endK = endK;
					entry.numBlocks = aggCost + encountered.size() - 1;
					//System.out.println("StartK = " + entry.startK + ", EndK = " + entry.endK + ", Cost = " + entry.numBlocks);
					catalog.add(entry);
					break;
				}
			}

			// put back
			for (QuadTree enc : encountered)
				countScan.add(enc);
		}
		return catalog;
	}

	private static ArrayList<CatalogEntry> getLocalityCatalogBSearch(QuadTree leafNode, QuadTree inputQTree) {
		ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();
		int k = 1;
		while (k < Constants.maxK) {
			CatalogEntry  cEntry = getCatalogEntryBSearch(k, Constants.maxK, leafNode, inputQTree);
			catalog.add(cEntry);

			k = cEntry.endK + 1;			
		}		
		return catalog;
	}

	private static ArrayList<CatalogEntry> getLocalityCatalogNaive(QuadTree leafNode, QuadTree inputQTree) {

		ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();

		int lastSize = Common.getLocality(1, leafNode, inputQTree).size();
		int lastK = 1;

		for (int k = 1; k < Constants.maxK; k++) {
			ArrayList<QuadTree> locality = Common.getLocality(k, leafNode, inputQTree);
			if (locality.size() != lastSize) {	
				CatalogEntry entry = new CatalogEntry();
				entry.startK = lastK;
				entry.endK = k - 1;
				entry.numBlocks = lastSize;
				catalog.add(entry);

				lastSize = locality.size();
				lastK = k;
			}
		}
		CatalogEntry entry = new CatalogEntry();
		entry.startK = lastK;
		entry.endK = Constants.maxK;
		entry.numBlocks = lastSize;
		catalog.add(entry);

		return catalog;
	}

	private static CatalogEntry getCatalogEntryBSearch(int startK, int maxK, QuadTree leafNode, QuadTree inputQTree) {
		CatalogEntry c = new CatalogEntry();
		c.startK = startK;
		int midK;
		int endK = maxK;

		while (true) {
			int startSize = Common.getLocality(startK, leafNode, inputQTree).size();
			midK = (startK + endK)/2;
			if (midK == startK)
				break;
			int midSize = Common.getLocality(midK, leafNode, inputQTree).size();
			if (startSize == midSize) {
				startK = midK;
			}
			else {
				endK = midK;
			}
		}
		c.endK= midK;
		if (endK == maxK)
			c.endK= maxK;

		ArrayList<QuadTree> locality = Common.getLocality(midK, leafNode, inputQTree); 
		c.numBlocks = locality.size();

		return c;
	}
	
	public static int estimateBySampling(int k, int sampleSize, QuadTree outer, QuadTree inner) {
		double total = 0;

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outer);

		int i = 0;
		int calculatedFor = 0;
		int step = (int)Math.ceil(Common.getNumLeafs(outer) / (double)sampleSize);
		//System.out.println("Step = " + step);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);

			if (node.isLeaf) {
				if (i++%step == 0) {
					calculatedFor++;
					total+=Common.getLocality(k, node, inner).size();
				}
			}
			else {
				for (QuadTree child : node.subTrees)
					queue.add(child);				
			}
		}

		double localitySize = total/calculatedFor;

		return (int)(localitySize * Common.getNumLeafs(outer));				
	}

	public static int actualCost(int k, QuadTree outer, QuadTree inner) {
		int total = 0;

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outer);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				total+=Common.getLocality(k, node, inner).size();
			}
			else {
				for (QuadTree child : node.subTrees)
					queue.add(child);				
			}
		}

		return total;
	}
}
