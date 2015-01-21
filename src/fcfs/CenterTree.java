
package fcfs;


import java.io.BufferedWriter;
import java.util.*;


public class CenterTree {
	
	String centerName;
	int maxCapacity = 9999;//18;//9999;	//DEFAULT MAX CAPACITY
	TreeSet<FlightIntervalNode> centerTraffic = new TreeSet<FlightIntervalNode>();
	TreeSet<AvailableWindowNode> centerAvailibilityBlocks = new TreeSet<AvailableWindowNode>();
	
	CenterTree(String name, int maxCap){
		centerName = name;
		maxCapacity = maxCap;
		centerAvailibilityBlocks.add(new AvailableWindowNode(0, Integer.MAX_VALUE));
	}
	
	CenterTree(String name){
		centerName = name;
		maxCapacity = 9999;
		centerAvailibilityBlocks.add(new AvailableWindowNode(0, Integer.MAX_VALUE));
	}
	
	//returns delay
	public int insertAtSoonestSlot(int enter, int exit){
		int soonest = getSoonestSlot(enter,exit);
		//duration
		insertFlight(soonest, soonest + (exit-enter));
		return soonest;
	}
	
	public int getSoonestSlot(int enter, int exit){
		AvailableWindowNode current = centerAvailibilityBlocks.floor(new AvailableWindowNode(enter));
		//System.out.println(enter + "**"+sectorName+"* "+current.open);
		if(current.open <= enter && exit <= current.close){
			return enter;
		}
		//int blockExit = Integer.MIN_VALUE;
		int duration = exit - enter;
		Iterator<AvailableWindowNode> it = centerAvailibilityBlocks.tailSet(current,false).iterator();
		while(it.hasNext()){
			current = it.next();
			if(current.close-current.open >= duration){
				return current.open;
			}
		}
		return 0;
	}
	
	private void insertFlight(int enter, int exit){
		FlightIntervalNode lower = centerTraffic.floor(new FlightIntervalNode(enter, 0));
		int newCapacity = lower==null?1:lower.capacity+1;
		FlightIntervalNode start = new FlightIntervalNode(enter,newCapacity);
		//add returns true if item doesn't already exist, or else doesn't add anything
		if(!centerTraffic.add(start)){
			centerTraffic.floor(start).capacity++;
		}

		//check for full capacity
		boolean blockOutInterval = false;
		int blockOutIntervalStart = Integer.MAX_VALUE;
		int blockOutIntervalEnd = Integer.MIN_VALUE;
		
		if(newCapacity == maxCapacity){
			blockOutInterval = true;
			blockOutIntervalStart = enter;
		}
		
		FlightIntervalNode current = centerTraffic.floor(start);
		Iterator<FlightIntervalNode> it = centerTraffic.tailSet(start,false).iterator();		
		
		U.Assert(current.capacity == newCapacity,"current.capacity == newCapacity");
		U.Assert(current.capacity <= maxCapacity,"current.capacity <= maxCapacity");
		
		int lastCapacity = current.capacity;
		
		while(it.hasNext()){
			current = it.next();
			if(current.time < exit){
				//System.out.print("nodes after "+ enter + " and before " + exit + " >> ");current.print();
				current.capacity++;
				if(blockOutInterval){
					if(current.capacity < maxCapacity){
						blockOutIntervalEnd = current.time;
						blockOutInterval = false;
						addIntervalToBlock(blockOutIntervalStart, blockOutIntervalEnd);
					}
				}
				if(current.capacity == maxCapacity){
					blockOutIntervalStart = current.time;
					blockOutInterval = true;
				}
				lastCapacity = current.capacity;
			}
		}
		FlightIntervalNode end = new FlightIntervalNode(exit, lastCapacity-1);
		centerTraffic.add(end);
		//update block
		if(blockOutInterval){
			blockOutInterval = false;
			addIntervalToBlock(blockOutIntervalStart, exit);
		}
	}
	
	//look at < signs!!!!!!!
	private void addIntervalToBlock(int start, int end){
		//returns block starting at or before interval
		AvailableWindowNode floor = centerAvailibilityBlocks.floor(new AvailableWindowNode(start));
		//interval contained by block
		Iterator<AvailableWindowNode> it = centerAvailibilityBlocks.tailSet(floor,true).iterator();
		ArrayList<AvailableWindowNode> nodesToRemove = new ArrayList<AvailableWindowNode>();
		AvailableWindowNode toAdd = null;
		//intervals
		//open/close represent the window that is available, start/end represent the interval to block out.
		while(it.hasNext()){
			//U.p(it.);
			//print();
			AvailableWindowNode current = it.next();
			if(current.open < end){
			//window opens before interval Ends	
				if(start < current.close){
				//interval starts before window Closes	
					if(current.open < start){
				    //window opens before interval starts  O < S	
						if(end < current.close){ //window closes after interval SPLIT
							//sectorAvailibilityBlocks.add(new AvailableWindowNode(end,current.close));
							toAdd = new AvailableWindowNode(end,current.close);
							current.close = start;							
						} else { //SHRINK open window end >= current.close e > c 
							current.close = start;
						}
					} else { 
					//interval starts before window opens S < O
						if(current.close < end){ //interval swallows window C < E
							nodesToRemove.add(current);
						} else {//SHRINK open window E < C
							current.open = end;
						}
					}
				} //window closes before interval starts, don't care				
			} // window opens after interval ends, don't care
		}
		if(toAdd!=null) {
			centerAvailibilityBlocks.add(toAdd);
		}
		for(AvailableWindowNode a : nodesToRemove) {
			centerAvailibilityBlocks.remove(a);
		}
	}
	
	public void print(){
		U.p(centerName + ":" + maxCapacity);
		printTraffic();
		printWindows();	
	}
	
	public void printWindows(){
		System.out.println("***Start Windows(" + centerAvailibilityBlocks.size() + ")");
		Iterator<AvailableWindowNode> it = centerAvailibilityBlocks.iterator();
		while(it.hasNext()){
			AvailableWindowNode cu = it.next();
			cu.print();
		}
		System.out.println("***End Windows");
	}
	
	public void printTraffic(){
		System.out.println("***Start Traffic(" + centerTraffic.size() + ")");
		Iterator<FlightIntervalNode> it = centerTraffic.iterator();
		while(it.hasNext()){
			FlightIntervalNode cu = it.next();
			cu.print();
		}
		System.out.println("***End Traffic");
	}
	
	public void printTrafficToFile(BufferedWriter out) {
		try {
			Iterator<FlightIntervalNode> it = centerTraffic.iterator();
			out.write(centerName + ",");
			while(it.hasNext()) {
				FlightIntervalNode cu = it.next();
				out.write(cu.time + ",");
			}
			out.write("\n");
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	/*
	public void printToFile(BufferedWriter out) {
		if (sectorTraffic.size() == 0) return;
		try {
			int time;
			int capacity;
			out.write(sectorName + "," + maxCapacity + "," + sectorTraffic.size() + ",");
			Iterator<FlightIntervalNode> it = sectorTraffic.iterator();
			while(it.hasNext()) {
				FlightIntervalNode cu = it.next();
				time = cu.getTime();
				capacity = cu.getCapacity();
				out.write(time + ":" + capacity + ",");
			}
			out.write("\n");
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printMaxCapacity(){
		Iterator<FlightIntervalNode> it = sectorTraffic.iterator();
		int max = 0;
		while(it.hasNext()){
			FlightIntervalNode cu = it.next();
			max = (cu.capacity > max)? cu.capacity: max;
		}
		U.p(sectorName + " cap: " + max + " | max cap: " + maxCapacity);
		if(max>maxCapacity){
			U.p("!!!!!!!!!!!!!!***********!!!!!!!ERROR MAX EXCEEDS CAPACITY");
			System.out.println(sectorName + " max cap: " + max + " : " + maxCapacity);
			U.Assert(max<=maxCapacity, sectorName + " max<=maxCapacity");
		}
	}
	*/
	public int getCapacity(int time){
		//time++;
		FlightIntervalNode f = centerTraffic.floor(new FlightIntervalNode(time, 0));
		return f==null?0:f.capacity;
	}
	
	class FlightIntervalNode implements Comparable<FlightIntervalNode>{
		int time = -999;
		int capacity = -999;
		//String nameOfFlight; need?
		FlightIntervalNode(int e){
			time = e;
		}
		FlightIntervalNode(int e, int c){
			U.Assert(c<=maxCapacity, centerName + ": " + c + " c<=maxCapacity " + maxCapacity); 
			time = e; 
			capacity = c;
		}
		void print(){ 
			System.out.printf("%d:%d\n", time, capacity);
		}
		//compares by entering time, if entering times are equals, returns -1(?)
		public int compareTo(FlightIntervalNode o ){
			return (time-o.time); //!= 0? time-o.time:-1;
		}
		public int getTime() {
			return time;
		}
		public int getCapacity() {
			return capacity;
		}
	}
	
	class AvailableWindowNode implements Comparable<AvailableWindowNode>{
		int open = -9999;
		int close = -999;
		AvailableWindowNode(int o){
			open = o;
		}
		AvailableWindowNode(int o, int c){
			open = o; 
			close = c;
		}
		void print(){ 
			System.out.printf("%d:%d\n", open, close);
		}
		//compares by entering time, if entering times are equals, returns -1(?)
		public int compareTo(AvailableWindowNode o ){
			return open - o.open;
		}
	}
	

}
