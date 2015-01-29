package fcfs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.TreeSet;

/*
 * This is an older version of the AirportTree scheduling that was based on Integers. The most recent version is based on the Flight object and is Airport.java and AirportTree.java.
 * Documentation on scheduling is found there.
 */

public class AirportsInt {
	
	Hashtable<String, AirportTreeInt> airportList= new Hashtable<String, AirportTreeInt>();
	
	public AirportTreeInt getAirport(String airportName){
		AirportTreeInt a = airportList.get(airportName);
		if(a == null){
			a = new AirportTreeInt(airportName, this);
			airportList.put(airportName, a);
		}
		return a;
	}
	
	//older, scheduling by int, Integer based
	public int scheduleArrivalInt(String airportName, int arrTime){
		AirportTreeInt a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime, arrTime);
	}
	//older
	public int scheduleArrivalInt(String airportName, int arrTime, int schArrTime){
		AirportTreeInt a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime, schArrTime);
	}
	//older
	public int scheduleDepartureInt(String airportName, int depTime, int schDepTime){
		AirportTreeInt a = getAirport(airportName);
		return a.insertAtSoonestDeparture(depTime, schDepTime);
	}
	//older 
	public int scheduleArrivalDepricated(String airportName, int arrTime, int schArrTime, Flight f) {
		AirportTreeInt a = getAirport(airportName);
		return a.insertAtSoonestArrivalDepricated(arrTime, schArrTime, f);
	}

	
	//older
	//See airportTree class
	public boolean removeFlightFromArrivalQueue(String airportName, int arrivalTime){
		AirportTreeInt a = getAirport(airportName);
		return a.freeArrivalSlot(arrivalTime);
	}
	
	//This method the older, int based way of managing slots
	//Huu
	public int getSoonestArrivalInt(String airportName, int arrivalTime){
		AirportTreeInt a = getAirport(airportName);
		return a.getSoonestArrivalSlot(arrivalTime);
	}
	
	//older
	public int getSoonestDepartureInt(String airportName, int arrivalTime){
		AirportTreeInt a = getAirport(airportName);
		return a.getSoonestArrivalSlot(arrivalTime);
	}
	
	//See airportTree class
	//older int based
	public void validateDepartureTraffic(){
		for (AirportTreeInt at: airportList.values()) {
			at.validateDepartureTraffic();
		}
	}
	//See airportTree class
	//older int based
	public void validateArrivalTraffic() {
		for (AirportTreeInt at: airportList.values()) {
			at.validateArrivalTraffic();
		}
	}
	
	public void printAirportsToFile(BufferedWriter cap, BufferedWriter dep, BufferedWriter schedDep, BufferedWriter arr, BufferedWriter schedArr) {
		for (AirportTreeInt a : airportList.values()){
			try{
				a.printToFile(cap,dep,schedDep,arr, schedArr);
			}catch (Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}
			
	}
	
	//See airportTree class
	public void printMinSpacing(){ 
		for (AirportTreeInt a : airportList.values()){ //io.println("");
			a.printMinSpacing();
		}
	}
	
	//See airportTree class
	public void offsetCapacities(int offset){ 
		for (AirportTreeInt a : airportList.values()){ //io.println("");
			a.offsetCapacities(offset);
		}
	}
	
	public void validate(){ 
		for (AirportTreeInt a : airportList.values()){ 
			a.validate();
		}
	}
	
	//See airportTree class
	//Empties schedule of all slots but retains loaded capacity information.
	public void resetToStart(){ 
		for (AirportTreeInt a : airportList.values()){
			a.resetToStart();
		}
	}
	
	//See airportTree class
	public boolean effectedByCFR(Flight f){
		return getAirport(f.arrivalAirport).effectedByCFR(f.arrivalTimeACES);
	}
	
	//Loads hourly capacity rates from ACES/ASPM input data.
		public void loadCapacitiesFromAces(String filePath){
			String[] subs = new String[1];
			try{
				//Read ACES Transit Time File Line by Line
				FileInputStream fstream = new FileInputStream(filePath);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String line;
				String name = "noexist";
				AirportTreeInt f = new AirportTreeInt(name, this);
				while ((line = br.readLine()) != null){
					line = line.trim();
					// location(0), sim day(1), hour(2), quarter(3), rates(4)
					subs = line.split(",");
					//io.printf("%s:%s\n",name, subs[0]);
					if( subs.length == 5 && !line.startsWith("*")){
						
						if (!name.equals(subs[0])){
							if(!name.equals("noexist")){airportList.put(name, f);}
							name = subs[0];
							f = new AirportTreeInt(name, this); 
						}
						//make list of capacities.
						int day = Integer.parseInt(subs[1]) - 1;
						int hour = Integer.parseInt(subs[2]);
						int quarterHour = Integer.parseInt(subs[3])-1;
						int timeInMills = (((day*24 + hour)*60 + quarterHour*15)*60*1000);// - (3*3600000); //converts all to milliseconds  
						String[] subss = subs[4].split("_");
						int adr = Integer.parseInt(subss[2]);
						int aar = Integer.parseInt(subss[4]);
						f.airportCapacities.add(new CapacityByTime(timeInMills, adr, aar));

					} else {
						U.p("not 6 " + line);
					} 
				}
				in.close();

			}catch (Exception e){
				U.p(subs[0]);
				System.err.println("airport load Error: " + e.getMessage());
				e.printStackTrace();
			}

		} 
		
		public void loadDelays(String path, String mode){
			try{
				FileInputStream fstream = new FileInputStream(path);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String line;
				String[] subs = new String[1];
				String airportName;
				while ((line = br.readLine()) != null){
					line = line.replaceAll("\\s","");//trim();
					subs = line.split(",");
					airportName = "K"+subs[0];
					AirportTreeInt a = airportList.get(airportName);
					if(a == null){
						a = new AirportTreeInt(airportName, this);
						airportList.put(airportName, a);
					}
					
					if(subs.length == 4){
						double zeroProbability = Double.parseDouble(subs[1]);
						double mean = Double.parseDouble(subs[2]);
						double std = Double.parseDouble(subs[3]);
						if(mode.equals("gate")){a.gateMean = mean; a.gateZeroProbablity = zeroProbability; a.gateStd = std;}
						if(mode.equals("taxi")){a.taxiMean = mean; a.taxiZeroProbablity = zeroProbability; a.taxiStd = std;}
					} else {
						a.taxiUnimpeded = Double.parseDouble(subs[1]);
					}
				}
				in.close();

			}catch (Exception e){
				System.err.println("airport Delay load Error: " + e.getMessage());
				e.printStackTrace();
			}

		}
}


/************************************************************************/
/////////////////////////////////AirportTreeInt///////////////////////////
/************************************************************************/


class AirportTreeInt {
	
	String airportName;
	
	AirportsInt airports; //needed?
	
	TreeSet<CapacityByTime> airportCapacities = new TreeSet<CapacityByTime>();

	//Earlier implementation storing arrivals/departures as ints representing time.
	TreeSet<Integer> airportArrivalTraffic = new TreeSet<Integer>();
	TreeSet<Integer> airportDepartureTraffic = new TreeSet<Integer>();

	//Huu's implementation
	TreeSet<Integer> scheduledAirportArrivalTraffic = new TreeSet<Integer>(); 
	TreeSet<Integer> scheduledAirportDepartureTraffic = new TreeSet<Integer>();
	
	double taxiUnimpeded = AirportTree.DEFAULT_UNIMPEDED_TAXI_TIME;//10 from aces 8.0;
	//these values are derived from work done by Gano, and are used generate stochastic gate and taxi time delays used in monte carlo simulation. See Delay Sensitivity TM by Gano Chatterji, Kee Palopo and Noam Almog.
	double gateMean = 0, gateZeroProbablity = 1, gateStd = 0, taxiMean = 0, 
		   taxiZeroProbablity = 1, taxiStd = 0; 
	
	int CFRstart = 0, CFRend = 0;
	
	public AirportTreeInt(String name, AirportsInt as){
		airportName = name;
		airports = as;
	}	
	
	public void resetToStart(){
		airportArrivalTraffic = new TreeSet<Integer>();	
		airportDepartureTraffic = new TreeSet<Integer>();

	}
	
	public void offsetCapacities(int offset){
		if(airportCapacities!=null){
			for(CapacityByTime c: airportCapacities){
				c.time += offset;
			}
		}
	}
	
	public boolean effectedByCFR(int time){
		return CFRstart <= time && time < CFRend; 
	}
	
	
	public int getDepartureSpacing(int time){
		CapacityByTime c = airportCapacities.floor(new CapacityByTime(time));
		int adr = c != null? c.adr: AirportTree.DEFAULT_DEP_RATE;//3600000; //30;//9999;
		return 60*60*1000 / adr; 
		//check this
		//return 10;
	}
	
	//returns spacing in milleseconds i.e AAR 120 / hr = 30000 ms / arrival
	public int getArrivalSpacing(int time){
		CapacityByTime c = airportCapacities.floor(new CapacityByTime(time));
		int aar = c != null? c.aar: AirportTree.DEFAULT_ARR_RATE; //3600000;//30;//9999;
		return 60*60*1000 / aar;
		
	}

	//older method for integer based queue
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

	////older method for integer based queue
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

	//older method for integer based queue
	//schedules flight at soonest time on or later than input parameter, returns time
	public int insertAtSoonestArrival(int arrival){
		int soonest = getSoonestArrivalSlot(arrival);
		airportArrivalTraffic.add(soonest);
		return soonest;
	}

	//older method for integer based queue
	public int insertAtSoonestArrival(int arrival, int scheduledArrival){
		int soonest = getSoonestArrivalSlot(arrival);
		airportArrivalTraffic.add(soonest);
		scheduledAirportArrivalTraffic.add(scheduledArrival);
		return soonest;
	}

	//older method for integer based queue
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

	//older method for integer based queue
	public int insertAtSoonestDeparture(int departure, int scheduledDeparture){
		int soonest = getSoonestDepartureSlot(departure);
		airportDepartureTraffic.add(soonest);
		scheduledAirportDepartureTraffic.add(scheduledDeparture + (int)(Math.random()*1000));
		return soonest;
	}

	//older method for integer based queue
	public int insertAtSoonestArrivalDepricated(int arrival, int scheduledArrival, Flight f) {
		int soonest = getSoonestArrivalSlot(arrival);
		airportArrivalTraffic.add(soonest);
		scheduledAirportArrivalTraffic.add(scheduledArrival);
		//scheduledArrivalTrafficByFlight.add(f);
		return soonest;
	}

	public void insertCapacity(CapacityByTime c){
		airportCapacities.add(c);
	}
	
	

	public void printDepTraffic(){
		U.p("***Start DEP(" + airportDepartureTraffic.size() + ")");
		for(int c: airportDepartureTraffic){
			System.out.println(c);
		}
		U.p("***End deps");
	}

	public void printScheduledDepTraffic(){
		U.p("***Start ScheduledDEP(" + scheduledAirportDepartureTraffic.size() + ")");
		for(int c: scheduledAirportDepartureTraffic){
			System.out.println(c);
		}
		U.p("***End scheduled deps");
	}

	public void printArrTraffic(){
		U.p("***Start ARR(" + airportArrivalTraffic.size() + ")");
		for(int c: airportArrivalTraffic){
			System.out.println(c);
		}
		U.p("***End arrs");
	}

	public void printScheduledArrTraffic() {
		U.p("***Start ScheduledARR(" + scheduledAirportArrivalTraffic.size() + ")");
		for(int c: scheduledAirportArrivalTraffic) {
			System.out.println(c);
		}
		U.p("***End scheduled arrs");
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
	
	
	public void printCaps(){
		U.p("***Start caps(" + airportCapacities.size() + ")");
		for(CapacityByTime cu: airportCapacities){
			cu.print();
		}
		U.p("***End caps");
	}
	
	public void print(){
		U.p(airportName);
		printCaps();
		U.p("Dep Traffic (Integer): " + airportDepartureTraffic.size());
		U.p("Arr Traffic (Integer): " + airportArrivalTraffic.size());
		printArrTraffic();
		printDepTraffic();
		U.p("");
	}
	
	public void printCapsToFile(BufferedWriter out) {
		try {
			if(airportCapacities.isEmpty()) {
				out.write(airportName +",");
				out.write("0" + "," + AirportTree.DEFAULT_DEP_RATE +"," + AirportTree.DEFAULT_ARR_RATE);
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
	
	public void printMinSpacing(){
		printMinDepartureSpacing();
		printMinArrivalSpacing();	
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
			U.Assert(minSpacing < getDepartureSpacing(timeOfMin), airportName + " min < getDepartureSpacing(time)");
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
			U.Assert(minSpacing < getArrivalSpacing(timeOfMin), "min < getArrivalSpacing(time)");
		}
		if(minSpacing == getArrivalSpacing(timeOfMin))
		System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
		
	}
	
	//older method for integer based queue
		public void validateDepartureTraffic() {
			Integer minSpacing = Integer.MAX_VALUE;
			Integer lastTime = Short.MIN_VALUE*10;
			Integer timeOfMin = 0;

			for(Integer currentTime: airportDepartureTraffic){				
				//REAL VALIDATION
				int lastTimeSpace = currentTime-lastTime;
				//System.out.println("lastTimeSpace= " + lastTimeSpace + " arrivalSpacing= " + getDepartureSpacing(lastTime));
				U.Assert(lastTimeSpace >= getDepartureSpacing(lastTime),lastTimeSpace + " DEP lastTimeSpace >= getArrivalSpacing(lastTime) " + getDepartureSpacing(lastTime));
				
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
				U.Assert(minSpacing < getDepartureSpacing(timeOfMin), airportName + " min < getDepartureSpacing(time)");
			}
			if(minSpacing == getDepartureSpacing(timeOfMin)){
			//System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			}
		}
		
		//older method for integer based queue
		public void validateArrivalTraffic() {
			Integer minSpacing = Integer.MAX_VALUE;
			Integer lastTime = Short.MIN_VALUE*10;
			Integer timeOfMin = 0;
			for(Integer currentTime: airportArrivalTraffic){
				
				//REAL VALIDATION
				int lastTimeSpace = currentTime-lastTime;
				//System.out.println("lastTimeSpace= " + lastTimeSpace + " arrivalSpacing= " + getArrivalSpacing(lastTime));
				U.Assert(lastTimeSpace >= getArrivalSpacing(lastTime),lastTimeSpace + 
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
				U.Assert(minSpacing < getArrivalSpacing(timeOfMin), "min < getArrivalSpacing(time)");
			}
			if(minSpacing == getArrivalSpacing(timeOfMin)){
				//System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			}
		}
		
		//validates that all flights meet capacity constraint spacing and no repeat flight ID's in schedule
		public void validate(){
			validateDepartureTraffic(); //older int based
			validateArrivalTraffic(); //older int based
		}
}