/**
 * 
 */

package fcfs;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * @author Noam Almog
 * 
 * @precondition Specify the environment variable -Dworking.directory=location. The working directory
 * 				  must exist and must contain the input data file paths (see below).
 * @precondition Specify the environment variable -Doutput.directory=location. The output directory
 * 				  must exist. It will be used to print the results of FCFS.
 * @precondition Specify -Dsector.crossing.file=pathname (relative to working directory). This is the
 *               flight sector crossing and transit time data.
 * @precondition Specify -Dsector.file=pathname relative to working directory. This is the sector name
 *               and capacity file.
 * @precondition Specify -Drunway.file=pathname relative to working directory. This is the runway name
 *               and acceptance rate file. The rates can be time dependent.
 */


/**
 * @todo get inputs from command line
 */
/**
 * @author nalmog
 */
public class Main
{

	/**
	 * 
	 */
	public static final String DEFAULT_WORKING_DIRECTORY = "/Users/nalmog/Desktop/scheduler/inputs/";

	/**
	 * 
	 */
	public static final String DEFAULT_OUTPUT_DIRECTORY = "../outputs/departureArrivalFCFS/";

	/**
	 * Sector crossings data
	 */
	private static final String DEFAULT_SECTOR_CROSSING_FILE = //"job_24_sector_transitTime_takeoffLanding_35h_1.csv";
															   "clean_job.csv"; //Kee Gano Noam Delay Sensitivity paper

	/**
	 * Sector names and capacities
	 */
	private static final String DEFAULT_SECTOR_FILE = "SectorList_YZ2007May.csv";

	/**
	 * Runways and capacities/acceptance rates
	 */
	private static final String DEFAULT_RUNWAY_FILE = "AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv";

	/**
	 * 
	 */
	private static String workingDirectory = System.getProperty("working.directory", DEFAULT_WORKING_DIRECTORY);

	/**
	 * currently defined relative to #workingDirectory.
	 */
	private static String outputDirectory = System.getProperty("output.directory", DEFAULT_OUTPUT_DIRECTORY);

	private static String sectorCrossingFileName = System.getProperty("sector.crossing.file", DEFAULT_SECTOR_CROSSING_FILE);

	private static String sectorFileName = System.getProperty("sector.file", DEFAULT_SECTOR_FILE);

	private static String runwayFileName = System.getProperty("runway.file", DEFAULT_RUNWAY_FILE);

	public static boolean Assert(boolean a, String expression)
	{
		if (!a)
		{
			throw new java.lang.Error("FAILED: " + expression);
		}
		return a;
	}

	public static boolean Assert(boolean a)
	{
		if (!a)
		{
			throw new java.lang.Error("FAILED: Assert()");
		}
		return a;
	}

	public static void p(Object s)
	{
		System.out.println(s.toString());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		File flightFile = new File(workingDirectory, sectorCrossingFileName);
		File sectorFile = new File(workingDirectory, sectorFileName);
		File airportFile = new File(workingDirectory, runwayFileName);

		Flights  flights  = loadFlightData(flightFile.getAbsolutePath());
		Sectors  sectors  = loadSectorData(sectorFile.getAbsolutePath());
		Airports airports = loadAirportData(airportFile.getAbsolutePath());
	   
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
		// Main.p(Action.scheduleAtPDT+"");
		// f.schedule(Action.scheduleByArrival);
		// Assert(false,"er");
		// learning();
		/*
		 * FCFSArrival f = new FCFSArrival(); //
		 */

		// TEST uncertainty

		// new AirportTree("test").testGaps();
		
		//Noam
        // display new properties
        //System.getProperties().list(System.out);
		noamMain();
		

	}
	
	public static void noamMain()
	{
		//java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("DDDHHmmss"); 
		//FCFSArrival f = new FCFSArrival(); f.schedule(dateFormat.format(new java.util.Date()));
		U.start();
		//U.pf("freeze horizon(min),scheduling Horizon(min),arrival normaalized for total(min),arrival airport (min),ground(min),air(min)\n");
		FCFSCoupledWUncertainty s = new FCFSCoupledWUncertainty(); s.schedule();
		for(int sh = 0; sh <= 60; sh += 5){
			for(int fh = 30; fh <= 120; fh += 5){
				//FCFSCoupledWUncertainty s = new FCFSCoupledWUncertainty(); s.schedule(sh, fh);
			}
		}
		U.end();
		//new AirportTree("TESTING").test1();
		
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
	public static Sectors loadSectorData(String file)
	{
		Sectors sectors = new Sectors();
		sectors.loadFromAces(file);

		return sectors;
	}
	
	public static Airports loadAirportData(String file)
	{
		Airports airports = new Airports();
		airports.loadFromAces(file);

		return airports;
	}

}




