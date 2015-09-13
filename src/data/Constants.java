package data;

import index.Rectangle;

public class Constants {

	public static long minLat = -900000000;
	public static long minLong = -1800000000;
	public static long maxLat = 900000000;
	public static long maxLong = 1800000000;

	public static long worldWidth = Constants.maxLong-Constants.minLong;
	public static long worldHeight = Constants.maxLat-Constants.minLat;

	public static int maxK = 10000;
	
	public static Rectangle getBounds() {
		return new Rectangle(minLong, minLat, maxLong - minLong, maxLat - minLat);
	}
}
