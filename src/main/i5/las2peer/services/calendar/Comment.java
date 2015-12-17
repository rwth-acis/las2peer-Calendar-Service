package i5.las2peer.services.calendar;

import java.util.Calendar;

import i5.las2peer.services.calendar.security.IdGeneration;

/**
 * Class that represents a comment inside of an entry. The class 
 * holds a string and a timestamp. It also saves who created the comment.
 * 
 */

public class Comment {
	
	/** unique String Id to identify this comment**/
	private String uniqueID;
	/** agent id of the agent who created the comment**/
	private long creatorId;
	/** time at which the comment was created**/
	private Calendar time;
	/** the comment itself saved as a string*/
	private String message;
	
	/**
	 * Constructor for a {@link Comment}. 
	 * 
	 * @param creatorId
	 *            id of the user agent who created this comment
	 * @param time
	 *            time at which the comment was posted
	 * @param message
	 *            the comment itself
	 * 				           
	 */
	
	public Comment(long creatorId, Calendar time, String message) {
		super();
		this.uniqueID = IdGeneration.createID();
		this.creatorId = creatorId;
		this.time = time;
		this.message = message;
	}
	
	public Comment(String uniqueID, long creatorId, Calendar time, String message) {
		super();
		this.uniqueID = uniqueID;
		this.creatorId = creatorId;
		this.time = time;
		this.message = message;
	}

	public long getCreatorId() {
		return creatorId;
	}
	
	public String getUniqueID() {
		return uniqueID;
	}

	public Calendar getTime() {
		return time;
	}

	public String getMessage() {
		return message;
	}

}
