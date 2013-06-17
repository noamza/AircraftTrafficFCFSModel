package fcfs;

/**
 * @author Noam Almog
 *
 */
import java.io.*;
import java.util.*;

public class Sectors {
	
	PrintStream io = System.out;
	//make smaller??
	Hashtable<String, SectorTree> sectorList = new Hashtable<String, SectorTree>();
	
	//Hashtable<String, SectorTree>() = ;
	//could make smaller <<

	/**
	 * Build a map of sectors and 
	 */
	@Deprecated
	public Sectors()
	{
	}
	
	public Sectors(Map<String, Integer> sectors)
	{
		for (Map.Entry<String, Integer> entry : sectors.entrySet())
		{
			sectorList.put(entry.getKey(), new SectorTree(entry.getKey(), entry.getValue()));
		}
	}
	 
	public void loadFromAces(String filePath){
		String[] subs = new String[1];
		try{
			
			  FileInputStream fstream = new FileInputStream(filePath);
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String line;
			  //skip past comment line
			  br.readLine();
			  while ((line = br.readLine()) != null){
				  // sectorid(0), capcity(1),
				  subs = line.split(",");
				  int cap = Integer.parseInt(subs[1]);
				  if( subs.length == 2){ //&& !line.startsWith("*")){
					  //f = new SectorTree(subs[0], Integer.parseInt(subs[1]));  
					  sectorList.put(subs[0], new SectorTree(subs[0], cap));
				  } else {
					  io.println("not 7 " + line);
				  } 
			  }
			  in.close();

		}catch (Exception e){
			io.println(subs[0]);
			System.err.println("sector load Error: " + e.getMessage());
			e.printStackTrace();
		}

	}

	public void printSectors(){ 
		for (SectorTree s : sectorList.values()){
			//io.print(++i + " "); 
			s.print();
		}
	}
	
	public void printSectorsToFile(BufferedWriter out) {
		for (SectorTree s: sectorList.values()) {
			try {
				//out.write("**SectorName" + "," +"MaxCapacity" + "," + "TrafficSize");
				s.printToFile(out);
			}catch (Exception e){//Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}
			
		}
	}
	
	public void printSectorMaxCaps(){ 
		for (SectorTree s : sectorList.values()){
			s.printMaxCapacity();
		}
	}
	
	public int getSoonestSlot(String sectorName, int enterTime, int exitTime){
		SectorTree s = sectorList.get(sectorName);
		if(s == null){
			s = new SectorTree(sectorName);
			sectorList.put(sectorName, s);
		}
		return s.getSoonestSlot(enterTime, exitTime);
	}
	
	public int schedule(String sectorName, int enterTime, int exitTime){
		SectorTree s = sectorList.get(sectorName);
		if(s == null){
			s = new SectorTree(sectorName);
			sectorList.put(sectorName, s);
		}
		return s.insertAtSoonestSlot(enterTime, exitTime);
	}
	

	
	/*
	class Sector{
		String name;
		int maxCapacity = -1;
		Sector(){
		}
		Sector(String n, int max){
			name = n; maxCapacity = max;
		}
		void print(){ 
			io.printf("ID: %s : %d", name, maxCapacity);
			io.println();
		}
	}//*/
	
}

