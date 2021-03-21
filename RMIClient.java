/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class RMIClient {
	
	public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static List<Integer> accountIds;

	private static List<RMIBankServer> serverStubs = new ArrayList<>();
	
	static Random random = new Random();

	private static List<Long> serviceTimes;

	public static void main(String[] args) throws SecurityException, IOException, InterruptedException{
		if (args.length != 3)
			throw new RuntimeException ("Syntax: RMIClient <clientID> <threadCount> <config-file-name>");
		
		// Parsing arguments
        int clientId = Integer.parseInt(args[0]);
        int threadCount = Integer.parseInt(args[1]);
		
		// This block configure the logger with handler and formatter  
        FileHandler fh = new FileHandler(String.format("./logs/Client-%d.log", clientId));  
        logger.addHandler(fh);
        System.setProperty("java.util.logging.SimpleFormatter.format", Constants.LOG_FORMAT);
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);
        
        Map<Integer, String[]> serverInfo = parseConfigFile(args[2]);
        
        // Connecting to servers.
        connectToCluster(serverInfo);

        accountIds = new ArrayList<Integer>();
        serviceTimes = Collections.synchronizedList(new ArrayList<>());
        
        System.setProperty("java.security.policy","file:./security.policy");
		
        System.setSecurityManager(new SecurityManager());
		try { 
			// Initialization
			initAccounts();
			
			// Perform 200 transfers between accounts (per thread) 
			spawnThreads(threadCount, Constants.NUM_TRANSFERS, clientId);
			
			// check balance in the end 
			sendHalt(clientId);
			 
		} catch (RemoteException e) {
			logger.severe("Couldn't establish RMI registry connection.");
			logger.severe(e.getMessage());
			e.printStackTrace();
		}

	}
	
	private static void initAccounts() {
		// Assuming that the servers already initialized 20 bank accounts (uids 1 to 20).
		for(int i=0; i<Constants.NUM_ACCOUNTS; i++) {
			accountIds.add(i+1);
		}
	}

	private static void spawnThreads(int threadCount, int iterationCount, int clientId) throws RemoteException { 
		List<Thread> transferThreads = new LinkedList<>();
		// create threads and create a session for each thread
		for (int i=0; i<threadCount; i++) {
			RMIClientThread c = new RMIClientThread(serverStubs, accountIds, iterationCount, clientId, serviceTimes); 
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
		
		printPerformance(clientId);
		
	}
	
	private static void printPerformance(int clientId) {
		logger.info("---- Performance Experiment (Client) ----");
		logger.info(String.format("Number of server replicas: %d", serverStubs.size()+1));
		
		double serviceTimeSum = 0;
		for(long time : serviceTimes) {
			serviceTimeSum += time;
		}
		double avg = serviceTimeSum / (double) serviceTimes.size();
		double seconds = avg / 1_000_000_000.0;
		
		logger.info(String.format("Average processing time seen by  Client-%d is %5.4f secs", 
				clientId, seconds));
	}
	
	private static void sendHalt(int clientId) throws RemoteException {
		serverStubs.get(0).clientHalt(clientId);
	}
	

	private static void checkBalances(int clientId) throws RemoteException {
		int totalBalance = 0;
		for(int accountId: accountIds) {
			int randomIndex = random.nextInt(serverStubs.size());
			RMIBankServer bank = serverStubs.get(randomIndex);
			
			logger.info(String.format(Constants.CLIENT_REQ_LOG, 
					clientId, randomIndex, LocalDateTime.now(), Constants.CHECK_MESSAGE, 
					String.format("uid:%d", accountId)));
			
			int balance = bank.getBalanceRMI(accountId);
			totalBalance += balance;
			
			logger.info(String.format(Constants.CLIENT_RSP_LOG, 
					clientId, randomIndex, LocalDateTime.now(), String.format("Balance:%d", balance)));
		}
		logger.info(String.format("Total balance (sum): %d", totalBalance));
	}

	private static void depositAllAccounts(int clientId, int amount) throws RemoteException {
		for(int accountId: accountIds) {
			int randomIndex = random.nextInt(serverStubs.size());
			RMIBankServer bank = serverStubs.get(randomIndex);
			
			logger.info(String.format(Constants.CLIENT_REQ_LOG, 
					clientId, randomIndex, LocalDateTime.now(), Constants.DEPOSIT_MESSAGE, 
					String.format("uid:%d amt:%d", accountId, amount)));
			
			String status = bank.depositRMI(accountId, amount); //deposit money into the account
			
			logger.info(String.format(Constants.CLIENT_RSP_LOG, 
					clientId, randomIndex, LocalDateTime.now(), status));
		}
	}

	private static void createAccounts(int clientId, int numAccounts) throws RemoteException {
		for(int i=0; i<numAccounts; i++) {
			int randomIndex = random.nextInt(serverStubs.size());
			RMIBankServer bank = serverStubs.get(randomIndex);
			
			logger.info(String.format(Constants.CLIENT_REQ_LOG, 
					clientId, randomIndex, LocalDateTime.now(), Constants.CREATE_MESSAGE, ""));
			
			int uid = bank.createAccountRMI(); // create account via RMI call
			accountIds.add(uid);
			
			logger.info(String.format(Constants.CLIENT_RSP_LOG, 
					clientId, randomIndex, LocalDateTime.now(), "Created with id:"+uid));
		}
	}
	
	/*
	 * Parse the config file to obtain other server info and the RMI registry port for this server
	 */
	private static Map<Integer, String[]> parseConfigFile(String fileName) throws FileNotFoundException {
		Scanner fileScanner = new Scanner(new FileReader(fileName));
		Map<Integer, String[]> map = new HashMap<>();
		if (!fileScanner.hasNextLine()) {
			System.out.println("Empty config file");
			throw new FileNotFoundException();
		}

		fileScanner.nextLine();
		while (fileScanner.hasNextLine()) {
			String[] line = fileScanner.nextLine().split(" ");
			int serverId = Integer.parseInt(line[1]);
			map.put(serverId, new String[] {line[0], line[2]});
		}
		return map;
	}
	
	private static void connectToCluster(Map<Integer, String[]> serverInfo) throws InterruptedException {
		System.out.println("Client is trying to connect to the Server Cluster");
		for (Map.Entry<Integer, String[]> entry: serverInfo.entrySet()) {
			RMIBankServer stub = null;
			int sID = entry.getKey();
			String hostName = entry.getValue()[0];
			String rmiPort = entry.getValue()[1];
			//System.out.println(rmiPort);
			StringBuilder sb = new StringBuilder(Constants.RMI_SERVER_NAME);
			sb.append(":").append(sID);
			String serverName = sb.toString();
			System.out.println("Connecting to:"+serverName);
			Thread.sleep(3000); // possibly increase the sleep time if cluster is taking time to form
			try {
				stub = (RMIBankServer) Naming.lookup(
						String.format("rmi://%s:%s/%s", hostName, rmiPort, serverName));
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				e.printStackTrace();
			}
			System.out.println("stub received for:"+serverName);
			serverStubs.add(stub);
				
		}
		System.out.println("size of serverStubs "+serverStubs.size());
	}

}
