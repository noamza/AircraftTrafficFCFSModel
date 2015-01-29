package fcfs;


/**
 * @author Noam Almog
 * This is the entry point to the program. 
 * All of the configuration, directory/file names, are found in the U.java class.
 * 
 *  This see how previous versions of the scheduler classes were initialized, se earrlier versions of Main.java in repo.
 * 
 * @precondition 
 * - The directory and file names are correct in the U class.				  
 */

public class Main
{
	/**
	 * @param args
	 * working directory
	 */
	public static void main(String[] args)
	{
		U.p(U.workingDirectory);
		
		//sample command line run:
		//manually sets working directory as opposed to System.properties.user.dir
		//java -Xms3024M -Xmx3024M -jar JarName.jar /Users/nalmog/Desktop/Scheduler/ 3
		Scheduler s = new SchedulerExample();
		if(args !=null && args.length > 0){
			U.workingDirectory = args[0];
			U.start();
			s.schedule();
			U.end();
		} else {
			U.start();
			s.schedule();
			U.end();
		}
		
		
	}
}




