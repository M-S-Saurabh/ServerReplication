package message;
/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/

import java.util.LinkedList;
import java.util.List;

import shared.Constants;

public class CheckMessage extends Message {
	
	public CheckMessage(int serverId, int lamportClock, int uid) {
		super(serverId, lamportClock, Constants.CHECK_MESSAGE, new LinkedList<>());
		this.request.add(uid);
	}
	
	public int getAccount() {
		return this.request.get(0);
	}
}
