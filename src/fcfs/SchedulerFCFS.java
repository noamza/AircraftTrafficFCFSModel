package fcfs;
import java.util.*;
import java.io.*;

public class SchedulerFCFS {

	Flights flights; Sectors sectors; Airports airports;

	public void init(){



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
		airports.loadFromAces(workingDirectory+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
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
		//*
		//    Sort flights by Departure Time.
		Collections.sort(flightList, new flightDepTimeComparator());
		
		for (Flight f: flightList){
			boolean validFlight = false;
			
			int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
			int fastestDuration =  (int)(nominalDuration/(1+speedUp));
			int longestDuration = (int)(nominalDuration/(1-slowDown));
			
			ArrayList<SectorAirport> path = f.path;
			int delay = 0;
			
			while(!validFlight){
				//Main.p("delay:: " + delay);
				int maxDelay = 0;
				int departureDelay = airports.getSoonestDeparture(f.departureAirport, f.departureTimeProposed+delay)-(f.departureTimeProposed+delay);
				maxDelay = java.lang.Math.max(departureDelay,maxDelay);
				
				for(SectorAirport s: path){	
					//Main.p("delay " + delay);
					int sectorDelay = sectors.getSoonestSlot(s.name, s.entryTime+delay, s.entryTime+delay + s.transitTime)-(s.entryTime+delay);
					maxDelay = java.lang.Math.max(sectorDelay,maxDelay);
				}
				
				int arrivalDelay = airports.getSoonestArrival(f.arrivalAirport, f.arrivalTimeProposed+delay)-(f.arrivalTimeProposed+delay);
				
				boolean here = false;
				
				//slow down
				int realDelay = 0;
				if(arrivalDelay <= (longestDuration - nominalDuration)) {
					//if(maxDelay==0&&arrivalDelay!=0){Main.p("is here " + arrivalDelay);}
					if(maxDelay==0&&arrivalDelay!=0){
						here = true; 
						realDelay = arrivalDelay;
					}
					arrivalDelay = 0;
					
				}
				
				maxDelay = java.lang.Math.max(arrivalDelay, maxDelay);
				//if(here){Main.p(f.id+" " + realDelay);}
				//{Main.p("maxdelay here: "+ maxDelay + " arrdelay " + arrivalDelay + " delay " + delay );}
				//if(here)
				
				if(maxDelay == 0){
					validFlight = true;
					//System.out.println("found a flight at: " + delay);
					int zero = 0;
					int departureTimeFinal = f.departureTimeProposed + delay; 
					zero = airports.scheduleDeparture(f.departureAirport, f.departureTimeProposed+delay, f.departureTimeProposed) - (f.departureTimeProposed+delay);
					f.departureTimeFinal = departureTimeFinal;
					
					Main.Assert(zero==0, "errror in scheduling, should be 0");
					
					for(SectorAirport s: path){	
						zero = sectors.schedule(s.name, s.entryTime + delay, s.entryTime + s.transitTime + delay) - (s.entryTime + delay);
						Main.Assert(zero==0, "errror in scheduling, should be 0");
					}
					
					
					zero = airports.scheduleArrival(f.arrivalAirport, f.arrivalTimeProposed+delay, f.arrivalTimeProposed)-(f.arrivalTimeProposed+delay);

					int arrivalTimeFinal = f.arrivalTimeProposed + delay + zero;
					f.arrivalTimeFinal = arrivalTimeFinal;
					//f.print();
					//Main.Assert(zero==0, "errror in scheduling, should be 0");

					f.atcGroundDelay = delay;

					td+=delay; 
					
					if(delay!=0) c++;

				} else delay+= maxDelay;
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
			int scheduledArrival = f.arrivalTimeProposed;
			int actualArrival = f.arrivalTimeFinal;
			int delay = actualArrival-scheduledArrival;
			totalD += delay;
		}
		
		Main.p("total ground delay in hours = " + " " + td/3600000 + " number of flights " + flightList.size() + " flights w delay " + c);
		Main.p("total ground delay per flight secs = " + td/(37000*1000));
		Main.p("Total Delay = " + totalD/3600000 + " Hours, or " + totalD + " milliseconds");
		System.out.printf("max mem %f total mem %f free mem %f\n", (double)r.maxMemory()/1048576.0, (double)r.totalMemory()/1048576.0, (double)r.freeMemory()/1048576.0);
		//sectors.printSectors();
		//*
		Collections.sort(flightList, new flightIDComparator());

		/*
		try{
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+"fcfs_out.csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("**flightid,entryTime(milliseconds),exitTime(milliseconds),transitTime(milliseconds),upperStreamSector,currentSector,downStreamSector\n");
			for (Flight f: flightList){
				int delay = f.atcGroundDelay;
				for(SectorAirport s: f.path){
					out.write(f.id +","+ (s.entryTime+delay)+","+(s.entryTime+s.transitTime+delay)+"," + s.transitTime+","+ s.raw+"\n");
				}				
			}
			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		*/
		
		//byFlight(workingDirectory);

		int n = Integer.MAX_VALUE;
		Main.p(""+n/(3600*1000*24.0));
		Main.p("FIN!");
		
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
