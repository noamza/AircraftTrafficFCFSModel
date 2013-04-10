package fcfs;
import java.util.*;
import java.io.*;

public class DepartureArrivalFCFS {

	Flights flights; 
	Airports airports;

	public void scheduleFCFS(){

		double speedUp = 0.025;
		double slowDown = 0.05;
		double totalGroundDelay = 0;
		double totalAirDelay = 0;
		flights = new Flights();
		airports = new Airports();

		String workingDirectory = "/Users/hvhuynh/Desktop/scheduler/inputs/";
		String outputdir = "departureArrivalFCFS_output/";
		//flights.loadFlightsFromAces(workingDirectory+"job_23_sector_transitTime_takeoffLanding_35h_1.csv", true); // constrained
		flights.loadFlightsFromAces(workingDirectory +"job_24_sector_transitTime_takeoffLanding_35h_1.csv", true); //unconstrained
		//flights.loadFlightsFromAces(workingDirectory +"job_40_sector_transitTime_takeoffLanding_35h_1.csv",true); //constrained
		
		
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May.csv");
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May_MAP9999.csv");
		airports.loadFromAces(workingDirectory+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");

		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		ArrayList<Flight> arrivingFlightList = new ArrayList<Flight>();
		
		//Sort flights by Departure Time.
		Collections.sort(flightList, new flightDepTimeComparator());
		
		//Schedule Departing Flights
		for (Flight f: flightList) {
			//get soonest time slot the flight can depart
			int departureTimeProposed = airports.getSoonestDeparture(f.departureAirport, f.departureTimeProposed);
			//schedule the flight
			int departureTimeFinal = airports.scheduleDeparture(f.departureAirport, departureTimeProposed, f.departureTimeProposed);
			int groundDelay = departureTimeFinal - f.departureTimeProposed;
			totalGroundDelay += groundDelay;
			f.atcGroundDelay = groundDelay;
			f.departureTimeFinal = departureTimeFinal;
			f.arrivalTimeScheduled = f.arrivalTimeProposed;
			//scheduled arrival time changes when ground delay is taken into account
			f.arrivalTimeProposed = f.arrivalTimeProposed + groundDelay;
			arrivingFlightList.add(f);			
		}
		
		//validate departure traffic spacing at airports.
		airports.validateDepartureTraffic();
		
		//Sort flights by proposed arrival time.
		Collections.sort(arrivingFlightList, new flightArrTimeComparator());
		
		//Schedule Arriving Flights
		for (Flight flight: arrivingFlightList) {
			//get soonest time slot the flight can land
			int arrivalTimeProposed = airports.getSoonestArrival(flight.arrivalAirport, flight.arrivalTimeProposed);
			//schedule the flight
			int arrivalTimeFinal = airports.scheduleArrival(flight.arrivalAirport,arrivalTimeProposed, flight.arrivalTimeScheduled);
			flight.arrivalTimeFinal = arrivalTimeFinal;
			int airDelay = arrivalTimeFinal - flight.arrivalTimeProposed;
			flight.atcAirDelay = airDelay;
			totalAirDelay += airDelay;
		}
		
		//validate arrival traffic spacing at airports.
		airports.validateArrivalTraffic();
		flights.validateFCFS();
		
		System.out.println("Total Ground Delay = " + totalGroundDelay/3600000);
		System.out.println("Total Air Delay = " + totalAirDelay/3600000);
		System.out.println("Total Delay = " + (totalGroundDelay+totalAirDelay)/3600000);
		System.out.println("Total Flights Flown = " + arrivingFlightList.size());
			
		//printSectorTraffic(sectors, workingDirectory+outputdir);
		
		printAirportDelays(flightList, workingDirectory+outputdir);
		printAirportTrafficCounts(airports,  workingDirectory+outputdir);
		printFlightDetails(flightList, workingDirectory+outputdir);
	}
		

		
		
		
	
	
	public void printFlightDetails(ArrayList<Flight> flightList, String dir){
		try {
			FileWriter fstream = new FileWriter(dir + "depArr_fcfs_flight_details.csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("FlightId,DepartureAirport,ArrivalAirport,DepartureTimeScheduled,DepartureTimeFinal,ArrivalTimeScheduled,ArrivalTimeFinal,TotalDelay,GroundDelay,AirDelay");
			out.write("\n");
			for(Flight f: flightList) {
				double totalDelay = f.atcGroundDelay - f.atcAirDelay;
				double totalGroundDelay = f.atcGroundDelay;
				double totalAirDelay = f.atcAirDelay;
				out.write(f.id + "," + f.departureAirport + "," + f.arrivalAirport + "," + f.departureTimeProposed + "," 
						+ f.departureTimeFinal + "," + f.arrivalTimeScheduled + "," + f.arrivalTimeFinal + "," + totalDelay +","+ totalGroundDelay+","+totalAirDelay);
				out.write("\n");
			}
			out.close();
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printAirportTrafficCounts(Airports airports, String dir) {
		try {
			
			FileWriter capstream = new FileWriter( dir + "fcfs_airport_capacities.csv");
			BufferedWriter airport_caps_out = new BufferedWriter(capstream);
			
			FileWriter depstream = new FileWriter(dir + "fcfs_airport_DEPtraffic.csv");
			BufferedWriter airport_DEPtraffic_out = new BufferedWriter(depstream);
			
			FileWriter arrstream = new FileWriter(dir + "fcfs_airport_ARRtraffic.csv");
			BufferedWriter airport_ARRtraffic_out = new BufferedWriter(arrstream);
			
			FileWriter sdepstream = new FileWriter(dir + "fcfs_airport_schedDEPtraffic.csv");
			BufferedWriter airport_schedDEPtraffic_out = new BufferedWriter(sdepstream);
			
			FileWriter sarrstream = new FileWriter(dir + "fcfs_airport_schedARRtraffic.csv");
			BufferedWriter airport_schedARRtraffic_out = new BufferedWriter(sarrstream);
			
			airports.printAirportsToFile(
										 airport_caps_out, 
										 airport_DEPtraffic_out, 
										 airport_schedDEPtraffic_out, 
										 airport_ARRtraffic_out, 
										 airport_schedARRtraffic_out
										 );
			
			airport_caps_out.close();
			airport_DEPtraffic_out.close();
			airport_ARRtraffic_out.close();
			airport_schedDEPtraffic_out.close();
			airport_schedARRtraffic_out.close();
			
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printSectorTraffic(Sectors sectors, String dir) {
		try {
			FileWriter fstream = new FileWriter(dir + "fcfs_sector_traffic.csv");
			BufferedWriter sector_out = new BufferedWriter(fstream);
			sectors.printSectorsToFile(sector_out);
			sector_out.close();
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printAirportDelays(ArrayList<Flight> flightList, String dir) {
		try {
			FileWriter fstream = new FileWriter(dir + "deparr_fcfs_airport_delays.csv");
			BufferedWriter out = new BufferedWriter(fstream);
			Hashtable<String, Double> airportDelay = new Hashtable<String,Double>();
			for(Flight f: flightList){
				double realDelay = f.arrivalTimeFinal - f.arrivalTimeScheduled;
				
				if(airportDelay.get(f.arrivalAirport)==null){
					airportDelay.put(f.arrivalAirport, 0.0);
				}				
				airportDelay.put(f.arrivalAirport, airportDelay.get(f.arrivalAirport) + realDelay);
			}
			Enumeration<String> enumKey = airportDelay.keys();
			while(enumKey.hasMoreElements()) {
				String k = enumKey.nextElement();
				Double val = airportDelay.get(k);
				if(val != 0.0) { val /= 60000;}
				out.write(k + "," + val);
				out.write("\n");
			}
			out.close();
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}


}
