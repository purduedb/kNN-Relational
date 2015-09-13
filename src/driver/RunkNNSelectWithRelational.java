package driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

import output.kNNToken;

import data.*;

import index.*;
import exec.*;

public class RunkNNSelectWithRelational {

	private static Tuple focalTuple = new Tuple();

	//	private static void runSRJoin() {
	//		Rectangle R = new Rectangle(0, 0, 100, 100);
	//		String path = "//Users//ahmed//Documents//MyWork//data//TPC-H//10//Preprocessed//Orders//orders.tbl1";
	//		DataReader reader = new DataReader(path);
	//
	//		System.out.println("Reading Data");		
	//		QuadTree qTree = new QuadTree(R);		
	//		reader.readCustomers(qTree);
	//
	//		System.out.println("Starting Execution");
	//
	//		int k = 1000;
	//
	//		kNNJoinWithRelational mykNN = new kNNJoinWithRelational();
	//		long startTime = System.nanoTime();
	//
	//		ArrayList<String> fastJoinOutput = mykNN.kNNFirstJoin(k, qTree, qTree);
	//
	//		//String myArr = mykNN.kNNSelect(10, focal.location, qTree);
	//		//System.out.println(myArr);
	//
	//		long endTime = System.nanoTime();
	//		double execTime = (endTime - startTime) / 1000000000.0;
	//		System.out.println(execTime);
	//
	//		startTime = System.nanoTime();
	//
	//		ArrayList<String> slowJoinOutput = mykNN.SRJoin(k, qTree, qTree);
	//
	//		endTime = System.nanoTime();
	//		execTime = (endTime - startTime) / 1000000000.0;
	//		System.out.println(execTime);
	//
	//		System.out.println(qTree.numTuples);
	//		System.out.println(fastJoinOutput.size());
	//		System.out.println(slowJoinOutput.size());
	//
	//		for (String str : slowJoinOutput) {
	//			if (!fastJoinOutput.contains(str)) {
	//				System.out.println("failure at " + str);
	//				return;
	//			}
	//		}
	//
	//		//		for (String str : slowJoinOutput) {
	//		//			System.out.println(str);
	//		//		}
	//		//		System.out.println("**************");
	//		//		
	//		//		for (String str : fastJoinOutput) {
	//		//			System.out.println(str);
	//		//		}
	//		System.out.println("success");
	//	}

	public static void main(String[] args) {
		//Random r = new Random(); 
		focalTuple.location.xCoord = 35;
		focalTuple.location.yCoord = 17;	
		//t.location.xCoord = r.nextDouble() *100;
		//t.location.yCoord = r.nextDouble() *100;

		runAllSelectivities();
		//runAllK();
		//runAllSF();

	}
	private static void runAllSF() {

		int k = 10;
		int threshold = 35;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//SF.csv"));
			out.write("Selectivity, Locality Guided, Relational First\r\n");

			out.flush();

			for (int sf = 1; sf < 10; sf ++) {

				HashMap<Integer, Order> orders = new HashMap<Integer, Order>();
				HashMap<Integer, ArrayList<LineItem>> itemsBySupplier = new HashMap<Integer, ArrayList<LineItem>>();
				HashMap<Integer, ArrayList<LineItem>> itemsByOrder = new HashMap<Integer, ArrayList<LineItem>>();
				Rectangle R = new Rectangle(0, 0, 100, 100);
				QuadTree qTree = new QuadTree(R);

				readDataQ21(sf, orders, itemsBySupplier, itemsByOrder, qTree);

				System.gc();

				System.out.println(sf + ", " + threshold + ", " + k + ",");
				out.write(sf + ", " + threshold + ", " + k + ",");
				runkNNSelect(out, k, threshold, focalTuple, orders, itemsByOrder, itemsBySupplier, qTree);
				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static void readDataQ21(int scaleFactor, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, QuadTree qTree) {
		System.out.println("Reading Data");
		String path = "//Users//ahmed//Documents//MyWork//data//TPC-H//" + scaleFactor + "//";
		DataReader reader = new DataReader(path);

		reader.readOrdersIndexByOrderKey(orders);
		reader.readLineItems(itemsBySupplier, itemsByOrder);
		reader.readSuppliers(qTree);
	}

	private static void runAllK() {

		int scaleFactor = 5;

		HashMap<Integer, Order> orders = new HashMap<Integer, Order>();
		HashMap<Integer, ArrayList<LineItem>> itemsBySupplier = new HashMap<Integer, ArrayList<LineItem>>();
		HashMap<Integer, ArrayList<LineItem>> itemsByOrder = new HashMap<Integer, ArrayList<LineItem>>();
		Rectangle R = new Rectangle(0, 0, 100, 100);
		QuadTree qTree = new QuadTree(R);

		readDataQ21(scaleFactor, orders, itemsBySupplier, itemsByOrder, qTree);

		System.gc();

		System.out.println("Starting Execution");
		int threshold = 35;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//k.csv"));
			out.write("Selectivity, Locality Guided, Relational First\r\n");

			out.flush();

			for (int k = 2; k <= 2048; k*=2) {
				System.out.println(scaleFactor + ", " + threshold + ", " + k + ",");
				out.write(scaleFactor + ", " + threshold + ", " + k + ",");
				runkNNSelect(out, k, threshold, focalTuple, orders, itemsByOrder, itemsBySupplier, qTree);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runAllSelectivities() {

		int scaleFactor = 5;

		HashMap<Integer, Order> orders = new HashMap<Integer, Order>();
		HashMap<Integer, ArrayList<LineItem>> itemsBySupplier = new HashMap<Integer, ArrayList<LineItem>>();
		HashMap<Integer, ArrayList<LineItem>> itemsByOrder = new HashMap<Integer, ArrayList<LineItem>>();
		Rectangle R = new Rectangle(0, 0, 100, 100);
		QuadTree qTree = new QuadTree(R);

		readDataQ21(scaleFactor, orders, itemsBySupplier, itemsByOrder, qTree);

		System.gc();
		
		//histogram(qTree, orders, itemsByOrder, itemsBySupplier);

		System.out.println("Starting Execution");
		int k = 10;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//selectivity.csv"));
			out.write("Selectivity, Locality Guided, Relational First\r\n");

			out.flush();

			for (int threshold = 14; threshold <= 60; threshold+=1) {
				System.out.println(scaleFactor + ", " + threshold + ", " + k + ",");
				out.write(scaleFactor + ", " + threshold + ", " + k + ",");
				runkNNSelect(out, k, threshold, focalTuple, orders, itemsByOrder, itemsBySupplier, qTree);
			}


			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void histogram(QuadTree suppliers, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier) {
		HashMap<Integer, Integer> counts = new HashMap<Integer, Integer>();
		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(suppliers);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);
			if (node.isLeaf) {
				for (Tuple tuple : node.tuples) {
					int count = count((Supplier)tuple, orders, itemsByOrder, itemsBySupplier);
					if (counts.containsKey(count))
						counts.put(count, counts.get(count) + 1);
					else
						counts.put(count, 1);					
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

	private static int count(Supplier supplier, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier) {

		boolean supplierFailedAlone;
		int count = 0;
		ArrayList<LineItem> supplierItems = itemsBySupplier.get(supplier.suppKey);

		for (LineItem item : supplierItems) {
			supplierFailedAlone = false;
			if (item.receiptDate > item.commitDate) {
				Order order = orders.get(item.orderKey);
				if (order.orderStatus == 'F') {
					ArrayList<LineItem> orderItems = itemsByOrder.get(item.orderKey);
					for (LineItem otherItem : orderItems) {
						if (otherItem.suppKey != item.suppKey) {
							supplierFailedAlone = true;
							break;
						}
					}
					if (supplierFailedAlone) {
						for (LineItem otherItem : orderItems) {
							if (otherItem.suppKey != item.suppKey) {
								if (otherItem.receiptDate <= otherItem.commitDate) {
									supplierFailedAlone = false;
									break;
								}
							}
						}
					}
				}
			}
			if(supplierFailedAlone) {
				count++;
			}
		}
		return count;
	}

	private static void runkNNSelect(BufferedWriter out, int k, int threshold, Tuple t, HashMap<Integer, Order> orders, HashMap<Integer, ArrayList<LineItem>> itemsByOrder, HashMap<Integer, ArrayList<LineItem>> itemsBySupplier, QuadTree qTree) throws IOException {

		kNNSelectWithRelational mykNN = new kNNSelectWithRelational();
		int repeated = 3;

		PriorityQueue<Tuple> fast = mykNN.kNNSelectRelationalOperator(k, threshold, t.location, orders, itemsByOrder, itemsBySupplier, qTree);
		out.write(((kNNToken)fast).numIO + ", ");
		System.out.println(((kNNToken)fast).numIO + ", ");

		long startTime = System.nanoTime();
		for (int i = 0; i < repeated; i ++) {
			//System.out.println(i);
			//long innerStartTime = System.nanoTime();
			fast = mykNN.kNNSelectLocalityGuided(k, threshold, t.location, orders, itemsByOrder, itemsBySupplier, qTree);			
			//			long innerEndTime = System.nanoTime();
			//			double innerExecTime = (innerEndTime - innerStartTime) / 1000000000.0;
			//			System.out.println(innerExecTime + "sec");
		}
		long endTime = System.nanoTime();
		double execTime = (endTime - startTime) / 1000000000.0;
		execTime/=repeated;
		System.out.println(execTime);

		out.write(execTime + ", ");



		//PriorityQueue<Tuple> slow = mykNN.kNNSelectRelFirst(k, threshold, t.location, orders, itemsByOrder, itemsBySupplier, qTree);
		//out.write(((kNNToken)slow).numIO + ", ");
		startTime = System.nanoTime();
		for (int i = 0; i < repeated; i ++) {
			//System.out.println(i);
			//long innerStartTime = System.nanoTime();
			//slow = mykNN.kNNSelectRelFirst(k, threshold, t.location, orders, itemsByOrder, itemsBySupplier, qTree);
			//			long innerEndTime = System.nanoTime();
			//			double innerExecTime = (innerEndTime - innerStartTime) / 1000000000.0;
			//			System.out.println(innerExecTime + "sec");
		}
		endTime = System.nanoTime();
		execTime = (endTime - startTime) / 1000000000.0;
		execTime/=repeated;
		//System.out.println(execTime);
		out.write(execTime + ", ");

		out.write("\r\n");
		out.flush();

//		while (slow.size() != 0) {
//			System.out.print(slow.remove().distance + " - ");
//		}

		System.out.println();

//		while (fast.size() != 0) {
//			System.out.print(fast.remove().distance + " - ");
//		}
	}
}