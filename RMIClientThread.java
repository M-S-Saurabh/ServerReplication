/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/


import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;


public class RMIClientThread implements Runnable {
	
	public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	private List<RMIBankServer> serverStubs; // one session per client thread
	private List<Integer> accountIds;
	private int iterationCount;
	private Random random;
	
	private int clientId;

	public RMIClientThread(List<RMIBankServer> serverStubs, List<Integer> accountIds, int iterationCount, int clientId) {
		this.serverStubs = serverStubs;
		this.accountIds = accountIds;
		this.iterationCount = iterationCount;
		this.random = new Random();
		this.clientId = clientId;
	}

	@Override
	public void run() {
		for(int i=0; i<iterationCount; i++) {
			// Pick a random server.
			int randomIndex = random.nextInt(serverStubs.size());
			RMIBankServer bankServer = serverStubs.get(randomIndex);
			
			// Pick a random element from list.
			int first = accountIds.get(random.nextInt(accountIds.size()));
			
			// Pick another random element from list until you get a different element.
			int second = accountIds.get(random.nextInt(accountIds.size()));
			while(second == first) {
				second = accountIds.get(random.nextInt(accountIds.size()));
			}
			int amount = Constants.TRANSFER_AMOUNT;
			
			try {
				logger.info(String.format(Constants.CLIENT_REQ_LOG, 
						clientId, randomIndex, LocalDateTime.now(), Constants.TRANSFER_MESSAGE, 
						String.format("from:%d to:%d amt:%d", first, second, amount)));
				
				// Transfer 10$ from first account to second account
				String status = bankServer.transferRMI(first, second, amount);
				
				// log the transaction status
				logger.info(String.format(Constants.CLIENT_RSP_LOG, 
						clientId, randomIndex, LocalDateTime.now(), status));
				
			} catch (RemoteException e) {
				logger.severe("Couldn't invoke transfer method.");
				logger.severe(e.getMessage());
				e.printStackTrace();
			}
		}

	}

}
