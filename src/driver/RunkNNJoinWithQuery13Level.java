package driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import data.*;

import index.*;
import exec.*;

public class RunkNNJoinWithQuery13Level {

	public static void main(String[] args) {

		//runAllK();
		//runAllSF();
		runAllSelectivities();
		//runClusters();

	}

	private static void runAllSelectivities() {

		int sf = 5;
		int k = 10;
		int numClusters = 1;

		Rectangle R = new Rectangle(0, 0, 100, 100);

		QuadTree supplierQTree = new QuadTree(R);
		QuadTree customerQTree = new QuadTree(R);
		HashMap<Integer, ArrayList<Order>> orders = new HashMap<Integer, ArrayList<Order>>();

		readData(numClusters, sf, supplierQTree, customerQTree, orders);

		System.out.println("Starting Execution");

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//selectivity.csv"));
			out.write("scale Factor, threshold, k, Materialized CPU, Materialized IO, Locality CPU, Locality IO \r\n");

			out.flush();

			for (int threshold = 0; threshold <= 44; threshold+=1) {

				System.out.println(numClusters + "," + sf + ", " + threshold + ", " + k + ", ");
				out.write(numClusters + "," + sf + ", " + threshold + ", " + k + ", ");
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
		int threshold = 10;
		int numClusters = 1;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//SF.csv"));
			out.write("clusters, scale Factor, threshold, k, Materialized CPU, Materialized IO, Locality CPU, Locality IO \r\n");

			out.flush();

			for (int sf = 1; sf < 10; sf++) {
				Rectangle R = new Rectangle(0, 0, 100, 100);

				QuadTree supplierQTree = new QuadTree(R);
				QuadTree customerQTree = new QuadTree(R);
				HashMap<Integer, ArrayList<Order>> orders = new HashMap<Integer, ArrayList<Order>>();

				readData(numClusters, sf, supplierQTree, customerQTree, orders);

				//histogram(customerQTree, orders);

				System.out.println(numClusters + "," + sf + ", " + threshold + ", " + k + ", ");
				out.write(numClusters + "," + sf + ", " + threshold + ", " + k + ", ");
				runJoin(out, k, threshold, supplierQTree, customerQTree, orders);
				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runClusters() {

		int k = 10;
		int threshold = 10;
		int sf = 5;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//Clusters.csv"));
			out.write("clusters, scale Factor, threshold, k, Materialized CPU, Materialized IO, Locality CPU, Locality IO \r\n");

			out.flush();

			for (int numClusters = 1; numClusters < 10; numClusters++) {
				Rectangle R = new Rectangle(0, 0, 100, 100);

				QuadTree supplierQTree = new QuadTree(R);
				QuadTree customerQTree = new QuadTree(R);
				HashMap<Integer, ArrayList<Order>> orders = new HashMap<Integer, ArrayList<Order>>();

				readData(numClusters, sf, supplierQTree, customerQTree, orders);

				//histogram(customerQTree, orders);

				System.out.println(numClusters + "," + sf + ", " + threshold + ", " + k + ", ");
				out.write(numClusters + "," + sf + ", " + threshold + ", " + k + ", ");
				runJoin(out, k, threshold, supplierQTree, customerQTree, orders);
				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void readData(int numClusters, int scaleFactor, QuadTree supplierQTree, QuadTree customerQTree, HashMap<Integer, ArrayList<Order>> orders) {
		System.out.println("Reading Data");
		String path = "//Users//ahmed//Documents//MyWork//data//TPC-H//" + scaleFactor + "//";
		DataReader reader = new DataReader(path);


		reader.readSuppliers(supplierQTree);
		reader.readCustomers(customerQTree);
		reader.readOrdersIndexByCust(orders);
	}

	private static void runAllK() {

		int sf = 5;
		int threshold = 10;
		int numClusters = 1;

		Rectangle R = new Rectangle(0, 0, 100, 100);

		QuadTree supplierQTree = new QuadTree(R);
		QuadTree customerQTree = new QuadTree(R);
		HashMap<Integer, ArrayList<Order>> orders = new HashMap<Integer, ArrayList<Order>>();

		readData(numClusters, sf, supplierQTree, customerQTree, orders);

		System.out.println("Starting Execution");

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//k.csv"));
			out.write("clusters, scale Factor, threshold, k, Materialized CPU, Materialized IO, Locality CPU, Locality IO \r\n");

			out.flush();

			for (int k = 2; k <= 2048; k*=2) {
				System.out.println(numClusters + "," + sf + ", " + threshold + ", " + k + ", ");
				out.write(numClusters + "," + sf + ", " + threshold + ", " + k + ", ");
				runJoin(out, k, threshold, supplierQTree, customerQTree, orders);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runJoin(BufferedWriter out, int k, int threshold, QuadTree supplierQTree, QuadTree customerQTree, HashMap<Integer, ArrayList<Order>> orders) throws IOException{
		System.gc();

		kNNJoinWithQuery13Level mykNN = new kNNJoinWithQuery13Level();
		int repeated = 1;

		customerQTree.clearFlags();
		System.out.println("kNN Materialized");
		ArrayList<String> kNNmaterialized = null;//mykNN.kNNMAterialized(k, threshold, supplierQTree, customerQTree, orders);

		long startTime = System.nanoTime();
		for (int i = 0; i < repeated; i ++) {
			//customerQTree.clearFlags();
			kNNmaterialized = mykNN.kNNMAterialized(k, threshold, supplierQTree, customerQTree, orders);			
		}
		long endTime = System.nanoTime();
		double execTime = (endTime - startTime) / 1000000000.0;
		execTime /= repeated;
		out.write(execTime + ", ");
		System.out.println(execTime + ", ");		
		out.write(mykNN.numIO + ", ");
		System.out.println(mykNN.numIO + ", ");
		
		

		System.out.println("Locality Guided");
		customerQTree.clearFlags();
		ArrayList<String> kNNLocalityGuided = null;//mykNN.localityGuided(k, threshold, supplierQTree, customerQTree, orders);
		startTime = System.nanoTime();
		for (int i = 0; i < repeated; i ++) {
			//customerQTree.clearFlags();
			kNNLocalityGuided = mykNN.localityGuided(k, threshold, supplierQTree, customerQTree, orders);
		}
		endTime = System.nanoTime();
		execTime = (endTime - startTime) / 1000000000.0;
		execTime/=repeated;
		out.write(execTime + ", ");
		System.out.println(execTime + ", ");
		out.write(mykNN.numIO + ", ");
		System.out.println(mykNN.numIO + ", ");
		
		//		
		if (kNNLocalityGuided.size() != kNNmaterialized.size())
			System.out.println("Failure " + kNNLocalityGuided.size() + ", " + kNNmaterialized.size());
		else
			System.out.println("Success " + kNNLocalityGuided.size() + ", " + kNNmaterialized.size());
		for (int i = 0; i < kNNLocalityGuided.size(); i++)
			if (!kNNLocalityGuided.get(i).equals(kNNmaterialized.get(i))) {
				System.out.println("Failure at " + i);
				System.out.println(kNNLocalityGuided.get(i));
				System.out.println(kNNmaterialized.get(i));
				return;
			}

		out.write("\r\n");
		out.flush();
	}
}