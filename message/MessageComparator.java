package message;

import java.util.Comparator;

public class MessageComparator implements Comparator<Message>{

	@Override
	public int compare(Message m1, Message m2) {
		int timeCompare = Integer.compare(m1.getTimeStamp(), m2.getTimeStamp());
		if ( timeCompare == 0) {
			return Integer.compare(m1.getServerId(), m2.getServerId());
		}else {
			return timeCompare;
		}
	}
	
}