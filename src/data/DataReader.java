package data;

import index.BPTree;
import index.QuadTree;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;

public class DataReader {

	private String path;	
	private String separator = "\\|";

	public DataReader(String dataPath) {
		this.path = dataPath;
	}

	public void readOrdersIndexedByPriceAndCustID(BPTree<Double, ArrayList<Order>> orderBPTree, 
			HashMap<Integer, ArrayList<Order>> ordersHashMap) {

		String strLine = null;
		try {					
			FileInputStream fstream = new FileInputStream(path + "orders.tbl");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				Order order = readOrderLine(strLine);
				ArrayList<Order> bPTreeValues = orderBPTree.get(order.totalPrice);
				if (bPTreeValues == null) {
					bPTreeValues = new ArrayList<Order>();
					orderBPTree.put(order.totalPrice, bPTreeValues);
				}
				bPTreeValues.add(order);

				ArrayList<Order> hashValues = ordersHashMap.get(order.custKey);
				if (hashValues == null) {
					hashValues = new ArrayList<Order>();
					ordersHashMap.put(order.custKey, hashValues);
				}
				hashValues.add(order);
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
	}

	public void readOrdersIndexByCust(HashMap<Integer, ArrayList<Order>> hash) {

		String strLine = null;
		try {					
			FileInputStream fstream = new FileInputStream(path + "orders.tbl");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				Order order = readOrderLine(strLine);
				ArrayList<Order> values = hash.get(order.custKey);
				if (values == null) {
					values = new ArrayList<Order>();
					hash.put(order.custKey, values);
				}
				values.add(order);
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
	}

	public void readOrdersIndexByOrderKey(HashMap<Integer, Order> hash) {

		System.out.println("Reading Orders");
		int read = 0;
		String strLine = null;
		try {

			FileInputStream fstream = new FileInputStream(path + "orders.tbl");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				if (read++ % 1000000 == 0) {
					System.out.println("Finished reading:" + read / 1000000 + " million tuples");
					System.gc();
					long usedMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
					System.out.println("memory usage " + usedMB + " MB");
				}

				Order order = readOrderLine(strLine);

				hash.put(order.orderKey, order);					
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
	}

	public void readLineItems(BPTree<Integer, ArrayList<LineItem>> bpTree) {

		String strLine = null;
		try {					
			FileInputStream fstream = new FileInputStream(path + "lineitem.tbl");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				LineItem item = readLineItemLine(strLine);
				ArrayList<LineItem> values = bpTree.get(item.suppKey);
				if (values == null) {
					values = new ArrayList<LineItem>();
					bpTree.put(item.suppKey, values);
				}
				values.add(item);
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
	}

	public void readLineItems(HashMap<Integer, ArrayList<LineItem>> hashBySupplier, HashMap<Integer, ArrayList<LineItem>> hashByOrder) {

		int read  = 0;
		System.out.println("Reading Items");

		String strLine = null;
		try {

			FileInputStream fstream = new FileInputStream(path + "lineitem.tbl");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				if (read++ % 1000000 == 0) {
					System.out.println("Finished reading:" + read / 1000000 + " million tuples");
					//System.gc();
					long usedMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
					System.out.println("memory usage " + usedMB + " MB");
				}

				LineItem item = readLineItemLine(strLine);
				ArrayList<LineItem> itemsBySupplier = hashBySupplier.get(item.suppKey);
				if (itemsBySupplier == null) {
					itemsBySupplier = new ArrayList<LineItem>();
					hashBySupplier.put(item.suppKey, itemsBySupplier);
				}
				itemsBySupplier.add(item);

				ArrayList<LineItem> itemsByOrder = hashByOrder.get(item.orderKey);
				if (itemsByOrder == null) {
					itemsByOrder = new ArrayList<LineItem>();
					hashByOrder.put(item.orderKey, itemsByOrder);
				}
				itemsByOrder.add(item);
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
	}

	private Order readOrderLine(String strLine) {

		String[] lineParts = strLine.split(separator);

		Order order = new Order();
		try {
			order.orderKey = Integer.parseInt(lineParts[0]);
			order.custKey = Integer.parseInt(lineParts[1]);
			order.orderStatus = lineParts[2].charAt(0);
			order.totalPrice = Double.parseDouble(lineParts[3]);
			//Date date = new SimpleDateFormat("yyyy-M-d", Locale.ENGLISH).parse(lineParts[4]);
			//order.orderDate = date.getTime();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

		return order;
	}

	private LineItem readLineItemLine(String strLine) {

		String[] lineParts = strLine.split(separator);

		LineItem item = new LineItem();
		try {
			item.orderKey = Integer.parseInt(lineParts[0]);
			//item.partKey = Integer.parseInt(lineParts[1]);
			item.suppKey = Integer.parseInt(lineParts[2]);
			//item.quantity = Integer.parseInt(lineParts[4]);

			//Date date = new SimpleDateFormat("yyyy-M-d", Locale.ENGLISH).parse(lineParts[10]);
			//item.shipDate = date.getTime();

			Date date = new SimpleDateFormat("yyyy-M-d", Locale.ENGLISH).parse(lineParts[11]);
			item.commitDate = date.getTime();

			date = new SimpleDateFormat("yyyy-M-d", Locale.ENGLISH).parse(lineParts[12]);
			item.receiptDate = date.getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

		return item;
	}

	private Tuple readCustomerLine(String strLine) {

		String[] lineParts = strLine.split(separator);

		Customer customer = new Customer();
		customer.custKey = Integer.parseInt(lineParts[0]);

		customer.acctBalance = Double.parseDouble(lineParts[5]);

		String locationStr = lineParts[8];
		String[] locationParts = locationStr.split(",");
		customer.location.xCoord = Double.parseDouble(locationParts[0]);
		customer.location.yCoord = Double.parseDouble(locationParts[1]);

		return customer;
	}	

	private Tuple readSupplierLine(String strLine) {

		String[] lineParts = strLine.split(separator);

		Supplier supplier = new Supplier();
		supplier.suppKey = Integer.parseInt(lineParts[0]);

		supplier.acctBalance = Double.parseDouble(lineParts[5]);

		String locationStr = lineParts[7];
		String[] locationParts = locationStr.split(",");
		supplier.location.xCoord = Double.parseDouble(locationParts[0]);
		supplier.location.yCoord = Double.parseDouble(locationParts[1]);

		return supplier;
	}

	public void readIntoQTree (String fileName, QuadTree qTree) {
		String strLine = null;
		try {					
			FileInputStream fstream = new FileInputStream(path + fileName);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				if (fileName.contains("customer"))
					qTree.insert(readCustomerLine(strLine));
				else if (fileName.contains("supplier"))
					qTree.insert(readSupplierLine(strLine));
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
	}

	public void readCustomers(QuadTree qTree, BPTree<Double, ArrayList<Tuple>> bpTree) {
		String strLine = null;
		try {					
			FileInputStream fstream = new FileInputStream(path + "customer-augmented.tbl");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				Tuple tuple = readCustomerLine(strLine);
				qTree.insert(tuple);

				ArrayList<Tuple> tuples = null;
				tuples = bpTree.get(((Customer)tuple).acctBalance);
				if (tuples == null) {
					tuples = new ArrayList<Tuple>();
					bpTree.put(((Customer)tuple).acctBalance, tuples);
				}	

				tuples.add(tuple);
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
	}

	public void readCustomersIndexedByLocationAndCustID(QuadTree qTree, HashMap<Integer, Customer> hash) {
		String strLine = null;
		try {					
			FileInputStream fstream = new FileInputStream(path + "customer-augmented.tbl");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				Tuple tuple = readCustomerLine(strLine);
				qTree.insert(tuple);

				hash.put(((Customer)tuple).custKey, (Customer)tuple);
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
	}

	public void readCustomers(QuadTree qTree) {
		readIntoQTree("customer-augmented.tbl", qTree);
	}

	public void readSuppliers(ArrayList<Tuple> suppliers) {
		String strLine = null;
		try {					
			FileInputStream fstream = new FileInputStream(path + "supplier-augmented.tbl");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				Tuple tuple = readSupplierLine(strLine);
				suppliers.add(tuple);
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
	}

	public void readSuppliers(QuadTree qTree) {
		readIntoQTree("supplier-augmented.tbl", qTree);
	}

	public void readBinPointLocations(QuadTree qTree, int scale) {
		String strLine = null;

		try {
			DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
			int i = 0;
			while (is.available() > 0) {
				int positionInFile = i%10;
				if (positionInFile < scale) {
					Tuple dummyTuple = new Tuple();
					dummyTuple.location.yCoord = is.readInt();
					dummyTuple.location.xCoord = is.readInt();
					if(qTree.numTuples % 1000000 == 0)
						System.out.println(qTree.numTuples/1000000 + " millions read");
					qTree.insert(dummyTuple);
					i++;
				}
				else {
					is.skip(8 * (10 - positionInFile));
					i = 0;
				}
			}
			is.close();
		} 
		catch (Exception e){
			System.err.println("Error: " + e.toString());
			System.err.println(strLine);					
		}
	}

	public void readTxtPointLocations(QuadTree qTree) {
		String strLine = null;

		try {					
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((strLine = br.readLine()) != null) {
				String[] parts = strLine.split(",");
				Tuple dummyTuple = new Tuple();
				dummyTuple.location.yCoord = Double.parseDouble(parts[0]);
				dummyTuple.location.xCoord = Double.parseDouble(parts[1]);
				qTree.insert(dummyTuple);
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
	}
}
