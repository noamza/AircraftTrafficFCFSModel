/*
 * @author hvhuynh
 * 
 * */

package fcfs;
import java.util.*;
import java.io.*;

public class DepartureArrivalFCFS {

	Flights flights; 
	Airports airports;
	Sectors sectors;
	
	public void scheduleFCFS(){

		//double speedUp = 0.025;
		//double slowDown = 0.05;
		double totalGroundDelay = 0;
		double totalAirDelay = 0;
		double totalSectorDelay = 0;
		flights = new Flights();
		airports = new Airports();
		sectors = new Sectors();
		
		String workingDirectory = "/Users/hvhuynh/Desktop/scheduler/inputs/";
		String outputdir = "departureArrivalFCFS_output/";
		//flights.loadFlightsFromAces(workingDirectory+"job_23_sector_transitTime_takeoffLanding_35h_1.csv", true); // constrained
		flights.loadFlightsFromAces(workingDirectory +"job_24_sector_transitTime_takeoffLanding_35h_1.csv", true); //unconstrained
		//flights.loadFlightsFromAces(workingDirectory +"job_40_sector_transitTime_takeoffLanding_35h_1.csv",true); //constrained
		
		
		sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May.csv");
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May_MAP9999.csv");
		airports.loadFromAces(workingDirectory+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");

		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		ArrayList<Flight> arrivingFlightList = new ArrayList<Flight>();
		
		//Sort flights by Departure Time.
		Collections.sort(flightList, new flightDepTimeComparator());
		
		//Schedule Departing Flights
		for (Flight flight: flightList) {
			
			//get soonest time slot the flight can depart
			int departureTimeProposed = airports.getSoonestDeparture(flight.departureAirport, flight.departureTimeScheduled);
			
			//schedule the flight
			int departureTimeFinal = airports.scheduleDeparture(flight.departureAirport, departureTimeProposed, flight.departureTimeScheduled);
			int groundDelay = departureTimeFinal - flight.departureTimeProposed;
			totalGroundDelay += groundDelay;
			flight.atcGroundDelay = groundDelay;
			flight.departureTimeFinal = departureTimeFinal;
			//scheduled arrival time changes when ground delay is taken into account
			flight.arrivalTimeProposed = flight.arrivalTimeScheduled + groundDelay;
			arrivingFlightList.add(flight);
			/*
			int blockDelay = 0;
			for (SectorAirport sa: flight.path) {
				if(flight.id == 30448) { 
					System.out.println("this is a good place to stop");
				}
				int proposedEntryTime = sa.entryTime + groundDelay + blockDelay;
				
				int sectorTimeProposed = sectors.getSoonestSlot(sa.name, proposedEntryTime, proposedEntryTime + sa.transitTime);
				
				//blockDelay += sectorTimeProposed - proposedEntryTime;
				
				int sectorTimeFinal = sectors.schedule(sa.name, sectorTimeProposed, sectorTimeProposed + sa.transitTime);
				blockDelay += sectorTimeFinal - proposedEntryTime;
				
			}
			System.out.println(flight.id);
			System.out.println(blockDelay);
			totalSectorDelay += blockDelay;*/
		}
		
		//validate departure traffic spacing at airports.
		airports.validateDepartureTraffic();
		
		//sectors
		
		
		
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
		//validate individual flights by checking departure/arrival times
		flights.validateFCFS();
		
		System.out.println("Total Ground Delay = " + totalGroundDelay/3600000);
		System.out.println("Total Air Delay = " + totalAirDelay/3600000);
		System.out.println("Total Delay in sectors = " + totalSectorDelay/3600000);
		System.out.println("Total Delay = " + (totalGroundDelay+totalAirDelay)/3600000);
		System.out.println("Total Flights Flown = " + arrivingFlightList.size());
			
		//printSectorTraffic(sectors, workingDirectory+outputdir);
		
		printAirportDelays(flightList, workingDirectory+outputdir);
		printAirportTrafficCounts(airports,  workingDirectory+outputdir);
		printFlightDetails(flightList, workingDirectory+outputdir);
		
		System.out.println("Finished");
	}
		

	
	public void printFlightDetails(ArrayList<Flight> flightList, String dir){
		try {
			String fname = "depArr_fcfs_flight_details.csv";
			System.out.println("Printing flight details to " + dir +fname);
			FileWriter fstream = new FileWriter(dir + fname);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("FlightId,DepartureAirport,ArrivalAirport,DepartureTimeScheduled,DepartureTimeFinal,ArrivalTimeScheduled,ArrivalTimeFinal,TotalDelay,GroundDelay,AirDelay");
			out.write("\n");
			for(Flight f: flightList) {
				double totalDelay = f.atcGroundDelay + f.atcAirDelay;
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
			System.out.println("Printing traffic counts to " + dir);
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
			String fname = "deparr_fcfs_airport_delays.csv";
			System.out.println("Printing airport delays to " + dir+fname);
			FileWriter fstream = new FileWriter(dir + fname);
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
