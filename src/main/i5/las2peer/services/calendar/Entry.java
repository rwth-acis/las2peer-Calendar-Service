package i5.las2peer.services.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.services.calendar.MyCalendar;
import i5.las2peer.services.calendar.security.IdGeneration;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;

/**
 * Class that represents a date created by the user. It will be used by the {@link MyCalendar} to organize dates.<br>
 * The class contains the start and of a date. Furthermore it contains the id of the creator.
 * 
 */
public class Entry implements XmlAble {
	
	/** unique String id to identify this entry**/
	private String uniqueID;
	/** agent id of the agent who created the entry**/
	private final long creatorId;
	/** start date of the calendar entry**/
	private Calendar start;
	/** end date of the calendar entry**/
	private Calendar end;
	/** title of the entry**/
	private String title;
	/** description of the entry**/
	private String description;
	/** list with comments of the calendar entry**/
	private final ArrayList<Comment> comments;
	
	public int commentAmount;
	
	/**
	 * Gets the title of this {@link Comment} .
	 * 
	 * @return the title of the comment.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title of this {@link Comment}.
	 * 
	 * @param title
	 *			 new title 
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the description of this {@link Comment} .
	 * 
	 * @return the description of the comment.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description of this {@link Comment}.
	 * 
	 * @param description
	 *			 new description. 
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * returns a list of the comments of this {@link Comment}.
	 * 
	 * @return a list of the comments.
	 */
	public ArrayList<Comment> getComments() {
		return comments;
	}
	
	/**
	 * Constructor for a {@link Entry}. 
	 * 
	 * @param idCreatedBy
	 *            user agent id who created the entry
	 * @param title
	 * 		      title of the new entry
	 * @param description
	 * 			  description of the new entry
	 * @param amount
	 * 			  amount of comments that can be added
	 * 				           
	 */
	
	public Entry(long idCreatedBy, String title, String description, int amount){
		this.uniqueID = IdGeneration.createID();
		commentAmount = amount;
		this.creatorId = idCreatedBy;
		this.comments = new ArrayList<Comment>(amount);
		this.title = title;
		this.description = description;
	}
	
	public Entry(String uniqueID, long idCreatedBy, String title, String description, int amount){
		this.uniqueID = uniqueID;
		commentAmount = amount;
		this.creatorId = idCreatedBy;
		this.comments = new ArrayList<Comment>(amount);
		this.title = title;
		this.description = description;
	}
	
	public String getUniqueID() {
		return uniqueID;
	}

	/**
	 * Gets the id of the user agent who created this {@link Entry} .
	 * 
	 * @return the user agent id
	 */
	public long getCreatorId() {
		return creatorId;
	}
	
	/**
	 * Gets the start date of this {@link Entry} .
	 * 
	 * @return the start date
	 */
	public Calendar getStart() {
		return this.start;
	}
	
	/**
	 * Set the start date of this {@link Entry} .
	 * 
	 * @param year
	 *			 year in which the entry begins
	 * @param month
	 *			 month in which the entry begins
	 * @param dayOfMonth
	 *			 day of month in which the entry begins
	 * @param hour
	 * 			 hour in which the entry begins
	 * @param minute
	 * 			 minute in which the entry begins
	 * @return whether or not start date could be set
	 */
	public boolean setStart(int year, int month, int dayOfMonth, int hour, int minute) {
		
		GregorianCalendar date = new GregorianCalendar(year, month, dayOfMonth, hour, minute);
		if(this.getEnd()!=null){ // if endDate has been set already and is before start date
			if(this.getEnd().before(date)){
				return false;
			}
		}
		
		this.start = date;
		return true;
	
	}
	
	/**
	 * Gets the end date of this {@link Entry} .
	 * 
	 * @return the end date
	 */
	public Calendar getEnd() {
		return this.end;
	}

	/**
	 * Set the end date of this {@link Entry} .
	 * 
	 * @param year
	 *			 year in which the entry ends
	 * @param month
	 *			 month in which the entry ends
	 * @param dayOfMonth
	 *			 day of month in which the entry ends
	 * @param hour
	 * 			 hour in which the entry ends
	 * @param minute
	 * 			 minute in which the entry ends
	 * @return whether or not end date could be set
	 */
	public boolean setEnd(int year, int month, int dayOfMonth, int hour, int minute) {
		
		GregorianCalendar date = new GregorianCalendar(year, month, dayOfMonth, hour, minute);
		if(this.getStart()!= null){ // if start date exists and is before end date
			if(this.getStart().after(date)){
			   return false;
			}
		}
		
		this.end = date;
		return true;
	}
	
	
	/**
	 * creates a comment of this entry
	 * 
	 * @param id
	 * 			the id of the current agent 	 
	 * @param comment
	 * 			the content of the comment
	 * @return
	 * 			the id of the comment. nothing if comment was empty
	 */
	
	public String createComment(long id, String comment){
		
		if(this.comments.size() >= commentAmount){
			return "";
		}
		
		if(comment == null || comment == ""){ //comment cannot be empty
			return "";
		}
		
		GregorianCalendar stamp = new GregorianCalendar();
		
		Comment c1 = new Comment(id, stamp, comment);
		this.comments.add(c1);
		return c1.getUniqueID();
		
	}
	
	/**
	 * deletes a comment of this entry
	 * 
	 * @param id
	 * 			the id of the comment that shall be deleted 	 
	 * @return
	 * 			if the comment could be found and deleted
	 */
	
	public boolean deleteComment(String id){
		
		for(Comment aComment: this.comments){
			if(aComment.getUniqueID().equals(id)){	
				this.comments.remove(aComment);
				return true;	
			}	
		}
		return false;
	}
	
	@Override
	public String toXmlString(){ //no date or comments at the moment
		
		return "<las2peer:entry id=\"" + this.uniqueID + "\" creator=\"" + this.creatorId 
				+ "\" description=\"" + this.description +"\">" + this.title + "</las2peer:entry>\n";
	}
	
	public static Entry createFromXml(String xml) throws MalformedXMLException {
		
		try{
			
			Element root = Parser.parse(xml, false);
			Element child = root.getFirstChild();
			
			String uniqueID = root.getAttribute("id");
			long creator = Long.parseLong(root.getAttribute("creator"));
			String title = child.getText();
			String description = root.getAttribute("description");
			
			Entry xmlEntry = new Entry(uniqueID, creator, title, description, 2);
			return xmlEntry;
			
		}
		
		catch(Exception e){
 			return null;
		}
	}
	
}
