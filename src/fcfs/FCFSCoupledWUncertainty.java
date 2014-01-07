package fcfs;

import java.util.*;
import java.io.*;

/*TODO:
 * 
 * check departure rates are being loaded in and used
 * 
 */

/*
 * 
 CFR
At pushback (0 look ahead):
Flights schedule arrival at 'scheduled departure time' (ASPM) for an arrival slot which is equal to 'scheduled arrival time' (scheduled departure time + unimpeded taxi time + nominal flight time).
Any atc delay from arrival airport is taken on the ground.
Flights  have a wheels off which is equal to scheduled departure time + unimpeded taxi time + controlled taxi uncertainty (a random value between 0-1 minute)
Flights reassess schedule at wheels off.
Flights try to make up this uncertainty with 2.5% speed adjustment.
If flights miss slot because of controlled taxi uncertainty, previous slot is freed, new slot assigned and additional atc delay is taken in the air.
Prior to pushback:
Flights arrival times are scheduled at the look ahead amount subtracted from the scheduled departure time.
The arrival slot assigned is for 'scheduled departure time' + unimpeded taxi time + nominal flight time.
Delay for the slot is assigned.
A gate uncertainty is calculated using log normal and aditya's constant correlated to the look ahead amount.
The gate uncertainty is added to the atc delay which is added to the unimpeded taxi time and the taxi uncertainty (from the log normal) to determine the wheels off time.
At wheels off, if the flight is leaving late and can't make up the time by speeding up, it's slot is freed and a new slot is assigned with additional delay being taken in the air.

NON CFR
At pushback:
Flights schedule arrival at 'scheduled departure time' for an arrival slot which is equal to 'scheduled arrival time' (scheduled departure time + unimpeded taxi time + nominal flight time).
Any atc delay from arrival airport is taken on the ground.
Flights  have a wheels off which is scheduled departure time + unimpeded taxi time + taxi uncertainty (a random value based on log n distribution with a zero probability)
Flights reassess schedule at wheels off.
Flights try to make up the taxi uncertainty with 2.5% speed adjustment.
If flights miss slot because of taxi uncertainty, previous slot is freed, new slot assigned and additional atc delay is taken in the air.
At freeze horizon:
Flights are scheduled at freeze horizon amount subtracted from scheduled arrival time.
Any atc delay from arrival scheduling is taken in the air.
 * 
 */


enum ScheduleMode {
	IAHCFR,
	scheduleInTheAir,
	scheduleArrival,
	scheduleDeparture,
	flightReadyToDepart,
	removeFromArrivalQueue,
	removeFromDepartureQueue,
	undef,
	WheelsOff,
}

class SchedulingEvent implements Comparable{
	int eventTime = 0;
	int coEventTime = 0;
	ScheduleMode mode = ScheduleMode.undef;
	Flight flight;
	public SchedulingEvent(int eventTime, int coEventTime, ScheduleMode mode, Flight flight)
	{ 
		this.eventTime= eventTime; 
		this.coEventTime = coEventTime; 
		this.mode = mode; 
		this.flight = flight;
	}
	void print(){ System.out.printf("r time: %d s time: %d\n", eventTime, coEventTime);}
	public int compareTo(Object o) { //orders priorityqueue by least time
		if(eventTime == ((SchedulingEvent)o).eventTime){
			return flight.id - ((SchedulingEvent)o).flight.id;
		}
		return eventTime-((SchedulingEvent)o).eventTime;
		//return ((rt)o).rescheduleTime - rescheduleTime; //order's priorityqueue by greatest first
	}
}

/*
 * Basic Algorithm:
 * 
 * Order flights by proposed gate departure time
 * schedule departures: 
 * 		if NON-CFR basic FCFS
 * 		if CFR, priority scheduling, push others forward
 * 			also schedule arrivals at this point
 * 		
 * Schedule arrivals 30min before proposed (at this point only non-CFR are left)
 * 	
 * 
 * */

public class FCFSCoupledWUncertainty implements Scheduler {


	public void printResults(ArrayList<Flight> flightList, String dir){

	}

	java.util.PriorityQueue<SchedulingEvent> schedulingQueue;
	Flights flights; 
	Airports airports;
	static double speedUp = 0.025; //0.025; 
	static double slowDown = 0.05; //0.05
	int rand = 0;
	Hashtable<String, Double> dispensedAirportDelayHrs;
	Hashtable<String, Double> absorbedAirportDelayHrs;
	//static java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("DDD:HH:mm:ss:SSSS");
	String ofolder = "output/";
	String infolder = "inputs/";
	//String workingDirectory = "C:\\Users\\Noam Almog\\Desktop\\scheduler\\scheduler\\atl_data\\";
	//String workingDirectory = "/Users/nalmog/Desktop/scheduler/atl_data/";
	//			String workingDirectory = "/Users/kpalopo/Desktop/scheduler/atl_data/";
	String workingDirectory = "/Users/nalmog/Desktop/scheduler/";
	java.util.Random random = new java.util.Random(1);//98);//98);//rand);//9 85); //used 98 for 100000 //6 it goes up //11 goes up

	Hashtable<Integer, Hashtable<Integer, Hashtable<String, Hashtable<String,Double>>>> 
	resultsFreezeSchedulinghorizonColumnCountmeanstd = new Hashtable<Integer, Hashtable<Integer, Hashtable<String, Hashtable<String,Double>>>>();
	//Hashtable<Integer, Hashtable<Integer, Hashtable<String, ArrayList<Double>>>> 
	//dataFreezeSchedulinghorizonColumnList = new Hashtable<Integer, Hashtable<Integer, Hashtable<String, ArrayList<Double>>>>();
	String[] columns = {"ground_delay_cfr_min","air_delay_cfr_min", 
						"ground_delay_non_min", "air_delay_non_min", 
						"arrival_airport_delay_hr",
						"weighted_delay_cfr_min","weighted_delay_non_min",
						"ground_delay_non_internal_min", "air_delay_non_internal_min","weighted_delay_non_internal_min",
						"ground_delay_non_external_min", "air_delay_non_external_min","weighted_delay_non_external_min"
						};

	//Hashtable<String, File> filesFreezeSchedulinghorizonColumnList = new Hashtable<String, File>();

	//File file = new File("C:/a");

	//initialize result structures

	public FCFSCoupledWUncertainty(){
		rand = Math.abs(new java.util.Random().nextInt());
	}

	/*
	algo description
	 */

	void load(String inputs, Flights flights, Airports airports){
		//flights.loadFlightsFromAces(workingDirectory+"clean_job.csv",false);
		airports.loadFromAces(inputs+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
		airports.loadDelays(inputs+"gate_delay.csv", "gate");	
		airports.loadDelays(inputs+"taxi_delay.csv", "taxi");
		airports.loadDelays(inputs+"taxi_u.csv", "taxi");
		flights.loadTaxiOffset(inputs+"AirportTaxi.csv");
		flights.loadCallSigns(inputs + "job_611_airline_flightid_map.csv");
		//System.out.println("		loaded " + dateFormat.format(new Date()));
		flights.pushFlightsForwardBy1hr(10*60*60*1000); // CHANGE //10
		airports.offsetCapacities(10*60*60*1000); //CHANGE //10 
		//TODO: airports.loadCFRdata();
	} //END LOAD()

	int schedulingHorizon;
	int freezeHorizon;

	TreeMap<String, BufferedWriter> files = new TreeMap<String, BufferedWriter>();

	public ArrayList<Flight> schedule(int sh, int fh){
		schedulingHorizon = sh;
		freezeHorizon = fh;
		return schedule();
	}
	
	boolean limitedCFRUncertainty = true;
	int montecarlo = 3;
	
	public ArrayList<Flight> schedule(){
		flights = new Flights(); 
		airports = new Airports();
		flights.loadFlightsFromAces(workingDirectory+infolder+"clean_job.csv",false);
		load(workingDirectory+infolder, flights, airports);

		//RUN SETTINGS
		final boolean pertrubGate = true; //gate taxi pert on/off (true false)
		final boolean pertrubTaxi = true; //used TRUE FOR 100000
		int uncertaintyToggle = 1; //gate uncertainty on/off (0 1)
		 //******************************************************************************************************//
		int counter = 0; // number of monte carlo runs
		int defaultPertMills = 0;//1*60000;
		

		//java.util.Random random = new java.util.Random(98);//98);//rand);//9 85); //used 98 for 100000 //6 it goes up //11 goes up
		//Start! 2013:12:08:23:40:52 monte carlo: 10000 estimated time 100 min FIN! 2013:12:09:01:57:37   
		//Start! 2013:12:09:06:34:54 FIN! 2013:12:09:08:53:00
		//FIN! Start! 2013:12:09:19:59:48 2013:12:09:22:49:25

		ScheduleMode modes[] = { ScheduleMode.IAHCFR };

		U.p("monte carlo: " + montecarlo + " estimated time " + 3*montecarlo/100 + " min");
		U.p("limited uncertainty: " + limitedCFRUncertainty);

		//INIT FILES
		String dname = montecarlo + "_limited_CFR_Uncertainty_" +limitedCFRUncertainty;
		String mcpath = workingDirectory+ofolder+"/"+ dname + "/";
		File mft = new File(mcpath);
		mft.mkdirs();
		String montecarlofolder = mft.getPath()+"/";

		//MAIN MONTE CARLO LOOP
		while (counter < montecarlo) {
			counter++;

			if(montecarlo > 1000){ if(counter % 1000 == 0){ U.p( U.now() + " " + (double)counter/montecarlo); } } 
			ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
			schedulingQueue = new java.util.PriorityQueue<SchedulingEvent>();
			//remove Non KIAH Flights
			Iterator<Flight> iter = flightList.iterator();
			int iahn = 0;
			TreeMap<String,Integer> temp = new TreeMap<String, Integer>();
			while(iter.hasNext()){
				Flight f = iter.next();
				if(!f.arrivalAirport.equals("KIAH")){
					iter.remove();
				} else {
					iahn++;
					int c = temp.get(f.departureAirport) == null? 1: temp.get(f.departureAirport)+1;// + 1;
					temp.put(f.departureAirport, c);
					//avg flight time


				}
			}

			String[] targets ={ "KATL","KAUS","KCLT","KDEN","KMIA","KDFW"};
			TreeMap<String, ArrayList<Double>> ft = new TreeMap<String, ArrayList<Double>>();
			for (String s: targets){ft.put(s, new ArrayList<Double>());}
			for(Flight f: flightList){
				for (String s: targets){
					if(f.departureAirport.equals(s)){
						double t = (f.arrivalTimeACES-f.departureTimeACES)/U.toMinutes;
						ft.get(s).add(t);
						//if(s.equals("KAUS")) U.p(s+ " " + t);
					}
				}
			}
			/*
			for (String s: targets){
				U.pf("%s: mean %.1f(min) total %.0f\n",
						s,
						Stats.count_sum_mean_std_min_max(ft.get(s)).get("mean"),
						Stats.count_sum_mean_std_min_max(ft.get(s)).get("count")
						);
			}
			U.p("**");
			 */
			//U.p(iahn + "*********airports into IAH*************** " + temp.size());
			//for(String s: temp.keySet()) U.p(s + " " + temp.get(s));

			Collections.sort(flightList, new flightDepTimeIDComparator());

			flights.resetPerturbationAndSchedulingDependentVariables();

			// SET KIAH CFR
			airports.getAirport("KIAH").setCFR(0, Integer.MAX_VALUE);



			for (Flight f: flightList){
				//the more random a flight departure is, the less delay.
				//int rm = (int)(1000*60*60*350*random.nextDouble()); f.arrivalTimeProposed+=rm; f.departureTimeProposed+=rm;
				AirportTree departureAirport = airports.airportList.get(f.departureAirport);
				int gate_noise_seconds = 0, taxi_noise_seconds = 0;		
				//Get gate and taxi perturbations
				f.gateUncertaintyConstant = random.nextGaussian();
				//U.p(f.gateUncertaintyConstant);

				if(departureAirport!=null){
					double gateR = random.nextDouble(), taxiR = random.nextDouble();
					//Main.p(gateR + " gate taxi " + taxiR + " " + departureAirport.taxiUnimpeded + " " + departureAirport.gateStd + " " + departureAirport.taxiMean);
					f.taxi_unimpeded_time = (int)(departureAirport.taxiUnimpeded)*60000;
					f.gate_perturbation = 0;
					/*
					if(pertrubGate && departureAirport.gateZeroProbablity < gateR){
						double gate_noise_minutes = Math.exp(random.nextGaussian()*departureAirport.gateStd + departureAirport.gateMean);

						gate_noise_minutes = gate_noise_minutes < 120? gate_noise_minutes: 120;
						gate_noise_seconds = (int)(gate_noise_minutes*60000);
						f.gate_perturbation = gate_noise_seconds;
						//ERROR OR NOT??
						if(gate_noise_minutes == 1) f.gate_perturbation = defaultPertMills;
					}
					*/
					if(pertrubTaxi && departureAirport.taxiZeroProbablity < taxiR){
						double taxi_noise_minutes = Math.exp(random.nextGaussian()*departureAirport.taxiStd + departureAirport.taxiMean);
						taxi_noise_minutes = taxi_noise_minutes < 45? taxi_noise_minutes: 45;
						taxi_noise_seconds = (int)(taxi_noise_minutes*60000);
						//U.p(""+taxi_noise_seconds/U.toMinutes);
						f.taxiUncertainty = taxi_noise_seconds;
						//ERROR OR NOT??
						if(taxi_noise_minutes == 1){
							Main.p(departureAirport.taxiZeroProbablity + " " /*c++*/ + " " + departureAirport.airportName);
							f.taxiUncertainty = defaultPertMills;
						}
						//						f.taxi_perturbation = 0;//taxi_noise_seconds; //CHANGE BACK
					}
					//for null airports on first run
					//Main.p("error in perturbation?");
					//TODO:add in airport cfr randomness

				} else {
					//Main.p("error in perturbation?");
					/* need??
					double gateR = random.nextDouble(), taxiR = random.nextDouble();
					double gate_noise_minutes = Math.exp(random.nextGaussian()*0);
					gate_noise_minutes = gate_noise_minutes < 120? gate_noise_minutes: 120;
					gate_noise_seconds = (int)(gate_noise_minutes*60000);
					double taxi_noise_minutes = Math.exp(random.nextGaussian()*0);
					taxi_noise_minutes = taxi_noise_minutes < 45? taxi_noise_minutes: 45;
					taxi_noise_seconds = (int)(taxi_noise_minutes*60000);
					f.gate_perturbation = gate_noise_seconds;
					f.taxi_perturbation = taxi_noise_seconds;
					//Main.p(gate_noise_seconds + " else");
					//keep?
					f.gate_perturbation = defaultPertMills;
					f.taxi_perturbation = defaultPertMills;
					 */
				}

			}


			for(ScheduleMode mode: modes){
				for(freezeHorizon = 30; freezeHorizon <= 120; freezeHorizon += 5){					
					if(counter == 1){
						resultsFreezeSchedulinghorizonColumnCountmeanstd.put(
								new Integer(freezeHorizon), new  Hashtable<Integer, Hashtable<String, Hashtable<String,Double>>>());
					}
					for(schedulingHorizon = 0; schedulingHorizon <= 60; schedulingHorizon += 15){
						String currentFilePrefix = freezeHorizon+"_"+schedulingHorizon+"_";
						if(counter == 1){
							resultsFreezeSchedulinghorizonColumnCountmeanstd.get(freezeHorizon).put(
									schedulingHorizon, new Hashtable<String, Hashtable<String, Double>>());
							
							for (String name: columns){
								//resultsFreezeSchedulinghorizonColumnCountmeanstd.get(freezeHorizon)
								//.get(schedulingHorizon).put(name, new Hashtable<String, Double>());
								for(String col: columns){
									String n = currentFilePrefix+col;
									File file = new File(mcpath+n+".csv");
									files.put(n, open(file.getPath()));
								}
							}

						}

						//RESET TO 0
						flights.resetSchedulingDependentVariables();
						airports.resetToStart();

						//int lookAheadMilliSec = minsAhd*minToMillisec;

						//int iii = 0;iii++;
						//System.out.println("millisec-days in an integer "+(double)Integer.MAX_VALUE/(1000.0*60*60*24));
						Hashtable<String,Integer> internalDepartures = new Hashtable<String, Integer>();
						int internalDeparturesC = 0;
						TreeMap<String, ArrayList<String>> demandIAH = new TreeMap<String, ArrayList<String>>();
						for(int t = 0; t <= 23; t++){String s = t<10? "0"+t: t+""; 
						demandIAH.put("3:"+s,new ArrayList<String>());demandIAH.put("4:"+s,new ArrayList<String>());}
						//U.p("******************************** " + temp.size());
						for (Flight f: flightList){

							//counting internal departures
							if(schedulingHorizon==0){
								//tracking demand.
								if(freezeHorizon==30){
									String s[] = U.timeToDateAdjustedShort(f.arrivalTimeACES).split(":");//U.p(s[0]+":"+ s[1] +":"+s[2]);
									demandIAH.get(s[0]+":"+s[1]).add(s[2]);//demandIAH.put(key, value)
								}
								//checking internals departures
								if(f.arrivalTimeACES-f.departureTimeACES < freezeHorizon*U.toMinutes){
									int t = internalDepartures.get(f.departureAirport) == null? 1: internalDepartures.get(f.departureAirport)+1;// + 1;
									internalDepartures.put(f.departureAirport, t);
									internalDeparturesC++;
									
								}
							}
							
							//checking internal departures
											//flight time
							if(f.arrivalTimeACES-f.departureTimeACES < freezeHorizon*U.toMinutes){
								if(!f.cfrEffected)f.nonCFRInternal = true;
							}
							
							switch (mode) {

							case IAHCFR:
							{	
								//WHICH FLIGHTS ARE CFR EFECTED
								if(airports.getArrivalAirport(f).effectedByCFR(f.arrivalTimeACES+f.taxi_unimpeded_time) 
										&& f.departureAirport.equals("KDFW")
										){
									f.cfrEffected = true;
								}
								U.Assert(f.arrivalAirport.equals("KIAH"));

								//DETERMINING WHEN FLIGHTS ARE FIRST SCHEDULED

								int departureSchedulingTime = f.departureTimeACES + f.gate_perturbation;

								//CFR UNCERTAINTY
								if(f.cfrEffected){

									departureSchedulingTime -= (int)(schedulingHorizon*U.toMinutes);
									AirportTree departureAirport = airports.getDepartureAirport(f);
									//net effect is 1.1 for 0 horizon and +.05 for every 15 min
									double lookAheadUncertaintyConstant = (int)1.1 + schedulingHorizon/(2*15*10);
									//NOTE: random.nextGaussian(); which is gauss mean 0.0 std 1.0 (-1,1)
									double gate_uncertainty_minutes = Math.exp(random.nextGaussian()*
											(departureAirport.gateStd*lookAheadUncertaintyConstant) + departureAirport.gateMean);
									f.gateUncertainty = (int)(gate_uncertainty_minutes*U.toMinutes);
									//determine if 0 probability

									//* ADD BACK IN
									double taxi_noise_minutes = Math.exp(random.nextGaussian()*departureAirport.taxiStd + departureAirport.taxiMean);
									taxi_noise_minutes = taxi_noise_minutes < 45? taxi_noise_minutes: 45;
									int taxi_noise_seconds = (int)(taxi_noise_minutes*60000);
									//U.p(""+taxi_noise_seconds/U.toMinutes);
									f.taxiUncertainty = taxi_noise_seconds;
									//*/

									//this is for -2 +1 min at 0 horizon scheduling. Not allowing leaving early, so only 0 or late.
									//SCHEDULE AT PUSHBSCK
									if(schedulingHorizon==0 || limitedCFRUncertainty){ //0
										double plus1minus2 = random.nextDouble()*3;
										plus1minus2 = plus1minus2 < 1? plus1minus2: 0;
										//plus1minus2 -= 2;
										if(schedulingHorizon!=0) U.Assert(limitedCFRUncertainty);
										f.taxiUncertainty = (int) (plus1minus2*U.toMinutes); 
										f.gateUncertainty = 0;
									}

									//0 probability
									double gateR = random.nextDouble();
									double taxiR = random.nextDouble();
									if(departureAirport.gateZeroProbablity > gateR){
										f.gateUncertainty = 0;
									}
									if(departureAirport.gateZeroProbablity > taxiR){
										f.taxiUncertainty = 0;
									}

									U.Assert(f.gateUncertainty >= 0);

									//NON CFR 	
								} else {

								}

								schedulingQueue.add(new SchedulingEvent(departureSchedulingTime, - 1, ScheduleMode.scheduleDeparture, f));

								//}
							}
							break;

							default:
							{
								Main.p("error in switch1 should not be here");
							}

							} //END SWITCH


						} // end (F: FLIGHTLIST) loop 
						//print demand
						if(schedulingHorizon == 0){
							if(freezeHorizon==30){
								for(String k:demandIAH.keySet()){
									//U.pp(k+"("+demandIAH.get(k).size()+") = ");
									//for(String s: demandIAH.get(k))U.pp(s+',');
									//U.p("");
								}
							}
							//U.p(freezeHorizon + " freeze horizon, interal departures " + internalDeparturesC + " from airports " +internalDepartures.size());
							for (String k: internalDepartures.keySet()){
								//U.pp(k+":"+internalDepartures.get(k)+", ");
							} //if(!internalDepartures.isEmpty())U.p("");
						}

						SchedulingEvent prevEvent = new SchedulingEvent(0, 0, ScheduleMode.scheduleDeparture, Flight.dummyDeparture(0));
						while(!schedulingQueue.isEmpty()){ // while there are events to process (main loop)

							//execute earliest event;
							SchedulingEvent event = schedulingQueue.remove();

							//U.Assert(prevEvent.eventTime <= event.eventTime, prevEvent.mode + " " + prevEvent.eventTime 
							//		+ " ERROR events happening out of order " + event.mode + " " + event.eventTime);

							Flight f = event.flight;
							f.scheduled = true;
							f.numberOfevents++;

							switch (event.mode) {

							case scheduleDeparture:
							{
								//scheduleDeparture(event);
								if(event.flight.cfrEffected){
									scheduleDepartureCFR(event);
								} else 
									scheduleDepartureNonCFR(event);
							}
							break;
							case WheelsOff:
							{	
								if(event.flight.cfrEffected){
									WheelsOffCFR(event);
								} else  {
									WheelsOffNon(event);
								}
							}
							break;
							case scheduleArrival:
							{	
								if(event.flight.cfrEffected){
									scheduleArrivalCFR(event);
								} else 
									scheduleArrivalNonCFR(event);
							}
							break;
							case removeFromDepartureQueue: // for case 3 and 4
							{
								removeFromDepartureQueue(event);
							}
							break;
							case removeFromArrivalQueue: // for case 3 and 4
							{
								removeFromArrivalQueue(event);
							}
							break;

							case undef:
							{
								Main.p("should not be here");
								System.err.println("EVENT ERROR SHOULD NOT BE HERE");
							}
							break;
							default:
							{
								Main.p("should not be here");
								System.err.println("EVENT ERROR SHOULD NOT BE HERE");

							}

							U.Assert(false, "should not be here in event loop");
							break;

							} //end switch statement
							//U.p(prevTime + " " + event.eventTime);
							prevEvent = event;

						} //END WHILE OF EVENTS

						//validate
						//flights.validate();
						airports.validate();

						Hashtable<String, Double> data = new Hashtable <String, Double>();
						for (String col: columns){
							data.put(col, 0d);
						}

						double groundDelayCFR = 0, airDelayCFR = 0, groundDelayAll = 0, airDelayAll = 0, arrivalAirportDelay = 0;
						int cfrs = 0, all = 0;
						//double minFt = 45;
						//int shorts = 0, 
						for(Flight f: flightList){
							if(f.scheduled){
								U.Assert(f.gate_perturbation == 0);
								//CFR
								if(f.cfrEffected){
									cfrs++;
									groundDelayCFR+= f.atcGroundDelay;
									airDelayCFR += f.atcAirDelay;
									U.Assert(f.atcGroundDelay + f.atcAirDelay == f.arrivalAirportDelay);

									write(files.get(currentFilePrefix+columns[0]), ""+(f.atcGroundDelay/U.toMinutes));
									write(files.get(currentFilePrefix+columns[1]), ""+(f.atcAirDelay/U.toMinutes));
									write(files.get(currentFilePrefix+columns[5]), ""+((2*f.atcAirDelay+f.atcGroundDelay)/U.toMinutes));

								} else {
									//NON CFR
									groundDelayAll += f.atcGroundDelay;
									airDelayAll += f.atcAirDelay;
									write(files.get(currentFilePrefix+columns[2]), ""+(f.atcGroundDelay/U.toMinutes));
									write(files.get(currentFilePrefix+columns[3]), ""+(f.atcAirDelay/U.toMinutes));
									write(files.get(currentFilePrefix+columns[6]), ""+((2*f.atcAirDelay+f.atcGroundDelay)/U.toMinutes));
									
//						"ground_delay_non_internal_min", "air_delay_non_internal_min","weighted_delay_non_internal_min",
//						"ground_delay_non_external_min", "air_delay_non_external_min","weighted_delay_non_external_min"
									
									//INTERNAL VS EXTERNAL
									if(f.nonCFRInternal){
										write(files.get(currentFilePrefix+"ground_delay_non_internal_min"), ""+(f.atcGroundDelay/U.toMinutes));
										write(files.get(currentFilePrefix+"air_delay_non_internal_min"), ""+(f.atcAirDelay/U.toMinutes));
										write(files.get(currentFilePrefix+"weighted_delay_non_internal_min"), ""+((2*f.atcAirDelay+f.atcGroundDelay)/U.toMinutes));
									} else {
										write(files.get(currentFilePrefix+"ground_delay_non_external_min"), ""+(f.atcGroundDelay/U.toMinutes));
										write(files.get(currentFilePrefix+"air_delay_non_external_min"), ""+(f.atcAirDelay/U.toMinutes));
										write(files.get(currentFilePrefix+"weighted_delay_non_external_min"), ""+((2*f.atcAirDelay+f.atcGroundDelay)/U.toMinutes));
									}
								}


								arrivalAirportDelay += f.arrivalAirportDelay;
								all++;
							}
						}
						write(files.get(currentFilePrefix+columns[4]), ""+(arrivalAirportDelay/U.toHours));

					} //END sched HOR
				} //END freeze HOR
			} // END by Mode

		} // END Monte carlo
		//		for(int s: delayedIntheAir.keySet()){
		//			if(delayedIntheAir.get(s)!=2){
		//				Main.p(s+" "+delayedIntheAir.get(s));
		//			}
		//		}
		Integer[] fhk = (Integer[]) resultsFreezeSchedulinghorizonColumnCountmeanstd.keySet().toArray(new Integer[0]);  
		Arrays.sort(fhk);
		Integer[] shk = (Integer[]) resultsFreezeSchedulinghorizonColumnCountmeanstd.get(fhk[0]).keySet().toArray(new Integer[0]);  
		Arrays.sort(shk);

		String header = "freeze horizon(min),look ahead (min),variable name,mean,std,min,max,sample size";

		for (String col: files.keySet()){
			close(files.get(col));
		}

		for(String s: columns){
			files.put(s, open(montecarlofolder+s+'_'+montecarlo+".csv"));
		}

		for (String col: columns){
			write(files.get(col), header);
		}

		U.p(header);
		for (int fh: fhk){
			for (int sh: shk){
				for (String col: columns){

					String t = montecarlofolder+fh+'_'+sh+'_'+col+".csv";
					Hashtable<String, Double> results = Stats.count_sum_mean_std_min_max(read(t));

					U.pf("%3d,%2d,%-40s%4.1f,%4.1f,%4.1f,%4.1f,%.0f\n",
							fh,sh,col+',',
							results.get("mean"),
							results.get("std"),
							results.get("min"),
							results.get("max"),
							results.get("count")
							);

					String line = String.format("%d,%d,%s,%.1f,%.1f,%.1f,%.1f,%.0f",
							fh,sh,col,
							results.get("mean"),
							results.get("std"),
							results.get("min"),
							results.get("max"),
							results.get("count")
							);

					write(files.get(col), line);
				}
			}
		}

		for (String col: files.keySet()){
			close(files.get(col));
		}
		//CLOSE FILES

		return new ArrayList<Flight>();

	} //END SCHEDULE()


	public ArrayList<Double> read(String path){
		ArrayList<Double> t = new ArrayList<Double>();
		try{
			//Read callsigns in
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = br.readLine()) != null){
				line = line.replaceAll("\\s","");//trim();

				if(line.length()>0) t.add(Double.parseDouble(line));
			}
			in.close();

		}catch (Exception e){
			System.err.println("call sign load Error: " + e.getMessage());
			e.printStackTrace();
		}
		return t;
	}

	public void write(BufferedWriter out, String s){
		try{
			out.write(s+"\n");
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}	

	public void close(BufferedWriter out){
		try{
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	public BufferedWriter open(String nameDir){
		try{
			// Create file 
			FileWriter fstream = new FileWriter(nameDir);
			BufferedWriter out = new BufferedWriter(fstream);
			return out;
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		System.err.println("Error: " + "couldnt open file");
		return null;
	}

	// /////////////////  //////////////// END OF FCFS
	/*
 CFR
At pushback (0 look ahead):
Flights schedule arrival at 'scheduled departure time' (ASPM) for an arrival slot which is equal to 'scheduled arrival time' (scheduled departure time + unimpeded taxi time + nominal flight time).
Any atc delay from arrival airport is taken on the ground.
Flights  have a wheels off which is equal to scheduled departure time + unimpeded taxi time + controlled taxi uncertainty (a random value between 0-1 minute)
Flights reassess schedule at wheels off.
Flights try to make up this uncertainty with 2.5% speed adjustment.
If flights miss slot because of controlled taxi uncertainty, previous slot is freed, new slot assigned and additional atc delay is taken in the air.

Prior to pushback:
Flights arrival times are scheduled at the look ahead amount subtracted from the scheduled departure time.
The arrival slot assigned is for 'scheduled departure time' + unimpeded taxi time + nominal flight time.
Delay for the slot is assigned.
A gate uncertainty is calculated using log normal and aditya's constant correlated to the look ahead amount.
The gate uncertainty is added to the atc delay which is added to the unimpeded taxi time and the taxi uncertainty (from the log normal) to determine the wheels off time.
At wheels off, if the flight is leaving late and can't make up the time by speeding up, it's slot is freed and a new slot is assigned with additional delay being taken in the air.

	 */
	int watching = -1;
	//depart:
	//schedule a departure meeting dep contraints
	// 
	//scheduling at gate perturbation.. or gate
	//what if flight is ready to leave before?????
	//limit taxi uncertainty 1 min
	public void scheduleDepartureCFR(SchedulingEvent event)
	{
		//schedule arrival
		Flight f = event.flight;
		if(f.id==watching){ U.p("scheduleDepartureCFR " + f.id); }
		//U.pf("(%.1f,%.1f)\n", f.gateUncertainty/U.toMinutes, f.taxi_perturbation/U.toMinutes);
		int currentTime = event.eventTime;
		U.Assert(f.cfrEffected);
		U.Assert(f.gateUncertainty >= 0);
		U.Assert(currentTime == f.gate_perturbation + f.departureTimeACES - U.toMinutes*schedulingHorizon);
		U.Assert(f.gate_perturbation==0);
		int departureAdditives = + f.taxi_unimpeded_time + f.gate_perturbation; //gate perturbation??
		f.departureTimeFinal = f.departureTimeACES + departureAdditives;
		schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.scheduleArrival, f));

	}

	public void scheduleArrivalCFR(SchedulingEvent event){
		Flight f = event.flight;
		if(f.id==watching){ U.p("scheduleArrivalCFR " + f.id); }
		int currentTime = event.eventTime;
		U.Assert(f.cfrEffected);
		U.Assert(f.departureTimeFinal > 1);
		U.Assert( f.gate_perturbation == 0);
		//int departureAdditives = + f.taxi_unimpeded_time; //gate perturbation??
		//int proposedDepartureTime = f.departureTimeProposed + departureAdditives; //TODO MORE HERE?
		//f.departureTimeFinal = proposedDepartureTime;
		int departureOffset = f.departureTimeFinal-f.departureTimeACES;
		if(f.firstTimeBeingArrivalScheduled){U.Assert(f.atcGroundDelay == 0);
		U.Assert(departureOffset == f.gate_perturbation+f.taxi_unimpeded_time + f.atcGroundDelay, 
				(f.gate_perturbation+f.taxi_unimpeded_time + f.atcGroundDelay)+" "+departureOffset+" "+f.id+ " " + f.numberOfevents);
		} else U.Assert(departureOffset == f.gate_perturbation+f.taxi_unimpeded_time + Math.max(f.atcGroundDelay,f.gateUncertainty)+f.taxiUncertainty);
		
		int proposedArrivalTime = f.arrivalTimeACES + departureOffset;
		U.Assert(f.arrivalTimeACES <= proposedArrivalTime);
		if(f.firstTimeBeingArrivalScheduled)
			U.Assert(currentTime <= f.departureTimeACES + f.gate_perturbation, currentTime+ " scheduleArrivalCFR "
					+ (f.departureTimeACES + f.gate_perturbation) + " " + (f.id));
		//U.p((proposedArrivalTime-f.departureTimeFinal) / U.toMinutes);
		
		if(f.firstTimeBeingArrivalScheduled){
			airports.scheduleArrival(f, proposedArrivalTime, currentTime);
			//wheels off
			U.Assert(f.departureTimeACES + f.gate_perturbation + f.taxi_unimpeded_time + f.atcGroundDelay == f.departureTimeFinal,
					(f.departureTimeACES + f.gate_perturbation + f.taxi_unimpeded_time + f.atcGroundDelay)
					+ " ?= " + f.departureTimeFinal + " " + f.id  );
			
			int wheelsOff = f.departureTimeACES + f.taxi_unimpeded_time +f.taxiUncertainty
					+ Math.max(f.gateUncertainty, f.atcGroundDelay);
			schedulingQueue.add(new SchedulingEvent(wheelsOff, -8, ScheduleMode.WheelsOff, f));
		}
		
		//second time being scheduled
		if(!f.firstTimeBeingArrivalScheduled){
			U.Assert(currentTime == f.departureTimeFinal,currentTime +" "+f.departureTimeFinal + " " + f.id);
			int nominalDuration = f.arrivalTimeACES-f.departureTimeACES;
			int shortestDuration =   (int)(nominalDuration/(1+speedUp));
			U.Assert(event.coEventTime == f.departureTimeFinal + shortestDuration, event.coEventTime+ " "+  f.departureTimeFinal + shortestDuration);
			//event.coEventTime should be wheelsofftime + shortest flight time.
			airports.scheduleArrival(f, event.coEventTime, currentTime);
			
		}
	}


	//check if flight can make it by speeding up/slowing down,
	public void WheelsOffCFR(SchedulingEvent event){
		//should have ground delay? what to do with it??
		//delete if don't need it
		//use it if there, delete rest if partway through..
		Flight f = event.flight;
		if(f.id==watching){ U.p("WheelsOffCFR " + f.id); }
		int currentTime = event.eventTime;
		U.Assert(f.cfrEffected);
		U.Assert(f.departureTimeFinal > 1);
		U.Assert(currentTime >= f.departureTimeFinal, currentTime + " wheelsOff final " +f.departureTimeFinal+ " no earlies " + f.id + " " + freezeHorizon);//for this case
		int nominalDuration = f.arrivalTimeACES-f.departureTimeACES;
		int shortestDuration =   (int)(nominalDuration/(1+speedUp)); 
		int longestDuration =  (int)(nominalDuration/(1-slowDown));
		//U.p((nominalDuration-longestDuration)/U.toMinutes);

		U.Assert(currentTime > f.departureTimeACES+f.gate_perturbation+f.taxiUncertainty);

		//TODO tabulation of ground delay
		//flight leaves too early
		
		f.departureTimeFinal = currentTime;
		//if(f.id == 4) U.p(currentTime +" "+f.departureTimeFinal + " " + f.id);
		f.firstTimeBeingArrivalScheduled = false;
		
		if(currentTime < f.departureTimeFinal && currentTime+longestDuration < f.arrivalTimeFinal){
			U.e( currentTime + " current final" +f.departureTimeFinal);
			schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.removeFromArrivalQueue, f));
			schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.scheduleArrival, f));

			//tabs
			//leaves too late.	
		}else if (currentTime > f.departureTimeFinal && currentTime + shortestDuration > f.arrivalTimeFinal){
			f.departureTimeFinal = currentTime;
			schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.removeFromArrivalQueue, f));
			//changed so that flight starts looking for slots at fastest arrival point.
			schedulingQueue.add(new SchedulingEvent(currentTime, currentTime + shortestDuration, ScheduleMode.scheduleArrival, f));
			//U.p("flights leaving late ");
			//tabs

		} else {
			f.departureTimeFinal = currentTime;
			//U.p("Smoooooooth");
		}
	}

	//schedules departures at perturbed gate time.
	//non cfr
	//schedule at aspm gate
	//take delay on ground
	//add taxi uncertainty might need rescheduling
	//take rest of delay in the air
	//
	/*
NON CFR
At pushback:
Flights schedule arrival at 'scheduled departure time' for an arrival slot which is equal to 'scheduled arrival time' (scheduled departure time + unimpeded taxi time + nominal flight time).
Any atc delay from arrival airport is taken on the ground.
Flights  have a wheels off which is scheduled departure time + unimpeded taxi time + taxi uncertainty (a random value based on log n distribution with a zero probability)
Flights reassess schedule at wheels off.
Flights try to make up the taxi uncertainty with 2.5% speed adjustment.
If flights miss slot because of taxi uncertainty, previous slot is freed, new slot assigned and additional atc delay is taken in the air.
At freeze horizon:
Flights are scheduled at freeze horizon amount subtracted from scheduled arrival time.
Any atc delay from arrival scheduling is taken in the air.
	 */

	public void scheduleDepartureNonCFR(SchedulingEvent event)
	{
		Flight f = event.flight;
		if(f.id==watching){ U.p("scheduleDepartureNonCFR " + f.id); }
		U.Assert(!f.cfrEffected);
		U.Assert(f.gate_perturbation == 0);
		int departureAdditives = f.taxi_unimpeded_time + f.gate_perturbation; //gate perturbation??
		f.departureTimeFinal = f.departureTimeACES + departureAdditives;
		int gateDeparture = f.departureTimeACES + f.gate_perturbation;
		int proposedArrivalTime = f.arrivalTimeACES + departureAdditives;
		//flight will schedule arrival the amount of the freeze horizon before arrival
		int freezeHorizonMil = freezeHorizon*(int) Flight.toMinutes;
		//so that don't schedule arrival before departure, otherwise schedule x minutes before arrival
		//int timeToScheduleArrival = Math.max(proposedArrivalTime - freezeHorizonMil, f.getDepartureTimeFinal());
		int timeToScheduleArrival = Math.max(proposedArrivalTime - freezeHorizonMil, gateDeparture);
		//U.e(schedulingHorizon + " " + f.taxiUncertainty/U.toMinutes + " f.gateUncertainty " +  f.gateUncertainty/U.toMinutes);
		//CFR flights will have been scheduled already
		schedulingQueue.add(new SchedulingEvent(timeToScheduleArrival, proposedArrivalTime, ScheduleMode.scheduleArrival, f));
	}

	//check if flight can make it by speeding up/slowing down,
	public void WheelsOffNon(SchedulingEvent event){
		//should have ground delay? what to do with it??
		//delete if don't need it
		//use it if there, delete rest if partway through..
		//U.p("sdfsdfsfd");
		Flight f = event.flight;
		if(f.id==watching){ U.p("WheelsOffNon " + f.id); }
		int currentTime = event.eventTime;
		U.Assert(f.departureTimeFinal > 1);
		//U.Assert(currentTime >= f.getDepartureTimeFinal(), currentTime + " wheelsOff final " +f.getDepartureTimeFinal()+ " no earlies " + f.id + " " + freezeHorizon);//for this case
		int nominalDuration = f.arrivalTimeACES-f.departureTimeACES;
		int shortestDuration = (int)(nominalDuration/(1+speedUp)); 
		int longestDuration =  (int)(nominalDuration/(1-slowDown));
		//U.p((nominalDuration-longestDuration)/U.toMinutes);

		//U.Assert(currentTime > f.departureTimeACES+f.gate_perturbation+f.taxiUncertainty);

		//TODO tabulation of ground delay
		//flight leaves too early
		if(currentTime < f.getDepartureTimeFinal() && currentTime+longestDuration < f.arrivalTimeFinal){
			U.e( currentTime + " current final" +f.departureTimeFinal + " no earlies " + freezeHorizon);
			f.departureTimeFinal = currentTime;
			schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.removeFromArrivalQueue, f));
			schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.scheduleArrival, f));

			//tabs
			//leaves too late.	
		}else if (currentTime > f.departureTimeFinal && currentTime + shortestDuration > f.arrivalTimeFinal){
			f.departureTimeFinal = currentTime;
			schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.removeFromArrivalQueue, f));
			//reschedules at soonest arrival point
			schedulingQueue.add(new SchedulingEvent(currentTime, currentTime+shortestDuration, ScheduleMode.scheduleArrival, f));
			//U.p("flights leaving late ");
			//tabs
		} else {
			f.departureTimeFinal = currentTime;
			//U.p("Smoooooooth non");
		}
	}


	//schedules arrivals at perturbed arrivaltime - freeze horizon, or wheels off, whichever is later
	public void scheduleArrivalNonCFR(SchedulingEvent event)
	{
		//Main.p(++ty);
		//TODO logic different if scheduled more than once.
		Main.Assert(!event.flight.cfrEffected,"!event.flight.CFRaffected");
		Flight f = event.flight;
		if(f.id==watching){ U.p("scheduleArrivalNonCFR " + f.id); }
		int currentTime = event.eventTime;
		int proposedArrivalTime = event.coEventTime;
		Main.Assert(f.atcAirDelay == 0, "f.atcAirDelay == 0");
		//there could be ground delay added from adjusting the arrival Queue, which would mean still more than 30min from arrival.
		if(f.firstTimeBeingArrivalScheduled){
			Main.Assert(f.atcGroundDelay == 0, "f.atcAirDelay == 0");
			airports.scheduleArrival(f, proposedArrivalTime, currentTime);
			f.firstTimeBeingArrivalScheduled = false;
			//schedulingQueue.add(new SchedulingEvent(currentTime+f.atcGroundDelay, proposedArrivalTime+f.atcGroundDelay, ScheduleMode.scheduleArrival, f));
		}
		else {							//used to be proposedArrivalTime
			airports.scheduleArrival(f, event.coEventTime, currentTime);
			f.arrivalTimeFrozen = true;
		}

		//check if internal flight
		if(currentTime == f.departureTimeACES){
			f.gateUncertainty = 0;
			int wheelsOff = f.departureTimeACES + f.taxi_unimpeded_time + f.gate_perturbation  
					+f.taxiUncertainty + f.atcGroundDelay;

			//non-uncertainty
			//f.gateUncertainty
			schedulingQueue.add(new SchedulingEvent(wheelsOff, -1, ScheduleMode.WheelsOff, f));
		}

	}

	public void removeFromDepartureQueue(SchedulingEvent event)
	{
		Flight f = event.flight;
		if(f.id==watching){ U.p("removeFromDepartureQueue " + f.id); }
		airports.removeFlightFromDepartureQueue(f);
	}

	public void removeFromArrivalQueue(SchedulingEvent event)
	{
		//TODO make this better by rebalancing queue after? queue repair?
		Flight f = event.flight;
		if(f.id==watching){ U.p("removeFromArrivalQueue " + f.id ); }
		airports.removeFlightFromArrivalQueue(f);
	}

	public void summarize(Collection<Flight> flightList){
		ArrayList<Double> timeDataE = new ArrayList<Double>();
		//compares to baseline
		for(Flight f: flightList){ 
			if(f.scheduled){
				//String first = "none", cur = "then"; int tot = 0; double mint = Double.MAX_VALUE
			} //end IF scheduled
		}//END for flights
		Hashtable<String, Double> timeE = Stats.count_sum_mean_std_min_max(timeDataE);

		/*	
		Hashtable<String,Double> results = new Hashtable<String,Double>();
		results.put("avgTimeE", timeE.get("mean"));
		results.put("stdTimeE", timeE.get("std"));
		results.put("avgPassE", totalPassesE.get("mean"));
		results.put("stdPassE", totalPassesE.get("std"));
		results.put("avgTimeU", timeU.get("mean"));
		results.put("stdTimeU", timeU.get("std"));
		results.put("avgPassU", totalPassedByU.get("mean"));
		results.put("stdPassU", totalPassedByU.get("std"));

		return results;
		//*/
	}

	/*
	public void scheduleDeparture(SchedulingEvent event)
	{
		//Main.p(++sici);
		//TODO logic different if being scheduled more than once
		//schedule priority or regular departure based on CFR
		Flight f = event.flight;
		//duration speeds
		int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
		int fastestDuration =  (int)(nominalDuration/(1+speedUp)); 
		Main.Assert(f.gateUncertainty<=0, "err uncertainty is '-' " + f.gateUncertainty); //????
		int delayDelta = Math.max(f.gateUncertainty,f.atcGroundDelay); 
		Main.Assert(f.atcAirDelay == 0, "f.atcAirDelay==0");
		Main.Assert(f.atcGroundDelay == 0, "f.atcGroundDelay==0");
		int departureAdditives = + f.taxi_unimpeded_time; //gate perturbation??
		int proposedDepartureTime = f.departureTimeProposed + departureAdditives; //TODO MORE HERE?

		if(airports.getArrivalAirport(f).effectedByCFR(f.arrivalTimeProposed+departureAdditives) 
				&& f.departureAirport.equals("KDFW")
				){
			f.cfrEffected = true;
			//f.airline = "cfr";
		}

		airports.scheduleDeparture(f, proposedDepartureTime, event.eventTime);	
		int proposedArrivalTime = f.arrivalTimeProposed + departureAdditives + f.atcGroundDelay;
		//flight will schedule arrival 30min before
		int minBeforeArrival = freezeHorizon*(int) Flight.toMinutes;
		//so that don't schedule arrival before departure, otherwise schedule x minutes before arrival
		int timeToScheduleArrival = Math.max(proposedArrivalTime - minBeforeArrival, f.departureTimeFinal); //use current or departure time?
		//schedule arrivals at departure time for cfrs;
		if(f.cfrEffected){
			//if(f.cfrEffected){
			timeToScheduleArrival = f.departureTimeFinal;
		}
		schedulingQueue.add(new SchedulingEvent(timeToScheduleArrival, proposedArrivalTime, ScheduleMode.scheduleArrival, f));
	}
	 */
	/*
	public void scheduleDeparture(SchedulingEvent event)
	{
		//Main.p(++sici);
		//TODO logic different if being scheduled more than once
		//schedule priority or regular departure based on CFR
		//schedule arrival if CFR
		Flight f = event.flight;
		//duration speeds
		int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
		int fastestDuration =  (int)(nominalDuration/(1+speedUp)); 
		Main.Assert(f.gateUncertainty<=0, "err uncertainty is '-' " + f.gateUncertainty); //????
		int delayDelta = Math.max(f.gateUncertainty,f.atcGroundDelay); 
		Main.Assert(f.atcAirDelay == 0, "f.atcAirDelay==0");
		Main.Assert(f.atcGroundDelay == 0, "f.atcGroundDelay==0");
		if(airports.getArrivalAirport(f).effectedByCFR(f.departureTimeProposed+f.taxi_unimpeded_time)){
			f.cfrEffected = true;
			//cfr_flights++;
		}
		int departureAdditives = + f.taxi_unimpeded_time; //gate perturbation??
		int proposedDepartureTime = f.departureTimeProposed + departureAdditives; //TODO MORE HERE?
		int proposedArrivalTime = f.arrivalTimeProposed + departureAdditives;
		f.cfrEffected = airports.effectedByCFR(f);
		//f.CFRaffected
		if(f.cfrEffected){
		  /*int diff = -1;
			int departureSchedulingDelay = 0;
			int i = 0;

			while (diff != 0){
//				departureSchedulingDelay += 
//						airports.getSoonestPriorityDeparture(f, proposedDepartureTime + departureSchedulingDelay, event.eventTime);
//				diff = airports.getSoonestArrival(f, proposedArrivalTime + departureSchedulingDelay, event.eventTime);
//				departureSchedulingDelay += diff;

				//DEPARTURE
				int depDelay = 
						airports.getSoonestPriorityDeparture(f, proposedDepartureTime + departureSchedulingDelay, event.eventTime);

				if(depDelay > 0){Main.p(f.id + " wackness ");}
				departureSchedulingDelay += depDelay;
				f.departureAirportDelay += depDelay;
				//ARRIVAL
				int arrDelay = airports.getSoonestArrival(f, proposedArrivalTime + departureSchedulingDelay, event.eventTime);
				departureSchedulingDelay += arrDelay;
				f.arrivalAirportDelay += arrDelay;
				diff = arrDelay;
			}
	 */
	//if(f.id==-851)Main.p(f.departureTimeProposed + " main proposedDepartureTime + departureSchedulingDelay " + proposedDepartureTime + " "+ departureSchedulingDelay+"\n");
	//int shouldBeZero = airports.schedulePriorityDeparture(f, proposedDepartureTime + departureSchedulingDelay, event.eventTime);
	//Main.Assert(shouldBeZero==0, "shouldBeZero not 0");
	//schedule arrival at time of departure scheduling;
	//shouldBeZero = airports.scheduleArrival(f, proposedArrivalTime + departureSchedulingDelay, event.eventTime);
	//Main.Assert(shouldBeZero==0, "shouldBeZero not 0");
	//f.atcGroundDelay = departureSchedulingDelay; //OVER WRITES, OK????
	//f.departureAirportDelay = f.departureTimeFinal - proposedDepartureTime;
	//f.arrivalAirportDelay = f.arrivalTimeFinal - proposedArrivalTime;
	/*
			airports.schedulePriorityDeparture(f, proposedDepartureTime, event.eventTime);	
		} else {
			airports.scheduleNonPriorityDeparture(f, proposedDepartureTime, event.eventTime);
			//schedule 30min in before arrival.
			//
		}
		proposedArrivalTime += f.atcGroundDelay;
		schedulingQueue.add(new SchedulingEvent((proposedArrivalTime - 30*(int) Flight.toMinutes), proposedArrivalTime, ScheduleMode.scheduleArrival, f));
		//if(f.departureAirport.equals("KCLT"));airports.getDepartureAirport(f).printDepTrafficByFlight();
		////////////////////nevermind this junk, add in code for event to schedule arrival 30min from landing

		int pushback = f.departureTimeProposed + f.gate_perturbation + delayDelta;
		int wheelsOffTime = pushback + f.taxi_unimpeded_time + f.taxi_perturbation; //add in taxi??
		f.wheelsOffTime = wheelsOffTime;
		int lastOnTimeDeparturePoint = f.arrivalFirstSlot - fastestDuration; 
		//totalGroundDelay+=f.departureDelayFromArrivalAirport/60000.0;
		if (wheelsOffTime > lastOnTimeDeparturePoint){
			//flight leaves too late.
			/////////schedulingQueue.add(new SchedulingEvent(lastOnTimeDeparturePoint, -8, ScheduleMode.removeByFlight, f));// -8 dummy value
			///////////schedulingQueue.add(new SchedulingEvent(wheelsOffTime, -4, ScheduleMode.scheduleInTheAirByFlight, f));
		}

	}
	 */


	public void writeToAirports(String workingDirectory, String name, int montecarlo, boolean writeNames){ 
		//write by airport
		double totalAirport = 0;
		try{								//WRITE DISPENSED
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+"delay_dispensed_by_airport_"+name,true);
			BufferedWriter out = new BufferedWriter(fstream);
			//			out.write("name,delay(hrs)\n");
			//
			//			for (Enumeration<String> e = dispensedAirportDelayHrs.keys(); e.hasMoreElements();){
			//				String a = e.nextElement();
			//				//Main.p(a+" " + dispensedAirportDelayHrs.get(a)/montecarlo);
			//				out.write(a+"," + dispensedAirportDelayHrs.get(a)/montecarlo+"\n");
			//				totalAirport+=dispensedAirportDelayHrs.get(a)/montecarlo;
			//			}
			//			out.close();
			//Main.p("total arrivalAirport dispensed" + totalAirport);

			if(writeNames){
				for (Enumeration<String> e = dispensedAirportDelayHrs.keys(); e.hasMoreElements();){
					String aName = e.nextElement();
					//Main.p(a+" " + absorbedAirportDelayHrs.get(a)/montecarlo);
					out.write(aName+",");
				} out.write("\n");

			}

			for (Enumeration<String> e = dispensedAirportDelayHrs.keys(); e.hasMoreElements();){
				String aName = e.nextElement();
				//Main.p(a+" " + absorbedAirportDelayHrs.get(a)/montecarlo);
				out.write(dispensedAirportDelayHrs.get(aName)+",");
			} out.write("\n");
			out.close();

		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		//Main.p(totalAirport + " " + (avgDoubles[2]+avgDoubles[3]));
		totalAirport = 0;
		//WRITE ABSORBED
		try{
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+"delay_absorbed_by_airport_"+name,true);
			BufferedWriter out = new BufferedWriter(fstream);

			if(writeNames){
				for (Enumeration<String> e = absorbedAirportDelayHrs.keys(); e.hasMoreElements();){
					String aName = e.nextElement();
					//Main.p(a+" " + absorbedAirportDelayHrs.get(a)/montecarlo);
					out.write(aName+",");
				} out.write("\n");

			}

			for (Enumeration<String> e = absorbedAirportDelayHrs.keys(); e.hasMoreElements();){
				String aName = e.nextElement();
				//Main.p(a+" " + absorbedAirportDelayHrs.get(a)/montecarlo);
				out.write(absorbedAirportDelayHrs.get(aName)+",");
			} out.write("\n");			
			out.close();

		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}



	public void writeOut1Run(String workingDirectory, String name, ArrayList<Flight> flightList){
		Collections.sort(flightList, new flightIDComparator()); 
		//*
		try{
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+"distribution_"+name);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("flight id," +
					//events
					"scheduled gate? departure time," +
					"wheels off time," +
					"wheelsoff-pushback offset min," + // for debugging

					"scheduled arrival time," +
					"first arrival slot," +
					"final arrival slot," +
					"final-first arrival offset min," + // debug

					//uncertainties
					"unimpeded taxitime," +
					"taxi perturbation," +
					"gate perturbation," +
					"gate uncertainty," + 

					//delays
					"ground delay," +
					"airborne delay," +
					"number of jiggles," +
					"total jiggle amount after last scheduling" + 
					"\n");

			for (Flight f: flightList){
				out.write(f.id +"," +
						//events
						f.departureTimeACES +","+
						f.wheelsOffTime + "," +
						(f.wheelsOffTime - f.departureTimeACES) + "," + // for debugging

						f.arrivalTimeACES +","+ 
						f.arrivalFirstSlot +","+
						f.arrivalTimeFinal +","+
						(f.arrivalTimeFinal - f.arrivalFirstSlot) + "," + // debug

						////uncertainties
						f.taxi_unimpeded_time + "," + 
						f.taxiUncertainty + "," + 
						f.gate_perturbation + "," + 
						f.gateUncertainty+ "," +

						//delays
						f.atcGroundDelay +","+ 
						f.atcAirDelay+ "," + 
						f.numberOfJiggles +","+ 
						f.totalJiggleAmount + 
						"\n"
						);	
			}
			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

	}

	public void writeOut(ArrayList<Flight> flightList, String workingDirectory, boolean prior){
		Collections.sort(flightList, new flightIDComparator());
		double totalGroundDelay = 0, totalAirDelaylAirDelay = 0; double totalAirDelaylDelay = 0;
		String name = "at_call_for_release_schedule";
		if(prior) name = "prior_to_call_for_release_schedule"; 
		//*
		try{
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+name+".csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("flight id," +
					"unimpeded taxitime," +
					"taxi perturbation," +
					"gate perturbation," +
					"ground delay," +
					"airborne delay," +
					"scheduled departure time," +
					"wheeld off time," +
					"scheduled arrival time," +
					"wheels on time\n");

			for (Flight f: flightList){
				out.write(f.id +"," + 
						f.taxi_unimpeded_time + "," + 
						f.taxiUncertainty + "," + 
						f.gate_perturbation + "," + 
						f.atcGroundDelay +","+ 
						f.atcAirDelay+ "," + 
						f.departureTimeACES +","+ 
						f.wheelsOffTime + "," + 
						f.arrivalTimeACES +","+ 
						f.arrivalTimeFinal +"\n"
						);	
				totalGroundDelay += f.atcGroundDelay/3600000.0;
				totalAirDelaylAirDelay += f.atcAirDelay/3600000.0;
			}
			//Close the output stream
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		//Main.p(a + " a b " + b); 		" totalAirDelayl flightList: " + flightList.size() +
		//Main.p(name +  "\ntotalAirDelayL: " + Math.round((totalAirDelaylAirDelay+totalGroundDelay))+" hrs\nground delay: " + Math.round(totalGroundDelay)
		//		+ " hrs \nairborne delay: " + Math.round(totalAirDelaylAirDelay) + " hrs");

	}

	// std is the sqrt of sum of variance (values-mean) squared divided by n (n-1 for sample std)
	// Change ( n - 1 ) to n if you have complete data instead of a sample.
	public static double standardDeviation(Double data[])
	{
		final int n = data.length;
		// return false if n is too small
		if(n<2) return Double.NaN;
		// Calculate the mean
		double mean = 0;
		for (int i=0; i<n; i++){mean += data[i];}
		mean /= n;
		// calculate the sum of squares
		double sum = 0;
		for ( int i=0; i<n; i++ ){
			final double v = data[i] - mean;
			sum += v*v;
		}
		return Math.sqrt(sum /n);
	}

	public void testrt(){
		java.util.PriorityQueue<SchedulingEvent> sr = new java.util.PriorityQueue<SchedulingEvent>();
		/*
		sr.add(new SchedulingEvent(9, -34));
		sr.add(new SchedulingEvent(3, -64));
		sr.add(new SchedulingEvent(1, 34));
		sr.add(new SchedulingEvent(6, 5));
		sr.add(new SchedulingEvent(5, 2));
		sr.add(new SchedulingEvent(99, 1));
		sr.add(new SchedulingEvent(2, -3));
		//*/
		while(!sr.isEmpty()){
			sr.poll().print();
		}

	}





}
