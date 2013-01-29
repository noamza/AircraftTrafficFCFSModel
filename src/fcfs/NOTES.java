package fcfs;

import java.util.ArrayList;

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
	
	
}
