package i5.las2peer.services.calendar.security;

import java.util.UUID;

public class IdGeneration {
	
	public static String createID(){
		UUID id = UUID.randomUUID();
		String newId = id.toString();
		return newId;
	}

}
