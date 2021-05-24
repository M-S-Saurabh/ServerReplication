package shared;
/*******************************************************************************
 * Authors:
 * ---------
 * Saurabh Mylavaram (mylav008@umn.edu)
 * Edwin Nellickal (nelli053@umn.edu)
 ******************************************************************************/



public final class Constants {
	// Sever init constants
	public static final int NUM_ACCOUNTS = 20;
	public static final int INIT_BALANCE = 1000;
	
	// Client thread constants
	public static final int TRANSFER_AMOUNT = 10;
	public static final int NUM_TRANSFERS = 200;

	private Constants() {
		// restrict instantiation
	}
	
	// Logging related
	public static final String LOG_FORMAT = "%5$s%6$s%n";
	
	public static final String SERVER_MSG_LOG = "Server-%d %s %s [%d, %d] %s %s";
	public static final String CLIENT_REQ = "CLIENT-REQ";
	public static final String SERVER_REQ = "SRV-REQ";
	public static final String REQ_PROCESSING = "REQ_PROCESSING";
	
	public static final String CLIENT_REQ_LOG = "CLNT-%d SRV-%d REQ %s %s %s";
	public static final String CLIENT_RSP_LOG = "CLNT-%d SRV-%d RSP %s %s";
	
	// Status message strings
	public static final String OK_STATUS = "OK";
	public static final String FAIL_STATUS = "FAIL";
	public static final String CREATE_MESSAGE = "CREATE";
	public static final String DEPOSIT_MESSAGE = "DEPOSIT";
	public static final String CHECK_MESSAGE = "CHECK";
	public static final String TRANSFER_MESSAGE = "TRANSFER";
	public static final String ACK_MESSAGE = "ACK";
	public static final String EXECUTE_MESSAGE = "EXECUTE";
	public static final String REMOVE_MESSAGE = "REMOVE";
	
	// Failure reason strings
	public static final String INSUFFICIENT_BALANCE = "In-sufficient balance in source account.";
	
	// RMI stub name used on registry.
	public static final String RMI_SERVER_NAME = "MyRMIBankServer";
	
	public static enum MessageType {
		CREATE, 
		DEPOSTI,
		CHECK, 
		TRANSFER,
		ACK
	};
}
