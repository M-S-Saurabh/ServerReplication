
/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RMIBankServerImpl extends UnicastRemoteObject implements RMIBankServer {

	private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static int RMIRegPort; // makes sense to keep static as each JVM instance would have on only one RMI reg 

	private List<RMIBankServer> serverStubs;
	
	private PriorityBlockingQueue<Message> lamportQueue;
	
	private Hashtable<Integer, BankAccount> accounts;
	
	private int lamportClock;

	private int serverID;

	private ExecuteHandler execHandler;

	private int receivedHalt;

	private int numClients;

	private long serviceTimeSum;
	private int numServices;

	private String serverName;
	
	public RMIBankServerImpl(int serverID, String serverName, Map<Integer, String[]> serverInfo, int numClients) throws RemoteException {
		super();
		this.initAccounts(Constants.NUM_ACCOUNTS, Constants.INIT_BALANCE);
		this.serverID = serverID;
		this.serverStubs = new LinkedList<>();
		this.receivedHalt = 0;
		this.numClients = numClients;
		
		this.serverName = serverName;
		
		this.lamportClock = 0;
		this.lamportQueue = new PriorityBlockingQueue<Message>(1000, new MessageComparator());
		
		this.execHandler = new ExecuteHandler(accounts, serverID, this.lamportQueue);
		
		this.serviceTimeSum = 0;
		this.numServices = 0;
		
		Thread postConstruct = new Thread(new ConnectToCluster(serverID, serverInfo, serverStubs));
		postConstruct.start();
	}

	private void initAccounts(int numAccounts, int initBalance) {
		this.accounts = new Hashtable<Integer, BankAccount>(numAccounts);
		for(int i=0; i<numAccounts; i++) {
			// Create account
			BankAccount newAccount = new BankAccount();
			// Set initial balance
			newAccount.setBalance(initBalance);
			// Store in hash table
	        this.accounts.put(newAccount.UID, newAccount);
		}
	}

	public static void main(String[] args) throws SecurityException, IOException, AlreadyBoundException {
		if (args.length != 3) {
			throw new RuntimeException("Syntax: RMIBankServerImpl <server ID> <configFile> <numClients>");
		}

		int serverId = Integer.parseInt(args[0]);
		int numClients = Integer.parseInt(args[2]);

		System.out.println(String.format("Hostname is %s", InetAddress.getLocalHost().getHostName()));

		Map<Integer, String[]> serverInfo = parseConfigFile(args[1], serverId);

		 // This block configure the logger with handler and formatter FileHandler fh
		 FileHandler fh = new FileHandler(String.format("./logs/Server-%d.log", serverId));
		 logger.addHandler(fh);
		 System.setProperty("java.util.logging.SimpleFormatter.format",
		 Constants.LOG_FORMAT); 
		 SimpleFormatter formatter = new SimpleFormatter();
		 fh.setFormatter(formatter);
		 
		 // setting the security policy
		 System.setProperty("java.security.policy", "file:./security.policy");
		 System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostName());
		 if (System.getSecurityManager() == null) {
			 System.setSecurityManager(new SecurityManager());
		 }
	
		 // the stub that is exposed via the RMI registry 
		 StringBuilder sb = new StringBuilder(Constants.RMI_SERVER_NAME);
		 sb.append(":").append(serverId);
		 String serverName = sb.toString();
		 
		 RMIBankServer bankStub = (RMIBankServer) UnicastRemoteObject.toStub(new RMIBankServerImpl(serverId, serverName, serverInfo, numClients)); 
		 logger.info(String.format("Using the supplied RMI registry port: %d", RMIRegPort)); 
		 Registry localRegistry = LocateRegistry.getRegistry(RMIRegPort);
		 
		 localRegistry.bind(serverName, bankStub); // setting up
	}

	
	/*
	 * Parse the config file to obtain other server info and the RMI registry port for this server
	 */
	private static Map<Integer, String[]> parseConfigFile(String fileName, int ID) throws FileNotFoundException {
		Scanner fileScanner = new Scanner(new FileReader(fileName));
		Map<Integer, String[]> map = new HashMap<>();
		if (!fileScanner.hasNextLine()) {
			System.out.println("Empty config file");
			throw new FileNotFoundException();
		}

		fileScanner.nextLine();
		while (fileScanner.hasNextLine()) {
			String[] line = fileScanner.nextLine().split(" ");
			int sID = Integer.parseInt(line[1]);
			if (sID == ID) {
				RMIRegPort = Integer.parseInt(line[2]); // might overwrite everytime, but doesn't matter
				continue;
			}
			map.put(sID, new String[] {line[0], line[2]});
		}
		return map;
	}

	/*
	 * Thread worker which iterates through all servre configs and connects to them one by one
	 * possibly increase the thred.sleep if you it is taking longer for the cluster to form
	 */
	public class ConnectToCluster implements Runnable {
		private int serverId;
		private Map<Integer, String[]> serverInfo;
		private List<RMIBankServer> serverStubs;

		ConnectToCluster(int serverId, Map<Integer, String[]> serverInfo, List<RMIBankServer> serverStubs) {
			this.serverId = serverId;
			this.serverInfo = serverInfo;
			this.serverStubs = serverStubs;
		}

		public void run() {
			logger.info("Server-" + this.serverId + " is Trying to Connect to Cluster");
			for (Map.Entry<Integer, String[]> entry: serverInfo.entrySet()) {
				RMIBankServer stub = null;
				int sID = entry.getKey();
				String hostName = entry.getValue()[0];
				String rmiPort = entry.getValue()[1];
				//System.out.println(rmiPort);
				StringBuilder sb = new StringBuilder(Constants.RMI_SERVER_NAME);
				sb.append(":").append(sID);
				String serverName = sb.toString();
				logger.info("Connecting to "+serverName);
				int count = 20;
				while (stub == null && count-- > 0) {
					try { // trying to get the bankserver stub from the RMI registry
						Thread.sleep(3000); // possibly increase the sleep time if cluster is taking time to form
						stub = (RMIBankServer) Naming.lookup(
								String.format("rmi://%s:%s/%s", hostName, rmiPort, serverName));
						logger.info("Server-stub received for "+serverName);
						serverStubs.add(stub);
					} catch (Exception e) {
						logger.info("Retry connecting to "+serverName);
					}
				}
			}
			logger.info("Connection trials complete. No of RMI stubs is "+serverStubs.size());
			logger.info("---- Server Initialization complete. Ready for client messages. ----");
		}
	}
	
	private void advanceClock() {
		this.lamportClock += 1;
	}
	
	private void setClock( int newTime) {
		this.lamportClock = newTime;
	}
	
	private int getClock() {
		return this.lamportClock;
	}
	
	private void sendMulticast(Message message) throws RemoteException {
		for (RMIBankServer server: serverStubs) {
			Message ack = server.receiveMulticast(message);
//			this.lamportQueue.add(ack);
			
			// Log the server message
			logger.info(String.format(Constants.SERVER_MSG_LOG, 
					this.serverID, Constants.SERVER_REQ, LocalDateTime.now(),
					ack.getTimeStamp(), ack.getServerId(), ack.getType(), ack.getRequest()));
		}
	}

	private void waitForQueue(Message message) {
//		System.out.println("Waiting for:"+message.toString());
		Message front = this.lamportQueue.peek();
//		System.out.println("Wait on :"+front.toString());
		// Wait for the message to be front of queue.
		while(!this.lamportQueue.peek().equals(message)) {
			front = this.lamportQueue.peek();
		}
	}
	
	private int messageRoutine(Message message) throws RemoteException {
		// Enqueue the client message in current process.
		this.lamportQueue.add(message);
		
		// Multicast this message to other servers
		this.sendMulticast(message);
		
		waitForQueue(message);
		
		// Increment clock and Multicast this message to other servers
		this.advanceClock();
		Message execMessage = new ExecuteMessage(this.serverID, this.getClock(), Constants.EXECUTE_MESSAGE, message);
		logger.info(String.format(Constants.SERVER_MSG_LOG, 
				this.serverID, Constants.SERVER_REQ, LocalDateTime.now(),
				execMessage.getTimeStamp(), execMessage.getServerId(), execMessage.getType(), execMessage.getRequest()));
		
		this.sendMulticast(execMessage);

		// Execute the message
		int response = this.execHandler.execute(message);
		this.advanceClock();
				
		return response;
	}
	
	@Override
	public int createAccountRMI() throws RemoteException {
		// Increment clock on receiving message.
		this.advanceClock();
		
		// Log the client message
		logger.info(String.format(Constants.SERVER_MSG_LOG, 
				this.serverID, Constants.CLIENT_REQ, LocalDateTime.now(),
				this.getClock(), this.serverID, Constants.CREATE_MESSAGE, ""));
		
		CreateMessage message = new CreateMessage(this.serverID, this.getClock());
		int uid = messageRoutine(message); 
		return uid;
	}

	@Override
	public String depositRMI(int uid, int amount) throws RemoteException {
		// Increment clock on receiving message.
		this.advanceClock();
		DepositMessage message = new DepositMessage(this.serverID, this.getClock(), uid, amount);
		int response = messageRoutine(message);
		
		logger.info("Deposited amount: " + amount + " to uid:" + uid + " with status:" + Constants.OK_STATUS);
		return Constants.OK_STATUS;
	}

	@Override
	public int getBalanceRMI(int uid) throws RemoteException {
		// Increment clock on receiving message.
		this.advanceClock();
		CheckMessage message = new CheckMessage(this.serverID, this.getClock(), uid);
		int balance = messageRoutine(message);
		
		if (balance < 0) {
			logger.severe(String.format("Acccount with id:%d balance is invalid.", uid));
			return -1;
		} else {
			return balance;
		}
	}

	@Override
	public String transferRMI(int sourceId, int targetId, int amount) throws RemoteException {
		long t0 = System.nanoTime(); 
		
		// Log the client message
		logger.info(String.format(Constants.SERVER_MSG_LOG, 
				this.serverID, Constants.CLIENT_REQ, LocalDateTime.now(),
				this.getClock(), this.serverID, Constants.TRANSFER_MESSAGE, 
				String.format("[%d, %d, %d]", sourceId, targetId, amount)));
		
		// Increment clock on receiving message.
		this.advanceClock();
		TransferMessage message = new TransferMessage(this.serverID, this.getClock(), sourceId, targetId, amount);
		int balance = messageRoutine(message);
		
		long serviceTime = (System.nanoTime() - t0);
		this.serviceTimeSum += serviceTime;
		this.numServices++;
		
		if(balance < 0) {
			return Constants.INSUFFICIENT_BALANCE;
		}else {
			return Constants.OK_STATUS;
		}
	}
	
	@Override
	public Message receiveMulticast(Message message) {
		// Advance the local clock
		int newTime = Math.max(message.getTimeStamp(), this.getClock()) + 1;
		this.setClock(newTime);
		
		// Log the server message
		logger.info(String.format(Constants.SERVER_MSG_LOG, 
				this.serverID, Constants.SERVER_REQ, LocalDateTime.now(),
				message.getTimeStamp(), message.getServerId(), message.getType(), message.getRequest()));
		
		if(message.getType().equals(Constants.EXECUTE_MESSAGE)) {
			// Execute the message
			Message toExec = ((ExecuteMessage) message).getExecMessage();
			this.execHandler.execute(toExec);
		}else {
			// Put in local queue.
			this.lamportQueue.add(message);
		}
		
		// Send an Ack with current timestamp
		Message ack = new AckMessage(this.serverID, this.getClock(),
				Constants.ACK_MESSAGE, message.getTimeStamp(), message.getServerId());
		return ack;
	}

	@Override
	public String clientHalt(int clientId) throws RemoteException {
		this.receivedHalt += 1;
		if(this.receivedHalt == this.numClients) {
			for (RMIBankServer server: serverStubs) {
				server.serverHalt();
			}
			this.serverHalt();
		}
		return Constants.OK_STATUS;
	}

	@Override
	public void serverHalt() throws RemoteException {
		try{
			pendingRequests();
			printBalances();
			printPerformance();
			
	        // Unregister ourself
			Registry localRegistry = LocateRegistry.getRegistry(RMIRegPort);
	        localRegistry.unbind(serverName);
	        
	        // Unexport; this will also remove us from the RMI runtime
	        UnicastRemoteObject.unexportObject(this, true);
	        logger.info(String.format("Server-%d has exited.", this.serverID));
	        
	        return;
	    }
	    catch(Exception e){
	    	e.printStackTrace();
	    }
	}

	private void printPerformance() {
		logger.info("---- Performance Experiment ----");
		logger.info(String.format("Number of server replicas: %d", 
				this.serverStubs.size()+1));
		
		double avg = (double)this.serviceTimeSum / (double)this.numServices;
		double seconds = avg / 1_000_000_000.0;
		
		logger.info(String.format("Average service processing time for Server-%d is %5.4f secs", 
				this.serverID, seconds));
	}

	private void pendingRequests() {
		logger.info(String.format("---- Pending requests in Server-%d :",this.serverID));
		if(this.lamportQueue.size() == 0) {
			logger.info("--None--");
		}
		for(Message msg : this.lamportQueue) {
			logger.info(msg.toString());
		}
	}

	private void printBalances() {
		logger.info("---- Printing Balances ----");
		int totalBalance = 0;
		for(int i=0; i<accounts.size(); i++) {
			int balance = accounts.get(i+1).getBalance();
			logger.info(String.format("Final balance in accountId:%d is %d", i, balance));
			totalBalance += balance;
		}
		logger.info(String.format("Sun of balances in all accounts is %d", totalBalance));
	}

}
