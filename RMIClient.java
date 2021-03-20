/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/


import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class RMIClient {
	
	public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static List<Integer> accountIds;

	public static void main(String[] args) throws SecurityException, IOException{
		if (args.length != 4)
			throw new RuntimeException ("Syntax: RMIClient <hostname> <port> <threadCount> <iterationCount>");
		
		// This block configure the logger with handler and formatter  
        FileHandler fh = new FileHandler("./logs/RMI_clientLogfile.log");  
        logger.addHandler(fh);
        System.setProperty("java.util.logging.SimpleFormatter.format", Constants.LOG_FORMAT);
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);
        
        accountIds = new ArrayList<Integer>();
        
        System.setProperty("java.security.policy","file:./security.policy");
		
        System.setSecurityManager(new SecurityManager());
		try { // trying to get the bankserver stub from the RMI registry
			StringBuilder sb = new StringBuilder(Constants.RMI_SERVER_NAME);
			sb.append(":").append("0");
			RMIBankServer bankServer = (RMIBankServer) Naming.lookup(
					String.format("rmi://%s:%s/%s", args[0], args[1], sb.toString())
			);
			
			// to create all accounts
			createAccounts(bankServer, 100);
			
			/*
			 * // to deposite montey in all accounts depositAllAccounts(bankServer, 100);
			 * 
			 * // to check balances of all accounts checkBalances(bankServer);
			 * 
			 * // perform transfers between accounts spawnThreads(bankServer,
			 * Integer.parseInt(args[2]), Integer.parseInt(args[3]));
			 * 
			 * // check balance in the end checkBalances(bankServer);
			 */
		} catch (MalformedURLException | RemoteException | NotBoundException e) {
			logger.severe("Couldn't establish RMI registry connection.");
			logger.severe(e.getMessage());
			e.printStackTrace();
		}

	}

	
	private static void spawnThreads(RMIBankServer bank, int threadCount, int iterationCount) throws RemoteException { 
		List<Thread> transferThreads = new LinkedList<>();
		// create threads and create a session for each thread
		for (int i=0; i<threadCount; i++) {
			RMIClientThread c = new RMIClientThread(bank, accountIds, iterationCount); 
			Thread txThread = new Thread(c);
			txThread.start();
			transferThreads.add(txThread); 
		}
		
		// wait for the client threads to complete by calling join
		for (Thread thread: transferThreads) {
			try {
				thread.join();
			} catch(InterruptedException e) { 
				logger.severe("join failed"); e.printStackTrace();
			}
	    }
	 }
	

	private static void checkBalances(RMIBankServer bank) throws RemoteException {
		StringBuilder sbR = new StringBuilder("Account-wise balance: ");
		int totalBalance = 0;
		for(int accountId: accountIds) {
			int balance = bank.getBalanceRMI(accountId);
			totalBalance += balance;
			
			sbR.append(String.format("%d:%d, ", accountId, balance));
		}
		logger.info(sbR.toString());
		logger.severe(String.format("Total balance (sum): %d", totalBalance));
	}

	private static void depositAllAccounts(RMIBankServer bank, int amount) throws RemoteException {
		for(int accountId: accountIds) {
			bank.depositRMI(accountId, amount); //deposit money into the account
		}
		logger.info("Deposited 100$ in all accounts.");
	}

	private static void createAccounts(RMIBankServer bank, int numAccounts) throws RemoteException {
		StringBuilder sbR = new StringBuilder("Created account ids: ");
		
		for(int i=0; i<numAccounts; i++) {
			int uid = bank.createAccountRMI(); // create account via RMI call
			accountIds.add(uid);
			
			sbR.append(uid);
			sbR.append(", ");
		}
		logger.info(sbR.toString());
	}

}
