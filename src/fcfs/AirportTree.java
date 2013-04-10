package fcfs;

import java.io.BufferedWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
	
//time in millisec
class CapacityByTime implements Comparable<CapacityByTime>{
	//adr actual departure rate 
	//aar actual arrival rate
	int time = -1; 
	int adr = -1; 
	int aar = -1;
	CapacityByTime(int t){ time = t;}
	CapacityByTime(int t, int d, int a){
		time = t; 
		adr = d; 
		aar = a;
	}
	
	public int compareTo(CapacityByTime o ){ 
		return (time-o.time); 
	} //!= 0? time-o.time:-1;
	
	void print(){ 
		System.out.printf("	%d: adr %d aar %d\n", time, adr, aar);
	}
}


//compare the first value of the TreeSet
class arrivalTrafficCMPcomparator implements Comparator<TreeSet<Integer>> {
	public int compare(TreeSet<Integer> ts1, TreeSet<Integer> ts2) {
		return (ts1.first() - ts2.first());
	}
}


public class AirportTree {
	//final static int DEFAULT_ARR_DEP_RATE = 30;//3600000; // 1 per 2 minute, or 1 per 1 millisec (unconstrained)
	final static int DEFAULT_ARR_DEP_RATE = 31;
	final static int DEFAULT_ARR_RATE = 31;
	final static int DEFAULT_DEP_RATE = 31;
	
	final static int DEFAULT_UNIMPEDED_TAXI_TIME = 5;//(min) from Gano
	String airportName;
	PrintStream io = System.out;
	
	TreeSet<Integer> airportArrivalTraffic = new TreeSet<Integer>();
	TreeSet<Integer> scheduledAirportArrivalTraffic = new TreeSet<Integer>();
	
	TreeSet<Integer> airportDepartureTraffic = new TreeSet<Integer>();
	TreeSet<Integer> scheduledAirportDepartureTraffic = new TreeSet<Integer>();
	
	TreeSet<Flight> arrivalTrafficByFlight = new TreeSet<Flight>(new flightFinalArrTimeComparator());	

	
	private boolean departureContract = true;
	
	TreeSet<CapacityByTime> airportCapacities = new TreeSet<CapacityByTime>(); //aspm data
	double gateMean = 0, gateZeroProbablity = 1, gateStd = 0, taxiMean = 0, 
		   taxiZeroProbablity = 1, taxiStd = 0, taxiUnimpeded = DEFAULT_UNIMPEDED_TAXI_TIME;//10 from aces 8.0;	

	public void setDepartureContract(boolean b){
		departureContract = false;
	}
	
	public void offsetCapacities(int offset){
		if(airportCapacities!=null){
			for(CapacityByTime c: airportCapacities){
				c.time += offset;
			}
		}
	}
	
	public AirportTree(String name){
		airportName = name;
	}
	
	public void resetToStart(){
		airportArrivalTraffic = new TreeSet<Integer>();
		
		airportDepartureTraffic = new TreeSet<Integer>();
		arrivalTrafficByFlight = new TreeSet<Flight>(new flightFinalArrTimeComparator());
		departureContract = true;
	}
	
	
	
	//find first gap, fill it, 
	//then push all following flights forward, 
	//filling in the gaps
	//until the next flight's spacing won't be effected.
	//previous and next assume model of times on a number line (previous = earlier)
	
	// **** the following note on this same line is not true anymore **** CALLED AT FIRST SCHEDULING WHEN FLIGHT IS ON THE GROUND(??)
	// No freeze horizon consideration in this version; i.e., for example, flight within a second of touchdown can still be delayed further in the air
	public void insertAtSoonestArrivalWithForwardGapsRemoved(Flight flight, int proposedArrivalTime, int currentTime){ 
		//this being empty does mean flight has not already been scheduled
		if(arrivalTrafficByFlight.isEmpty()){
			//no delay, because no other arrivals at this point
			if(flight.arrivalFirstSlot == -1)flight.arrivalFirstSlot = proposedArrivalTime;
			flight.arrivalTimeFinal = proposedArrivalTime;
			arrivalTrafficByFlight.add(flight);			
		//other flights already in the arrival queue
		} else {
			if(flight.id == 45151){
				assert(flight.id==45151);
			}
			int proposedSlotTime = proposedArrivalTime;
			Flight previousFlight;
			Integer previousArrival;//arrival time in ms 
			Integer previousSpace; //time spacing for particular arrival time (in ms) (to prevent tail gating)
			boolean cont = true;
			while(cont){
				previousFlight = arrivalTrafficByFlight.floor(Flight.dummyArrival(proposedSlotTime));
				previousArrival = previousFlight != null? previousFlight.arrivalTimeFinal : Integer.MIN_VALUE;
				//nextFlight = arrivalTrafficByFlight.higher(Flight.dummyArrival(proposedArrivalTime));
				//nextArrival = nextFlight!=null? nextFlight.arrivalTimeFinal : Integer.MAX_VALUE;
				previousSpace = getArrivalSpacing(previousArrival);
				//previous flight is far enough back. //there's a gap!
				if( previousArrival + previousSpace <= proposedSlotTime){
					int additionalDelay = proposedSlotTime-proposedArrivalTime;
					//first time being scheduled, all delay goes to ground
					if(flight.arrivalFirstSlot == -1){
						flight.arrivalFirstSlot = proposedSlotTime;
						Main.Assert(flight.atcGroundDelay == 0, "flight.departureDelayFromArrivalAirport == 0 shouldn't be ground delay yet");
						flight.atcGroundDelay = additionalDelay; // add ground delay
					} else {
						if(flight.id == 45084){
							assert(flight.id==45084);
						}
						//flight is being re-scheduled
						if(currentTime <= flight.wheelsOffTime ){
							//all flights here are current time == wheels off time
							//except jiggle where wheels off time is pushed forward sometimes.
							if(departureContract) {
								//only add to air to maintain departure time contract
								flight.atcAirDelay += additionalDelay; 
							} else {
//								the following two go together to allow contract changes; and either these two are active or the next line is active at a time
								flight.atcGroundDelay += additionalDelay; // add ground delay
								flight.wheelsOffTime += additionalDelay;
								flight.additionalGDfromReschedule += additionalDelay;
							}
						} else {
							//scenario does not include rescheduling in the air, 
							System.out.println( "error scheduling in the air in airport tree");
							//current time is after wheels off
							//flight.atcAirDelay += proposedSlotTime-proposedArrivalTime;
						}
					}
					flight.arrivalTimeFinal = proposedSlotTime;
					Main.Assert(!arrivalTrafficByFlight.contains(flight), "arrivalTrafficByFlight.contains(flight) flight not already in arrival tree");
					arrivalTrafficByFlight.add(flight); // should not bee added a secondtime
					
					mendTheGapsGoingForward(flight, currentTime);
					// mend the gaps!
					cont = false;
				} else {
					proposedSlotTime = previousArrival + previousSpace;
				}
			}	
		}
	}
	
	//pushes all flights later than f forward to make room for f
	//only pushes forward if necessary
	public void mendTheGapsGoingForward(Flight f, int currentTime){
		//includes flight just added above
		Iterator<Flight> iter = arrivalTrafficByFlight.tailSet(f).iterator();
		//starts on flight just added
		Flight current = iter.next();
		boolean cont = true;
		Flight next;
		ArrayList<Flight> addBackIn = new ArrayList<Flight>();
		
		//pushes flights forward, filling in gaps as moves forward
		while (iter.hasNext() && cont){
			next = iter.next();
			int currentSpacing = getArrivalSpacing(current.arrivalTimeFinal);
			
			//Main.p("current arrival time " + current.arrivalTimeFinal);Main.p("current spacing " + currentSpacing);Main.p("next arrival time " + next.arrivalTimeFinal);
			
			//if next flight is too close
			if(current.arrivalTimeFinal + currentSpacing > next.arrivalTimeFinal){
				
				iter.remove(); // iter is on next so next is removed
				Main.Assert(currentTime <= next.arrivalTimeFinal, next.airline + " " +currentTime+ " " + next.departureTimeProposed +
						" currentTime >= f.arrivalTimeFinal not scheduling flights that have landed ");
				//int arrivalDelay = getArrivalSpacing(current.arrivalTimeFinal);
				int delayNeeded = current.arrivalTimeFinal + currentSpacing - next.arrivalTimeFinal;			
				//do delays
				//flight is still on the ground
				if(currentTime <= next.wheelsOffTime ){

					if(departureContract){
						//only add to air to maintain departure time contract
						next.atcAirDelay += delayNeeded;
					}else{
						// next flight gets air delay so that we can keep departures constant
						// the following two go together to allow contract changes; and either these two are active or the next line is active at a time
						next.atcGroundDelay += delayNeeded; //all further delay taken in air
						next.wheelsOffTime += delayNeeded; //all further delay taken in air						
					}
				} else {
					next.atcAirDelay += delayNeeded;
				}
				//new arrival time is soonest time after current arrival with it's spacing
				
				//keep track of jiggles
				if(delayNeeded != 0){
					next.totalJiggleAmount += delayNeeded;
					next.numberOfJiggles++;
				}
				
				next.arrivalTimeFinal = current.arrivalTimeFinal + currentSpacing;
				addBackIn.add(next);
				current = next;
				currentSpacing = getArrivalSpacing(current.arrivalTimeFinal);
				//Main.p("next arrival time after" + next.arrivalTimeFinal);
			} else {
				cont = false;
			}
		}
		arrivalTrafficByFlight.addAll(addBackIn); //Sort by ground or air??
	}
	
	//returns false if this arrival time was not in list
	public boolean freeArrivalSlot(Flight f){
		if(arrivalTrafficByFlight.remove(f)){
			return true;
		} else {
			throw new java.lang.Error("FAILED: flight not removed, not found in arrival queue");
			
		}
		
	}
	
	//IMPLEMENT
	public void validateByFlight(){
		TreeSet<Integer> noRepeats = new TreeSet<Integer>();
		
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;
		Integer currentTime;
		for(Flight cf: arrivalTrafficByFlight){
			//asserts no flights in traffic twice
			Main.Assert(noRepeats.contains(cf.id),"dup id's in arrival traffic!!!! " + cf.id);
			noRepeats.add(cf.id);
			
			currentTime = cf.arrivalTimeFinal;
			int lastTimeSpace = currentTime-lastTime; //space between last arrival and this arrival
			Main.Assert(lastTimeSpace >= getArrivalSpacing(lastTime), "lastTimeSpace >= getArrivalSpacing(lastTime)");
			
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			}
			lastTime = currentTime;
		}
		if(minSpacing < getArrivalSpacing(timeOfMin)){
			System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printArrTraffic();
			Main.Assert(minSpacing < getArrivalSpacing(timeOfMin), "min < getArrivalSpacing(time)");
		}
		minSpacing = Integer.MAX_VALUE;
		lastTime = Short.MIN_VALUE*10;
		timeOfMin = 0;
	}
	
	
	public void testGaps(){
		airportCapacities.add(new CapacityByTime(0, -1, 30*60*1000));
		airportCapacities.add(new CapacityByTime(8, -1, 10*60*1000));
		System.out.println("getArrivalCapacity(1): " + getArrivalSpacing(1));
		System.out.println("getArrivalCapacity(8): " + getArrivalSpacing(8));
		/*
		arrivalTrafficByFlight.add(Flight.dummyArrival(1));
		arrivalTrafficByFlight.add(Flight.dummyArrival(4));
		arrivalTrafficByFlight.add(Flight.dummyArrival(6));
		arrivalTrafficByFlight.add(Flight.dummyArrival(8));
		arrivalTrafficByFlight.add(Flight.dummyArrival(12));
		arrivalTrafficByFlight.add(Flight.dummyArrival(14));
		//*/
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),1,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),6,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),14,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),4,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),12,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),8,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),3,1);
		printArrTrafficGapless();
	}	
	
	public void printArrTrafficGapless(){
		io.println("***Start ARR(" + arrivalTrafficByFlight.size() + ")");
		for(Flight c: arrivalTrafficByFlight){
			System.out.println(c.arrivalTimeFinal);
		}
		io.println("***End arrs");
	}
	
	//returns spacing in milleseconds i.e AAR 120 / hr = 30000 ms / arrival
	public int getArrivalSpacing(int time){
		CapacityByTime c = airportCapacities.floor(new CapacityByTime(time));
		int aar = c != null? c.aar: DEFAULT_ARR_RATE; //3600000;//30;//9999;
		return 60*60*1000 / aar;
		
	}
	
	public int getDepartureSpacing(int time){
		CapacityByTime c = airportCapacities.floor(new CapacityByTime(time));
		int adr = c != null? c.adr: DEFAULT_DEP_RATE;//3600000; //30;//9999;
		return 60*60*1000 / adr;
	}
	
	public int getSoonestDepartureSlot(int departureTime){
		Integer before, previousSpace, currentSpace, after;
		while(true){
			before = airportDepartureTraffic.floor(departureTime);
			before = before != null? before : Integer.MIN_VALUE;
			after = airportDepartureTraffic.ceiling(departureTime);
			after = after!=null? after : Integer.MAX_VALUE;
			//spacing based on adr rates. i.e. 30 dep per hour means 2min space between flights.
			currentSpace = getDepartureSpacing(departureTime);
			previousSpace = getDepartureSpacing(before);
			//ensures departure time will be spaced out between any two flights.
			if( before + previousSpace <= departureTime && departureTime + currentSpace <= after){
				//System.out.printf(" before: %d after: %d ",before, after);
				return departureTime;
			}
			//first tries to depart at the space after last flight.
			if(before + previousSpace > departureTime){
				departureTime = before + previousSpace;
			//otherwise at space after next flight
			} else {
				departureTime = after + getDepartureSpacing(after);
			}
		}
	}
	
	//works the same as departures
	public int getSoonestArrivalSlot(int arrivalTime){
		Integer before, previousSpace, currentSpace, after; 
		while(true){
			before = airportArrivalTraffic.floor(arrivalTime);
			before = before != null? before : Integer.MIN_VALUE;
			after = airportArrivalTraffic.ceiling(arrivalTime);
			after = after!=null? after : Integer.MAX_VALUE;
			currentSpace = getArrivalSpacing(arrivalTime);
			previousSpace = getArrivalSpacing(before);
			if( before + previousSpace <= arrivalTime && arrivalTime + currentSpace <= after){
				//System.out.printf(" before: %d after: %d ",before, after);
				return arrivalTime;
			}
			if(before + previousSpace > arrivalTime){
				arrivalTime = before + previousSpace; 
			} 
			
			else {
				arrivalTime = after + getArrivalSpacing(after); 
			}
		}
	}
	
	public int getSoonestArrivalSlot(int arrivalTime, int minArrivalTime, int maxArrivalTime){
		Integer before, previousSpace, currentSpace, after; 
		while(true){
			before = airportArrivalTraffic.floor(arrivalTime);
			before = before != null? before : Integer.MIN_VALUE;
			after = airportArrivalTraffic.ceiling(arrivalTime);
			after = after!=null? after : Integer.MAX_VALUE;
			currentSpace = getArrivalSpacing(arrivalTime);
			previousSpace = getArrivalSpacing(before);
			if( before + previousSpace <= arrivalTime && arrivalTime + currentSpace <= after){
				//System.out.printf(" before: %d after: %d ",before, after);
				return arrivalTime;
			}
			if(before + previousSpace > arrivalTime){
				arrivalTime = before + previousSpace; 
				
				//System.out.println("arrivalTime " + arrivalTime);
			} 
			else {
				arrivalTime = after + getArrivalSpacing(after); 
			}
		}
	}
	
	
	//schedules flight at soonest time on or later than input parameter, returns time
	public int insertAtSoonestArrival(int arrival){
		int soonest = getSoonestArrivalSlot(arrival);
		airportArrivalTraffic.add(soonest);
		return soonest;
	}
	
	public int insertAtSoonestArrival(int arrival, int scheduledArrival){
		int soonest = getSoonestArrivalSlot(arrival);
		airportArrivalTraffic.add(soonest);
		scheduledAirportArrivalTraffic.add(scheduledArrival);
		return soonest;
	}
	
	//returns false if this arrival time was not in list
	public boolean freeArrivalSlot(int arrivalTimeToRemove){
		int closest = airportArrivalTraffic.floor(arrivalTimeToRemove);
		if(closest == arrivalTimeToRemove){
			airportArrivalTraffic.remove(arrivalTimeToRemove);
			return true;
		} else {
			throw new java.lang.Error("FAILED: flight not removed, not found in arrival queue");
			
		}
		
	}
	
	/*
	public int insertAtSoonestDeparture(int departure){
		int soonest = getSoonestDepartureSlot(departure);
		airportDepartureTraffic.add(soonest);
		return soonest;
	}*/
	
	public int insertAtSoonestDeparture(int departure, int scheduledDeparture){
		int soonest = getSoonestDepartureSlot(departure);
		airportDepartureTraffic.add(soonest);
		scheduledAirportDepartureTraffic.add(scheduledDeparture + (int)(Math.random()*1000));
		return soonest;
	}
	
	public void insertCapacity(CapacityByTime c){
		airportCapacities.add(c);
	}
	
	public void printDepTraffic(){
		io.println("***Start DEP(" + airportDepartureTraffic.size() + ")");
		for(int c: airportDepartureTraffic){
			System.out.println(c);
		}
		io.println("***End deps");
	}
	
	public void printScheduledDepTraffic(){
		io.println("***Start ScheduledDEP(" + scheduledAirportDepartureTraffic.size() + ")");
		for(int c: scheduledAirportDepartureTraffic){
			System.out.println(c);
		}
		io.println("***End scheduled deps");
	}
	
	public void printCaps(){
		io.println("***Start caps(" + airportCapacities.size() + ")");
		for(CapacityByTime cu: airportCapacities){
			cu.print();
		}
		io.println("***End caps");
	}
	
	public void printCapsToFile(BufferedWriter out) {
		try {
			if(airportCapacities.isEmpty()) {
				out.write(airportName +",");
				out.write("0" + "," + DEFAULT_DEP_RATE +"," + DEFAULT_ARR_RATE);
				out.write("\n");
			}
			else {
				for(CapacityByTime cu: airportCapacities) {
					out.write(airportName+",");
					out.write(cu.time + "," + cu.adr + "," + cu.aar);
					out.write("\n");
				}
			}
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printArrTraffic(){
		io.println("***Start ARR(" + airportArrivalTraffic.size() + ")");
		for(int c: airportArrivalTraffic){
			System.out.println(c);
		}
		io.println("***End arrs");
	}
	
	public void printScheduledArrTraffic() {
		io.println("***Start ScheduledARR(" + scheduledAirportArrivalTraffic.size() + ")");
		for(int c: scheduledAirportArrivalTraffic) {
			System.out.println(c);
		}
		io.println("***End scheduled arrs");
	}
	
	public void printDepTrafficToFile(BufferedWriter dep, BufferedWriter schedDep) {
		try{
			dep.write(airportName + ",");
			for(int c: airportDepartureTraffic) {
				dep.write(c +",");
			}
			dep.write("\n");
			
			schedDep.write(airportName + ",");
			for(int d: scheduledAirportDepartureTraffic) {
				schedDep.write(d+",");
			}
			schedDep.write("\n");
			
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printArrTrafficToFile(BufferedWriter arr, BufferedWriter schedArr) {
		try{
			arr.write(airportName + ",");
			for(int c: airportArrivalTraffic) {
				arr.write(c +",");
			}
			arr.write("\n");
			
			schedArr.write(airportName +",");
			for(int c: scheduledAirportArrivalTraffic) {
				schedArr.write(c+",");
			}
			schedArr.write("\n");
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printDelayVars(){
		io.printf("Gate: mean: %.3f zero: %.3f std: %.3f\n", gateMean, gateZeroProbablity, gateStd);
		io.printf("Taxi: mean: %.3f zero: %.3f std: %.3f taxi unimpeded: %.3f\n", taxiMean, taxiZeroProbablity, taxiStd, taxiUnimpeded);
	}
	
	public void print(){
		io.println(airportName);
		io.println("Arr Traffic: " + airportArrivalTraffic.size());
		io.println("Dep Traffic: " + airportDepartureTraffic.size());
		printDelayVars();
		printCaps();
		printArrTraffic();
		printDepTraffic();
		io.println("");
	}
	
	public void print(String airport) {
		if(airportName.equals(airport)) {
			io.println(airportName);
			io.println("Arr Traffic: " + airportArrivalTraffic.size());
			io.println("Dep Traffic: " + airportDepartureTraffic.size());
			printDelayVars();
			printCaps();
			printArrTraffic();
			printScheduledArrTraffic();
			printDepTraffic();
			printScheduledDepTraffic();
			io.println("");
		}
	}
	
	public void printToFile(BufferedWriter cap, BufferedWriter dep, BufferedWriter schedDep, BufferedWriter arr, BufferedWriter schedArr) {
		try{
			printCapsToFile(cap);
			if(!airportArrivalTraffic.isEmpty() && !scheduledAirportArrivalTraffic.isEmpty()) {
				printArrTrafficToFile(arr, schedArr);
			}
			if(!airportDepartureTraffic.isEmpty() && !scheduledAirportDepartureTraffic.isEmpty()) {
				printDepTrafficToFile(dep, schedDep);
			}
			
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	
	public void printMinDepartureSpacing() {
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;

		for(Integer currentTime: airportDepartureTraffic){
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			} 
			lastTime = currentTime;
		}
		if(minSpacing < getDepartureSpacing(timeOfMin)){
			System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printDepTraffic();
			Main.Assert(minSpacing < getDepartureSpacing(timeOfMin), airportName + " min < getDepartureSpacing(time)");
		}
		if(minSpacing == getDepartureSpacing(timeOfMin))
		System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
	}
	
	public void printMinArrivalSpacing() {
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;

		for(Integer currentTime: airportArrivalTraffic){
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			}
			lastTime = currentTime;
		}
		if(minSpacing < getArrivalSpacing(timeOfMin)){
			System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printArrTraffic();
			Main.Assert(minSpacing < getArrivalSpacing(timeOfMin), "min < getArrivalSpacing(time)");
		}
		if(minSpacing == getArrivalSpacing(timeOfMin))
		System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
		
	}
	
	public void printMinSpacing(){
		printMinDepartureSpacing();
		printMinArrivalSpacing();	
	}
	
	public void validateDepartureTraffic() {
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;

		for(Integer currentTime: airportDepartureTraffic){				
			//REAL VALIDATION
			int lastTimeSpace = currentTime-lastTime;
			//System.out.println("lastTimeSpace= " + lastTimeSpace + " arrivalSpacing= " + getDepartureSpacing(lastTime));
			Main.Assert(lastTimeSpace >= getDepartureSpacing(lastTime),lastTimeSpace + " DEP lastTimeSpace >= getArrivalSpacing(lastTime) " + getDepartureSpacing(lastTime));
			
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			} 
			lastTime = currentTime;
		}
		if(minSpacing < getDepartureSpacing(timeOfMin)){
			System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printDepTraffic();
			Main.Assert(minSpacing < getDepartureSpacing(timeOfMin), airportName + " min < getDepartureSpacing(time)");
		}
		if(minSpacing == getDepartureSpacing(timeOfMin)){
		//System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
		}
	}
	
	public void validateArrivalTraffic() {
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;
		for(Integer currentTime: airportArrivalTraffic){
			
			//REAL VALIDATION
			int lastTimeSpace = currentTime-lastTime;
			//System.out.println("lastTimeSpace= " + lastTimeSpace + " arrivalSpacing= " + getArrivalSpacing(lastTime));
			Main.Assert(lastTimeSpace >= getArrivalSpacing(lastTime),lastTimeSpace + 
					" ARR lastTimeSpace >= getArrivalSpacing(lastTime) " + getArrivalSpacing(lastTime));
			
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			}
			lastTime = currentTime;
		}
		if(minSpacing < getArrivalSpacing(timeOfMin)){
			System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printArrTraffic();
			Main.Assert(minSpacing < getArrivalSpacing(timeOfMin), "min < getArrivalSpacing(time)");
		}
		if(minSpacing == getArrivalSpacing(timeOfMin)){
			//System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
		}
	}
	
	public void validate(){
		validateDepartureTraffic();
		validateArrivalTraffic();
	}
	
	public void test(){
		airportCapacities.add(new CapacityByTime(0, 30*60*1000, 10*60*1000));
		airportArrivalTraffic.add(1);
		airportArrivalTraffic.add(5);
		airportArrivalTraffic.add(7);
		System.out.println("getArrivalCapacity(9): " + getArrivalSpacing(9));
		for(int n = 0; n < 13; n++){
		//
		System.out.println("getSoonetArrivalSlot(" + n + "): " + getSoonestArrivalSlot(n));
		}
		
		
	}	
}




