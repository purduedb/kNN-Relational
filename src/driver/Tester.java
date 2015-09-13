package driver;

import index.QuadTree;
import index.Rectangle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

import data.Constants;
import data.Tuple;
import exec.Common;
import exec.DistanceBrowser;

public class Tester {

	private static double scale = 1;
	private static ArrayList<Tuple> innerPoints;
	private static ArrayList<Tuple> outerPoints;

	private static int k = 100;

	public static void main(String[] args) {
		readData();
		
		for (int i = 0; i < 1000; i++) {
			testSelect();
		}
		//testJoin();
	}

	private static void testSelect() {
		
		Rectangle R = new Rectangle(Constants.minLong, Constants.minLat, Constants.maxLong-Constants.minLong, Constants.maxLat-Constants.minLat);

		QuadTree innerTree = new QuadTree(R);
		innerTree.insert(innerPoints);

		Tuple q = new Tuple();
		k = (int)(Math.random() * 100000) + 1;
		q.location.xCoord = Math.random() * Constants.worldWidth;
		q.location.yCoord = Math.random() * Constants.worldHeight;

		ArrayList<QuadTree> locality = Common.getLocality(k, q.location, innerTree);
		PriorityQueue<Tuple> fast = Common.getkNNFromLocality(k, q.location, locality);
		ArrayList<Double> fastDist = new ArrayList<Double>();		
		while (!fast.isEmpty())
			fastDist.add(fast.remove().distance);

		PriorityQueue<Tuple> slow = Common.getkNN(k, q.location, innerPoints);
		ArrayList<Double> slowDist = new ArrayList<Double>();		
		while (!slow.isEmpty())
			slowDist.add(slow.remove().distance);
		
		DistanceBrowser dBrowser = new DistanceBrowser(q.location, innerTree);
		ArrayList<Double> browserDist = new ArrayList<Double>();
		for (int i = 1; i <=k; i++) {
			Tuple next = dBrowser.getNext();
			//System.out.print(next.distance + ", ");
			browserDist.add(next.distance);
		}
		//System.out.println();
		
		for (int i = 0; i < browserDist.size()/2; i++) {
			double temp = browserDist.get(i);
			browserDist.set(i, browserDist.get(browserDist.size() - i - 1));
			browserDist.set(browserDist.size() - i - 1, temp);
		}
//		for (int i = 0; i < browserDist.size(); i++) {
//			System.out.print(browserDist.get(i) + ", ");
//		}
//		System.out.println();
		
		int n = k;
		while (slowDist.size() > 0) {			
			n--;
			if (Math.abs(fastDist.get(0) - slowDist.get(0)) > 0.1 || Math.abs(fastDist.get(0) - browserDist.get(0)) > 0.1) {			
				System.out.println("OOPS... FAILURE");
				System.out.println("k = " + k);
				System.out.println("Failed at " + (k - n));
				System.out.println("Q Location " + q.location.xCoord + ", " + q.location.yCoord);
				System.out.println(fastDist.get(0) + "    " + slowDist.get(0) + "  " + browserDist.get(0));				
			}
			fastDist.remove(0); slowDist.remove(0); browserDist.remove(0);
		}
		System.out.println("SUCCESS");
	}

	private static void testJoin() {
		Rectangle R = new Rectangle(Constants.minLong, Constants.minLat, Constants.maxLong-Constants.minLong, Constants.maxLat-Constants.minLat);
		//QuadTree tree = new QuadTree(R, 10);
		QuadTree outerTree = new QuadTree(R, 10000, 0);
		outerTree.insert(outerPoints);

		QuadTree innerTree = new QuadTree(R);
		innerTree.insert(innerPoints);

		HashSet<String> exact = new HashSet<String>();
		for (Tuple t : outerPoints) {
			exact.add(Common.flushOutput(t.location, Common.getkNN(k, t.location, innerPoints)));
		}

		HashSet<String> fast = new HashSet<String>();
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(outerTree);
		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				ArrayList<QuadTree> locality = Common.getLocality(k, node, innerTree);
				ArrayList<String> join = Common.getkNNFromLocality(k, node, locality);
				for (String str : join) {
					fast.add(str);
				}
			}
			else {
				queue.add(node.subTrees[0]);
				queue.add(node.subTrees[1]);
				queue.add(node.subTrees[2]);
				queue.add(node.subTrees[3]);
			}
		}

		if (fast.size() != exact.size()) {
			System.out.println("Failure, size not the same");
			return;
		}
		boolean failure = false;
		for (String str : fast) {
			if (!exact.contains(str)) {
				System.out.println("Failure");
				System.out.println(str);
				failure = true;
				break;
			}
		}
		if (!failure)
			System.out.println("Success");
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