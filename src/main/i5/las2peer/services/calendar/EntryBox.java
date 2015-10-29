package i5.las2peer.services.calendar;

import java.io.Serializable;
import java.util.ArrayList;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Context;

public class EntryBox implements Serializable {
		
		private static final long serialVersionUID = -300617519857096303L;
		private final ArrayList<String> entries;
		
		/**
		 * Constructor to create the storage of the entries with capacity
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
		 * 
		 * @param newEntry
		 * 			   entry that should be returned
		 */
		public Entry returnEntry(String id){
			try{
			for(String anEntry : entries) {
				if(anEntry.contains(id)){
					return Entry.createFromXml(anEntry);
				}
			}
			Context.logError(this, "couldn't find entry '");
			return null;
			}
			catch(Exception e){
				Context.logError(this, "can't parse massage from XML '");
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
			for (String xml : entries) {
				try {
					Entry newEntry = Entry.createFromXml(xml);
					result.add(newEntry);
				}
				catch (MalformedXMLException e){
					Context.logError(this, "can't parse massage from XML '" + xml);
				}
		   }
			
			Entry[] resultArray = result.toArray(new Entry[0]);
			return resultArray;
			
		}
		
		/**
		 * delete an entry by putting in its id
		 * 
		 * return whether or not entry was found
		 */
		public boolean delete(String id){
			for(String anEntry : entries) {
				if(anEntry.contains(id)){
					
					entries.remove(anEntry);
					return true;
				}
			}
			Context.logError(this, "id wasn't found'");
			return false;
		}
		
		
		/**
		 * returns the size of the entry storage
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
