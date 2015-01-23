package fcfs;

import java.io.*;
import java.util.*;

//@author Huu Huynh
public class Centers {
	

	Hashtable<String, CenterTree> centerList = new Hashtable<String, CenterTree>();

	 
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
				  // centerid(0), capcity(1),
				  subs = line.split(",");
				  int cap = Integer.parseInt(subs[1]);
				  if( subs.length == 2){  
					  
					  /*if (subs[0].equals("ZFW")){
						  cap = 31;
					  }*/
					  //cap=100;
					  centerList.put(subs[0], new CenterTree(subs[0], cap));
				  } 
			  }
			  in.close();

		}catch (Exception e){
			System.out.println(subs[0]);
			System.err.println("center load Error: " + e.getMessage());
			e.printStackTrace();
		}

	}

	public void printCentersTransit(BufferedWriter out) {
		for (CenterTree c : centerList.values()) {
			try {
				
				c.printTrafficToFile(out);
			}catch (Exception e){//Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}
		}
	}
	
	public void printCenters(){ 
		for (CenterTree c : centerList.values()){
			c.print();
		}
	}
/*	
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
	*/
	public int getSoonestSlot(String centerName, int enterTime, int exitTime){
		CenterTree c = centerList.get(centerName);
		if(c == null){
			c = new CenterTree(centerName);
			centerList.put(centerName, c);
		}
		return c.getSoonestSlot(enterTime, exitTime);
	}
	
	public int schedule(String centerName, int enterTime, int exitTime){
		CenterTree c = centerList.get(centerName);
		if(c == null){
			c = new CenterTree(centerName);
			centerList.put(centerName, c);
		}
		return c.insertAtSoonestSlot(enterTime, exitTime);
	}
	

	
}

