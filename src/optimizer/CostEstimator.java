package optimizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

import data.Tuple;
import exec.Common;
import index.QuadTree;

public class CostEstimator {

	private static String estimatesPath = "//Users//ahmed//Documents//MyWork//data//TPC-H//kNNCostEstimates//estimates.txt";
	private static String catalogPrintPath = "//Users//ahmed//Desktop//kNNRelational Experimental Results//catalog.csv";

	public static final int MAXK = 1000000;	

	public static void estimateRandomLeafCost(QuadTree inputQTree) {

		QuadTree leaf = Common.getRandomLeaf(inputQTree);

		//estimateNaive(leaf, inputQTree);
		estimate(leaf, inputQTree);
		printCatalogInformation(leaf);

		System.out.println("--------");

		Tuple t = new Tuple();
		t.location.xCoord = leaf.bounds.x + leaf.bounds.width/2;
		t.location.yCoord = leaf.bounds.y + leaf.bounds.height/2;
		inputQTree.insert(t);

		estimate(leaf, inputQTree);

		System.out.println("------");
	}

	private static void readFromFile(QuadTree inputQTree) {

		try {
			FileReader fr = new FileReader(new File(estimatesPath));
			BufferedReader br = new BufferedReader(fr);

			ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
			queue.add(inputQTree);
			while (!queue.isEmpty()) {
				QuadTree node = queue.remove(0);
				if (node.isLeaf) {

					String line = br.readLine();
					ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();					
					while (line.contains(",")) {
						String[] parts = line.split(",");
						CatalogEntry cEntry = new CatalogEntry();
						cEntry.startK = Integer.parseInt(parts[0]);
						cEntry.endK = Integer.parseInt(parts[1]);
						cEntry.numBlocks = Integer.parseInt(parts[2]);
						cEntry.numPoints = Integer.parseInt(parts[3]);
						catalog.add(cEntry);
						line = br.readLine();
					}
					node.catalog = catalog;
					//					for (int i = 0; i < catalog.size(); i++) {
					//						estimate(node, inputQTree);
					//						System.out.println(catalog.get(i).startK + ", " + catalog.get(i).endK + ", "+ catalog.get(i).numBlocks + "," + catalog.get(i).numPoints);
					//						System.out.println(node.catalog.get(i).startK + ", " + node.catalog.get(i).endK + ", "+ node.catalog.get(i).numBlocks + "," + node.catalog.get(i).numPoints);
					//					}
				}
				else {
					for (QuadTree subTree : node.subTrees)
						queue.add(subTree);					
				}
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Estimates read from file " + estimatesPath);
	}

	public static void estimatekNNCost(QuadTree inputQTree) {

		File f = new File(estimatesPath);
		if(f.exists()) {
			readFromFile(inputQTree);
			return;
		}

		System.out.println("Starting the estimation process");


		try {
			FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);

			ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
			queue.add(inputQTree);

			while (!queue.isEmpty()) {
				QuadTree node = queue.remove(0);
				if (node.isLeaf) {
					estimate(node, inputQTree);
					System.out.println("Done for " + node);
					for (CatalogEntry cEntry : node.catalog) {
						bw.write(cEntry.startK + "," + cEntry.endK + "," + cEntry.numBlocks + "," + cEntry.numPoints + "\r\n");
					}
					bw.write("\r\n");
				}
				else {
					for (QuadTree subTree : node.subTrees)
						queue.add(subTree);					
				}
			}
			System.out.println("All estimates are calculated and dumped into " + estimatesPath);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void estimateNaive(QuadTree leafNode, QuadTree inputQTree) {

		leafNode.catalog = new ArrayList<CatalogEntry>();

		int lastSize = Common.getLocality(1, leafNode, inputQTree).size();
		int lastK = 1;

		for (int k = 1; k < inputQTree.numTuples; k++) {
			ArrayList<QuadTree> locality = Common.getLocality(k, leafNode, inputQTree);
			if (locality.size() != lastSize) {	
				int sum = 0;
				for (QuadTree q : locality) {
					sum += q.numTuples;
				}

				System.out.println("k = " + lastK + "  -> " + (k-1) + "            " + lastSize);

				lastSize = locality.size();
				lastK = k;
			}
		}
		System.out.println("k = " + lastK + "  -> " + inputQTree.numTuples + "            " + lastSize);
	}

	public static void estimate(QuadTree leafNode, QuadTree inputQTree) {

		leafNode.catalog = new ArrayList<CatalogEntry>();

		int k = 1;
		//while (k < inputQTree.numTuples) {
		while (k < MAXK) {

			//CatalogEntry  cEntry = getCatalogEntry(k, inputQTree.numTuples, leafNode, inputQTree);
			CatalogEntry  cEntry = getCatalogEntry(k, MAXK, leafNode, inputQTree);
			//System.out.println("k = " + cEntry.startK + "  -> " + cEntry.endK + "            " + cEntry.numBlocks);
			leafNode.catalog.add(cEntry);

			k = cEntry.endK + 1;			
		}		
	}

	private static CatalogEntry getCatalogEntry(int startK, int maxK, QuadTree leafNode, QuadTree inputQTree) {
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
		int sum = 0;
		for (QuadTree node : locality)
			sum += node.numTuples;

		c.numPoints = sum;

		return c;
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

	public static void printCatalogInformation(QuadTree node) {

		try {
			FileWriter fw = new FileWriter(new File(catalogPrintPath));
			BufferedWriter bw = new BufferedWriter(fw);
			for (CatalogEntry entry : node.catalog) {
				for (int i = entry.startK; i < entry.endK; i++) {
					bw.write(i + "," + entry.numBlocks);
					bw.write("\r\n");
					//				bw.write(entry.startK + ", " + entry.numBlocks);
					//				bw.write("\r\n");
					//				bw.write(entry.endK + ", " + entry.numBlocks);
					//				bw.write("\r\n");
				}
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Catalog information can be visualized at " + catalogPrintPath);
	}

	public static void printCatalogInformationCondensed(QuadTree node) {

		try {
			FileWriter fw = new FileWriter(new File(catalogPrintPath));
			BufferedWriter bw = new BufferedWriter(fw);
			for (CatalogEntry entry : node.catalog) {
				//for (int i = entry.startK; i < entry.endK; i++) {
				bw.write(entry.startK + ", " + entry.endK + " -> " +entry.numBlocks);
				bw.write("\r\n");
				//				bw.write(entry.startK + ", " + entry.numBlocks);
				//				bw.write("\r\n");
				//				bw.write(entry.endK + ", " + entry.numBlocks);
				//				bw.write("\r\n");
				//}
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Catalog information can be visualized at " + catalogPrintPath);
	}

	public static ArrayList<CatalogEntry> estimatekNNJoinCost(QuadTree outer, QuadTree inner) {
		return estimatekNNJoinCost(Common.getNumLeafs(outer), outer, inner);
	}

	public static ArrayList<CatalogEntry> estimatekNNJoinCost(int sampleSize, QuadTree outer, QuadTree inner) {

		// Merge Sort
		Comparator<MergeEntry> descComparer = new MergeEntryComparer();
		PriorityQueue<MergeEntry> pq = new PriorityQueue<MergeEntry>(50, descComparer);

		ArrayList<QuadTree> list = new ArrayList<QuadTree>();
		list.add(outer);
		

		int initialCost = 0; int processed = 0;
		//int i = 0;
		
		int step = (int)Math.ceil(Common.getNumLeafs(outer) / (double)sampleSize);
		Random r = new Random();
		
		int i = r.nextInt(step);
		
		while (!list.isEmpty()) {
			QuadTree node = list.remove(0);
			if (node.isLeaf) {
				if (i++%step == 0) {
					processed++;

					estimate(node, inner);
					//System.out.println("Done for " + node);

					initialCost+=node.catalog.get(0).numBlocks;
					if (node.catalog.size() == 1)
						continue;
					MergeEntry entry = new MergeEntry();
					entry.node = node;
					entry.positionInCatalog = 1;
					entry.startK = node.catalog.get(1).startK;
					pq.add(entry);
				}
				//else {
				//	initialCost+=Common.getLocality(1,node, inner).size();
				//}
			}
			else {
				list.add(node.subTrees[0]);
				list.add(node.subTrees[1]);
				list.add(node.subTrees[2]);
				list.add(node.subTrees[3]);
			}
		}
		while (processed != sampleSize) {
			processed++;
			QuadTree node = Common.getRandomLeaf(outer);
			estimate(node, inner);
			initialCost+=node.catalog.get(0).numBlocks;
			if (node.catalog.size() == 1)
				continue;
			MergeEntry entry = new MergeEntry();
			entry.node = node;
			entry.positionInCatalog = 1;
			entry.startK = node.catalog.get(1).startK;
			pq.add(entry);			
		}
		
		return merge(Common.getNumLeafs(outer) / (double)processed, initialCost, pq);

	}

	private static ArrayList<CatalogEntry> merge(double factor, int initialCost, PriorityQueue<MergeEntry> pq) {
		int totalCost = initialCost;

		//System.out.println("PQ Size " + pq.size());
		//System.out.println("k = 1,  Cost = " + totalCost);

		ArrayList<CatalogEntry> catalog = new ArrayList<CatalogEntry>();
		int lastK = 1;
		while (!pq.isEmpty()) {
			MergeEntry entry = pq.remove();
			totalCost -= entry.node.catalog.get(entry.positionInCatalog - 1).numBlocks;
			totalCost += entry.node.catalog.get(entry.positionInCatalog).numBlocks;
			if (lastK != entry.startK) {				
				CatalogEntry e = new CatalogEntry();
				e.startK = lastK;
				e.endK = entry.startK - 1;
				e.numBlocks = (int) (totalCost * factor);
				catalog.add(e);
				lastK = entry.startK;
			}
			// New Entry:
			if (entry.node.catalog.get(entry.positionInCatalog).endK == MAXK)
				continue;
			entry.positionInCatalog++;
			entry.startK = entry.node.catalog.get(entry.positionInCatalog).startK;
			pq.add(entry);
		}

		//for (CatalogEntry ce : catalog) {
		//System.out.println("k = " + ce.startK + " - " + ce.endK +  "          ->        Cost = " + ce.numBlocks);
		//}

		return catalog;
	}

	public static class MergeEntry{
		public QuadTree node;
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
}
