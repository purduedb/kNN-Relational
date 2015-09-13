package driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import data.Tuple;

public class AugmentSpatialAttributes {

	private static double scale;
	private static ArrayList<Tuple> customerPoints;
	private static ArrayList<Tuple> supplierPoints;
	private static ArrayList<Tuple> allPoints;
	
	public static void main(String[] args) {
	
		allPoints = new ArrayList<Tuple>();
		readAll();
		
		for (int scale = 1; scale <= 10; scale++)
			augment(scale);
		
		System.out.println("Augmentation Complete");
	}

	private static void augment(int scale) {
		
		customerPoints = new ArrayList<Tuple>();
		supplierPoints = new ArrayList<Tuple>();

		for (int i = 0; i < scale * 150000; i++)
			customerPoints.add(allPoints.get((int)(Math.random() * allPoints.size())));
		
		for (int i = 0; i < scale * 10000; i++)
			supplierPoints.add(allPoints.get((int)(Math.random() * allPoints.size())));
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("/Users/ahmed/Documents/MyWork/data/TPC-H/"+scale+"/customer.tbl"));
			BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/ahmed/Documents/MyWork/data/TPC-H/"+scale+"/customer-augmented.tbl"));
			int i = 0;
			String line = br.readLine();
			while (line != null) {
				writer.write(line + customerPoints.get(i).location.xCoord + "," + customerPoints.get(i).location.yCoord + "|\r\n");
				i++;
				line = br.readLine();
			}
			br.close();
			writer.close();
			
			
			br = new BufferedReader(new FileReader("/Users/ahmed/Documents/MyWork/data/TPC-H/"+scale+"/supplier.tbl"));
			writer = new BufferedWriter(new FileWriter("/Users/ahmed/Documents/MyWork/data/TPC-H/"+scale+"/supplier-augmented.tbl"));
			i = 0;
			line = br.readLine();
			while (line != null) {
				writer.write(line + supplierPoints.get(i).location.xCoord + "," + supplierPoints.get(i).location.yCoord + "|\r\n");
				i++;
				line = br.readLine();
			}
			br.close();
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void readAll() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("/Users/ahmed/Documents/MyWork/data/osm/reduced.txt"));

			String line = br.readLine();
			while (line != null) {

				String[] parts = line.split(",");
				Tuple t = new Tuple();

				t.location.xCoord = Long.parseLong(parts[1]);
				t.location.yCoord = Long.parseLong(parts[0]);

				allPoints.add(t);

				line = br.readLine();
			}

			System.out.println("all read");

			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
