/**
 * 
 */

package fcfs;


/**
 * @author Noam Almog
 * This is the entry point to the program. 
 * All of the configuration, directory/file names, are found in the U.java class. 
 * 
 * @precondition 
 * - The directory and file names are correct in the U class.				  
 */

public class Main
{
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		/*
		File flightFile = new File(workingDirectory, sectorCrossingFileName);
		File sectorFile = new File(workingDirectory, sectorFileName);
		File airportFile = new File(workingDirectory, runwayFileName);
		
		Flights  flights  = loadFlightData(flightFile.getAbsolutePath());
		Sectors  sectors  = loadSectorData(sectorFile.getAbsolutePath());
		Airports airports = loadAirportData(airportFile.getAbsolutePath());
	   */
		//HUU'S WORK
		//Scheduler scheduler = new DepartureArrivalFCFS_basic(flights, airports, sectors);
		//ArrayList<Flight> flightList = scheduler.schedule();
		//scheduler.printResults(flightList, new File(workingDirectory, outputDirectory).getAbsolutePath());

		// MSR: These are deprecated
//		DepartureArrivalFCFS_basic scheduler = new DepartureArrivalFCFS_basic();
//		scheduler.scheduleFCFS(workingDirectory, outputDirectory);

		// SchedulerFCFS s = new SchedulerFCFS();
		// s.init();

		// DepartureArrivalFCFS scheduler = new DepartureArrivalFCFS();
		// scheduler.scheduleFCFS();

		// FCFSFlexibleSpeed f = new FCFSFlexibleSpeed();
		// f.schedule(Action.scheduleAtPDT);
		// f.schedule(Action.scheduleAtPushback);
		// U.p(Action.scheduleAtPDT+"");
		// f.schedule(Action.scheduleByArrival);

		
		//Noam
        // display new properties
        //System.getProperties().list(System.out);
		noamMain(args);
		

	}
	
	public static void noamMain(String[] args)
	{
		//sample command line run:
		//java -Xms3024M -Xmx3024M -jar FCFSDelaySensitivity.jar /Users/nalmog/Desktop/Scheduler/ 3
		
		U.start();

		FCFSCoupledWUncertainty s;
		if(args !=null && args.length > 0){
			U.workingDirectory = args[0];
			s = new FCFSCoupledWUncertainty(Integer.parseInt(args[1]));
		} else {
			s = new FCFSCoupledWUncertainty(); 
			s.montecarlo = 2;
		}
		s.limitedCFRUncertainty = false;
		s.schedule();
		/*
		s.allCFR = true; s.limitedCFRUncertainty = true; s.noneCFR = false;
		s.schedule();;
		s.schedule();
		*/
		U.end();
	}

	/**
	 * @param file
	 * @return
	 */
	public static Flights loadFlightData(String file)
	{
		Flights flights = new Flights();
		flights.loadFlightsFromAces(file, true);

		return flights;
	}
		
	/**
	 * @param file
	 * @return
	 */
	/*
	public static Sectors loadSectorData(String file)
	{
		Sectors sectors = news Sector();
		sectors.loadFromAces(file);
		return sectors;
	}
	*/
	
	public static Airports loadAirportData(String file)
	{
		Airports airports = new Airports();
		airports.loadCapacitiesFromAces(file);

		return airports;
	}

}




