package driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import data.*;

import index.*;
import exec.*;

public class RunkNNJoinWithPredicateLevel {

	public static void main(String[] args) {

		runAllK();
		//runAllSF();
		//runAllSelectivities();

	}

	private static void runAllSelectivities() {

		int sf = 5;
		int k = 10;

		Rectangle R = new Rectangle(0, 0, 100, 100);

		QuadTree supplierQTree = new QuadTree(R);
		QuadTree customerQTree = new QuadTree(R);
		HashMap<Integer, ArrayList<Order>> orders = new HashMap<Integer, ArrayList<Order>>();

		readData(sf, supplierQTree, customerQTree, orders);

		System.out.println("Starting Execution");

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//selectivity.csv"));
			out.write("scale Factor, threshold, k, kNNFirst, kinCircle \r\n");

			out.flush();

			for (int threshold = 0; threshold <= 44; threshold+=1) {

				System.out.println(sf + ", " + threshold + "," + k + ",");
				out.write(sf + ", " + threshold + "," + k + ",");
				runJoin(out, k, threshold, supplierQTree, customerQTree, orders);
				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runAllSF() {

		int k = 10;
		int threshold = 25;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//SF.csv"));
			out.write("scale Factor, threshold, k, kNNFirst, kinCircle \r\n");

			out.flush();

			for (int sf = 1; sf < 10; sf++) {
				Rectangle R = new Rectangle(0, 0, 100, 100);

				QuadTree supplierQTree = new QuadTree(R);
				QuadTree customerQTree = new QuadTree(R);
				HashMap<Integer, ArrayList<Order>> orders = new HashMap<Integer, ArrayList<Order>>();

				readData(sf, supplierQTree, customerQTree, orders);

				//histogram(customerQTree, orders);

				System.out.println(sf + ", " + threshold + ", " + k + ", ");
				out.write(sf + ", " + threshold + "," + k + ",");
				runJoin(out, k, threshold, supplierQTree, customerQTree, orders);
				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readData(int scaleFactor, QuadTree supplierQTree, QuadTree customerQTree, HashMap<Integer, ArrayList<Order>> orders) {
		System.out.println("Reading Data");
		String path = "//Users//ahmed//Documents//MyWork//data//TPC-H//" + scaleFactor + "//";
		DataReader reader = new DataReader(path);


		reader.readSuppliers(supplierQTree);
		reader.readCustomers(customerQTree);
		reader.readOrdersIndexByCust(orders);
	}

	private static void runAllK() {

		int sf = 5;
		int threshold = 25;

		Rectangle R = new Rectangle(0, 0, 100, 100);

		QuadTree supplierQTree = new QuadTree(R);
		QuadTree customerQTree = new QuadTree(R);
		HashMap<Integer, ArrayList<Order>> orders = new HashMap<Integer, ArrayList<Order>>();

		readData(sf, supplierQTree, customerQTree, orders);

		System.out.println("Starting Execution");

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//k.csv"));
			out.write("scale Factor, threshold, k, kNNFirst, kinCircle \r\n");

			out.flush();

			for (int k = 2; k <= 2048; k*=2) {
				System.out.println(sf + ", " + threshold + "," + k + ",");
				out.write(sf + ", " + threshold + "," + k + ",");
				runJoin(out, k, threshold, supplierQTree, customerQTree, orders);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void histogram(QuadTree customerQTree, HashMap<Integer, ArrayList<Order>> orders) {
		HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>();
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(customerQTree);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				for (Tuple tuple : node.tuples) {
					ArrayList<Order> myOrders = orders.get(((Customer)tuple).custKey);
					if (myOrders != null) {
						if (counts.containsKey(myOrders.size()))
							counts.put(myOrders.size(), counts.get(myOrders.size()) + 1);
						else
							counts.put(myOrders.size(), 1);
					}
				}
			}
			else {
				for (QuadTree qTree : node.subTrees)
					queue.add(qTree);
			}
		}

		int total = 0;
		for (int key : counts.keySet()) {
			System.out.println(key + ",    " + counts.get(key));
			total+=counts.get(key);
		}
		System.out.println(total);
	}

	private static void runJoin(BufferedWriter out, int k, int threshold, QuadTree supplierQTree, QuadTree customerQTree, HashMap<Integer, ArrayList<Order>> orders) throws IOException{
		//System.gc();

		kNNJoinWithPredicateLevel mykNN = new kNNJoinWithPredicateLevel();
		int repeated = 1;

		customerQTree.clearFlags();
		System.out.println("kNN First");
		ArrayList<String> kNNFirst = null;//mykNN.kNNFirst(k, threshold, supplierQTree, customerQTree, orders);

		customerQTree.clearFlags();
		long startTime = System.nanoTime();
		for (int i = 0; i < repeated; i ++) {
			//customerQTree.clearFlags();
			kNNFirst = mykNN.kNNFirst(k, threshold, supplierQTree, customerQTree, orders);			
		}
		long endTime = System.nanoTime();
		double execTime = (endTime - startTime) / 1000000000.0;
		execTime /= repeated;
		out.write(execTime + ", ");
		System.out.println(execTime + ", ");


		System.out.println("k in circle");
		customerQTree.clearFlags();
		ArrayList<String> kinCircle = null;//mykNN.SRJoin(k, threshold, supplierQTree, customerQTree, orders);
		startTime = System.nanoTime();
		customerQTree.clearFlags();
		for (int i = 0; i < repeated; i ++) {
			//customerQTree.clearFlags();
			kinCircle = mykNN.SRJoin(k, threshold, supplierQTree, customerQTree, orders);
		}
		endTime = System.nanoTime();
		execTime = (endTime - startTime) / 1000000000.0;
		execTime/=repeated;
		out.write(execTime + ", ");
		System.out.println(execTime + ", ");
		
//		if (kinCircle.size() != kNNFirst.size())
//			System.out.println("Failure " + kinCircle.size() + ", " + kNNFirst.size());
//		else
//			System.out.println("Success " + kinCircle.size() + ", " + kNNFirst.size());
//		for (int i = 0; i < kinCircle.size(); i++)
//			if (!kinCircle.get(i).equals(kNNFirst.get(i))) {
//				System.out.println("Failure at " + i);
//				System.out.println(kinCircle.get(i));
//				System.out.println(kNNFirst.get(i));
//				return;
//			}

		out.write("\r\n");
		out.flush();

	}
}