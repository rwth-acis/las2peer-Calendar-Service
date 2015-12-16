package i5.las2peer.services.calendar.database;

import java.util.ArrayList;
import java.util.Calendar;

import i5.las2peer.services.calendar.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class Serialization {
	
	public static JSONObject serializeEntry(Entry a) {
		
		JSONObject res = new JSONObject();
		
		res.put("entry_id", a.getUniqueID());
		res.put("title", a.getTitle());
		res.put("description", a.getDescription());
		res.put("creator", a.getCreatorId());
		if(a.getComments().isEmpty()==false){
		res.put("comments", a.getComments());
		}
		else{
		ArrayList<Comment> empty = new ArrayList<>(10);
		res.put("comments", empty);
		}
		if(a.getStart()!=null){
		res.put("syear", a.getStart().get(Calendar.YEAR));
		res.put("smonth", a.getStart().get(Calendar.MONTH));
		res.put("sday", a.getStart().get(Calendar.DAY_OF_MONTH));
		res.put("shour", a.getStart().get(Calendar.HOUR));
		res.put("sminute", a.getStart().get(Calendar.MINUTE));
		}
		
		if(a.getEnd()!=null){
		res.put("eyear", a.getEnd().get(Calendar.YEAR));
		res.put("emonth", a.getEnd().get(Calendar.MONTH));
		res.put("eday", a.getEnd().get(Calendar.DAY_OF_MONTH));
		res.put("ehour",a.getEnd().get(Calendar.HOUR));
		res.put("eminute",a.getEnd().get(Calendar.MINUTE));
		}
		
		return res;
	}

	public static JSONArray serializeEntries(ArrayList<Entry> entries){
		
		JSONArray res = new JSONArray();
		
		for(Entry a : entries){
			res.add(serializeEntry(a));
		}
		
		return res;
		
	}
	
	
}
