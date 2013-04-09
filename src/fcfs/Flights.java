/**
 * 
 */
package fcfs;

/**
 * @author Noam Almog
 *
 */
import java.io.*;
import java.util.*;

public class Flights {
	
	PrintStream io = System.out;
	int ACES_FDS_OFFSET = 3*3600000;//3 hours
	Hashtable<Integer, Flight> flightList;
	//Map<Integer,Flight> f = new Map<Integer, Flight>();
	Hashtable<String, Integer> taxiOffset; //in millisecs
	
	public Flights(){
		flightList = new Hashtable<Integer, Flight>();
		taxiOffset = new Hashtable<String, Integer>();
	}
	
	
	public void loadCallSigns(String path){
		try{
			//Read callsigns in
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			String[] subs = new String[1];
			int flightId;
			String airline;
			br.readLine();
			while ((line = br.readLine()) != null){
				line = line.replaceAll("\\s","");//trim();
				// location(0), sim day(1), hour(2), quarter(3), rates(4)
				subs = line.split(",");
				flightId = Integer.parseInt(subs[0]);
				airline = subs[1].substring(0, 3);
				//Main.p(airline + " " + flightId);
				if(flightList.get(flightId) != null){
					flightList.get(flightId).airline = airline;
				}
			}
			in.close();

		}catch (Exception e){
			System.err.println("call sign load Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void loadTaxiOffset(String path){
		try{
			//Read ACES Transit Time File Line by Line
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			String[] subs = new String[1];
			String[] os = new String[1];
			String airportName;
			int taxiOffSet;
			br.readLine();
			while ((line = br.readLine()) != null){
				line = line.replaceAll("\\s","");//trim();
				// location(0), sim day(1), hour(2), quarter(3), rates(4)
				subs = line.split(",");
				airportName = subs[0];
				os = subs[1].split(":");
				//Main.p("os " + os[0]);
				taxiOffSet = Integer.parseInt(os[1])*60000 + Integer.parseInt(os[2])*1000;
				//System.out.printf("test os    %s %d\n", airportName,taxiOffSet);
				taxiOffset.put(airportName, taxiOffSet);
			}
			in.close();

		}catch (Exception e){
			System.err.println("taxi offset load Error: " + e.getMessage());
			e.printStackTrace();
		}
		
		for (Flight f: flightList.values()){
			//Main.p("be " + f.departureTimeProposed + " " + f.arrivalTimeProposed );
			int a = f.departureTimeProposed, b = f.arrivalTimeProposed;
			if(taxiOffset.get(f.departureAirport)!=null){
					f.correctForTaxiOffset(taxiOffset.get(f.departureAirport));
					//check for correctness
					if(a - taxiOffset.get(f.departureAirport)+ 540000 != f.departureTimeProposed ){Main.p("ERROR in to");}
					if(b - taxiOffset.get(f.departureAirport)+ 540000 != f.arrivalTimeProposed ){Main.p("ERROR in to");}
			} else {
				//corrects default of 10 minutes
				f.correctForTaxiOffset(600000);
				//check for correctness
				if(a - 600000 + 540000 != f.departureTimeProposed ){Main.p("ERROR in to d");}
				if(b - 600000 + 540000 != f.arrivalTimeProposed){Main.p("ERROR in to d");}
			}
			//Main.p("a " + f.departureTimeProposed + " " + f.arrivalTimeProposed + " " + );
			
		}
	}
	
	public void pushFlightsForwardBy1hr(int maxoffset){
		for (Flight f: flightList.values()){
			f.pushFlightForwardInTime(maxoffset);
		}
	}
	 
	public void loadFlightsFromAces(String filePath, boolean loadSectors){
		//Hashtable<Integer, Integer> test = new Hashtable<Integer, Integer>();
		String[] subs = new String[1];
		int max = 0, inputCount = 0;
		try{
			//Read ACES Transit Time File Line by Line
			  FileInputStream fstream = new FileInputStream(filePath);
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String line;
			  Flight f = new Flight(-1);
			  boolean correctTransitSequence = false;
			  boolean writeSector = true;
			  
			  //skip past comment line
			  br.readLine();
			  while ((line = br.readLine()) != null){
				  // flightid(0), entryTime(1),exitTime(2),transitTime(3),
				  //upperStreamSector(4),currentSector(5),downStreamSector(6)
				  subs = line.split(",");
				  if( subs.length == 7){ //&& !line.startsWith("*")){
					  //inputCount++;
					  //test.put(Integer.parseInt(subs[0]),0);
					  int entryTime = Integer.parseInt(subs[1]) + ACES_FDS_OFFSET;
					  //max = java.lang.Math.max(max, Integer.parseInt(subs[2]));
					  //max = java.lang.Math.max(max, Integer.parseInt(subs[1]));
					  int transitTime = Integer.parseInt(subs[3]) + ACES_FDS_OFFSET;
					  String facilityName = subs[5];
					  writeSector = true;
					  
					  //Main.Assert(entryTime < 2000000000);
					  //check for large times? can't handle times longer than 24 days.
					  
					  if(subs[4].equals("XXXX")){
						  inputCount++;
						  correctTransitSequence = true;
						  //SETS f.departureTimeProposed
						  f = new Flight(Integer.parseInt(subs[0]));
						  f.departureTimeProposed = entryTime; 
						  f.departureAirport = facilityName;
						  //add tracons to list;
						  facilityName = subs[6];
						  //writeSector = false;
					  }

					  if(subs[6].equals("XXXX")){
						  int exitTime = Integer.parseInt(subs[2]) + ACES_FDS_OFFSET;
						  f.arrivalTimeProposed = exitTime;
						  correctTransitSequence = false;
						  f.arrivalAirport = facilityName;
						  //add tracons to list
						  facilityName = subs[4];
						  //f.path.add(new SectorAirport(facilityName, entryTime, transitTime));
						  f.path.add(new SectorAirport(facilityName, entryTime, transitTime, subs[4]+","+subs[5]+","+subs[6]));
						  flightList.put(Integer.parseInt(subs[0]), f);
						  f = null;
					  }
					  
					  if(correctTransitSequence){ 
						  //f.path.add(new SectorAirport(facilityName, entryTime, transitTime));
						  if(loadSectors)f.path.add(new SectorAirport(facilityName, entryTime, transitTime, subs[4]+","+subs[5]+","+subs[6]));
					  } // bad entry else {io.println("bad flightId entry " + subs[0]); }
					  
				  } else {
					  io.println("not 7 " + line);
				  }

			  }
			  in.close();
			  
		}catch (Exception e){
			io.println(subs[0]);
			System.err.println("la Error: " + e.getMessage());
			 e.printStackTrace();
		}
		//io.println(inputCount + " input output " + flightList.size());
		//io.println("maxxx "+max);
		//io.println(test.size() + " total, usable " + flightList.size());
		//io.println(inputCount + " total, usable " + flightList.size());
		//flightList.get(36440).fullPrint();
	}
	
	Flight getFlightByID(int id){
		return flightList.get(id);
	}

	void validate(){
		for (Flight f : flightList.values()){
			f.validate();
		}
	}
	
	void validateFCFS(){
		for (Flight f : flightList.values()){
			f.validateFCFS();
		}
	}
	
	void resetValues(){
		for (Flight f : flightList.values()){
			f.resetValues();
		}
	}
	
	void resetValuesNotPerturbations(){
		for (Flight f : flightList.values()){
			f.resetValuesNotPerturbations();
		}
	}
	
	public Collection<Flight> getFlights(){
		return new ArrayList<Flight>(flightList.values());
	}
	
	public void printFlights(){
		for (Flight f : flightList.values()){
			f.print();
		}
		io.println("total number of flights: " + flightList.size());
	}
	
	public void printFlightsFull(){
		for (Flight f : flightList.values()){
			f.printFull();
		}
	}
	
	
}
