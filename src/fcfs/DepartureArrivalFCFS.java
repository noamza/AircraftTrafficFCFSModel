package fcfs;
import java.util.*;
import java.io.*;

public class DepartureArrivalFCFS {

	Flights flights; 
	Airports airports;

	public void scheduleFCFS(){

		double speedUp = 0.025;
		double slowDown = 0.05;

		flights = new Flights();

		airports = new Airports();

		String workingDirectory = "/Users/hvhuynh/Desktop/scheduler/inputs/";

		//flights.loadFlightsFromAces(workingDirectory+"job_23_sector_transitTime_takeoffLanding_35h_1.csv", true); // constrained
		flights.loadFlightsFromAces(workingDirectory +"job_24_sector_transitTime_takeoffLanding_35h_1.csv", true); //unconstrained
		//flights.loadFlightsFromAces(workingDirectory +"job_40_sector_transitTime_takeoffLanding_35h_1.csv",true);
		//flights.loadTaxiOffset(workingDirectory+"AirportTaxi.csv");

		
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May.csv");
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May_MAP9999.csv");
		//sectors.printSectors();
		airports.loadFromAces(workingDirectory+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
		//airports.printAirports();


		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		ArrayList<Flight> departedFlightList = new ArrayList<Flight>();
		
		//    Sort flights by Departure Time.
		Collections.sort(flightList, new flightDepTimeComparator());
		double totalGroundDelay = 0;
		
		for (Flight f: flightList){

			int departureTimeProposed = airports.getSoonestDeparture(f.departureAirport, f.departureTimeProposed);
			
			int departureTimeFinal = airports.scheduleDeparture(f.departureAirport, departureTimeProposed, f.departureTimeProposed);
			int groundDelay = departureTimeFinal - f.departureTimeProposed;
			totalGroundDelay += groundDelay;
			f.atcGroundDelay = groundDelay;
			f.departureTimeFinal = departureTimeFinal;
			f.arrivalTimeScheduled = f.arrivalTimeProposed;
			f.arrivalTimeProposed = f.arrivalTimeProposed + groundDelay;
			departedFlightList.add(f);		
				
		}
		
		double totalAirDelay = 0;
		Collections.sort(departedFlightList, new flightArrTimeComparator());
		
		for (Flight flight: departedFlightList) {
			int arrivalTimeProposed = airports.getSoonestArrival(flight.arrivalAirport, flight.arrivalTimeProposed);
			int arrivalTimeFinal = airports.scheduleArrival(flight.arrivalAirport,arrivalTimeProposed, flight.arrivalTimeScheduled);
			int airDelay = arrivalTimeFinal- flight.arrivalTimeProposed;
			flight.atcAirDelay = airDelay;
			totalAirDelay += airDelay;
		}
		
		System.out.println("Total Ground Delay = " + totalGroundDelay/3600000);
		System.out.println("Total Air Delay = " + totalAirDelay/3600000);
		System.out.println("Total Delay = " + (totalGroundDelay+totalAirDelay)/3600000);
		System.out.println("Total Flights Flown = " + departedFlightList.size());
			
		
		

		//String fcfsdir = "fcfs_output/";
		//printSectorTraffic(sectors, workingDirectory+fcfsdir);
		

		//printAirportTrafficCounts(airports,  workingDirectory+fcfsdir);
		
		//airports.validate();
		//flights.validateFCFS();

	}
		

		
		//printAirportDelays(flightList,workingDirectory+fcfsdir);
		//printFlightDetails(flightList, workingDirectory+fcfsdir);
	
	
	public void printFlightDetails(ArrayList<Flight> flightList, String dir){
		try {
			FileWriter fstream = new FileWriter(dir + "fcfs_flight_details.csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("FlightId,DepartureAirport,ArrivalAirport,DepartureTimeProposed,DepartureTimeFinal,ArrivalTimeProposed,ArrivalTimeFinal,actualDelay,atcGroundDelay,DelayAbsorbedInAir");
			out.write("\n");
			for(Flight f: flightList) {
				double actualDelay = f.arrivalTimeFinal - f.arrivalTimeProposed;
				double delayAbsorbedInAir = actualDelay - f.atcGroundDelay;
				out.write(f.id + "," + f.departureAirport + "," + f.arrivalAirport + "," + f.departureTimeProposed + "," 
						+ f.departureTimeFinal + "," + f.arrivalTimeProposed + "," + f.arrivalTimeFinal + "," + f.atcGroundDelay+","+actualDelay+","+delayAbsorbedInAir);
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
			FileWriter fstream = new FileWriter(dir + "fcfs_airport_delays.csv");
			BufferedWriter out = new BufferedWriter(fstream);
			Hashtable<String, Double> airportDelay = new Hashtable<String,Double>();
			for(Flight f: flightList){
				double realDelay = f.arrivalTimeFinal - f.arrivalTimeProposed;
				
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
