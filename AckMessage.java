/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/

import java.util.LinkedList;
import java.util.List;

public class AckMessage extends Message {
	AckMessage(int serverId, int lamportClock, String type, int msgTimestamp, int msgServerId) {
		super(serverId, lamportClock, type, new LinkedList<>());
		this.request.add(msgTimestamp);
		this.request.add(msgServerId);
	}
	
	boolean ackEquals(Message msg) {
		return (this.request.get(0) == msg.getTimeStamp()) && (this.request.get(1) == msg.getServerId());
	}
}
