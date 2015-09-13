package optimizer;

import index.QuadTree;
import index.Rectangle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import data.Constants;
import data.Tuple;
import exec.Common;

public class TestCost {

	private static String printPath = "//Users//ahmed//Desktop//kNNRelational Experimental Results//joinCostEstimate.csv";


	private static double scale = 1;
	private static ArrayList<Tuple> innerPoints;
	private static ArrayList<Tuple> outerPoints;

	private static int k = 100;

	public static void main(String[] args) {

		readData();

		Rectangle R = new Rectangle(Constants.minLong, Constants.minLat, Constants.maxLong-Constants.minLong, Constants.maxLat-Constants.minLat);
		//QuadTree tree = new QuadTree(R, 10);
		QuadTree outerTree = new QuadTree(R, 1000, 0);
		outerTree.insert(outerPoints);

		QuadTree innerTree = new QuadTree(R, 1000, 0);
		innerTree.insert(innerPoints);
		
		CostEstimator.estimateRandomLeafCost(innerTree);
		//traverse(innerTree);

		System.out.println("Quad trees created");

		System.out.println("Number of leafs in outer " + getNumLeafs(outerTree));
		System.out.println("Number of leafs in inner " + getNumLeafs(innerTree));

		int exact = getExactCost(outerTree, innerTree);
		System.out.println("Exact Cost = " + exact);

		//int estimate1 = getEstimateCost1(30, outerTree, innerTree);
		//System.out.println("Estimate Cost 1 = " + estimate1);

		//testSampling(outerTree, innerTree, exact);
		
		//testVirtualGrid(outerTree, innerTree, exact);
		
		CostEstimator.estimatekNNJoinCost(outerTree, innerTree);
	}
	
	private static void traverse(QuadTree tree) {
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(tree);
		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				//System.out.println(node.tuples.size() + "       " + node.numTuples);
				if (node.numTuples > 1000) {
					System.out.println(node.bounds.x + node.bounds.width / 2);
					System.out.println(node.bounds.y + node.bounds.height / 2);
				}
			}
			else {
				queue.add(node.subTrees[0]);
				queue.add(node.subTrees[1]);
				queue.add(node.subTrees[2]);
				queue.add(node.subTrees[3]);
			}
		}
		
	}

	private static void testVirtualGrid(QuadTree outerTree, QuadTree innerTree, int exact) {
		System.out.println("Building Virtual Grid");
		VirtualGrid vg = new VirtualGrid(20, 10, innerTree);
		System.out.println("Virtual Grid built");
		
		double estimate = vg.estimatekNNJoinCost(k, outerTree);
		System.out.println("Exact = " + exact);
		
		System.out.println("Estimate Cost By Virtual Grid = " + estimate);
		System.out.println("Accuracy = " + (1 - Math.abs(exact - estimate)/(double)exact)*100 + "%");
	}

	private static void testSampling(QuadTree outerTree, QuadTree innerTree, int exact) {
		FileWriter fw;
		try {
			fw = new FileWriter(new File(printPath));

			BufferedWriter bw = new BufferedWriter(fw);

			for (int sampleSize = 1; sampleSize < 100; sampleSize+=2) {
				int estimate2 = getEstimateCost2(sampleSize, outerTree, innerTree);
				System.out.println("Estimate Cost 2 = " + estimate2);
				System.out.println("sample Size = " + sampleSize);
				System.out.println("Accuracy = " + (1 - Math.abs(exact - estimate2)/(double)exact)*100 + "%");
				bw.write(sampleSize + "," + (1 - Math.abs(exact - estimate2)/(double)exact) + "\r\n");
				bw.flush();
			}

			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static int getEstimateCost1(int sampleSize, QuadTree outer, QuadTree inner) {
		double total = 0;

		for (int i = 0; i < sampleSize; i++) {
			QuadTree node = getRandomLeaf(outer);
			total+=Common.getLocality(k, node, inner).size();
		}
		double localitySize = total/sampleSize;

		System.out.println("Average Locality Size = " + localitySize);

		System.out.println("Number of outer Leafs = " + getNumLeafs(outer));

		return (int)(localitySize * getNumLeafs(outer));				
	}

	private static int getEstimateCost2(int sampleSize, QuadTree outer, QuadTree inner) {
		double total = 0;

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outer);

		int i = 0;
		int calculatedFor = 0;
		int step = (int)Math.ceil(getNumLeafs(outer) / (double)sampleSize);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);

			if (node.isLeaf) {
				if (i++%step == 0) {
					calculatedFor++;
					total+=Common.getLocality(k, node, inner).size();
				}
			}
			else {
				queue.add(node.subTrees[0]);
				queue.add(node.subTrees[1]);
				queue.add(node.subTrees[2]);
				queue.add(node.subTrees[3]);
			}
		}


		double localitySize = total/calculatedFor;

		return (int)(localitySize * getNumLeafs(outer));				
	}

	private static QuadTree getRandomLeaf(QuadTree tree) {
		QuadTree node = tree;
		Random r = new Random();
		while (!node.isLeaf) {			
			node = node.subTrees[r.nextInt(4)];
		}

		return node;
	}

	private static int getExactCost(QuadTree outer, QuadTree inner) {
		int total = 0;

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outer);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);

			if (node.isLeaf) {
				total+=Common.getLocality(k, node, inner).size();
				//total+=node.numTuples * Common.getLocality(k, node, inner).size();
//				for (Tuple t : node.tuples) {
//					total+=Common.getLocality(k, t.location, inner).size();
//				}
			}
			else {
				queue.add(node.subTrees[0]);
				queue.add(node.subTrees[1]);
				queue.add(node.subTrees[2]);
				queue.add(node.subTrees[3]);
			}
		}

		return total;
	}

	private static int getNumLeafs(QuadTree tree) {
		int total = 0;

		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(tree);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);

			if (node.isLeaf) {
				total++;
			}
			else {
				queue.add(node.subTrees[0]);
				queue.add(node.subTrees[1]);
				queue.add(node.subTrees[2]);
				queue.add(node.subTrees[3]);
			}
		}

		return total;
	}

	private static void readData() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("/Users/ahmed/Documents/MyWork/data/osm/reduced.txt"));

			ArrayList<Tuple> temp = new ArrayList<Tuple>();

			String line = br.readLine();
			while (line != null) {

				String[] parts = line.split(",");
				Tuple t = new Tuple();

				t.location.xCoord = Long.parseLong(parts[1]);
				t.location.yCoord = Long.parseLong(parts[0]);

				temp.add(t);

				line = br.readLine();
			}

			System.out.println("all read");


			innerPoints = new ArrayList<Tuple>();
			outerPoints = new ArrayList<Tuple>();


			for (int i = 0; i < scale * 150000; i++) {
				innerPoints.add(temp.get((int)(Math.random() * temp.size())));
			}

			for (int i = 0; i < scale / 15 * 150000; i++) {
				outerPoints.add(temp.get((int)(Math.random() * temp.size())));
			}

			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
