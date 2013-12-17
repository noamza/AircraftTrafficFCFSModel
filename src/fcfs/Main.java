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
		
		//173 different departure airports into IAH
		//808 flights into IAH
		//16 flights DFW to IAH
		//Sim day: 1/3/2011 length 33hrs
		//slow down : 2.3 min speed 1.0 min
		/*
	CFR flight arrival times (DFW to IAH)
   ID  D  H  M  S
	4: 3:13:53:21
 3657: 3:22:44:05
 4466: 3:23:58:29
 5408: 4:00:09:26
 9545: 4:02:35:15
11460: 4:03:09:24
11955: 4:04:05:11
14398: 4:04:47:15
16637: 4:04:50:25
19684: 4:06:11:15
22122: 4:07:46:26
25547: 4:08:06:13
28567: 4:09:43:13
33722: 4:11:14:36
36889: 4:12:09:15
36342: 4:13:05:34
		  
	Internal departures pre freeze horizon	  
	30 freeze horizon, internal departures 0 from airports 0
	35 freeze horizon, internal departures 0 from airports 0
	40 freeze horizon, internal departures 16 from airports 6
	45 freeze horizon, internal departures 82 from airports 13
	50 freeze horizon, internal departures 95 from airports 18
	55 freeze horizon, internal departures 127 from airports 22
	60 freeze horizon, internal departures 152 from airports 25
	65 freeze horizon, internal departures 166 from airports 27
	70 freeze horizon, internal departures 195 from airports 33
	75 freeze horizon, internal departures 211 from airports 35
	80 freeze horizon, internal departures 227 from airports 40
	85 freeze horizon, internal departures 245 from airports 42
	90 freeze horizon, internal departures 265 from airports 46
	95 freeze horizon, internal departures 273 from airports 49
	100 freeze horizon, internal departures 298 from airports 54
	105 freeze horizon, internal departures 309 from airports 56
	110 freeze horizon, internal departures 337 from airports 62
	115 freeze horizon, internal departures 370 from airports 70
	120 freeze horizon, internal departures 417 from airports 81
		 
3:00(0) = 
3:01(0) = 
3:02(0) = 
3:03(0) = 
3:04(0) = 
3:05(0) = 
3:06(0) = 
3:07(0) = 
3:08(0) = 
3:09(0) = 
3:10(0) = 
3:11(0) = 
3:12(0) = 
3:13(1) = 53,
3:14(2) = 22,48,
3:15(1) = 43,
3:16(3) = 14,05,38,
3:17(3) = 18,03,36,
3:18(2) = 05,21,
3:19(1) = 45,
3:20(0) = 
3:21(1) = 56,
3:22(19) = 55,02,11,01,35,36,09,50,42,41,44,32,56,39,43,44,36,49,52,
3:23(45) = 45,01,45,07,37,54,43,59,50,29,38,43,53,49,22,34,36,39,46,49,55,48,48,54,47,01,25,55,42,08,55,34,21,44,57,52,27,53,54,26,42,38,47,53,58,
4:00(44) = 23,13,13,23,14,09,14,02,05,04,00,06,32,13,33,14,10,57,30,08,57,08,05,11,05,46,21,29,56,53,00,50,35,23,00,09,35,56,48,22,16,49,37,56,
4:01(53) = 01,30,01,08,19,03,10,37,12,31,00,06,40,07,40,04,10,14,57,06,17,26,34,39,22,24,00,19,14,23,30,22,45,42,32,10,50,03,35,27,25,03,16,21,14,28,32,31,17,56,28,47,58,
4:02(65) = 17,06,13,02,14,10,21,36,18,30,18,11,07,33,29,00,03,31,30,21,13,29,28,29,23,50,14,58,02,32,24,26,00,18,54,53,55,28,59,38,02,24,44,50,08,57,00,10,12,21,36,50,17,49,48,26,52,57,50,33,35,31,49,58,54,
4:03(40) = 47,55,00,28,07,03,41,30,45,36,05,51,39,11,56,13,42,39,58,55,03,47,14,42,28,59,35,11,56,08,46,09,38,10,20,33,39,40,49,52,
4:04(52) = 19,29,14,19,05,31,16,09,04,26,32,04,42,07,05,29,39,52,09,17,57,51,18,52,58,00,53,28,56,13,01,58,04,12,42,39,13,14,05,35,02,22,32,19,22,34,47,45,50,50,57,53,
4:05(49) = 52,34,35,14,29,03,30,11,16,01,35,37,29,22,12,25,09,36,28,02,18,06,14,13,52,41,58,55,26,59,43,51,38,10,59,43,30,54,08,13,42,36,27,33,39,56,46,59,54,
4:06(60) = 47,44,35,17,26,44,34,08,04,23,15,10,32,24,42,21,39,31,15,37,23,16,28,50,30,29,46,53,12,56,11,49,42,46,58,50,38,53,45,10,15,03,21,01,15,52,11,06,26,46,50,55,29,50,47,47,45,54,53,59,
4:07(25) = 03,03,49,45,12,06,01,52,19,35,40,47,03,35,42,17,48,54,32,07,11,02,15,57,46,
4:08(67) = 28,44,41,19,11,29,49,09,07,42,29,11,16,12,33,58,01,25,58,15,20,17,57,50,33,53,43,41,59,19,33,13,13,13,29,18,24,40,32,21,47,47,29,22,17,39,50,37,04,44,49,30,19,46,59,37,06,47,35,48,57,30,25,59,54,45,59,
4:09(55) = 01,10,46,35,15,13,51,40,24,20,01,12,23,05,14,28,35,26,09,18,52,12,08,22,33,25,25,16,17,30,10,29,55,31,28,45,50,55,56,57,57,41,03,02,41,45,16,25,51,34,43,45,39,43,57,
4:10(56) = 35,43,53,19,20,09,28,40,39,01,17,23,31,06,00,00,02,30,04,11,01,05,03,09,00,20,50,08,05,13,33,19,53,11,01,51,07,56,21,00,01,39,44,57,15,12,03,18,04,12,39,36,14,16,28,29,
4:11(55) = 05,51,01,14,17,10,32,18,08,33,04,59,52,11,27,23,11,39,06,45,50,41,42,59,41,44,32,37,28,13,20,38,25,35,04,39,38,35,51,37,39,25,49,05,59,50,25,14,28,48,59,58,46,51,53,
4:12(48) = 18,44,32,25,41,18,19,00,19,03,19,03,14,01,26,34,27,10,43,05,10,02,29,28,16,37,27,29,52,02,19,21,08,10,20,13,30,32,31,18,09,17,16,57,15,33,42,57,
4:13(28) = 17,01,26,09,27,16,07,17,52,04,49,02,36,21,48,35,47,10,16,05,27,04,56,48,03,05,08,35,
4:14(9) = 21,28,41,32,43,50,51,47,41,
4:15(10) = 34,11,28,13,05,36,50,42,04,51,
4:16(8) = 20,20,16,33,14,45,40,42,
4:17(2) = 06,50,
4:18(1) = 28,
4:19(1) = 18,
4:20(0) = 
4:21(2) = 05,03,
4:22(0) = 
4:23(0) = 
		 
		 
30 freeze horizon, interal departures 0 from airports 0
35 freeze horizon, interal departures 0 from airports 0
40 freeze horizon, interal departures 16 from airports 6
KIAH:1, KBPT:1, KSAT:1, KSGR:1, KGRK:1, KAUS:11, 
45 freeze horizon, interal departures 82 from airports 13
KGRK:1, KDFW:16, KVCT:2, KSGR:1, KSAT:11, KCLL:7, KDAL:7, KBPT:5, KAUS:12, KLCH:6, KIAH:1, KAEX:2, KCRP:11, 
50 freeze horizon, interal departures 95 from airports 18
KBPT:5, KACT:1, KIAH:1, KLFT:5, KAUS:13, KDAL:7, KSAT:11, KCRP:11, KLCH:6, KCLL:7, KSGR:1, KFTW:1, KEFD:1, KAEX:2, KSHV:3, KVCT:2, KDFW:16, KGRK:2, 
55 freeze horizon, interal departures 127 from airports 22
KMSY:5, KBPT:5, KIAH:1, KACT:5, KLFT:6, KAUS:13, KDAL:7, KHRL:6, KSAT:11, KCRP:11, KLCH:6, KCLL:7, KSGR:1, KBTR:8, KFTW:1, KEFD:1, KAEX:4, KSHV:3, KVCT:3, KDFW:16, KGRK:6, KBAZ:1, 
60 freeze horizon, interal departures 152 from airports 25
KMFE:6, KMSY:10, KBPT:5, KIAH:1, KACT:5, KLFT:8, KAUS:13, KDAL:7, KHRL:6, KSAT:11, KCRP:11, KCLL:7, KLCH:6, KSGR:1, KBTR:9, KFTW:1, KLRD:5, KEFD:1, KAEX:6, KSHV:3, KVCT:3, KDFW:16, KGRK:6, KBAZ:1, KBRO:4, 
65 freeze horizon, interal departures 166 from airports 27
KMFE:6, KMSY:10, KBPT:5, KIAH:1, KACT:5, KLFT:8, KDAL:7, KAUS:13, KHRL:6, KSAT:11, KCRP:11, KCLL:7, KLCH:6, KSGR:1, KBTR:9, KJAN:5, KFTW:1, KLRD:5, KEFD:1, KAEX:6, KGRK:6, KSHV:3, KVCT:3, KDFW:16, KBAZ:1, KBRO:4, KOKC:9, 
70 freeze horizon, interal departures 195 from airports 33
KMFE:6, KMSY:10, KBPT:5, KIAH:1, KACT:5, KMOB:2, KLFT:8, KLIT:5, KDAL:7, KAUS:13, KHRL:6, MMMY:11, KMAF:5, KSAT:11, KCRP:11, KCLL:7, KLCH:6, KSGR:1, KBTR:9, KJAN:5, KFTW:1, KPWA:1, KLRD:5, KEFD:1, KAEX:6, KGRK:6, KSHV:3, KVCT:3, KDFW:16, KBAZ:1, KGPT:5, KBRO:4, KOKC:9, 
75 freeze horizon, interal departures 211 from airports 35
KMFE:6, KMSY:10, KBPT:5, KIAH:1, KACT:5, KMOB:5, KLFT:8, KLIT:5, KDAL:7, KAUS:13, KHRL:6, MMMY:11, KMAF:6, KSAT:11, KCRP:11, KCLL:7, KLCH:6, KSGR:1, KBTR:9, KJAN:5, KFTW:1, KPWA:1, KLRD:5, KEFD:1, KAEX:6, KGRK:6, KSHV:4, KVCT:3, KDFW:16, KMLU:4, KBAZ:1, KGPT:5, KBRO:4, KTUL:7, KOKC:9, 
80 freeze horizon, interal departures 227 from airports 40
KGRK:6, MMIO:1, KVCT:3, KSAT:11, KSGR:1, KDFW:16, KBPT:5, KMLU:4, KJAN:5, KPNS:5, KEFD:1, KLFT:8, KLIT:5, KMFE:6, KFTW:1, KLCH:6, KIAH:1, KMOB:5, KCLL:7, KCRP:11, KAMA:1, KPWA:1, KOKC:9, KXNA:4, KLRD:5, KACT:5, KTUL:8, KBRO:4, KGPT:5, MMMY:11, KAUS:13, KLBB:4, KSHV:4, KAEX:6, KMSY:10, KBTR:9, KHRL:6, KDAL:7, KMAF:6, KBAZ:1, 
85 freeze horizon, interal departures 245 from airports 42
KGRK:6, MMIO:1, KVCT:3, KSAT:11, KSGR:1, KDFW:16, KBPT:5, KMLU:4, KJAN:5, KPNS:5, KEFD:1, KLFT:8, KLIT:5, KMFE:6, KFTW:1, KIAH:1, KLCH:6, KMOB:5, KCLL:7, KCRP:11, KPWA:1, KAMA:7, KOKC:9, KXNA:4, KLRD:5, KACT:5, KTUL:8, KMEM:7, KBRO:4, KGPT:5, MMMY:11, KAUS:13, KLBB:4, KSHV:4, KAEX:6, KVPS:4, KMSY:11, KBTR:9, KHRL:6, KDAL:7, KMAF:6, KBAZ:1, 
90 freeze horizon, interal departures 265 from airports 46
KGRK:6, MMIO:1, KVCT:3, KSAT:11, KSGR:1, KDFW:16, KTYR:4, KBPT:5, KMLU:4, KJAN:5, KPNS:5, KEFD:1, KLFT:8, KLIT:5, KMFE:6, KFTW:1, KIAH:1, KLCH:6, KMOB:5, KCLL:7, KCRP:11, KPWA:1, KAMA:7, KOKC:9, KXNA:4, MMTM:1, KLRD:5, KACT:5, KTUL:8, KMEM:9, KBRO:4, KGPT:5, KICT:5, MMMY:11, KAUS:13, KLBB:4, KSHV:4, KAEX:6, KBHM:8, KVPS:4, KMSY:11, KBTR:9, KHRL:6, KDAL:7, KMAF:6, KBAZ:1, 
95 freeze horizon, interal departures 273 from airports 49
KGRK:6, MMIO:1, KVCT:3, KSAT:11, KSGR:1, KDFW:16, KTYR:5, KBPT:5, KMLU:4, KJAN:5, KPNS:5, KEFD:1, KLFT:8, KDRT:2, KLIT:6, KMFE:6, KFTW:1, KIAH:1, KLCH:6, KMOB:5, KCLL:7, KCRP:11, KPWA:1, KAMA:7, KOKC:9, KHSV:3, KXNA:4, MMTM:1, KLRD:5, KACT:5, KTUL:8, KMEM:9, KBRO:4, MMTC:1, KGPT:5, KICT:5, MMMY:11, KAUS:13, KLBB:4, KSHV:4, KAEX:6, KBHM:8, KVPS:4, KMSY:11, KBTR:9, KHRL:6, KDAL:7, KMAF:6, KBAZ:1, 
100 freeze horizon, interal departures 298 from airports 54
KGRK:6, MMIO:1, KATL:6, KVCT:3, KSAT:11, KSGR:1, KELP:6, KDFW:16, KTYR:5, KBPT:5, KMLU:4, KJAN:5, KPNS:5, KEFD:1, KMCI:10, KLFT:8, KDRT:2, KLIT:6, KMFE:6, KFTW:1, KIAH:1, KLCH:6, KMOB:5, KCLL:7, KCRP:11, KPWA:1, KAMA:7, KOKC:9, KHSV:3, KXNA:4, MMTM:1, KLRD:5, KACT:5, KTUL:8, KMEM:9, KBRO:4, MMTC:1, KGPT:5, KICT:5, MMMY:11, KAUS:13, KLBB:4, MMSP:2, KSHV:4, KAEX:6, KBHM:8, KVPS:4, KMSY:11, KBTR:9, KHRL:6, KDAL:7, KMAF:6, MMCU:1, KBAZ:1, 
105 freeze horizon, interal departures 309 from airports 56
KGRK:6, MMIO:1, KATL:15, MMLO:1, KVCT:3, KSAT:11, KSGR:1, KELP:6, KDFW:16, KTYR:5, KBPT:5, KMLU:4, KJAN:5, KPNS:5, KEFD:1, KMCI:10, KLFT:8, KDRT:2, KLIT:6, KMFE:6, KFTW:1, KIAH:1, KLCH:6, KMOB:5, KCLL:7, KCRP:11, KPWA:1, KAMA:7, KOKC:9, KHSV:3, KXNA:4, MMTM:1, KLRD:5, KACT:5, KTUL:8, KMEM:9, KBRO:4, MMTC:1, KGPT:5, KICT:5, MMMY:11, KAUS:13, MMDO:1, KLBB:4, MMSP:2, KSHV:4, KAEX:6, KBHM:8, KVPS:4, KMSY:11, KBTR:9, KHRL:6, KDAL:7, KMAF:6, MMCU:1, KBAZ:1, 
110 freeze horizon, interal departures 337 from airports 62
KBNA:7, KGRK:6, MMIO:1, KATL:17, MMLO:5, KVCT:3, KSAT:11, KSGR:1, KELP:6, KTYR:5, KDFW:16, KBPT:5, KMLU:4, KJAN:5, KPNS:5, KEFD:1, KMCI:10, KLFT:8, KDRT:2, KTPA:2, KLIT:6, KMFE:6, KFTW:1, KIAH:1, KLCH:6, MMQT:2, KMOB:5, KCLL:7, KCRP:11, KPWA:1, KAMA:7, KOKC:9, KHSV:3, KXNA:4, MMTM:1, KLRD:5, KACT:5, KTUL:8, KMEM:9, KBRO:4, MMTC:1, KGPT:5, KICT:5, MMAS:1, MMMY:11, KAUS:13, MMDO:1, KLBB:4, MMSP:2, KSHV:9, KAEX:6, KBHM:8, MMMD:1, KVPS:4, KMSY:11, KBTR:9, KHRL:6, KDAL:7, KMAF:6, MMCU:1, KSTL:4, KBAZ:1, 
115 freeze horizon, interal departures 370 from airports 70
KBNA:7, KGRK:6, MMIO:1, KATL:18, MMLO:5, KVCT:3, KSAT:11, KSGR:1, KTYS:4, KELP:6, KTYR:5, KDFW:16, KBPT:5, KMCO:1, KMLU:4, KJAN:5, KPNS:5, KEFD:1, KMCI:10, KLFT:8, KDRT:2, KTPA:6, KLIT:6, KMFE:6, KFTW:1, KIAH:1, KLCH:6, MMQT:2, KMOB:5, KCLL:7, KCRP:11, KPWA:1, KAMA:7, KOKC:9, KHSV:3, KXNA:4, MMTM:1, KLRD:5, KACT:5, KTUL:8, KMEM:9, KBRO:4, MMTC:1, KGPT:5, KICT:5, MMAS:1, MMMY:11, MMMX:10, KAUS:13, MMDO:1, MMGL:1, KLBB:4, MMMM:2, MMSP:2, KSHV:9, KAEX:6, KOMA:1, KBHM:8, MMMD:1, KVPS:4, KMSY:11, KBTR:9, KABQ:6, MMPB:1, KHRL:6, KDAL:7, KMAF:6, MMCU:1, KSTL:6, KBAZ:1, 
120 freeze horizon, interal departures 417 from airports 81
MMQT:2, KDFW:16, KGRK:6, KELP:6, KEFD:1, KDRT:2, KAMA:7, KCLL:7, KTUL:8, KCRP:11, MMDO:1, KBRO:4, KXNA:4, MMVR:2, KSHV:9, MMCZ:2, KMCO:6, KAEX:6, MMPB:1, MMCU:1, KLIT:6, KMCI:10, KGPT:5, KSTL:6, KLCH:6, MMIO:1, KICT:5, KPNS:5, KSAT:11, KMOB:5, MMUN:6, KSGR:1, KTYS:4, KTYR:5, KRSW:2, KBPT:5, KLBB:4, MMTO:1, MMTM:1, KAVL:1, KCOS:5, KMSY:11, KACT:5, MMAS:1, MMTC:1, MMMZ:2, KMAF:6, KJAX:5, MMMY:11, MMMX:11, KOMA:7, KFTW:1, MMGL:5, KJAN:5, KAUS:13, MMMM:2, MMSP:2, KIAH:1, MMMD:1, KMLU:4, KLFT:8, KHSV:3, KABQ:6, KBHM:8, KMFE:6, KBTR:9, KVPS:4, MMLO:5, KBAZ:1, KDAL:7, KATL:18, KBNA:7, KLRD:5, KMEM:9, KVCT:3, KSDF:3, KDSM:2, KOKC:9, KPWA:1, KHRL:6, KTPA:6, 
freeze horizon(min),look ahead (min),variable name,mean,std,min,max

808 flights*********airports into IAH*************** 173
MMAS 1
MMZH 1
MMMZ 2
MMMY 11
MMMX 11
CYVR 1
KSGR 1
KBOS 6
KDCA 8
MHTG 1
KLBB 4
MMMM 2
KGUC 1
MMAA 1
KMTJ 1
KAUS 13
KTYS 4
MSLP 3
KLAX 14
KTYR 5
MMMD 1
SEQU 1
KLAS 8
KJAX 6
KCHS 3
KMSY 11
KJAN 5
KMSP 10
KSFO 9
MMLO 5
CYUL 1
KDAY 2
SPIM 1
KBAZ 1
KATL 18
PHNL 2
KBNA 7
LFPG 2
KDAL 7
CYHM 1
KGSP 3
KGSO 3
KEGE 1
KMFE 6
KORF 2
KORD 19
KSEA 9
KGRR 2
SKBO 2
KMEM 9
KFLL 5
KGRK 6
KCRW 1
KEFD 1
MZBZ 2
KSDL 1
MKJS 1
KCRP 11
MMVR 2
KSDF 5
KPWA 1
KAEX 6
MMVA 1
MMIO 1
MROC 4
KTUS 5
KPIT 5
MMUN 6
KTUL 8
KGPT 5
KBWI 5
KMCO 8
KRIC 3
SBGR 1
KLIT 6
CYEG 1
KMCI 10
SBGL 1
KICT 5
EGLL 4
KPHX 15
MMTO 1
MMTM 1
KMOB 5
KPHL 7
KONT 3
MMTC 1
KSAV 3
KSNA 5
KAPA 1
KSAT 11
KACT 5
KCOS 5
KSAN 8
MMGL 6
MMSP 2
MNMG 2
KRSW 4
MGGT 3
KSMF 3
KFTW 1
MMSD 2
KIND 6
KABQ 6
KDTW 9
KMAF 6
MRLB 2
TJSJ 1
KBHM 8
KOMA 7
KIAH 1
KBTR 9
KLGA 8
EDDF 2
KIAD 5
KHSV 3
KSLC 9
KLFT 8
KMLU 4
MHLM 1
KCAE 3
MMQT 2
KDSM 2
KJFK 1
KCMH 6
KDFW 16
KLEX 2
KLRD 5
TTPP 1
MWCR 1
KAMA 7
KHRL 6
KELP 6
CYYZ 6
KCLT 16
MMDO 1
KVPS 4
KDRT 2
SVMI 1
KRDU 6
KOKC 9
KPDX 4
KCLL 7
MMPR 4
KMKE 5
KBRO 4
KCLE 6
KVCT 3
MMCZ 2
CYYC 7
KSJC 3
EHAM 3
MMPB 1
MMCU 1
KTPA 7
KDEN 13
MMOX 1
KEWR 13
MMCE 1
KHDN 1
MMBT 1
KSHV 9
KLCH 6
MPTO 2
KBPT 5
KXNA 4
KMIA 10
KPBI 4
MMZO 1
KPNS 5
KCVG 7
KSTL 6
KAVL 1

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

}




