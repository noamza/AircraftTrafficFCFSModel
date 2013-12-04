package fcfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

public class Stats {
/*
	public static void cumulativeDist(PreferentialMerge p){
		ArrayList<Flight> temp = new ArrayList<Flight>(p.flightList.values());
		Collections.sort(temp, new airlineComparator()); //idComparator
		int max = 100;
		int[] passes = new int[max];
		int[] passeb = new int[max];
		Double[][] numberOfFlights0passes1passedBy2  = new Double[max][3];
		for(int i = 0; i < max; i++ ){
			for(Flight f: temp){
				if(f.equipped){
					if(f.totalPasses >= i){
						passes[i]++;
					}
				} else { //is this right??
					if(f.totalpassedBy >= i){
						passeb[i]++;
						if(i==10)T.p(f.id);
					}
				}
			}
		} //T.p("");T.p("PASS");
		for(int i = 0; i < max; i++ ){
			numberOfFlights0passes1passedBy2[i][0] = 1.*i;
			numberOfFlights0passes1passedBy2[i][1] = 100.0 * passes[i]/passes[0];
			numberOfFlights0passes1passedBy2[i][2] = 100.0 * passeb[i]/passeb[0];
		}
		String[] columNames = {"number of flights","% Equipped pass", "% Unequipped get passed by"};
		FileIO.writeToCsv(T.workingDirectory + T.plotFolder + "Cumulative " + p.CSVfileName(),
				columNames,
				numberOfFlights0passes1passedBy2);
	}
	*/
	public static Hashtable<String, Double> count_sum_mean_std_min_max(ArrayList<Double> t){
		return count_sum_mean_std_min_max(t.toArray(new Double[t.size()]));	
	}
	
	public static Hashtable<String, Double> count_sum_mean_std_min_max(Double data[])
	{
		final int n = data.length;
		// return false if n is too small
//		T.Assert(n<2, "not enough data");
		double min = 0, max = 0,  sum = 0;
		// Calculate the mean sum min max
		for (int i=0; i<n; i++){
			if(i == 0){min = max = data[i];} //initialize min max}
			sum += data[i];
			if(data[i]>max){max = data[i];}else if(data[i]<min){min = data[i];}
		}
		double mean = sum/n;
		// calculate the sum of squares
		double sumVariance = 0;
		for ( int i=0; i<n; i++ ){
			final double v = data[i] - mean;
			sumVariance += v*v;
		}
		
		Hashtable<String, Double> temp = new Hashtable<String, Double>();
		temp.put("count", n*1.);temp.put("sum", sum);temp.put("mean", mean);
		temp.put("std", Math.sqrt(sumVariance /n));temp.put("min",min);temp.put("max", max);
		//Double[] temp = {n*1., sum, mean, Math.sqrt(sumVariance /n), min, max};
		return temp;
	}
	
	public static double mean(double[] data){
		double sum = 0; 
		for (double d: data){sum += d;}
		return sum/data.length;
	}


}
