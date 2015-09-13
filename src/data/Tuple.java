package data;

public class Tuple {

	public Location location;
	
	//public ArrayList<String> relAttributes;
	public double distance;
	
	public boolean processed;
	public boolean relationallyQualifies;
	
	public Tuple() {
		location = new Location();
	}
	
	public void setDistance(Location pointLocation) {
		double sumOfSquares = 0;
		sumOfSquares += Math.pow(this.location.xCoord - pointLocation.xCoord, 2);
		sumOfSquares += Math.pow(this.location.yCoord - pointLocation.yCoord, 2);				
		this.distance = Math.sqrt(sumOfSquares);
	}
	
	public class Location {
		
		public double xCoord;
		public double yCoord;
				
	}
	
}
