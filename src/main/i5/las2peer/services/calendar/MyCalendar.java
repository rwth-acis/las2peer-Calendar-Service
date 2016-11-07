package i5.las2peer.services.calendar;

import java.io.Serializable;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.Context;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.logging.NodeObserver.Event;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
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
 * This is a simple calendar service to store entries inside.
 * 
 * Note: If you plan on using Swagger you should adapt the information below in the ApiInfo annotation to suit your
 * project. If you do not intend to provide a Swagger documentation of your service API, the entire ApiInfo annotation
 * should be removed.
 * 
 * 
 * SERVICE_CUSTOM_MESSAGE_1 - Added entry SERVICE_CUSTOM_MESSAGE_2 - Fetched entry SERVICE_CUSTOM_MESSAGE_3 - Deleted
 * entry SERVICE_CUSTOM_MESSAGE_4 - GetNumberOFEntries Call SERVICE_CUSTOM_MESSAGE_5 - Set Start Call
 * SERVICE_CUSTOM_MESSAGE_6 - Set End Call SERVICE_CUSTOM_MESSAGE_7 - Created Comment SERVICE_CUSTOM_MESSAGE_8 - Deleted
 * Comment SERVICE_CUSTOM_MESSAGE_9 - Get Day Call SERVICE_CUSTOM_MESSAGE_10 - Get Month Call SERVICE_CUSTOM_MESSAGE_11
 * - Create Regular Call
 * 
 */
@ServicePath("/calendar")
@Api
@SwaggerDefinition(
		info = @Info(
				title = "LAS2peer Calendar Service",
				version = "0.3",
				description = "A LAS2peer Calendar Service.",
				termsOfService = "http://your-terms-of-service-url.com",
				contact = @Contact(
						name = "Melvin Bender",
						url = "dbis.rwth-aachen.de",
						email = "bender@dbis.rwth-aachen.de"),
				license = @License(
						name = "your software license name",
						url = "http://your-software-license-url.com")))
public class MyCalendar extends RESTService {

	// instantiate the logger class

	/**
	 * service properties with default values, can be overwritten with properties file config/MyCalendar.properties
	 */
	public int MAXIMUM_COMMENT_AMOUNT = 100;
	/** list with entries of the calendar **/
	private final ArrayList<Entry> entries = new ArrayList<Entry>(MAXIMUM_COMMENT_AMOUNT);

	public ArrayList<Entry> getEntries() {
		return entries;
	}

	public MyCalendar() {
		// read and set properties values
		// IF THE SERVICE CLASS NAME IS CHANGED, THE PROPERTIES FILE NAME NEED TO BE CHANGED TOO!
		setFieldValues();
		// instantiate a database manager to handle database connection pooling and credentials
	}

	public UserAgent getUserAgent() {
		return (UserAgent) Context.getCurrent().getMainAgent();
	}

	public void createEntry(String title, String description, String comments, String sYear, String sMonth, String sDay,
			String sHour, String sMinute, String eYear, String eMonth, String eDay, String eHour, String eMinute) {

		Resource r = new Resource();
		String content = "{\"title\":\"" + title + "\",\"description\":\"" + description + "\",\"groupID\":\""
				+ comments + "\"}";
		String result = r.create(content).getEntity().toString();
		JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);

		try {
			JSONObject params = (JSONObject) parser.parse(result);
			String id = (String) params.get("entry_id");
			content = "{\"year\":\"" + sYear + "\", \"month\":\"" + sMonth + "\", \"day\":\"" + sDay + "\", \"hour\":\""
					+ sHour + "\", \"minute\":\"" + sMinute + "\"}";
			r.setStart(id, content);
			content = "{\"year\":\"" + eYear + "\", \"month\":\"" + eMonth + "\", \"day\":\"" + eDay + "\", \"hour\":\""
					+ eHour + "\", \"minute\":\"" + eMinute + "\"}";
			r.setEnd(id, content);
		} catch (Exception e) {
			return;
		}
	}

	// returns an entry within the calendar with an id
	public Entry retrieveEntry(String id) {
		for (Entry anEntry : this.getEntries()) {
			if (anEntry.getUniqueID().equals(id)) {
				return anEntry;
			}
		}

		return null; // id wasn't found within an entry
	}

	private static String stringfromJSON(JSONObject obj, String key) throws Exception {
		String s = (String) obj.get(key);
		if (s == null) {
			throw new Exception("Key " + key + " is missing in JSON");
		}
		return s;
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(Resource.class);
	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// Service methods.
	// //////////////////////////////////////////////////////////////////////////////////////
	@Path("/") // this is the root resource
	public static class Resource {
		private MyCalendar service = (MyCalendar) Context.getCurrent().getService();
		private final String STORAGE_NAME = "entrystorage";
		// put here all your service methods

		@POST
		@Path("/create")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Entry"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"), })
		@ApiOperation(
				value = "create",
				notes = "Create an entry inside a Calendar")
		public Response create(String content) {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(content);
				String title = stringfromJSON(o, "title");
				String description = stringfromJSON(o, "description");
				String group = stringfromJSON(o, "groupID");
				Long groupID = Long.valueOf(group);
				String threadID = "";

				if ((title.equals("")) || (description.equals(""))) {
					return Response.status(Status.BAD_REQUEST).entity("One of the parameters is emtpy").build();
				}

				try {
					// RMI call without parameters
					Object result = Context.getCurrent().invoke(
							"i5.las2peer.services.threadedCommentService.ThreadedCommentService@0.1",
							"createCommentThread",
							new Serializable[] { Context.getCurrent().getMainAgent().getId(), groupID, groupID });
					if (result != null) {
						threadID = (String) result;
					} else {
						// create and publish a monitoring message
						L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Couldn't create thread. ");
					}
				} catch (Exception e) {
					// one may want to handle some exceptions differently
					L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
				}
				Entry newEntry = new Entry(Context.getCurrent().getMainAgent().getId(), title, description, threadID);

				// save entry using envelopes
				try {

					Envelope env = null;

					try {
						env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);
					} catch (ArtifactNotFoundException eA) {
						L2pLogger.logEvent(this, Event.SERVICE_ERROR,
								"Network storage not found. Creating new one. " + eA.toString());
						env = Context.getCurrent().createEnvelope(STORAGE_NAME, new EntryBox(1));
					} catch (Exception e) {
					}

					EntryBox stored = (EntryBox) env.getContent();
					stored.addEntry(newEntry);
					env = Context.getCurrent().createEnvelope(env, stored);
					storeEnvelope(env);

					JSONObject toString = Serialization.serializeEntry(newEntry);
					L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_1, Context.getCurrent().getMainAgent(), "" + title);
					return Response.status(Status.OK).entity(toString.toJSONString()).build();
				} catch (Exception e) {
					// create and publish a monitoring message
					L2pLogger.logEvent(this, Event.SERVICE_ERROR,
							"Can't persist entries to network storage! " + e.getMessage());
					return Response.status(Status.BAD_REQUEST).entity("Error " + e).build();
				}
			} catch (Exception e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity("Received invalid JSON").build();
			}
		}

		/**
		 * Gets the entry by the id
		 * 
		 * @param id Id of the entry you are looking for
		 * @return The entry with the given id
		 */
		@GET
		@Path("/getEntry/{id}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Entry"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_NOT_FOUND,
								message = "Entry not found"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"), })
		@ApiOperation(
				value = "getEntry",
				notes = "finds an Entry by an id")
		public Response getEntry(@PathParam("id") String id) {

			try {

				Envelope env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);
				EntryBox stored = (EntryBox) env.getContent();
				Entry returnEntry = stored.returnEntry(id);
				if (returnEntry == null) {
					return Response.status(Status.NOT_FOUND).entity("Couldn't find entry with id: " + id).build();
				}

				JSONObject res = Serialization.serializeEntry(returnEntry);
				String returnString = res.toJSONString();

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_2, Context.getCurrent().getMainAgent(), "" + id);
				return Response.status(Status.OK).entity(returnString).build();

			} catch (Exception e) {
				return Response.status(Status.BAD_REQUEST).entity("Fail").build();
			}
		}

		/**
		 * Deletes an entry inside the calendar
		 * 
		 * @param id The id of the entry
		 * @return Success or error message
		 */
		@DELETE
		@Path("/deleteEntry/{id}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Entry"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_FORBIDDEN,
								message = "Deletion forbidden"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_NOT_FOUND,
								message = "Entry not found"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"), })
		@ApiOperation(
				value = "deleteEntry",
				notes = "deletes an entry")
		public Response deleteEntry(@PathParam("id") String id) {

			try {

				Envelope env = null;

				try {
					env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);
				}

				catch (Exception e) {
					// create and publish a monitoring message
					L2pLogger.logEvent(this, Event.SERVICE_ERROR,
							"Network storage not found. Creating new one. " + e.toString());
					env = Context.getCurrent().createEnvelope(STORAGE_NAME, new EntryBox(1));
				}

				EntryBox stored = (EntryBox) env.getContent();
				Entry toDelete = stored.returnEntry(id);
				if (toDelete.getCreatorId() != Context.getCurrent().getMainAgent().getId()) {
					// create and publish a monitoring message
					L2pLogger.logEvent(this, Event.SERVICE_ERROR, "cannot delete this entry by another user");
					return Response.status(Status.FORBIDDEN).entity("entry couldn't be deleted").build();
				}
				boolean result = stored.delete(id);
				Context.getCurrent().createEnvelope(env, stored);
				storeEnvelope(env);

				if (result == true) {
					// store information in log
					L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_3, Context.getCurrent().getMainAgent(), "" + id);

					return Response.status(Status.OK).entity(Serialization.serializeEntry(toDelete).toJSONString())
							.build();
				}

				else {
					return Response.status(Status.NOT_FOUND).entity("entry " + id + " was not found").build();

				}

			} catch (Exception e) {
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Couldn't delete the entry" + e.getMessage());
				return Response.status(Status.BAD_REQUEST).entity("error " + e).build();
			}

		}

		/**
		 * Gets the number of entries in the calendar
		 * 
		 * @return Success or error message
		 */
		@GET
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Entry number"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"), })
		@Path("/getNumber")
		@ApiOperation(
				value = "getNumber",
				notes = "get Number of entries")
		public Response getNumberOfEntries() {

			Envelope env = null;

			try { // try to load the entryBox
				env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);
			}

			catch (Exception e) {
				return Response.status(Status.BAD_REQUEST).entity("0").build();
			}

			try {

				EntryBox stored = (EntryBox) env.getContent();
				int size = stored.size();
				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_4, Context.getCurrent().getMainAgent(), "");
				return Response.status(Status.OK).entity("The amount of entries is: " + size).build();

			}

			catch (Exception e) {
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Can't read messages from storage" + e.getMessage());

			}
			return Response.status(Status.NOT_FOUND).entity("GetNumber Fail").build();
		}

		/**
		 * Sets the start date of an entry
		 * 
		 * @param id The id of the entry whose start date is supposed to be changed
		 * @param content Containing the year, month, day, hour and minute of the start of the entry
		 * 
		 * @return Success or error message
		 */

		@PUT
		@Path("/setStart/{id}")
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "start date set"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_NOT_FOUND,
								message = "Entry not found") })
		@ApiOperation(
				value = "setStart",
				notes = "set startdate of entry")
		public Response setStart(@PathParam("id") String id, String content) {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(content);
				String year = stringfromJSON(o, "year");
				String month = stringfromJSON(o, "month");
				String day = stringfromJSON(o, "day");
				String hour = stringfromJSON(o, "hour");
				String minute = stringfromJSON(o, "minute");
				int yearInt = Integer.parseInt(year);
				int monthInt = Integer.parseInt(month);
				monthInt--; // Calendar month start at 0 so reduce
				int dayInt = Integer.parseInt(day);
				int hourInt = Integer.parseInt(hour);
				int minuteInt = Integer.parseInt(minute);

				Envelope env = null;

				try {
					env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);
				}

				catch (Exception e) {
					// create and publish a monitoring message
					L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
					return Response.status(Status.BAD_REQUEST).entity("Fail").build();
				}

				try {

					env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);

					EntryBox stored = (EntryBox) env.getContent();
					Entry updatedEntry = stored.returnEntry(id); // get the entry whose start date is supposed to be
																	// stored
					updatedEntry.setStart(yearInt, monthInt, dayInt, hourInt, minuteInt);
					stored.delete(id);
					stored.addEntry(updatedEntry);
					JSONObject entry = Serialization.serializeEntry(updatedEntry);
					String rest = entry.toJSONString();
					env = Context.getCurrent().createEnvelope(env, stored);
					storeEnvelope(env);

					L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_5, Context.getCurrent().getMainAgent(), "" + id);
					return Response.status(Status.OK).entity(rest).build();
				} catch (Exception e) {
					// create and publish a monitoring message
					L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
					return Response.status(Status.NOT_FOUND).entity("entry could not be found").build();
				}
			} catch (Exception e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity("Received invalid JSON").build();
			}
		}

		/**
		 * Sets the end date of an entry
		 * 
		 * @param id The id of the entry whose end date is supposed to be changed
		 * @param content Containing the year, month, day, hour and minute of the end of the entry
		 * 
		 * @return Success or error message
		 */

		@PUT
		@Path("/setEnd/{id}")
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "end date set"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_NOT_FOUND,
								message = "Entry not found") })
		@ApiOperation(
				value = "setEnd",
				notes = "set end date of entry")
		public Response setEnd(@PathParam("id") String id, String content) {
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(content);
				int yearInt = Integer.parseInt(stringfromJSON(o, "year"));
				int monthInt = Integer.parseInt(stringfromJSON(o, "month"));
				monthInt--; // Calendar month start at 0 so reduce
				int dayInt = Integer.parseInt(stringfromJSON(o, "day"));
				int hourInt = Integer.parseInt(stringfromJSON(o, "hour"));
				int minuteInt = Integer.parseInt(stringfromJSON(o, "minute"));

				Envelope env = null;

				try {
					env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);
				}

				catch (Exception e) {
					// create and publish a monitoring message
					L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
					return Response.status(Status.BAD_REQUEST).entity("fail").build();
				}

				try {

					env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);

					EntryBox stored = (EntryBox) env.getContent();
					Entry updatedEntry = stored.returnEntry(id); // get the entry whose end date is supposed to be
																	// stored
					updatedEntry.setEnd(yearInt, monthInt, dayInt, hourInt, minuteInt);
					JSONObject entry = Serialization.serializeEntry(updatedEntry);
					String rest = entry.toJSONString();
					stored.delete(id);
					stored.addEntry(updatedEntry);

					env = Context.getCurrent().createEnvelope(env, stored);
					storeEnvelope(env);

					L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_6, Context.getCurrent().getMainAgent(), "" + id);
					return Response.status(Status.OK).entity(rest).build();
				} catch (Exception e) {
					// create and publish a monitoring message
					L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
					return Response.status(Status.NOT_FOUND).entity("entry could not be found").build();

				}
			} catch (Exception e) {
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, e.toString());
				return Response.status(Status.BAD_REQUEST).entity("Received invalid JSON").build();

			}
		}

		/**
		 * Get all the ids of the entries on a certain day
		 * 
		 * @param year Year of the certain day
		 * @param month Month of the certain day
		 * @param day The day you want to look up
		 * @return all entries of a day
		 * 
		 */
		@GET
		@Path("/getDay/{year}/{month}/{day}")
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Entry number"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"), })
		@ApiOperation(
				value = "getDay",
				notes = "get all entries of a day")
		public Response getDay(@PathParam("year") String year, @PathParam("month") String month,
				@PathParam("day") String day) {

			Envelope env = null;
			ArrayList<Entry> entryList = new ArrayList<>();
			try {
				env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);
			}

			catch (Exception e) {
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
				return Response.status(Status.BAD_REQUEST).entity("fail").build();

			}

			try {

				env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);

				EntryBox stored = (EntryBox) env.getContent();

				Entry[] entries = stored.getEntries();
				int yearInt = Integer.parseInt(year);
				int monthInt = Integer.parseInt(month);
				monthInt--;
				int dayInt = Integer.parseInt(day);
				GregorianCalendar dayDate = new GregorianCalendar(yearInt, monthInt, dayInt);

				for (Entry anEntry : entries) {
					if ((anEntry.getEnd() != null) && (anEntry.getStart() != null)) {
						Calendar date = anEntry.getStart();
						Calendar end = anEntry.getEnd();
						if ((date.get(Calendar.YEAR) == yearInt) && (date.get(Calendar.MONTH) == monthInt)
								&& (date.get(Calendar.DAY_OF_MONTH) == dayInt)) { // if entry starts on the day

							entryList.add(anEntry);

						}

						else if ((end.get(Calendar.YEAR) == yearInt) && (end.get(Calendar.MONTH) == monthInt)
								&& (end.get(Calendar.DAY_OF_MONTH) == dayInt)) { // if entry ends on the day

							entryList.add(anEntry);

						}

						else { // if entry starts before the day and ends after the day
							if (date.before(dayDate) && end.after(dayDate)) {
								entryList.add(anEntry);
							}
						}
					}
				}

				String returnString = Serialization.serializeEntries(entryList).toJSONString();

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_9, Context.getCurrent().getMainAgent(), "");
				return Response.status(Status.OK).entity(returnString).build();
			}

			catch (Exception e) {
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
				return Response.status(Status.BAD_REQUEST).entity("entry could not be found" + e.getMessage()).build();
			}

		}

		/**
		 * Get all the ids of the entries on a certain month
		 * 
		 * @param year year of the month
		 * @param month month that should be used
		 * @return all entries of the month
		 * 
		 */
		@GET
		@Path("/getMonth/{year}/{month}")
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Entry number"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"), })
		@ApiOperation(
				value = "getMonth",
				notes = "get all entries of a month")
		public Response getMonth(@PathParam("year") String year, @PathParam("month") String month) {

			Envelope env = null;

			try {
				env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);
			}

			catch (Exception e) {
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Network storage not there yet" + e.toString());
				return Response.status(Status.BAD_REQUEST).entity("Fail").build();
			}

			try {

				ArrayList<Entry> entryList = new ArrayList<>();
				env = Context.getCurrent().fetchEnvelope(STORAGE_NAME);

				EntryBox stored = (EntryBox) env.getContent();

				Entry[] entries = stored.getEntries();
				int yearInt = Integer.parseInt(year);
				int monthInt = Integer.parseInt(month);
				monthInt--;
				GregorianCalendar dayDate = new GregorianCalendar(yearInt, monthInt, 1);

				for (Entry anEntry : entries) {
					if ((anEntry.getEnd() != null) && (anEntry.getStart() != null)) {
						Calendar date = anEntry.getStart();
						Calendar end = anEntry.getEnd();
						if ((date.get(Calendar.YEAR) == yearInt) && (date.get(Calendar.MONTH) == monthInt)) { // if
																												// entry
																												// starts
																												// on
																												// the
																												// day

							entryList.add(anEntry);

						}

						else if ((end.get(Calendar.YEAR) == yearInt) && (end.get(Calendar.MONTH) == monthInt)) { // if
																													// entry
																													// ends
																													// on
																													// the
																													// day

							entryList.add(anEntry);

						}

						else { // if entry starts before the day and ends after the day
							if (date.before(dayDate) && end.after(dayDate)) {
								entryList.add(anEntry);
							}
						}
					}
				}

				String returnString = Serialization.serializeEntries(entryList).toJSONString();

				L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_10, Context.getCurrent().getMainAgent(), "");
				return Response.status(Status.OK).entity(returnString).build();

			}

			catch (Exception e) {
				// create and publish a monitoring message
				L2pLogger.logEvent(this, Event.SERVICE_ERROR, "Could not open storage! " + e.getMessage());
				return Response.status(Status.BAD_REQUEST).entity("entry could not be found" + e.getMessage()).build();
			}

		}

		/**
		 * method to create entries on a regular basis
		 * 
		 * @param content Containing all information for the entries, like title, description, interval, number,
		 *            startDate (year, month, day...) and endDate (year, month, day...)
		 * @return whether entry was created or not
		 */
		@POST
		@Path("/createRegular")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Entries created"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_BAD_REQUEST,
								message = "Bad Request"), })
		@ApiOperation(
				value = "create regular entries",
				notes = "creates entries on a regular basis")
		public Response createRegular(String content) {

			// parse Strings to integers
			int startYear, startMonth, startDay, startHour, startMinute, endYear, endMonth, endDay, endHour, endMinute,
					numbers;
			String title, description, interval, comments;
			JSONObject o;
			try {
				o = (JSONObject) JSONValue.parseWithException(content);
				title = stringfromJSON(o, "title");
				description = stringfromJSON(o, "description");
				interval = stringfromJSON(o, "interval");
				comments = stringfromJSON(o, "comments");

				startYear = Integer.parseInt(stringfromJSON(o, "syear"));
				startMonth = Integer.parseInt(stringfromJSON(o, "smonth"));
				startDay = Integer.parseInt(stringfromJSON(o, "sday"));
				startHour = Integer.parseInt(stringfromJSON(o, "shour"));
				startMinute = Integer.parseInt(stringfromJSON(o, "sminute"));
				endYear = Integer.parseInt(stringfromJSON(o, "eyear"));
				endMonth = Integer.parseInt(stringfromJSON(o, "emonth"));
				endDay = Integer.parseInt(stringfromJSON(o, "eday"));
				endHour = Integer.parseInt(stringfromJSON(o, "ehour"));
				endMinute = Integer.parseInt(stringfromJSON(o, "eminute"));
				numbers = Integer.parseInt(stringfromJSON(o, "number"));
			} catch (Exception e) {
				String error = "Strings invalid";
				return Response.status(Status.BAD_REQUEST).entity(error).build();
			}

			GregorianCalendar start = new GregorianCalendar(startYear, startMonth, startDay, startHour, startMinute);
			GregorianCalendar end = new GregorianCalendar(endYear, endMonth, endDay, endHour, endMinute);

			if (start.after(end)) {
				return Response.status(Status.BAD_REQUEST).entity("Start was after end.").build();
			}

			switch (interval) {
			case "week":
				service.createEntry(title, description, comments, Integer.toString(startYear),
						Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour),
						Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth),
						Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;

				while (numbers > (0)) {
					start.add(Calendar.DATE, 7);
					end.add(Calendar.DATE, 7);
					String sYear = Integer.toString(start.get(Calendar.YEAR));
					String sMonth = Integer.toString(start.get(Calendar.MONTH));
					String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
					String eYear = Integer.toString(end.get(Calendar.YEAR));
					String eMonth = Integer.toString(end.get(Calendar.MONTH));
					String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
					service.createEntry(title, description, comments, sYear, sMonth, sDay, Integer.toString(startHour),
							Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour),
							Integer.toString(endMinute));
					numbers--;
				}

				break;

			case "month":
				service.createEntry(title, description, comments, Integer.toString(startYear),
						Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour),
						Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth),
						Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;

				while (numbers > (0)) {
					start.add(Calendar.MONTH, 1);
					end.add(Calendar.MONTH, 1);
					String sYear = Integer.toString(start.get(Calendar.YEAR));
					String sMonth = Integer.toString(start.get(Calendar.MONTH));
					String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
					String eYear = Integer.toString(end.get(Calendar.YEAR));
					String eMonth = Integer.toString(end.get(Calendar.MONTH));
					String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
					service.createEntry(title, description, comments, sYear, sMonth, sDay, Integer.toString(startHour),
							Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour),
							Integer.toString(endMinute));
					numbers--;
				}

				break;

			case "year":

				service.createEntry(title, description, comments, Integer.toString(startYear),
						Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour),
						Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth),
						Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;

				while (numbers > (0)) {
					start.add(Calendar.YEAR, 1);
					end.add(Calendar.YEAR, 1);
					String sYear = Integer.toString(start.get(Calendar.YEAR));
					String sMonth = Integer.toString(start.get(Calendar.MONTH));
					String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
					String eYear = Integer.toString(end.get(Calendar.YEAR));
					String eMonth = Integer.toString(end.get(Calendar.MONTH));
					String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
					service.createEntry(title, description, comments, sYear, sMonth, sDay, Integer.toString(startHour),
							Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour),
							Integer.toString(endMinute));
					numbers--;
				}

				break;
			case "biweek":

				service.createEntry(title, description, comments, Integer.toString(startYear),
						Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour),
						Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth),
						Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;

				while (numbers > (0)) {
					start.add(Calendar.DATE, 14);
					end.add(Calendar.DATE, 14);
					String sYear = Integer.toString(start.get(Calendar.YEAR));
					String sMonth = Integer.toString(start.get(Calendar.MONTH));
					String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
					String eYear = Integer.toString(end.get(Calendar.YEAR));
					String eMonth = Integer.toString(end.get(Calendar.MONTH));
					String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
					service.createEntry(title, description, comments, sYear, sMonth, sDay, Integer.toString(startHour),
							Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour),
							Integer.toString(endMinute));
					numbers--;
				}

				break;

			case "bimonth":
				service.createEntry(title, description, comments, Integer.toString(startYear),
						Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour),
						Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth),
						Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;

				while (numbers > (0)) {
					start.add(Calendar.MONTH, 2);
					end.add(Calendar.MONTH, 2);
					String sYear = Integer.toString(start.get(Calendar.YEAR));
					String sMonth = Integer.toString(start.get(Calendar.MONTH));
					String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
					String eYear = Integer.toString(end.get(Calendar.YEAR));
					String eMonth = Integer.toString(end.get(Calendar.MONTH));
					String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));
					service.createEntry(title, description, comments, sYear, sMonth, sDay, Integer.toString(startHour),
							Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour),
							Integer.toString(endMinute));
					numbers--;
				}

				break;
			case "quarter":

				service.createEntry(title, description, comments, Integer.toString(startYear),
						Integer.toString(startMonth), Integer.toString(startDay), Integer.toString(startHour),
						Integer.toString(startMinute), Integer.toString(endYear), Integer.toString(endMonth),
						Integer.toString(endDay), Integer.toString(endHour), Integer.toString(endMinute));
				numbers--;

				while (numbers > (0)) {
					start.add(Calendar.MONTH, 3);
					end.add(Calendar.MONTH, 3);
					String sYear = Integer.toString(start.get(Calendar.YEAR));
					String sMonth = Integer.toString(start.get(Calendar.MONTH));
					String sDay = Integer.toString(start.get(Calendar.DAY_OF_MONTH));
					String eYear = Integer.toString(end.get(Calendar.YEAR));
					String eMonth = Integer.toString(end.get(Calendar.MONTH));
					String eDay = Integer.toString(end.get(Calendar.DAY_OF_MONTH));

					service.createEntry(title, description, comments, sYear, sMonth, sDay, Integer.toString(startHour),
							Integer.toString(startMinute), eYear, eMonth, eDay, Integer.toString(endHour),
							Integer.toString(endMinute));
					numbers--;
				}

				break;

			default:
				String error = "invalid interval";
				return Response.status(Status.BAD_REQUEST).entity(error).build();
			}

			String returnString = "entries successfully created";

			L2pLogger.logEvent(Event.SERVICE_CUSTOM_MESSAGE_11, Context.getCurrent().getMainAgent(), "" + title);
			return Response.status(Status.OK).entity(returnString).build();

		}

		/**
		 * Function to get the login name of an agent
		 * 
		 * @param id The id of the agent
		 * @return The login name
		 */
		@GET
		@Path("/name/{id}")
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Name"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_NOT_FOUND,
								message = "Not Found"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_INTERNAL_ERROR,
								message = "Internal error") })
		@ApiOperation(
				value = "name",
				notes = "get the name of an agent")
		public Response getName(@PathParam("id") String id) {

			long agentid = Long.parseLong(id);
			try {
				UserAgent fred = (UserAgent) Context.getCurrent().getAgent(agentid);
				String name = fred.getLoginName();
				return Response.status(Status.OK).entity(name).build();
			} catch (AgentNotKnownException e) {
				String error = "Agent not found";
				return Response.status(Status.NOT_FOUND).entity(error).build();
			} catch (Exception e) {
				String error = "Internal error";
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build();
			}
		}

		/**
		 * Function to get the current id of the active agent
		 * 
		 * @return The id of the current agent
		 */
		@GET
		@Path("/id")
		@ApiResponses(
				value = { @ApiResponse(
						code = HttpURLConnection.HTTP_OK,
						message = "Name"),
						@ApiResponse(
								code = HttpURLConnection.HTTP_INTERNAL_ERROR,
								message = "Internal error") })
		@ApiOperation(
				value = "name",
				notes = "get the name of an agent")
		public Response getId() {
			try {
				UserAgent user = (UserAgent) Context.getCurrent().getMainAgent();
				long id = user.getId();
				String rString = Long.toString(id);
				return Response.status(Status.OK).entity(rString).build();
			} catch (Exception e) {
				String error = "Internal error";
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build();
			}
		}

		// envelope methods
		private void storeEnvelope(Envelope env) {
			try {
				Context.getCurrent().storeEnvelope(env);
			} catch (StorageException e) {

				e.printStackTrace();
			}
		}

	}

}
