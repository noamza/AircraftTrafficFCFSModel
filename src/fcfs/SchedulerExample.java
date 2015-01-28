package fcfs;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
		flights.loadFlightsFromAces(U.workingDirectory + U.inputFolder + U.ACESflightTrackData,false);
		airports.loadCapacitiesFromAces(U.workingDirectory + U.inputFolder + U.airportCapacity);
	}
	
	//schedules (only) arrival slots by order of departure
	public ArrayList<Flight> scheduleByDeparture() {
		U.p("scheduling flights by departure order..");
		flights = new Flights(); 
		airports = new Airports();
		load();
		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		double totalDelayHours = 0;
		
		Collections.sort(flightList, new flightDepTimeIDComparator()); //This is the key line that specifies the order of scheduling	(by departure)
		for(Flight f: flightList){
			//this schedules a flight as close to the ACES arrival time as possible and returns any delay caused by capacity constraints. Current time is f.departureTimeACES which means flights are being scheduled at their scheduled departure time.
			//you could take into account taxi scheduling into the proposed arrival time if you want to.
			int delay = airports.scheduleArrival(f, f.arrivalTimeACES, f.departureTimeACES);
			f.atcGroundDelay += delay; //delay distribution needs to specified. In this case on the ground.
			//the scheduler will store a value for f.arrivalAirportAssignedDelay
			f.departureTimeFinal += delay;//flight takes off after ground delay
			//you could also add taxi out time to departure departureTimeFinal etc etc..
			totalDelayHours += delay/U.toHours;
		}
		airports.validate(); //validates that all slots meet capacity constraints and no duplicate flights scheduled.
		validateFlights(flightList); //custom validation
		U.pf("total delay in hrs %.1f\n", totalDelayHours);
		writeOutArrivalDepartures(flightList);
		return flightList;
	}
	
	//schedules (only) arrival slots by order of arrival
	public ArrayList<Flight> scheduleByArrival() {
		U.p("scheduling flights by arrival order..");
		flights.resetPerturbationAndSchedulingDependentVariables();
		airports.resetToStart();
		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		double totalDelayHours = 0;
		
		Collections.sort(flightList, new flightArrTimeIDComparator()); //schedule by order of departure
		for(Flight f: flightList){
			int delay = airports.scheduleArrival(f, f.arrivalTimeACES, f.departureTimeACES);
			f.atcAirDelay += delay; // this time delay is taken in the air
			f.departureTimeFinal = f.departureTimeACES;//this time delay is taken in the air so 
			totalDelayHours += delay/U.toHours;
		}
		airports.validate(); 
		validateFlights(flightList); 
		U.pf("total delay in hrs %.1f\n", totalDelayHours);
		writeOutArrivalDepartures(flightList);
		return flightList;
	}
	
	//example to validate flights. There built in validate functions but they are somewhat customized.
	void validateFlights(ArrayList<Flight> temp){
		for (Flight f: temp){
			U.Assert(f.arrivalAirportAssignedDelay == f.arrivalTimeFinal - f.arrivalTimeACES, 
					"error in delay");
		}
	}
	
	//shows a demo of writing out the variables we used and are interested in from this schedule.
	public void writeOutArrivalDepartures(ArrayList<Flight> temp){
		String path = U.workingDirectory+U.outFolder+"example/scheduleByArrival.csv";
		Collections.sort(temp, new flightIDComparator()); 
		//*
		try{
			// Create file 
			FileWriter fstream = new FileWriter(path);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("flight id," +
					//events
					"departure airport," +
					"departure time," +
					"arrival airport," +
					"arrival time," +
					"total delay" +
					"\n");

			for (Flight f: temp){
				out.write(f.id +"," +
						//events
						f.departureAirport +","+
						f.departureTimeFinal + "," +
						f.arrivalAirport +","+ 
						f.arrivalTimeFinal +","+
						(f.arrivalTimeFinal - f.arrivalTimeACES)+
						"\n"
						);	
			}
			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		
	}

}
