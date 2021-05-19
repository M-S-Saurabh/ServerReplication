package message;
/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import shared.Constants;

public class CreateMessage extends Message {
	public CreateMessage(int serverId, int lamportClock) {
		super(serverId, lamportClock, Constants.CREATE_MESSAGE, new LinkedList<>());
	}
	
//	public int getAccount() {
//		return this.request.get(0);
//	}
}
