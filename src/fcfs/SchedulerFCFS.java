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
		//flights.loadTaxiOffset(workingDirectory+"AirportTaxi.csv");
		//flights.loadFlightsFromAces(workingDirectory+"job_9_sector_transitTime_takeoffLanding_35h_1.csv", true );
		
		//flights.loadFlightsFromAces(workingDirectory + "job_KSFO_arrs.csv", true);
		//flights.loadFlightsFromAces(workingDirectory+"TEST_fcfsj.csv");
		//flights.loadFromAces(workingDirectory+"recapture_chunki_from_clean.csv");
		//flights.loadFromAces(workingDirectory+"fcfsj2.csv");
		//flights.printFlights();
		
		sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May.csv");
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May_MAP9999.csv");
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
		//h.put("hey", 8);
		//h.get("hey");
		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		//Collections.sort(flightList, new flightIDComparator());
		//flights.printFlightsFull();

		double td = 0; int c = 0;
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

				//slow down
				if(arrivalDelay <= (longestDuration - nominalDuration)) {
					arrivalDelay = 0;
				}
			
				maxDelay = java.lang.Math.max(arrivalDelay, maxDelay);
				
				if(maxDelay == 0){
					validFlight = true;
					//System.out.println("found a flight at: " + delay);
					int zero = 0;
					int departureTimeFinal = f.departureTimeProposed + delay; 
					zero = airports.scheduleDeparture(f.departureAirport, f.departureTimeProposed+delay, f.departureTimeProposed) - (f.departureTimeProposed+delay);
					f.departureTimeFinal = departureTimeFinal;
					
					//Main.p("zero 0")
					Main.Assert(zero==0, "errror in scheduling, should be 0");
					
					for(SectorAirport s: path){	
						zero = sectors.schedule(s.name, s.entryTime + delay, s.entryTime + s.transitTime + delay) - (s.entryTime + delay);
						Main.Assert(zero==0, "errror in scheduling, should be 0");
					}
					
					int arrivalTimeFinal = f.arrivalTimeProposed + delay;
					zero = airports.scheduleArrival(f.arrivalAirport, f.arrivalTimeProposed+delay, f.arrivalTimeProposed)-(f.arrivalTimeProposed+delay);
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
		
		try {
			FileWriter fstream = new FileWriter(workingDirectory+"fcfs_sector_traffic.csv");
			BufferedWriter sector_out = new BufferedWriter(fstream);
			sectors.printSectorsToFile(sector_out);
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
			//System.out.println("sectors max cap:");
		//sectors.printSectorMaxCaps();
		//airports.printMinSpacing();
		airports.printAirports("KBOS");

		Main.p("total delay in hours = " + td/3600000 + " number of flights " + flightList.size() + " flights w delay " + c);
		Main.p("total delay per flight secs = " + td/(37000*1000));
		System.out.printf("max mem %f total mem %f free mem %f\n", (double)r.maxMemory()/1048576.0, (double)r.totalMemory()/1048576.0, (double)r.freeMemory()/1048576.0);
		//sectors.printSectors();
		//*
		Collections.sort(flightList, new flightIDComparator());

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
		//*/
		//byFlight(workingDirectory);

		int n = Integer.MAX_VALUE;
		Main.p(""+n/(3600*1000*24.0));
		Main.p("FIN!");
		/*
		Hashtable<String, Double> airportDelay = new Hashtable<String,Double>();
		for(Flight f: flightList){
			if(airportDelay.get(f.departureAirport)==null){
				airportDelay.put(f.departureAirport, 0.0);
			}				
			airportDelay.put(f.departureAirport, airportDelay.get(f.departureAirport) + f.atcGroundDelay);
		}
		Enumeration<String> enumKey = airportDelay.keys();
		while(enumKey.hasMoreElements()) {
			String k = enumKey.nextElement();
			Double val = airportDelay.get(k);
			if(val != 0.0) { val /= 60000;}
			//System.out.println(k + "," + val);
		}*/
		/*
		System.out.println("FlightId,DepartureAirport,ArrivalAirport,DepartureTimeProposed,ArrivalTimeProposed,atcGroundDelay");
		for(Flight f: flightList) {
			
			if(f.departureAirport.equals("KJFK")) {
				System.out.println(f.id + "," + f.departureAirport + "," + f.arrivalAirport + "," + f.departureTimeProposed + "," + f.arrivalTimeProposed + "," + f.atcGroundDelay);
			}
		}*/
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
