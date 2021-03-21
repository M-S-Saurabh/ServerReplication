/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/

import java.io.Serializable;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

public class Message implements Serializable  {
	private static int count = 0;
	private final int messageId;
	
	private final int timeStamp;
	
	private final String type;
	
	protected final List<Integer> request;
	private int serverId;
	
	Message(int serverId, int lamportClock, String type, List<Integer> request) {
		this.serverId = serverId;
		this.timeStamp = lamportClock;
		this.type = type;
		this.request = request;
		this.messageId = ++count;
	}
	
	boolean equals(Message msg) {
		return (this.messageId == msg.messageId) && (this.serverId == msg.serverId);
	}
	
	int getMessageId() {
		return this.messageId;
	}
	
	int getServerId() {
		return this.serverId;
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
		return "Message [timeStamp="+ timeStamp +", pid="+ serverId+", type="+ type + ", request="+ request +"]";
	}
}

class MessageComparator implements Comparator<Message>{

	@Override
	public int compare(Message m1, Message m2) {
		int timeCompare = Integer.compare(m1.getTimeStamp(), m2.getTimeStamp());
		if ( timeCompare == 0) {
			return Integer.compare(m1.getServerId(), m2.getServerId());
		}else {
			return timeCompare;
		}
	}
	
}
