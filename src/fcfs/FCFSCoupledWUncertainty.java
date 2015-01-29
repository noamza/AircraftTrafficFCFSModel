package fcfs;

import java.util.*;
import java.io.*;

/*TODO:
 * 
 * check departure rates are being loaded in and used
 * 
 */

/*
 * This Scheduler simulates mixing flights that are scheduled at departure (cfr flights) with flights that are scheduled in the air at a freeze horizon (non-cfr flights). 
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


	public void printResults(ArrayList<Flight> flightList, String dir){}

	java.util.PriorityQueue<SchedulingEvent> schedulingQueue;
	Flights flights; 
	Airports airports;
	static double speedUp = 0.025; //0.025; 
	static double slowDown = 0.05; //0.05
	int rand = 0;
	Hashtable<String, Double> dispensedAirportDelayHrs;
	Hashtable<String, Double> absorbedAirportDelayHrs;
	//static java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("DDD:HH:mm:ss:SSSS");
	String ofolder = U.outFolder;
	String infolder = U.inputFolder;
	String workingDirectory = U.workingDirectory;
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
						"ground_delay_non_external_min", "air_delay_non_external_min",
						"weighted_delay_non_external_min"
						};

	//Hashtable<String, File> filesFreezeSchedulinghorizonColumnList = new Hashtable<String, File>();

	//File file = new File("C:/a");

	//initialize result structures
	int watching = -1;
	
	public FCFSCoupledWUncertainty(){
		rand = Math.abs(new java.util.Random().nextInt());
	}
	
	public FCFSCoupledWUncertainty(int mont){
		workingDirectory = U.workingDirectory;
		montecarlo = mont;
		rand = Math.abs(new java.util.Random().nextInt());
	}

	/*
	algo description
	 */

	void load(String inputs, Flights flights, Airports airports){
		//flights.loadFlightsFromAces(workingDirectory+"clean_job.csv",false);
		airports.loadCapacitiesFromAces(inputs+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
		airports.loadDelays(inputs+"gate_delay.csv", "gate");	
		airports.loadDelays(inputs+"taxi_delay.csv", "taxi");
		airports.loadDelays(inputs+"taxi_u.csv", "taxi");
		flights.loadTaxiOffset(inputs+"AirportTaxi.csv");
		flights.loadCallSigns(inputs + "job_611_airline_flightid_map.csv");
		//System.out.println("		loaded " + dateFormat.format(new Date()));
		flights.pushFlightsForwardInTime(10*(int)U.toHours); // CHANGE //10
		airports.offsetCapacities(10*(int)U.toHours); //CHANGE //10 
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
	boolean allCFR = false;
	boolean noneCFR = false;
	int montecarlo = -1;
	
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
		
		U.Assert(!(allCFR&&noneCFR), "allCFR&&noneCFR");

		ScheduleMode modes[] = { ScheduleMode.IAHCFR };

		U.p("monte carlo: " + montecarlo + " estimated time " + 3*montecarlo/100 + " min");
		U.p("limited uncertainty:" + limitedCFRUncertainty + " all cfr:" + allCFR + " none cfr:" + noneCFR);

		//INIT FILES
		String dname = montecarlo + "_limited_CFR_Uncertainty_" +limitedCFRUncertainty +"_allcfr_"+allCFR+"_nonecfr_"+noneCFR;
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
			Collections.sort(flightList, new flightDepTimeIDComparator());

			flights.resetPerturbationAndSchedulingDependentVariables();

			// SET KIAH CFR
			airports.getAirport("KIAH").setCFR(0, Integer.MAX_VALUE);

			for (Flight f: flightList){
				//the more random a flight departure is, the less delay.
				AirportTree departureAirport = airports.airportList.get(f.departureAirport);
				int gate_noise_seconds = 0, taxi_noise_seconds = 0;		
				//Get gate and taxi perturbations
				f.gateUncertaintyConstant = random.nextGaussian();
				//U.p(f.gateUncertaintyConstant);
				
				if(departureAirport!=null){
					double gateR = random.nextDouble(), taxiR = random.nextDouble();
					//U.p(gateR + " gate taxi " + taxiR + " " + departureAirport.taxiUnimpeded + " " + departureAirport.gateStd + " " + departureAirport.taxiMean);
					f.taxi_unimpeded_time = (int)(departureAirport.taxiUnimpeded)*60000;
					f.gate_perturbation = 0; /////////////////GATE PERTURBATION SET TO 0 //////////////////////////
					
					if(pertrubTaxi && departureAirport.taxiZeroProbablity < taxiR){
						double taxi_noise_minutes = Math.exp(random.nextGaussian()*departureAirport.taxiStd + departureAirport.taxiMean);
						taxi_noise_minutes = taxi_noise_minutes < 45? taxi_noise_minutes: 45;
						taxi_noise_seconds = (int)(taxi_noise_minutes*60000);
						f.taxiUncertainty = taxi_noise_seconds;
						if(taxi_noise_minutes == 1){
							U.p(departureAirport.taxiZeroProbablity + " " /*c++*/ + " " + departureAirport.airportName);
							f.taxiUncertainty = defaultPertMills;
						}
					}
					//for null airports on first run

				} else {
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
							
							for (@SuppressWarnings("unused") String name: columns){
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
									String s[] = U.timeToDateAdjustedShort(f.arrivalTimeACES).split(":");
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
									f.priority = true;
								}
								if(allCFR)f.cfrEffected = true;
								if(noneCFR)f.cfrEffected = false;
								U.Assert(f.arrivalAirport.equals("KIAH"));

								//DETERMINING WHEN FLIGHTS ARE FIRST SCHEDULED
								int departureSchedulingTime = f.departureTimeACES + f.gate_perturbation; //gate perutbation all 0
								
								U.Assert(f.gate_perturbation==0,"gate_p should be 0 " + f.gate_perturbation);

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
										//plus1minus2 = plus1minus2 < 1? plus1minus2: 0; // turns -'s to 0's
										plus1minus2 -= 2; // -2 +1
										//U.p(plus1minus2 + " after");
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
								U.p("error in switch1 should not be here");
							}

							} //END SWITCH


						} // end (F: FLIGHTLIST) loop 
						//print demand
						if(schedulingHorizon == 0){
							if(freezeHorizon==30){
							}
						}

						@SuppressWarnings("unused")
						SchedulingEvent prevEvent = new SchedulingEvent(0, 0, ScheduleMode.scheduleDeparture, Flight.dummyDeparture(0));
						
						while(!schedulingQueue.isEmpty()){ // while there are events to process (main loop)

							//execute earliest event;
							SchedulingEvent event = schedulingQueue.remove();


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
								U.p("should not be here");
								System.err.println("EVENT ERROR SHOULD NOT BE HERE");
							}
							break;
							default:
							{
								U.p("should not be here");
								System.err.println("EVENT ERROR SHOULD NOT BE HERE");

							}

							U.Assert(false, "should not be here in event loop");
							break;

							} //end switch statement
							//U.p(prevTime + " " + event.eventTime);
							prevEvent = event;

						} //END WHILE OF EVENTS
						if(watching>0){ U.p(watching +" *** "); }
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
									U.Assert(f.atcGroundDelay + f.atcAirDelay == f.arrivalAirportAssignedDelay);

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


								arrivalAirportDelay += f.arrivalAirportAssignedDelay;
								all++;
							}
						}
						write(files.get(currentFilePrefix+columns[4]), ""+(arrivalAirportDelay/U.toHours));

					} //END sched HOR
				} //END freeze HOR
			} // END by Mode

		} 
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

		//U.p(header); //ADD HEADER
		for (int fh: fhk){
			for (int sh: shk){
				for (String col: columns){
					
					String t = montecarlofolder+fh+'_'+sh+'_'+col+".csv";
					Hashtable<String, Double> results = Stats.count_sum_mean_std_min_max(read(t));
					
					//*
					U.pf("%3d,%2d,%-40s%4.1f,%4.1f,%4.1f,%4.1f,%.0f\n",
							fh,sh,col+',', results.get("mean"),results.get("std"),
							results.get("min"),results.get("max"),results.get("count"));//*/
					
					String line = String.format("%d,%d,%s,%.1f,%.1f,%.1f,%.1f,%.0f",
							fh,sh,col,
							results.get("mean"),
							results.get("std"),
							results.get("min"),
							results.get("max"),
							results.get("count")
							);	
					
					//U.p(line);
					
					write(files.get(col), line);
				}
			}
		}
		
		
		
		for (String col: files.keySet()){
			close(files.get(col));
		}
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
	
	public void writeOver(BufferedWriter out){
		try{
			out.write("empty to save more space\n");
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
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
		int currentTime = event.eventTime;
		if(f.id==watching){ U.p(f.id + " scheduleDepartureCFR at " + currentTime); }
		//U.pf("(%.1f,%.1f)\n", f.gateUncertainty/U.toMinutes, f.taxi_perturbation/U.toMinutes);
		
		U.Assert(f.cfrEffected);
		U.Assert(f.gateUncertainty >= 0);
		U.Assert(currentTime == f.gate_perturbation + f.departureTimeACES - U.toMinutes*schedulingHorizon);
		U.Assert(f.gate_perturbation==0);
		int departureAdditives = + f.taxi_unimpeded_time + f.gate_perturbation; //gate perturbation?? added twice
		f.departureTimeFinal = f.departureTimeACES + departureAdditives;
		schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.scheduleArrival, f));

	}

	public void scheduleArrivalCFR(SchedulingEvent event){
		Flight f = event.flight;
		int currentTime = event.eventTime;
		if(f.id==watching){ U.p(f.id + " scheduleArrivalCFR at " + currentTime); }
		U.Assert(f.cfrEffected);
		U.Assert(f.departureTimeFinal > 1);
		U.Assert( f.gate_perturbation == 0);
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
			
			int wheelsOff = f.departureTimeACES + f.taxi_unimpeded_time +f.taxiUncertainty + f.gate_perturbation
					+ Math.max(f.gateUncertainty, f.atcGroundDelay); //no gate perturbation!!!!
			U.Assert(wheelsOff >= currentTime,"wheelsOff < currentTime");//make sure wheels off comes after scheduling..
			schedulingQueue.add(new SchedulingEvent(wheelsOff, -8, ScheduleMode.WheelsOff, f));
		}
		
		//second time being scheduled
		if(!f.firstTimeBeingArrivalScheduled){
			U.Assert(currentTime == f.departureTimeFinal,currentTime +" "+f.departureTimeFinal + " " + f.id);
			int nominalDuration = f.arrivalTimeACES-f.departureTimeACES;
			int shortestDuration =   (int)(nominalDuration/(1+speedUp));
			int longestDuration =  (int)(nominalDuration/(1-slowDown));
			//can be leaving later or earlier
			U.Assert(event.coEventTime == f.departureTimeFinal + shortestDuration || event.coEventTime == f.departureTimeFinal + longestDuration, 
					event.coEventTime+ " "+  f.departureTimeFinal + shortestDuration + " f.id " + f.id + " cfr " + f.cfrEffected);
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
		int currentTime = event.eventTime;
		if(f.id==watching){ U.p(f.id + " WheelsOffCFR at " + currentTime); }
		U.Assert(f.cfrEffected);
		U.Assert(f.departureTimeFinal > 1);
		//U.Assert(currentTime >= f.departureTimeFinal, currentTime + " wheelsOff final " +f.departureTimeFinal+ " no earlies " + f.id + " " + freezeHorizon);//for this case
		int nominalDuration  = f.arrivalTimeACES-f.departureTimeACES;
		int shortestDuration = (int)(nominalDuration/(1+speedUp)); 
		int longestDuration  = (int)(nominalDuration/(1-slowDown));
		//U.p((nominalDuration-longestDuration)/U.toMinutes);

		U.Assert(currentTime > f.departureTimeACES+f.gate_perturbation+f.taxiUncertainty);

		//flight leaves too early
		//if(currentTime < f.departureTimeFinal)U.p("leaving early, awe yiss");
		//if(f.id == 4) U.p(currentTime +" "+f.departureTimeFinal + " " + f.id);
		f.firstTimeBeingArrivalScheduled = false;
		
		if(currentTime < f.departureTimeFinal && currentTime+longestDuration < f.arrivalTimeFinal){
			//U.p( currentTime + " current final, leaving to early need to reschedule" +f.departureTimeFinal);
			f.departureTimeFinal = currentTime;
			schedulingQueue.add(new SchedulingEvent(currentTime, -8, ScheduleMode.removeFromArrivalQueue, f));
			schedulingQueue.add(new SchedulingEvent(currentTime, currentTime+longestDuration, ScheduleMode.scheduleArrival, f));

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
		//add taxi uncertainty
		//check departure uncertainty in general?
		Flight f = event.flight;		
		int currentTime = event.eventTime;
		if(f.id==watching){ U.p(f.id + " scheduleDepartureNonCFR at " + currentTime); }
		U.Assert(!f.cfrEffected);
		U.Assert(f.gate_perturbation == 0);
		int departureAdditives = f.taxi_unimpeded_time + f.gate_perturbation; //gate perturbation?? set to 0 at this point
		f.departureTimeFinal = f.departureTimeACES + departureAdditives;
		int gateDeparture = f.departureTimeACES + f.gate_perturbation;
		int proposedArrivalTime = f.arrivalTimeACES + departureAdditives;
		//flight will schedule arrival the amount of the freeze horizon before arrival
		int freezeHorizonMil = freezeHorizon*(int) U.toMinutes;
		//so that don't schedule arrival before departure, otherwise schedule x minutes before arrival
		//int timeToScheduleArrival = Math.max(proposedArrivalTime - freezeHorizonMil, f.getDepartureTimeFinal());
		//SCHEDULES AT FREEZE HORIZON AMOUNT AWAY FROM ARRIVAL OR GATE DEPARTURE DEPENDS ON WHICH IS BIGGER
		U.Assert(gateDeparture == currentTime, "non gateDeparture == currentTime");
		int timeToScheduleArrival = Math.max(proposedArrivalTime - freezeHorizonMil, gateDeparture);
		schedulingQueue.add(new SchedulingEvent(timeToScheduleArrival, proposedArrivalTime, ScheduleMode.scheduleArrival, f));
	}

	//check if flight can make it by speeding up/slowing down,
	public void WheelsOffNon(SchedulingEvent event){
		//should have ground delay? what to do with it??
		//delete if don't need it
		//use it if there, delete rest if partway through..
		//U.p("sdfsdfsfd");
		Flight f = event.flight;
		int currentTime = event.eventTime;
		if(f.id==watching){ U.p(f.id + " WheelsOffNon at " + currentTime); }
		U.Assert(f.departureTimeFinal > 1);
		//U.Assert(currentTime >= f.getDepartureTimeFinal(), currentTime + " wheelsOff final " +f.getDepartureTimeFinal()+ " no earlies " + f.id + " " + freezeHorizon);//for this case
		int nominalDuration = f.arrivalTimeACES-f.departureTimeACES;
		int shortestDuration = (int)(nominalDuration/(1+speedUp)); 
		int longestDuration =  (int)(nominalDuration/(1-slowDown));
		//U.p((nominalDuration-longestDuration)/U.toMinutes);

		//U.Assert(currentTime > f.departureTimeACES+f.gate_perturbation+f.taxiUncertainty);

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
		//U.p(++ty);
		U.Assert(!event.flight.cfrEffected,"!event.flight.CFRaffected");
		Flight f = event.flight;
		int currentTime = event.eventTime;
		if(f.id==watching){ U.p(f.id + " scheduleArrivalNonCFR at " + currentTime); }
		int proposedArrivalTime = event.coEventTime;
		U.Assert(f.atcAirDelay == 0, "f.atcAirDelay == 0");
		//there could be ground delay added from adjusting the arrival Queue, which would mean still more than 30min from arrival.
		if(f.firstTimeBeingArrivalScheduled){
			U.Assert(f.atcGroundDelay == 0, "f.atcAirDelay == 0");
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
		int currentTime = event.eventTime;
		if(f.id==watching){ U.p(f.id + " removeFromDepartureQueue at " + currentTime); }
		airports.removeFlightFromDepartureQueue(f);
	}

	public void removeFromArrivalQueue(SchedulingEvent event)
	{
		Flight f = event.flight;
		int currentTime = event.eventTime;
		if(f.id==watching){ U.p(f.id + " removeFromArrivalQueue at " + currentTime); }
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

	}

	public void writeToAirports(String workingDirectory, String name, int montecarlo, boolean writeNames){ 
		//write by airport
		double totalAirport = 0;
		try{								//WRITE DISPENSED
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+"delay_dispensed_by_airport_"+name,true);
			BufferedWriter out = new BufferedWriter(fstream);

			if(writeNames){
				for (Enumeration<String> e = dispensedAirportDelayHrs.keys(); e.hasMoreElements();){
					String aName = e.nextElement();
					//U.p(a+" " + absorbedAirportDelayHrs.get(a)/montecarlo);
					out.write(aName+",");
				} out.write("\n");

			}

			for (Enumeration<String> e = dispensedAirportDelayHrs.keys(); e.hasMoreElements();){
				String aName = e.nextElement();
				//U.p(a+" " + absorbedAirportDelayHrs.get(a)/montecarlo);
				out.write(dispensedAirportDelayHrs.get(aName)+",");
			} out.write("\n");
			out.close();

		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		//U.p(totalAirport + " " + (avgDoubles[2]+avgDoubles[3]));
		totalAirport = 0;
		//WRITE ABSORBED
		try{
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+"delay_absorbed_by_airport_"+name,true);
			BufferedWriter out = new BufferedWriter(fstream);

			if(writeNames){
				for (Enumeration<String> e = absorbedAirportDelayHrs.keys(); e.hasMoreElements();){
					String aName = e.nextElement();
					//U.p(a+" " + absorbedAirportDelayHrs.get(a)/montecarlo);
					out.write(aName+",");
				} out.write("\n");

			}

			for (Enumeration<String> e = absorbedAirportDelayHrs.keys(); e.hasMoreElements();){
				String aName = e.nextElement();
				//U.p(a+" " + absorbedAirportDelayHrs.get(a)/montecarlo);
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

}
