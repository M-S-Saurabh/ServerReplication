/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/
import java.util.List;

public class DepositMessage extends Message {
	DepositMessage(int lamportClock, String type, List<Integer> request) {
		super(lamportClock, type, request);
	}
	
	public int getAccount() {
		return this.request.get(0);
	}
	
	public int getAmount() {
		return this.request.get(1);
	}
}
