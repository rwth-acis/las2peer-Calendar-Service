package i5.las2peer.services.calendar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import i5.las2peer.api.Service;
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
import i5.las2peer.services.calendar.storage.MyStorageObject;
import i5.las2peer.services.calendar.storage.StorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONObject;

/**
 * LAS2peer Service
 * 
 * This is a template for a very basic LAS2peer service
 * that uses the LAS2peer Web-Connector for RESTful access to it.
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
				version = "0.1",
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

	@GET
	@Path("/awake/{test}")
	public HttpResponse awake( @PathParam("test") String message){
		
		MyStorageObject test = new MyStorageObject(message);
		StorageService moin = new StorageService();
		moin.persistObject("hallo", test);
		return new HttpResponse ("perfekt", HttpURLConnection.HTTP_ACCEPTED);
		
	}
		
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
	 * 		   whether or not creation was sucessful
	 */
	@GET
	@Path("/create/{title}/{description}")
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
			 Context.logMessage(this, "stored " + stored.size() + " entries in network storage");
			 return new HttpResponse("entry was sucessfully stored", HttpURLConnection.HTTP_OK);
		     } catch (Exception e) {
				Context.logError(this, "Can't persist entries to network storage! " + e.getMessage());
				return new HttpResponse("error" + e, HttpURLConnection.HTTP_BAD_REQUEST);
		     }
	}
		
	/**
	 * returns all entries of the storage
	 */
	
	@GET
	@Path("/get")
	public HttpResponse get(){
		try{

			Envelope env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			env.open(getAgent());
			EntryBox stored = env.getContent(EntryBox.class);
			Context.logMessage(this, "Loaded " + stored.size() + "entries from the storage");
			Entry[] result = stored.getEntries();
			env.close();
			String res = "";
			for (Entry a : result){
				res += a.getTitle();
			}
			return new HttpResponse(res, HttpURLConnection.HTTP_ACCEPTED);
		}
		catch (Exception e){
			Context.logError(this, "Can't read messages from storage");
		}
		return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
	}
	
	/**
	 * Creates a new entry inside the calendar
	 * 
	 * @param title
	 * 			  the title of the entry
	 * @param description
	 * 			  the description of the entry
	 * @return success or error message
	 */
	@GET
	@Path("/createEntry/{title}/{description}")
	public HttpResponse createEntry( @PathParam("title") String title, @PathParam("description") String description) {
		Entry newEntry = new Entry(getActiveAgent().getId(), title, description, MAXIMUM_COMMENT_AMOUNT);
		String id = newEntry.getUniqueID();
		this.entries.add(newEntry);
			
		String returnString = "";
		returnString += ((UserAgent) getActiveAgent()).getLoginName() + " created an entry with the id:" + id +": and the title:" + title;
		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
	}
	
	@GET
	@Path("/getEntry/{id}")
	public HttpResponse getEntry( @PathParam("id") String id){
		Entry test = new Entry(MAXIMUM_COMMENT_AMOUNT, "dd", "ddd", 2);
		try{
		Envelope env = Envelope.fetchClassIdEnvelope(test.getClass(), id);
		String returnString = env.getContentAsString();
		return new HttpResponse(returnString, HttpURLConnection.HTTP_ACCEPTED);
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
	@GET
	@Path("/deleteEntry/{id}")
	public HttpResponse deleteEntry( @PathParam("id") String id) {
		Entry dummy = retrieveEntry(id);
		if(dummy == null){
			return new HttpResponse("fail", HttpURLConnection.HTTP_NOT_FOUND);
		}
		this.getEntries().remove(dummy);
		String returnString = "";
		returnString += ((UserAgent) getActiveAgent()).getLoginName() + " deleted an entry with the name " + dummy.getTitle();
		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
	}
	
	/**
	 * gets the number of entries in the calendar
	 * 
	 * 
	 * @return success or error message
	 */
	@GET
	@Path("/getNumber")
	public HttpResponse getNumberOfEntries(){
		int counter = this.getEntries().size();
		String count = Integer.toString(counter);
		return new HttpResponse(count, HttpURLConnection.HTTP_OK);
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
	
	@POST
	@Path("/setStart/{id}/{year}/{month}/{day}/{hour}/{minute}")
	public HttpResponse setStart( @PathParam("id") String id, @PathParam ("year") String year, @PathParam ("month") String month,
			@PathParam ("day") String day, @PathParam ("hour") String hour, @PathParam ("minute") String minute)
	{
		if(retrieveEntry(id) == null){
			return new HttpResponse("Entry not found", HttpURLConnection.HTTP_NOT_FOUND);
		}
		
		int yearInt   = Integer.parseInt(year);
		int monthInt  = Integer.parseInt(month);
		int dayInt    = Integer.parseInt(day);
		int hourInt   = Integer.parseInt(hour);
		int minuteInt = Integer.parseInt(minute);
		
		
		boolean result = retrieveEntry(id).setStart(yearInt, monthInt, dayInt, hourInt, minuteInt);
		if(result == true){
		return new HttpResponse("Start date set", HttpURLConnection.HTTP_OK);
		}
		
		else{
			return new HttpResponse("Start date could not be set", HttpURLConnection.HTTP_BAD_REQUEST);
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
	
	@POST
	@Path("/setEnd/{id}/{year}/{month}/{day}/{hour}/{minute}")
	public HttpResponse setEnd( @PathParam("id") String id, @PathParam ("year") String year, @PathParam ("month") String month,
			@PathParam ("day") String day, @PathParam ("hour") String hour, @PathParam ("minute") String minute)
	{
		if(retrieveEntry(id) == null){
			return new HttpResponse("Entry not found", HttpURLConnection.HTTP_NOT_FOUND);
		}
		
		int yearInt   = Integer.parseInt(year);
		int monthInt  = Integer.parseInt(month);
		int dayInt    = Integer.parseInt(day);
		int hourInt   = Integer.parseInt(hour);
		int minuteInt = Integer.parseInt(minute);
		
		
		boolean result = retrieveEntry(id).setEnd(yearInt, monthInt, dayInt, hourInt, minuteInt);
		if(result == true){
		return new HttpResponse("End date set", HttpURLConnection.HTTP_OK);
		}
		
		else{
			return new HttpResponse("End date could not be set", HttpURLConnection.HTTP_BAD_REQUEST);
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
	public HttpResponse createComment( @PathParam("id") String id, @PathParam("comment") String comment) {
		
		Entry entry = retrieveEntry(id);
		if(entry == null){
			return new HttpResponse("entry could not be found", HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
		String commentId = entry.createComment(getActiveAgent().getId(), comment);
		
		if(commentId.equals("")){
			return new HttpResponse("comment was empty", HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
		String returnString = "";
		returnString += "Comment could be created for the entry:" + id + "and the new id of the comment is:" + commentId + ":"; 
		
		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
	}
	
	/**
	 * Delete a comment
	 * 
	 * @param id
	 * 			  the id of the comment that shall be deleted
	 * @return success or error message
	 */
	@POST
	@Path("/deleteComment/{id}")
	public HttpResponse deleteComment( @PathParam("id") String id) {
		
		for(Entry anEntry: this.getEntries()){
			for(Comment aComment: anEntry.getComments()){
				if(aComment.getUniqueID().equals(id)){
					anEntry.getComments().remove(aComment);
					return new HttpResponse("comment with the following id was deleted" + id, HttpURLConnection.HTTP_OK);
				}
			}
		}
		
		return new HttpResponse("comment was not found", HttpURLConnection.HTTP_BAD_REQUEST);
	}
	
	/**
	 * get all the ids of the entries on a certain day
	 * 
	 */
	@GET
	@Path("/getDay/{year}/{month}/{day}")
	public HttpResponse getDay ( @PathParam("year") String year, @PathParam ("month") String month, @PathParam("day") String day){
		
		ArrayList<Entry> entryList = new ArrayList<Entry>();
		int yearInt = Integer.parseInt(year);
		int monthInt = Integer.parseInt(month);
		int dayInt = Integer.parseInt(day);
		GregorianCalendar dayDate = new GregorianCalendar(yearInt, monthInt, dayInt);
		
		for(Entry anEntry: this.getEntries()){
			Calendar date = anEntry.getStart();
			Calendar end = anEntry.getEnd();
			if(date.get(Calendar.YEAR) == yearInt){ // if entry starts on the day
				if(date.get(Calendar.MONTH) == monthInt){
					if(date.get(Calendar.DAY_OF_MONTH) == dayInt){
						entryList.add(anEntry);
					}
				}
			}
			else if(end.get(Calendar.YEAR) == yearInt){ // if entry ends on the day
				  if(end.get(Calendar.MONTH) == monthInt){
				   if(end.get(Calendar.DAY_OF_MONTH) == dayInt){
						entryList.add(anEntry);
					}
				}
			}
			
			else{  //if entry starts before the day and ends after the day
				if(date.before(dayDate) && end.after(dayDate)){
					entryList.add(anEntry);
				}
			}
		}
		
		if(entryList.isEmpty()){
			return new HttpResponse("no matching entries were found", HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
		String returnString = "The following entries were found:";
		for(Entry anEntry: entryList){
			returnString += anEntry.getUniqueID() + ","; 
		}
		
		return new HttpResponse (returnString, HttpURLConnection.HTTP_OK);
		
	}

	/**
	 * Simple function to validate a user login.
	 * Basically it only serves as a "calling point" and does not really validate a user
	 * (since this is done previously by LAS2peer itself, the user does not reach this method
	 * if he or she is not authenticated).
	 * 
	 */
	@GET
	@Path("/validation")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "User Validation",
			notes = "Simple function to validate a user login.")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Validation Confirmation"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized")
	})
	public HttpResponse validateLogin() {
		String returnString = "";
		returnString += "You are " + ((UserAgent) getActiveAgent()).getLoginName() + " and your login is valid!";
		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
	}

	/**
	 * Example method that returns a phrase containing the received input.
	 * 
	 * @param myInput
	 * 
	 */
	@POST
	@Path("/myResourcePath/{input}")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Input Phrase"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized")
	})
	@ApiOperation(value = "Sample Resource",
			notes = "Example method that returns a phrase containing the received input.")
	public HttpResponse exampleMethod(@PathParam("input") String myInput) {
		String returnString = "";
		returnString += "You have entered " + myInput + "!";

		return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
	}

	/**
	 * Example method that shows how to retrieve a user email address from a database 
	 * and return an HTTP response including a JSON object.
	 * 
	 * WARNING: THIS METHOD IS ONLY FOR DEMONSTRATIONAL PURPOSES!!! 
	 * IT WILL REQUIRE RESPECTIVE DATABASE TABLES IN THE BACKEND, WHICH DON'T EXIST IN THE TEMPLATE.
	 * 
	 */
	@GET
	@Path("/userEmail/{username}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "User Email"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "User not found"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "Email Address Administration",
			notes = "Example method that retrieves a user email address from a database."
					+ " WARNING: THIS METHOD IS ONLY FOR DEMONSTRATIONAL PURPOSES!!! "
					+ "IT WILL REQUIRE RESPECTIVE DATABASE TABLES IN THE BACKEND, WHICH DON'T EXIST IN THE TEMPLATE.")
	public HttpResponse getUserEmail(@PathParam("username") String username) {
		String result = "";
		Connection conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try {
			// get connection from connection pool
			conn = dbm.getConnection();

			// prepare statement
			stmnt = conn.prepareStatement("SELECT email FROM users WHERE username = ?;");
			stmnt.setString(1, username);

			// retrieve result set
			rs = stmnt.executeQuery();

			// process result set
			if (rs.next()) {
				result = rs.getString(1);

				// setup resulting JSON Object
				JSONObject ro = new JSONObject();
				ro.put("email", result);

				// return HTTP Response on success
				return new HttpResponse(ro.toJSONString(), HttpURLConnection.HTTP_OK);
			} else {
				result = "No result for username " + username;

				// return HTTP Response on error
				return new HttpResponse(result, HttpURLConnection.HTTP_NOT_FOUND);
			}
		} catch (Exception e) {
			// return HTTP Response on error
			return new HttpResponse("Internal error: " + e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
		} finally {
			// free resources
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					return new HttpResponse("Internal error: " + e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
				}
			}
			if (stmnt != null) {
				try {
					stmnt.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					return new HttpResponse("Internal error: " + e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					return new HttpResponse("Internal error: " + e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
				}
			}
		}
	}

	/**
	 * Example method that shows how to change a user email address in a database.
	 * 
	 * WARNING: THIS METHOD IS ONLY FOR DEMONSTRATIONAL PURPOSES!!! 
	 * IT WILL REQUIRE RESPECTIVE DATABASE TABLES IN THE BACKEND, WHICH DON'T EXIST IN THE TEMPLATE.
	 * 
	 */
	@POST
	@Path("/userEmail/{username}/{email}")
	@Produces(MediaType.TEXT_PLAIN)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Update Confirmation"),
			@ApiResponse(code = HttpURLConnection.HTTP_UNAUTHORIZED, message = "Unauthorized"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal Server Error")
	})
	@ApiOperation(value = "setUserEmail",
			notes = "Example method that changes a user email address in a database."
					+ " WARNING: THIS METHOD IS ONLY FOR DEMONSTRATIONAL PURPOSES!!! "
					+ "IT WILL REQUIRE RESPECTIVE DATABASE TABLES IN THE BACKEND, WHICH DON'T EXIST IN THE TEMPLATE.")
	public HttpResponse setUserEmail(@PathParam("username") String username, @PathParam("email") String email) {

		String result = "";
		Connection conn = null;
		PreparedStatement stmnt = null;
		ResultSet rs = null;
		try {
			conn = dbm.getConnection();
			stmnt = conn.prepareStatement("UPDATE users SET email = ? WHERE username = ?;");
			stmnt.setString(1, email);
			stmnt.setString(2, username);
			int rows = stmnt.executeUpdate(); // same works for insert
			result = "Database updated. " + rows + " rows affected";

			// return
			return new HttpResponse(result, HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			// return HTTP Response on error
			return new HttpResponse("Internal error: " + e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
		} finally {
			// free resources if exception or not
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					return new HttpResponse("Internal error: " + e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
				}
			}
			if (stmnt != null) {
				try {
					stmnt.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					return new HttpResponse("Internal error: " + e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					Context.logError(this, e.getMessage());

					// return HTTP Response on error
					return new HttpResponse("Internal error: " + e.getMessage(), HttpURLConnection.HTTP_INTERNAL_ERROR);
				}
			}
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
