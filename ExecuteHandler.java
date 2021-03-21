import java.time.LocalDateTime;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;

/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/

public class ExecuteHandler {
	
	public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private int serverId;
	
	protected Hashtable<Integer, BankAccount> accounts;

	private PriorityBlockingQueue<Message> lamportQueue;
	
	/*
	 * Each thread calls its own request handler object.
	 */
	public ExecuteHandler(Hashtable<Integer, BankAccount> accounts, int serverId, PriorityBlockingQueue<Message> lamportQueue2) {
		this.accounts = accounts;
		this.serverId = serverId;
		this.lamportQueue = lamportQueue2;
	}
	
	/*
	 * This method processes all kinds of requests.
	 * It calls the right function based on the field operationName
	 */
	int execute(Message message) {
		// "Server-%d %s %s [%d, %d] %s %s"
		logger.info(String.format(Constants.SERVER_MSG_LOG, 
				this.serverId, Constants.REQ_PROCESSING, LocalDateTime.now(),
				message.getTimeStamp(), message.getServerId(), "", ""));
		
		String operationName = message.getType();
		int response = 0;
        switch(operationName) 
        {
	        case Constants.CREATE_MESSAGE:
	        	response = createAccount((CreateMessage) message);
	        	break;
	        case Constants.CHECK_MESSAGE:
	        	response = getBalance((CheckMessage) message);
	        	break;
	        case Constants.DEPOSIT_MESSAGE:
	        	response = deposit((DepositMessage) message);
	        	break;
	        case Constants.TRANSFER_MESSAGE:
	        	response = transfer((TransferMessage) message);
	        	break;
	    	default:
	    		logger.severe("ServerID:"+this.serverId+" message:"+message.getMessageId()+": Operation "+operationName+" not implemented. Response is null.");
        }
        
        // Remove message and its acks after execution.
        removeMessages(message);
        
		return response;
	}
	
	private void removeMessages(Message message) {
		// Remove the message which is executed.
		for (Iterator<Message> iterator = lamportQueue.iterator(); iterator.hasNext(); ) {
		    Message msg = iterator.next();
		    if (msg.equals(message)){ 
		    	iterator.remove(); continue; 
	    	}
//		    if (msg.getType() == Constants.ACK_MESSAGE && ((AckMessage) msg).ackEquals(message)) {
//	    		iterator.remove();
//		    }
		}
	}

	private int transfer(TransferMessage message) {
		
		BankAccount source = accounts.get(message.getFromAccount());
		BankAccount target = accounts.get(message.getToAccount());
		int amount = message.getAmount();
		int response;
		if(amount <= source.getBalance()) {
			source.setBalance(source.getBalance() - amount);
			target.setBalance(target.getBalance() + amount);
			response = 1;
		}else {
			response = -1;
		}
		return response;
	}

	private int getBalance(CheckMessage message) {
		int accID = message.getAccount();
		int balance = accounts.get(accID).getBalance();
		return balance;
	}

	private int createAccount(CreateMessage message) {
		BankAccount newAccount = new BankAccount();
        this.accounts.put(newAccount.UID, newAccount);
		return newAccount.UID;
	}
	
	private int deposit(DepositMessage message) {
		int accID = message.getAccount();
		int amount = message.getAmount();
		int balance = accounts.get(accID).getBalance();
		accounts.get(accID).setBalance(balance + amount);
		return (balance+amount);
	}
}
