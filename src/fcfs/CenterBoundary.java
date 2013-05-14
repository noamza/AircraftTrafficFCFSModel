/* *
 * @author hvhuynh
 * */

package fcfs;

import java.io.BufferedWriter;
import java.io.PrintStream;
import java.util.*;


public class CenterBoundary {

	
	String enteringCenter = "";
	String exitingCenter = "";
	String centerBoundaryName = "";
	//int centerBoundarySpacing = 5000; //5 second
	int centerBoundarySpacing = 5000;
	
	TreeSet<Integer> CenterBoundaryScheduledTraffic = new TreeSet<Integer>();
	TreeSet<Integer> CenterBoundaryActualTraffic = new TreeSet<Integer>();
	
	TreeSet<CenterTransit> CenterTransitBoundaryActualTraffic = new TreeSet<CenterTransit>(new centerBoundaryFinalEntryTimeComparator());
	
	public CenterBoundary(String boundaryName, String exitName, String enterName){
		exitingCenter = exitName;
		enteringCenter = enterName;
		centerBoundaryName = boundaryName;
	}
	public CenterBoundary(String exitName, String enterName){
		exitingCenter = exitName;
		enteringCenter = enterName;
		centerBoundaryName = exitName + "->" + enterName;
	}
	
	public void removeFromSchedule(CenterTransit ct) {
		CenterTransitBoundaryActualTraffic.remove(ct);
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
	
	//using center transit objects so we can alter schedule by specific objects
	public int getSoonestEntrySlot(CenterTransit ct, int entryTime){
		Integer before, previousSpace, currentSpace, after;
		while(true){
			CenterTransit tempBefore = CenterTransitBoundaryActualTraffic.floor(ct);
			if (tempBefore == null) { before = Integer.MIN_VALUE; }
			else { before = tempBefore.finalEntryTime; }
			
			CenterTransit tempAfter = CenterTransitBoundaryActualTraffic.ceiling(ct);
			if (tempAfter == null) { after = Integer.MAX_VALUE; }
			else { after = tempAfter.finalEntryTime; }
			
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
				ct.proposedEntryTime = entryTime;
				ct.finalEntryTime = entryTime;
			//otherwise at space after next flight
			} else {
				entryTime = after + centerBoundarySpacing;
				ct.proposedEntryTime = entryTime;
				ct.finalEntryTime = entryTime;
			}
		}
	}
		
	
	public int insertAtSoonestCenterBoundary(int entryTime, int scheduledEntryTime){
		int soonest = getSoonestEntrySlot(entryTime);
		CenterBoundaryActualTraffic.add(soonest);
		CenterBoundaryScheduledTraffic.add(scheduledEntryTime);
		return soonest;
	}
	
	public int insertAtSoonestCenterBoundary(CenterTransit ct){
		int soonest = getSoonestEntrySlot(ct, ct.proposedEntryTime);
		ct.finalEntryTime = soonest;
		CenterTransitBoundaryActualTraffic.add(ct);
		CenterBoundaryScheduledTraffic.add(ct.entryTime);
		return soonest;
	}
	
}
