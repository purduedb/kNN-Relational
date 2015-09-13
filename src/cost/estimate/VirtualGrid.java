package cost.estimate;

import java.util.HashSet;
import optimizer.CatalogEntry;
import data.Constants;
import index.QuadTree;
import index.Rectangle;

public class VirtualGrid {

	private QuadTree[][] grid;
	private int numRows;
	private int numColumns;

	private double hStep;
	private double vStep;

	public VirtualGrid(int numRows, int numColumns, QuadTree innerQTree) {
		this.numRows = numRows;
		this.numColumns = numColumns;

		hStep = (Constants.maxLong - Constants.minLong) / numColumns;
		vStep = (Constants.maxLat - Constants.minLat) / numRows;

		grid = new QuadTree[numRows][numColumns];

		init(innerQTree);
		//precalculate(innerQTree);
	}
	
	public int getStorageOverhead() {
		int total = 0;
		for (int row = 0; row < numRows; row++) {
			for (int column = 0; column < numColumns; column++) {
				total += grid[row][column].localityCatalog.size();
			}
		}
		return total;
	}

	private void init(QuadTree innerQTree) {
		double xCoord = Constants.minLong;
		double yCoord = Constants.minLat;

		for (int row = 0; row < numRows; row++) {			
			xCoord = Constants.minLong;
			for (int column = 0; column < numColumns; column++) {
				//Rectangle r = new Rectangle(xCoord+hStep/2, yCoord+vStep/2, 0, 0);
				//Rectangle r = new Rectangle(xCoord, yCoord, 0, 0);
				Rectangle r = new Rectangle(xCoord, yCoord, hStep, vStep);
				//System.out.println(xCoord + ", " + yCoord + ", " + (xCoord + hStep) + ", " + (yCoord + vStep));
				grid[row][column] = new QuadTree(r);
				grid[row][column].localityCatalog = JoinEstimator.getLocalityCatalogOnePass(grid[row][column], innerQTree);
				
				xCoord += hStep;
			}
			yCoord += vStep;
		}
	}

	private void precalculate(QuadTree innerQTree) {
		//double xCoord = Constants.minLong;
		//double yCoord = Constants.minLat;

		for (int row = 0; row < numRows; row++) {
			//xCoord = Constants.minLong;
			for (int column = 0; column < numColumns; column++) {

				//Rectangle r = new Rectangle(xCoord, yCoord, hStep, vStep);
				//grid[row][column].bounds = r;

				//CostEstimator.estimate(grid[row][column], innerQTree);
				grid[row][column].localityCatalog = JoinEstimator.getLocalityCatalogOnePass(grid[row][column], innerQTree);

				//xCoord += hStep;
			}
			//yCoord += vStep;
		}
	}

	public double estimatekNNJoinCost(int k, QuadTree outerQTree) {
		//		Rectangle R = new Rectangle(Constants.minLong, Constants.minLat, Constants.maxLong-Constants.minLong, Constants.maxLat-Constants.minLat);
		//		int countt = countDistinct(new HashSet<QuadTree>(), R, outerQTree);
		//		System.out.println(countt);
		//				
		double total = 0;
		HashSet<QuadTree> hashSet = new HashSet<QuadTree>();
		for (int row = 0; row < numRows; row++) {
			for (int column = 0; column < numColumns; column++) {
				double countWithRatio = countWithRatio(hashSet, grid[row][column].bounds, outerQTree);

				if (countWithRatio == 0)
					continue;
				CatalogEntry catalogEntry = Helper.searchInCatalog(grid[row][column].localityCatalog, k);
				//total += count * catalogEntry.numBlocks;

				//double countDistinct = countDistinct(hashSet, grid[row][column].bounds, outerQTree);

				//total += countDistinct * catalogEntry.numBlocks / 4;
				total += countWithRatio * catalogEntry.numBlocks;
				
				//Try different diagonal sizes

			}
		}

		//System.out.println(hashSet.size());

		return total;
	}

	private double countWithRatio(HashSet<QuadTree>hashSet, Rectangle searchBounds, QuadTree outerQTree) {
		if (outerQTree.overlaps(searchBounds)) {
			if (outerQTree.isLeaf) {
				if(!hashSet.contains(outerQTree)) {					
					hashSet.add(outerQTree);
					//return 1;
					double nodeDiagonal = Math.sqrt(Math.pow(outerQTree.bounds.width, 2) + Math.pow(outerQTree.bounds.height, 2));
					//double nodeDiagonal = outerQTree.bounds.width * outerQTree.bounds.height;

					double searchDiagonal = Math.sqrt(Math.pow(searchBounds.width, 2) + Math.pow(searchBounds.height, 2));
					//double searchDiagonal = searchBounds.width + searchBounds.height;
					//System.out.println(nodeDiagonal/searchDiagonal);					

					return nodeDiagonal/searchDiagonal;
					//	return outerQTree.numTuples;
				}
				//				int count = 0;
				//				for (Tuple t : outerQTree.tuples) {
				//					if (t.location.xCoord >= searchBounds.x && t.location.xCoord <= (searchBounds.x + searchBounds.width)
				//							&& t.location.yCoord >= searchBounds.y && t.location.yCoord <= (searchBounds.y + searchBounds.height))
				//						count++;
				//				}
				//				return count;
			}
			else {
				return countWithRatio(hashSet, searchBounds, outerQTree.subTrees[0])
						+ countWithRatio(hashSet, searchBounds, outerQTree.subTrees[1])
						+ countWithRatio(hashSet, searchBounds, outerQTree.subTrees[2])
						+ countWithRatio(hashSet, searchBounds, outerQTree.subTrees[3]);
			}
		}
		return 0;
	}

	private int countDistinct(HashSet<QuadTree>hashSet, Rectangle searchBounds, QuadTree outerQTree) {
		if (outerQTree.overlaps(searchBounds)) {
			if (outerQTree.isLeaf) {
				if(!hashSet.contains(outerQTree)) {					
					hashSet.add(outerQTree);
					return 1;
				}
				else {
					return countDistinct(hashSet, searchBounds, outerQTree.subTrees[0])
							+ countDistinct(hashSet, searchBounds, outerQTree.subTrees[1])
							+ countDistinct(hashSet, searchBounds, outerQTree.subTrees[2])
							+ countDistinct(hashSet, searchBounds, outerQTree.subTrees[3]);
				}
			}
		}
		return 0;
	}
	
}