package fcfs;

/**
 * @author Noam Almog
 *
 */
import java.io.*;
import java.util.*;

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
					a = new AirportTree(airportName);
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
			AirportTree f = new AirportTree(name);
			while ((line = br.readLine()) != null){
				line = line.trim();
				// location(0), sim day(1), hour(2), quarter(3), rates(4)
				subs = line.split(",");
				//io.printf("%s:%s\n",name, subs[0]);
				if( subs.length == 5 && !line.startsWith("*")){
					
					if (!name.equals(subs[0])){
						if(!name.equals("noexist")){airportList.put(name, f);}
						name = subs[0];
						f = new AirportTree(name); 
					}
					
					//make list of capacities.
					int day = Integer.parseInt(subs[1]) - 1;
					int hour = Integer.parseInt(subs[2]);
					int quarterHour = Integer.parseInt(subs[3])-1;
					int timeInMills = (((day*24 + hour)*60 + quarterHour*15)*60*1000); //converts all to milliseconds  
					String[] subss = subs[4].split("_");
					int adr = Integer.parseInt(subss[2]);
					int aar = Integer.parseInt(subss[4]);
					//Main.p(name);
					/*
					if(name.equals("KATL")){
						adr = (int)(adr*1.);
						aar = (int)(aar*1.);
						//Main.p(adr + " katl " + aar );
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
		AirportTree a = airportList.get(f.arrivalAirport);
		if(a == null){
			a = new AirportTree(f.arrivalAirport);
			airportList.put(f.arrivalAirport, a);
		}
		return a.freeArrivalSlot(f);
	}
	
	public void schedulePackedArrival(Flight flight, int proposedArrivalTime, int currentTime){
		AirportTree a = airportList.get(flight.arrivalAirport);
		if(a == null){
			a = new AirportTree(flight.arrivalAirport);
			airportList.put(flight.arrivalAirport, a);
		}
		a.insertAtSoonestArrivalWithForwardGapsRemoved(flight, proposedArrivalTime, currentTime);
	}
	public void validate(){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.validate();
		}
	}	
	public void resetToStart(){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.resetToStart();
		}
	}
	
	public void printDelays(){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			Main.p(f.airportName + "*");
			f.printDelayVars();
		}
		io.println("TOTAL Airports: " + airportList.size());
	}	
	
	public void printAirports(){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.print();
		}
		io.println("TOTAL Airports: " + airportList.size());
	}
	
	public void printMinSpacing(){ 
		for (AirportTree f : airportList.values()){ //io.println("");
			f.printMinSpacing();
		}
	}
	
	public int scheduleDeparture(String airportName, int depTime){
		AirportTree a = airportList.get(airportName);
		if(a == null){
			a = new AirportTree(airportName);
			airportList.put(airportName, a);
		}
		return a.insertAtSoonestDeparture(depTime);
	}
	
	public int scheduleArrival(String airportName, int arrTime){
		AirportTree a = airportList.get(airportName);
		if(a == null){
			a = new AirportTree(airportName);
			airportList.put(airportName, a);
		}
		return a.insertAtSoonestArrival(arrTime);
	}
	
	public int getSoonestDeparture(String airportName, int departureTime){
		AirportTree a = airportList.get(airportName);
		if(a == null){
			a = new AirportTree(airportName);
			airportList.put(airportName, a);
		}
		return a.getSoonestDepartureSlot(departureTime);
	}
	
	public int getSoonestArrival(String airportName, int arrivalTime){
		AirportTree a = airportList.get(airportName);
		if(a == null){
			a = new AirportTree(airportName);
			airportList.put(airportName, a);
		}
		return a.getSoonestArrivalSlot(arrivalTime);
	}
	
	public boolean removeFlightFromArrivalQueue(String airportName, int arrivalTime){
		AirportTree a = airportList.get(airportName);
		if(a == null){
			a = new AirportTree(airportName);
			airportList.put(airportName, a);
		}
		return a.freeArrivalSlot(arrivalTime);
	}
}
