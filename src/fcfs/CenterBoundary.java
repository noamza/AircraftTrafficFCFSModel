/* *
 * @author hvhuynh
 * */

package fcfs;

import java.io.BufferedWriter;
import java.io.PrintStream;
import java.util.*;


public class CenterBoundary {

	String CenterBoundaryName = "";
	int centerBoundarySpacing = 1000; //1 second
	
	
	TreeSet<Integer> CenterBoundaryScheduledTraffic = new TreeSet<Integer>();
	TreeSet<Integer> CenterBoundaryActualTraffic = new TreeSet<Integer>();
	
	public CenterBoundary(String name){
		CenterBoundaryName = name;
	}
	
	public int getSoonestEntrySlot(int entryTime){
		Integer before, previousSpace, currentSpace, after;
		while(true){
			before = CenterBoundaryActualTraffic.floor(entryTime);
			before = before != null? before : Integer.MIN_VALUE;
			
			after = CenterBoundaryActualTraffic.ceiling(entryTime);
			after = after!=null? after : Integer.MAX_VALUE;
			
			//spacing based on entries per time
			currentSpace =  centerBoundarySpacing;
			previousSpace = centerBoundarySpacing;
			
			//ensures entry time will be spaced out between any two flights.
			if( before + previousSpace <= entryTime && entryTime + currentSpace <= after){
				//System.out.printf(" before: %d after: %d ",before, after);
				return entryTime;
			}
			//first tries to set entry time at the space after last flight.
			if(before + previousSpace > entryTime){
				entryTime = before + previousSpace;
			//otherwise at space after next flight
			} else {
				entryTime = after + centerBoundarySpacing;
			}
		}
	}
	
	public int insertAtSoonestCenterBoundary(int entryTime, int scheduledEntryTime){
		int soonest = getSoonestEntrySlot(entryTime);
		CenterBoundaryActualTraffic.add(soonest);
		CenterBoundaryScheduledTraffic.add(scheduledEntryTime);
		return soonest;
	}
}
