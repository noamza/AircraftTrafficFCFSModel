package fcfs;

import java.util.*;
import java.io.*;
/*
enum Action {
	schedulingComplete,
	scheduleAtPDT,
	scheduleAtPushback,
	scheduleInTheAir,
	scheduleByArrival,
	remove,
	undef
}

class rescheduleEvent implements Comparable{
	int eventTime = 0;
	int targetTime = 0;
	Action mode = Action.undef; //1 = slot to remove, 2 = slot  to reschedule
	Flight flight;

	public rescheduleEvent(int et, int st, Action m, Flight f){ 
		eventTime= et; targetTime = st; mode = m; flight = f;
	}

	public rescheduleEvent(int et, int st){eventTime=et; targetTime=st;}

	void print(){ System.out.printf("r time: %d s time: %d\n", eventTime, targetTime);}

	public int compareTo(Object o) {
		//orders priorityqueue by least time
		return eventTime-((rescheduleEvent)o).eventTime;
		//order's priorityqueue by greatest first
		//return ((rt)o).rescheduleTime - rescheduleTime;
	}
}
//*/

/*
 *Flights scheduled when they are ready to go.
 *
 * Before CFR
 * at PDT schedule Slot including NT for NF. 
 * If delay is bigger than GP, add TP take delay in air if have to. 
 * Store delay taken on ground, and taken in air 
 * If GP is passed delay and flight can't make it by speeding up, 
 * Remove Slots 
 * Goto CFR
 * 
 * if delay and GP overlap, only count delay beyond gp??
 * 
 *
 * @CFR
 * at GP get Slot including NT for NF.
 * Take delay, add TP.
 * If can't make it, Remove slot, reschedule Slot,
 * Take delay in air
 * Store delay on ground and in the air.
 * 
 * 
 * 
 * 
 * 
 */	


/*
 * this seems to have been replaced by FCFSArrival.java
 * not necessary
 */

public class FCFSFlexibleSpeed {
	
	final static double toMinutes = 60000, toHours = 3600000;
	static double speedUp = 0.025; //0.025; 
	static double slowDown = 0.05;
	int rand = 0;
	Hashtable<String, Double> arrivalAirportDelayHrs;
	Hashtable<String, Double> departureAirportDelayHrs;

	public FCFSFlexibleSpeed(){
		rand = Math.abs(new java.util.Random().nextInt());
		
	}


	public void schedule(Action schedulingMode){
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm:ss:SSSS");
		arrivalAirportDelayHrs = new Hashtable<String, Double>();
		departureAirportDelayHrs = new Hashtable<String, Double>();
		//System.out.println("scheduleAtCallForRelease() " + dateFormat.format(date));
		Flights flights; Airports airports;
		flights = new Flights(); airports = new Airports();
		//String workingDirectory = "C:\\Users\\Noam Almog\\Desktop\\scheduler\\scheduler\\atl_data\\";
		String workingDirectory = "/Users/nalmog/Desktop/scheduler/atl_data/";
		flights.loadFlightsFromAces(workingDirectory+"clean_job.csv",false);
		airports.loadFromAces(workingDirectory+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
		airports.loadDelays(workingDirectory+"gate_delay.csv", "gate");	
		airports.loadDelays(workingDirectory+"taxi_delay.csv", "taxi");
		airports.loadDelays(workingDirectory+"taxi_u.csv", "taxi");
		flights.loadTaxiOffset(workingDirectory+"AirportTaxi.csv");
		//airports.printDelays();
		//airports.printAirports();
		System.out.println("		loaded " + dateFormat.format(new Date()));

		int montecarlo = 20, counter = 0;
		//double avgGround, avgAir;

		//mean median max #delayed #made slots X air, ground, Delay by airport.. 
		int groundSlots = 0, airSlots = 0, delayedOnGround = 0, delayedInAir = 0; 
		double totalAirDelay = 0, totalGroundDelay = 0, maxGroundDelay = 0, maxAirDelay = 0, totalMissedSlotMetric = 0;
		int avgInts[] = new int[4];
		double avgDoubles[] = new double [4];
		
		
		String mod = "";//"_no_perturbation";
		boolean pertrubGateAndTaxi = true;
		
		String name = "at_pushback_avg_delays_"+montecarlo+"_runs"+mod;
		if(schedulingMode == Action.scheduleAtPDT) name = "prior_pushback_avg_delays_" + montecarlo+"_runs"+mod;
		if(schedulingMode == Action.scheduleByArrival) name = "at_arrival_avg_delays_" + montecarlo+"_runs"+mod;
		
		U.p(schedulingMode+ " " + montecarlo);

		try{
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+name+".csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("flights that made their slots,");
			out.write("flights that missed their slots,");
			out.write("flights delayed on the ground,");
			out.write("flights delayed in the air,");
			out.write("max ground delay(min),");
			out.write("max air delay(min),");
			out.write("total ground delay(hr),");
			out.write("total air delay(hr),");
			out.write("avg ground delay(min),");
			out.write("avg air delay(min),");
			out.write("avg slot missed time(min)\n");

			while (counter < montecarlo) {
				counter++;
				groundSlots = 0; airSlots = 0; delayedOnGround = 0; delayedInAir = 0; 
				totalAirDelay = 0; totalGroundDelay = 0; maxGroundDelay = 0; maxAirDelay = 0; totalMissedSlotMetric = 0;

				java.util.Random random = new java.util.Random();//rand);//9 85);
				ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
				java.util.PriorityQueue<rescheduleEvent> rescheduleQueue = new java.util.PriorityQueue<rescheduleEvent>();
				
				
				//*			
				//GENERATE GATE TAXI PERTURBATIONS
				for (Flight f: flightList){
					//the more random a flight is, the less delay.
					//int rm = (int)(1000*60*60*350*random.nextDouble()); f.arrivalTimeProposed+=rm; f.departureTimeProposed+=rm;
					AirportTree da = airports.airportList.get(f.departureAirport);
					int gate_noise_seconds = 0, taxi_noise_seconds = 0;		
					//Get gate and taxi perturbations
					if(da!=null){
						double gateR = random.nextDouble(), taxiR = random.nextDouble();
						//U.p(gateR + " gate taxi " + taxiR + " " + da.taxiUnimpeded + " " + da.gateStd + " " + da.taxiMean);
						f.taxi_unimpeded_time = (int)(da.taxiUnimpeded)*60000;
						if(pertrubGateAndTaxi && da.gateZeroProbablity < gateR){
							double gate_noise_minutes = Math.exp(random.nextGaussian()*da.gateStd + da.gateMean);
							gate_noise_minutes = gate_noise_minutes < 120? gate_noise_minutes: 120;
							gate_noise_seconds = (int)(gate_noise_minutes*60000);
							f.gate_perturbation = gate_noise_seconds;
							//U.p("random");
						}
						if(pertrubGateAndTaxi && da.taxiZeroProbablity < taxiR){
							double taxi_noise_minutes = Math.exp(random.nextGaussian()*da.taxiStd + da.taxiMean);
							taxi_noise_minutes = taxi_noise_minutes < 45? taxi_noise_minutes: 45;
							taxi_noise_seconds = (int)(taxi_noise_minutes*60000);
							f.taxiUncertainty = taxi_noise_seconds;
						}
					}

					//schedules at PDT
					//if(priorToCFR)rescheduleQueue.add(new rescheduleEvent(f.departureTimeProposed, -3, Action.scheduleAtPDT, f));
					//schedules at CFR
					//else rescheduleQueue.add(new rescheduleEvent(f.departureTimeProposed + f.gate_perturbation, -2, Action.scheduleAtPushback, f));
					
					//schedules at PDT
					if(schedulingMode == Action.scheduleAtPDT)rescheduleQueue.add(new rescheduleEvent(f.departureTimeACES, -3, Action.scheduleAtPDT, f));
					//schedules at CFR
					if(schedulingMode == Action.scheduleAtPushback) rescheduleQueue.add(new rescheduleEvent(f.departureTimeACES + f.gate_perturbation, -2, Action.scheduleAtPushback, f));
					//schedule at ARRIVAL
					if(schedulingMode == Action.scheduleByArrival)rescheduleQueue.add(new rescheduleEvent(f.arrivalTimeACES+f.taxi_unimpeded_time, -4, Action.scheduleByArrival, f));
					
				}
				//int p = 0, c= 0, a= 0, id = 85; double totalAirDelay = 0, totalGroundDelay = 0; double maxGroundDelay = 0, maxAirDelay = 0 ;
				//EXECUTE SCHEDULING
				while(!rescheduleQueue.isEmpty()){
					//execute earliest event;
					rescheduleEvent event = rescheduleQueue.remove();
					Flight f = event.flight;
					int nominalDuration = f.arrivalTimeACES-f.departureTimeACES;
					int fastestDuration =  (int)(nominalDuration/(1+speedUp));
					//if(f.id == 15073)U.p("n "+ nominalDuration /1000 + " f " + fastestDuration/1000);

					switch (event.mode) {
					//schedulue by arrival
					case scheduleByArrival:
					{ //more flights have to be scheduled twice with ground since slot arrival time estimate is bad,
						//so more totalAirDelayl delay per pdt run.
						int proposedArrivalTime = f.arrivalTimeACES + f.taxi_unimpeded_time;
						int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot
						f.arrivalFirstSlot = arrivalSlot;
						int delayFromFirstScheduling = arrivalSlot - proposedArrivalTime; 
						if(delayFromFirstScheduling != 0){ delayedOnGround++; }
						int delayDelta = Math.max(delayFromFirstScheduling-f.gate_perturbation,0);
						f.atcGroundDelay = delayFromFirstScheduling; 
						//f.departureDelayFromArrivalAirport = delayDelta;
						int addons = f.taxiUncertainty + f.gate_perturbation + f.taxi_unimpeded_time;
						int pushbackTime = f.departureTimeACES + delayDelta + f.gate_perturbation + f.taxi_unimpeded_time; //add in taxi??
						//int wheelsOffTime =  f.departureTimeProposed + f.departureDelayFromArrivalAirport + f.taxi_unimpeded_time; //add in taxi??
						int wheelsOffTime =  pushbackTime + f.taxiUncertainty;
						f.wheelsOffTime = wheelsOffTime;
						int lastOnTimeDeparturePoint = arrivalSlot - fastestDuration;
						//if(f.id == id)U.p("l "+ lastOnTimeDeparturePoint /1000 + " w " + wheelsOffTime/1000 + " d " + delayFromFirstScheduling);
						//f.departureDelayFromArrivalAirport = 
								Math.max(2*delayFromFirstScheduling - Math.max(f.gate_perturbation, delayFromFirstScheduling), 0);	
						
						totalGroundDelay+=f.atcGroundDelay/60000.0;
						
						if (wheelsOffTime > lastOnTimeDeparturePoint){
							//flight leaves too late.
							rescheduleQueue.add(new rescheduleEvent(event.eventTime, arrivalSlot, Action.remove, f));//???? DOES THIS MAKE SENSE???<<
							rescheduleQueue.add(new rescheduleEvent(event.eventTime + addons, -4, Action.scheduleInTheAir, f));//???? DOES THIS MAKE SENSE???<<
						} else {
							f.arrivalTimeFinal = arrivalSlot;
							f.validate();
							groundSlots++;
						}
						//add airport delay
						if(f.atcGroundDelay!=0){
							if(arrivalAirportDelayHrs.get(f.arrivalAirport) != null){
								arrivalAirportDelayHrs.put(f.arrivalAirport, 
										arrivalAirportDelayHrs.get(f.arrivalAirport)+f.atcGroundDelay/(60*60*1000.0));
							} else {
								arrivalAirportDelayHrs.put(f.arrivalAirport, f.atcGroundDelay/(60*60*1000.0));
							}
							if(departureAirportDelayHrs.get(f.departureAirport) != null){
								departureAirportDelayHrs.put(f.departureAirport, 
										departureAirportDelayHrs.get(f.departureAirport)+f.atcGroundDelay/(60*60*1000.0));
							} else {
								departureAirportDelayHrs.put(f.departureAirport, f.atcGroundDelay/(60*60*1000.0));
							}	
						}
						
					}	
					break;
					
					
					
					//schedule arrival slot at call for release
					case scheduleAtPDT:
					{ //more flights have to be scheduled twice with ground since slot arrival time estimate is bad,
						//so more totalAirDelayl delay per pdt run.
						int proposedArrivalTime = f.arrivalTimeACES + f.taxi_unimpeded_time;
						int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot
						f.arrivalFirstSlot = arrivalSlot;
						int delayFromFirstScheduling = arrivalSlot - proposedArrivalTime; 
						if(delayFromFirstScheduling != 0){ delayedOnGround++; }
						int delayDelta = Math.max(delayFromFirstScheduling-f.gate_perturbation,0);
						f.atcGroundDelay = delayFromFirstScheduling; 
						//f.departureDelayFromArrivalAirport = delayDelta; 
						int pushbackTime = f.departureTimeACES+ delayDelta + f.gate_perturbation + f.taxi_unimpeded_time; //add in taxi??
						int wheelsOffTime =  pushbackTime + f.taxiUncertainty;
						f.wheelsOffTime = wheelsOffTime;
						int lastOnTimeDeparturePoint = arrivalSlot - fastestDuration;
						//if(f.id == id)U.p("l "+ lastOnTimeDeparturePoint /1000 + " w " + wheelsOffTime/1000 + " d " + delayFromFirstScheduling);
						f.atcGroundDelay = 
								Math.max(2*delayFromFirstScheduling - Math.max(f.gate_perturbation, delayFromFirstScheduling), 0);	
						totalGroundDelay+=f.atcGroundDelay/60000.0;
						
						if (wheelsOffTime > lastOnTimeDeparturePoint){
							//flight leaves too late.
							rescheduleQueue.add(new rescheduleEvent(lastOnTimeDeparturePoint, arrivalSlot, Action.remove, f));
							rescheduleQueue.add(new rescheduleEvent(wheelsOffTime, -4, Action.scheduleInTheAir, f));
						} else {
							f.arrivalTimeFinal = arrivalSlot;
							f.validate();
							groundSlots++;
						}
						//add airport delay
						if(f.atcGroundDelay!=0){
							if(arrivalAirportDelayHrs.get(f.arrivalAirport) != null){
								arrivalAirportDelayHrs.put(f.arrivalAirport, 
										arrivalAirportDelayHrs.get(f.arrivalAirport)+f.atcGroundDelay/(60*60*1000.0));
							} else {
								arrivalAirportDelayHrs.put(f.arrivalAirport, f.atcGroundDelay/(60*60*1000.0));
							}
							if(departureAirportDelayHrs.get(f.departureAirport) != null){
								departureAirportDelayHrs.put(f.departureAirport, 
										departureAirportDelayHrs.get(f.departureAirport)+f.atcGroundDelay/(60*60*1000.0));
							} else {
								departureAirportDelayHrs.put(f.departureAirport, f.atcGroundDelay/(60*60*1000.0));
							}	
						}
						
					}	
					break;

					case scheduleAtPushback:
					{
						//calling at CFR
						int cfrProposedArrivalTime = f.arrivalTimeACES + f.taxi_unimpeded_time + f.gate_perturbation; 
						int cfrArrivalSlot = airports.scheduleArrival(f.arrivalAirport, cfrProposedArrivalTime); //Make Slot including perturbation
						f.arrivalFirstSlot = cfrArrivalSlot;
						int delayFromCfrScheduling = cfrArrivalSlot - cfrProposedArrivalTime; 
						if(delayFromCfrScheduling!= 0){ delayedOnGround++; }
						f.atcGroundDelay = delayFromCfrScheduling; 
						totalGroundDelay+=delayFromCfrScheduling/60000.0;
						int wheelsOffTime = f.departureTimeACES+f.gate_perturbation + delayFromCfrScheduling + f.taxiUncertainty + f.taxi_unimpeded_time; 
						f.wheelsOffTime = wheelsOffTime;
						int lastOnTimeDeparturePoint = cfrArrivalSlot - fastestDuration;
						//slot should be more reachable since slot is further out.
						//therefore more flights should make their slots
						//therefore LESS airborne cases
						//if(f.id == id)U.p("l "+ lastOnTimeDeparturePoint /1000 + " w " + wheelsOffTime/1000 + " d " + delayFromCfrScheduling);

						if(wheelsOffTime > lastOnTimeDeparturePoint){
							rescheduleQueue.add(new rescheduleEvent(lastOnTimeDeparturePoint, cfrArrivalSlot, Action.remove, f));
							rescheduleQueue.add(new rescheduleEvent(wheelsOffTime,-6, Action.scheduleInTheAir, f));// wheelsOffTime+nominalDuration
							
							if(wheelsOffTime < f.departureTimeACES + f.gate_perturbation){
								U.p("error scheduling in past tense");
							}

						} else {
							f.arrivalTimeFinal = cfrArrivalSlot;
							f.validate();
							groundSlots++;
						}					
						//delay by airport
						if(delayFromCfrScheduling!=0){
							if(arrivalAirportDelayHrs.get(f.arrivalAirport) != null){
								arrivalAirportDelayHrs.put(f.arrivalAirport, 
										arrivalAirportDelayHrs.get(f.arrivalAirport)+delayFromCfrScheduling/(60*60*1000.0));
							} else {
								arrivalAirportDelayHrs.put(f.arrivalAirport, delayFromCfrScheduling/(60*60*1000.0));
							}
							if(departureAirportDelayHrs.get(f.departureAirport) != null){
								departureAirportDelayHrs.put(f.departureAirport, 
										departureAirportDelayHrs.get(f.departureAirport)+delayFromCfrScheduling/(60*60*1000.0));
							} else {
								departureAirportDelayHrs.put(f.departureAirport, delayFromCfrScheduling/(60*60*1000.0));
							}			
						}
						
					}
					break;

					case remove:
					{
						airports.removeFlightFromArrivalQueue(f.arrivalAirport, event.targetTime);
						break;
					}
					case scheduleInTheAir:
					{
						airSlots++;
						int targetArrival = f.wheelsOffTime + nominalDuration;
						int finalArrivalSlot = airports.scheduleArrival(f.arrivalAirport, targetArrival);// event.targetTime);
						int airDelay = finalArrivalSlot - targetArrival;
						f.atcAirDelay = airDelay;				
						if(airDelay != 0){ delayedInAir++; }
						totalAirDelay += airDelay/60000.0;
						f.arrivalTimeFinal = finalArrivalSlot;
						totalMissedSlotMetric += (finalArrivalSlot- f.arrivalFirstSlot)/toMinutes;
						//tp += (finalArrivalSlot - f.arrivalTimeACES)/toMinutes;
						
						//delay by airport
						if(airDelay!=0){
							if(arrivalAirportDelayHrs.get(f.arrivalAirport) != null){
								arrivalAirportDelayHrs.put(f.arrivalAirport, 
										arrivalAirportDelayHrs.get(f.arrivalAirport)+airDelay/(60*60*1000.0));
							} else {
								arrivalAirportDelayHrs.put(f.arrivalAirport, airDelay/(60*60*1000.0));
							}
						}
						
						f.validate();
						break;
					}
					case undef:
					{
						U.p("should not be here");
						System.err.println("EVENT ERROR SHOULD NOT BE HERE");
						break;
					}
					default:
					{
						U.p("should not be here");
						System.err.println("EVENT ERROR SHOULD NOT BE HERE");
						break;
					}
					} //end switch statement

					//if(f.departureDelayFromArrivalAirport/60000.0>210) U.p(f.id + " delay more than 200 " + f.departureDelayFromArrivalAirport/60000.0);
					maxGroundDelay = Math.max(f.atcGroundDelay/60000.0,maxGroundDelay);
					maxAirDelay = Math.max(f.atcAirDelay/60000.0,maxAirDelay);

				} //END WHILE OF EVENTS
				
				airports.validate();
				//U.p("mg " + maxGroundDelay); U.p("ma " + maxAirDelay);
				flights.validate();
				//flights.getFlightByID(18).printVariables();
				flights.resetPerturbationAndSchedulingDependentVariables();
				airports.resetToStart();
				//flights.getFlightByID(18).printVariables();
				
				
				/*
				System.out.printf("%d,",groundSlots);
				System.out.printf("%d,",airSlots);
				System.out.printf("%d,",delayedOnGround);
				System.out.printf("%d,",delayedInAir);
				System.out.printf("%.1f,",maxGroundDelay);
				System.out.printf("%.1f,",maxAirDelay);
				System.out.printf("%.1f,",totalGroundDelay/60);
				System.out.printf("%.1f,",totalAirDelay/60);
				System.out.printf("%d,",groundSlots+airSlots);
				System.out.printf("%.1f\n",(totalGroundDelay+totalAirDelay)/60);
				*/
				//U.p(totalMissedSlotMetric/flightList.size() + " slot \n");
				//U.p(tp/flightList.size() + " tp \n"); tp = 0;
				
				out.write(groundSlots + ",");
				out.write(airSlots + ",");
				out.write(delayedOnGround + ",");
				out.write(delayedInAir + ",");
				out.write(maxGroundDelay + ",");
				out.write(maxAirDelay + ",");
				out.write(totalGroundDelay/60 + ",");
				out.write(totalAirDelay/60 + ",");
				out.write(totalGroundDelay/flightList.size() + ",");
				out.write(totalAirDelay/flightList.size() + ",");
				out.write(totalMissedSlotMetric/flightList.size() + "\n");
				
				avgInts[0] +=groundSlots; avgInts[1] += airSlots ; avgInts[2] += delayedOnGround; avgInts[3] += delayedInAir;
				avgDoubles[0] += maxGroundDelay; avgDoubles[1] += maxAirDelay; avgDoubles[2] += totalGroundDelay/60; avgDoubles[3] += totalAirDelay/60;

			} // END MONTE CARLO

			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		avgInts[0] /= montecarlo; avgInts[1] /= montecarlo; avgInts[2] /= montecarlo; avgInts[3] /= montecarlo;
		avgDoubles[0] /= montecarlo; avgDoubles[1] /= montecarlo; avgDoubles[2] /= montecarlo; avgDoubles[3] /= montecarlo;
		
		//*
		int n = avgInts[0]+avgInts[1];
		System.out.printf("ground slots %7d %7.0f hrs ground delay %7.1f min per flight\n",avgInts[0],(double)Math.round(avgDoubles[2]),avgDoubles[2]*60/avgInts[0]);
		System.out.printf("air    slots %7d %7.0f hrs air    delay %7.1f min per flight \n",avgInts[1],(double)Math.round(avgDoubles[3]),avgDoubles[3]*60/avgInts[1]);
		System.out.printf("total delay %.0f hrs \n", (double)Math.round(avgDoubles[2]+avgDoubles[3])); 
		 //*/
		double totalAirport = 0;
		
		try{
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+name+"_delay_by_arrival_airport"+".csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("name,delay(hrs)\n");

			for (Enumeration<String> e = arrivalAirportDelayHrs.keys(); e.hasMoreElements();){
				String a = e.nextElement();
				//U.p(a+" " + ArrivalAirportDelayHrs.get(a)/montecarlo);
				out.write(a+"," + arrivalAirportDelayHrs.get(a)/montecarlo+"\n");
				totalAirport+=arrivalAirportDelayHrs.get(a)/montecarlo;
			}
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		
		//U.p(totalAirport + " " + (avgDoubles[2]+avgDoubles[3]));
		totalAirport = 0;
				
		try{
			// Create file 
			FileWriter fstream = new FileWriter(workingDirectory+name+"_delay_by_departure_airport"+".csv");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("name,delay(hrs)\n");

			for (Enumeration<String> e = departureAirportDelayHrs.keys(); e.hasMoreElements();){
				String a = e.nextElement();
				//U.p(a+" " + ArrivalAirportDelayHrs.get(a)/montecarlo);
				out.write(a+"," + departureAirportDelayHrs.get(a)/montecarlo+"\n");
				totalAirport+=departureAirportDelayHrs.get(a)/montecarlo;
			}
			out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		//U.p(totalAirport + " " + avgDoubles[2]);
		
		airports.printMinSpacing();
		//airports.printAirports();
		System.out.println("		FIN! " + dateFormat.format(new Date()));
		
		
		//writeOut(flightList, workingDirectory, priorToCFR);
	}
	
	public void test(){
		
	}
	
	/*
	 * 
mg 224.3738
ma 239.71873333333335
PDT slots   19182    1941 hrs ground delay     6.1 min per flight
air slots   18164     828 hrs air    delay     2.7 min per flight 
totalAirDelayL 2769 hrs 
mg 232.025
ma 238.96985
CFR slots   27212    1993 hrs ground delay     4.4 min per flight
air slots   10134     652 hrs air    delay     3.9 min per flight 
totalAirDelayL 2646 hrs 
	 * 
	 * 
14978 delay more than 200 206.11785
12465 delay more than 200 222.65951666666666
12549 delay more than 200 214.3238
12575 delay more than 200 213.21665
12335 delay more than 200 217.17973333333333
15077 delay more than 200 214.32165
14493 delay more than 200 220.1576
15073 delay more than 200 220.3795
14602 delay more than 200 217.48473333333334
16315 delay more than 200 200.32088333333334
17777 delay more than 200 213.3469
17535 delay more than 200 203.49903333333333
18096 delay more than 200 200.18333333333334
	 * 
PDT slots   14339    1934 hrs ground delay     8.1 min per flight
air slots   23006     916 hrs air    delay     2.4 min per flight 
totalAirDelayL 2850 hrs 
CFR slots   26144    2001 hrs ground delay     4.6 min per flight
air slots   11201     711 hrs air    delay     3.8 min per flight 
totalAirDelayL 2711 hrs 

loaded 10:43:11:0503 100
PDT slots   14344    1936 hrs ground delay     8.1 min per flight
air slots   23001     909 hrs air    delay     2.4 min per flight 
totalAirDelayL 2845 hrs 
CFR slots   26162    1998 hrs ground delay     4.6 min per flight
air slots   11183     705 hrs air    delay     3.8 min per flight 
totalAirDelayL 2703 hrs 

loaded 10:47:30:0366 1000
PDT slots   14302    1935 hrs ground delay     8.1 min per flight
air slots   23043     911 hrs air    delay     2.4 min per flight 
totalAirDelayL 2846 hrs
CFR slots   26148    1999 hrs ground delay     4.6 min per flight
air slots   11197     710 hrs air    delay     3.8 min per flight 
totalAirDelayL 2709 hrs 
FIN! 10:49:45:0902

loaded 17:04:10:0983 10000
PDT slots   14294    1937 hrs ground delay     8.1 min per flight
air slots   23051     912 hrs air    delay     2.4 min per flight 
totaL 2849 hrs 
loaded 17:26:24:0307
CFR slots   26146    1997 hrs ground delay     4.6 min per flight
air slots   11199     709 hrs air    delay     3.8 min per flight 
totaL 2706 hrs 
	 */

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
		//U.p(a + " a b " + b); 		" totalAirDelayl flightList: " + flightList.size() +
		//U.p(name +  "\ntotalAirDelayL: " + Math.round((totalAirDelaylAirDelay+totalGroundDelay))+" hrs\nground delay: " + Math.round(totalGroundDelay)
		//		+ " hrs \nairborne delay: " + Math.round(totalAirDelaylAirDelay) + " hrs");

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