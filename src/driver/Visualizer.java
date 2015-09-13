package driver;

import index.QuadTree;
import index.Rectangle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JFrame;

import optimizer.CostEstimator;

import data.Constants;
import data.Tuple;


public class Visualizer extends JComponent {

	public static class Line{
		public final double x1; 
		public final double y1;
		public final double x2;
		public final double y2;   

		public Line(double x1, double y1, double x2, double y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}               
	}

	private static  ArrayList<Tuple> points = new ArrayList<Tuple>();
	private static  ArrayList<Line> lines = new ArrayList<Line>();

	private static Tuple q = new Tuple();
	private static double scale = 3;
	//private static double scale = 1.0/15;


	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(Color.BLACK);
		for (Tuple t : points) {
			g.fillOval((int)scaleX(t.location.xCoord), (int)scaleY(t.location.yCoord), 1, 1);
		}

		g.setColor(Color.RED);
		for (Line line : lines) {			
			g.drawLine((int)scaleX(line.x1), (int)scaleY(line.y1), (int)scaleX(line.x2), (int)scaleY(line.y2));
		}

		g.setColor(Color.BLUE);
		g.fillOval((int)scaleX(q.location.xCoord), (int)scaleY(q.location.yCoord), 10, 10);
	}

	private static int scaleX(double x) {
		return (int)((x - Constants.minLong) / (Constants.maxLong - Constants.minLong) * 1600);
	}

	private static int scaleY(double y) {
		return (int)(900 - ((y - Constants.minLat) / (Constants.maxLat - Constants.minLat) * 800));
	}

	private static void showMap(JComponent component) {

		component.repaint();
	}

	public static void main(String[] args) {
		double mapScale = 100000000;
		q.location.xCoord = 3.7529 * mapScale;
		q.location.yCoord = 5.5766 * mapScale;

		JFrame testFrame = new JFrame();
		testFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		final Visualizer comp = new Visualizer();
		comp.setPreferredSize(new Dimension(1650, 950));
		testFrame.getContentPane().add(comp, BorderLayout.CENTER);

		//readPoints();
		readBinPoints();

		addQTreeLines();

		showMap(comp);

		testFrame.pack();
		testFrame.setVisible(true);
	}

	private static void readPoints() {

		try {
			BufferedReader br = new BufferedReader(new FileReader("/Users/ahmed/Documents/MyWork/data/osm/reduced.txt"));

			ArrayList<Tuple> temp = new ArrayList<Tuple>();

			String line = br.readLine();
			while (line != null) {

				String[] parts = line.split(",");
				Tuple t = new Tuple();

				t.location.xCoord = Long.parseLong(parts[1]);
				t.location.yCoord = Long.parseLong(parts[0]);

				temp.add(t);

				line = br.readLine();
			}
			if (temp.size() > scale * 150000)
				for (int i = 0; i < scale * 150000; i++) {
					points.add(temp.get((int)(Math.random() * temp.size())));
				}
			else
				points = temp;


			System.out.println("all read");


			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void readBinPoints() {
		ArrayList<Tuple> temp = new ArrayList<Tuple>();
		try {
			String path = "/Users/ahmed/Documents/MyWork/data/osm/reduced.binary";
			DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));

			System.out.println("reading");
			while (is.available() > 0) {
				Tuple dummyTuple = new Tuple();
				dummyTuple.location.yCoord = is.readInt();
				dummyTuple.location.xCoord = is.readInt();
				temp.add(dummyTuple);
				if (temp.size()%1000000 == 0)
					System.out.println(temp.size() / 1000000 + " millions processed");
				if (temp.size()/1000000 > 1)
					break;
			}

			is.close();
			System.out.println("all read");
			
			if (temp.size() > scale * 100000)
				for (int i = 0; i < scale * 100000; i++) {
					points.add(temp.get((int)(Math.random() * temp.size())));
				}
			else
				points = temp;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void addQTreeLines() {
		Rectangle R = new Rectangle(Constants.minLong, Constants.minLat, Constants.maxLong-Constants.minLong, Constants.maxLat-Constants.minLat);
		//QuadTree tree = new QuadTree(R, 10);
		QuadTree tree = new QuadTree(R, 1000, 0);
		tree.insert(points);

		System.out.println("Quad Tree created");


		ArrayList<QuadTree> queue = new ArrayList<QuadTree>();
		queue.add(tree);

		while (!queue.isEmpty()) {
			QuadTree node = queue.remove(0);

			if (node.isLeaf) {
				lines.add(new Line(node.bounds.x, node.bounds.y, node.bounds.x, node.bounds.y + node.bounds.height)); // left |
				lines.add(new Line(node.bounds.x, node.bounds.y, node.bounds.x + node.bounds.width, node.bounds.y)); // bottom -
				lines.add(new Line(node.bounds.x, node.bounds.y + node.bounds.height, node.bounds.x + node.bounds.width, node.bounds.y + node.bounds.height)); // top -
				lines.add(new Line(node.bounds.x + node.bounds.width, node.bounds.y, node.bounds.x + node.bounds.width, node.bounds.y + node.bounds.height)); // Right |				 
			}
			else {
				queue.add(node.subTrees[0]);
				queue.add(node.subTrees[1]);
				queue.add(node.subTrees[2]);
				queue.add(node.subTrees[3]);
			}
		}

		//kNNCostEstimator.estimatekNNCost(tree);
		QuadTree enclosingNode = tree.searchEnclosingLeaf(q.location);
		System.out.println(q.location.xCoord + ", " + q.location.yCoord);
		System.out.println(enclosingNode.numTuples);
		System.out.println((enclosingNode.bounds.x + enclosingNode.bounds.width/2) + ", " + (enclosingNode.bounds.y + enclosingNode.bounds.height/2));

		CostEstimator.estimate(enclosingNode, tree);
		CostEstimator.printCatalogInformationCondensed(tree.searchEnclosingLeaf(q.location));
		//kNNCostEstimator.printCatalogInformation(tree.searchEnclosingLeaf(q.location));

	}


}
