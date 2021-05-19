package message;
import java.util.LinkedList;

public class ExecuteMessage extends Message {
	Message messageToExec;
	
	public ExecuteMessage(int serverId, int lamportClock, String type, Message message) {
		super(serverId, lamportClock, type, new LinkedList<>());
		this.messageToExec = message;
		this.request.add(message.getTimeStamp());
		this.request.add(message.getServerId());
	}
	
	public Message getExecMessage() {
		return messageToExec;
	}
}
