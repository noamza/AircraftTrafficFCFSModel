package fcfs;

/**
 * @author Noam Almog
 *
 */
import java.io.*;
import java.util.*;

/*
 * This class holds the collection of airports being scheduled.
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
	public void loadFromAces(String filePath){
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
	
	//See airportTree class
	public boolean removeFlightFromDepartureQueue(Flight f){
			AirportTree a = getAirport(f.departureAirport);
			return a.freeDepartureSlot(f);
	}
	//See airportTree class
	public boolean effectedByCFR(Flight f){
		return getAirport(f.arrivalAirport).effectedByCFR(f.arrivalTimeACES);
	}
	//See airportTree class
	public int getSoonestArrival(Flight f, int proposedArrivalTime, int currentTime){
		return getAirport(f.arrivalAirport).getSoonestNonPrioritySlot(f, proposedArrivalTime, false) - proposedArrivalTime;
	}
	//See airportTree class
	public int getSoonestNonPriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).getSoonestNonPrioritySlot(f, proposedDepartureTime, true) - proposedDepartureTime;
	}
	//See airportTree class
	public int getSoonestPriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).getSoonestPriorityDepartureSlot(f, proposedDepartureTime) - proposedDepartureTime;
	}
	//See airportTree class
	public int scheduleArrival(Flight f, int proposedArrivalTime, int currentTime){
		//return getAirport(f.arrivalAirport).insertNonPriorityArrival(f, proposedArrivalTime, currentTime);
		return getAirport(f.arrivalAirport).scheduleArrival(f, proposedArrivalTime, currentTime);
	}
	//See airportTree class
	public int scheduleDeparture(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).scheduleDeparture(f, proposedDepartureTime, currentTime);
	}
	/*
	public int scheduleNonPriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).scheduleDeparture(f, proposedDepartureTime, currentTime);
	}
	
	public int schedulePriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).scheduleDeparture(f, proposedDepartureTime, currentTime);
	}
	*/
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
	public void turnOffDepartureContract(){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.setDepartureContract(false);
		}
	}	
	//See airportTree class
	public void offsetCapacities(int offset){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.offsetCapacities(offset);
		}
	}	
	//See airportTree class
	public boolean removeFlightFromArrivalQueue(Flight f){
		AirportTree a = getAirport(f.arrivalAirport);
		return a.freeArrivalSlot(f);
	}
	//See airportTree class
	public void schedulePackedArrival(Flight flight, int proposedArrivalTime, int currentTime){
		AirportTree a = getAirport(flight.arrivalAirport);
		a.insertAtSoonestArrivalWithForwardGapsRemoved(flight, proposedArrivalTime, currentTime);
	}
	//See airportTree class
	public void validateDepartureTraffic(){
		for (AirportTree at: airportList.values()) {
			at.validateDepartureTraffic();
		}
	}
	//See airportTree class
	public void validateArrivalTraffic() {
		for (AirportTree at: airportList.values()) {
			at.validateArrivalTraffic();
		}
	}
	//See airportTree class
	public void validate(){ 
		for (AirportTree a : airportList.values()){ 
			a.validate();
		}
	}
	//See airportTree class
	public void resetToStart(){ 
		for (AirportTree a : airportList.values()){
			a.resetToStart();
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
	/*
	public int scheduleDeparture(String airportName, int depTime){
		AirportTree a = airportList.get(airportName);
		if(a == null){
			a = new AirportTree(airportName);
			airportList.put(airportName, a);
		}
		return a.insertAtSoonestDeparture(depTime);
	}*/
	//See airportTree class
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
	
	/*
	public int getSoonestCMParrival(String airportName, int arrivalTime) {
		AirportTree a = airportList.get(airportName);
		if(a == null) {
			a = new AirportTree(airportName);
			airportList.put(airportName,  a);
		}
		return a.getSoonestCMParrivalSlot(arrivalTime);
	}*/
	//See airportTree class
	public int getSoonestArrival(String airportName, int arrivalTime){
		AirportTree a = getAirport(airportName);
		return a.getSoonestArrivalSlot(arrivalTime);
	}
	//See airportTree class
	public int getSoonestArrival(String airportName, int arrivalTime, int minArrivalTime, int maxArrivalTime) {
		AirportTree a = getAirport(airportName);
		return a.getSoonestArrivalSlot(arrivalTime, minArrivalTime, maxArrivalTime);
	}
	//See airportTree class
	public boolean removeFlightFromArrivalQueue(String airportName, int arrivalTime){
		AirportTree a = getAirport(airportName);
		return a.freeArrivalSlot(arrivalTime);
	}
}
