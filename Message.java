/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/

import java.io.Serializable;
import java.util.List;

public class Message implements Serializable  {
	private final int timeStamp;
	
	private final String type;
	
	protected final List<Integer> request;
	
	Message(int lamportClock, String type, List<Integer> request) {
		this.timeStamp = lamportClock;
		this.type = type;
		this.request = request;
	}
	
	int getTimeStamp() {
		return this.timeStamp;
	}
	
	String getType() {
		return this.type;
	}
	
	List<Integer> getRequest() {
		return this.request;
	}
	
	@Override 
	public String toString() {
		return "Message [timeStamp="+ timeStamp +", type="+ type + ", request="+ request +"]";
	}
}
