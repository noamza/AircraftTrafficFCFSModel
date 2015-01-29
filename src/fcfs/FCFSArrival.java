package fcfs;

import java.util.*;
import java.io.*;

/**
 * 
 * @author nalmog
 *
 *NOTE: this class was refactored to use scheduling by Flight object instead of Integer on 1/28/15, it may not behave exactly as before and should be checked. 
 *For the version corresponding to Delay Sensitivity paper use previous version of repository. Regardless behavior should be very close.
 *
 */

/*
 * used for Delay Sensitivity to Call For Release Scheduling Time, Kee Palopo, Gano Broto Chatterji, and Noam Almog, 12th AIAA Aviation Technology, Integration, and Operations (ATIO) Conference, September 2012
 * See readme for more documentation.
 * 
 */

//Different modes of scheduling.
//For explanation see: Delay Sensitivity to Call For Release Scheduling Time, Kee Palopo, Gano Broto Chatterji, and Noam Almog, 12th AIAA Aviation Technology, Integration, and Operations (ATIO) Conference, September 2012
//These tell the event queue which mode to schedule in.
enum Action {
	remove,
	undef,

	FCFS, //first come first served by departure order
	FCFSUncertainty, //same but with uncertainty
	ArrivalJiggle, //Schedules flights by order of departure, but fills in gaps in arrival queue as it goes along and 'jiggles' other flights accordingly (delays them). 
	ArrivalJiggleUncertainty,//Same with uncertainty
	ArrivalJiggleNoDepartureContract,//Same but allows added delay from jiggling of flights to be taken on the ground if flights haven't departed at time of scheduling.
	ArrivalJiggleUncertaintyNoDepartureContract, //same but with uncertainty.
	FCFSArrWithNoGateUncertainty,//only taxi uncertainty(?)
	scheduleInTheAir,//for when flights re-scheduled after departure and need to be re-scheduled in the air.
	scheduleByArrival,//schedules all flights in the order they arrive: least amount of delay

	removeByFlight,//same as above corresponding cases, but switched to Flight vs Int scheduling.
	scheduleInTheAirByFlight,//same

	scheduleAtPushback, //used in FCFSFlexibleSpeed, should be refactored there
	scheduleAtPDT, //used in FCFSFlexibleSpeed, should be refactored there
}

class rescheduleEvent implements Comparable<rescheduleEvent>{
	int eventTime = 0;
	int targetTime = 0;
	Action mode = Action.undef;
	Flight flight;
	public rescheduleEvent(int et, int st, Action m, Flight f){ 
		eventTime= et; targetTime = st; mode = m; flight = f;}	
	public rescheduleEvent(int et, int st){eventTime=et; targetTime=st;}
	void print(){ System.out.printf("r time: %d s time: %d\n", eventTime, targetTime);}
	public int compareTo(rescheduleEvent o) { //orders priorityqueue by least time
		if(eventTime == ((rescheduleEvent)o).eventTime){
			return flight.id - ((rescheduleEvent)o).flight.id;
		}
		return eventTime-((rescheduleEvent)o).eventTime;
		//return ((rt)o).rescheduleTime - rescheduleTime; //order's priorityqueue by greatest first
	}
}




public class FCFSArrival {
	
	Hashtable<Integer, Integer> delayedIntheAir = new Hashtable<Integer, Integer>();
	
	final static double toMinutes = 60000, toHours = 3600000;
	final static int minToMillisec = 1000*60;
	static double speedUp = 0.025; //0.025; 
	static double slowDown = 0.05;
	int rand = 0;
	Hashtable<String, Double> dispensedAirportDelayHrs;
	Hashtable<String, Double> absorbedAirportDelayHrs;
	static java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("DDD:HH:mm:ss:SSSS");
	String ofolder = U.outFolder;
	String infolder = U.inputFolder;
	String workingDirectory = U.workingDirectory;
	public FCFSArrival(){
		rand = Math.abs(new java.util.Random().nextInt());

	}

	/*

	 ALL RUNS ARE MADE AT 0, 15, 30, 45, 60 MIN IN ADVANCE	 

	 1.  FCFS DEP, ANNOUNCED ESTIMATE 10 MINUTES IN ADVANCE, CAN'T ADJUST ANNOUNCED FLIGHTS.

	 2.  SAME AS 1 WITH UNCERTAINTY. IF YOU MISS A SLOT, RESCHEDULE, RECYCLE, AND RE-ASSIGN SLOT

	 3.	 SAME AS 1 EXCEPT FCFS -> PFCFS, ALLOW JIGGLING UP TO ARRIVAL.

	 4.  SAME AS 2 WITH UNCERTAINTY

	 5.  SAME AS 3,4 BUT SCHEDULAR HAS UP TO 10 MIN BEFORE ESTIMATED DEPARTURE TO RESPOND WITH DELAYS. (?)

	 ESTIMATE = GATE DEP PERTURBATION + UNIMPEDED TAXI	 
	 ACTUAL GATE PERTUBATION = ABS GAUSSIAN OF 0,2,4,6,8 MIN FOR CORRESPENDING # OF MIN IN ADVANCE
	 WHEELS OFF = ESTIMATE + AGT + TP

	 */

	void load(String inputs, Flights flights, Airports airports){
		//flights.loadFlightsFromAces(workingDirectory+"clean_job.csv",false);
		airports.loadCapacitiesFromAces(inputs+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
		airports.loadDelays(inputs+"gate_delay.csv", "gate");	
		airports.loadDelays(inputs+"taxi_delay.csv", "taxi");
		airports.loadDelays(inputs+"taxi_u.csv", "taxi");
		flights.loadTaxiOffset(inputs+"AirportTaxi.csv");
		flights.loadCallSigns(inputs + "job_611_airline_flightid_map.csv");
		flights.pushFlightsForwardInTime(10*(int)U.toHours); //pushes simulation forward in time so no negative values CHANGE //10 
		airports.offsetCapacities(10*(int)U.toHours);  //pushes simulation forward in time so no negative values //CHANGE //10  
		//TODO: airports.loadCFRdata();
	} //END LOAD()

	public void schedule(String dateId){
		System.out.println("Start " + dateFormat.format(new Date()));
		dispensedAirportDelayHrs = new Hashtable<String, Double>();
		absorbedAirportDelayHrs = new Hashtable<String, Double>();
		Flights flights  = new Flights(); 
		Airports airports = new Airports(); 
		flights.loadFlightsFromAces(workingDirectory+infolder+"clean_job.csv",false);
		load(workingDirectory+infolder, flights, airports);

		//RUN SETTINGS
		final boolean pertrubGate = true; //gate taxi pert on/off (true false)
		final boolean pertrubTaxi = true; //used TRUE FOR 100000
		int uncertaintyToggle = 1; //gate uncertainty on/off (0 1)
		final int montecarlo = 1;
		int counter = 0; // number of monte carlo runs
		int defaultPertMills = 0;//1*60000;
		java.util.Random random = new java.util.Random(98);//98);//rand);//9 85); //used 98 for 100000 //6 it goes up //11 goes up


		Action modes[] = { 	
				Action.FCFS,
				//Action.FCFS,
				//Action.FCFSUncertainty,
				//Action.ArrivalJiggleNoDepartureContract,
				//Action.ArrivalJiggleUncertaintyNoDepartureContract,
				//Action.ArrivalJiggle,
				//Action.ArrivalJiggle,
				//Action.ArrivalJiggleUncertainty,
				//Action.scheduleByArrival
				//Action.FCFSArrWithNoGateUncertainty
		};	

		//MAIN MONTE CARLO LOOP
		
		while (counter < montecarlo) {
			counter++;
			ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
			java.util.PriorityQueue<rescheduleEvent> rescheduleQueue = new java.util.PriorityQueue<rescheduleEvent>();
			Collections.sort(flightList, new flightDepTimeIDComparator());
			
			try{
			if(counter==1)calculateDelays(flightList, "SWA", false, new BufferedWriter(new FileWriter(workingDirectory+ofolder+"ignore",true)), true, -1);
			} catch(Exception e){
				
			}
			
			int c = 0;
			//GENERATE GATE TAXI PERTURBATIONS
			flights.resetPerturbationAndSchedulingDependentVariables();

			for (Flight f: flightList){
				
				if(counter == 1){
					absorbedAirportDelayHrs .put(f.departureAirport, 0.0);
					dispensedAirportDelayHrs.put(f.arrivalAirport  , 0.0);
				}
				//the more random a flight departure is, the less delay.
				//int rm = (int)(1000*60*60*350*random.nextDouble()); f.arrivalTimeProposed+=rm; f.departureTimeProposed+=rm;
				AirportTree departureAirport = airports.airportList.get(f.departureAirport);
				int gate_noise_seconds = 0, taxi_noise_seconds = 0;		
				//Get gate and taxi perturbations
				if(departureAirport!=null){
					double gateR = random.nextDouble(), taxiR = random.nextDouble();
					//U.p(gateR + " gate taxi " + taxiR + " " + departureAirport.taxiUnimpeded + " " + departureAirport.gateStd + " " + departureAirport.taxiMean);
					f.taxi_unimpeded_time = (int)(departureAirport.taxiUnimpeded)*60000;
					if(pertrubGate && departureAirport.gateZeroProbablity < gateR){
						double gate_noise_minutes = Math.exp(random.nextGaussian()*departureAirport.gateStd + departureAirport.gateMean);
						gate_noise_minutes = gate_noise_minutes < 120? gate_noise_minutes: 120;
						gate_noise_seconds = (int)(gate_noise_minutes*60000);
						f.gate_perturbation = gate_noise_seconds;
						//ERROR OR NOT??
						if(gate_noise_minutes == 1) f.gate_perturbation = defaultPertMills;
						//f.gate_perturbation = 0;
						//U.p("random");
					}
					if(pertrubTaxi && departureAirport.taxiZeroProbablity < taxiR){
						double taxi_noise_minutes = Math.exp(random.nextGaussian()*departureAirport.taxiStd + departureAirport.taxiMean);
						taxi_noise_minutes = taxi_noise_minutes < 45? taxi_noise_minutes: 45;
						taxi_noise_seconds = (int)(taxi_noise_minutes*60000);
						f.taxiUncertainty = taxi_noise_seconds;
						//ERROR OR NOT??
						if(taxi_noise_minutes == 1){
							U.p(departureAirport.taxiZeroProbablity + " " + c++ + " " + departureAirport.airportName);
							f.taxiUncertainty = defaultPertMills;
						}
					}
					
				} else {
				}
			}


			for(Action mode: modes){
				U.p("");
				
				if(counter != 0)U.p(mode + " c " + counter ); 


				for(int minsAhd = 0; minsAhd <= 0; minsAhd += 60){//: minutesAhead){afss++;										
					//RESET TO 0
					flights.resetSchedulingDependentVariables();
					airports.resetToStart();
					//SET AIRPORT DATA TO 0
					for (Enumeration<String> e = absorbedAirportDelayHrs.keys(); e.hasMoreElements();){
						String aName = e.nextElement();
						absorbedAirportDelayHrs.put(aName, 0.0);
					}
					for (Enumeration<String> e = dispensedAirportDelayHrs.keys(); e.hasMoreElements();){
						String aName = e.nextElement();
						dispensedAirportDelayHrs.put(aName, 0.0);
					}

					int lookAheadMilliSec = minsAhd*minToMillisec;
										
					int swa = 0; 
					String airline = "CFR"; //SWA airline of flights that get to be scheduled in advance
					//int iii = 0;iii++;
					System.out.println("millisec-days in an integer "+(double)Integer.MAX_VALUE/(1000.0*60*60*24));
					for (Flight f: flightList){
						lookAheadMilliSec = minsAhd*minToMillisec;
						int airlinePriorityLookAhead = 0;
						int CFRpriorityLookAhead = 0;
						
						//flights from a particular airline get scheduled ahead of others
						if(f.airline.equals(airline)){
							swa++;
							airlinePriorityLookAhead = 60*minToMillisec; //these flights get scheduled 60min in advances

						}
						
						//flight's sdt is during cfr, get's scheduled 30min in advance
						//or closest to CFR time
						if(airports.effectedByCFR(f)){
							//CFRpriorityLookAhead =  //CHECK THIS<<<<<<<<<<<<<
									//Math.min(f.departureTimeProposed - airports.getArrivalAirport(f).CFRstart, 30*minToMillisec);
							//CFRpriorityLookAhead = 0;
							f.cfrEffected = true;
							f.priority = true;
							//U.p(CFRpriorityLookAhead + " c l " + lookAheadMilliSec);
						}
						
						lookAheadMilliSec += (airlinePriorityLookAhead + CFRpriorityLookAhead);
						if(CFRpriorityLookAhead!=0){
							//U.p(CFRpriorityLookAhead + " c l " + lookAheadMilliSec);
						}

						
						//1.  FCFS DEP, ANNOUNCED ESTIMATE 10 MINUTES IN ADVANCE, CAN'T ADJUST ANNOUNCED FLIGHTS. 
						if(mode == Action.FCFS){
							//						for (Flight f: flightList){
							rescheduleQueue.add(new rescheduleEvent(f.departureTimeACES + f.gate_perturbation - lookAheadMilliSec, -9, mode, f));
							//						}
						}

						// 2.  SAME AS 1 WITH UNCERTAINTY. IF YOU MISS A SLOT, RESCHEDULE, RECYCLE, AND RE-ASSIGN SLOT
						if(mode == Action.FCFSUncertainty){
							//						for (Flight f: flightList){
							rescheduleQueue.add(new rescheduleEvent(f.departureTimeACES + f.gate_perturbation - lookAheadMilliSec, -2, mode, f));
							//						}
						}					

						//					3 with contract
						if(mode == Action.ArrivalJiggleNoDepartureContract){
							airports.turnOffDepartureContract();
							//						for (Flight f: flightList){
							rescheduleQueue.add(new rescheduleEvent(
									f.departureTimeACES  + f.gate_perturbation - lookAheadMilliSec, -4, Action.ArrivalJiggle,f));
							//						}
						}
						//					4 with contract 
						if(mode == Action.ArrivalJiggleUncertaintyNoDepartureContract){
							airports.turnOffDepartureContract();
							//						for (Flight f: flightList){
							rescheduleQueue.add(new rescheduleEvent(
									f.departureTimeACES  + f.gate_perturbation - lookAheadMilliSec, -4, Action.ArrivalJiggleUncertainty,f));
							//						}
						}


						// 3.	 SAME AS 1 EXCEPT FCFS -> PFCFS, ALLOW JIGGLING UP TO ARRIVAL.
						if(mode == Action.ArrivalJiggle){
							//						for (Flight f: flightList){
							rescheduleQueue.add(new rescheduleEvent(
									f.departureTimeACES + f.gate_perturbation - lookAheadMilliSec, -3, mode, f));
							//						}
						}
						//4.  SAME AS 2 WITH UNCERTAINTY
						if(mode == Action.ArrivalJiggleUncertainty){
							//						for (Flight f: flightList){
							rescheduleQueue.add(new rescheduleEvent(
									f.departureTimeACES  + f.gate_perturbation - lookAheadMilliSec, -4, mode,f));
							//						}
						}


						if(mode == Action.scheduleByArrival){
							//						for (Flight f: flightList){
							rescheduleQueue.add(new rescheduleEvent(
									//f.arrivalTimeProposed - lookAheadMilliSec, -4, mode,f));}}
									f.arrivalTimeACES + f.gate_perturbation + f.taxi_unimpeded_time - lookAheadMilliSec, -4, mode,f));
							//							}
						}
						//f.departureTimeProposed + f.gate_perturbation - lookAheadMilliSec, -3, mode, f));}}

						if(mode == Action.FCFSArrWithNoGateUncertainty){
							//						for (Flight f: flightList){
							rescheduleQueue.add(new rescheduleEvent(
									//f.arrivalTimeProposed - lookAheadMilliSec, -4, mode,f));}}
									f.arrivalTimeACES + f.gate_perturbation + f.taxi_unimpeded_time - lookAheadMilliSec, -4, mode,f));
							//							}
						}
						//f.departureTimeProposed + f.gate_perturbation - lookAheadMilliSec, -3, mode, f));}}


					} // end FLIGHT loop

					//					U.p(airline + " " + swa);

					String name = mode+"_"+minsAhd+"_min_ahead_"+montecarlo+"_runs_"+".csv";//+dateId
					//String ofolder = "output\\";

					try{
						//U.p(name + " " + montecarlo);
						FileWriter fstream = new FileWriter(workingDirectory+ofolder+name,true);
						BufferedWriter out = new BufferedWriter(fstream);
						if(counter==1){
							//calculateDelays(flightList, airline, false, out, true, minsAhd); //???????????????????????????????????
						}
						//U.p("***************************************************************************");
						int i2 = 0;
						while(!rescheduleQueue.isEmpty()){ // while there are events to process (main loop)
							i2++;
							//execute earliest event;
							rescheduleEvent event = rescheduleQueue.remove();
							Flight f = event.flight;
							//duration speeds
							int nominalDuration = f.arrivalTimeACES-f.departureTimeACES;
							int fastestDuration =  (int)(nominalDuration/(1+speedUp));
							
							//if(f.id==67) f.printVariables(); 
 							if(i2<10){
 								//U.p(f.id);
 								//U.p(event.eventTime + " " + event.targetTime +" "+ f.id);
 								//U.p((f.departureTimeProposed + f.gate_perturbation - lookAheadMilliSec) 
 									//	+" (f.departureTimeProposed + f.gate_perturbation - lookAheadMilliSec)");
 								//f.printVariables();
 							}

							switch (event.mode) {

							//1.  FCFS DEP, ANNOUNCED ESTIMATE 10 MINUTES IN ADVANCE, CAN'T ADJUST ANNOUNCED FLIGHTS.
							case FCFS:
							{
								int proposedArrivalTime = f.arrivalTimeACES + f.gate_perturbation + f.taxi_unimpeded_time; 
								int arrivalSlot = airports.scheduleArrival(f, proposedArrivalTime, Integer.MAX_VALUE);//(f.arrivalAirport, proposedArrivalTime); //Make Slot including perturbation
								f.arrivalFirstSlot = arrivalSlot;
								int delayFromFirstScheduling = arrivalSlot - proposedArrivalTime;
								f.atcGroundDelay = delayFromFirstScheduling; 
								//int delayDelta = Math.max(delayFromFirstScheduling-f.gate_perturbation,0); 
								//delayFromFirstScheduling = 0; //TAKE THIS OUT
								int wheelsOffTime = f.departureTimeACES+f.gate_perturbation + f.taxi_unimpeded_time + delayFromFirstScheduling;// + f.taxi_perturbation; 
								f.wheelsOffTime = wheelsOffTime;
								int lastOnTimeDeparturePoint = arrivalSlot - fastestDuration;

								//slot should be more reachable since slot is further out.
								//therefore more flights should make their slots
								//therefore LESS airborne cases
								if(wheelsOffTime > lastOnTimeDeparturePoint){

									rescheduleQueue.add(new rescheduleEvent(lastOnTimeDeparturePoint, arrivalSlot, Action.remove, f));
									rescheduleQueue.add(new rescheduleEvent(wheelsOffTime,-6, Action.scheduleInTheAir, f));// wheelsOffTime+nominalDuration
									if(wheelsOffTime < f.departureTimeACES + f.gate_perturbation){
										U.p("error scheduling in past tense");
									}
								} else {
									//flight can arrive by speeding up

									f.arrivalTimeFinal = arrivalSlot;
								}
								//if(f.id == 23672){ U.p(mode + " " + minsAhd);f.printVariables();}

							}
							break;

							// 2.  SAME AS 1 WITH UNCERTAINTY. IF YOU MISS A SLOT, RESCHEDULE, RECYCLE, AND RE-ASSIGN SLOT
							case FCFSUncertainty:
							{ //more flights have to be scheduled twice with ground since slot arrival time estimate is bad,
								//so more totalAirDelayl delay per pdt run.
								int proposedArrivalTime = f.arrivalTimeACES + f.gate_perturbation + f.taxi_unimpeded_time; 
								int arrivalSlot = airports.scheduleArrival(f, proposedArrivalTime, Integer.MAX_VALUE);//Int(f.arrivalAirport, proposedArrivalTime); //Make Slot
								f.arrivalFirstSlot = arrivalSlot;
								int delayFromFirstScheduling = arrivalSlot - proposedArrivalTime;
								//uncertainty
								int uncertaintyMilliSec = (int)(Math.abs( ( random.nextGaussian()*(minsAhd*2/15) ) )*minToMillisec)*uncertaintyToggle;
								f.gateUncertainty = uncertaintyMilliSec;
								int delayDelta = Math.max(uncertaintyMilliSec,delayFromFirstScheduling);
								f.atcGroundDelay = delayFromFirstScheduling;
								//f.departureDelayFromArrivalAirport = delayDelta; ??????
								int pushbackTime = f.departureTimeACES + delayDelta + f.gate_perturbation; //delayFromFirstScheduling or delay delta???
								int wheelsOffTime =  pushbackTime + f.taxi_unimpeded_time + f.taxiUncertainty;
								f.wheelsOffTime = wheelsOffTime;
								int lastOnTimeDeparturePoint = arrivalSlot - fastestDuration;
								//Gano delay accounting
								//f.departureDelayFromArrivalAirport = Math.max(2*delayFromFirstScheduling - Math.max(f.gate_perturbation, delayFromFirstScheduling), 0);

								if (wheelsOffTime > lastOnTimeDeparturePoint){
									//flight leaves too late.
									rescheduleQueue.add(new rescheduleEvent(lastOnTimeDeparturePoint, arrivalSlot, Action.remove, f));
									rescheduleQueue.add(new rescheduleEvent(wheelsOffTime, -4, Action.scheduleInTheAir, f));
								} else {
									f.arrivalTimeFinal = arrivalSlot;
								}

							}	
							break;

							// 3.	 SAME AS 1 EXCEPT FCFS -> PFCFS, ALLOW JIGGLING UP TO ARRIVAL.
							case ArrivalJiggle:
							{ //more flights have to be scheduled twice with ground since slot arrival time estimate is bad,
								//so more totalAirDelayl delay per pdt run.
								int proposedArrivalTime = f.arrivalTimeACES + f.gate_perturbation + f.taxi_unimpeded_time;
								//int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot
								airports.scheduleArrivalCompact(f, proposedArrivalTime, event.eventTime);
								//f.departureDelayFromArrivalAirport = delayDelta;
								int pushback = f.departureTimeACES + f.gate_perturbation + f.atcGroundDelay;
								int wheelsOffTime = pushback + f.taxi_unimpeded_time;// + f.taxi_perturbation; //add in taxi??
								f.wheelsOffTime = wheelsOffTime;
								int lastOnTimeDeparturePoint = f.arrivalFirstSlot - fastestDuration;
								//if(f.id == id)U.p("l "+ lastOnTimeDeparturePoint /1000 + " w " + wheelsOffTime/1000 + " d " + delayFromFirstScheduling);
								//f.departureDelayFromArrivalAirport = 
								//Math.max(2*delayFromFirstScheduling - Math.max(f.gate_perturbation, delayFromFirstScheduling), 0);

								if (wheelsOffTime > lastOnTimeDeparturePoint){
									//flight leaves too late.
									System.out.println(" I am here no taxi pert");
									rescheduleQueue.add(new rescheduleEvent(lastOnTimeDeparturePoint, -8, Action.removeByFlight, f));// -8 dummy value
									rescheduleQueue.add(new rescheduleEvent(wheelsOffTime, -4, Action.scheduleInTheAirByFlight, f));
								} else {


									//f.arrivalTimeFinal = arrivalSlot;
									//f.validate();
									//groundSlots++;
								}
							}	
							break;

							//4.  SAME AS 3 WITH UNCERTAINTY
							case ArrivalJiggleUncertainty:
							{ //more flights have to be scheduled twice with ground since slot arrival time estimate is bad,
								//so more totalAirDelayl delay per pdt run.
								int proposedArrivalTime = f.arrivalTimeACES + f.gate_perturbation + f.taxi_unimpeded_time;
								//int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot
								airports.scheduleArrivalCompact(f, proposedArrivalTime, event.eventTime);
								//f.departureDelayFromArrivalAirport = delayDelta;
								int uncertaintyMilliSec = (int)(Math.abs( ( random.nextGaussian()*(minsAhd*2/15) ) )*minToMillisec)*uncertaintyToggle;
								f.gateUncertainty = uncertaintyMilliSec;
								if(uncertaintyMilliSec<0){System.out.println("err uncertainty is '-' " + uncertaintyMilliSec);}

								//uncertaintyMilliSec = 2*minToMillisec;
								//if(minsAhd == 15){uncertaintyMilliSec = (int)(Math.abs( ( random.nextGaussian()*(18000*2/15) ) )*minToMillisec);}
								int delayDelta = Math.max(uncertaintyMilliSec,f.atcGroundDelay); // only works if this is the first time scheduled/ground-delay once only/contract enforcement
								//related to the way CFR is expected to work in reality (take max of the two (ATC vs Airline delay). Any further delay/rescheduling is taken in the air
								int pushback = f.departureTimeACES + f.gate_perturbation + delayDelta;
								int wheelsOffTime = pushback + f.taxi_unimpeded_time + f.taxiUncertainty; //add in taxi??
								f.wheelsOffTime = wheelsOffTime;
								int lastOnTimeDeparturePoint = f.arrivalFirstSlot - fastestDuration;
								//totalGroundDelay+=f.departureDelayFromArrivalAirport/60000.0;

								if (wheelsOffTime > lastOnTimeDeparturePoint){
									//flight leaves too late.
									rescheduleQueue.add(new rescheduleEvent(lastOnTimeDeparturePoint, -8, Action.removeByFlight, f));// -8 dummy value
									rescheduleQueue.add(new rescheduleEvent(wheelsOffTime, -4, Action.scheduleInTheAirByFlight, f));

								} else {
									//f.arrivalTimeFinal = arrivalSlot;
									//f.validate();
									//groundSlots++;
								}
							}	
							break;

							case scheduleByArrival:
							{
								int proposedArrivalTime = f.arrivalTimeACES + f.gate_perturbation + f.taxi_unimpeded_time;
								int arrivalSlot = airports.scheduleArrival(f, proposedArrivalTime, Integer.MAX_VALUE);//Int(f.arrivalAirport, proposedArrivalTime); //Make Slot
								f.arrivalFirstSlot = arrivalSlot;
								int delayFromFirstScheduling = arrivalSlot - proposedArrivalTime;
								//int delayDelta = Math.max(delayFromFirstScheduling-f.gate_perturbation,0);
								f.atcGroundDelay = delayFromFirstScheduling; 
								//f.departureDelayFromArrivalAirport = delayDelta;
								int addons = f.taxiUncertainty + f.gate_perturbation + f.taxi_unimpeded_time;
								int pushbackTime = f.departureTimeACES + /*delayDelta*/ delayFromFirstScheduling + f.gate_perturbation + f.taxi_unimpeded_time; //add in taxi??
								//int wheelsOffTime =  f.departureTimeProposed + f.departureDelayFromArrivalAirport + f.taxi_unimpeded_time; //add in taxi??
								int wheelsOffTime =  pushbackTime; //+ f.taxi_perturbation;
								f.wheelsOffTime = wheelsOffTime;
								int lastOnTimeDeparturePoint = arrivalSlot - fastestDuration;
								if (wheelsOffTime > lastOnTimeDeparturePoint){
									System.err.println("err in idealic scheduling");
									rescheduleQueue.add(new rescheduleEvent(lastOnTimeDeparturePoint, arrivalSlot, Action.remove, f));//???? DOES THIS MAKE SENSE???<<
									rescheduleQueue.add(new rescheduleEvent(event.eventTime + addons, -4, Action.scheduleInTheAir, f));//???? DOES THIS MAKE SENSE???<<
								} else {
									f.arrivalTimeFinal = arrivalSlot;
								}
							}	
							break;

							case FCFSArrWithNoGateUncertainty:
							{	
								int proposedArrivalTime = f.arrivalTimeACES + f.gate_perturbation + f.taxi_unimpeded_time;
								airports.scheduleArrivalCompact(f, proposedArrivalTime, event.eventTime);
								int pushback = f.departureTimeACES + f.gate_perturbation + f.atcGroundDelay;
								int wheelsOffTime = pushback + f.taxi_unimpeded_time;// + f.taxi_perturbation; //add in taxi??
								f.wheelsOffTime = wheelsOffTime;
								int lastOnTimeDeparturePoint = f.arrivalTimeFinal - fastestDuration;
								if (wheelsOffTime > lastOnTimeDeparturePoint){
									System.err.println("err in idealic scheduling");
								} else {
									//f.arrivalTimeFinal = arrivalSlot;
								}
							}	
							break;


							// following four cases are shared
							//schedule arrival slot at call for release
							case removeByFlight: // for case 3 and 4
							{
								airports.removeFlightFromArrivalQueue(f);
							}
							break;

							case scheduleInTheAirByFlight: // for case 3 and 4
							{	
								//by allowing wheels off time to be modified by other flights some, some flights wheels off time is greater than current time
								if(f.wheelsOffTime > event.eventTime){
									rescheduleQueue.add(new rescheduleEvent(f.wheelsOffTime, -4, Action.scheduleInTheAirByFlight, f));
								} else {								
									f.rescheduled = true;
									f.atcAirDelay = 0;
									f.numberOfJiggles = 0; f.originalJiggle = f.totalJiggleAmount;
									f.totalJiggleAmount = 0;
									int targetArrival = f.wheelsOffTime + nominalDuration; 						
									airports.scheduleArrivalCompact(f, targetArrival, event.eventTime); //f.wheelsOffTime? //event.eventTime << original
									//delay by airport
									//								original slot =   f.arrivalTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time +     groundDelay;                                         (ground)   == FIRST SLOT 
									//								wheelsOffTime = f.departureTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time +     f.taxi_perturbation + (uncertainty || groundDelay)  + jiggle;   == FINAL SLOT
								}

							}
							break;

							case remove: // for case 1 and 2
							{
								airports.removeFlightFromArrivalQueue(f);//(f.arrivalAirport, event.targetTime);

							}
							break;

							case scheduleInTheAir: // for case 1 and 2
							{
								f.rescheduled = true;
								int targetArrival = f.wheelsOffTime + nominalDuration;
								int finalArrivalSlot = airports.scheduleArrival(f, targetArrival, Integer.MAX_VALUE);//Int(f.arrivalAirport, targetArrival);// event.targetTime);
								int airDelay = finalArrivalSlot - targetArrival;
								f.atcAirDelay = airDelay;	
								f.arrivalTimeFinal = finalArrivalSlot;
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
							break;

							} //end switch statement


						} //END WHILE OF EVENTS

						//U.p(airports.airportList.size() + " airports.airportList.size()");
						//U.p(absorbedAirportDelayHrs.size() + " absorbedAirportDelayHrs.size()");
						//U.p(dispensedAirportDelayHrs.size();
						
						calculateDelays(flightList, airline, true, out, false, minsAhd);
						
						System.out.println("just for " + airline);
						calculateDelays(flightList, airline, false, out, false, minsAhd);
						
						

						
						out.close();

						//write out to airport
						//						if(counter == 1)writeToAirports(workingDirectory+ofolder,name,montecarlo,true); //workingDirectory+ofolder+
						//						else writeToAirports(workingDirectory+ofolder,name,montecarlo, false);
						//write out details of 1 run
						if(counter == 1)writeOut1Run(workingDirectory+ofolder,name, flightList);
						//*/

						//validate
						flights.validate();
						airports.validate();


					} catch (Exception e){
						System.err.println("Error: " + e.getMessage());
					}

				} //END Mins Ahead
			} // END by Mode

		} // END Monte carlo;
//		for(int s: delayedIntheAir.keySet()){
//			if(delayedIntheAir.get(s)!=2){
//				U.p(s+" "+delayedIntheAir.get(s));
//			}
//		}
		System.out.println("FIN! " + dateFormat.format(new Date()));

	} //END SCHEDULE()

	// /////////////////  //////////////// END OF FCFS
	
	
	public void calculateDelays(ArrayList<Flight> flightList, String airline, boolean calculateAll,
			BufferedWriter out, boolean printHeader, int minutesAhead){
		
		if(printHeader){
			try{
				
			
			out.write("# of flights that made their slots,");
			out.write("# of flights that missed their slots,");
			out.write("avg amount flights missed their slots by (min),");
			out.write("std amount flights missed their slots by (min),");


			out.write("# of flights  delayed on the ground,");
			out.write("max ground delay (min),");
			out.write("avg ground delay (min),");
			out.write("std ground delay (min),");
			out.write("total ground delay (hrs),");

			out.write("# of flights  delayed in the air,");
			out.write("max air delay (min),");
			out.write("avg air delay (min),");
			out.write("std air delay (min),");
			out.write("total air delay (hrs),");

			out.write("total delay (hrs),");
			out.write("total weighted delay (hrs),");
			out.write("avg jiggles per flight,"); //8 !!
			out.write("avg jiggle amount after last scheduling per flight (SECS),"); //9
			out.write("std jiggle amount after last scheduling per flight (SECS)\n");
			
			} catch (Exception e){
				System.out.println(e.getMessage());
			}
			
		}
		

		int delayedOnGround = 0, delayedInAir = 0, missedSlots = 0; 
		double totalAirDelay = 0, totalGroundDelay = 0, maxGroundDelay = 0, maxAirDelay = 0, totalMissedSlotMetric = 0, 
				meanJiggles = 0, totalJiggles =0;

		ArrayList<Double> groundDelay = new ArrayList<Double>();
		ArrayList<Double> airDelay = new ArrayList<Double>();
		ArrayList<Double> missedSlotMetric = new ArrayList<Double>();
		ArrayList<Double> totalJiggleAmount = new ArrayList<Double>();

		int totalSWAs = 0;

		for (Flight f: flightList){

			if(f.airline.equals(airline) || calculateAll || (airline.equals("CFR") && f.cfrEffected) ){
				
				//STD
				totalSWAs++;
				
				groundDelay.add(f.atcGroundDelay/toMinutes);
				airDelay.add(f.atcAirDelay/toMinutes);
				missedSlotMetric.add((f.arrivalTimeFinal - f.arrivalFirstSlot)/toMinutes);
				totalJiggleAmount.add(f.totalJiggleAmount/1000.0); // in secs


				//missed slots
				if(f.arrivalFirstSlot != f.arrivalTimeFinal){ 
					missedSlots++;
					totalMissedSlotMetric += f.arrivalTimeFinal - f.arrivalFirstSlot;
				}
				totalJiggles += f.totalJiggleAmount/1000.0; // in seconds
				meanJiggles += f.numberOfJiggles;

				//ground delays
				if(f.atcGroundDelay != 0) { 
					delayedOnGround++; 
					totalGroundDelay += f.atcGroundDelay;
					maxGroundDelay = Math.max(f.atcGroundDelay/toMinutes,maxGroundDelay);
					//puts delay in departure and arrival airports, must add entry if does not exist for each case
					if(dispensedAirportDelayHrs.get(f.arrivalAirport) != null){
						dispensedAirportDelayHrs.put(f.arrivalAirport, 
								dispensedAirportDelayHrs.get(f.arrivalAirport)+f.atcGroundDelay/toHours);
					} else {
						System.err.println(" why does airport not exist?");
						dispensedAirportDelayHrs.put(f.arrivalAirport, f.atcGroundDelay/toHours);
					}
					if(absorbedAirportDelayHrs.get(f.departureAirport) != null){
						absorbedAirportDelayHrs.put(f.departureAirport, 
								absorbedAirportDelayHrs.get(f.departureAirport)+f.atcGroundDelay/toHours);
					} else {
						System.err.println(" why does airport not exist?");
						absorbedAirportDelayHrs.put(f.departureAirport, f.atcGroundDelay/toHours);
					}			
				}

				// air delays
				if(f.atcAirDelay != 0){
					
					if(delayedIntheAir.get(f.id)!=null){
						delayedIntheAir.put(f.id, delayedIntheAir.get(f.id)+1);
					} else {
						delayedIntheAir.put(f.id, 1);
					}
					
					delayedInAir++; 
					totalAirDelay+=f.atcAirDelay ;
					maxAirDelay = Math.max(f.atcAirDelay/toMinutes,maxAirDelay);
					//puts delay in departure and arrival airports, must add entry if does not exist for each case
					if(dispensedAirportDelayHrs.get(f.arrivalAirport) != null){
						dispensedAirportDelayHrs.put(f.arrivalAirport, 
								dispensedAirportDelayHrs.get(f.arrivalAirport)+f.atcAirDelay/toHours);
					} else {
						System.err.println(" why does airport not exist?");
						dispensedAirportDelayHrs.put(f.arrivalAirport, f.atcAirDelay/toHours);
					}	
				}


			} // END airline if

		} // END FLights loop

		totalAirDelay /= toMinutes; totalGroundDelay /= toMinutes; totalMissedSlotMetric /= toMinutes;


		if(printHeader){
			System.out.println("minAhead,"); //9
			System.out.println("avg delay per flight,"); //9
			System.out.println("total weighted delay hrs"+","); //9
			System.out.println("totalSWAs-missedSlots"+","); //1 flights that made slots
			System.out.println("missedSlots"+","); //1 flights that made slots
			System.out.println("totalMissedSlotMetric/totalSWAs"+","); //2
			System.out.println("delayedOnGround"+","); //3
			System.out.println("delayedInAir"+","); //4 !!
			//System.out.println("maxGroundDelay"+","); //5
			//System.out.println("maxAirDelay"+","); //6 !!
			System.out.println("totalGroundDelay hrs"+","); //7
			System.out.println("totalAirDelay hrs"+","); //8 !!
			//System.out.println("totalGroundDelay+totalAirDelay) hrs"+","); //9
			//System.out.println("meanJiggles/(double)totalSWAs"+","); //8 !!
			System.out.println("(totalGroundDelay+totalAirDelay)/((double)totalSWAs)");
		} else {
			System.out.print(minutesAhead+",");
			System.out.printf("%5.2f,",(totalGroundDelay+totalAirDelay*2)/totalSWAs); //9
			System.out.printf("%7.1f,",(totalGroundDelay+totalAirDelay*2)/60); //9
			System.out.printf("%7d, ",totalSWAs-missedSlots); //1 flights that made slots
			System.out.printf("%5d,", missedSlots);
			System.out.printf("%5.1f, ",totalMissedSlotMetric/totalSWAs); //2
			System.out.printf("%5d, ",delayedOnGround); //3
			System.out.printf("%5d, ",delayedInAir); //4 !!
			//System.out.printf("%5.0f, ",maxGroundDelay); //5
			//System.out.printf("%5.0f, ",maxAirDelay); //6 !!
			System.out.printf("%6.1f, ",totalGroundDelay/60); //7
			System.out.printf("%6.1f, ",totalAirDelay/60); //8 !!
			//System.out.printf("%6.0f, ",(totalGroundDelay+totalAirDelay)/60); //9
			//System.out.printf("%5.1f,",meanJiggles/(double)totalSWAs); //8 !!
			System.out.printf("%5.1f",(totalGroundDelay+totalAirDelay)/((double)totalSWAs)*60); //9
			System.out.print("\n"); //9
		}
		
		//System.out.printf("%5.1f\n",totalJiggles/(double)totalSWAs); //9
		
		//WRITING OUT TO MC FILE
		try {
			out.write(flightList.size()-missedSlots+",");
			out.write(missedSlots+",");
			out.write(totalMissedSlotMetric/flightList.size() + ",");
			out.write(standardDeviation(missedSlotMetric.toArray(new Double[missedSlotMetric.size()])) +",");

			out.write(delayedOnGround + ",");
			out.write(maxGroundDelay + ",");
			out.write(totalGroundDelay/flightList.size() + ",");
			out.write(standardDeviation(groundDelay.toArray(new Double[groundDelay.size()])) +",");
			out.write(totalGroundDelay/60 + ",");

			out.write(delayedInAir + ",");
			out.write(maxAirDelay + ",");
			out.write(totalAirDelay/flightList.size() + ",");
			out.write(standardDeviation(airDelay.toArray(new Double[airDelay.size()])) +",");
			out.write(totalAirDelay/60 + ",");


			out.write(totalGroundDelay+totalAirDelay +",");
			out.write(((totalGroundDelay+totalAirDelay*2)/60) +",");

			out.write(meanJiggles/(double)flightList.size()+","); //8 !!
			out.write(totalJiggles/(double)flightList.size()+","); //9
			out.write(standardDeviation(totalJiggleAmount.toArray(new Double[totalJiggleAmount.size()])) +"\n");

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	} //END CALCULATE DELAYS()




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
			//				//U.p(a+" " + dispensedAirportDelayHrs.get(a)/montecarlo);
			//				out.write(a+"," + dispensedAirportDelayHrs.get(a)/montecarlo+"\n");
			//				totalAirport+=dispensedAirportDelayHrs.get(a)/montecarlo;
			//			}
			//			out.close();
			//U.p("total arrivalAirport dispensed" + totalAirport);

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
	
	public void testrt(){
		java.util.PriorityQueue<rescheduleEvent> sr = new java.util.PriorityQueue<rescheduleEvent>();
		//*
		sr.add(new rescheduleEvent(9, -34));
		sr.add(new rescheduleEvent(3, -64));
		sr.add(new rescheduleEvent(1, 34));
		sr.add(new rescheduleEvent(6, 5));
		sr.add(new rescheduleEvent(5, 2));
		sr.add(new rescheduleEvent(99, 1));
		sr.add(new rescheduleEvent(2, -3));
		//*/
		while(!sr.isEmpty()){
			sr.poll().print();
		}

	}
	

}

