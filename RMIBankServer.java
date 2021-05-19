/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/


import java.rmi.Remote;
import java.rmi.RemoteException;

import message.Message;
//Bank Server application that is exposed via the RMI registry
public interface RMIBankServer extends Remote {
	// helps us create an account in the bank server
	int createAccountRMI() throws RemoteException;
	
	// helps us deposit money in a bank account
	String depositRMI(int uid, int amount) throws RemoteException;
	
	// helps us get the balance of a bank account
	int getBalanceRMI(int uid) throws RemoteException;
	
	// helps us transfer money between bank accounts
	String transferRMI(int sourceId, int targetId, int amount) throws RemoteException;
	
	// Indicates the client's operations are complete.
	String clientHalt(int clientId) throws RemoteException;
	
	// Indicates the client's operations are complete.
	void serverHalt() throws RemoteException;
	
	Message receiveMulticast(Message message) throws RemoteException;
}
