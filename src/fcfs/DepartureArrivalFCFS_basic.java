package fcfs;
import java.util.*;
import java.io.*;

//@author Huu Huynh


/**
 * @todo Deprecate the {@link #scheduleFCFS()} method and use externally initialized data
 */
/**
 * 
 */
public class DepartureArrivalFCFS_basic implements Scheduler {

	Flights flights = new Flights();
	Airports airports = new Airports();
	Sectors sectors = new Sectors();

	/**
	 * Added to support programmatic initialization (as opposed to initialization from files).
	 * @param flights
	 * @param airports
	 * @param sectors
	 */
	public DepartureArrivalFCFS_basic(Flights flights, Airports airports, Sectors sectors)
	{
		this.flights = flights;
		this.airports = airports;
		this.sectors = sectors;
	}

	@Deprecated
	public DepartureArrivalFCFS_basic()
	{
	}

	@Deprecated
	public void scheduleFCFS(String workingDirectory, String outputDirectory){

		initialize();

		loadDataFromFiles(workingDirectory);

		ArrayList<Flight> flightList = schedule();
			
		printResults(flightList, workingDirectory, outputDirectory);
		
		System.out.println("Finished");
	}

	/**
	 * 
	 */
	@Deprecated
	private void initialize()
	{
		flights = new Flights();
		airports = new Airports();
		sectors = new Sectors();
	}

	/**
	 * @param flightList
	 * @param workingDirectory
	 * @param outputdir
	 */
	@Deprecated
	public void printResults(ArrayList<Flight> flightList, String workingDirectory, String outputDirectory)
	{
		String dir = new File(workingDirectory, outputDirectory).getAbsolutePath();

		printResults(flightList, dir);
	}

	/**
	 * @param flightList
	 * @param dir
	 */
	@Override
	public void printResults(ArrayList<Flight> flightList, String dir)
	{
		//printSectorTraffic(sectors, dir);

		printAirportDelays(flightList, dir);
		printAirportTrafficCounts(airports,  dir);
		printFlightDetails(flightList, dir);
	}



	/**
	 * @return
	 */
	@Override
	public ArrayList<Flight> schedule()
	{
		//double speedUp = 0.025;
		//double slowDown = 0.05;
		double totalGroundDelay = 0;
		double totalAirDelay = 0;
		double totalSectorDelay = 0;
		
		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		ArrayList<Flight> arrivingFlightList = new ArrayList<Flight>();
		
		//Sort flights by Departure Time.
		Collections.sort(flightList, new flightDepTimeIDComparator());
		
		//Schedule Departing Flights
		for (Flight flight: flightList) {
			
			//get soonest time slot the flight can depart
			int departureTimeProposed = airports.getSoonestDeparture(flight.departureAirport, flight.departureTimeScheduled);
			
			//schedule the flight
			int departureTimeFinal = airports.scheduleDeparture(flight.departureAirport, departureTimeProposed, flight.departureTimeScheduled);
			int groundDelay = departureTimeFinal - flight.departureTimeACES;
			totalGroundDelay += groundDelay;
			flight.atcGroundDelay = groundDelay;
			flight.departureTimeFinal = departureTimeFinal;
			//scheduled arrival time changes when ground delay is taken into account
			flight.arrivalTimeACES = flight.arrivalTimeScheduled + groundDelay;
			arrivingFlightList.add(flight);
			
			int blockDelay = 0;
			for (SectorAirport sa: flight.path) {
				int proposedEntryTime = sa.entryTime + groundDelay + blockDelay;
				
				int sectorTimeProposed = sectors.getSoonestSlot(sa.name, proposedEntryTime, proposedEntryTime + sa.transitTime);
				
				//blockDelay += sectorTimeProposed - proposedEntryTime;
				
				int sectorTimeFinal = sectors.schedule(sa.name, sectorTimeProposed, sectorTimeProposed + sa.transitTime);
				blockDelay += sectorTimeFinal - proposedEntryTime;
				
			}
			totalSectorDelay += blockDelay;
		}
		
		//validate departure traffic spacing at airports.
		airports.validateDepartureTraffic();
		
		//sectors
		
		
		
		//Sort flights by proposed arrival time.
		Collections.sort(arrivingFlightList, new flightArrTimeIDComparator());
		
		//Schedule Arriving Flights
		for (Flight flight: arrivingFlightList) {
			//get soonest time slot the flight can land
			int arrivalTimeProposed = airports.getSoonestArrival(flight.arrivalAirport, flight.arrivalTimeACES);
			//schedule the flight
			int arrivalTimeFinal = airports.scheduleArrival(flight.arrivalAirport,arrivalTimeProposed, flight.arrivalTimeScheduled);
			flight.arrivalTimeFinal = arrivalTimeFinal;
			int airDelay = arrivalTimeFinal - flight.arrivalTimeACES;
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

		return flightList;
	}



	/**
	 * @param workingDirectory
	 */
	@Deprecated
	public void loadDataFromFiles(String workingDirectory)
	{
		//flights.loadFlightsFromAces(workingDirectory+"job_23_sector_transitTime_takeoffLanding_35h_1.csv", true); // constrained
		flights.loadFlightsFromAces(workingDirectory +"job_24_sector_transitTime_takeoffLanding_35h_1.csv", true); //unconstrained
		//flights.loadFlightsFromAces(workingDirectory +"job_40_sector_transitTime_takeoffLanding_35h_1.csv",true); //constrained
		
		
		sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May.csv");
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May_MAP9999.csv");
		airports.loadCapacitiesFromAces(workingDirectory+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
	}
		

	
	public void printFlightDetails(ArrayList<Flight> flightList, String dir){
		try {
			String fname = "depArr_fcfs_flight_details.csv";
			String filepath = new File(dir, fname).getAbsolutePath();
			System.out.println("Printing flight details to " + filepath);
			FileWriter fstream = new FileWriter(filepath);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("FlightId,DepartureAirport,ArrivalAirport,DepartureTimeScheduled,DepartureTimeFinal,ArrivalTimeScheduled,ArrivalTimeFinal,TotalDelay,GroundDelay,AirDelay");
			out.write("\n");
			for(Flight f: flightList) {
				double totalDelay = f.atcGroundDelay + f.atcAirDelay;
				double totalGroundDelay = f.atcGroundDelay;
				double totalAirDelay = f.atcAirDelay;
				out.write(f.id + "," + f.departureAirport + "," + f.arrivalAirport + "," + f.departureTimeACES + "," 
						+ f.getDepartureTimeFinal() + "," + f.arrivalTimeScheduled + "," + f.arrivalTimeFinal + "," + totalDelay +","+ totalGroundDelay+","+totalAirDelay);
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
			String capsFile = new File(dir, "fcfs_airport_capacities.csv").getAbsolutePath();
			FileWriter capstream = new FileWriter( capsFile);
			BufferedWriter airport_caps_out = new BufferedWriter(capstream);
			
			String depsFile = new File(dir, "fcfs_airport_DEPtraffic.csv").getAbsolutePath();
			FileWriter depstream = new FileWriter(depsFile);
			BufferedWriter airport_DEPtraffic_out = new BufferedWriter(depstream);
			
			String arrsFile = new File(dir, "fcfs_airport_ARRtraffic.csv").getAbsolutePath();
			FileWriter arrstream = new FileWriter(arrsFile);
			BufferedWriter airport_ARRtraffic_out = new BufferedWriter(arrstream);
			
			String schedDepsFile = new File(dir, "fcfs_airport_schedDEPtraffic.csv").getAbsolutePath();
			FileWriter sdepstream = new FileWriter(schedDepsFile);
			BufferedWriter airport_schedDEPtraffic_out = new BufferedWriter(sdepstream);
			
			String schedArrsFile = new File(dir, "fcfs_airport_schedARRtraffic.csv").getAbsolutePath();
			FileWriter sarrstream = new FileWriter(schedArrsFile);
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
			String filepath = new File(dir, fname).getAbsolutePath();
			System.out.println("Printing airport delays to " + filepath);
			FileWriter fstream = new FileWriter(filepath);
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
