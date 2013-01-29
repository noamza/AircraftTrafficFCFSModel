/**
 * 
 */
package preferentialmerge;

/**
 * @author Noam Almog
 *
 */
import java.io.*;
import java.util.*;

public class Flights {
	
	PrintStream io = System.out;
	ArrayList<Flight> flightList;
	//Map<Integer,Flight> f = new Map<Integer, Flight>();
	
	public Flights(){
		flightList = new ArrayList<Flight>();
	}
	
	
	public void loadNames(String path){
		try{
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line; 
			String[] subs = new String[1];
			while ((line = br.readLine()) != null){
				line = line.replaceAll("\\s","");//trim();
				subs = line.split(",");
				Flight f = new Flight(subs[0]);
				flightList.add(f);
				//airline = subs[1].substring(0, 3);
			}
			in.close();

		}catch (Exception e){
			System.err.println("call name load Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void loadSizes(String path){
		try{
			FileInputStream fstream = new FileInputStream(path);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			int index = 0;
			while ((line = br.readLine()) != null){
				line = line.replaceAll("\\s","");//trim();
				flightList.get(index).size = line;
				index++;
			}
			in.close();
		}catch (Exception e){
			System.err.println("call size load Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	Flight getFlightByID(int id){
		return flightList.get(id);
	}

	void validate(){
		for (Flight f : flightList){ //.values()
			f.validate();
		}
	}
	
	void resetValues(){
		for (Flight f : flightList){
			f.resetValues();
		}
	}
	
	public Collection<Flight> getFlights(){
		return new ArrayList<Flight>(flightList);
	}
	
	public void printFlights(){
		for (Flight f : flightList){
			f.print();
		}
		io.println("total number of flights: " + flightList.size());
	}
	
	public void printFlightsFull(){
		for (Flight f : flightList){
			f.printFull();
		}
	}
	
	
}
