package fcfs;
import java.io.*;
public class FileWrite {

	public void writeToCsv(Flights f, String nameDir){
		
		try{
			  // Create file 
			  FileWriter fstream = new FileWriter(nameDir);
			  BufferedWriter out = new BufferedWriter(fstream);
			  
			  
			  
			  out.write("Hello Java");
			  //Close the output stream
			  out.close();
			  }catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
			  }
		
	}
	
	
}
