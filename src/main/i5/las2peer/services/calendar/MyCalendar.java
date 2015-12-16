package i5.las2peer.services.calendar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import i5.las2peer.api.Service;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.security.Context;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.calendar.database.DatabaseManager;
import i5.las2peer.services.calendar.database.Serialization;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import i5.las2peer.security.UserAgent;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.api.Service;


/**
 * LAS2peer Calendaqr Service
 * 
 * This is a simple calendar service to store 
 * entries inside.
 * 
 * Note:
 * If you plan on using Swagger you should adapt the information below
 * in the ApiInfo annotation to suit your project.
 * If you do not intend to provide a Swagger documentation of your service API,
 * the entire ApiInfo annotation should be removed.
 * 
 */
@Path("/example")
@Version("0.1") // this annotation is used by the XML mapper
@Api
@SwaggerDefinition(
		info = @Info(
				title = "LAS2peer Calendar Service",
				version = "0.2",
				description = "A LAS2peer Calendar Service.",
				termsOfService = "http://your-terms-of-service-url.com",
				contact = @Contact(
						name = "Melvin Bender",
						url = "dbis.rwth-aachen.de",
						email = "bender@dbis.rwth-aachen.de"
				),
				license = @License(
						name = "your software license name",
						url = "http://your-software-license-url.com"
				)
		))
public class MyCalendar extends Service {

	/**
	 * service properties with default values, can be overwritten with properties file
	 * config/MyCalendar.properties
	 */
	public int MAXIMUM_COMMENT_AMOUNT = 100;
	/** list with entries of the calendar **/
	private final ArrayList<Entry> entries = new ArrayList<Entry>(MAXIMUM_COMMENT_AMOUNT);
	private final String STORAGE_NAME = "entrystorage";
	
	
	public ArrayList<Entry> getEntries() {
		return entries;
	}

	/*
	 * Database configuration
	 */
	private String jdbcDriverClassName;
	private String jdbcLogin;
	private String jdbcPass;
	private String jdbcUrl;
	private String jdbcSchema;
	private DatabaseManager dbm;

	public MyCalendar() {
		// read and set properties values
		// IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED TO BE CHANGED TOO!
		setFieldValues();
		// instantiate a database manager to handle database connection pooling and credentials
		dbm = new DatabaseManager(jdbcDriverClassName, jdbcLogin, jdbcPass, jdbcUrl, jdbcSchema);
	}
	
	public UserAgent getUserAgent(){
		return (UserAgent) getActiveAgent();
	}
	
	public void createEntry(String title, String description, String year, String month, String day, 
							String sHour, String sMinute, String eHour, String eMinute){
		String result = create(title, description).getResult();
		JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
		
		try{
		JSONObject params = (JSONObject)parser.parse(result);
		String id = (String) params.get("entry_id");
		
		result = setStart(id, year, month, day, sHour,sMinute).getResult();
		result = setEnd(id, year, month, day, eHour, eMinute).getResult();
		} catch (Exception e){
			return;
		}
	}
	
	// returns an entry within the calendar with an id
	public Entry retrieveEntry(String id) {
		for(Entry anEntry: this.getEntries()){
			if(anEntry.getUniqueID().equals(id)){
				return anEntry;
			}
		}
		
		return null; //id wasn't found within an entry
	}
	
	// returns a comment within the calendar with an id
		public Comment retrieveComment(String id) {
			for(Entry anEntry: this.getEntries()){
				for(Comment aComment: anEntry.getComments()){
					if(aComment.getUniqueID().equals(id)){
						return aComment;
					}
				}
			}
			
			return null; //id wasn't found within an entry
		}
	
	// //////////////////////////////////////////////////////////////////////////////////////
	// Service methods.
	// //////////////////////////////////////////////////////////////////////////////////////

		
	/**
	 * Creates an entry and saves it permanently in the node storage.
	 * THIS DOES NOT WORK YET UNFORTUNATELY BUT THIS IS THE WAY TO GO
	 * To create temporary entries use the method createEntry.
	 * 
	 * @param title
	 * 			title of the new entry
	 * @param description
	 * 			description of the new entry
	 * @return
	 * 		   whether or not creation was successful
	 */
		
	@POST
	@Path("/create/{title}/{description}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@ApiOperation(value = "create",
			notes = "Create an entry inside a Calendar")
	public HttpResponse create( @PathParam("title") String title, @PathParam ("description") String description){
		 
		 if((title.equals("")) || (description.equals(""))){
			 return new HttpResponse ("one of the parameters is empty", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
		 
		 Entry newEntry = new Entry(getActiveAgent().getId(), title, description, MAXIMUM_COMMENT_AMOUNT);
		 
		 // save entry using envelopes
		 try{
			 
			 Envelope env = null;
			 
			 try{
				 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			 }
			 
			 catch (Exception e){
				 Context.logMessage(this, "Network storage not found. Creating new one. " + e);
				 env = Envelope.createClassIdEnvelope(new EntryBox(1), STORAGE_NAME, getAgent());
			 }
			 
			 env.open(getAgent());
			 EntryBox stored = env.getContent(EntryBox.class);
			 stored.addEntry(newEntry);
			 env.updateContent(stored);
			 env.addSignature(getAgent());
			 env.store();
			 env.close();
			 
			 JSONObject toString = Serialization.serializeEntry(newEntry);
			 
			 Context.logMessage(this, "stored " + stored.size() + " entries in network storage");
			 return new HttpResponse(toString.toJSONString(), HttpURLConnection.HTTP_OK);
		     } catch (Exception e) {
				Context.logError(this, "Can't persist entries to network storage! " + e.getMessage());
				return new HttpResponse("error" + e, HttpURLConnection.HTTP_BAD_REQUEST);
		     }
	}
		
	
	/**
	 * gets the entry by the id
	 */
	@GET
	@Path("/getEntry/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@ApiOperation(value = "getEntry",
	notes = "finds an Entry by an id")
	public HttpResponse getEntry( @PathParam("id") String id){
	
		try{

			Envelope env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			env.open(getAgent());
			EntryBox stored = env.getContent(EntryBox.class);
			Entry returnEntry = stored.returnEntry(id);
			if(returnEntry == null){
				return new HttpResponse("Couldn't find entry with id: " + id, HttpURLConnection.HTTP_NOT_FOUND);
			}
			
			JSONObject res = Serialization.serializeEntry(returnEntry);
			String returnString = res.toJSONString();
			env.close();
			
			return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
		
		}
		catch(Exception e){
			return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
		}
	}
	
	/**
	 * deletes an entry inside the calendar
	 * 
	 * @param id
	 * 			  the id of the entry
	 * @return success or error message
	 */
	@DELETE
	@Path("/deleteEntry/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "Deletion forbidden"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@ApiOperation(value = "deleteEntry",
	notes = "deletes an entry")
	public HttpResponse deleteEntry( @PathParam("id") String id) {
		
		try{

			 Envelope env = null;
			 
			 try{
				 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			 }
			 
			 catch (Exception e){
				 Context.logMessage(this, "Network storage not found. " + e);
				 return new HttpResponse("There is not network storage yet", HttpURLConnection.HTTP_BAD_REQUEST);
			 }
			 
			 env.open(getAgent());
			 EntryBox stored = env.getContent(EntryBox.class);
			 Entry toDelete = stored.returnEntry(id);
			 if(toDelete.getCreatorId()!=getActiveAgent().getId()){
				 Context.logMessage(this, "cannot delete this entry by another user");
				 return new HttpResponse("entry couldn't be deleted", HttpURLConnection.HTTP_FORBIDDEN);
			 }
			 boolean result = stored.delete(id);
			 env.updateContent(stored);
			 env.addSignature(getAgent());
			 env.store();
			 env.close();
			 
			 if(result==true){
			 Context.logMessage(this, "deleted" + stored.size() + " entries in network storage");
			 return new HttpResponse(Serialization.serializeEntry(toDelete).toJSONString(), HttpURLConnection.HTTP_OK);
			 }
			 
			 else{
				 
				 return new HttpResponse("entry with id: " + id +" wasn't found", HttpURLConnection.HTTP_NOT_FOUND);
				 
			 }
			 
		     } catch (Exception e) {
				Context.logError(this, "Couldn't delete the entry" + e.getMessage());
				return new HttpResponse("error" + e, HttpURLConnection.HTTP_BAD_REQUEST);
		     }
		
	}
	
	/**
	 * gets the number of entries in the calendar
	 * 
	 * @return success or error message
	 */
	@GET
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry number"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@Path("/getNumber")
	@ApiOperation(value = "getNumber",
	notes = "get Number of entries")
	public HttpResponse getNumberOfEntries(){
		
		Envelope env = null;
		 
		 try{ //try to load the entryBox
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		 }
		 
		 catch (Exception e){
			 Context.logMessage(this, "Network storage not found." + e);
			 return new HttpResponse("0", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
		 
		 try{
			 
		 env.open(getAgent());
		 EntryBox stored = env.getContent(EntryBox.class);
		 int size = stored.size();
		 env.close();
		 return new HttpResponse("The amount of entries is: " + size, HttpURLConnection.HTTP_OK);
		 
		 }
		 
		 catch(Exception e){
			 Context.logError(this, "Can't read messages from storage");
		     }
		 return new HttpResponse("GetNumber Fail", HttpURLConnection.HTTP_BAD_REQUEST);
	}
	
	
	/**
	 * Sets the start date of an entry
	 * 
	 * @param id
	 * 			  the id of the entry whose start date is supposed to be changed
	 * @param year
	 * 			  year of the start date
	 * @param month
	 * 			  month of the start date
	 * @param day 
	 *            day in the month of the start date
	 * @param hour 
	 * 			  hour of the start date
	 * @param minute 
	 * 			  minute of the start date
	 * 
	 * @return success or error message
	 */
	
	@PUT
	@Path("/setStart/{id}/{year}/{month}/{day}/{hour}/{minute}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "start date set"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found")
	})
	@ApiOperation(value = "setStart",
	notes = "set startdate of entry")
	public HttpResponse setStart( @PathParam("id") String id, @PathParam ("year") String year, @PathParam ("month") String month,
			@PathParam ("day") String day, @PathParam ("hour") String hour, @PathParam ("minute") String minute)
	{
		
			int yearInt   = Integer.parseInt(year);
			int monthInt  = Integer.parseInt(month);
			monthInt--; //Calender month start at 0 so reduce
			int dayInt    = Integer.parseInt(day);
			int hourInt   = Integer.parseInt(hour);
			int minuteInt = Integer.parseInt(minute);
			
			 Envelope env = null;
			 
			 try{
				 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			 }
			 
			 catch (Exception e){
				 Context.logMessage(this, "there is not a storage yet");
				 return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
			 }
			
			 try{
				 
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			
			 env.open(getAgent());
			 EntryBox stored = env.getContent(EntryBox.class);
			 Entry updatedEntry = stored.returnEntry(id); //get the entry whose start date is supposed to be stored
			 updatedEntry.setStart(yearInt, monthInt, dayInt, hourInt, minuteInt);
			 stored.delete(id);
			 stored.addEntry(updatedEntry);
			 JSONObject entry = Serialization.serializeEntry(updatedEntry);
			 String rest = entry.toJSONString();
			 env.updateContent(stored);
			 env.addSignature(getAgent());
			 env.store();
			 env.close();
			 
			 Context.logMessage(this, "stored " + stored.size() + " entries in network storage");
			 return new HttpResponse(rest, HttpURLConnection.HTTP_OK);
			 } 
			 catch(Exception e){
				 Context.logMessage(this, "couldn't open the storage");
				 return new HttpResponse("entry could not be found", HttpURLConnection.HTTP_NOT_FOUND);
			 }
	}
	
	
	/**
	 * Sets the end date of an entry
	 * 
	 * @param id
	 * 			  the id of the entry whose end date is supposed to be changed
	 * @param year
	 * 			  year of the end date
	 * @param month
	 * 			  month of the end date
	 * @param day 
	 *            day in the month of the end date
	 * @param hour 
	 * 			  hour of the end date
	 * @param minute 
	 * 			  minute of the end date
	 * 
	 * @return success or error message
	 */
	
	@PUT
	@Path("/setEnd/{id}/{year}/{month}/{day}/{hour}/{minute}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "end date set"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found")
	})
	@ApiOperation(value = "setEnd",
	notes = "set end date of entry")
	public HttpResponse setEnd( @PathParam("id") String id, @PathParam ("year") String year, @PathParam ("month") String month,
			@PathParam ("day") String day, @PathParam ("hour") String hour, @PathParam ("minute") String minute)
	{
		
			int yearInt   = Integer.parseInt(year);
			int monthInt  = Integer.parseInt(month);
			monthInt--;  //Calendar starts from 0 so reduce
			int dayInt    = Integer.parseInt(day);
			int hourInt   = Integer.parseInt(hour);
			int minuteInt = Integer.parseInt(minute);
			
			 Envelope env = null;
			 
			 try{
				 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			 }
			 
			 catch (Exception e){
				 Context.logMessage(this, "there is not storage yet");
				 return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
			 }
			
			 try{
				 
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			
			 env.open(getAgent());
			 EntryBox stored = env.getContent(EntryBox.class);
			 Entry updatedEntry = stored.returnEntry(id); //get the entry whose end date is supposed to be stored
			 updatedEntry.setEnd(yearInt, monthInt, dayInt, hourInt, minuteInt);
			 JSONObject entry = Serialization.serializeEntry(updatedEntry);
			 String rest = entry.toJSONString();
			 stored.delete(id);
			 stored.addEntry(updatedEntry);
			 
			 env.updateContent(stored);
			 env.addSignature(getAgent());
			 env.store();
			 env.close();
			 
			 Context.logMessage(this, "stored " + stored.size() + " entries in network storage");
			 return new HttpResponse(rest, HttpURLConnection.HTTP_OK);
			 }
			 catch(Exception e){
				 Context.logMessage(this, "couldn't open the storage");
				 return new HttpResponse("entry could not be found", HttpURLConnection.HTTP_NOT_FOUND);
			 }
	}
	
	/**
	 * Create a comment for an entry
	 * 
	 * @param id
	 * 			  the id of the entry which a comment is to be created for
	 * @param comment
	 * 			  the content of the comment
	 * @return success or error message
	 */
	@POST
	@Path("/createComment/{id}/{comment}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry number"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found")
	})
	@ApiOperation(value = "createComment",
	notes = "add comment to entry")
	public HttpResponse createComment( @PathParam("id") String id, @PathParam("comment") String comment) {
		
		Envelope env = null;
		 
		 try{
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		 }
		 
		 catch (Exception e){
			 Context.logMessage(this, "there is not storage yet");
			 return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
		
		 try{
			 
		 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		
		 env.open(getAgent());
		 EntryBox stored = env.getContent(EntryBox.class);
		 Entry updatedEntry = stored.returnEntry(id); //get the entry where a comment shall be written to
		 if(updatedEntry==null){
			 return new HttpResponse("Could not find entry to comment on ", HttpURLConnection.HTTP_NOT_FOUND);
		 }
		 updatedEntry.createComment(getActiveAgent().getId(), comment); //add the comment
		 stored.delete(id); //delete the former entry
		 stored.addEntry(updatedEntry); //upload the new entry
		 JSONObject entry = Serialization.serializeEntry(updatedEntry);
		 String rest = entry.toJSONString();

		 env.updateContent(stored);
		 env.addSignature(getAgent());
		 env.store();
		 env.close();
		 
		 Context.logMessage(this, "stored " + stored.size() + " entries in network storage");
		 return new HttpResponse(rest, HttpURLConnection.HTTP_OK);
		 } 
		 catch(Exception e){
			 Context.logMessage(this, "couldn't open the storage");
			 return new HttpResponse("could not use storage", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
	}
	
	/**
	 * Delete a comment
	 * 
	 * @param id
	 * 			  the id of the comment that shall be deleted
	 * @return success or error message
	 */
	@DELETE
	@Path("/deleteComment/{id}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry number"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_FORBIDDEN, message = "not allowed")
	})
	@ApiOperation(value = "deleteComment",
	notes = "delete comment of entry")
	public HttpResponse deleteComment( @PathParam("id") String id) {
		
		Envelope env = null;
		 
		 try{
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		 }
		 
		 catch (Exception e){
			 Context.logMessage(this, "there is not storage yet");
			 return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
		
		 try{
			 
		 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		
		 env.open(getAgent());
		 EntryBox stored = env.getContent(EntryBox.class);
		 String entryID = stored.findComment(id);
		 if(entryID.equals("")){
			 Context.logMessage(this, "Comment was not found");
			 return new HttpResponse("Comment was not found", HttpURLConnection.HTTP_NOT_FOUND);
		 }
		 
		 Entry newEntry = stored.returnEntry(entryID);
		 Comment deleteComment = newEntry.returnComment(id);
		 if((deleteComment.getCreatorId()!=getActiveAgent().getId()) && (newEntry.getCreatorId()!=getActiveAgent().getId())){
			 
				 Context.logMessage(this, "cannot delete this comment by another user");
				 return new HttpResponse("comment couldn't be deleted", HttpURLConnection.HTTP_FORBIDDEN);
				 
		 }
		 newEntry.deleteComment(id);
		 stored.delete(entryID); //delete the former entry
		 stored.addEntry(newEntry); //upload the new entry
		 
		 env.updateContent(stored);
		 env.addSignature(getAgent());
		 env.store();
		 env.close();
		 
		 Context.logMessage(this, "stored " + stored.size() + " entries in network storage");
		 return new HttpResponse("entry with id:" + id +":was sucessfully changed. ", HttpURLConnection.HTTP_OK);
		 } 
		 catch(Exception e){
			 Context.logMessage(this, "couldn't open the storage");
			 return new HttpResponse("entry could not be found", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
	}
	
	
	
	/**
	 * get all the ids of the entries on a certain day
	 * 
	 */
	@GET
	@Path("/getDay/{year}/{month}/{day}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry number"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@ApiOperation(value = "getDay",
	notes = "get all entries of a day")
	public HttpResponse getDay ( @PathParam("year") String year, @PathParam ("month") String month, @PathParam("day") String day){
		
		Envelope env = null;
		
		 try{
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		 }
		 
		 catch (Exception e){
			 Context.logMessage(this, "there is not storage yet");
			 return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
		 
		 try{
			 
			 ArrayList<Entry> entryList = new ArrayList<>();
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			
			 env.open(getAgent());
			 EntryBox stored = env.getContent(EntryBox.class);
			 
			 Entry[] entries = stored.getEntries();
			 int yearInt = Integer.parseInt(year);
			 int monthInt = Integer.parseInt(month);
			 monthInt--;
			 int dayInt = Integer.parseInt(day);
			 GregorianCalendar dayDate = new GregorianCalendar(yearInt, monthInt, dayInt);
			 
		
		for(Entry anEntry: entries){
			if((anEntry.getEnd()!= null) && (anEntry.getStart() != null)) {
			Calendar date = anEntry.getStart(); 
			Calendar end = anEntry.getEnd();
			if((date.get(Calendar.YEAR) == yearInt) && (date.get(Calendar.MONTH) == monthInt) && (date.get(Calendar.DAY_OF_MONTH) == dayInt)){ // if entry starts on the day
	
						entryList.add(anEntry);
			
			}
			
			else if((end.get(Calendar.YEAR) == yearInt) && (end.get(Calendar.MONTH) == monthInt) && (end.get(Calendar.DAY_OF_MONTH) == dayInt)){ // if entry ends on the day
				
						entryList.add(anEntry);
				
			}
			
			else{  //if entry starts before the day and ends after the day
				if(date.before(dayDate) && end.after(dayDate)){
					entryList.add(anEntry);
				}
			}
		  }
		}
	
		
		String returnString = Serialization.serializeEntries(entryList).toJSONString();
		
		return new HttpResponse (returnString, HttpURLConnection.HTTP_OK);
		
		}
		 
		catch(Exception e){
			 Context.logMessage(this, "couldn't open the storage" + e.getMessage());
			 return new HttpResponse("entry could not be found" + e.getMessage(), HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
	}
	
	
	/**
	 * method to create entries on a regular basis
	 * 
	 * @param year
	 * 			the year in which the dates should start
	 * @param sMonth
	 * 			starting month of the first date
	 * @param day
	 * 			starting day of the month of the first date
	 * @param lengthWeek
	 * 			the amount of entries that should be created (one per week)
	 * @param sHour
	 * 			the starting hour of the entries
	 * @param sMinute
	 * 			the starting minute of the entries
	 * @param eHour
	 * 			the end hour of the entries
	 * @param eMinute
	 * 			the ending minute of the entries
	 * @param title
	 * 			the title of the entries
	 * @param description
	 * 			the description of the entries
	 * @param interval	
	 * 			the interval between the dates
	 */
	@POST
	@Path("/createRegular/{year}/{month}/{day}/{startHour}/{startMinute}/{endHour}/{endMinute}/{title}/{description}/{interval}/{number}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entries created"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@ApiOperation(value = "create regular entries",
	notes = "creates entries on a regular basis")
	public HttpResponse createRegular ( @PathParam("year") String year, @PathParam("month") String sMonth, 
									   @PathParam("day") String day, 
									   @PathParam ("startHour") String sHour,
									   @PathParam("startMinute") String sMinute, @PathParam("endHour") String eHour, 
									   @PathParam("endMinute") String eMinute, @PathParam("title") String title, @PathParam("description") String description,
									   @PathParam("interval") String interval, @PathParam("number") String number){

		//parse Strings to integers
		int startYear, month,  startDay, startHour, startMinute, endHour, endMinute, numbers;
		
		try{
		startYear = Integer.parseInt(year);
		month = Integer.parseInt(sMonth);
		numbers = Integer.parseInt(number);
		startDay = Integer.parseInt(day);
		startHour = Integer.parseInt(sHour);
		startMinute = Integer.parseInt(sMinute);
		endHour = Integer.parseInt(eHour);
		endMinute = Integer.parseInt(eMinute);
		} catch (NumberFormatException e){
			String error = "strings invalid";
			return new HttpResponse(error, HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
		GregorianCalendar start = new GregorianCalendar(startYear, month, startDay, startHour, startMinute);
		GregorianCalendar end = new GregorianCalendar(startYear, month, startDay, endHour, endMinute);
		
		if(start.after(end)){
			return new HttpResponse("start was after end", HttpURLConnection.HTTP_BAD_REQUEST);
		}

		
		switch (interval) {
		case "week": 
			createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.DATE, 7);
				year = Integer.toString(start.get(Calendar.YEAR));
				sMonth = Integer.toString(start.get(Calendar.MONTH));
				day = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
				numbers--;
			}
			
			break;
			
			
		case "month":
			createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.MONTH, 1);
				year = Integer.toString(start.get(Calendar.YEAR));
				sMonth = Integer.toString(start.get(Calendar.MONTH));
				day = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
				numbers--;
			}
			
			break;
		
		case "year":
			
			createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.YEAR, 1);
				year = Integer.toString(start.get(Calendar.YEAR));
				sMonth = Integer.toString(start.get(Calendar.MONTH));
				day = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
				numbers--;
			}
			
			break;
		case "biweek": 

			createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.DATE, 14);
				year = Integer.toString(start.get(Calendar.YEAR));
				sMonth = Integer.toString(start.get(Calendar.MONTH));
				day = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
				numbers--;
			}
			
			break;
		
		case "bimonth": 
		createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
		numbers--;
		
		while(numbers>(0)){
			start.add(Calendar.MONTH, 2);
			year = Integer.toString(start.get(Calendar.YEAR));
			sMonth = Integer.toString(start.get(Calendar.MONTH));
			day = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
			createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
			numbers--;
		}
		
		break;		
		case "quarter":
			
			createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.MONTH, 3);
				year = Integer.toString(start.get(Calendar.YEAR));
				sMonth = Integer.toString(start.get(Calendar.MONTH));
				day = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, year, sMonth, day, sHour, sMinute, eHour, eMinute);
				numbers--;
			}
			
			break;
			
		default: String error = "invalid interval";
				 return new HttpResponse(error, HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
		String error = "entries successfully created";
		return new HttpResponse(error, HttpURLConnection.HTTP_OK);
		
	}
	
	
	/**
	 * function to get the login name of an agent
	 * 
	 * @param id
	 * 		the id of the agent 
	 * @return
	 *		the login name
	 */
	@GET
	@Path("/name/{id}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Name"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Not Found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal error")
	})
	@ApiOperation(value = "name",
			notes = "get the name of an agent")
	public HttpResponse getName( @PathParam("id") String id){
		 
		long agentid = Long.parseLong(id);
	    try{
	    	UserAgent fred = (UserAgent) getContext().getAgent(agentid);
	    	String name = fred.getLoginName();
	    	return new HttpResponse(name, HttpURLConnection.HTTP_OK);
	    }
	    catch(AgentNotKnownException e){
	    	String error = "Agent not found";
	    	return new HttpResponse(error, HttpURLConnection.HTTP_NOT_FOUND);
	    }
	    catch(Exception e){
	    	String error = "Internal error";
	    	return new HttpResponse(error, HttpURLConnection.HTTP_INTERNAL_ERROR);
	    }
	}
	
	// //////////////////////////////////////////////////////////////////////////////////////
	// Methods required by the LAS2peer framework.
	// //////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Method for debugging purposes.
	 * Here the concept of restMapping validation is shown.
	 * It is important to check, if all annotations are correct and consistent.
	 * Otherwise the service will not be accessible by the WebConnector.
	 * Best to do it in the unit tests.
	 * To avoid being overlooked/ignored the method is implemented here and not in the test section.
	 * @return  true, if mapping correct
	 */
	public boolean debugMapping() {
		String XML_LOCATION = "./restMapping.xml";
		String xml = getRESTMapping();

		try {
			RESTMapper.writeFile(XML_LOCATION, xml);
		} catch (IOException e) {
			e.printStackTrace();
		}

		XMLCheck validator = new XMLCheck();
		ValidationResult result = validator.validate(xml);

		if (result.isValid()) {
			return true;
		}
		return false;
	}

	/**
	 * This method is needed for every RESTful application in LAS2peer. There is no need to change!
	 * 
	 * @return the mapping
	 */
	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
	}

}
