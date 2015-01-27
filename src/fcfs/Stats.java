package fcfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

public class Stats {

	public static Hashtable<String, Double> count_sum_mean_std_min_max(ArrayList<Double> t){
		return count_sum_mean_std_min_max(t.toArray(new Double[t.size()]));	
	}
	
	public static Hashtable<String, Double> count_sum_mean_std_min_max(Double data[])
	{
		final int n = data.length;
		if(n==0){
			//System.err.println("warning this data array is empty, you sure about this?");
			Hashtable<String, Double> temp = new Hashtable<String, Double>();
			temp.put("count", n*1.);temp.put("sum", 0d);temp.put("mean", 0d);
			temp.put("std", 0d);temp.put("min",0d);temp.put("max", 0d);
			//Double[] temp = {n*1., sum, mean, Math.sqrt(sumVariance /n), min, max};
			return temp;
		}
		
		U.Assert(n>0,"error stats: data length must be greater than 0");
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
