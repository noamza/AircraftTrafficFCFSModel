package preferentialmerge;

import java.io.PrintStream;

public 	class Flight implements Comparable<Flight>{
	
	static PrintStream io = System.out;
	
	String id = "unknown";
	String size = "grande";
	int departureTimeProposed = -1;//proposed times loaded by ACES/ASDI
	int arrivalTimeProposed = -1;//proposed times loaded by ACES/ASDI
	String departureAirport  = "undef dep";	
	String airline = "unknown";
	
	public void resetValues(){
	}
	
	//IMPLEMENT
	public boolean validate(){
		boolean valid = true;
		if(id.equals("unknown")){T.print("ERROR " + id + " is invalid! id<0"); printVariables(); valid =  false;}
		return valid;
	}
	
	void printVariables(){		
		io.println("[] ");
		io.println("id " + id + " from " + departureAirport);
		io.println("departureTimeProposed " + departureTimeProposed/T.toMinutes);
		io.println("arrivalTimeProposed " + arrivalTimeProposed/T.toMinutes);
		io.println("[][][][]");
	}
	
	
	public Flight(String i){
		id = i;
	}
	
	
	void print(){
		io.printf("ID %d  dep %d arr %d: %s", id, departureTimeProposed, arrivalTimeProposed, departureAirport);
		io.println(" " + departureAirport);
	}
	
	void printFull(){
		io.printf("ID: %d depAairport %s depTime %d\n", id, departureAirport, departureTimeProposed/1000);
		io.println("departure airport:" + departureAirport + " " + " arrTime: " + arrivalTimeProposed/1000 + "\n");
	}

	@Override
	//sorts by departure time.
	public int compareTo(Flight o) {
		return (departureTimeProposed-o.departureTimeProposed);
	}
	
}
/*
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
*/
