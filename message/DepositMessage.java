package message;
/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/
import java.util.LinkedList;

import shared.Constants;

public class DepositMessage extends Message {
	public DepositMessage(int serverId, int lamportClock, int uid, int amount) {
		super(serverId, lamportClock, Constants.DEPOSIT_MESSAGE, new LinkedList<>());
		this.request.add(uid);
		this.request.add(amount);
	}
	
	public int getAccount() {
		return this.request.get(0);
	}
	
	public int getAmount() {
		return this.request.get(1);
	}
}
