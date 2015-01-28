package fcfs;
import java.util.*;
import java.io.*;

/*
 * This is a straight forward implementation of FCFS (by departure for scheduling 
 * flights with departure, sector, and arrival constraints.
 * 
 * it sorts flights by departure time, then for each flight, it does a depth-first
 * search from departure->sectors->arrival, adding delay for each one, until it
 * finds the amount of delay where it can take off and pass through each node
 * without violating any constraints.
 * Some speeding up and slowing down is allowed to meet constraints.
*/
public class SchedulerFCFS {

	Flights flights; Sectors sectors; Airports airports;

	public void init(){

		//test comment
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm:ss:SSSS");
		Date date = new Date();
		System.out.println("init() " + dateFormat.format(date));
		double speedUp = 0.025;
		double slowDown = 0.05;

		flights = new Flights();
		sectors = new Sectors();
		airports = new Airports();

		String workingDirectory = "/Users/hvhuynh/Desktop/scheduler/inputs/";
		//flights.loadFlightsFromAces(workingDirectory+"clean_job.csv",true);
		//flights.loadFlightsFromAces(workingDirectory+"job_23_sector_transitTime_takeoffLanding_35h_1.csv", true); // constrained
		flights.loadFlightsFromAces(workingDirectory +"job_24_sector_transitTime_takeoffLanding_35h_1.csv", true); //unconstrained
		//flights.loadFlightsFromAces(workingDirectory +"job_40_sector_transitTime_takeoffLanding_35h_1.csv",true);
		//flights.loadTaxiOffset(workingDirectory+"AirportTaxi.csv");
		//flights.loadFlightsFromAces(workingDirectory+"job_9_sector_transitTime_takeoffLanding_35h_1.csv", true );
		//flights.loadFlightsFromAces(workingDirectory + "job_KSFO_arrs.csv", true);
		//flights.loadFlightsFromAces(workingDirectory+"TEST_fcfsj.csv");
		//flights.loadFromAces(workingDirectory+"recapture_chunki_from_clean.csv");
		//flights.loadFromAces(workingDirectory+"fcfsj2.csv");
		//flights.printFlights();
		
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May.csv");
		sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May_MAP9999.csv");
		//sectors.printSectors();
		airports.loadCapacitiesFromAces(workingDirectory+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
		//airports.printAirports();
		date = new Date();
		System.out.println("loaded " + dateFormat.format(date));
		
		/*
		 * sort by departure time
		 * For Each Flight
		 * 	find soonest time that works for all sectors + airport, than insert there
		 * print schedule and print delays
		 */

		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		//Collections.sort(flightList, new flightIDComparator());
		//flights.printFlightsFull();

		double td = 0; int c = 0;
		double totalDelayAbsorbed = 0;
		//    Sort flights by Departure Time.
		Collections.sort(flightList, new flightDepTimeIDComparator());
		//schedule each flight
		for (Flight f: flightList){
			boolean validFlight = false;
			
			int nominalDuration = f.arrivalTimeACES-f.departureTimeACES;
			int fastestDuration =  (int)(nominalDuration/(1+speedUp));
			int longestDuration = (int)(nominalDuration/(1-slowDown)); 
			
			ArrayList<SectorAirport> path = f.path;
			int delay = 0;
			
			/*for each iteration of the loop, a schedule is checked starting at the scheduled departure time
			 * plus the delay from the last iteration. The max delay of all the constraints is saved from each
			 * iteration, because this is the soonest the aircraft can depart without violating any constraints
			 * 
			 */
			while(!validFlight){
				int maxDelay = 0;
				//departure delay
				int departureDelay = airports.getSoonestDepartureInt(f.departureAirport, f.departureTimeACES+delay)-(f.departureTimeACES+delay);
				maxDelay = java.lang.Math.max(departureDelay,maxDelay);
				//sector delay
				for(SectorAirport s: path){	
					//U.p("delay " + delay);
					int sectorDelay = sectors.getSoonestSlot(s.name, s.entryTime+delay, s.entryTime+delay + s.transitTime)-(s.entryTime+delay);
					maxDelay = java.lang.Math.max(sectorDelay,maxDelay);
				}
				//arrival delay
				int arrivalDelay = airports.getSoonestArrivalInt(f.arrivalAirport, f.arrivalTimeACES+delay)-(f.arrivalTimeACES+delay);
				//slow down
				if(arrivalDelay <= (longestDuration - nominalDuration)) {
					//this does not seem correct since it would effect sector entry times as well.
					//without sector times it would be correct.
					arrivalDelay = 0;	
				}
				//use max such that the constraint with the most needed delay is met.
				//this should be optimally the least amount of delay.
				maxDelay = java.lang.Math.max(arrivalDelay, maxDelay);
				
				//when max delay==0 it means that the loop can end, because a departure time
				//that causes no delay (does not violate any constraints) has been found.
				if(maxDelay == 0){
					validFlight = true;
					//asserting that in fact none of the constraints add any additional delay
					int shouldBeZero = 0;
					int departureTimeFinal = f.departureTimeACES + delay; 
					shouldBeZero = airports.scheduleDepartureInt(f.departureAirport, f.departureTimeACES+delay, f.departureTimeACES) - (f.departureTimeACES+delay);
					U.Assert(shouldBeZero==0, "errror in scheduling, should be 0");
					f.departureTimeFinal = departureTimeFinal;
					for(SectorAirport s: path){	
						shouldBeZero = sectors.schedule(s.name, s.entryTime + delay, s.entryTime + s.transitTime + delay) - (s.entryTime + delay);
						U.Assert(shouldBeZero==0, "errror in scheduling, should be 0");
					}
					//shouldBeZero = airports.scheduleArrival(f.arrivalAirport, f.arrivalTimeProposed+delay, f.arrivalTimeProposed)-(f.arrivalTimeProposed+delay);
					//U.Assert(zero==0, "errror in scheduling, should be 0"); //should this be back in?
																				 //maybe it is taken out to account for slow down
					int amountAbsorbedSlowingDown = airports.scheduleArrivalInt(f.arrivalAirport, f.arrivalTimeACES+delay, f.arrivalTimeACES)-(f.arrivalTimeACES+delay);
					int arrivalTimeFinal = f.arrivalTimeACES + delay + amountAbsorbedSlowingDown; //in this case should be zero 
					f.arrivalTimeFinal = arrivalTimeFinal;
					f.atcGroundDelay = delay;
					td+=delay; 					
					if(delay!=0) c++;
				} else {
					delay+= maxDelay;
				}
			}
		}
		//*/
		date = new Date();
		System.out.println("done " + dateFormat.format(date));
		Runtime r = Runtime.getRuntime();
		//sectors.printSectors();
		String fcfsdir = "fcfs_output/";
		printSectorTraffic(sectors, workingDirectory+fcfsdir);
		//System.out.println("sectors max cap:");
		//sectors.printSectorMaxCaps();
		//airports.printMinSpacing();
		//airports.printAirports("CYYZ");
		printAirportTrafficCounts(airports,  workingDirectory+fcfsdir);
		airports.validate();
		flights.validateFCFS();
		double totalD = 0;
		for(Flight f: flightList){
			int scheduledArrival = f.arrivalTimeACES;
			int actualArrival = f.arrivalTimeFinal;
			int delay = actualArrival-scheduledArrival;
			totalD += delay;
		}
		
		U.p("total ground delay in hours = " + " " + td/3600000 + " number of flights " + flightList.size() + " flights w delay " + c);
		U.p("total ground delay per flight secs = " + td/(37000*1000));
		U.p("Total Delay = " + totalD/3600000 + " Hours, or " + totalD + " milliseconds");
		System.out.printf("max mem %f total mem %f free mem %f\n", (double)r.maxMemory()/1048576.0, (double)r.totalMemory()/1048576.0, (double)r.freeMemory()/1048576.0);
		//sectors.printSectors();
		Collections.sort(flightList, new flightIDComparator());
		int n = Integer.MAX_VALUE;
		U.p("maximum millisec-days in int "+n/(3600*1000*24.0));
		U.p("FIN!");
		printAirportDelays(flightList,workingDirectory+fcfsdir);
		printFlightDetails(flightList, workingDirectory+fcfsdir);
	}
	
	public void printFlightDetails(ArrayList<Flight> flightList, String dir){
		try {
			FileWriter fstream = new FileWriter(dir + "fcfs_flight_details.csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("FlightId,DepartureAirport,ArrivalAirport,DepartureTimeProposed,DepartureTimeFinal,ArrivalTimeProposed,ArrivalTimeFinal,actualDelay,atcGroundDelay,DelayAbsorbedInAir");
			out.write("\n");
			for(Flight f: flightList) {
				double actualDelay = f.arrivalTimeFinal - f.arrivalTimeACES;
				double delayAbsorbedInAir = actualDelay - f.atcGroundDelay;
				out.write(f.id + "," + f.departureAirport + "," + f.arrivalAirport + "," + f.departureTimeACES + "," 
						+ f.getDepartureTimeFinal() + "," + f.arrivalTimeACES + "," + f.arrivalTimeFinal + "," + f.atcGroundDelay+","+actualDelay+","+delayAbsorbedInAir);
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
				double realDelay = f.arrivalTimeFinal - f.arrivalTimeACES;
				
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

	public void byFlight(String workingDirectory){

		Hashtable<String,ArrayList<Flight>> byAirport = new Hashtable<String,ArrayList<Flight>>();
		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		Collections.sort(flightList, new flightIDComparator());
		
		String[] temp = {
				"KATL",
				"KBOS",
				"KBWI",
				"KCLE",
				"KCLT",
				"KCVG",
				"KDAL",
				"KDCA",
				"KDEN",
				"KDFW",
				"KDTW",
				"KEWR",
				"KFLL",
				"KHOU",
				"KHPN",
				"KIAD",
				"KIAH",
				"KJFK",
				"KLAS",
				"KLAX",
				"KLGA",
				"KMCO",
				"KMDW",
				"KMEM",
				"KMHT",
				"KMIA",
				"KMKE",
				"KMSP",
				"KOAK",
				"KORD",
				"KPDX",
				"KPHL",
				"KPHX",
				"KSAN",
				"KSAT",
				"KSEA",
				"KSFO",
				"KSLC",
				"KSTL",
		"KTEB"};

		ArrayList<String> arports = new ArrayList<String>();
		for(String r:temp){ arports.add(r); }

		for (Flight f: flightList){
			if( arports.contains(f.arrivalAirport)){
				ArrayList<Flight> l = byAirport.get(f.arrivalAirport);
				if(l==null){
					l =  new ArrayList<Flight>();
					byAirport.put(f.arrivalAirport, l);
				}
				l.add(f);
			}
		}

		for( ArrayList<Flight> s: byAirport.values()){

			Collections.sort(s, new flightIDComparator());
			try{
				// Create file 
				FileWriter fstream = new FileWriter(workingDirectory+"by flight/"+s.get(0).arrivalAirport+"_flight_absorption_data.csv");
				BufferedWriter out = new BufferedWriter(fstream);
				out.write("**flightid,total delay\n");
				for (Flight f: s){
					out.write(f.id +","+ f.atcGroundDelay/60000.0 +"\n");
					/*
					for(SectorAirport s: f.path){
						out.write(f.id +","+ (s.entryTime+delay)+","+(s.entryTime+s.transitTime+delay)+"," + s.transitTime+","+ s.raw+"\n");
					}
					 */				
				}
				//Close the output stream
				out.close();
			}catch (Exception e){//Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}

		}	
	}

}
