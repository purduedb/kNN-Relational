package index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import optimizer.CatalogEntry;

import data.Tuple;

public class QuadTree {	

	public boolean processed;
	public boolean relationallyQualifies;

	// The MAX_OBJECTS
	public int maxNumObjects = 10000;
	
	public static int skipped = 0;

	// The objects list
	public ArrayList<Tuple> tuples;

	// The bounds of this tree
	public Rectangle bounds;

	public double distance; // could be min or max dist
	public double mDistance; // could be min or max dist

	// Branches of this tree, i.e., the quadrants
	public QuadTree[] subTrees;

	public boolean isLeaf;
	public int numTuples;
	public int depth;

	// Catalog information used by the optimizer
	public ArrayList<CatalogEntry> centerCatalog;
	public ArrayList<CatalogEntry> cornerCatalog;
	public ArrayList<CatalogEntry> localityCatalog;

	/**
	 * Construct a QuadTree with custom values. Used to create sub trees or branches
	 * @param l The level of this tree
	 * @param b The bounds of this tree
	 */
	public QuadTree(Rectangle b) {

		bounds = b;
		numTuples = 0;

		tuples = new ArrayList<Tuple>();
		subTrees = new QuadTree[4];
		isLeaf = true;
	}

	/**
	 * Construct a QuadTree with custom values. Used to create sub trees or branches
	 * @param l The level of this tree
	 * @param b The bounds of this tree
	 */
	public QuadTree(Rectangle b, int maxNumPoints, int parentDepth) {

		bounds = b;
		numTuples = 0;
		depth = parentDepth + 1;

		tuples = new ArrayList<Tuple>();
		subTrees = new QuadTree[4];
		isLeaf = true;
		this.maxNumObjects = maxNumPoints;
	}

	// Split the tree into 4 quadrants
	private void split() {

		isLeaf = false;
		double subWidth = bounds.width / 2.0;
		double subHeight = bounds.height / 2.0;
		double x = bounds.x;
		double y = bounds.y;
		subTrees[0] = new QuadTree(new Rectangle(x, y, subWidth, subHeight), maxNumObjects, depth);
		subTrees[1] = new QuadTree(new Rectangle(x + subWidth, y, subWidth, subHeight), maxNumObjects, depth);
		subTrees[2] = new QuadTree(new Rectangle(x, y + subHeight, subWidth, subHeight), maxNumObjects, depth);
		subTrees[3] = new QuadTree(new Rectangle(x + subWidth, y + subHeight, subWidth, subHeight), maxNumObjects, depth);
	}

	// Get the index of a tuple
	private int getIndex(Tuple.Location location){
		int index = -1;
		double verticalMidpoint = bounds.x + (bounds.width / 2);
		double horizontalMidpoint = bounds.y + (bounds.height / 2);
		boolean bottomQuadrant = (location.yCoord <= horizontalMidpoint) && (location.yCoord >= bounds.y);
		boolean topQuadrant = (location.yCoord > horizontalMidpoint) && (location.yCoord < (bounds.y + bounds.height));

		if (location.xCoord <= verticalMidpoint && location.xCoord >= bounds.x){
			if (bottomQuadrant){
				index = 0;
			} else if (topQuadrant){//
				index = 2;
			}
		} else if (location.xCoord > verticalMidpoint && location.xCoord < (bounds.x + bounds.width)){//
			if (bottomQuadrant){
				index = 1;
			} else if (topQuadrant){//
				index = 3;
			}
		}
//		if (index == -1) {
//			System.out.println("How?");
//			System.out.println(location.xCoord + "," + location.yCoord);
//		}
		return index;
	}

	public QuadTree searchEnclosingLeaf(Tuple.Location location) {
		QuadTree node = this;
		while (!node.isLeaf) {
			int index = node.getIndex(location);
			if (index < 0)
				return null;
			node = node.subTrees[index];			
		}
		return node;
	}


	/**
	 * Insert an object into this tree
	 */
	public void insert(Tuple t){
		numTuples++;

		if (!isLeaf){
			int index = getIndex(t.location);
			if (index!=-1){
				subTrees[index].insert(t);
				return;
			}
		}
		tuples.add(t);
		//if (tuples.size() > maxNumObjects && this.depth < 1000){
		if (tuples.size() > maxNumObjects){
			double mid = bounds.x + (bounds.width / 2);
			if (mid == bounds.x) return;				
			mid = bounds.y + (bounds.height / 2);
			if (mid == bounds.y) return;

			split();
			while (tuples.size() != 0) {
				Tuple next = tuples.remove(0);
				int index = getIndex(next.location);
				if (index < 0) {
					skipped++;
					continue;
				}
				subTrees[index].insert(next);
			}
		}
	}

	/**
	 * Insert an ArrayList of tuples into this tree
	 */
	public void insert(ArrayList<Tuple> tuples){
		for (Tuple t : tuples){
			insert(t);
		}
	}

	@Override
	public boolean equals(Object node) {
		if (((QuadTree) node).bounds.x != this.bounds.x)
			return false;
		if (((QuadTree) node).bounds.y != this.bounds.y)
			return false;
		if (((QuadTree) node).bounds.width != this.bounds.width)
			return false;
		if (((QuadTree) node).bounds.height != this.bounds.height)
			return false;
		return true;
	}

	public void clearFlags() {
		this.processed = false;
		this.relationallyQualifies = false;
		for (Tuple t : this.tuples) {
			t.processed = false;
			t.relationallyQualifies = false;
		}
		if (this.isLeaf)
			return;
		subTrees[0].clearFlags();
		subTrees[1].clearFlags();
		subTrees[2].clearFlags();
		subTrees[3].clearFlags();
	}

	public int getNumLeaves() {
		if (this.isLeaf)
			return 1;
		else
			return subTrees[0].getNumLeaves() + subTrees[1].getNumLeaves() + subTrees[2].getNumLeaves() + subTrees[3].getNumLeaves(); 
	}

	public boolean overlaps(Rectangle searchBounds) {

		double leftBorder =  searchBounds.x;
		double rightBorder =  searchBounds.x + searchBounds.width;
		if (rightBorder < this.bounds.x) {			
			return false;
		}
		if (leftBorder > (this.bounds.x + this.bounds.width)) {
			return false;
		}

		double bottomBorder =  searchBounds.y;
		double topBorder =  searchBounds.y + searchBounds.height;
		if (topBorder < this.bounds.y) {
			return false;
		}
		if (bottomBorder > (this.bounds.y + this.bounds.height)) {
			return false;
		}

		return true;
	}

	public void serializeCountData(DataOutputStream os) {
		try {
			os.writeBoolean(this.isLeaf);
			os.writeInt(this.numTuples);
			os.writeDouble(this.bounds.x);
			os.writeDouble(this.bounds.y);
			os.writeDouble(this.bounds.width);
			os.writeDouble(this.bounds.height);
			if (!this.isLeaf) {
				this.subTrees[0].serializeCountData(os);
				this.subTrees[1].serializeCountData(os);
				this.subTrees[2].serializeCountData(os);
				this.subTrees[3].serializeCountData(os);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static QuadTree deserializeCountData(DataInputStream is) {
		QuadTree tree = null;
		try {
			boolean isLeaf = is.readBoolean();
			int numTuples = is.readInt();
			Rectangle bounds = new Rectangle(is.readDouble(), is.readDouble(), is.readDouble(), is.readDouble());
			tree = new QuadTree(bounds);
			tree.isLeaf = isLeaf;
			tree.numTuples = numTuples;
			if (!isLeaf) {
				tree.subTrees = new QuadTree[4];
				tree.subTrees[0] = deserializeCountData(is);
				tree.subTrees[1] = deserializeCountData(is);
				tree.subTrees[2] = deserializeCountData(is);
				tree.subTrees[3] = deserializeCountData(is);
			}			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tree;
	}

	public static boolean validateSerialization(QuadTree original, QuadTree serialized) {
		if (original.isLeaf != serialized.isLeaf)
			return false;
		if (original.numTuples != serialized.numTuples)
			return false;
		if (original.bounds.x != serialized.bounds.x)
			return false;
		if (original.bounds.y != serialized.bounds.y)
			return false;
		if (original.bounds.width != serialized.bounds.width)
			return false;
		if (original.bounds.height != serialized.bounds.height)
			return false;

		if (!original.isLeaf) {
			if (!validateSerialization(original.subTrees[0], serialized.subTrees[0]))
				return false;
			if (!validateSerialization(original.subTrees[1], serialized.subTrees[1]))
				return false;
			if (!validateSerialization(original.subTrees[2], serialized.subTrees[2]))
				return false;
			if (!validateSerialization(original.subTrees[3], serialized.subTrees[3]))
				return false;
		}

		return true;
	}

	public static QuadTree readFromSerialized(String serializedTreePath) {
		DataInputStream is;
		QuadTree serializedTree = null;
		try {
			is = new DataInputStream(new BufferedInputStream(new FileInputStream(serializedTreePath)));
			serializedTree = QuadTree.deserializeCountData(is);
			is.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return serializedTree;
	}
}