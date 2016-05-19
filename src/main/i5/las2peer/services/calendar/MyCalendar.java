package i5.las2peer.services.calendar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.restMapper.HttpResponse;
import i5.las2peer.restMapper.MediaType;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.ContentParam;
import i5.las2peer.restMapper.annotations.Version;
import i5.las2peer.restMapper.tools.ValidationResult;
import i5.las2peer.restMapper.tools.XMLCheck;
import i5.las2peer.security.UserAgent;
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
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;


/**
 * LAS2peer Calendar Service
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
 * 
 * SERVICE_CUSTOM_MESSAGE_1 - Added entry
 * SERVICE_CUSTOM_MESSAGE_2 - Fetched entry
 * SERVICE_CUSTOM_MESSAGE_3 - Deleted entry
 * SERVICE_CUSTOM_MESSAGE_4 - GetNumberOFEntries Call
 * SERVICE_CUSTOM_MESSAGE_5 - Set Start Call
 * SERVICE_CUSTOM_MESSAGE_6 - Set End Call
 * SERVICE_CUSTOM_MESSAGE_7 - Created Comment
 * SERVICE_CUSTOM_MESSAGE_8 - Deleted Comment
 * SERVICE_CUSTOM_MESSAGE_9 - Get Day Call
 * SERVICE_CUSTOM_MESSAGE_10 - Get Month Call
 * SERVICE_CUSTOM_MESSAGE_11 - Create Regular Call
 * 
 */
@Path("/calendar")
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
	
	// instantiate the logger class
	private final L2pLogger logger = L2pLogger.getInstance(MyCalendar.class.getName());

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

	public MyCalendar() {
		// read and set properties values
		// IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED TO BE CHANGED TOO!
		setFieldValues();
		// instantiate a database manager to handle database connection pooling and credentials
	}
	
	public UserAgent getUserAgent(){
		return (UserAgent) getContext().getMainAgent();
	}
	
	public void createEntry(String title, String description, String sYear, String sMonth, String sDay, 
							String sHour, String sMinute, String eYear, String eMonth, String eDay, String eHour, String eMinute){
		
		String content = "{\"title\":\""+ title + "\",\"description\":\""+ description +"\"}";
		String result = create(content).getResult();
		JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
		
		try{
			JSONObject params = (JSONObject)parser.parse(result);
			String id = (String) params.get("entry_id");
			content = "{\"year\":\""+ sYear + "\", \"month\":\""+ sMonth +"\", \"day\":\""+ sDay + "\", \"hour\":\""+ sHour+"\", \"minute\":\""+ sMinute +"\"}";
			result = setStart(id, content).getResult();
			content = "{\"year\":\""+ eYear + "\", \"month\":\""+ eMonth +"\", \"day\":\""+ eDay + "\", \"hour\":\""+ eHour+"\", \"minute\":\""+ eMinute +"\"}";
			result = setEnd(id, content).getResult();
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
	
		private static String stringfromJSON(JSONObject obj, String key) throws Exception {
			String s = (String) obj.get(key);
			if (s == null) {
				throw new Exception("Key " + key + " is missing in JSON");
			}
			return s;
		}
	// //////////////////////////////////////////////////////////////////////////////////////
	// Service methods.
	// //////////////////////////////////////////////////////////////////////////////////////

		
	/**
	 * Creates an entry and saves it permanently in the node storage.
	 * THIS DOES NOT WORK YET UNFORTUNATELY BUT THIS IS THE WAY TO GO
	 * To create temporary entries use the method createEntry.
	 * 
	 * @param content
	 * 			Containing title and description
	 * @return
	 * 		   Whether or not creation was successful
	 */
		
	@POST
	@Path("/create")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@ApiOperation(value = "create",
			notes = "Create an entry inside a Calendar")
	public HttpResponse create( @ContentParam String content){
		JSONObject o;
		try{
			o = (JSONObject) JSONValue.parseWithException(content);
			String title = stringfromJSON(o, "title");
			String description = stringfromJSON(o, "description");
			
			
			 if((title.equals("")) || (description.equals(""))){
				 return new HttpResponse ("one of the parameters is empty", HttpURLConnection.HTTP_BAD_REQUEST);
			 }
			 
			 Entry newEntry = new Entry( getContext().getMainAgent().getId() , title, description, MAXIMUM_COMMENT_AMOUNT);
			
			 // save entry using envelopes
			 try{
				 
				 Envelope env = null;
				 
				 try{
					 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
				 }
				 
				 catch (Exception e){
					 // write error to logfile and console
					 logger.log(Level.SEVERE, e.toString(), e);
					 // create and publish a monitoring message
					 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not found. Creating new one. " + e.toString());
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
		
				 //store information in log
				 logger.log(Level.FINE, "stored " + stored.size() + " entries in network storage");
				 L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_1, getContext().getMainAgent(), ""+title);
				 return new HttpResponse(toString.toJSONString(), HttpURLConnection.HTTP_OK);
			     } catch (Exception e) {
			    	 
			    	// write error to log file and console 	 
					logger.log(Level.SEVERE, e.toString(), e);
					// create and publish a monitoring message
					L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Can't persist entries to network storage! " + e.getMessage());;
					return new HttpResponse("error" + e, HttpURLConnection.HTTP_BAD_REQUEST);
			     }
		}catch(Exception e){
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse("Received invalid JSON");
			res.setStatus(400);
			return res;
		}
	}
		
	
	/**
	 * Gets the entry by the id
	 * @param id 
	 * @return 
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

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_2, getContext().getMainAgent(), ""+id);
			return new HttpResponse(returnString, HttpURLConnection.HTTP_OK);
		
		}
		catch(Exception e){
			return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
		}
	}
	
	/**
	 * Deletes an entry inside the calendar
	 * 
	 * @param id
	 * 			  The id of the entry
	 * @return Success or error message
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
				// write error to log file and console
				 logger.log(Level.SEVERE, e.toString(), e);
				 // create and publish a monitoring message
				 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not found. Creating new one. " + e.toString());
				 env = Envelope.createClassIdEnvelope(new EntryBox(1), STORAGE_NAME, getAgent());
			 }
			 
			 env.open(getAgent());
			 EntryBox stored = env.getContent(EntryBox.class);
			 Entry toDelete = stored.returnEntry(id);
			 if(toDelete.getCreatorId()!=getUserAgent().getId()){
				 
				 // write error to log file and console
				 logger.log(Level.SEVERE, "cannot delete this entry by another user");
				 // create and publish a monitoring message
				 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "cannot delete this entry by another user");

				 return new HttpResponse("entry couldn't be deleted", HttpURLConnection.HTTP_FORBIDDEN);
			 }
			 boolean result = stored.delete(id);
			 env.updateContent(stored);
			 env.addSignature(getAgent());
			 env.store();
			 env.close();
			 
			 if(result==true){
			 //store information in log
		     logger.log(Level.FINE, "deleted " + stored.size() + " entries in network storage");	
			 L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_3, getContext().getMainAgent(), ""+id);
			 return new HttpResponse(Serialization.serializeEntry(toDelete).toJSONString(), HttpURLConnection.HTTP_OK);
			 }
			 
			 else{
				 
				 return new HttpResponse("entry with id: " + id +" wasn't found", HttpURLConnection.HTTP_NOT_FOUND);
				 
			 }
			 
		     } catch (Exception e) {
		    	 
		    	// write error to log file and console
				logger.log(Level.SEVERE, "Couldn't delete the entry", e);
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Couldn't delete the entry" + e.getMessage());
		    	 
				return new HttpResponse("error" + e, HttpURLConnection.HTTP_BAD_REQUEST);
		     }
		
	}
	
	/**
	 * Gets the number of entries in the calendar
	 * 
	 * @return Success or error message
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
			 
			 // write info to log file and console
			 logger.log(Level.FINE, "Network storage not found.", e);

			 return new HttpResponse("0", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
		 
		 try{
			 
		 env.open(getAgent());
		 EntryBox stored = env.getContent(EntryBox.class);
		 int size = stored.size();
		 env.close();
		 L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_4, getContext().getMainAgent(), "");
		 return new HttpResponse("The amount of entries is: " + size, HttpURLConnection.HTTP_OK);
		 
		 }
		 
		 catch(Exception e){
			 
			 // write error to log file and console
			 logger.log(Level.SEVERE, "Can't read messages from storage", e);
			 // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Can't read messages from storage" + e.getMessage());
	     
		 }
		 return new HttpResponse("GetNumber Fail", HttpURLConnection.HTTP_BAD_REQUEST);
	}
	
	
	/**
	 * Sets the start date of an entry
	 * 
	 * @param id
	 * 			  The id of the entry whose start date is supposed to be changed
	 * @param content
	 * 			  Containing the year, month, day, hour and minute of the start of the entry
	 * 
	 * @return Success or error message
	 */
	
	@PUT
	@Path("/setStart/{id}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "start date set"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found")
	})
	@ApiOperation(value = "setStart",
	notes = "set startdate of entry")
	public HttpResponse setStart( @PathParam("id") String id, @ContentParam String content){
		JSONObject o;
		try{
			o = (JSONObject) JSONValue.parseWithException(content);
			String year = stringfromJSON(o,"year");
			String month = stringfromJSON(o,"month");
			String day = stringfromJSON(o,"day");
			String hour = stringfromJSON(o,"hour");
			String minute = stringfromJSON(o,"minute");
			int yearInt   = Integer.parseInt(year);
			int monthInt  = Integer.parseInt(month);
			monthInt--; //Calendar month start at 0 so reduce
			int dayInt    = Integer.parseInt(day);
			int hourInt   = Integer.parseInt(hour);
			int minuteInt = Integer.parseInt(minute);
			
			 Envelope env = null;
			 
			 try{
				 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			 }
			 
			 catch (Exception e){
				// write error to log file and console
				logger.log(Level.SEVERE, e.toString(), e);
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
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
			 
			 logger.log(Level.FINE, "stored " + stored.size() + " entries in network storage");
			 L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_5, getContext().getMainAgent(), ""+id);
			 return new HttpResponse(rest, HttpURLConnection.HTTP_OK);
			 } 
			 catch(Exception e){
				// write error to log file and console 	 
			    logger.log(Level.SEVERE, e.toString(), e);
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());;
				
				return new HttpResponse("entry could not be found", HttpURLConnection.HTTP_NOT_FOUND);
			 }
		}catch(Exception e){
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse("Received invalid JSON");
			res.setStatus(400);
			return res;
		}
	}
	
	
	/**
	 * Sets the end date of an entry
	 * 
	 * @param id
	 * 			  The id of the entry whose end date is supposed to be changed
	 * @param content
	 * 			  Containing the year, month, day, hour and minute of the end of the entry
	 * 
	 * @return Success or error message
	 */
	
	@PUT
	@Path("/setEnd/{id}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "end date set"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found")
	})
	@ApiOperation(value = "setEnd",
	notes = "set end date of entry")
	public HttpResponse setEnd( @PathParam("id") String id, @ContentParam String content){
		JSONObject o;
		try{
			o = (JSONObject) JSONValue.parseWithException(content);
			int yearInt   = Integer.parseInt(stringfromJSON(o,"year"));
			int monthInt  = Integer.parseInt(stringfromJSON(o,"month"));
			monthInt--; //Calendar month start at 0 so reduce
			int dayInt    = Integer.parseInt(stringfromJSON(o,"day"));
			int hourInt   = Integer.parseInt(stringfromJSON(o,"hour"));
			int minuteInt = Integer.parseInt(stringfromJSON(o,"minute"));
			
			 Envelope env = null;
			 
			 try{
				 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
			 }
			 
			 catch (Exception e){
				 // write error to log file and console
				 logger.log(Level.SEVERE, e.toString(), e);
				 // create and publish a monitoring message
				 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
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
			 
			 logger.log(Level.FINE, "stored " + stored.size() + " entries in network storage");
			 L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_6, getContext().getMainAgent(), ""+id);
			 return new HttpResponse(rest, HttpURLConnection.HTTP_OK);
			 }
			 catch(Exception e){
				 // write error to log file and console 	 
				 logger.log(Level.SEVERE, e.toString(), e);
			     // create and publish a monitoring message
				 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
				 return new HttpResponse("entry could not be found", HttpURLConnection.HTTP_NOT_FOUND);
			 }
		}catch(Exception e){
			L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
			HttpResponse res = new HttpResponse("Received invalid JSON");
			res.setStatus(400);
			return res;
		}
	}
	
	/**
	 * Create a comment for an entry
	 * 
	 * @param id
	 * 			  The id of the entry which a comment is to be created for
	 * @param comment
	 * 			  The content of the comment
	 * @return success or error message
	 */
	@POST
	@Path("/createComment/{id}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry number"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
			@ApiResponse(code = HttpURLConnection.HTTP_NOT_FOUND, message = "Entry not found")
	})
	@ApiOperation(value = "createComment",
	notes = "add comment to entry")
	public HttpResponse createComment( @PathParam("id") String id, @ContentParam String comment) {
		
		Envelope env = null;
		 
		 try{
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		 }
		 
		 catch (Exception e){
			 // write error to log file and console
			 logger.log(Level.SEVERE, e.toString(), e);
			 // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
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
		 updatedEntry.createComment(getContext().getMainAgent().getId(), comment); //add the comment
		 stored.delete(id); //delete the former entry
		 stored.addEntry(updatedEntry); //upload the new entry
		 JSONObject entry = Serialization.serializeEntry(updatedEntry);
		 String rest = entry.toJSONString();

		 env.updateContent(stored);
		 env.addSignature(getAgent());
		 env.store();
		 env.close();
		 
		 logger.log(Level.FINE, "stored " + stored.size() + " entries in network storage");
		 L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_7, getContext().getMainAgent(), ""+id);
		 return new HttpResponse(rest, HttpURLConnection.HTTP_OK);
		 } 
		 catch(Exception e){
			// write error to log file and console 	 
			 logger.log(Level.SEVERE, e.toString(), e);
		     // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
			 return new HttpResponse("could not use storage", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
	}
	
	/**
	 * Delete a comment
	 * 
	 * @param id
	 * 			  The id of the comment that shall be deleted
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
			 // write error to log file and console
			 logger.log(Level.SEVERE, e.toString(), e);
			 // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
			 return new HttpResponse("fail", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
		
		 try{
			 
		 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		
		 env.open(getAgent());
		 EntryBox stored = env.getContent(EntryBox.class);
		 String entryID = stored.findComment(id);
		 if(entryID.equals("")){
			 // write error to log file and console
			 logger.log(Level.SEVERE, "comment cannot be found");
			 // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Comment was not found");
			 return new HttpResponse("Comment was not found", HttpURLConnection.HTTP_NOT_FOUND);
		 }
		 
		 Entry newEntry = stored.returnEntry(entryID);
		 Comment deleteComment = newEntry.returnComment(id);
		 if((deleteComment.getCreatorId()!=getUserAgent().getId()) && (newEntry.getCreatorId()!=getUserAgent().getId())){
			 
			 	 // write error to log file and console
			     logger.log(Level.SEVERE, "this comment cannot be deleted by another user");
				 return new HttpResponse("comment couldn't be deleted", HttpURLConnection.HTTP_FORBIDDEN);
				 
		 }
		 newEntry.deleteComment(id);
		 stored.delete(entryID); //delete the former entry
		 stored.addEntry(newEntry); //upload the new entry
		 
		 env.updateContent(stored);
		 env.addSignature(getAgent());
		 env.store();
		 env.close();
		 
		 logger.log(Level.FINE, "stored " + stored.size() + " entries in network storage");
		 L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_8, getContext().getMainAgent(), ""+id);
		 return new HttpResponse("entry with id:" + id +":was sucessfully changed. ", HttpURLConnection.HTTP_OK);
		 } 
		 catch(Exception e){
			 // write error to log file and console 	 
			 logger.log(Level.SEVERE, e.toString(), e);
		     // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
			 return new HttpResponse("entry could not be found", HttpURLConnection.HTTP_BAD_REQUEST);
		 }
	}
	
	
	
	/**
	 * Get all the ids of the entries on a certain day
	 * @param year 
	 * @param month 
	 * @param day 
	 * @return 
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
			 // write error to log file and console
			 logger.log(Level.SEVERE, e.toString(), e);
			 // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
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

		L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_9, getContext().getMainAgent(), "");
		return new HttpResponse (returnString, HttpURLConnection.HTTP_OK);
		
		}
		 
		catch(Exception e){
			 // write error to log file and console 	 
			 logger.log(Level.SEVERE, e.toString(), e);
		     // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
			 return new HttpResponse("entry could not be found" + e.getMessage(), HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
	}
	
	/**
	 * Get all the ids of the entries on a certain month
	 * @param year 
	 * @param month 
	 * @return 
	 * 
	 */
	@GET
	@Path("/getMonth/{year}/{month}")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entry number"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@ApiOperation(value = "getMonth",
	notes = "get all entries of a month")
	public HttpResponse getMonth ( @PathParam("year") String year, @PathParam ("month") String month){
		
		Envelope env = null;
		
		 try{
			 env = getContext().getStoredObject(EntryBox.class, STORAGE_NAME);
		 }
		 
		 catch (Exception e){
			 // write error to log file and console
			 logger.log(Level.SEVERE, e.toString(), e);
			 // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
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
			 GregorianCalendar dayDate = new GregorianCalendar(yearInt, monthInt, 1);
			 
		
		for(Entry anEntry: entries){
			if((anEntry.getEnd()!= null) && (anEntry.getStart() != null)) {
			Calendar date = anEntry.getStart(); 
			Calendar end = anEntry.getEnd();
			if((date.get(Calendar.YEAR) == yearInt) && (date.get(Calendar.MONTH) == monthInt)){ // if entry starts on the day
	
						entryList.add(anEntry);
			
			}
			
			else if((end.get(Calendar.YEAR) == yearInt) && (end.get(Calendar.MONTH) == monthInt)){ // if entry ends on the day
				
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

		L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_10, getContext().getMainAgent(), "");
		return new HttpResponse (returnString, HttpURLConnection.HTTP_OK);
		
		}
		 
		catch(Exception e){
			 // write error to log file and console 	 
			 logger.log(Level.SEVERE, e.toString(), e);
		     // create and publish a monitoring message
			 L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
			 return new HttpResponse("entry could not be found" + e.getMessage(), HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
	}
	
	
	/**
	 * method to create entries on a regular basis
	 * 
	 * @param content
	 * 			Containing all information for the entries, like title, description, interval, number, startDate (year, month, day...) and endDate (year, month, day...)
	 * @return 
	 */
	@POST
	@Path("/createRegular")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Entries created"),
			@ApiResponse(code = HttpURLConnection.HTTP_BAD_REQUEST, message = "Bad Request"),
	})
	@ApiOperation(value = "create regular entries",
	notes = "creates entries on a regular basis")
	public HttpResponse createRegular ( @ContentParam String content){

		//parse Strings to integers
		int startYear, startMonth,  startDay, startHour, startMinute, endYear, endMonth, endDay ,endHour, endMinute, numbers;
		String title, description, interval;
		JSONObject o;
		try{
		o = (JSONObject) JSONValue.parseWithException(content);
		title = stringfromJSON(o,"title");
		description = stringfromJSON(o,"description");
		interval = stringfromJSON(o,"interval");
		
		startYear = Integer.parseInt(stringfromJSON(o,"syear"));
		startMonth = Integer.parseInt(stringfromJSON(o,"smonth"));
		startDay = Integer.parseInt(stringfromJSON(o,"sday"));
		startHour = Integer.parseInt(stringfromJSON(o,"shour"));
		startMinute = Integer.parseInt(stringfromJSON(o,"sminute"));
		endYear = Integer.parseInt(stringfromJSON(o,"eyear"));
		endMonth = Integer.parseInt(stringfromJSON(o,"emonth"));
		endDay = Integer.parseInt(stringfromJSON(o,"eday"));
		endHour = Integer.parseInt(stringfromJSON(o,"ehour"));
		endMinute = Integer.parseInt(stringfromJSON(o,"eminute"));
		numbers = Integer.parseInt(stringfromJSON(o,"number"));
		} catch (Exception e){
			String error = "Strings invalid";
			return new HttpResponse(error, HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
		GregorianCalendar start = new GregorianCalendar(startYear, startMonth, startDay, startHour, startMinute);
		GregorianCalendar end = new GregorianCalendar(endYear, endMonth, endDay, endHour, endMinute);
		
		if(start.after(end)){
			return new HttpResponse("start was after end", HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
		switch (interval) {
		case "week": 
			createEntry(title, description, Integer.toString(startYear), Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour), Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth), Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.DATE, 7);
				end.add(Calendar.DATE, 7);
				String sYear = Integer.toString(start.get(Calendar.YEAR));
				String sMonth = Integer.toString(start.get(Calendar.MONTH));
				String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				String eYear = Integer.toString(end.get(Calendar.YEAR));
				String eMonth = Integer.toString(end.get(Calendar.MONTH));
				String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, sYear, sMonth, sDay, Integer.toString(startHour), Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;
			}
			
			break;
			
			
		case "month":
			createEntry(title, description, Integer.toString(startYear), Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour), Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth), Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.MONTH, 1);
				end.add(Calendar.MONTH, 1);
				String sYear = Integer.toString(start.get(Calendar.YEAR));
				String sMonth = Integer.toString(start.get(Calendar.MONTH));
				String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				String eYear = Integer.toString(end.get(Calendar.YEAR));
				String eMonth = Integer.toString(end.get(Calendar.MONTH));
				String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, sYear, sMonth, sDay, Integer.toString(startHour), Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;
			}
			
			break;
		
		case "year":
			
			createEntry(title, description, Integer.toString(startYear), Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour), Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth), Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.YEAR, 1);
				end.add(Calendar.YEAR, 1);
				String sYear = Integer.toString(start.get(Calendar.YEAR));
				String sMonth = Integer.toString(start.get(Calendar.MONTH));
				String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				String eYear = Integer.toString(end.get(Calendar.YEAR));
				String eMonth = Integer.toString(end.get(Calendar.MONTH));
				String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, sYear, sMonth, sDay, Integer.toString(startHour), Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;
			}
			
			break;
		case "biweek": 

			createEntry(title, description, Integer.toString(startYear), Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour), Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth), Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.DATE, 14);
				end.add(Calendar.DATE, 14);
				String sYear = Integer.toString(start.get(Calendar.YEAR));
				String sMonth = Integer.toString(start.get(Calendar.MONTH));
				String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				String eYear = Integer.toString(end.get(Calendar.YEAR));
				String eMonth = Integer.toString(end.get(Calendar.MONTH));
				String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, sYear, sMonth, sDay, Integer.toString(startHour), Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;
			}
			
			break;
		
		case "bimonth": 
			createEntry(title, description, Integer.toString(startYear), Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour), Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth), Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
			numbers--;
		
		while(numbers>(0)){
			start.add(Calendar.MONTH, 2);
			end.add(Calendar.MONTH, 2);
			String sYear = Integer.toString(start.get(Calendar.YEAR));
			String sMonth = Integer.toString(start.get(Calendar.MONTH));
			String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
			String eYear = Integer.toString(end.get(Calendar.YEAR));
			String eMonth = Integer.toString(end.get(Calendar.MONTH));
			String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
			createEntry(title, description, sYear, sMonth, sDay, Integer.toString(startHour), Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour), Integer.toString(endMinute));
			numbers--;
		}
		
		break;		
		case "quarter":
			
			createEntry(title, description, Integer.toString(startYear), Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour), Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth), Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
			numbers--;
			
			while(numbers>(0)){
				start.add(Calendar.MONTH, 3);
				end.add(Calendar.MONTH, 3);
				String sYear = Integer.toString(start.get(Calendar.YEAR));
				String sMonth = Integer.toString(start.get(Calendar.MONTH));
				String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
				String eYear = Integer.toString(end.get(Calendar.YEAR));
				String eMonth = Integer.toString(end.get(Calendar.MONTH));
				String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
				createEntry(title, description, sYear, sMonth, sDay, Integer.toString(startHour), Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;
			}
			
			break;
			
		default: String error = "invalid interval";
				 return new HttpResponse(error, HttpURLConnection.HTTP_BAD_REQUEST);
		}
		
		String error = "entries successfully created";

		L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_11, getContext().getMainAgent(), ""+title);
		return new HttpResponse(error, HttpURLConnection.HTTP_OK);
		
	}
	
	
	/**
	 * Function to get the login name of an agent
	 * 
	 * @param id
	 * 		The id of the agent 
	 * @return
	 *		The login name
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
	
	/**
	 * Function to get the current id of the active agent
	 * 
	 * @return
	 *		The id of the current agent
	 */
	@GET
	@Path("/id")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Name"),
			@ApiResponse(code = HttpURLConnection.HTTP_INTERNAL_ERROR, message = "Internal error")
	})
	@ApiOperation(value = "name",
			notes = "get the name of an agent")
	public HttpResponse getId(){
		 
	    try{
	    	UserAgent user = (UserAgent) getContext().getMainAgent();
	    	long id = user.getId();
	    	String rString = Long.toString(id);
	    	return new HttpResponse(rString, HttpURLConnection.HTTP_OK);
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
	 * @return The mapping
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
