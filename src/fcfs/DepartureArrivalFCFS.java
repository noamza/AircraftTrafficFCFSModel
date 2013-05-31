/*
 * @author hvhuynh
 * 
 * */

package fcfs;
import java.util.*;
import java.io.*;

public class DepartureArrivalFCFS {

	Flights flights; 
	Airports airports;
	Sectors sectors;
	Centers centers;
	CenterBoundaries centerBoundaries;
	
	public void scheduleFCFS(){

		//double speedUp = 0.025;
		//double slowDown = 0.05;
		double totalGroundDelay = 0;
		double totalAirDelay = 0;
		double totalSectorDelay = 0;
		double totalCenterDelay = 0;
		double totalCenterBoundaryDelay = 0;
		double newTotalAirDelay = 0;
		double totalAllDelay = 0;
		double MAX_SLOWDOWN = 0.10;
		
		flights = new Flights();
		airports = new Airports();
		sectors = new Sectors();
		centers = new Centers();
		centerBoundaries = new CenterBoundaries();
		
		String workingDirectory = "/Users/hvhuynh/Desktop/scheduler/inputs/";
		String outputdir = "departureArrivalFCFS_output/";
		
		//flights.loadFlightsFromAces(workingDirectory+"job_23_sector_transitTime_takeoffLanding_35h_1.csv", true); // constrained
		flights.loadFlightsFromAces(workingDirectory +"job_24_sector_transitTime_takeoffLanding_35h_1.csv", true); //unconstrained
		//flights.loadFlightsFromAces(workingDirectory +"job_40_sector_transitTime_takeoffLanding_35h_1.csv",true); //constrained
		sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May.csv");
		//sectors.loadFromAces(workingDirectory+"SectorList_YZ2007May_MAP9999.csv");
		centers.loadFromAces(workingDirectory+"centerList_2007_may_9999.csv");
		airports.loadFromAces(workingDirectory+"AdvancedState_Hourly_Runways_AllCapacities_20110103_20110104.csv");
		
		//job 29 is an unconstrained ACES run used to gather center transit data
		flights.loadCenterTransitFromAces(workingDirectory + "centerCrossingV2_job_29_ldc_20130418_160504_20130418_deftfm_j611fid_uncons_5fts.csv");
		
		ArrayList<Flight> flightList = new ArrayList<Flight>(flights.getFlights());
		ArrayList<Flight> arrivingFlightList = new ArrayList<Flight>();
		
		//Sort flights by Departure Time.
		Collections.sort(flightList, new flightDepTimeComparator());
		
		//Schedule Departing Flights
		for (Flight flight: flightList) {
			
			//get soonest time slot the flight can depart
			int departureTimeProposed = airports.getSoonestDeparture(flight.departureAirport, flight.departureTimeScheduled);
			
			//schedule the flight
			int departureTimeFinal = airports.scheduleDeparture(flight.departureAirport, departureTimeProposed, flight.departureTimeScheduled);
			int groundDelay = departureTimeFinal - flight.departureTimeProposed;
			totalGroundDelay += groundDelay;
			flight.atcGroundDelay = groundDelay;
			flight.departureTimeFinal = departureTimeFinal;
			//scheduled arrival time changes when ground delay is taken into account
			flight.arrivalTimeProposed = flight.arrivalTimeScheduled + groundDelay;
			arrivingFlightList.add(flight);
			
			
			//BEGIN SECTOR
			/* calculate sector delay.  TODO: add delay from sectors to arrival = more arrival delay*
			int blockDelay = 0;
			for (SectorAirport sa: flight.path) {
				//if(flight.id == 30448) { System.out.println("this is a good place to stop");}
				int proposedEntryTime = sa.entryTime + groundDelay + blockDelay;
				
				int sectorTimeProposed = sectors.getSoonestSlot(sa.name, proposedEntryTime, proposedEntryTime + sa.transitTime);
				
				//blockDelay += sectorTimeProposed - proposedEntryTime;
				
				int sectorTimeFinal = sectors.schedule(sa.name, sectorTimeProposed, sectorTimeProposed + sa.transitTime);
				blockDelay += sectorTimeFinal - proposedEntryTime;
				
			}
			totalSectorDelay += blockDelay;*/
			//END SECTOR STUFF
			
			//BEGIN CENTER
			
			if (flight.centerPath.size() == 1) {
				continue;//flight only crosses 1 center boundary - from TRACON to center and not from center to center.
			}
			
			int centerBlockDelay = 0;
			int centerBoundaryDelay = 0;
			//sort the path through center boundaries
			Collections.sort(flight.centerPath, new centerBoundaryEntryTimeComparator());
			for (CenterTransit ct: flight.centerPath) {
				
				int scheduledCenterEntryTime = ct.entryTime;
				int scheduledCenterExitTime = ct.exitTime;
				int transitTime = ct.transitTime;
				String facilityName = ct.facilityName;
				String prevFacility = ct.prevFacilityName;
				String centerBoundaryName = prevFacility + "->" + facilityName;
				
				//skip initial Center boundary between TRACON and Center. We only care about Center to Center
				if (prevFacility.startsWith("T") || prevFacility.startsWith("C") || prevFacility.startsWith("N")) {
					continue;
				}
				
				//record center path of flight
				if (flight.centersTravelledPath == null) {
					flight.centersTravelledPath = prevFacility + "->" + facilityName;
					flight.centersTravelled.add(prevFacility);
					flight.centersTravelled.add(facilityName);
				} 
				else {
					flight.centersTravelledPath += ("->" + facilityName);
					flight.centersTravelled.add(facilityName);
				}
				
				/*TODO: incorporate this into the center boundary model later
				//schedule flights through Center
				int proposedCenterEntryTime = centers.getSoonestSlot(facilityName, scheduledCenterEntryTime + flight.atcGroundDelay + centerBlockDelay, scheduledCenterExitTime + flight.atcGroundDelay + centerBlockDelay);
				//int proposedCenterDelay = proposedCenterEntryTime - scheduledCenterEntryTime;
				int finalCenterEntryTime = centers.schedule(facilityName, proposedCenterEntryTime,proposedCenterEntryTime+transitTime);
				int centerDelay = finalCenterEntryTime - proposedCenterEntryTime;
				centerBlockDelay += centerDelay;
				totalCenterDelay += centerDelay;
				flight.centerDelay += centerDelay;
				ct.finalEntryTime = finalCenterEntryTime;
				ct.finalExitTime = finalCenterEntryTime + transitTime;
				*/
				
				
				//TODO: Still need to add delay into the scheduling of center boundaries. Pass more than just the CenterTransit object to the scheduling function.
				//schedule flights through center boundary
				ct.proposedEntryTime = ct.entryTime + flight.atcGroundDelay + centerBoundaryDelay;
				ct.finalEntryTime = ct.proposedEntryTime; //need this to sort Center Transit objects correctly.  Gets overridden by real finalEntryTime later on.
				int proposedCenterBoundaryEntryTime = centerBoundaries.getSoonestSlot(centerBoundaryName, ct);
				//int proposedCenterBoundaryEntryTime = centerBoundaries.getSoonestProposedSlot(centerBoundaryName, prevFacility, facilityName, scheduledCenterEntryTime + flight.atcGroundDelay + centerBoundaryDelay);
				ct.proposedEntryTime = proposedCenterBoundaryEntryTime;
				int finalProposedCenterBoundaryEntryTime = centerBoundaries.schedule(centerBoundaryName, ct);
				
				
				//int finalProposedCenterBoundaryEntryTime = centerBoundaries.scheduleProposed(centerBoundaryName, prevFacility, facilityName, proposedCenterBoundaryEntryTime, scheduledCenterEntryTime);
				ct.finalEntryTime = finalProposedCenterBoundaryEntryTime;
				centerBoundaryDelay = (ct.finalEntryTime - (ct.entryTime + flight.atcGroundDelay + centerBoundaryDelay));
				ct.delay = centerBoundaryDelay;
				flight.centerBoundaryDelay += centerBoundaryDelay;
				//System.out.println(centerBoundaryName + " " + flight.id + " : " + centerBoundaryDelay);
				ct.flightid = flight.id;
				
			}//END CENTER STUFF
			
			//System.out.println(flight.centersTravelledPath + " " + flight.id);
			
		}//end departures

		//validate departure traffic spacing at airports.
		airports.validateDepartureTraffic();
		
		//Sort flights by proposed arrival time.
		Collections.sort(arrivingFlightList, new flightArrTimeComparator());
		
		//Schedule Arriving Flights
		for (Flight flight: arrivingFlightList) {
			
			//get soonest time slot the flight can land
			int arrivalTimeProposed = airports.getSoonestArrival(flight.arrivalAirport, flight.arrivalTimeProposed + flight.centerBoundaryDelay);
			//schedule the flight
			int airDelay = arrivalTimeProposed - flight.arrivalTimeProposed;
			
			//if (airDelay != 0) {System.out.println(airDelay);}
			
			//absorb delay in air
			if (flight.centerPath.size() == 1) {
				int arrivalTimeFinal = airports.scheduleArrival(flight.arrivalAirport, arrivalTimeProposed, flight.arrivalTimeScheduled);
				flight.arrivalTimeFinal = arrivalTimeFinal;
				airDelay = arrivalTimeFinal - flight.arrivalTimeProposed;
				flight.atcAirDelay = airDelay;
				totalAirDelay += airDelay;
				totalAllDelay += (flight.arrivalTimeFinal - flight.arrivalTimeScheduled);
			}
			
			//absorb delay at boundaries
			else {
				//reverse path of flight through center boundaries. we want to disperse air delay starting from the boundary closest to the airport
				int delayPassedBack = 0;
				int leftOverDelay = 0;
				Collections.reverse(flight.centerPath);
				int delayToAbsorb = airDelay;
				while (delayToAbsorb > 0) {
					for (CenterTransit ct: flight.centerPath) {
						int scheduledCenterEntryTime = ct.entryTime;
						int scheduledCenterExitTime = ct.exitTime;
						int transitTime = scheduledCenterExitTime - scheduledCenterEntryTime;
						int proposedEntryTime = ct.proposedEntryTime;
						String facilityName = ct.facilityName;
						String prevFacility = ct.prevFacilityName;
						String centerBoundaryName = prevFacility + "->" + facilityName;
						
						//skip initial Center boundary between TRACON and Center. We only care about Center to Center
						if (facilityName.startsWith("T") || facilityName.startsWith("C") || facilityName.startsWith("N")) {
							continue;
						}
						
						double max_absorbable_delay = (MAX_SLOWDOWN * transitTime);
						
						if (max_absorbable_delay > delayToAbsorb) {
							max_absorbable_delay = delayToAbsorb;
							delayToAbsorb = 0;
						}
						else {
							delayToAbsorb -= max_absorbable_delay;
						}
						//remove from schedule to re-schedule with added air delay
						centerBoundaries.removeFromSchedule(ct, centerBoundaryName);
						ct.proposedEntryTime = (int) (ct.finalEntryTime + max_absorbable_delay);
						ct.finalEntryTime = ct.proposedEntryTime;
						int proposedBoundaryCrossingTime = centerBoundaries.getSoonestSlot(centerBoundaryName, ct);
						ct.proposedEntryTime = proposedBoundaryCrossingTime;
						int finalCenterBoundaryCrossingTime = centerBoundaries.schedule(centerBoundaryName, ct);
						//ct.proposedEntryTime += airDelay;
						ct.finalEntryTime = finalCenterBoundaryCrossingTime;
						delayPassedBack += (ct.finalEntryTime - proposedEntryTime);
					}
					if (delayToAbsorb > 0) {
						//System.out.println(delayToAbsorb);
						leftOverDelay = delayToAbsorb;
						delayToAbsorb = 0;
					}
				}
				//if (delayPassedBack > 0) System.out.println(delayPassedBack);
				//re-check for arrival time with new center boundary delays
				int arrivalTimeProposed_new = airports.getSoonestArrival(flight.arrivalAirport ,flight.arrivalTimeProposed + leftOverDelay);
				
				int newAirDelay = arrivalTimeProposed_new - (flight.arrivalTimeProposed + leftOverDelay);
				//System.out.println(newAirDelay);
				newTotalAirDelay+=newAirDelay;
				int arrivalTimeFinal = airports.scheduleArrival(flight.arrivalAirport, arrivalTimeProposed_new, flight.arrivalTimeScheduled, flight);
				flight.arrivalTimeFinal = arrivalTimeFinal;
				totalAllDelay += (flight.arrivalTimeFinal - flight.arrivalTimeScheduled);
				//flight.atcAirDelay = airDelay;
				//totalAirDelay += airDelay;
				totalCenterBoundaryDelay += flight.centerBoundaryDelay;
			}
			
		}
		System.out.println("Total Of All Delays :" + totalAllDelay/3600000);
		System.out.println("Total new Air Delay: " + newTotalAirDelay/3600000);
		//validate arrival traffic spacing at airports.
		airports.validateArrivalTraffic();
		//validate individual flights by checking departure/arrival times
		flights.validateFCFS();
		
		//centers.printCenters();
		
		System.out.println("Total Ground Delay = " + totalGroundDelay/3600000);
		System.out.println("Total Air Delay = " + totalAirDelay/3600000);
		//System.out.println("Total Delay in sectors = " + totalSectorDelay/3600000);
		System.out.println("Total Center Boundary Delay = " + totalCenterBoundaryDelay/3600000);
		System.out.println("Total Delay = " + (totalGroundDelay+totalAirDelay+totalCenterBoundaryDelay)/3600000);
		System.out.println("Total Flights Flown = " + arrivingFlightList.size());
			
		//printSectorTraffic(sectors, workingDirectory+outputdir);
		
		printAirportDelays(flightList, workingDirectory+outputdir);
		printAirportTrafficCounts(airports,  workingDirectory+outputdir);
		printFlightDetails(flightList, workingDirectory+outputdir);
		printCenterTransitTraffic(centers, workingDirectory+outputdir);
		
		
		System.out.println("Finished");
	}
		

	public void printCenterTransitTraffic(Centers centers, String dir) {
		try {
			String fname = "center_transit.csv";
			System.out.println("Printing center transit details to " + dir +fname);
			FileWriter fstream = new FileWriter(dir + fname);
			BufferedWriter out = new BufferedWriter(fstream);
			centers.printCentersTransit(out);
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printFlightDetails(ArrayList<Flight> flightList, String dir){
		try {
			String fname = "depArr_fcfs_flight_details.csv";
			System.out.println("Printing flight details to " + dir +fname);
			FileWriter fstream = new FileWriter(dir + fname);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("FlightId,DepartureAirport,ArrivalAirport,DepartureTimeScheduled,DepartureTimeFinal,ArrivalTimeScheduled,ArrivalTimeFinal,TotalDelay,GroundDelay,AirDelay");
			out.write("\n");
			for(Flight f: flightList) {
				double totalDelay = f.atcGroundDelay + f.atcAirDelay;
				double totalGroundDelay = f.atcGroundDelay;
				double totalAirDelay = f.atcAirDelay;
				out.write(f.id + "," + f.departureAirport + "," + f.arrivalAirport + "," + f.departureTimeProposed + "," 
						+ f.departureTimeFinal + "," + f.arrivalTimeScheduled + "," + f.arrivalTimeFinal + "," + totalDelay +","+ totalGroundDelay+","+totalAirDelay);
				out.write("\n");
			}
			out.close();
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printAirportTrafficCounts(Airports airports, String dir) {
		try {
			System.out.println("Printing traffic counts to " + dir);
			FileWriter capstream = new FileWriter( dir + "fcfs_airport_capacities.csv");
			BufferedWriter airport_caps_out = new BufferedWriter(capstream);
			
			FileWriter depstream = new FileWriter(dir + "fcfs_airport_DEPtraffic.csv");
			BufferedWriter airport_DEPtraffic_out = new BufferedWriter(depstream);
			
			FileWriter arrstream = new FileWriter(dir + "fcfs_airport_ARRtraffic.csv");
			BufferedWriter airport_ARRtraffic_out = new BufferedWriter(arrstream);
			
			FileWriter sdepstream = new FileWriter(dir + "fcfs_airport_schedDEPtraffic.csv");
			BufferedWriter airport_schedDEPtraffic_out = new BufferedWriter(sdepstream);
			
			FileWriter sarrstream = new FileWriter(dir + "fcfs_airport_schedARRtraffic.csv");
			BufferedWriter airport_schedARRtraffic_out = new BufferedWriter(sarrstream);
			
			airports.printAirportsToFile(
										 airport_caps_out, 
										 airport_DEPtraffic_out, 
										 airport_schedDEPtraffic_out, 
										 airport_ARRtraffic_out, 
										 airport_schedARRtraffic_out
										 );
			
			airport_caps_out.close();
			airport_DEPtraffic_out.close();
			airport_ARRtraffic_out.close();
			airport_schedDEPtraffic_out.close();
			airport_schedARRtraffic_out.close();
			
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printSectorTraffic(Sectors sectors, String dir) {
		try {
			FileWriter fstream = new FileWriter(dir + "fcfs_sector_traffic.csv");
			BufferedWriter sector_out = new BufferedWriter(fstream);
			sectors.printSectorsToFile(sector_out);
			sector_out.close();
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void printAirportDelays(ArrayList<Flight> flightList, String dir) {
		try {
			String fname = "deparr_fcfs_airport_delays.csv";
			System.out.println("Printing airport delays to " + dir+fname);
			FileWriter fstream = new FileWriter(dir + fname);
			BufferedWriter out = new BufferedWriter(fstream);
			Hashtable<String, Double> airportDelay = new Hashtable<String,Double>();
			for(Flight f: flightList){
				double realDelay = f.arrivalTimeFinal - f.arrivalTimeScheduled;
				
				if(airportDelay.get(f.arrivalAirport)==null){
					airportDelay.put(f.arrivalAirport, 0.0);
				}				
				airportDelay.put(f.arrivalAirport, airportDelay.get(f.arrivalAirport) + realDelay);
			}
			Enumeration<String> enumKey = airportDelay.keys();
			while(enumKey.hasMoreElements()) {
				String k = enumKey.nextElement();
				Double val = airportDelay.get(k);
				if(val != 0.0) { val /= 60000;}
				out.write(k + "," + val);
				out.write("\n");
			}
			out.close();
		}catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}


}
