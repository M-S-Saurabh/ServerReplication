/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/
import java.util.List;

public class TransferMessage extends Message {
	TransferMessage(int lamportClock, String type, List<Integer> request) {
		super(lamportClock, type, request);
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
