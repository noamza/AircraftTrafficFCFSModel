package fcfs;

import java.util.ArrayList;
import java.util.TreeSet;

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

class t{
	int x;
	t(int i){x=i;}
}


public class NOTES {
	ArrayList<t> a = new ArrayList<t>(); t m;
	void a(ArrayList<t> b){
	m = new t(0);
	a = b;
	a.get(1).x = -2;
	print();
	a.add(new t(-3));
	//a.add(new t(2));
	a.get(0).x = a.get(0).x*-1;
	}
	void m2(){ m.x = 2;}
	void m(t k){ m = k; }
	void mp(){ Main.p("m: " + m.x);}
	void print(){
		for( t c: a) Main.p("NO v = "+c.x); Main.p("no*");
		
	}
	
	/*
	limited uncertainty:false all cfr:false none cfr:false
	
	limited uncertainty:false all cfr:true none cfr:false
	
	limited uncertainty:false all cfr:false none cfr:true
	
	limited uncertainty:true all cfr:false none cfr:false
	
	limited uncertainty:true all cfr:true none cfr:false
	*/


	
	
	
	/*
	 * params from mo
	 * 
-Xmx2048M
-Xms512M
-Dworking.directory=/Users/mrefai/Documents/workspace/git/fcfs/data/inputs
-Doutput.directory=../outputs/departureArrivalFCFS
-Dsector.crossing.file=job_24_sector_transitTime_takeoffLanding_35h_1.csv
-Dsector.file=SectorList_YZ2007May.csv
-Dairport.file=AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv
	 * 
	 * 
	 */
	

	/*
	 * for 100,000 runs x 7 scenarios, with airport data output 
	 Start 13:22:39:0591
	 FIN!  19:10:34:0114
	 
	 	FCFSDep
	 	
	  * why when random seeded, first run different from all the rest?????
	  
	 FIN!  13:01:23:0176 10 = 1 min 100 = 10 min?
	 Start 13:00:34:0230
	
	 Start 13:06:36:0877
	   FIN! 13:13:39:0478 7 min 100
	   
	  
	  >> no preference for ground delay
	  >> flights pushed forward not unpushed..
	 
	 
- The more randomized a schedule is the less delay there is.

- Scheduling ahead of time means less accuracy therefore more flights can't make their slots 
	and need to be rescheduled therefore two rounds of delay are added up
	However, if you subtract the amount of time the flight had to wait anyway, the amount of extra time
	these flights have to wait from ATC is less, than if you schedule only at CFR.
	
- Scheduling flights at pushback means more flights make their slots, however any delay given
	needs to be added on top of how much the flight is already late. 
	 
	 
	 ALL RUNS ARE MADE AT 0, 15, 30, 45, 60 MIN IN ADVANCE	 
	 
	 1.  FCFS DEP, ANNOUNCED ESTIMATE 10 MINUTES IN ADVANCE, CAN'T ADJUST ANNOUNCED FLIGHTS.
	 
	 2.  SAME AS 1 WITH UNCERTAINTY. IF YOU MISS A SLOT, RESCHEDULE, RECYCLE, AND RE-ASSIGN SLOT
	 
	 3.	 SAME AS 1 EXCEPT FCFS -> PFCFS, ALLOW JIGGLING UP TO ARRIVAL.
	 
	 4.  SAME AS 3 WITH UNCERTAINTY
	 
	 5.  SAME AS 3,4 BUT SCHEDULAR HAS UP TO 10 MIN BEFORE ESTIMATED DEPARTURE TO RESPOND WITH DELAYS. (?)
	 
	 
	 
	 ESTIMATE = GATE DEP PERTURBATION + UNIMPEDED TAXI	 
	 ACTUAL GATE PERTUBATION = ABS GAUSSIAN OF 0,2,4,6,8 MIN FOR CORRESPENDING # OF MIN IN ADVANCE
	 WHEELS OFF = ESTIMATE + AGT + TP
	 
	 */
	
	
	/* JAVA NOTES	 
	 
	 //TreeSet// does maintain ordering if references are changed around.
	  
	 //Object Variables//  
	 When assigning one variable to another, both variables are pointing to the same memory location.
	 If you assign another object to one of the variables, they will no longer be pointing to the same memory.
	 Hence =='s.
	 i.e. o a = o.1; o b = a; a = o.2; NOW a == o.2 and b == 0.1 
	 
	 
	 */
	
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
		 * /*
		 * int i = 10; double b = 7; System.out.println(i); System.out.println(b/i);
		 */
		
	}
	
}



/*
public void scheduleInTheAirByFlight(SchedulingEvent event) // case 3 and 4
{
	Flight f = event.flight;
	//duration speeds
	int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
	int fastestDuration =  (int)(nominalDuration/(1+speedUp));
	//by allowing wheels off time to be modified by other flights some, some flights wheels off time is greater than current time
	if(f.wheelsOffTime > event.eventTime){
		schedulingQueue.add(new SchedulingEvent(f.wheelsOffTime, -4, ScheduleMode.scheduleInTheAirByFlight, f));
	} else {								
		f.rescheduled = true;
		f.atcAirDelay = 0;
		f.numberOfJiggles = 0; f.originalJiggle = f.totalJiggleAmount;
		f.totalJiggleAmount = 0;
		int targetArrival = f.wheelsOffTime + nominalDuration; 						
		airports.schedulePackedArrival(f, targetArrival, event.eventTime); //f.wheelsOffTime? //event.eventTime << original
		//delay by airport
		// original slot =   f.arrivalTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time +     groundDelay;                                         (ground)   == FIRST SLOT 
		// wheelsOffTime = f.departureTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time +     f.taxi_perturbation + (uncertainty || groundDelay)  + jiggle;   == FINAL SLOT
	}

}
//*/

/*
//1.  FCFS DEP, ANNOUNCED ESTIMATE 10 MINUTES IN ADVANCE, CAN'T ADJUST ANNOUNCED FLIGHTS.
private void fcfs(SchedulingEvent event)
{
	Flight f = event.flight;
	//duration speeds
	int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
	int fastestDuration =  (int)(nominalDuration/(1+speedUp));

	int proposedArrivalTime = f.arrivalTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time; 
	int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot including perturbation
	f.arrivalFirstSlot = arrivalSlot;
	int delayFromFirstScheduling = arrivalSlot - proposedArrivalTime;
	f.atcGroundDelay = delayFromFirstScheduling; 
	int delayDelta = Math.max(delayFromFirstScheduling-f.gate_perturbation,0); 
	//delayFromFirstScheduling = 0; //TAKE THIS OUT
	int wheelsOffTime = f.departureTimeProposed+f.gate_perturbation + f.taxi_unimpeded_time + delayFromFirstScheduling;// + f.taxi_perturbation; 
	f.wheelsOffTime = wheelsOffTime;
	int lastOnTimeDeparturePoint = arrivalSlot - fastestDuration;

	//slot should be more reachable since slot is further out.
	//therefore more flights should make their slots
	//therefore LESS airborne cases
	if(wheelsOffTime > lastOnTimeDeparturePoint){

		schedulingQueue.add(new SchedulingEvent(lastOnTimeDeparturePoint, arrivalSlot, ScheduleMode.remove, f));
		schedulingQueue.add(new SchedulingEvent(wheelsOffTime,-6, ScheduleMode.scheduleInTheAir, f));// wheelsOffTime+nominalDuration
		if(wheelsOffTime < f.departureTimeProposed + f.gate_perturbation){
			Main.p("error scheduling in past tense");
		}
	} else {
		//flight can arrive by speeding up

		f.arrivalTimeFinal = arrivalSlot;
	}
	//if(f.id == 23672){ Main.p(mode + " " + minsAhd);f.printVariables();}

}
//*/
/*
// 2.  SAME AS 1 WITH UNCERTAINTY. IF YOU MISS A SLOT, RESCHEDULE, RECYCLE, AND RE-ASSIGN SLOT

private void FCFSUncertainty(SchedulingEvent event)
{
	Flight f = event.flight;
	//duration speeds
	int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
	int fastestDuration =  (int)(nominalDuration/(1+speedUp)); //more flights have to be scheduled twice with ground since slot arrival time estimate is bad,
	//so more totalAirDelayl delay per pdt run.
	int proposedArrivalTime = f.arrivalTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time; 
	int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot
	f.arrivalFirstSlot = arrivalSlot;
	int delayFromFirstScheduling = arrivalSlot - proposedArrivalTime;
//	//uncertainty //check???????????
//	int uncertaintyMilliSec = (int)(Math.abs( ( random.nextGaussian()*(minsAhd*2/15) ) )*minToMillisec)*uncertaintyToggle;
//	f.gateUncertainty = uncertaintyMilliSec;
	int delayDelta = Math.max(f.gateUncertainty,delayFromFirstScheduling);
	f.atcGroundDelay = delayFromFirstScheduling;
	//f.departureDelayFromArrivalAirport = delayDelta; ??????
	int pushbackTime = f.departureTimeProposed + delayDelta + f.gate_perturbation; //delayFromFirstScheduling or delay delta???
	int wheelsOffTime =  pushbackTime + f.taxi_unimpeded_time + f.taxi_perturbation;
	f.wheelsOffTime = wheelsOffTime;
	int lastOnTimeDeparturePoint = arrivalSlot - fastestDuration;
	//Gano delay accounting
	//f.departureDelayFromArrivalAirport = Math.max(2*delayFromFirstScheduling - Math.max(f.gate_perturbation, delayFromFirstScheduling), 0);

	if (wheelsOffTime > lastOnTimeDeparturePoint){
		//flight leaves too late.
		schedulingQueue.add(new SchedulingEvent(lastOnTimeDeparturePoint, arrivalSlot, ScheduleMode.remove, f));
		schedulingQueue.add(new SchedulingEvent(wheelsOffTime, -4, ScheduleMode.scheduleInTheAir, f));
	} else {
		f.arrivalTimeFinal = arrivalSlot;
	}

}
//*/
/*
// 3.	 SAME AS 1 EXCEPT FCFS -> PFCFS, ALLOW JIGGLING UP TO ARRIVAL.
public void ArrivalJiggle(SchedulingEvent event)
{
	Flight f = event.flight;
	//duration speeds
	int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
	int fastestDuration =  (int)(nominalDuration/(1+speedUp)); //more flights have to be scheduled twice with ground since slot arrival time estimate is bad,
	//so more totalAirDelayl delay per pdt run.
	int proposedArrivalTime = f.arrivalTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time;
	//int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot
	airports.schedulePackedArrival(f, proposedArrivalTime, event.eventTime);
	//f.departureDelayFromArrivalAirport = delayDelta;
	int pushback = f.departureTimeProposed + f.gate_perturbation + f.atcGroundDelay;
	int wheelsOffTime = pushback + f.taxi_unimpeded_time;// + f.taxi_perturbation; //add in taxi??
	f.wheelsOffTime = wheelsOffTime;
	int lastOnTimeDeparturePoint = f.arrivalFirstSlot - fastestDuration;
	//if(f.id == id)Main.p("l "+ lastOnTimeDeparturePoint /1000 + " w " + wheelsOffTime/1000 + " d " + delayFromFirstScheduling);
	//f.departureDelayFromArrivalAirport = 
	//Math.max(2*delayFromFirstScheduling - Math.max(f.gate_perturbation, delayFromFirstScheduling), 0);

	if (wheelsOffTime > lastOnTimeDeparturePoint){
		//flight leaves too late.
		System.out.println(" I am here no taxi pert");
		schedulingQueue.add(new SchedulingEvent(lastOnTimeDeparturePoint, -8, ScheduleMode.removeByFlight, f));// -8 dummy value
		schedulingQueue.add(new SchedulingEvent(wheelsOffTime, -4, ScheduleMode.scheduleInTheAirByFlight, f));
	} else {


		//f.arrivalTimeFinal = arrivalSlot;
		//f.validate();
		//groundSlots++;
	}
}	
//*/
/*
//4.  SAME AS 3 WITH UNCERTAINTY
public void ArrivalJiggleUncertainty(SchedulingEvent event)
{
	Flight f = event.flight;
	//duration speeds
	int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
	int fastestDuration =  (int)(nominalDuration/(1+speedUp)); //more flights have to be scheduled twice with ground since slot arrival time estimate is bad,
	//so more totalAirDelayl delay per pdt run.
	int proposedArrivalTime = f.arrivalTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time;
	//int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot
	airports.schedulePackedArrival(f, proposedArrivalTime, event.eventTime);

	if(f.gateUncertainty<0){System.out.println("err uncertainty is '-' " + f.gateUncertainty);}
	int delayDelta = Math.max(f.gateUncertainty,f.atcGroundDelay); // only works if this is the first time scheduled/ground-delay once only/contract enforcement
	//related to the way CFR is expected to work in reality (take max of the two (ATC vs Airline delay). Any further delay/rescheduling is taken in the air
	int pushback = f.departureTimeProposed + f.gate_perturbation + delayDelta;
	int wheelsOffTime = pushback + f.taxi_unimpeded_time + f.taxi_perturbation; //add in taxi??
	f.wheelsOffTime = wheelsOffTime;
	int lastOnTimeDeparturePoint = f.arrivalFirstSlot - fastestDuration;
	//totalGroundDelay+=f.departureDelayFromArrivalAirport/60000.0;

	if (wheelsOffTime > lastOnTimeDeparturePoint){
		//flight leaves too late.
		schedulingQueue.add(new SchedulingEvent(lastOnTimeDeparturePoint, -8, ScheduleMode.removeByFlight, f));// -8 dummy value
		schedulingQueue.add(new SchedulingEvent(wheelsOffTime, -4, ScheduleMode.scheduleInTheAirByFlight, f));

	} else {
		//f.arrivalTimeFinal = arrivalSlot;
		//f.validate();
		//groundSlots++;
	}
}	
//*/

/*
public void scheduleByArrival(SchedulingEvent event)
{
	Flight f = event.flight;
	//duration speeds
	int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
	int fastestDuration =  (int)(nominalDuration/(1+speedUp));
	int proposedArrivalTime = f.arrivalTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time;
	int arrivalSlot = airports.scheduleArrival(f.arrivalAirport, proposedArrivalTime); //Make Slot
	f.arrivalFirstSlot = arrivalSlot;
	int delayFromFirstScheduling = arrivalSlot - proposedArrivalTime;
	int delayDelta = Math.max(delayFromFirstScheduling-f.gate_perturbation,0);
	f.atcGroundDelay = delayFromFirstScheduling; 
	//f.departureDelayFromArrivalAirport = delayDelta;
	int addons = f.taxi_perturbation + f.gate_perturbation + f.taxi_unimpeded_time;
	int pushbackTime = f.departureTimeProposed + /*delayDelta*/ /*delayFromFirstScheduling + f.gate_perturbation + f.taxi_unimpeded_time; //add in taxi??
	//int wheelsOffTime =  f.departureTimeProposed + f.departureDelayFromArrivalAirport + f.taxi_unimpeded_time; //add in taxi??
	int wheelsOffTime =  pushbackTime; //+ f.taxi_perturbation;
	f.wheelsOffTime = wheelsOffTime;
	int lastOnTimeDeparturePoint = arrivalSlot - fastestDuration;
	if (wheelsOffTime > lastOnTimeDeparturePoint){
		System.err.println("err in idealic scheduling");
		schedulingQueue.add(new SchedulingEvent(lastOnTimeDeparturePoint, arrivalSlot, ScheduleMode.remove, f));//???? DOES THIS MAKE SENSE???<<
		schedulingQueue.add(new SchedulingEvent(event.eventTime + addons, -4, ScheduleMode.scheduleInTheAir, f));//???? DOES THIS MAKE SENSE???<<
	} else {
		f.arrivalTimeFinal = arrivalSlot;
	}
}
//*/
/*
public void FCFSArrWithNoGateUncertainty(SchedulingEvent event)
{
	Flight f = event.flight;
	//duration speeds
	int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
	int fastestDuration =  (int)(nominalDuration/(1+speedUp));	
	int proposedArrivalTime = f.arrivalTimeProposed + f.gate_perturbation + f.taxi_unimpeded_time;
	airports.schedulePackedArrival(f, proposedArrivalTime, event.eventTime);
	int pushback = f.departureTimeProposed + f.gate_perturbation + f.atcGroundDelay;
	int wheelsOffTime = pushback + f.taxi_unimpeded_time;// + f.taxi_perturbation; //add in taxi??
	f.wheelsOffTime = wheelsOffTime;
	int lastOnTimeDeparturePoint = f.arrivalTimeFinal - fastestDuration;
	if (wheelsOffTime > lastOnTimeDeparturePoint){
		System.err.println("err in idealic scheduling");
	} else {
		//f.arrivalTimeFinal = arrivalSlot;
	}
}	
 */

/*
public void scheduleInTheAir(SchedulingEvent event)// for case 1 and 2
{
	Flight f = event.flight;
	//duration speeds
	int nominalDuration = f.arrivalTimeProposed-f.departureTimeProposed;
	int fastestDuration =  (int)(nominalDuration/(1+speedUp));
	f.rescheduled = true;
	int targetArrival = f.wheelsOffTime + nominalDuration;
	int finalArrivalSlot = airports.scheduleArrival(f.arrivalAirport, targetArrival);// event.targetTime);
	int airDelay = finalArrivalSlot - targetArrival;
	f.atcAirDelay = airDelay;	
	f.arrivalTimeFinal = finalArrivalSlot;
}
 */
/*
public void remove(SchedulingEvent event) //case 1 and 2
{
	Flight f = event.flight;
	airports.removeFlightFromArrivalQueue(f.arrivalAirport, event.targetTime);

}
 */