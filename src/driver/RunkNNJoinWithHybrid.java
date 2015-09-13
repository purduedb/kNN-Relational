package driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import output.kNNToken;

import data.*;

import index.*;
import exec.*;

public class RunkNNJoinWithHybrid {

	public static void main(String[] args) {

		//runAllK();
		runAllSF();

	}
	private static void runAllSF() {

		int k = 16;

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//SF.csv"));
			out.write("Selectivity, Locality Guided, Relational First\r\n");

			out.flush();

			for (int sf = 1; sf < 10; sf ++) {

				BPTree<Double, ArrayList<Tuple>> customerBPTree = new BPTree<Double, ArrayList<Tuple>>();
				ArrayList<Tuple> suppliers = new ArrayList<Tuple>();
				Rectangle R = new Rectangle(0, 0, 100, 100);
				QuadTree customerQTree = new QuadTree(R);

				readData(sf, suppliers, customerQTree, customerBPTree);

				System.gc();

				System.out.println(sf + ", " + k + ",");
				out.write(sf + ", " + k + ",");
				runJoin(out, k, suppliers, customerQTree, customerBPTree);
				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readData(int scaleFactor, ArrayList<Tuple> suppliers, QuadTree customerQTree, BPTree<Double, ArrayList<Tuple>> customerBPTree) {
		System.out.println("Reading Data");
		String path = "//Users//ahmed//Documents//MyWork//data//TPC-H//" + scaleFactor + "//";
		DataReader reader = new DataReader(path);


		reader.readSuppliers(suppliers);
		reader.readCustomers(customerQTree, customerBPTree);
	}

	private static void runAllK() {

		int scaleFactor = 5;

		BPTree<Double, ArrayList<Tuple>> customerBPTree = new BPTree<Double, ArrayList<Tuple>>();
		ArrayList<Tuple> suppliers = new ArrayList<Tuple>();
		Rectangle R = new Rectangle(0, 0, 100, 100);
		QuadTree customerQTree = new QuadTree(R);

		readData(scaleFactor, suppliers, customerQTree, customerBPTree);

		System.gc();

		System.out.println("Starting Execution");

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("//Users//ahmed//Desktop//k.csv"));
			out.write("Selectivity, Locality Guided, Relational First\r\n");

			out.flush();

			for (int k = 2; k <= 2048; k*=2) {
				System.out.println(scaleFactor + ", " + ", " + k + ",");
				out.write(scaleFactor + ", " + ", " + k + ",");
				runJoin(out, k, suppliers, customerQTree, customerBPTree);
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void runJoin(BufferedWriter out, int k, ArrayList<Tuple> suppliers, QuadTree customerQTree, BPTree<Double, ArrayList<Tuple>> customerBPTree) throws IOException{

		kNNJoinWithHybrid mykNN = new kNNJoinWithHybrid();
		int repeated = 2;

		System.out.println("hybrid");
		ArrayList<kNNToken> hybrid = mykNN.hybridJoin(k, suppliers, customerQTree, customerBPTree);
		//out.write(((kNNToken)fast).numIO + ", ");

		long startTime = System.nanoTime();
		for (int i = 0; i < repeated; i ++) {
			hybrid = mykNN.hybridJoin(k, suppliers, customerQTree, customerBPTree);			
		}
		long endTime = System.nanoTime();
		double execTime = (endTime - startTime) / 1000000000.0;
		execTime/=repeated;
		out.write(execTime + ", ");
		System.out.println(execTime + ", ");
		
		int numIO = 0;
		for (kNNToken token : hybrid)
			numIO += token.numIO;
		out.write(numIO + ", ");
		System.out.println(numIO + ", ");

		
		
		System.out.println("Locality Only");
		ArrayList<kNNToken> localityOnly = mykNN.localityOnlyJoin(k, suppliers, customerQTree);
		//out.write(((kNNToken)slow).numIO + ", ");
		startTime = System.nanoTime();
		for (int i = 0; i < repeated; i ++) {
			localityOnly = mykNN.localityOnlyJoin(k, suppliers, customerQTree);
		}
		endTime = System.nanoTime();
		execTime = (endTime - startTime) / 1000000000.0;
		execTime/=repeated;
		out.write(execTime + ", ");
		System.out.println(execTime + ", ");
		
		numIO = 0;
		for (kNNToken token : localityOnly)
			numIO += token.numIO;
		out.write(numIO + ", ");
		System.out.println(numIO + ", ");

//		System.out.println("Relational Only");
//		ArrayList<kNNToken> relOnly = mykNN.relationalOnlyJoin(k, suppliers, customerBPTree);
//		//out.write(((kNNToken)slow).numIO + ", ");
//		startTime = System.nanoTime();
//		for (int i = 0; i < repeated; i ++) {
//			//relOnly = mykNN.relationalOnlyJoin(k, suppliers, customerBPTree);
//		}
//		endTime = System.nanoTime();
//		execTime = (endTime - startTime) / 1000000000.0;
//		execTime/=repeated;
//		out.write(execTime + ", ");
//		System.out.println(execTime + ", ");
//
		out.write("\r\n");
		out.flush();

//		for (int i = 0; i < 10; i++) {
//			kNNToken hybridToken = hybrid.get(i);
//			kNNToken localityOnlyToken = localityOnly.get(i);
//			kNNToken relOnlyToken = relOnly.get(i);
//
//			System.out.print(hybridToken.focalPoint + "     ");
//			while (hybridToken.size() != 0)
//				System.out.print(hybridToken.remove() + " - ");
//
//			System.out.println();
//
//			System.out.print(localityOnlyToken.focalPoint + "     ");
//			while (localityOnlyToken.size() != 0)
//				System.out.print(localityOnlyToken.remove() + " - ");
//			
//			System.out.println();
//			
//			System.out.print(relOnlyToken.focalPoint + "     ");
//			
//			while (relOnlyToken.size() != 0)
//				System.out.print(relOnlyToken.remove() + " - ");
//				
//			
//			System.out.println();
//			System.out.println();
//		}
		
	}
}