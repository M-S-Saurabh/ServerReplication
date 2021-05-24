package message;
import java.util.LinkedList;

public class RemoveMessage extends Message {
	Message messageToRemove;
	
	public RemoveMessage(int serverId, int lamportClock, String type, Message message) {
		super(serverId, lamportClock, type, new LinkedList<>());
		this.messageToRemove = message;
		this.request.add(message.getTimeStamp());
		this.request.add(message.getServerId());
	}
	
	public Message getRemoveMessage() {
		return messageToRemove;
	}
}
