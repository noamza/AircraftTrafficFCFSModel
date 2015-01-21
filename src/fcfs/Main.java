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
		//java -Xms3024M -Xmx3024M -jar FCFSDelaySensitivity.jar /Users/nalmog/Desktop/Scheduler/ 3
		//java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("DDDHHmmss"); 
		//FCFSArrival f = new FCFSArrival(); f.schedule(dateFormat.format(new java.util.Date()));
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
		s.allCFR = true;
		s.noneCFR = false;
		s.schedule();
		s.allCFR = false;
		s.noneCFR = true;
		s.schedule();
		s.limitedCFRUncertainty = true;
		s.allCFR = false;
		s.noneCFR = false;
		s.schedule();
		s.allCFR = true;
		s.noneCFR = false;
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
		airports.loadFromAces(file);

		return airports;
	}

}




