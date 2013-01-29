package preferentialmerge;

public class T{
	
	static double toMinutes = 60*1000.0;

	public static boolean Assert(boolean a, String expression){ 
		if(!a){throw new java.lang.Error("FAILED: " + expression);}
		return a;}
	
	public static boolean Assert(boolean a){ 
		if(!a){throw new java.lang.Error("FAILED: Assert()");}
		return a;}
	
	public static void print(String s){
		System.out.println(s);
	}

}