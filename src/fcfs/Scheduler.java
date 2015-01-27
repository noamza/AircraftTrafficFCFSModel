/**
 * 
 */

package fcfs;

import java.util.ArrayList;

/**
 * @author Mohamad Refai
 * 
 * good idea, should be expanded.
 */
public interface Scheduler
{

	/**
	 * @param flightList
	 * @param dir
	 */
	public abstract void printResults(ArrayList<Flight> flightList, String dir);

	/**
	 * @return
	 */
	public abstract ArrayList<Flight> schedule();

}
