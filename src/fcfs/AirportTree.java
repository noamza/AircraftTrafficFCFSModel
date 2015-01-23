package fcfs;

import java.io.BufferedWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
	
/*
 * This inner class is used to store airport departure and arrival capacity per hour (ADR, AAR) by time of day.
 * Capacities are based on ASPM input data and are for up to 15min increments.
 * Values are stored in milliseconds.
 */
class CapacityByTime implements Comparable<CapacityByTime>{
	//adr actual departure rate 
	//aar actual arrival rate
	int time = -1; //time of day, milliseconds
	int adr = -1; //number of aircraft allowed to depart per hour
	int aar = -1; //number of aircraft allowed to arrive per hour
	CapacityByTime(int t){ time = t;}
	CapacityByTime(int t, int d, int a){
		time = t; 
		adr = d; 
		aar = a;
	}
	
	public int compareTo(CapacityByTime o ){ 
		return (time-o.time); 
	} //!= 0? time-o.time:-1;
	
	void print(){ 
		System.out.printf("	%s: adr %d aar %d\n", U.timeToDateAdjusted(time), adr, aar);
	}
}


//compare the first value of the TreeSet
class arrivalTrafficCMPcomparator implements Comparator<TreeSet<Integer>> {
	public int compare(TreeSet<Integer> ts1, TreeSet<Integer> ts2) {
		return (ts1.first() - ts2.first());
	}
}

/*
 * This class represents the airport as an ordered queue of departure/arrival slots while maintaining capacity constraints by separating slots by time.
 * The bulk of departure/arrival scheduling logic is held in this class.
 * Essentially, flights are stored in departure/arrival queues (TreeSet's) ordered by time.
 * A flight requests a slot at a given time. The flight is then inserted in the queue at the soonest possible slot that maintains the capacity constraints of the airport at that time. Any additional delay required is recorded.
 * Capacity constraints are stored as departures/arrivals per hour (ADR, AAR). 
 * To ensure capacity constraints, slots are spaced such that there is a 1hr/ADR (or AAR for arrivals) amount of time between each slot. For example, an airport with an ADR of 30, will separate each flight by 2 minutes (1hr/30 departures_per_hour) such that departure capacity is never exceeded.
 * 
 *  NOTE: CFR stands for 'call for release'. This value was used in prioritizing scheduling for CFR fligHts over non-CFR flights in some of the scenarios of the Delay Sensitivity study. 
 * 
 */
public class AirportTree {
	final static int DEFAULT_ARR_DEP_RATE = 30;
	final static int DEFAULT_ARR_RATE = 30; //arrival capacity per hour
	final static int DEFAULT_DEP_RATE = 30; //departure capacity per hour
	
	final static int DEFAULT_UNIMPEDED_TAXI_TIME = 5;//(minutes) value from Gano
	String airportName;
	PrintStream io = System.out;
	
	//Earlier implementation storing arrivals/departures as ints representing time.
	TreeSet<Integer> airportArrivalTraffic = new TreeSet<Integer>();
	TreeSet<Integer> airportDepartureTraffic = new TreeSet<Integer>();
	
	//Huu's implementation
	TreeSet<Integer> scheduledAirportArrivalTraffic = new TreeSet<Integer>(); 
	TreeSet<Integer> scheduledAirportDepartureTraffic = new TreeSet<Integer>();
	
	//latest implementation, store flight objects which include delay data
	TreeSet<Flight> arrivalTrafficByFlight = new TreeSet<Flight>(new flightFinalArrTimeComparator());
	TreeSet<Flight> departureTrafficByFlight = new TreeSet<Flight>(new flightFinalDepTimeComparator());

	private boolean departureContract = true; //this boolean is for determining whether a flight has a fixed departure time, or a mutable departure time that can be changed to the modify the schedule as it is being generated to be more efficient. (used in 'jiggeling')
	
	private int CFRstart = 0; //time at which a CFR program is in effect, not used
	private int CFRend   = 0; //time at which a CFR program is in effect, not used
	
	Airports airports;
	
	TreeSet<CapacityByTime> airportCapacities = new TreeSet<CapacityByTime>(); //ASPM capacity data
	
	//these values are derived from work done by Gano, and are used generate stochastic gate and taxi time delays used in monte carlo simulation. See Delay Sensitivity TM by Gano Chatterji, Kee Palopo and Noam Almog.
	double gateMean = 0, gateZeroProbablity = 1, gateStd = 0, taxiMean = 0, 
		   taxiZeroProbablity = 1, taxiStd = 0; 
		   
	double taxiUnimpeded = DEFAULT_UNIMPEDED_TAXI_TIME;//10 from aces 8.0;	
	
	public void setCFR(int s, int e){
		CFRstart = s; CFRend = e;
	}
	
	public void setDepartureContract(boolean b){
		departureContract = false;
	}
	
	//offsets capacities by set amount. Used to reconcile time discrpencies between ASPM and ACES simulation time.
	public void offsetCapacities(int offset){
		if(airportCapacities!=null){
			for(CapacityByTime c: airportCapacities){
				c.time += offset;
			}
		}
	}
	
	public AirportTree(String name, Airports as){
		airportName = name;
		airports = as;
	}
	
	public void resetToStart(){
		airportArrivalTraffic = new TreeSet<Integer>();	
		airportDepartureTraffic = new TreeSet<Integer>();
		arrivalTrafficByFlight = new TreeSet<Flight>(new flightFinalArrTimeComparator());
		departureTrafficByFlight = new TreeSet<Flight>(new flightFinalDepTimeComparator()); 
		departureContract = true;
		
		//Might be necessary
		//CFRstart = Double.NEGATIVE_INFINITY;
		//CFRend   = Double.NEGATIVE_INFINITY;
	}
	
	public boolean effectedByCFR(int time){
		return CFRstart <= time && time < CFRend; 
	}
	
	//Throws exception if this Flight was not in list
	public boolean freeDepartureSlot(Flight f){
		if(departureTrafficByFlight.remove(f)){
			return true;
		} else {
			throw new java.lang.Error("FAILED: flight not removed, not found in arrival queue");
			
		}
		
	}
	
	//returning spacing based on capacity constraints at a given time
	int getSpacing(int time, boolean departure){
		return departure? getDepartureSpacing(time) : getArrivalSpacing(time);
	}
	
	public int scheduleDeparture(Flight f, int proposedDepartureTime, int currentTime){
		if(f.cfrEffected) return insertPriorityDeparture(f, proposedDepartureTime, currentTime);
		else			  return insertNonPriorityDeparture(f, proposedDepartureTime, currentTime);
	}
	public int scheduleArrival(Flight f, int proposedDepartureTime, int currentTime){
		if(f.cfrEffected) return insertNonPriorityArrival(f, proposedDepartureTime, currentTime);
		else			  return insertNonPriorityArrival(f, proposedDepartureTime, currentTime);
	}
	
	//returns the soonest available slot on a First-Come-First-Served principle
	public int getSoonestNonPrioritySlot(Flight f, int time, boolean departure){
		int candidateSlotTime = time;
		Flight previousFlight, nextFlight; 
		int previousSlot, //previous slot time in queue 
			previousSpace, //capacity time spacing for previous slot time
			currentSpace, //same as previous
			nextSlot; //same as previous
		TreeSet<Flight> queue = departure? departureTrafficByFlight : arrivalTrafficByFlight;
		int flightsPassed = -1; 
		//this loop finds the nearest scheduled slots before and after a given candidate slot time, and checks if for spacing capacity violations at that time. If the constraints aren't met, a new slot is attempted to be found either after the previous flight's, or the next flight's spacing constraints. 
		while(true){ flightsPassed++;
			if(departure){
				previousFlight = queue.floor(Flight.dummyDeparture(candidateSlotTime)); 
				nextFlight =     queue.higher(Flight.dummyDeparture(candidateSlotTime)); //or not ceiling so doesn't pick same flight for both
				previousSlot =   previousFlight != null? previousFlight.departureTimeFinal : Integer.MIN_VALUE;
				nextSlot =       nextFlight!=null? nextFlight.departureTimeFinal : Integer.MAX_VALUE;
			} else {
				previousFlight = queue.floor(Flight.dummyArrival(candidateSlotTime));
				nextFlight =     queue.higher(Flight.dummyArrival(candidateSlotTime));
				previousSlot =   previousFlight != null? previousFlight.arrivalTimeFinal : Integer.MIN_VALUE;
				nextSlot =       nextFlight!=null? nextFlight.arrivalTimeFinal : Integer.MAX_VALUE;
			}
			//spacing based on adr rates. i.e. 30 dep per hour means 2min space between flights.
			currentSpace = getSpacing(candidateSlotTime, departure);
			previousSpace = getSpacing(previousSlot, departure);
			//ensures slot time will be spaced out between any two flights.
			if( previousSlot + previousSpace <= candidateSlotTime && candidateSlotTime + currentSpace <= nextSlot){
				//int diff = 0; diff = candidateSlotTime - time; 
				return candidateSlotTime;
				//done
			}
			//first tries to depart at the space after last flight.
			else if(previousSlot + previousSpace > candidateSlotTime){
				candidateSlotTime = previousSlot + previousSpace;
				//otherwise at space after next flight
			} else {
				candidateSlotTime = nextSlot + getSpacing(nextSlot, departure);
			}
		}
	}
	
	//This method uses getSoonestNonPrioritySlot, to find the next available arrival slot, and inserts a flight to the arrival queue at that time. 
	//This also keeps track of the types of delay needed to absorb the delay generated by arrival scheduling.
	public int insertNonPriorityArrival(Flight f, int proposedArrivalTime, int currentTime){
		//no flights
		if(arrivalTrafficByFlight.isEmpty()){
			f.arrivalTimeFinal = proposedArrivalTime;
			arrivalTrafficByFlight.add(f);			
			return 0;
		//there are flights already in the arrival queue
		} else {
			int candidateArrivalSlot = getSoonestNonPrioritySlot(f, proposedArrivalTime, false);
			f.arrivalTimeFinal = candidateArrivalSlot;
			arrivalTrafficByFlight.add(f);
			//calculate delay
			//This code also checks for departure constraints to make sure delay can be taken on the ground
			int delayNeeded = f.arrivalTimeFinal - proposedArrivalTime;
			if(currentTime < f.departureTimeFinal){ //should there be different cases for < and =
				U.Assert(f.firstTimeBeingArrivalScheduled);
				//code to make sure delay can be taken on the ground..When there are departure constraints!
				int delayThatCanBeAbsorbedOnTheGround = timeUntilNextSlot(f, f.departureTimeFinal, true);
				U.Assert(delayThatCanBeAbsorbedOnTheGround >= 0, 
						delayThatCanBeAbsorbedOnTheGround+ " delayThatCanBeAbsorbedOnTheGround >= 0");
				int groundDelayPortion = Math.min(delayThatCanBeAbsorbedOnTheGround, delayNeeded);
				int airDelayPortion = delayNeeded - groundDelayPortion;
				
				U.Assert(airDelayPortion >= 0 && groundDelayPortion >= 0, 
						airDelayPortion + " airDelayPortion > 0 && groundDelayPortion > 0," + groundDelayPortion);
				//When there are departure constraints!
				//U.Assert(airDelayPortion == 0, "airDelayPortion == 0"); //remove if departure constraints are used.
				
				f.atcGroundDelay += groundDelayPortion;
				f.atcAirDelay += airDelayPortion;
				f.departureTimeFinal = f.departureTimeFinal + groundDelayPortion; //flight departs later.. //TODO
				
			} else {
				//has to do with CFR ground delay scenario, see Delay Sensitivity TM.
				if(f.cfrEffected)U.Assert(!f.firstTimeBeingArrivalScheduled, ""+ f.id);
				
				//for speeding flights, delay in the air only beyond nominal flight time?
				if(proposedArrivalTime < f.departureTimeFinal + (f.arrivalTimeACES - f.departureTimeACES)){
				
					f.atcAirDelay += delayNeeded;	
				} else {
					f.atcAirDelay += delayNeeded;
				}
			}
			
			if(delayNeeded > 0 && f.cfrEffected){ 
				//print("should not be delay here delay needed here??"); //relevant??
			}
			
			f.arrivalAirportDelay += delayNeeded;
			return delayNeeded;
		}
	}
	
	//returns the time until the next valid slot.
	public int timeUntilNextSlot(Flight f, int time, boolean departure){
		//TODO
		Flight nextFlight; 
		int currentSpace, nextSlot;
		AirportTree a = departure? airports.getAirport(f.departureAirport) : airports.getAirport(f.arrivalAirport);
		TreeSet<Flight> queue = departure? a.departureTrafficByFlight : a.arrivalTrafficByFlight;
		if(departure){
			nextFlight =     queue.higher(Flight.dummyDeparture(time)); 
			nextSlot =       nextFlight!=null? nextFlight.departureTimeFinal : Integer.MAX_VALUE;	
		} else {
			//arrival
			nextFlight =     queue.higher(Flight.dummyArrival(time));
			nextSlot =       nextFlight!=null? nextFlight.arrivalTimeFinal : Integer.MAX_VALUE;
		}
		currentSpace = a.getSpacing(nextSlot-1, departure);
		//this could be invalid on edge cases;
		while(nextSlot-currentSpace + a.getSpacing(nextSlot-currentSpace, departure) > nextSlot){
			currentSpace += a.getSpacing(nextSlot-currentSpace,departure);
		}
		
		int timeUntilNextSlot = nextSlot - currentSpace - time;
		timeUntilNextSlot = timeUntilNextSlot < 0? 0: timeUntilNextSlot;
		U.Assert(a.getSpacing(nextSlot-currentSpace, departure) <= nextSlot, "getSpacing(nextSlot-currentSpace, departure) <= nextSlot");
		
		return timeUntilNextSlot; //time until latest valid slot
	}


	//This method uses getSoonestNonPrioritySlot, to find the next available departure slot, and inserts a flight to the departure queue at that time. 
		//This also keeps track of the types of delay needed to absorb the delay generated by departure scheduling.
	public int insertNonPriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		//departure queue empty
		if(departureTrafficByFlight.isEmpty()){
			f.departureTimeFinal = proposedDepartureTime;
			departureTrafficByFlight.add(f);
			return 0;
			//other flights already in the departure queue
		} else {
			int candidateDepartureSlot = getSoonestNonPrioritySlot(f, proposedDepartureTime, true);
			if(f.id==3){
				U.p("candidateDepartureSlot" + candidateDepartureSlot);
			}
			f.departureTimeFinal = candidateDepartureSlot;
			U.Assert(!departureTrafficByFlight.contains(f),
					"DepartureTrafficByFlight.contains(flight) flight already in Departure tree id: " + f.id);
			departureTrafficByFlight.add(f);
			//calculate delay
			int delayNeeded = f.departureTimeFinal - proposedDepartureTime;
			f.atcGroundDelay += delayNeeded;
			f.departureAirportDelay += delayNeeded;
			return delayNeeded;
		}
	}
	
	//Gets the soonest viable priority departure slot based on whether a flight in the queue is CFR or not, with CFR flights being priority over non-CFR. 
	//will prioritize CFR, inserting at this time will push other flights forward (insertPriorityDeparture).
	public int getSoonestPriorityDepartureSlot(Flight f, int proposedDepartureTime){
		int candidateDepartureSlot = proposedDepartureTime;
		Flight previousFlight;
		int previousDeparture;//Departure time in ms 
		int previousSpace; //time spacing for particular Departure time (in ms) (to prevent tail gating)
		int necessaryBuffer = candidateDepartureSlot+getDepartureSpacing(candidateDepartureSlot);
		previousFlight = departureTrafficByFlight.lower(Flight.dummyDeparture(necessaryBuffer));
		if(previousFlight == null){
			previousDeparture = Integer.MIN_VALUE;
		} else {
			if(!previousFlight.cfrEffected){ 
				previousFlight = departureTrafficByFlight.lower(Flight.dummyDeparture(candidateDepartureSlot));
			}
			//previousDeparture = previousFlight.departureTimeFinal;
			previousDeparture = (previousFlight == null)? Integer.MIN_VALUE : previousFlight.departureTimeFinal;
		}
		//null check here
		//previousDeparture = (previousFlight == null)? Integer.MIN_VALUE : previousFlight.departureTimeFinal;
		previousSpace = getDepartureSpacing(previousDeparture);
		//add in all good case
		if(previousDeparture + previousSpace <= candidateDepartureSlot){
			//do nothing //correct
		}else if(previousFlight.cfrEffected ){
			while(previousFlight.cfrEffected && candidateDepartureSlot < previousDeparture + previousSpace){ 
				candidateDepartureSlot = previousDeparture + previousSpace;
				int buffer = candidateDepartureSlot + getDepartureSpacing(candidateDepartureSlot);
				previousFlight = departureTrafficByFlight.floor(Flight.dummyDeparture(buffer)); //use buffer?
				previousDeparture = previousFlight.departureTimeFinal;
				previousSpace = getDepartureSpacing(previousDeparture);	
				//correct
			}
		} 
		U.Assert(proposedDepartureTime != -1, "proposedDepartureTime != -1");
		return candidateDepartureSlot;
		
	}
	
	//Uses getSoonestPriorityDepartureSlot to get soonest viable departure slot based on whether a flight in the queue is CFR or not, with CFR flights being priority over non-CFR. 
	//will prioritize CFR, inserting at this time will push other flights forward in queue.
	//If a flight departing before a CFR flight overlaps, it is pushed forward in queue.
	//returns delay needed for this departure if any
	public int insertPriorityDeparture(Flight f, int proposedDepartureTime, int currentTime){
		if(departureTrafficByFlight.isEmpty()){
			//no delay, because no other Departures at this point
			f.departureTimeFinal = proposedDepartureTime;
			departureTrafficByFlight.add(f);
			return 0;
		//other flights already in the Departure queue
		} else {
			//gets soonest slot that doesn't interfere with previous CFR slots
			int candidateDepartureSlot = getSoonestPriorityDepartureSlot(f, proposedDepartureTime);
			f.departureTimeFinal = candidateDepartureSlot;
			int delayNeeded = f.departureTimeFinal - proposedDepartureTime;
			f.atcGroundDelay += delayNeeded;
			f.departureAirportDelay += delayNeeded;
			
			Flight previousFlight = departureTrafficByFlight.lower(Flight.dummyDeparture(candidateDepartureSlot)); 
			
			//checks for non-cfr flights scheduled before slot that overlap in constraints
			int prevBufferedSlotTime = previousFlight.departureTimeFinal + getDepartureSpacing(previousFlight.departureTimeFinal);
			if(prevBufferedSlotTime <= candidateDepartureSlot){ //true means previous flight does not interfere.
				//otherwise check for flights between candidate slot and its capacity buffer
				int necessaryBuffer = candidateDepartureSlot+getDepartureSpacing(candidateDepartureSlot);
				previousFlight = departureTrafficByFlight.lower(Flight.dummyDeparture(necessaryBuffer)); //catches non-cfr in slot
				prevBufferedSlotTime = previousFlight.departureTimeFinal + getDepartureSpacing(previousFlight.departureTimeFinal);
			}

			//to ensure next flight comes after, will set any previous interfering departure to 1 millisecond after candidate priority slot, and  space them out in mendGaps() below
			//if prev flight buffer overlaps, and prev flights departs before, need to push forward.
			//as long as prev flight departs after candidate, the gaps will be mended in mend()
			if(previousFlight!= null &&  candidateDepartureSlot < prevBufferedSlotTime && previousFlight.departureTimeFinal <= candidateDepartureSlot){ 
				//int offset = previousFlight.departureTimeFinal<=candidateDepartureSlot? 1 : 0;
				int offset = 1; //so that previous flight gets mended up ahead, set to 1 millisec after candidate slot
				int prevDelayNeeded = candidateDepartureSlot - previousFlight.departureTimeFinal + offset;
				previousFlight.atcGroundDelay += prevDelayNeeded;
				previousFlight.departureTimeFinal = candidateDepartureSlot + offset;
				previousFlight.departureAirportDelay += prevDelayNeeded;
				//print(offset + " adding one to " + previousFlight.id);
				U.Assert(!previousFlight.cfrEffected, "should be no CFRs here");
			}
			U.Assert(!departureTrafficByFlight.contains(Flight.dummyDeparture(candidateDepartureSlot)),
					"DepartureTrafficByFlight.contains(flight) deptime already in Departure tree id: " + candidateDepartureSlot);
			departureTrafficByFlight.add(f);
			//makes sure flights are spaced out correctly
			mendTheGapsGoingForwardDeparture(f, currentTime);
			return delayNeeded;
		}
	}
	
	//mends departure queue for correct capacity time spacing after priority departure insertion in the queue.
	//pushes all flights later than f forward to make room for for current flight
	//only pushes forward if necessary
	//change mend so CFR are fixed?
	public void mendTheGapsGoingForwardDeparture(Flight f, int currentTime){
		//includes flight just added above
		Iterator<Flight> iter = departureTrafficByFlight.tailSet(f).iterator();
		//starts on flight just added
		Flight current = iter.next();
		boolean cont = true;
		Flight next;
		ArrayList<Flight> addBackIn = new ArrayList<Flight>();
		
		//pushes flights forward, filling in gaps as moves forward
		while (iter.hasNext() && cont){
			next = iter.next();
			int currentSpacing = getDepartureSpacing(current.departureTimeFinal);
			
			//if next flight is too close
			if(current.departureTimeFinal + currentSpacing > next.departureTimeFinal){
				iter.remove(); // iter is on next so next is removed
				U.Assert(currentTime <= next.departureTimeFinal, airportName + " " + next.airline + " " +currentTime+ " " + next.departureTimeACES +
						" currentTime > f.DepartureTimeFinal scheduling after they have departed " +current.id+ " " + next.id);
				//should have assured that flight has not departed yet(?)
				int delayNeeded = current.departureTimeFinal + currentSpacing - next.departureTimeFinal;
				U.Assert(delayNeeded >= 0, "delayNeeded >= 0 mend the gaps");
				//add ground delay
				next.atcGroundDelay += delayNeeded; 
				next.departureAirportDelay += delayNeeded;
				if(next.cfrEffected){U.p("pushing ahead cfr effected");};
				next.departureTimeFinal = current.departureTimeFinal + currentSpacing;
				addBackIn.add(next);
				current = next;
				currentSpacing = getDepartureSpacing(current.departureTimeFinal);
				//U.p("next Departure time after" + next.DepartureTimeFinal);
			} else {
				cont = false;
			}
		}
		departureTrafficByFlight.addAll(addBackIn); //Sort by ground or air??
	}
	
	public void test1(){ //check delays<<<<<<<<
		Flight f1 = Flight.dummyDeparture(0); f1.cfrEffected = false; 
		Flight f2 = Flight.dummyDeparture(0); f2.cfrEffected = true;
		Flight f3 = Flight.dummyDeparture(0);f3.cfrEffected = false;
		Flight f4 = Flight.dummyDeparture(0);f4.cfrEffected = true;
		U.p(f1.cfrEffected+" f1 "+f1.departureTimeFinal);
		U.p(f2.cfrEffected+" f2 "+f2.departureTimeFinal);
		U.p(f3.cfrEffected+" f3 "+f3.departureTimeFinal);
		U.p(f4.cfrEffected+" f4 "+f4.departureTimeFinal);
		scheduleDeparture(f1, f1.departureTimeFinal, 0);printDepTrafficByFlight();
		scheduleDeparture(f2, f2.departureTimeFinal, 0);printDepTrafficByFlight();
		scheduleDeparture(f3, f3.departureTimeFinal, 0);printDepTrafficByFlight();
		scheduleDeparture(f4, f4.departureTimeFinal, 0);
		U.p("###after");
		printDepTrafficByFlight();
	}
	
	//older method:
	//finds first available slots and inserts slot 
	//then push all following flights forward with minimal spacing
	//until the next flight's spacing won't be effected.
	//'previous' and 'next' assume model of times on a number line (previous = earlier)
	// No freeze horizon consideration in this version; i.e., for example, 
	//flight within a second of touchdown can still be delayed further in the air (still true)
	public void insertAtSoonestArrivalWithForwardGapsRemoved(Flight flight, int proposedArrivalTime, int currentTime){ 
		//this being empty does mean flight has not already been scheduled
		if(arrivalTrafficByFlight.isEmpty()){
			//no delay, because no other arrivals at this point
			if(flight.arrivalFirstSlot == -1)flight.arrivalFirstSlot = proposedArrivalTime;
			flight.arrivalTimeFinal = proposedArrivalTime;
			arrivalTrafficByFlight.add(flight);			
		//other flights already in the arrival queue
		} else {
			int proposedSlotTime = proposedArrivalTime;
			Flight previousFlight;
			Integer previousArrival;//arrival time in ms 
			Integer previousSpace; //time spacing for particular arrival time (in ms) (to prevent tail gating)
			boolean cont = true;
			while(cont){
				previousFlight = arrivalTrafficByFlight.floor(Flight.dummyArrival(proposedSlotTime));
				previousArrival = previousFlight != null? previousFlight.arrivalTimeFinal : Integer.MIN_VALUE;
				//nextFlight = arrivalTrafficByFlight.higher(Flight.dummyArrival(proposedArrivalTime));
				//nextArrival = nextFlight!=null? nextFlight.arrivalTimeFinal : Integer.MAX_VALUE;
				previousSpace = getArrivalSpacing(previousArrival);
				//previous flight is far enough back. //there's a gap!
				if( previousArrival + previousSpace <= proposedSlotTime){
					int additionalDelay = proposedSlotTime-proposedArrivalTime;
					//first time being scheduled, all delay goes to ground
					if(flight.arrivalFirstSlot == -1){
						flight.arrivalFirstSlot = proposedSlotTime;
						U.Assert(flight.atcGroundDelay == 0, "flight.departureDelayFromArrivalAirport == 0 shouldn't be ground delay yet");
						flight.atcGroundDelay = additionalDelay; // add ground delay
					} else {
						//flight is being re-scheduled
						if(currentTime <= flight.wheelsOffTime ){
							//all flights here are current time == wheels off time
							//except jiggle where wheels off time is pushed forward sometimes.
							if(departureContract) {
								//only add to air to maintain departure time contract
								flight.atcAirDelay += additionalDelay; 
							} else {
//								the following two go together to allow contract changes; and either these two are active or the next line is active at a time
								flight.atcGroundDelay += additionalDelay; // add ground delay
								flight.wheelsOffTime += additionalDelay;
								flight.additionalGDfromReschedule += additionalDelay;
							}
						} else {
							//scenario does not include rescheduling in the air, 
							System.out.println( "error scheduling in the air in airport tree");
							//current time is after wheels off
							//flight.atcAirDelay += proposedSlotTime-proposedArrivalTime;
						}
					}
					flight.arrivalTimeFinal = proposedSlotTime;
					U.Assert(!arrivalTrafficByFlight.contains(flight), "arrivalTrafficByFlight.contains(flight) flight not already in arrival tree");
					arrivalTrafficByFlight.add(flight); // should not bee added a secondtime
					
					mendTheGapsGoingForward(flight, currentTime);
					// mend the gaps!
					cont = false;
				} else {
					proposedSlotTime = previousArrival + previousSpace;
				}
			}	
		}
	}
	
	//pushes all flights later than f forward in arrival queue to make room for f
	//only pushes forward if necessary
	public void mendTheGapsGoingForward(Flight f, int currentTime){
		//includes flight just added above
		Iterator<Flight> iter = arrivalTrafficByFlight.tailSet(f).iterator();
		//starts on flight just added
		Flight current = iter.next();
		boolean cont = true;
		Flight next;
		ArrayList<Flight> addBackIn = new ArrayList<Flight>();
		
		//pushes flights forward, filling in gaps as moves forward
		while (iter.hasNext() && cont){
			next = iter.next();
			int currentSpacing = getArrivalSpacing(current.arrivalTimeFinal);
			
			//U.p("current arrival time " + current.arrivalTimeFinal);U.p("current spacing " + currentSpacing);U.p("next arrival time " + next.arrivalTimeFinal);
			
			//if next flight is too close
			if(current.arrivalTimeFinal + currentSpacing > next.arrivalTimeFinal){
				
				iter.remove(); // iter is on next so next is removed
				U.Assert(currentTime <= next.arrivalTimeFinal, next.airline + " " +currentTime+ " " + next.departureTimeACES +
						" currentTime >= f.arrivalTimeFinal not scheduling flights that have landed ");
				//int arrivalDelay = getArrivalSpacing(current.arrivalTimeFinal);
				int delayNeeded = current.arrivalTimeFinal + currentSpacing - next.arrivalTimeFinal;			
				//do delays
				//flight is still on the ground
				if(currentTime <= next.wheelsOffTime ){

					if(departureContract){
						//only add to air to maintain departure time contract
						next.atcAirDelay += delayNeeded;
					}else{
						// next flight gets air delay so that we can keep departures constant
						// the following two go together to allow contract changes; and either these two are active or the next line is active at a time
						next.atcGroundDelay += delayNeeded; //all further delay taken in air
						next.wheelsOffTime += delayNeeded; //all further delay taken in air						
					}
				} else {
					next.atcAirDelay += delayNeeded;
				}
				//new arrival time is soonest time after current arrival with it's spacing
				
				//keep track of jiggles
				if(delayNeeded != 0){
					next.totalJiggleAmount += delayNeeded;
					next.numberOfJiggles++;
				}
				
				next.arrivalTimeFinal = current.arrivalTimeFinal + currentSpacing;
				addBackIn.add(next);
				current = next;
				currentSpacing = getArrivalSpacing(current.arrivalTimeFinal);
				//U.p("next arrival time after" + next.arrivalTimeFinal);
			} else {
				cont = false;
			}
		}
		arrivalTrafficByFlight.addAll(addBackIn); //Sort by ground or air??
	}
	
	//throws exception if this flight was not in arrival queue
	public boolean freeArrivalSlot(Flight f){
		if(arrivalTrafficByFlight.remove(f)){
			return true;
		} else {
			throw new java.lang.Error("FAILED: flight not removed, not found in arrival queue");
			
		}
		
	}
	
	
	//Validate the departure/arrival queues that store Flights.
	//checks capacity constraint spacing and that no flight is in queue twice
	public void validateByFlight(){
		TreeSet<Integer> noRepeats = new TreeSet<Integer>();
		
		int minSpacing = Integer.MAX_VALUE;
		int lastTime = Short.MIN_VALUE*10;
		int timeOfMin = 0;
		int currentTime;
		int prevId = -1;
		
		//departure validation
		for(Flight f: departureTrafficByFlight){
			//asserts no flights in traffic twice
			U.Assert(!noRepeats.contains(f.id),"dup id's in departure traffic!!!! " + f.id);
			noRepeats.add(f.id);
			currentTime = f.departureTimeFinal;
			int lastTimeSpace = currentTime-lastTime; //space between last arrival and this arrival
			U.Assert(lastTimeSpace >= getDepartureSpacing(lastTime),
					airportName + " lastTimeSpace >= getDepartureSpacing(lastTime) between " + prevId + " and "+ f.id 
					+ " pd "+lastTime+" cd "+ currentTime + " last space is " + lastTimeSpace + " should be " +  getDepartureSpacing(lastTime));
			
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			}
			lastTime = currentTime;
			prevId = f.id;
			
		}
		
		if(minSpacing < getDepartureSpacing(timeOfMin)){
			System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			U.Assert(minSpacing < getDepartureSpacing(timeOfMin), "min < getArrivalSpacing(time)");
		}
		
		//arrival testing
		minSpacing = Integer.MAX_VALUE;
		lastTime = Short.MIN_VALUE*10;
		timeOfMin = 0;
		noRepeats = new TreeSet<Integer>();

		for(Flight f: arrivalTrafficByFlight){
			//asserts no flights in traffic twice
			U.Assert(!noRepeats.contains(f.id),airportName + " dup id's in arrival traffic!!!! " + f.id);
			noRepeats.add(f.id);
			
			currentTime = f.arrivalTimeFinal;
			int lastTimeSpace = currentTime-lastTime; //space between last arrival and this arrival
			U.Assert(lastTimeSpace >= getArrivalSpacing(lastTime), "lastTimeSpace >= getArrivalSpacing(lastTime)");
			
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			}
			lastTime = currentTime;
		}
		
		
		if(minSpacing < getArrivalSpacing(timeOfMin)){
			System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			U.Assert(minSpacing < getArrivalSpacing(timeOfMin), "min < getArrivalSpacing(time)");
		}
		minSpacing = Integer.MAX_VALUE;
		lastTime = Short.MIN_VALUE*10;
		timeOfMin = 0;
		// need this??
	}
	
	//Testing that flights are spaced correctly
	public void testGaps(){
		airportCapacities.add(new CapacityByTime(0, -1, 30*60*1000));
		airportCapacities.add(new CapacityByTime(8, -1, 10*60*1000));
		System.out.println("getArrivalCapacity(1): " + getArrivalSpacing(1));
		System.out.println("getArrivalCapacity(8): " + getArrivalSpacing(8));
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),1,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),6,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),14,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),4,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),12,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),8,1);
		printArrTrafficGapless();
		insertAtSoonestArrivalWithForwardGapsRemoved(Flight.dummyArrival(-1),3,1);
		printArrTrafficGapless();
	}	
	
	//returns spacing in milleseconds i.e AAR 120 / hr = 30000 ms / arrival
	public int getArrivalSpacing(int time){
		CapacityByTime c = airportCapacities.floor(new CapacityByTime(time));
		int aar = c != null? c.aar: DEFAULT_ARR_RATE; //3600000;//30;//9999;
		return 60*60*1000 / aar;
		
	}
	
	public int getDepartureSpacing(int time){
		CapacityByTime c = airportCapacities.floor(new CapacityByTime(time));
		int adr = c != null? c.adr: DEFAULT_DEP_RATE;//3600000; //30;//9999;
		return 60*60*1000 / adr; 
		//check this
		//return 10;
	}
	
	//older method for integer based queue
	public int getSoonestDepartureSlot(int departureTime){
		Integer before, previousSpace, currentSpace, after;
		while(true){
			before = airportDepartureTraffic.floor(departureTime);
			before = before != null? before : Integer.MIN_VALUE;
			after = airportDepartureTraffic.ceiling(departureTime);
			after = after!=null? after : Integer.MAX_VALUE;
			//spacing based on adr rates. i.e. 30 dep per hour means 2min space between flights.
			currentSpace = getDepartureSpacing(departureTime);
			previousSpace = getDepartureSpacing(before);
			//ensures departure time will be spaced out between any two flights.
			if( before + previousSpace <= departureTime && departureTime + currentSpace <= after){
				//System.out.printf(" before: %d after: %d ",before, after);
				return departureTime;
			}
			//first tries to depart at the space after last flight.
			if(before + previousSpace > departureTime){
				departureTime = before + previousSpace;
			//otherwise at space after next flight
			} else {
				departureTime = after + getDepartureSpacing(after);
			}
		}
	}
	
	////older method for integer based queue
	//works the same as departures
	public int getSoonestArrivalSlot(int arrivalTime){
		Integer before, previousSpace, currentSpace, after; 
		while(true){
			before = airportArrivalTraffic.floor(arrivalTime);
			before = before != null? before : Integer.MIN_VALUE;
			after = airportArrivalTraffic.ceiling(arrivalTime);
			after = after!=null? after : Integer.MAX_VALUE;
			currentSpace = getArrivalSpacing(arrivalTime);
			previousSpace = getArrivalSpacing(before);
			if( before + previousSpace <= arrivalTime && arrivalTime + currentSpace <= after){
				//System.out.printf(" before: %d after: %d ",before, after);
				return arrivalTime;
			}
			if(before + previousSpace > arrivalTime){
				arrivalTime = before + previousSpace; 
			} 
			
			else {
				arrivalTime = after + getArrivalSpacing(after); 
			}
		}
	}
	
	//older method for integer based queue
	public int getSoonestArrivalSlot(int arrivalTime, int minArrivalTime, int maxArrivalTime){
		Integer before, previousSpace, currentSpace, after; 
		while(true){
			before = airportArrivalTraffic.floor(arrivalTime);
			before = before != null? before : Integer.MIN_VALUE;
			after = airportArrivalTraffic.ceiling(arrivalTime);
			after = after!=null? after : Integer.MAX_VALUE;
			currentSpace = getArrivalSpacing(arrivalTime);
			previousSpace = getArrivalSpacing(before);
			if( before + previousSpace <= arrivalTime && arrivalTime + currentSpace <= after){
				//System.out.printf(" before: %d after: %d ",before, after);
				return arrivalTime;
			}
			if(before + previousSpace > arrivalTime){
				arrivalTime = before + previousSpace; 
				
				//System.out.println("arrivalTime " + arrivalTime);
			} 
			else {
				arrivalTime = after + getArrivalSpacing(after); 
			}
		}
	}
	
	//older method for integer based queue
	//schedules flight at soonest time on or later than input parameter, returns time
	public int insertAtSoonestArrival(int arrival){
		int soonest = getSoonestArrivalSlot(arrival);
		airportArrivalTraffic.add(soonest);
		return soonest;
	}
	
	//older method for integer based queue
	public int insertAtSoonestArrival(int arrival, int scheduledArrival){
		int soonest = getSoonestArrivalSlot(arrival);
		airportArrivalTraffic.add(soonest);
		scheduledAirportArrivalTraffic.add(scheduledArrival);
		return soonest;
	}
	
	//older method for integer based queue
	public int insertAtSoonestArrival(int arrival, int scheduledArrival, Flight f) {
		int soonest = getSoonestArrivalSlot(arrival);
		airportArrivalTraffic.add(soonest);
		scheduledAirportArrivalTraffic.add(scheduledArrival);
		arrivalTrafficByFlight.add(f);
		return soonest;
	}
	
	//older method for integer based queue
	//returns false if this arrival time was not in list
	public boolean freeArrivalSlot(int arrivalTimeToRemove){
		int closest = airportArrivalTraffic.floor(arrivalTimeToRemove);
		if(closest == arrivalTimeToRemove){
			airportArrivalTraffic.remove(arrivalTimeToRemove);
			return true;
		} else {
			throw new java.lang.Error("FAILED: flight not removed, not found in arrival queue");
			
		}
		
	}
	
	//older method for integer based queue
	public int insertAtSoonestDeparture(int departure, int scheduledDeparture){
		int soonest = getSoonestDepartureSlot(departure);
		airportDepartureTraffic.add(soonest);
		scheduledAirportDepartureTraffic.add(scheduledDeparture + (int)(Math.random()*1000));
		return soonest;
	}
	
	public void insertCapacity(CapacityByTime c){
		airportCapacities.add(c);
	}
	
	public void printDepTraffic(){
		io.println("***Start DEP(" + airportDepartureTraffic.size() + ")");
		for(int c: airportDepartureTraffic){
			System.out.println(c);
		}
		io.println("***End deps");
	}
	
	public void printDepTrafficByFlight(){
		io.println("***Start DEP(" + departureTrafficByFlight.size() + ")");
		for(Flight f: departureTrafficByFlight){
			//U.p(++i);
			f.print();
		}
		io.println("***End deps");
	}
	public void printArrTrafficByFlight(){
		io.println("***Start ARR(" + arrivalTrafficByFlight.size() + ")");
		int i = 0;
		for(Flight f: arrivalTrafficByFlight){
			//U.p(++i);
			U.pp(++i + " ");f.print();
		}
		io.println("***End deps");
	}
	
	public void printArrTrafficOrdering(){
		io.println("***Start ARR(" + arrivalTrafficByFlight.size() + ")");
		int i = 0;
		for(Flight f: arrivalTrafficByFlight){
			//U.p(++i);
			U.p(++i + " " +f.id);
		}
		io.println("***End deps");
	}
	
	public void printScheduledDepTraffic(){
		io.println("***Start ScheduledDEP(" + scheduledAirportDepartureTraffic.size() + ")");
		for(int c: scheduledAirportDepartureTraffic){
			System.out.println(c);
		}
		io.println("***End scheduled deps");
	}
	
	public void printCapsToFile(BufferedWriter out) {
		try {
			if(airportCapacities.isEmpty()) {
				out.write(airportName +",");
				out.write("0" + "," + DEFAULT_DEP_RATE +"," + DEFAULT_ARR_RATE);
				out.write("\n");
			}
			else {
				for(CapacityByTime cu: airportCapacities) {
					out.write(airportName+",");
					out.write(cu.time + "," + cu.adr + "," + cu.aar);
					out.write("\n");
				}
			}
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printArrTrafficGapless(){
		io.println("***Start ARR(" + arrivalTrafficByFlight.size() + ")");
		for(Flight c: arrivalTrafficByFlight){
			System.out.println(c.arrivalTimeFinal);
		}
		io.println("***End arrs");
	}
	
	
	public void printArrTraffic(){
		io.println("***Start ARR(" + airportArrivalTraffic.size() + ")");
		for(int c: airportArrivalTraffic){
			System.out.println(c);
		}
		io.println("***End arrs");
	}
	
	public void printScheduledArrTraffic() {
		io.println("***Start ScheduledARR(" + scheduledAirportArrivalTraffic.size() + ")");
		for(int c: scheduledAirportArrivalTraffic) {
			System.out.println(c);
		}
		io.println("***End scheduled arrs");
	}
	
	public void printDepTrafficToFile(BufferedWriter dep, BufferedWriter schedDep) {
		try{
			dep.write(airportName + ",");
			for(int c: airportDepartureTraffic) {
				dep.write(c +",");
			}
			dep.write("\n");
			
			schedDep.write(airportName + ",");
			for(int d: scheduledAirportDepartureTraffic) {
				schedDep.write(d+",");
			}
			schedDep.write("\n");
			
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printArrTrafficToFile(BufferedWriter arr, BufferedWriter schedArr) {
		try{
			arr.write(airportName + ",");
			for(int c: airportArrivalTraffic) {
				arr.write(c +",");
			}
			arr.write("\n");
			
			schedArr.write(airportName +",");
			for(int c: scheduledAirportArrivalTraffic) {
				schedArr.write(c+",");
			}
			schedArr.write("\n");
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printDelayVars(){
		io.printf("Gate: mean: %.3f zero: %.3f std: %.3f\n", gateMean, gateZeroProbablity, gateStd);
		io.printf("Taxi: mean: %.3f zero: %.3f std: %.3f taxi unimpeded: %.3f\n", taxiMean, taxiZeroProbablity, taxiStd, taxiUnimpeded);
	}
	
	public void printCaps(){
		io.println("***Start caps(" + airportCapacities.size() + ")");
		for(CapacityByTime cu: airportCapacities){
			cu.print();
		}
		io.println("***End caps");
	}
	
	public void print(){
		io.println(airportName);
		printDelayVars();
		printCaps();

		io.printf("CFR start: %d end: %d",CFRstart, CFRend);
		io.println("Dep Traffic (Integer): " + airportDepartureTraffic.size());
		io.println("Arr Traffic (Integer): " + airportArrivalTraffic.size());
		printArrTraffic();
		printDepTraffic();
		io.println("Dep Traffic (Flight): " + arrivalTrafficByFlight.size());
		io.println("Arr Traffic (Flight): " + departureTrafficByFlight.size());
		printDepTrafficByFlight();
		printArrTrafficByFlight();
		io.println("");
	}
	
	public void printToFile(BufferedWriter cap, BufferedWriter dep, BufferedWriter schedDep, BufferedWriter arr, BufferedWriter schedArr) {
		try{
			printCapsToFile(cap);
			if(!airportArrivalTraffic.isEmpty() && !scheduledAirportArrivalTraffic.isEmpty()) {
				printArrTrafficToFile(arr, schedArr);
			}
			if(!airportDepartureTraffic.isEmpty() && !scheduledAirportDepartureTraffic.isEmpty()) {
				printDepTrafficToFile(dep, schedDep);
			}
			
		}catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	
	public void printMinDepartureSpacing() {
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;

		for(Integer currentTime: airportDepartureTraffic){
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			} 
			lastTime = currentTime;
		}
		if(minSpacing < getDepartureSpacing(timeOfMin)){
			System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printDepTraffic();
			U.Assert(minSpacing < getDepartureSpacing(timeOfMin), airportName + " min < getDepartureSpacing(time)");
		}
		if(minSpacing == getDepartureSpacing(timeOfMin))
		System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
	}
	
	public void printMinArrivalSpacing() {
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;

		for(Integer currentTime: airportArrivalTraffic){
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			}
			lastTime = currentTime;
		}
		if(minSpacing < getArrivalSpacing(timeOfMin)){
			System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printArrTraffic();
			U.Assert(minSpacing < getArrivalSpacing(timeOfMin), "min < getArrivalSpacing(time)");
		}
		if(minSpacing == getArrivalSpacing(timeOfMin))
		System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
		
	}
	
	public void printMinSpacing(){
		printMinDepartureSpacing();
		printMinArrivalSpacing();	
	}
	
	//older method for integer based queue
	public void validateDepartureTraffic() {
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;

		for(Integer currentTime: airportDepartureTraffic){				
			//REAL VALIDATION
			int lastTimeSpace = currentTime-lastTime;
			//System.out.println("lastTimeSpace= " + lastTimeSpace + " arrivalSpacing= " + getDepartureSpacing(lastTime));
			U.Assert(lastTimeSpace >= getDepartureSpacing(lastTime),lastTimeSpace + " DEP lastTimeSpace >= getArrivalSpacing(lastTime) " + getDepartureSpacing(lastTime));
			
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			} 
			lastTime = currentTime;
		}
		if(minSpacing < getDepartureSpacing(timeOfMin)){
			System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printDepTraffic();
			U.Assert(minSpacing < getDepartureSpacing(timeOfMin), airportName + " min < getDepartureSpacing(time)");
		}
		if(minSpacing == getDepartureSpacing(timeOfMin)){
		//System.out.println(airportName + " departure: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getDepartureSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
		}
	}
	
	//older method for integer based queue
	public void validateArrivalTraffic() {
		Integer minSpacing = Integer.MAX_VALUE;
		Integer lastTime = Short.MIN_VALUE*10;
		Integer timeOfMin = 0;
		for(Integer currentTime: airportArrivalTraffic){
			
			//REAL VALIDATION
			int lastTimeSpace = currentTime-lastTime;
			//System.out.println("lastTimeSpace= " + lastTimeSpace + " arrivalSpacing= " + getArrivalSpacing(lastTime));
			U.Assert(lastTimeSpace >= getArrivalSpacing(lastTime),lastTimeSpace + 
					" ARR lastTimeSpace >= getArrivalSpacing(lastTime) " + getArrivalSpacing(lastTime));
			
			if((currentTime-lastTime) < minSpacing){
				minSpacing = currentTime-lastTime;
				timeOfMin = lastTime;//currentTime;
			}
			lastTime = currentTime;
		}
		if(minSpacing < getArrivalSpacing(timeOfMin)){
			System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
			printCaps();
			printArrTraffic();
			U.Assert(minSpacing < getArrivalSpacing(timeOfMin), "min < getArrivalSpacing(time)");
		}
		if(minSpacing == getArrivalSpacing(timeOfMin)){
			//System.out.println(airportName + " arrival: min spacing = " + (double)minSpacing/60000 + " spacing: " + (double)getArrivalSpacing(timeOfMin)/60000 + " time: " + timeOfMin);
		}
	}
	
	//older method for integer based queue
	public void validate(){
		validateDepartureTraffic();
		validateArrivalTraffic();
		validateByFlight();
	}
}




