package driver.cost.estimation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

import optimizer.CatalogEntry;
import cost.estimate.JoinEstimator;
import cost.estimate.SelectEstimator;
import index.QuadTree;
import data.Constants;
import data.DataReader;
import data.Tuple;
import exec.Common;

public class RunSelect {

	static String txtDataPath = "/Users/ahmed/Documents/MyWork/data/osm/reduced.txt";
	static String binDataPath = "/Users/ahmed/Documents/MyWork/data/osm/reduced.binary";
	static String resultsPath = "/Users/ahmed/Documents/MyWork/Results/kNNCost/Select/";

	public static void main(String[] args) {

		//varyK();
		//varyScale();

		showCatalog();
	}

	private static void showCatalog() {
		System.out.println("Loading data points");
		DataReader reader = new DataReader(binDataPath);
		QuadTree qTree = new QuadTree(Constants.getBounds(), 10000, 0);
		reader.readBinPointLocations(qTree, 1);

		try {
			for (int i = 0; i < 100; i++) {
				BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "catalog.csv"));

				QuadTree node = Common.getRandomLeaf(qTree);
				//ArrayList<CatalogEntry> catalog = SelectEstimator.getPivotCatalogOnePass(Common.getCenter(node), qTree);
				ArrayList<CatalogEntry> catalog = JoinEstimator.getLocalityCatalogOnePass(node, qTree);
				for (CatalogEntry ce : catalog) {
					System.out.println("[" + ce.startK + ", " + ce.endK + "] " + ce.numBlocks);
					out.write(ce.startK + ", " + ce.endK + ", " + ce.numBlocks + "\r\n");
				}
				out.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void varyK() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "vary_k.csv"));
			out.write("K, Time Density, Time No Corner, Time Corner\r\n");
			out.flush();

			//Constants.maxK = 10000;

			System.out.println("Loading data points");
			DataReader reader = new DataReader(binDataPath);
			QuadTree qTree = new QuadTree(Constants.getBounds(), 10000, 0);
			reader.readBinPointLocations(qTree, 1);			
			System.out.println("All points: " + qTree.numTuples);
			System.out.println("Skipped " + qTree.skipped);
			System.err.println("All data loaded successfuly");

			System.out.println("Loading Queries");
			ArrayList<Tuple> qPoints = readQPoints();
			System.out.println("Queries Loaded");

			System.out.println("Vary K Experiment");
			System.out.println("Preprocessing Centers");
			SelectEstimator.preprocessCenter(qTree);
			System.out.println("Preprocessing centers complete");

			System.out.println("Preprocessing Corners");
			SelectEstimator.preprocessCorners(qTree);
			System.out.println("Preprocessing centers complete");

			for (int k = 1; k <= 10000; k*=2) {
				System.err.println("\nK = " + k);

				out.write(k + ", ");
				runVaryK(k, qPoints, qTree, out);
				out.flush();
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void varyScale() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "vary_scale.csv"));
			out.write("Scale Factor, Preprocessing Time no Corner, Preprocessing Time with Corner, Storage Density, Storage no Corner, Storage with Corner," +
					" Accuracy Density, Accuracy no Corner, Accuracy with Corner\r\n");
			out.flush();

			Constants.maxK = 1000;
			System.out.println("Vary Size Experiment");
			for (int scale = 1; scale <= 10; scale++) {
				System.err.println("\nScale = " + scale);
				System.out.println("Loading data points");
				DataReader reader = new DataReader(binDataPath);
				QuadTree qTree = new QuadTree(Constants.getBounds(), 100000, 0);
				reader.readBinPointLocations(qTree, scale);			
				System.out.println("All points: " + qTree.numTuples);
				System.out.println("Skipped " + qTree.skipped);
				System.err.println("All data loaded successfuly");
				out.write(scale + ", ");
				runVaryScale(qTree, out);
				out.flush();
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void runVaryK(int k, ArrayList<Tuple> qPoints, QuadTree quadTree, BufferedWriter out) {

		System.out.println("Running Density Estimation:");
		long startTime = System.nanoTime();
		for (Tuple t : qPoints) {
			if (quadTree.searchEnclosingLeaf(t.location) == null) 
				continue;			
			SelectEstimator.estimateDensityBased(t.location, quadTree, k);			
		}
		long endTime = System.nanoTime();
		double timeDensity = (endTime - startTime) / 1000000000.0 / qPoints.size();
		System.out.println("Density Time: " + timeDensity);

		System.out.println("Running No Corner Estimation:");
		startTime = System.nanoTime();
		for (Tuple t : qPoints) {
			if (quadTree.searchEnclosingLeaf(t.location) == null) 
				continue;
			SelectEstimator.estimateWithoutCorners(t.location, quadTree, k);			
		}
		endTime = System.nanoTime();
		double timeNoCorner = (endTime - startTime) / 1000000000.0 / qPoints.size();
		System.out.println("No Corner Time: " + timeNoCorner);

		System.out.println("Running with Corners Estimation:");
		startTime = System.nanoTime();
		for (Tuple t : qPoints) {
			if (quadTree.searchEnclosingLeaf(t.location) == null) 
				continue;
			SelectEstimator.estimateWithCorners(t.location, quadTree, k);					
		}
		endTime = System.nanoTime();
		double timeWithCorners = (endTime - startTime) / 1000000000.0 / qPoints.size();
		System.out.println("With Corner Time: " + timeWithCorners);

		//out.write("Time Density, Time No Corner, Time Corner\r\n");
		try {
			out.write(timeDensity + ", " + timeNoCorner + ", " + timeWithCorners + "\r\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void runVaryScale(QuadTree quadTree, BufferedWriter out) {

		SelectEstimator.preprocessCenter(quadTree);
		System.out.println("Preprocessing Centers");
		long startTime = System.nanoTime();
		SelectEstimator.preprocessCenter(quadTree);
		long endTime = System.nanoTime();
		System.out.println("Preprocessing centers complete");
		double preprocessingCenterOnlyTime = (endTime - startTime) / 1000000000.0;
		System.out.println("Preprocessing center time = " + preprocessingCenterOnlyTime);

		SelectEstimator.preprocessCorners(quadTree);
		System.out.println("Preprocessing Corners");
		startTime = System.nanoTime();
		SelectEstimator.preprocessCorners(quadTree);
		endTime = System.nanoTime();
		System.out.println("Preprocessing corners complete");
		double preprocessingWithCornersTime = (endTime - startTime) / 1000000000.0 + preprocessingCenterOnlyTime;
		System.out.println("Preprocessing with corners time = " + preprocessingWithCornersTime);

		if (true)
			return;
		long storageOverheadNoCorners =  SelectEstimator.computeStorage(false, quadTree);
		long storageOverheadWithCorners =  SelectEstimator.computeStorage(true, quadTree);
		long storageOverheadDensity =  quadTree.getNumLeaves();

		//System.gc();
		System.out.println("Loading Queries");
		ArrayList<Tuple> qPoints = readQPoints();
		double accDensity = 0;
		double accCorner = 0;
		double accNoCorner = 0;
		int computedFor = 0;
		System.out.println("Queries Loaded");

		int i = 0;
		for (Tuple t : qPoints) {
			if (i++%10000 == 0) {
				int numQueriesProcessed = i/1000;
				System.out.println(numQueriesProcessed + " thousand queries processed");
				System.out.println("Density accuracy " + accDensity / computedFor);
				System.out.println("Corner accuracy " + accCorner / computedFor);
				System.out.println("No Corner accuracy " + accNoCorner / computedFor);
				//				System.out.println("Density accuracy " + accDensity / accActual);
				//				System.out.println("Corner accuracy " + accCorner / accActual);
				//				System.out.println("No Corner accuracy " + accNoCorner / accActual);
			}

			//int k = (int)(Math.random() * Constants.maxK) + 1;
			int k = (int)(Math.random() * (Constants.maxK)) + 1;
			if (quadTree.searchEnclosingLeaf(t.location) == null) 
				continue;
			double cornersCost = SelectEstimator.estimateWithCorners(t.location, quadTree, k);
			double noCornersCost = SelectEstimator.estimateWithoutCorners(t.location, quadTree, k);
			double densityCost = SelectEstimator.estimateDensityBased(t.location, quadTree, k);
			double actualCost = SelectEstimator.getActualCost(t.location, quadTree, k);


			//accActual += actualCost;

			// Corner accuracy
			//accCorner += cornersCost;
			double ratio = 1 - Math.abs(densityCost - actualCost) / actualCost;
			if (ratio < 0.3)
				continue;
			computedFor++;
			if (cornersCost < actualCost)
				accCorner += (double)cornersCost / (double)actualCost;
			else
				accCorner += (double)actualCost / (double)cornersCost;
			//			accCorner += ratio;


			//			// No Corner accuracy
			//accNoCorner += noCornersCost;
			//ratio = 1 - Math.abs(noCornersCost - actualCost) / actualCost;
			if (noCornersCost < actualCost)
				accNoCorner += (double)noCornersCost / (double)actualCost;
			else
				accNoCorner += (double)actualCost / (double)noCornersCost;
			//accNoCorner += ratio;

			//			// Density accuracy
			//accDensity += densityCost;
			//ratio = 1 - Math.abs(densityCost - actualCost) / actualCost;
			if (densityCost < actualCost)
				accDensity += (double)densityCost / (double)actualCost;
			else
				accDensity += (double)actualCost / (double)densityCost;
			//accDensity += ratio;
		}
		System.out.println("Density accuracy " + accDensity / computedFor);
		System.out.println("Corner accuracy " + accCorner / computedFor);
		System.out.println("No Corner accuracy " + accNoCorner / computedFor);

		//out.write("Preprocessing Time no Corner, Preprocessing Time with Corner, Storage Density, Storage no Corner, Storage with Corner," +
		//		  " Accuracy Density, Accuracy no Corner, Accuracy with Corner\r\n");
		try {
			out.write(preprocessingCenterOnlyTime + ", " + preprocessingWithCornersTime + ", " +
					storageOverheadDensity + ", " + storageOverheadNoCorners + ", " + storageOverheadWithCorners + ", " + 
					accDensity / computedFor + ", " + accNoCorner / computedFor + ", " + accCorner / computedFor + "\r\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static ArrayList<Tuple> readQPoints() {
		ArrayList<Tuple> allPoints = new ArrayList<Tuple>();
		String strLine = null;

		try {					
			FileInputStream fstream = new FileInputStream(txtDataPath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			int i = 0;
			while ((strLine = br.readLine()) != null) {
				String[] parts = strLine.split(",");
				Tuple dummyTuple = new Tuple();
				dummyTuple.location.yCoord = Double.parseDouble(parts[0]);
				dummyTuple.location.xCoord = Double.parseDouble(parts[1]);
				if (i++%27 == 0)
					allPoints.add(dummyTuple);
			}
			br.close();
		} catch (PatternSyntaxException p) {
			System.err.println("Split Error: " + strLine);
		}
		catch (NumberFormatException p) {
			System.err.println("Number Format Error: " + strLine);
		}
		catch (Exception e){
			System.err.println("Error: " + e.toString());
			System.err.println(strLine);					
		}
		return allPoints;
		//		ArrayList<Tuple> qPoints = new ArrayList<Tuple>();
		//		for (int i = 0; i < numPoints; i++) {
		//			Tuple q = allPoints.get((int)(Math.random() * qPoints.size()));
		//			qPoints.add(q);
		//		}
		//		return qPoints;
	}
}
