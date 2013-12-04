package fcfs;

/**
 * @author Noam Almog
 *
 */
import java.io.*;
import java.io.ObjectInputStream.GetField;
import java.util.*;

/*
 * this class holds the collection of airports in scheduling
 * has get, sets for the airports and loading from input data
 */
public class Airports {

	PrintStream io = System.out;
	//creates hash of AirportTrees.
	Hashtable<String, AirportTree> airportList= new Hashtable<String, AirportTree>();

	public void loadDelays(String path, String mode){
		try{
			//Read ACES Transit Time File Line by Line
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			String[] subs = new String[1];
			String airportName;
			while ((line = br.readLine()) != null){
				line = line.replaceAll("\\s","");//trim();
				// location(0), sim day(1), hour(2), quarter(3), rates(4)
				subs = line.split(",");
				airportName = "K"+subs[0];
				//make list of capacities.
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
					
					//adr = 9999; //testing unconstrained
					//aar = 9999; //testing unconstrained
					/*
					if(name.equals("KDFW")){
						aar = 50;
						//adr = 50;
					}
					*/
					/*
					else{
						aar = aar * 6/10;
						adr = adr * 6/10;
					}*/
					
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
	
	public boolean removeFlightFromDepartureQueue(Flight f){
			AirportTree a = getAirport(f.departureAirport);
			return a.freeDepartureSlot(f);
	}
	
	public boolean effectedByCFR(Flight f){
		return getAirport(f.arrivalAirport).effectedByCFR(f.arrivalTimeACES);
	}
	
	public int getSoonestArrival(Flight f, int proposedArrivalTime, int currentTime){
		return getAirport(f.arrivalAirport).getSoonestNonPrioritySlot(f, proposedArrivalTime, false) - proposedArrivalTime;
	}
	
	public int getSoonestNonPriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).getSoonestNonPrioritySlot(f, proposedDepartureTime, true) - proposedDepartureTime;
	}
	
	public int getSoonestPriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		return getAirport(f.departureAirport).getSoonestPriorityDepartureSlot(f, proposedDepartureTime) - proposedDepartureTime;
	}

	public int scheduleArrival(Flight f, int proposedArrivalTime, int currentTime){
		//return getAirport(f.arrivalAirport).insertNonPriorityArrival(f, proposedArrivalTime, currentTime);
		return getAirport(f.arrivalAirport).scheduleArrival(f, proposedArrivalTime, currentTime);
	}
	
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

	public void turnOffDepartureContract(){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.setDepartureContract(false);
		}
	}	
	
	public void offsetCapacities(int offset){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.offsetCapacities(offset);
		}
	}	
	
	public boolean removeFlightFromArrivalQueue(Flight f){
		AirportTree a = getAirport(f.arrivalAirport);
		return a.freeArrivalSlot(f);
	}
	
	public void schedulePackedArrival(Flight flight, int proposedArrivalTime, int currentTime){
		AirportTree a = getAirport(flight.arrivalAirport);
		a.insertAtSoonestArrivalWithForwardGapsRemoved(flight, proposedArrivalTime, currentTime);
	}
	
	public void validateDepartureTraffic(){
		for (AirportTree at: airportList.values()) {
			at.validateDepartureTraffic();
		}
	}
	
	public void validateArrivalTraffic() {
		for (AirportTree at: airportList.values()) {
			at.validateArrivalTraffic();
		}
	}
	
	public void validate(){ 
		for (AirportTree a : airportList.values()){ 
			a.validate();
		}
	}	
	public void resetToStart(){ 
		for (AirportTree a : airportList.values()){
			a.resetToStart();
		}
	}
	
	public void printDelays(){ 
		for (AirportTree a : airportList.values()){ //io.println("");
			Main.p(a.airportName + "*");
			a.printDelayVars();
		}
		io.println("TOTAL Airports: " + airportList.size());
	}	
	
	public void printAirports(){ 
		for (AirportTree a : airportList.values()){ //io.println("");
			
			a.print();
		}
		io.println("TOTAL Airports: " + airportList.size());
	}
	
	public void printAirports(String airport){ 
		for (AirportTree a : airportList.values()){ //io.println("");
			if (a.airportName.equals(airport)) {
				a.print(airport);
			}
		}
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
	
	public int scheduleArrival(String airportName, int arrTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime);
	}
	
	public int scheduleDeparture(String airportName, int depTime, int schDepTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestDeparture(depTime, schDepTime);
	}
	
	public int scheduleArrival(String airportName, int arrTime, int schArrTime){
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime, schArrTime);
	}
	
	public int scheduleArrival(String airportName, int arrTime, int schArrTime, Flight f) {
		AirportTree a = getAirport(airportName);
		return a.insertAtSoonestArrival(arrTime, schArrTime, f);
	}
	
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
	
	public int getSoonestArrival(String airportName, int arrivalTime){
		AirportTree a = getAirport(airportName);
		return a.getSoonestArrivalSlot(arrivalTime);
	}
	
	public int getSoonestArrival(String airportName, int arrivalTime, int minArrivalTime, int maxArrivalTime) {
		AirportTree a = getAirport(airportName);
		return a.getSoonestArrivalSlot(arrivalTime, minArrivalTime, maxArrivalTime);
	}
	
	public boolean removeFlightFromArrivalQueue(String airportName, int arrivalTime){
		AirportTree a = getAirport(airportName);
		return a.freeArrivalSlot(arrivalTime);
	}
}
