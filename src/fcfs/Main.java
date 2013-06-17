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
class fl implements Comparable<fl>
{
	int time = -1;

	char id;

	fl(int t, char d)
	{
		time = t;
		id = d;
	}

	public int compareTo(fl o)
	{
		return (time - o.time);
	}

	void print()
	{
		System.out.printf("	%s %d\n", id, time);
	}
}

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
	public static final String DEFAULT_WORKING_DIRECTORY = "/Users/hvhuynh/Desktop/scheduler/inputs/";

	/**
	 * 
	 */
	public static final String DEFAULT_OUTPUT_DIRECTORY = "../outputs/departureArrivalFCFS/";

	/**
	 * Sector crossings data
	 */
	private static final String DEFAULT_SECTOR_CROSSING_FILE = "job_24_sector_transitTime_takeoffLanding_35h_1.csv";

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

	public static void p(String s)
	{
		System.out.println(s);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		File flightFile = new File(workingDirectory, sectorCrossingFileName);
		File sectorFile = new File(workingDirectory, sectorFileName);
		File airportFile = new File(workingDirectory, runwayFileName);

		Flights flights = loadFlightData(flightFile.getAbsolutePath());
		Sectors sectors = loadSectorData(sectorFile.getAbsolutePath());
		Airports airports = loadAirportData(airportFile.getAbsolutePath());

		Scheduler scheduler = new DepartureArrivalFCFS_basic(flights, airports, sectors);
		ArrayList<Flight> flightList = scheduler.schedule();
		scheduler.printResults(flightList, new File(workingDirectory, outputDirectory).getAbsolutePath());

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

		/*
		 * java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("DDDHHmmss"); FCFSArrival
		 * f = new FCFSArrival(); f.schedule(dateFormat.format(new Date()));
		 */
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
	
	static void learning()
	{

		/*
		 * ArrayList are by reference, change the list, or item in the list.
		 */

		/*
		 * TreeSet<Integer> test = new TreeSet<Integer>(); Integer x = 1, y = 7, n = 3, m = 4;
		 * test.add(m);test.add(y);test.add(n);test.add(x); y = 2; //
		 */

		// *
		TreeSet<fl> test = new TreeSet<fl>();
		fl x = new fl(1, 'x'), y = new fl(3, 'y'), n = new fl(5, 'n'), m = new fl(7, 'm');// y.time
																														// =2;
		// test.add(new fl(20, 't'));
		// test.add(new fl(21, 'g'));
		test.add(m);
		test.add(y);
		test.add(n);
		test.add(x);
		for (fl c : test)
		{
			c.print();
		}
		Main.p("");
		test.remove(y);
		y.time = 6;
		for (fl c : test)
		{
			c.print();
		}
		Main.p("");
		test.add(y);
		for (fl c : test)
		{
			c.print();
		}
		Main.p(""); // y.time = 55; // y.id = 'l';
		test.remove(m);
		m.time = 8;
		test.add(m);
		test.add(y);
		test.add(y);
		fl z = new fl(4, 'z');
		test.add(z);
		for (fl c : test)
		{
			c.print();
		}
		Main.p("");
		test.remove(z);
		z.time = 10;
		test.add(z);
		for (fl c : test)
		{
			c.print();
		}
		Main.p("");
		test.remove(n);
		n.time = 11;
		test.add(n);
		for (fl c : test)
		{
			c.print();
		}
		Main.p("");
		test.remove(y);
		y.time = 12;
		test.add(y);
		for (fl c : test)
		{
			c.print();
		}
		Main.p("");
		test.remove(m);
		m.time = 13;
		test.add(m);
		for (fl c : test)
		{
			c.print();
		}
		Main.p("");

		// */
		/*
		 * NOTES no = new NOTES(); ArrayList<t> b = new ArrayList<t>(); b.add(new t(1)); t d = new
		 * t(2); b.add(d); //b.add(d); for( t c: b) p("M v = "+c.x);p("m*"); no.a(b); p("d: " + d.x);
		 * //no.a.get(1).x = 99; d.x = 22; for( t c: b) p("M v = "+c.x);p("m*"); //d.x = 222; d =
		 * no.a.get(0); b.add(d); d.x = 10; //b.add(new t(4)); no.print(); Main.p("***"); no.mp();
		 * no.m = d; no.mp(); no.m2(); no.mp(); p("d: " + d.x); t dd = new t(99); d = new t(9); t r =
		 * d; d.x = 3;//d = dd;//new t(8); p("d: " + d.x); p("r: " + r.x); no.mp(); //d.x = 11; //d =
		 * no.a.get(1); //d.x = 2; //no.print(); //
		 */
	}

}

/*
 * int i = 10; double b = 7; System.out.println(i); System.out.println(b/i);
 */

/*
 * Flights flights = new Flights(); Sectors sectors = new Sectors(); Airports airports = new
 * Airports(); String workingDirectory =
 * "C:\\Users\\Noam Almog\\Desktop\\scheduler\\scheduler\\atl_data\\"; //String workingDirectory =
 * "/Users/nalmog/Desktop/scheduler/atl_data/"; //flights.loadFromAces(workingDirectory+
 * "job_611_sector_transitTime_takeoffLanding_35h_500_wtracon_flts.csv"); //flights.printFlights();
 * sectors.loadFromAces(workingDirectory+"SectorList_AllUconstrained_b71_WxRerFds_Vor18High.csv");
 * sectors.printSectors(); airports.loadFromAces(workingDirectory+
 * "AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv"); airports.printAirports(); //
 * [1-7] [2-5] SectorTree s = new SectorTree("",3); s.insertFlight(2, 5); s.insertFlight(6, 9);
 * s.insertFlight(0, 3); s.insertFlight(4, 8); s.insertFlight(1, 7); // //s.print();
 * //s.insertFlight(1, 9); /* s.insertFlight(5, 8); //8 0 s.insertFlight(4, 6); //6 2
 * s.insertFlight(5, 8); //5 3 s.insertFlight(4, 5); s.insertFlight(4, 5); s.insertFlight(4, 5); //4
 * 5 s.insertFlight(3, 5); //3 1 //
 */
/*
 * System.out.printf("capacity at %d is %d\n", 2, s.getCapacity(2));
 * System.out.printf("capacity at %d is %d\n", 4, s.getCapacity(4));
 * System.out.printf("capacity at %d is %d\n", 5, s.getCapacity(5));
 * System.out.printf("capacity at %d is %d\n", 9, s.getCapacity(9)); //s.test(); //s.testWindows();
 * //s.printWindows(); //s.testCapacities(); s.test(); s.print(); s.printWindows();
 * System.out.println("s.getSoonestSlot(2, 5)" + s.getSoonestSlot(2, 4));
 */
