package i5.las2peer.services.calendar.database;

import i5.las2peer.services.calendar.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class Serialization {
	
	public static JSONObject serializeEntry(Entry a) {
		
		JSONObject res = new JSONObject();
		
		res.put("entry_id", a.getUniqueID());
		res.put("title", a.getTitle());
		res.put("description", a.getDescription());
		
		return res;
	}

}
