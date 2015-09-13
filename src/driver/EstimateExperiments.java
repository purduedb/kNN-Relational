package driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import optimizer.CatalogEntry;
import optimizer.CostEstimator;
import index.QuadTree;
import index.Rectangle;
import data.Constants;
import data.DataReader;
import exec.Common;

public class EstimateExperiments {

	private static String path = "//Users//ahmed//Desktop//kNNRelational Experimental Results//final//Cost Estimates//";			

	private static Rectangle bounds;
	private static ArrayList<Integer> ks = new ArrayList<Integer>();


	public static void main(String[] args) {		
		bounds = new Rectangle(Constants.minLong, Constants.minLat, Constants.maxLong-Constants.minLong, Constants.maxLat-Constants.minLat);

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "accuracyANDtime.csv"));
			out.write("sample size, Accuracy, Execution Time\r\n");
			out.flush();

			int scaleFactor = 5;
			QuadTree outerTree = new QuadTree(bounds, 1000, 0);
			QuadTree innerTree = new QuadTree(bounds, 1000, 0);

			System.out.println("Reading Data");
			String path = "//Users//ahmed//Documents//MyWork//data//TPC-H//" + scaleFactor + "//";
			DataReader reader = new DataReader(path);

			reader.readCustomers(innerTree);
			reader.readSuppliers(outerTree);
			System.out.println("Outer Leaves: " + Common.getNumLeafs(outerTree));
			System.out.println("Inner Leaves: " + Common.getNumLeafs(innerTree));

			ArrayList<CatalogEntry> exactCatalog = CostEstimator.estimatekNNJoinCost(outerTree, innerTree);

			Random r = new Random();
			int numKs = 1000;
			for (int i = 0; i < numKs; i++) {
				ks.add(r.nextInt(100000));
			}

			double execTime = 0; int numRuns = 10;
			for (int i = 2; i <= 20; i+=1) {
				System.err.println("Sample size = " + i);
				double accuracy = 0;
				long startTime = System.nanoTime();
				for (int run = 1; run <= numRuns; run++) {
					ArrayList<CatalogEntry> approxCatalog = CostEstimator.estimatekNNJoinCost(i, outerTree, innerTree);
					accuracy += compare(exactCatalog, approxCatalog);
				}
				long endTime = System.nanoTime();
				execTime = (endTime - startTime) / 1000000000.0;				
				System.err.println(accuracy/numRuns);
				out.write(i + "," + accuracy/numRuns + "," + execTime/numRuns + "\r\n");
				out.flush();
			}

			out.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static double compare(ArrayList<CatalogEntry> exactCatalog, ArrayList<CatalogEntry> approxCatalog) {
		double accAccuracy = 0;		
		for (int k : ks) {
			CatalogEntry exact = CostEstimator.searchInCatalog(exactCatalog, k);
			CatalogEntry approx = CostEstimator.searchInCatalog(approxCatalog, k);
			double accuracy;
			if (approx.numBlocks > 2 * exact.numBlocks)
				accuracy = 0;
			else
				accuracy = (1 - Math.abs(exact.numBlocks - approx.numBlocks)/(double)exact.numBlocks);

			accAccuracy += accuracy;
			if (accAccuracy < 0)
				System.out.println("How?");
		}
		System.out.println("Accuracy = " + accAccuracy/ks.size());
		return accAccuracy/ks.size();
	}

}
