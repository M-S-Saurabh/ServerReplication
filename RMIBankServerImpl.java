
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
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Scanner;

public class RMIBankServerImpl extends UnicastRemoteObject implements RMIBankServer {

	private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static int RMIRegPort; // makes sense to keep static as each JVM instance would have on only one RMI reg 

	private List<RMIBankServer> serverStubs;
	
	private PriorityQueue<Message> lamportQueue;
	
	private Hashtable<Integer, BankAccount> accounts;
	
	private int lamportClock;

	private int serverID;
	
	private int simpleCounter; // TODO for intitial debugging remove later

	public RMIBankServerImpl(int serverID, Map<Integer, String[]> serverInfo) throws RemoteException {
		super();
		this.accounts = new Hashtable<Integer, BankAccount>(100);
		this.serverID = serverID;
		this.serverStubs = new LinkedList<>();
		this.lamportClock = 0;
		this.simpleCounter = 0;
		this.lamportQueue = new PriorityQueue<Message>((a,b)->Integer.compare(a.getTimeStamp(), b.getTimeStamp()));
		Thread postConstruct = new Thread(new ConnectToCluster(serverID, serverInfo, serverStubs));
		postConstruct.start();
	}

	public static void main(String[] args) throws SecurityException, IOException, AlreadyBoundException {
		if (args.length != 2) {
			throw new RuntimeException("Syntax: RMIBankServerImpl <server ID> <configFile>");
		}

		int ID = Integer.parseInt(args[0]);

		System.out.println(InetAddress.getLocalHost().getHostName());

		Map<Integer, String[]> serverInfo = parseConfigFile(args[1], ID);

		 // This block configure the logger with handler and formatter FileHandler fh
		 FileHandler fh = new FileHandler("./logs/RMI_serverLogfile.log");
		 logger.addHandler(fh);
		 System.setProperty("java.util.logging.SimpleFormatter.format",
		 Constants.LOG_FORMAT); SimpleFormatter formatter = new SimpleFormatter();
		 fh.setFormatter(formatter);
		 
		 // setting the security policy
		 System.setProperty("java.security.policy", "file:./security.policy");
		 System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostName());
		 if (System.getSecurityManager() == null) {
			 System.setSecurityManager(new SecurityManager());
		 }
	
		 // the stub that is exposed via the RMI registry 
		 RMIBankServer bankStub = (RMIBankServer) UnicastRemoteObject.toStub(new RMIBankServerImpl(ID, serverInfo)); 
		 logger.severe(String.format("Using the supplied RMI registry port: %d", RMIRegPort)); 
		 Registry localRegistry = LocateRegistry.getRegistry(RMIRegPort);
		 StringBuilder sb = new StringBuilder(Constants.RMI_SERVER_NAME);
		 sb.append(":").append(ID);
		 localRegistry.bind(sb.toString(), bankStub); // setting up
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
			System.out.println("Server:" + this.serverId + " is Trying to Connect to Cluster");
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
				int count = 10;
				while (stub == null && count-- > 0) {
					try { // trying to get the bankserver stub from the RMI registry
						Thread.sleep(3000); // possibly increase the sleep time if cluster is taking time to form
						stub = (RMIBankServer) Naming.lookup(
								String.format("rmi://%s:%s/%s", hostName, rmiPort, serverName));
						System.out.println("stub received for:"+serverName);
						serverStubs.add(stub);
					} catch (Exception e) {
						System.out.println("retrying connecting to:"+serverName);
					}
				}
			}
			System.out.println("size of serverStubs "+serverStubs.size());
		}
	}

	@Override
	public int createAccountRMI() throws RemoteException {
		CreateMessage message = new CreateMessage(++this.lamportClock, Constants.CREATE_MESSAGE, new LinkedList<>(Arrays.asList(this.simpleCounter++)));
		this.lamportQueue.add(message);
		for (RMIBankServer server: serverStubs) {
			server.multicast(message);
		}
		/*
		 * BankAccount newAccount = new BankAccount();
		 * logger.info("New Account created uid: " + newAccount.UID);
		 * accounts.put(newAccount.UID, newAccount); return newAccount.UID;
		 */
		return 0;
	}

	@Override
	public String depositRMI(int uid, int amount) throws RemoteException {
		BankAccount account = accounts.get(uid);
		account.setBalance(account.getBalance() + amount);
		logger.info("deposite amount: " + amount + " to uid:" + account.UID + " with status:" + Constants.OK_STATUS);
		return Constants.OK_STATUS;
	}

	@Override
	public int getBalanceRMI(int uid) throws RemoteException {
		BankAccount account = accounts.get(uid);
		if (account == null) {
			logger.severe(String.format("Acccount with id:%d could not be found.", uid));
			return -1;
		} else {
			return account.getBalance();
		}
	}

	@Override
	public String transferRMI(int sourceId, int targetId, int amount) throws RemoteException {
		BankAccount source = accounts.get(sourceId);
		BankAccount target = accounts.get(targetId);
		if (source.getBalance() < amount) {
			logger.severe(String.format("Transfer failed: %s", Constants.INSUFFICIENT_BALANCE));
			return Constants.FAIL_STATUS;
		} else {
			source.setBalance(source.getBalance() - amount);
			target.setBalance(target.getBalance() + amount);
			logger.info("transfer amount: " + amount + " from uid:" + sourceId + " to uid:" + targetId + " with status:"
					+ Constants.OK_STATUS);
			return Constants.OK_STATUS;
		}
	}
	
	@Override
	public void multicast(Message message) {
		//System.out.println("message recived at server: "+this.serverID+"for account: "+message.getRequest().get(0));
		System.out.println("message recived at server: "+this.serverID+" type:"+ message.getType() +"for account: "+message.getRequest().get(0));
	}

}
