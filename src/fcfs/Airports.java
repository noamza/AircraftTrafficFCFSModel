package fcfs;

/**
 * @author Noam Almog
 *
 */
import java.io.*;
import java.util.*;

/*
 * This class holds the collection of airports being scheduled.
 * It also loads the data for the airports from input.
 * Essentially it is a container class for AirportTree objects which hold the bulk of the scheduling logic for airport departure/arrivals.
 * This class has get/set methods for the airports and loading from input data.
 * 
 */
public class Airports {

	PrintStream io = System.out;
	//creates hash of AirportTrees.
	Hashtable<String, AirportTree> airportList= new Hashtable<String, AirportTree>();
	
	//this method loads delay data for each airports. 
	//This data is used for generating stochastic delay for departure times for Montecarlo simulation. 
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
				AirportTree a = airportList.get(airportName);
				if(a == null){
					a = new AirportTree(airportName, this);
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
			AirportTree f = new AirportTree(name, this);
			while ((line = br.readLine()) != null){
				line = line.trim();
				// location(0), sim day(1), hour(2), quarter(3), rates(4)
				subs = line.split(",");
				//io.printf("%s:%s\n",name, subs[0]);
				if( subs.length == 5 && !line.startsWith("*")){
					
					if (!name.equals(subs[0])){
						if(!name.equals("noexist")){airportList.put(name, f);}
						name = subs[0];
						f = new AirportTree(name, this); 
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
					//c.print();
					//io.println(airportList.size());
					//io.printf("name: %s time: %d adr: %d aar: %d\n",name, timeInMills, adr, aar); 
					//f.actualRates.get(f.actualRates.size()-1).print();

				} else {
					io.println("not 6 " + line);
				} 
			}
			in.close();

		}catch (Exception e){
			io.println(subs[0]);
			System.err.println("airport load Error: " + e.getMessage());
			e.printStackTrace();
		}

	}
	
	//NOTE: the documentation for the following methods can be found in the AirportTree class.
	
	//Schedules all departures first come first served ensuring capacity constraints
	//current time refers to the time this scheduling is taking place in simulation time (can be before/after departure etc..)
	//returns delay assigned by scheduling
	//NOTE:this handles delay in a custom way: delayNeeded is always the amount of delay required for scheduling into current queue but how that delay is distributed is custom and might need to be modified.
	public int scheduleDeparture(Flight f, int proposedDepartureTime, int currentTime){
		//returns delay from proposedDepartureTime, millisec always positive
		return getAirport(f.departureAirport).scheduleDeparture(f, proposedDepartureTime, currentTime);
	}
	
	//schedules a priority slot at current time (fcfs only to other priority flights)
	//current time refers to the time this scheduling is taking place in simulation time (can be before/after departure etc..)
	//returns delay assigned by scheduling
	//NOTE:this handles delay in a custom way: delayNeeded is always the amount of delay required for scheduling into current queue but how that delay is distributed is custom and might need to be modified.
	public int schedulePriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		//returns delay from proposedDepartureTime, millisec always positive
		return getAirport(f.departureAirport).schedulePriorityDeparture(f, proposedDepartureTime, currentTime);
	}
	
	//Schedules all arrivals first come first served ensuring capacity constraints
	//current time refers to the time this scheduling is taking place in simulation time (can be before/after departure etc..)
	//returns delay assigned by scheduling
	//NOTE:method handles delay distribution in a custom way. delay returned is always the amount of delay required for scheduling into current queue but how that delay is distributed is custom in AirportTree and might need to be modified.
	//NOTE: WHEN CALLED BEFORE FLIGHT HAS DEPARTED (currentTime < f.departureTimeFinal) WILL MODIFY f.departureTimeFinal AND DISTRIBUTE DELAY IN CUSTOM WAY
	public int scheduleArrival(Flight f, int proposedArrivalTime, int currentTime){
		//returns delay from proposedDepartureTime, millisec always positive
		return getAirport(f.arrivalAirport).scheduleArrival(f, proposedArrivalTime, currentTime);
	}
	
	//older method of scheduling with Jiggling, see coupled scheduling paper.
	//finds first gap between slots and inserts slot, adjusting later slots, makes a more compact schedule.
	//See airport tree
	//see coupled sensitivity paper
	public void scheduleArrivalCompact(Flight flight, int proposedArrivalTime, int currentTime){
		AirportTree a = getAirport(flight.arrivalAirport);
		a.insertAtSoonestGapArrival(flight, proposedArrivalTime, currentTime);
	}
	
	
	//See airportTree class
	//returns soonest available slot on a first come first served basis
	public int getSoonestDepartureSlot(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).getSoonestFirstComeFirstServedSlot(f, proposedDepartureTime, true);
	}
	//See airportTree class
	//returns soonest available slot that's not occupied by a priority flight.
	public int getSoonestPriorityDepartureSlot(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).getSoonestPriorityDepartureSlot(f, proposedDepartureTime);
	}
	
	//See airportTree class
	//Huu
	public boolean removeFlightFromArrivalQueue(Flight f){
		AirportTree a = getAirport(f.arrivalAirport);
		return a.freeArrivalSlot(f);
	}
	
	//See airportTree class
	//Huu
	public boolean removeFlightFromDepartureQueue(Flight f){
			AirportTree a = getAirport(f.departureAirport);
			return a.freeDepartureSlot(f);
	}
	
	//See airportTree class
	//Empties schedule of all slots but retains loaded capacity information.
	public void resetToStart(){ 
		for (AirportTree a : airportList.values()){
			a.resetToStart();
		}
	}
	
	
	/* older methods
	public int scheduleArrival(String airportName, int arrTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime);
	}
	//See airportTree class
	public int scheduleDeparture(String airportName, int depTime, int schDepTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestDeparture(depTime, schDepTime);
	}
	//See airportTree class
	public int scheduleArrival(String airportName, int arrTime, int schArrTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime, schArrTime);
	}
	//See airportTree class
	public int scheduleArrival(String airportName, int arrTime, int schArrTime, Flight f) {
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime, schArrTime, f);
	}
	//See airportTree class
	public int getSoonestDeparture(String airportName, int departureTime){
		AirportTree a = getAirport(airportName);
		return a.getSoonestDepartureSlot(departureTime);
	}
	
	*/	
	
	
	//older, scheduling by int, Integer based
	public int scheduleArrivalInt(String airportName, int arrTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime, arrTime);
	}
	//older
	public int scheduleArrivalInt(String airportName, int arrTime, int schArrTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime, schArrTime);
	}
	//older
	public int scheduleDepartureInt(String airportName, int depTime, int schDepTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestDeparture(depTime, schDepTime);
	}
	//older 
	public int scheduleArrivalDepricated(String airportName, int arrTime, int schArrTime, Flight f) {
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrivalDepricated(arrTime, schArrTime, f);
	}
	
	//older
	//See airportTree class
	public boolean removeFlightFromArrivalQueue(String airportName, int arrivalTime){
		AirportTree a = getAirport(airportName);
		return a.freeArrivalSlot(arrivalTime);
	}
	
	//returns soonest available slot on a first come first served basis
	public int getSoonestArrivalSlot(Flight f, int proposedArrivalTime, int currentTime){
		return getAirport(f.arrivalAirport).getSoonestFirstComeFirstServedSlot(f, proposedArrivalTime, false);
	}
	
	
	//This method the older, int based way of managing slots
	//Huu
	public int getSoonestArrivalInt(String airportName, int arrivalTime){
		AirportTree a = getAirport(airportName);
		return a.getSoonestArrivalSlot(arrivalTime);
	}
	
	//older
	public int getSoonestDepartureInt(String airportName, int arrivalTime){
		AirportTree a = getAirport(airportName);
		return a.getSoonestArrivalSlot(arrivalTime);
	}
	
	
	public AirportTree getAirport(String airportName){
		AirportTree a = airportList.get(airportName);
		if(a == null){
			a = new AirportTree(airportName, this);
			airportList.put(airportName, a);
		}
		return a;
	}
	
	public AirportTree getDepartureAirport(Flight f){
		return getAirport(f.departureAirport);
	}
	
	public AirportTree getArrivalAirport(Flight f){
		return getAirport(f.arrivalAirport);
	}

	//See airportTree class
	public void offsetCapacities(int offset){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.offsetCapacities(offset);
		}
	}
	
	//See airportTree class
	public boolean effectedByCFR(Flight f){
		return getAirport(f.arrivalAirport).effectedByCFR(f.arrivalTimeACES);
	}
	
	//See airportTree class
	public void turnOffDepartureContract(){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.setDepartureContract(false);
		}
	}	

	//See airportTree class
	//older int based
	public void validateDepartureTraffic(){
		for (AirportTree at: airportList.values()) {
			at.validateDepartureTraffic();
		}
	}
	//See airportTree class
	//older int based
	public void validateArrivalTraffic() {
		for (AirportTree at: airportList.values()) {
			at.validateArrivalTraffic();
		}
	}
	//See airportTree class
	//validates that all flights meet capacity constraint spacing and no repeat flight ID's in schedule
	public void validate(){ 
		for (AirportTree a : airportList.values()){ 
			a.validate();
		}
	}

	
	//See airportTree class
	public void printDelays(){ 
		for (AirportTree a : airportList.values()){ //io.println("");
			U.p(a.airportName + "*");
			a.printDelayVars();
		}
		io.println("TOTAL Airports: " + airportList.size());
	}	
	//See airportTree class
	public void printAirports(){ 
		for (AirportTree a : airportList.values()){ //io.println("");
			
			a.print();
		}
		io.println("TOTAL Airports: " + airportList.size());
	}
	
	
	public void printAirportsToFile(BufferedWriter cap, BufferedWriter dep, BufferedWriter schedDep, BufferedWriter arr, BufferedWriter schedArr) {
		for (AirportTree a : airportList.values()){
			try{
				a.printToFile(cap,dep,schedDep,arr, schedArr);
			}catch (Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}
			
	}
	//See airportTree class
	public void printMinSpacing(){ 
		for (AirportTree a : airportList.values()){ //io.println("");
			a.printMinSpacing();
		}
	}
	

}
