package i5.las2peer.services.calendar;

import java.io.Serializable;
import java.util.ArrayList;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;

public class EntryBox implements Serializable {
		
		private static final long serialVersionUID = -300617519857096303L;
		private final ArrayList<String> entries;
		
		/**
		 * Constructor to create the storage of the entries with capacity
		 * @param storageSize 
		 */
		public EntryBox(int storageSize){
			entries = new ArrayList<>(storageSize);
		}
		
		/**
		 * adds an Entry to the storage
		 * 
		 * @param newEntry
		 * 		       entry that should be stored
		 */
		public void addEntry(Entry newEntry){
			entries.add(newEntry.toXmlString());
		}
		
		/**
		 * get Entry by putting in ID
		 * @param id 
		 * 			   entry that should be returned
		 * @return 
		 */
		public Entry returnEntry(String id){
			try{
			for(String anEntry : entries) {
				if(anEntry.contains(id)){
					return Entry.readJSON(anEntry);
				}
			}
			
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "could not find entry");
			return null;
			}
			catch(Exception e){
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "can't parse massage from XML " + e.toString());
				return null;
			}
		}
		
		/**
		 * get an entry with all the entries of the storage
		 * 
		 * @return array with all entries of the storage
		 */
		public Entry[] getEntries(){
			ArrayList<Entry> result = new ArrayList<>(entries.size());
			for (String json : entries) {
				try {
					Entry newEntry = Entry.readJSON(json);
					result.add(newEntry);
				}
				catch (Exception e){
					L2pLogger.logEvent(this, Event.SERVICE_ERROR, "can't parse massage from JSON '" + json);
				}
		   }
			
			Entry[] resultArray = result.toArray(new Entry[0]);
			return resultArray;
			
		}
		
		/**
		 * delete an entry by putting in its id
		 * 
		 * return whether or not entry was found
		 * @param id 
		 * @return 
		 */
		public boolean delete(String id){
			for(String anEntry : entries) {
				if(anEntry.contains(id)){
					
					entries.remove(anEntry);
					return true;
				}
			}
			
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, "id wasn't found'");
			return false;
		}
		
		
		/**
		 * returns the size of the entry storage
		 * @return 
		 */
		public int size(){
			return entries.size();
		}
		
		/**
		 * deletes all entries in storage
		 */
		public void delete(){
			entries.clear();
		}
		
		/**
		 * find the entry id for a comment i
		 * return the id of the entry
		 * @param commentID 
		 * @return 
		 */
		public String findComment(String commentID){
			for(String anEntry: entries){
				if(anEntry.contains(commentID)){
					return anEntry;
				}
			}
			return "";
		}
		
		
		
	
}
