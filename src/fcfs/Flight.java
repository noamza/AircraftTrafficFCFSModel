package fcfs;

import java.io.PrintStream;
import java.util.*;



public 	class Flight implements Comparable<Flight>{
	static double toMinutes = 60*1000.0;
	
	ArrayList<SectorAirport> path = new ArrayList<SectorAirport>();
	static PrintStream io = System.out;
	
	int id = -1;
	int departureTimeProposed = -1;//proposed times loaded by ACES/ASDI
	int arrivalTimeProposed = -1;//proposed times loaded by ACES/ASDI
	int taxi_unimpeded_time = AirportTree.DEFAULT_UNIMPEDED_TAXI_TIME*60000; //from gano
	String arrivalAirport = "undef arr";
	String departureAirport  = "undef dep";
	
	int gate_perturbation = 0; // based on ASPM data, always positive
	int taxi_perturbation = 0; // based on ASPM data, always positive
	
	//change each run.
	int atcAirDelay = 0;//air delay
	int atcGroundDelay = 0;//ground delay	
	int wheelsOffTime = -1; //wheels off time
	int arrivalFirstSlot = -1; //original slot, if flight misses this slot, we want to know how much it misses it by
	int departureTimeFinal = -1;
	int arrivalTimeFinal = -1; //wheels on time
	int arrivalAirportDelay = -1;
	int departureAirportDelay = -1;
	
	//for debugging
	int numberOfJiggles = 0; //number of times a flights departure and or arrival times are modified
	int totalJiggleAmount = 0;// in millisecs
	int gateUncertainty = 0;
	int additionalGDfromReschedule = 0;
	boolean rescheduled = false;
	int originalJiggle = 0;
	int uncertaintyMinusGroundDelay = 0; //uncertaintyMinusGroundDelay + atcAirDelay
	
	String airline = "unknown";
	
	

	public void resetValuesNotPerturbations(){
		 atcAirDelay = 0;
		 atcGroundDelay = 0;
		 wheelsOffTime = -1;
		 arrivalTimeFinal = -1;
		 arrivalFirstSlot = -1;
		 numberOfJiggles = 0; //number of times a flights departure and or arrival times are modified
		 totalJiggleAmount = 0;// in millisecs
		 gateUncertainty = 0;
		 additionalGDfromReschedule = 0;
		 originalJiggle = 0;
		 rescheduled = false;
		 uncertaintyMinusGroundDelay = 0;
	}

	public void resetValues(){
		 gate_perturbation = 0;
		 taxi_perturbation = 0;
		 taxi_unimpeded_time = AirportTree.DEFAULT_UNIMPEDED_TAXI_TIME*60000; //right?
		 resetValuesNotPerturbations();
	}
	

	
	public static Flight dummyArrival(int arrivalTime){
		Flight f = new Flight(-1);
		f.arrivalTimeFinal = arrivalTime;
		return f;
	}	
	
	public boolean validateFCFS(){
		boolean valid = true;
		if(id<0){Main.p("ERROR " + id + " is invalid! id<0"); printVariables(); valid =  false;}
		if(departureTimeProposed<0){Main.p("ERROR " + id + " is invalid! departureTimeProposed<0"); printVariables(); valid =  false;}
		//if(atcAirDelay<0){Main.p("ERROR " + id + " is invalid! "); printVariables(); valid =  false;}
		if(atcGroundDelay<0){Main.p("ERROR " + id + " is invalid! airDelayFromArrivalAirport<0"); printVariables(); valid =  false;}		
		if(departureTimeFinal<0){Main.p("ERROR " + id + " is invalid! taxi_unimpeded_time<0"); printVariables(); valid =  false;}
		if(departureTimeProposed>departureTimeFinal){Main.p("ERROR " + id + " is invalid! departureTimeProposed>departureTimeFinal"); printVariables(); valid =  false;}
		if(arrivalTimeProposed>arrivalTimeFinal){Main.p("ERROR " + id + " is invalid! arrivalTimeProposed>arrivalTimeFinal"); printVariables(); valid =  false;}
		if(arrivalTimeFinal<=departureTimeFinal){Main.p("ERROR " + id + " is invalid! arrivalTimeFinal<=departureTimeFinal"); printVariables(); valid =  false;}
		if(arrivalTimeProposed<=departureTimeProposed){Main.p("ERROR " + id + " is invalid! arrivalTimeProposed<=departureTimeProposed"); printVariables(); valid =  false;}
		if(departureTimeProposed+atcGroundDelay!=departureTimeFinal){
			Main.p("ERROR " + id + " is invalid! departureTimeProposed+atcGroundDelay!=departureTimeFinal");printVariables(); valid =  false;}
		//if(arrivalTimeProposed+atcGroundDelay!=arrivalTimeFinal){
			//Main.p("ERROR " + id + " is invalid! arrivalTimeProposed+atcGroundDelay!=arrivalTimeFinal");printVariables(); valid =  false;}
		//Main.p("here");
		//if(arrivalTimeFinal - arrivalTimeProposed != atcGroundDelay){Main.p("good news");}
		//if(id==36788){Main.p(arrivalTimeFinal - arrivalTimeProposed +" "+ atcGroundDelay);}
		
		return valid;
	}
	
	public boolean validate(){
		boolean valid = true;
		if(id<0){Main.p("ERROR " + id + " is invalid! id<0"); printVariables(); valid =  false;}
		if(departureTimeProposed<0){Main.p("ERROR " + id + " is invalid! departureTimeProposed<0"); printVariables(); valid =  false;}
		if(atcAirDelay<0){Main.p("ERROR " + id + " is invalid! "); printVariables(); valid =  false;}
		if(atcGroundDelay<0){Main.p("ERROR " + id + " is invalid! airDelayFromArrivalAirport<0"); printVariables(); valid =  false;}
		if(gate_perturbation<0){Main.p("ERROR " + id + " is invalid! gate_perturbation<0"); printVariables(); valid =  false;}
		if(taxi_perturbation<0){Main.p("ERROR " + id + " is invalid! taxi_perturbation<0"); printVariables(); valid =  false;}
		if(taxi_unimpeded_time<0){Main.p("ERROR " + id + " is invalid! "); printVariables(); valid =  false;}
		if(wheelsOffTime<0){Main.p("ERROR " + id + " is invalid! taxi_unimpeded_time<0"); printVariables(); valid =  false;}
		if(departureTimeProposed>wheelsOffTime){Main.p("ERROR " + id + " is invalid! departureTimeProposed>departureTimeFinal"); printVariables(); valid =  false;}
		if(arrivalTimeProposed>arrivalTimeFinal){Main.p("ERROR " + id + " is invalid! arrivalTimeProposed>arrivalTimeFinal"); printVariables(); valid =  false;}
		if(arrivalTimeFinal<=wheelsOffTime){Main.p("ERROR " + id + " is invalid! arrivalTimeFinal<=departureTimeFinal"); printVariables(); valid =  false;}
		if(arrivalTimeProposed<=departureTimeProposed){Main.p("ERROR " + id + " is invalid! arrivalTimeProposed<=departureTimeProposed"); printVariables(); valid =  false;}
		if(wheelsOffTime<departureTimeProposed+taxi_unimpeded_time+gate_perturbation){ //taxi_perturbation
			Main.p("ERROR " + id + " is invalid! wheelsOffTime<departureTimeProposed+taxi_perturbation+taxi_unimpeded_time+gate_perturbation"); printVariables(); valid =  false;
		}
		if(arrivalFirstSlot<=0){
			Main.p("ERROR " + id + " arrivalFirstSlot is invalid! arrivalFirstSlot<=0"); printVariables(); valid =  false;}
		if(arrivalFirstSlot>arrivalTimeFinal){
			Main.p("ERROR " + id + " arrivalFirstSlot is invalid! arrivalFirstSlot>arrivalTimeFinal"); printVariables(); valid =  false;}
		
		//3 different possible outcomes for wheels on - first slot 
		if(arrivalTimeFinal-arrivalFirstSlot !=  totalJiggleAmount // makes it on first time schedule
	    && arrivalTimeFinal-arrivalFirstSlot !=  totalJiggleAmount + taxi_perturbation + originalJiggle + additionalGDfromReschedule  // no contract, jiggle uncertainty rescheduling
	    && arrivalTimeFinal-arrivalFirstSlot !=  totalJiggleAmount + taxi_perturbation + originalJiggle + additionalGDfromReschedule + uncertaintyMinusGroundDelay  
	    && arrivalTimeFinal-arrivalFirstSlot !=  totalJiggleAmount + taxi_perturbation + originalJiggle + additionalGDfromReschedule + uncertaintyMinusGroundDelay + atcAirDelay  
		&& arrivalTimeFinal-arrivalFirstSlot !=  totalJiggleAmount + taxi_perturbation + originalJiggle + additionalGDfromReschedule +                               atcAirDelay
		&& arrivalTimeFinal-arrivalFirstSlot !=   					 taxi_perturbation +											   uncertaintyMinusGroundDelay + atcAirDelay //contract jiggle uncertainty
		&& arrivalTimeFinal-arrivalFirstSlot !=                      taxi_perturbation +         																	 atcAirDelay //contract

//		&& arrivalTimeFinal-arrivalFirstSlot !=  					 taxi_perturbation + gateUncertainty - atcGroundDelay + 2*additionalGDfromReschedule + totalJiggleAmount + originalJiggle 
//		&& arrivalTimeFinal-arrivalFirstSlot !=  					taxi_perturbation + gateUncertainty - atcGroundDelay + 2*originalJiggle + totalJiggleAmount + additionalGDfromReschedule
//		&& arrivalTimeFinal-arrivalFirstSlot != atcAirDelay
				){
			Main.p("ERROR " + id + " wheels on - arrivalFirstSlot = " + (arrivalTimeFinal-arrivalFirstSlot)/toMinutes); printVariables(); valid =  false;}
		
		return valid;
	}
	
	void printVariables(){
		PrintStream o = System.out;
		o.println("[] ");
		o.println("id " + id + " " + arrivalAirport + " to " + departureAirport);
		o.println("departureTimeProposed " + departureTimeProposed/toMinutes);
		o.println("departureTimeFinal " + departureTimeFinal/toMinutes);
		o.println("arrivalTimeProposed " + arrivalTimeProposed/toMinutes);
		o.println("wheelsOffTime " + wheelsOffTime/toMinutes);
		o.println("arrivalTimeFinal " + arrivalTimeFinal/toMinutes);
		o.println("arrivalFirstSlot " + arrivalFirstSlot/toMinutes);
		o.println("gate_perturbation " + gate_perturbation/toMinutes);
		o.println("taxi_unimpeded_time " + taxi_unimpeded_time/toMinutes);
		o.println("taxi_perturbation " + taxi_perturbation/toMinutes);
		o.println("gateUncertainty " + gateUncertainty/toMinutes);
		o.println("uncertaintyMinusGroundDelay " + uncertaintyMinusGroundDelay/toMinutes);
		o.println("atcAirDelay " + atcAirDelay/toMinutes);
		o.println("atcGroundDelay " + atcGroundDelay/toMinutes);
		o.println("numberOfJiggles " + numberOfJiggles);
		o.println("totalJiggleAmount " + totalJiggleAmount/toMinutes);
		o.println("additionalGDfromReschedule " + additionalGDfromReschedule/toMinutes);
		o.println("originalJiggle " + originalJiggle/toMinutes);
		o.println("rescheduled " + rescheduled);
		o.println("[][][][]");
	}
	
	
	public Flight(int i){
		id = i;
	}
	
	public void addDelay(int d){
		atcGroundDelay = d;
		departureTimeProposed += d;
		arrivalTimeProposed += d;
		
		for(SectorAirport s: path){
			s.entryTime += d;
		}
		
	}
	
	//changes all variables back to their values
	//before simulation
	
	
	
	public void correctForTaxiOffset(int d){
		//540000 is the biggest offset, this ensures we don't have negative starting value
		//(it is specific to this dataset only)
		d -= 540000; // CHANGE AND REMOVE THIS MAGIC NUMBER
		departureTimeProposed -= d;
		arrivalTimeProposed -= d;
		for(SectorAirport s: path){
			s.entryTime -= d;
		}
	}
	
	//millisec
	public void pushFlightForwardInTime(int d){
		departureTimeProposed += d;
		arrivalTimeProposed += d;
		for(SectorAirport s: path){
			s.entryTime += d;
		}
	}
	
	void print(){
		io.printf("ID %d delay %d dep %d arr %d: %s", id, atcGroundDelay, departureTimeProposed, arrivalTimeProposed, departureAirport);
		for(SectorAirport s: path){
			io.print(" " + s.name);
		}
		io.println(" " + arrivalAirport);
	}
	
	void printFull(){
		io.printf("ID: %d Delay: %d\ndepAairport %s depTime %d\n", id, atcGroundDelay, departureAirport, departureTimeProposed/1000);
		io.println("path:");
		for(SectorAirport s: path){
			io.printf("name: %s entryTime: %d transitTime: %d raw: %s\n", s.name, s.entryTime/1000, s.transitTime/1000, s.raw );
		}
		io.println("arrAirport:" + arrivalAirport + " " + " arrTime: " + arrivalTimeProposed/1000 + "\n");
	}

	@Override
	//sorts by departure time.
	public int compareTo(Flight o) {
		return (departureTimeProposed-o.departureTimeProposed);
	}
	
}

class flightFinalArrTimeComparator implements Comparator<Flight>{
	public int compare(Flight f1, Flight f2){
		return f1.arrivalTimeFinal - f2.arrivalTimeFinal;
	}
}


class flightIDComparator implements Comparator<Flight>{
	public int compare(Flight f1, Flight f2){
		return f1.id - f2.id;
	}
}

class flightDepTimeComparator implements Comparator<Flight>{
	public int compare(Flight f1, Flight f2){
		return f1.departureTimeProposed - f2.departureTimeProposed;
	}
}

class flightGateTaxiUnimDepComparator implements Comparator<Flight>{
	public int compare(Flight f1, Flight f2){
		return (f1.departureTimeProposed+f1.gate_perturbation+f1.taxi_unimpeded_time) - 
			   (f2.departureTimeProposed+f2.gate_perturbation+f2.taxi_unimpeded_time);
	}
}

class SectorAirport{
	String raw;
	String name;
	int entryTime;
	int transitTime;
	//int scheduledEntryTime;
	SectorAirport(String n, int e, int s){
		name = n; entryTime = e; transitTime = s;
	}
	SectorAirport(String n, int e, int s, String r){
		name = n; entryTime = e; transitTime = s; raw = r;
	}
}
