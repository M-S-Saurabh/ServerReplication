/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/
import java.util.LinkedList;
import java.util.List;

public class TransferMessage extends Message {
	TransferMessage(int serverId, int lamportClock, int sourceId, int targetId, int amount) {
		super(serverId, lamportClock, Constants.TRANSFER_MESSAGE, new LinkedList<>());
		this.request.add(sourceId);
		this.request.add(targetId);
		this.request.add(amount);
	}
	
	public int getFromAccount() {
		return this.request.get(0);
	}
	
	public int getToAccount() {
		return this.request.get(1);
	}
	
	public int getAmount() {
		return this.request.get(2);
	}
}
