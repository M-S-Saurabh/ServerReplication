import java.util.LinkedList;

public class ExecuteMessage extends Message {
	Message messageToExec;
	
	ExecuteMessage(int serverId, int lamportClock, String type, Message message) {
		super(serverId, lamportClock, type, new LinkedList<>());
		this.messageToExec = message;
		this.request.add(message.getTimeStamp());
		this.request.add(message.getServerId());
	}
	
	Message getExecMessage() {
		return messageToExec;
	}
}
