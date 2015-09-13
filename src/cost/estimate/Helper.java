package cost.estimate;

import index.QuadTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import data.Constants;
import data.Tuple;
import exec.Common;
import optimizer.CatalogEntry;

public class Helper {

	public static ArrayList<CatalogEntry> mergeCatalogs(ArrayList<ArrayList<CatalogEntry>> catalogs) {

		// Merge Sort
		Comparator<MergeEntry> descComparer = new MergeEntryComparer();
		PriorityQueue<MergeEntry> pq = new PriorityQueue<MergeEntry>(50, descComparer);

		int maxCost = 0;
		for (ArrayList<CatalogEntry> catalog : catalogs) {
			if (catalog.get(0).numBlocks > maxCost)
				maxCost = catalog.get(0).numBlocks;
			
			if (catalog.size() == 1)
				continue;
			MergeEntry entry = new MergeEntry();
			entry.catalog = catalog;
			entry.positionInCatalog = 1;
			entry.startK = catalog.get(1).startK;
			pq.add(entry);
		}
		
		ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();
		int lastK = 1;
		while (!pq.isEmpty()) {
			MergeEntry entry = pq.remove();
			
			// New Entry.
			if (entry.catalog.get(entry.positionInCatalog).numBlocks > maxCost) {				
				CatalogEntry e = new CatalogEntry();
				e.startK = lastK;
				e.endK = entry.startK - 1;
				e.numBlocks = maxCost;
				catalog.add(e);
				lastK = entry.startK;
				
				maxCost = entry.catalog.get(entry.positionInCatalog).numBlocks;
			}
			
			if (entry.catalog.get(entry.positionInCatalog).endK >= Constants.maxK)
				continue;
			entry.positionInCatalog++;
			entry.startK = entry.catalog.get(entry.positionInCatalog).startK;
			pq.add(entry);
		}
		if (catalog.size() == 0) {
			CatalogEntry lastEntry = new CatalogEntry();
			lastEntry.startK = 1;
			lastEntry.endK = Constants.maxK;
			lastEntry.numBlocks = maxCost;
			catalog.add(lastEntry);
		}
		else if (catalog.get(catalog.size() - 1).endK < Constants.maxK) {
			CatalogEntry lastEntry = new CatalogEntry();
			lastEntry.startK = catalog.get(catalog.size() - 1).endK + 1;
			lastEntry.endK = Constants.maxK;
			lastEntry.numBlocks = maxCost;
			catalog.add(lastEntry);
		}

		return catalog;
	}
	
	public static ArrayList<CatalogEntry> aggregateMergeCatalogs(ArrayList<ArrayList<CatalogEntry>> catalogs) {

		// Merge Sort
		Comparator<MergeEntry> descComparer = new MergeEntryComparer();
		PriorityQueue<MergeEntry> pq = new PriorityQueue<MergeEntry>(50, descComparer);

		int currentCost = 0;
		for (ArrayList<CatalogEntry> catalog : catalogs) {
			currentCost += catalog.get(0).numBlocks;
			if (catalog.size() == 1)
				continue;
			MergeEntry entry = new MergeEntry();
			entry.catalog = catalog;
			entry.positionInCatalog = 1;
			entry.startK = catalog.get(1).startK;
			pq.add(entry);
		}
		
		ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();
		int lastK = 1;
		while (!pq.isEmpty()) {
			MergeEntry entry = pq.remove();
			
			// New Entry.
			if (lastK != entry.startK) {				
				CatalogEntry e = new CatalogEntry();
				e.startK = lastK;
				e.endK = entry.startK - 1;
				e.numBlocks = currentCost;
				catalog.add(e);
				lastK = entry.startK;
			}
			currentCost -= entry.catalog.get(entry.positionInCatalog - 1).numBlocks;
			currentCost += entry.catalog.get(entry.positionInCatalog).numBlocks;
			if (entry.catalog.get(entry.positionInCatalog).endK >= Constants.maxK)
				continue;
			entry.positionInCatalog++;
			entry.startK = entry.catalog.get(entry.positionInCatalog).startK;
			pq.add(entry);
		}
		
		if (catalog.get(catalog.size() - 1).endK < Constants.maxK) {
			CatalogEntry lastEntry = new CatalogEntry();
			lastEntry.startK = catalog.get(catalog.size() - 1).endK + 1;
			lastEntry.endK = Constants.maxK;
			lastEntry.numBlocks = currentCost;
			catalog.add(lastEntry);
		}

		return catalog;
	}

	public static QuadTree getNextBlock(QuadTree leafNode, PriorityQueue<QuadTree> queue) {
		QuadTree node = null;

		while (!queue.isEmpty()) {
			node = queue.remove();
			if (node.isLeaf) {
				if( node.numTuples > 0)
					return node;
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = Common.minDist(leafNode, child);
					child.mDistance = Common.maxDist(leafNode, child);
					queue.add(child);
				}
			}
		}
		return node;
	}
	
	public static CatalogEntry searchInCatalog(ArrayList<CatalogEntry> catalog, int k) {
		int start = 0;
		int end = catalog.size() - 1;

		while (true) {
			int mid = (start + end) / 2;
			if (k >= catalog.get(mid).startK && k <= catalog.get(mid).endK)
				return catalog.get(mid);
			else if (k < catalog.get(mid).startK)
				end = mid - 1;
			else
				start = mid + 1;
		}		
	}

	public static QuadTree getNextBlock(Tuple.Location focalPoint, PriorityQueue<QuadTree> queue) {
		QuadTree node = null;

		while (!queue.isEmpty()) {
			node = queue.remove();
			if (node.isLeaf) {
				if( node.numTuples > 0)
					return node;
			}
			else {
				for (QuadTree child : node.subTrees) {
					child.distance = Common.minDist(focalPoint, child);
					queue.add(child);
				}
			}
		}
		return node;
	}
	public static class MergeEntry{
		public ArrayList<CatalogEntry> catalog;
		public int positionInCatalog;
		public int startK;
	}

	public static class MergeEntryComparer implements Comparator<MergeEntry>{

		@Override
		public int compare(MergeEntry a, MergeEntry b) {

			if (a.startK > b.startK)
				return 1;
			if (a.startK < b.startK)
				return -1;

			return 0;
		}
	}

	public static void unitTest() {
		ArrayList<CatalogEntry> c1 = new ArrayList<CatalogEntry>();
		ArrayList<CatalogEntry> c2 = new ArrayList<CatalogEntry>();
		ArrayList<CatalogEntry> c3 = new ArrayList<CatalogEntry>();
		ArrayList<CatalogEntry> c4 = new ArrayList<CatalogEntry>();
		
		CatalogEntry e11 = new CatalogEntry(); CatalogEntry e12 = new CatalogEntry();
		e11.numBlocks = 2; e12.numBlocks = 10;
			e11.startK = 1; e11.endK = 10;
			e12.startK = 11; e12.endK = Constants.maxK;
		c1.add(e11); c1.add(e12);
		
		CatalogEntry e21 = new CatalogEntry(); CatalogEntry e22 = new CatalogEntry();
		e21.numBlocks = 5; e22.numBlocks = 13;
			e21.startK = 1; e21.endK = 3;
			e22.startK = 4; e22.endK = Constants.maxK;
		c2.add(e21); c2.add(e22);
		
		CatalogEntry e31 = new CatalogEntry(); CatalogEntry e32 = new CatalogEntry();
		e31.numBlocks = 6; e32.numBlocks = 9;
			e31.startK = 1; e31.endK = 7;
			e32.startK = 8; e32.endK = Constants.maxK;
		c3.add(e31); c3.add(e32);
		
		CatalogEntry e41 = new CatalogEntry(); CatalogEntry e42 = new CatalogEntry();
		e41.numBlocks = 4; e42.numBlocks = 8;
			e41.startK = 1; e41.endK = 5;
			e42.startK = 6; e42.endK = Constants.maxK;
		c4.add(e41); c4.add(e42);
		
		ArrayList<ArrayList<CatalogEntry>> allCatalogs = new ArrayList<ArrayList<CatalogEntry>>();
		allCatalogs.add(c1); allCatalogs.add(c2); allCatalogs.add(c3); allCatalogs.add(c4);
		
		ArrayList<CatalogEntry> merged = mergeCatalogs(allCatalogs);
		for (CatalogEntry e : merged) {
			System.out.println("Start k " + e.startK + ", end k " + e.endK + ", cost = " + e.numBlocks);
		}
	}
	
	public static void unitTestMax() {
		ArrayList<CatalogEntry> c1 = new ArrayList<CatalogEntry>();
		ArrayList<CatalogEntry> c2 = new ArrayList<CatalogEntry>();
		ArrayList<CatalogEntry> c3 = new ArrayList<CatalogEntry>();
		ArrayList<CatalogEntry> c4 = new ArrayList<CatalogEntry>();
		
		CatalogEntry e11 = new CatalogEntry(); CatalogEntry e12 = new CatalogEntry();
		e11.numBlocks = 2; e12.numBlocks = 10;
			e11.startK = 1; e11.endK = 10;
			e12.startK = 11; e12.endK = Constants.maxK;
		c1.add(e11); c1.add(e12);
		
		CatalogEntry e21 = new CatalogEntry(); CatalogEntry e22 = new CatalogEntry();
		e21.numBlocks = 5; e22.numBlocks = 13;
			e21.startK = 1; e21.endK = 3;
			e22.startK = 4; e22.endK = Constants.maxK;
		c2.add(e21); c2.add(e22);
		
		CatalogEntry e31 = new CatalogEntry(); CatalogEntry e32 = new CatalogEntry();
		e31.numBlocks = 6; e32.numBlocks = 19;
			e31.startK = 1; e31.endK = 7;
			e32.startK = 8; e32.endK = Constants.maxK;
		c3.add(e31); c3.add(e32);
		
		CatalogEntry e41 = new CatalogEntry(); CatalogEntry e42 = new CatalogEntry();
		e41.numBlocks = 4; e42.numBlocks = 8;
			e41.startK = 1; e41.endK = 5;
			e42.startK = 6; e42.endK = Constants.maxK;
		c4.add(e41); c4.add(e42);
		
		ArrayList<ArrayList<CatalogEntry>> allCatalogs = new ArrayList<ArrayList<CatalogEntry>>();
		allCatalogs.add(c1); allCatalogs.add(c2); allCatalogs.add(c3); allCatalogs.add(c4);
		
		ArrayList<CatalogEntry> merged = mergeCatalogs(allCatalogs);
		for (CatalogEntry e : merged) {
			System.out.println("Start k " + e.startK + ", end k " + e.endK + ", cost = " + e.numBlocks);
		}
	}
}
