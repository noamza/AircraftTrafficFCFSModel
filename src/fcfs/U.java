package fcfs;

import java.util.Comparator;
import java.util.*;

public class U{	
	static java.io.PrintStream io = System.out;
	static boolean debug = false;
	static int watchingFlight = -1;
	static boolean verbose = false;
	//Wed, 18 Apr 2012 00:00:00 UTC  // Month is 0 based!!??
	//static final double simulationStart = (double)(new GregorianCalendar(2012,4-1,18).getTimeInMillis());
	//20110103
	static final double simulationStart = (double)(new GregorianCalendar(2011,1-1,3).getTimeInMillis());
	
	static final java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy:M:dd:HH:mm:ss");
	static final double toMinutes = 60*1000.0;
	static final double toHours   = 60*toMinutes;

	//FOLDER FILE NAMES
	static String workingDirectory = "/Users/nalmog/Desktop/Scheduler/";
	static final String csv = "ACES_input_04192012_CrossTimes_v2_wSpeedConstraint.csv";
	static final String inputFolder = "input/";
	static final String plotFolder = "plots/";
	static final String monteCarloFolder = "monte carlo/";
	static final String outFolder = "output/";
	static final String CSV_Name = inputFolder + csv;
	static final String airportCapacity = inputFolder + "AdvancedState_Hourly_Runways_AllCapacities_20120418_20120420.csv";
	
	public static String timeToDate(int time){
		//return new java.text.SimpleDateFormat("DDD:HH:mm:ss:SSSS").format(new java.util.Date((long)time));
		return new java.text.SimpleDateFormat("yyyy:M:dd:HH:mm:ss:SSSS").format(new java.util.Date((long)time));
	}
	public static String timeToDateAdjusted(int time){
		double t = time;
		t += simulationStart;
		//return new java.text.SimpleDateFormat("DDD:HH:mm:ss:SSSS").format(new java.util.Date((long)time));
		return new java.text.SimpleDateFormat("yyyy:M:dd:HH:mm:ss").format(new java.util.Date((long)t));
	}
	
	public static String timeToDateAdjustedShort(int time){
		double t = time;
		t += simulationStart;
		//return new java.text.SimpleDateFormat("DDD:HH:mm:ss:SSSS").format(new java.util.Date((long)time));
		return new java.text.SimpleDateFormat("D:HH:mm:ss").format(new java.util.Date((long)t));
	}
	
	public static String timeToString(int time){
		//return new java.text.SimpleDateFormat("DDD:HH:mm:ss:SSSS").format(new java.util.Date((long)time));
		return new java.text.SimpleDateFormat("yyyy:M:DDD:HH:mm:ss:SSSS").format(new java.util.Date((long)time));
	}

	public static String CSVfileName(String mainAirline){ 
		return ".csv";
	}
	
	public static boolean Assert(boolean a, String expression){ 
		if(!a){throw new java.lang.Error("FAILED: " + expression);}
		return a;}
	
	public static boolean Assert(boolean a){ 
		if(!a){throw new java.lang.Error("FAILED: Assert()");}
		return a;}
	
	public static void pp(Object s) {System.out.print(s.toString());} 
	public static void p(String s){ System.out.println(s);}
	public static void p(int s){ System.out.println(s);}
	public static void p(double s){ System.out.println(s);}
	public static void e(Object s) {System.err.print(s.toString()+"\n");}
	public static void pf(String format, Object... args){ System.out.printf(format, args);}
	public static void epf(String format, Object... args){ System.err.printf(format, args);}
	
	public static void start() { System.out.println("Start! " + dateFormat.format(new Date()));}
	public static void end() { System.out.println("FIN! " + dateFormat.format(new Date()));}

	
}

