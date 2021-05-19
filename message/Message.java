package message;
/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/

import java.io.Serializable;
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
	
	public boolean equals(Message msg) {
		return (this.messageId == msg.messageId) && (this.serverId == msg.serverId);
	}
	
	public int getMessageId() {
		return this.messageId;
	}
	
	public int getServerId() {
		return this.serverId;
	}

	public int getTimeStamp() {
		return this.timeStamp;
	}
	
	public String getType() {
		return this.type;
	}
	
	public List<Integer> getRequest() {
		return this.request;
	}
	
	@Override 
	public String toString() {
		return "Message [timeStamp="+ timeStamp +", pid="+ serverId+", type="+ type + ", request="+ request +"]";
	}
}
