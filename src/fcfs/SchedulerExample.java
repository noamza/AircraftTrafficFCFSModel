package fcfs;

import java.util.ArrayList;
import java.util.Collections;

public class SchedulerExample implements Scheduler {
	
	Flights flights; 
	Airports airports;
	
	@Override
	public void printResults(ArrayList<Flight> flightList, String dir) {
		
	}
	
	@Override
	public ArrayList<Flight> schedule() {
		U.p("scheduling: " + U.ACESflightTrackData);
		scheduleByDeparture();
		scheduleByArrival();
		return null;
	}
	
	public void load(){
		flights.loadFlightsFromAces(U.ACESflightTrackData,false);
		airports.loadCapacitiesFromAces(U.airportCapacity);
	}
	
	//schedules (only) arrival slots by order of departure
	public ArrayList<Flight> scheduleByDeparture() {
		U.p("scheduling flights by departure");
		flights = new Flights(); 
		airports = new Airports();
		load();
		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		//This is the keyline that specifies the order of scheduling	
		Collections.sort(flightList, new flightDepTimeIDComparator()); 
		for(Flight f: flightList){
			airports.sc
		}
		
		return null;
	}
	
	//schedules (only) arrival slots by order of arrival
	public ArrayList<Flight> scheduleByArrival() {
		U.p("scheduling flights by departure");
		// TODO Auto-generated method stub
		return null;
	}

}
