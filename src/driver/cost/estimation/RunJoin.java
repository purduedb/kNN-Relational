package driver.cost.estimation;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import optimizer.CatalogEntry;
import cost.estimate.Helper;
import cost.estimate.JoinEstimator;
import cost.estimate.VirtualGrid;
import index.QuadTree;
import data.Constants;
import data.DataReader;

public class RunJoin {

	static String binDataPath = "/Users/ahmed/Documents/MyWork/data/osm/reduced.binary";
	static String serializedTreePath = "/Users/ahmed/Documents/MyWork/data/osm/serialized_trees/";
	static String resultsPath = "/Users/ahmed/Documents/MyWork/Results/kNNCost/Join/";

	public static void main(String[] args) {
		//serializeAllTrees();

		// Preprocessing (time and storage overhead)
		//preprocessingAll(); // Vary Scale
		//preprocessingVirtualGridOnly(); // Vary Grid Size
		//preprocessingMergeOnly(); // Vary Sample Size

		// Query Time
		queryExecVaryK(); // All
		//queryExecVarySample(); // All but VG
		//queryExecVaryGridSize(); // VG

		// Accuracy
		//accuracyVarySampleSize(); // all but VG
		//accuracyVaryGridSize(); // VG only
	}

	private static void serializeAllTrees() {
		try {
			System.out.println("Serializing All Trees");
			for (int scale = 1; scale <= 10; scale++) {
				System.err.println("\nScale = " + scale);
				System.out.println("Loading data points");
				DataReader reader = new DataReader(binDataPath);
				QuadTree qTree = new QuadTree(Constants.getBounds(), 10000, 0);
				reader.readBinPointLocations(qTree, scale);			
				System.out.println("All points: " + qTree.numTuples);
				System.out.println("Skipped " + qTree.skipped);
				System.err.println("All data loaded successfuly");

				DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(serializedTreePath + scale + ".serialized")));
				qTree.serializeCountData(os);
				os.close();

				System.out.println("Done serialization");

				DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + scale + ".serialized")));
				QuadTree serialized = QuadTree.deserializeCountData(is);
				is.close();

				System.out.println("Done deserialization");

				if (QuadTree.validateSerialization(qTree, serialized))
					System.err.println("Identical");
				else
					System.err.println("Something went wrong");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void accuracyVarySampleSize() {
		try {
			// Inner Tree (fixed):
			int innerTreeScale = 10;
			DataInputStream innerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + innerTreeScale + ".serialized")));
			QuadTree inner = QuadTree.deserializeCountData(innerTreeInputStream);
			innerTreeInputStream.close();
			System.out.println("Done deserialization of inner Tree; scale = " + innerTreeScale);

			// Outer Tree (fixed):
			int outerTreeScale = 10;
			System.out.println("Loading data points");
			DataInputStream outerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + outerTreeScale + ".serialized")));
			QuadTree outer = QuadTree.deserializeCountData(outerTreeInputStream);
			outerTreeInputStream.close();
			System.out.println("Done deserialization of outer Tree; scale = " + outerTreeScale);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "accuracy_vary_sample_size.csv"));
			out.write("Sample Size, Accuracy Sampling, Accuracy Merge\r\n");
			out.flush();


			System.out.println("Vary Sample Size Experiment");
			int k = 10;
			double exactCost = JoinEstimator.actualCost(k, outer, inner);

			double accuracySampling = 0;
			double accuracyMerge = 0;
			int step = 50; int increment = 1;
			for (int sampleSize = 0; sampleSize <= 500; sampleSize+=increment) {
				if (sampleSize == 0)
					continue;
				if (sampleSize%step == 0) {
					System.err.println("\nsampleSize = " + sampleSize);	
					System.out.println("Accuracy (sampling) = " + accuracySampling / step * increment);
					System.out.println("Accuracy (merge) = " + accuracyMerge / step * increment);

					out.write(sampleSize + ", " + accuracySampling/step + ", " + accuracyMerge/step + "\r\n");
					out.flush();
					accuracySampling = 0;
					accuracyMerge = 0;
				}

				double samplingCost = JoinEstimator.estimateBySampling(k, sampleSize, outer, inner);
				ArrayList<CatalogEntry> mergeCatalog = JoinEstimator.getMergedCatalog(sampleSize, outer, inner);
				double mergeCost = Helper.searchInCatalog(mergeCatalog, k).numBlocks;

				if (samplingCost < exactCost)
					accuracySampling += samplingCost / exactCost;
				else
					accuracySampling += exactCost / samplingCost;	
				
				if (mergeCost < exactCost)
					accuracyMerge += mergeCost / exactCost;
				else
					accuracyMerge += exactCost / mergeCost;	
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void accuracyVaryGridSize() {
		try {
			// Inner Tree (fixed):
			int innerTreeScale = 1;
			DataInputStream innerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + innerTreeScale + ".serialized")));
			QuadTree inner = QuadTree.deserializeCountData(innerTreeInputStream);
			innerTreeInputStream.close();
			System.out.println("Done deserialization of inner Tree; scale = " + innerTreeScale);

			// Outer Tree (fixed):
			int outerTreeScale = 1;
			System.out.println("Loading data points");
			DataInputStream outerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + outerTreeScale + ".serialized")));
			QuadTree outer = QuadTree.deserializeCountData(outerTreeInputStream);
			outerTreeInputStream.close();
			System.out.println("Done deserialization of outer Tree; scale = " + outerTreeScale);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "accuracy_vary_grid_size.csv"));
			out.write("Grid Size, Accuracy Virtual Grid\r\n");
			out.flush();


			System.out.println("Vary Grid Size Experiment");
			
			int numQueries = 1000;
			double accuracyVG = 0;
			ArrayList<Integer> queries = new ArrayList<Integer>();
			ArrayList<Double> exactCosts = new ArrayList<Double>();
			for (int i = 0; i < numQueries; i++) {
				int k = (int)(Math.random() * Constants.maxK) + 1;
				queries.add(k);
				double exactCost = JoinEstimator.actualCost(k, outer, inner);
				exactCosts.add(exactCost);
			}
			
			for (int gridSize = 4; gridSize <= 20; gridSize++) {				
				VirtualGrid vg = new VirtualGrid(gridSize, gridSize, inner);
				
				accuracyVG = 0;
				for (int i = 0; i < numQueries; i++) {					
					double vgCost = vg.estimatekNNJoinCost(queries.get(i), outer);										
					
					if (vgCost < exactCosts.get(i))
						accuracyVG += vgCost / exactCosts.get(i);
					else
						accuracyVG += exactCosts.get(i) / vgCost;			
				}
				
				System.err.println("\ngrid Size = " + gridSize);	
				System.out.println("Accuracy (vg) = " + accuracyVG/numQueries);				

				out.write(gridSize + ", " + accuracyVG/numQueries + "\r\n");
				out.flush();
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void queryExecVarySample() {
		try {
			// Inner Tree (fixed):
			int innerTreeScale = 10;
			DataInputStream innerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + innerTreeScale + ".serialized")));
			QuadTree inner = QuadTree.deserializeCountData(innerTreeInputStream);
			innerTreeInputStream.close();
			System.out.println("Done deserialization of inner Tree; scale = " + innerTreeScale);

			// Outer Tree (fixed):
			int outerTreeScale = 10;
			System.out.println("Loading data points");
			DataInputStream outerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + outerTreeScale + ".serialized")));
			QuadTree outer = QuadTree.deserializeCountData(outerTreeInputStream);
			outerTreeInputStream.close();
			System.out.println("Done deserialization of outer Tree; scale = " + outerTreeScale);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "exec_time_vary_sample_size.csv"));
			out.write("Sample Size, Time Exact, Time Sampling, Time Merge\r\n");
			out.flush();

			System.out.println("Time - vary sample size Experiment");

			ArrayList<CatalogEntry> mergeCatalog = JoinEstimator.getMergedCatalog(outer, inner);
			
			long startTime, endTime;
			int repeat = 10;
			int k = 10;
			for (int sampleSize = 100; sampleSize <= 1000; sampleSize+=100) {
				System.err.println("\nsample size = " + sampleSize);	

				// Exact
				startTime = System.nanoTime();				
				for (int i = 0; i < repeat; i++) {
					JoinEstimator.actualCost(k, outer, inner);
				}
				endTime = System.nanoTime();
				double exactTime = (endTime - startTime) / 1000000000.0 / repeat;
				
				// Sampling
				startTime = System.nanoTime();				
				for (int i = 0; i < repeat; i++) {
					JoinEstimator.estimateBySampling(k, sampleSize, outer, inner);
				}
				endTime = System.nanoTime();
				double samplingTime = (endTime - startTime) / 1000000000.0 / repeat;
				
				// Merge
				startTime = System.nanoTime();				
				for (int i = 0; i < repeat; i++) {
					Helper.searchInCatalog(mergeCatalog, k);
				}
				endTime = System.nanoTime();
				double mergeTime = (endTime - startTime) / 1000000000.0 / repeat;

				
				System.out.println("Exact = " + exactTime);
				System.out.println("Sampling = " + samplingTime);
				System.out.println("Merge = " + mergeTime);
				

				out.write(sampleSize + ", " +  exactTime + ", " + samplingTime + ", " + mergeTime + "\r\n");
				out.flush();					
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void queryExecVaryGridSize() {
		try {
			// Inner Tree (fixed):
			int innerTreeScale = 10;
			DataInputStream innerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + innerTreeScale + ".serialized")));
			QuadTree inner = QuadTree.deserializeCountData(innerTreeInputStream);
			innerTreeInputStream.close();
			System.out.println("Done deserialization of inner Tree; scale = " + innerTreeScale);

			// Outer Tree (fixed):
			int outerTreeScale = 10;
			System.out.println("Loading data points");
			DataInputStream outerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + outerTreeScale + ".serialized")));
			QuadTree outer = QuadTree.deserializeCountData(outerTreeInputStream);
			outerTreeInputStream.close();
			System.out.println("Done deserialization of outer Tree; scale = " + outerTreeScale);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "exec_time_vary_grid_size.csv"));
			out.write("grid Size, Time Virtual Grid\r\n");
			out.flush();

			
			System.out.println("Time - vary grid size Experiment");			
			long startTime, endTime;
			
			int repeat = 1000;
			int k = 10;
			for (int gridSize = 4; gridSize <= 20; gridSize+=1) {
				System.err.println("\ngrid size = " + gridSize);	
				VirtualGrid vitualGrid = new VirtualGrid(gridSize, gridSize, inner);

				// VG
				startTime = System.nanoTime();				
				for (int i = 0; i < repeat; i++) {
					vitualGrid.estimatekNNJoinCost(k, outer);
				}
				endTime = System.nanoTime();
				double virtualGridTime = (endTime - startTime) / 1000000000.0 / repeat;				
				
				System.out.println("virtual Grid = " + virtualGridTime);
				
				out.write(gridSize + ", " + virtualGridTime + "\r\n");
				out.flush();					
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void queryExecVaryK() {
		try {
			// Inner Tree (fixed):
			int innerTreeScale = 10;
			DataInputStream innerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + innerTreeScale + ".serialized")));
			QuadTree inner = QuadTree.deserializeCountData(innerTreeInputStream);
			innerTreeInputStream.close();
			System.out.println("Done deserialization of inner Tree; scale = " + innerTreeScale);

			// Outer Tree (fixed):
			int outerTreeScale = 10;
			System.out.println("Loading data points");
			DataInputStream outerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + outerTreeScale + ".serialized")));
			QuadTree outer = QuadTree.deserializeCountData(outerTreeInputStream);
			outerTreeInputStream.close();
			System.out.println("Done deserialization of outer Tree; scale = " + outerTreeScale);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "exec_time_vary_k.csv"));
			out.write("k, Time Exact, Time Sampling, Time Merge, Time Virtual Grid\r\n");
			out.flush();

			System.out.println("Time Vary k Experiment");

			ArrayList<CatalogEntry> mergeCatalog = JoinEstimator.getMergedCatalog(outer, inner);
			VirtualGrid virtualGrid = new VirtualGrid(10, 10, inner);

			long startTime, endTime;
			int repeat = 10;
			for (int k = 2; k <= Constants.maxK; k*=2) {
				System.err.println("\nk = " + k);	

				// Exact
				startTime = System.nanoTime();				
				for (int i = 0; i < repeat; i++) {
					JoinEstimator.actualCost(k, outer, inner);
				}
				endTime = System.nanoTime();
				double exactTime = (endTime - startTime) / 1000000000.0 / repeat;
				
				// Sampling
				startTime = System.nanoTime();				
				for (int i = 0; i < repeat; i++) {
					JoinEstimator.estimateBySampling(k, 1000, outer, inner);
				}
				endTime = System.nanoTime();
				double samplingTime = (endTime - startTime) / 1000000000.0 / repeat;
				
				// Merge
				startTime = System.nanoTime();				
				for (int i = 0; i < repeat; i++) {
					Helper.searchInCatalog(mergeCatalog, k);
				}
				endTime = System.nanoTime();
				double mergeTime = (endTime - startTime) / 1000000000.0 / repeat;

				// Grid
				startTime = System.nanoTime();				
				for (int i = 0; i < repeat; i++) {
					virtualGrid.estimatekNNJoinCost(k, outer);
				}
				endTime = System.nanoTime();
				double vgTime = (endTime - startTime) / 1000000000.0 / repeat;

				
				System.out.println("Exact = " + exactTime);
				System.out.println("Sampling = " + samplingTime);
				System.out.println("Merge = " + mergeTime);
				System.out.println("Virtual Grid = " + vgTime);

				out.write(k + ", " +  exactTime + ", " + samplingTime + ", " + mergeTime + ", " + vgTime + "\r\n");
				out.flush();					
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void preprocessingAll() {
		try {
			// Inner Tree (fixed):
			int innerTreeScale = 5;
			DataInputStream innerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + innerTreeScale + ".serialized")));
			QuadTree inner = QuadTree.deserializeCountData(innerTreeInputStream);
			innerTreeInputStream.close();
			System.out.println("Done deserialization of inner Tree; scale = " + innerTreeScale);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "preprocessing_time_all.csv"));
			out.write("Outer Table Scale, Preprocessing Time Merge, Preprocessing Time Virtual Grid, Storage Overhead Merge, Storage Overhead Virtual Grid\r\n");
			out.flush();

			// Outer Tree:
			for (int outerScale = 1; outerScale <= 10; outerScale++) {
				System.out.println("Loading data points");
				DataInputStream outerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + outerScale + ".serialized")));
				QuadTree outer = QuadTree.deserializeCountData(outerTreeInputStream);
				outerTreeInputStream.close();
				System.out.println("Done deserialization of outer Tree; scale = " + outerScale);

				long startTime = 0;
				long endTime = 0;
				int repeat = 10;
				startTime = System.nanoTime();
				// Preprocessing Merge
				for (int i = 0; i < repeat; i++) {
					// n c 2
					JoinEstimator.getMergedCatalog(1000, outer, inner);
					//JoinEstimator.getMergedCatalogWithSampling(1000, inner, outer);					
				}
				endTime = System.nanoTime();
				double mergeTime = (endTime - startTime) / 1000000000.0 / repeat;
				System.out.println("Merge time " + mergeTime);

				// Preprocessing Virtual Grid
				startTime = System.nanoTime();
				new VirtualGrid(10, 10, inner);
				endTime = System.nanoTime();
				double vgTime = (endTime - startTime) / 1000000000.0;
				System.out.println("virtual grid time " + vgTime);
				
				int storageMerge = JoinEstimator.getMergedCatalog(1000, outer, inner).size();
				VirtualGrid vg = new VirtualGrid(10, 10, inner);
				int storageVG = vg.getStorageOverhead();
				
				out.write(outerScale + ", " + mergeTime + ", " + vgTime + ", " + storageMerge + " , " + storageVG + "\r\n");
				out.flush();					
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void preprocessingMergeOnly() {
		try {
			// Inner Tree (fixed):
			int innerTreeScale = 10;
			DataInputStream innerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + innerTreeScale + ".serialized")));
			QuadTree inner = QuadTree.deserializeCountData(innerTreeInputStream);
			innerTreeInputStream.close();
			System.out.println("Done deserialization of inner Tree; scale = " + innerTreeScale);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "preprocessing_time_merge_only.csv"));
			out.write("Sample Size, Preprocessing Time Merge\r\n");
			out.flush();

			// Outer Tree:
			int outerScale = 5;
			System.out.println("Loading data points");
			DataInputStream outerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + outerScale + ".serialized")));
			QuadTree outer = QuadTree.deserializeCountData(outerTreeInputStream);
			outerTreeInputStream.close();
			System.out.println("Done deserialization of outer Tree; scale = " + outerScale);
			for (int sampleSize = 100; sampleSize <= 1000; sampleSize+=100) {

				// Preprocessing Merge
				int repeat = 100;
				long startTime = System.nanoTime();
				for (int i = 0; i < repeat; i++) {
					// n c 2
					JoinEstimator.getMergedCatalog(sampleSize, outer, inner);
					//JoinEstimator.getMergedCatalogWithSampling(sampleSize, inner, outer);
				}
				long endTime = System.nanoTime();

				double mergeTime = (endTime - startTime) / 1000000000.0 / repeat;
				System.out.println("Merge time " + mergeTime);

				int storageMerge = JoinEstimator.getMergedCatalog(sampleSize, outer, inner).size();
				
				out.write(sampleSize + ", " + mergeTime + ", " + storageMerge + "\r\n");
				out.flush();					
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void preprocessingVirtualGridOnly() {
		try {
			// Inner Tree (fixed):
			int innerTreeScale = 10;
			DataInputStream innerTreeInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath  + innerTreeScale + ".serialized")));
			QuadTree inner = QuadTree.deserializeCountData(innerTreeInputStream);
			innerTreeInputStream.close();
			System.out.println("Done deserialization of inner Tree; scale = " + innerTreeScale);

			BufferedWriter out = new BufferedWriter(new FileWriter(resultsPath + "preprocessing_time_vg_only.csv"));
			out.write("Grid Size, Preprocessing Time Virtual Grid, Storage Overhead Virtual Grid\r\n");
			out.flush();

			for (int gridSize = 4; gridSize <= 20; gridSize++) {
				// Preprocessing VG
				int repeat = 10;
				long startTime = System.nanoTime();
				for (int i = 0; i < repeat; i++) {
				
					new VirtualGrid(gridSize, gridSize, inner);
				}
				long endTime = System.nanoTime();

				double vgTime = (endTime - startTime) / 1000000000.0 / repeat;
				System.out.println("VG time " + vgTime);
				
				VirtualGrid vg = new VirtualGrid(gridSize, gridSize, inner);
				int storageVG = vg.getStorageOverhead();
								
				out.write(gridSize + ", " + vgTime + ", " + storageVG + "\r\n");
				out.flush();					
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
