package i5.las2peer.services.calendar;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

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

public class StorageTest {


	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgent testAgent;
	private static UserAgent secondAgent;
	
	private static final String testPass = "adamspass";
	private static final String secondPass = "evespass";
	private static final String testTemplateService = MyCalendar.class.getCanonicalName();

	private static final String mainPath = "example/";

	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void startServer() throws Exception {

		// start node
		node = LocalNode.newNode();
		node.storeAgent(MockAgentFactory.getAdam());
		node.storeAgent(MockAgentFactory.getEve());
		node.launch();

		ServiceAgent testService = ServiceAgent.generateNewAgent(testTemplateService, "a pass");
		testService.unlockPrivateKey("a pass");

		node.registerReceiver(testService);

		// start connector
		logStream = new ByteArrayOutputStream();

		connector = new WebConnector(true, HTTP_PORT, false, 1000);
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);
		Thread.sleep(1000); // wait a second for the connector to become ready
		testAgent = MockAgentFactory.getAdam();
		secondAgent = MockAgentFactory.getEve();

		connector.updateServiceList();
		// avoid timing errors: wait for the repository manager to get all services before continuing
		try
		{
			System.out.println("waiting..");
			Thread.sleep(10000);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

	}

	/**
	 * Called after the tests have finished.
	 * Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void shutDownServer() throws Exception {

		connector.stop();
		node.shutDown();

		connector = null;
		node = null;

		LocalNode.reset();

		System.out.println("Connector-Log:");
		System.out.println("--------------");

		System.out.println(logStream.toString());

	}
	
	@Test
	public void saveTEST()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		MiniClient c2 = new MiniClient();
		c2.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try{
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			c2.setLogin(Long.toString(secondAgent.getId()), secondPass);
			ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", ""); //assert there are no entries
			
			assertTrue(result.getResponse().contains("0")); 
			assertEquals(400,result.getHttpCode());
			
		    result = c.sendRequest("POST", mainPath + "create/hello/test", ""); //create an entry
			assertEquals(200,result.getHttpCode());
			result = c.sendRequest("GET", mainPath + "getNumber", ""); // should return one 
			
			assertTrue(result.getResponse().contains("1"));
			result = c.sendRequest("POST", mainPath + "create/neuereintrag/willkommen", ""); //create another entry
			
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject)parser.parse(result.getResponse());
			
			String deleteID = (String) params.get("entry_id"); //get the id of the second entry
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().contains("2"));
			
			result = c.sendRequest("PUT", mainPath + "setStart/" + deleteID + "/2002/3/9/15/12", "");
			assertEquals(200,result.getHttpCode());
			 
			result = c.sendRequest("PUT", mainPath + "setEnd/" + deleteID + "/2002/3/9/15/22", "");
			
			result = c.sendRequest("GET", mainPath + "getDay/2002/3/9", "");
			result = c.sendRequest("GET", mainPath + "getDay/2002/3/8", "");
			assertFalse(result.getResponse().contains("willkommen"));
			
			result = c.sendRequest("DELETE", mainPath + "deleteEntry/" + deleteID, ""); //delete the second entry
			assertEquals(200,result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getNumber", ""); //check if it really
			assertTrue(result.getResponse().contains("1"));
			
			result = c.sendRequest("POST", mainPath + "create/deletethis/iwanttobedeleted", "");
			params = (JSONObject)parser.parse(result.getResponse());
			
			deleteID = (String) params.get("entry_id"); // get the id
			
			result = c2.sendRequest("DELETE", mainPath + "deleteEntry/" + deleteID, ""); 
			assertEquals(403,result.getHttpCode()); //shouldn't be able to delete this time
			
			result = c.sendRequest("DELETE", mainPath + "deleteEntry/" + deleteID, ""); //should be able to delete
			assertEquals(200,result.getHttpCode());
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		
	}
	
	@Test
	public void intervallTest()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try{
			
			ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", ""); //assert there is one entry from the previous test
			assertTrue(result.getResponse().contains("1"));
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			result = c.sendRequest("POST", mainPath + "createRegular/2012/2/3/14/22/15/24/wocheneintrag/jedewoche/week/2", ""); //create two entries
			assertEquals(200, result.getHttpCode());
		
			result = c.sendRequest("GET", mainPath + "getDay/2012/2/10", "");
			assertEquals(200, result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().contains("3")); //check if they have been created
			
			result = c.sendRequest("GET", mainPath + "getDay/2042/2/3", ""); 
			
			result = c.sendRequest("POST", mainPath + "createRegular/2002/5/10/12/12/17/34/monatseintrag/jedermonat/month/4", ""); //create 4 entries
			assertEquals(200, result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().contains("7")); //check if they have been created
		
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}		
	}
	
	
	@Test
	public void dateTEST()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try{
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("POST", mainPath + "create/hello/test", ""); //create an entry
		
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject)parser.parse(result.getResponse());
			String dateID = (String) params.get("entry_id"); //get the id of the second entry
			
			result = c.sendRequest("PUT", mainPath + "setStart/" + dateID + "/2013/12/11/15/12", "");
			result = c.sendRequest("PUT", mainPath + "setEnd/" + dateID + "/2013/12/12/16/12", "");
			assertEquals(200,result.getHttpCode());
		
			result = c.sendRequest("GET", mainPath + "getDay/2013/12/11", "");
			assertEquals(200,result.getHttpCode());
			

			
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}
	
	@Test
	public void commentTest()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try{
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("POST", mainPath + "create/firstEntry/myentry/", "");
			assertEquals(200, result.getHttpCode());
			
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject)parser.parse(result.getResponse());
			String ID = (String) params.get("entry_id"); //get the id of the second entry
			
			result = c.sendRequest("POST", mainPath + "createComment/" + ID + "/firstComment", "");
			assertEquals(200, result.getHttpCode());
			
			result = c.sendRequest("PUT", mainPath + "setStart/" + ID + "/2013/3/11/15/12", "");
			result = c.sendRequest("PUT", mainPath + "setEnd/" + ID + "/2013/3/12/16/12", "");
			
			result = c.sendRequest("GET", mainPath + "getEntry/" + ID, "");
			assertEquals(200, result.getHttpCode());
			assertTrue(result.getResponse().contains(String.valueOf(testAgent.getId()))); //creator id needs to be returned too
		
			assertTrue(result.getResponse().contains("firstComment"));
		
			result = c.sendRequest("GET", mainPath + "getDay/2013/3/11", "");
			assertTrue(result.getResponse().contains("firstComment"));

			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}		
	}
	
	@Test
	public void nameTest()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try{
			
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", mainPath + "name/" + testAgent.getId(), "");
		
			assertEquals(result.getHttpCode(), 200);
			
			result = c.sendRequest("POST", mainPath + "create/authorentry/junit", "");
			assertTrue(result.getResponse().contains(String.valueOf(testAgent.getId())));
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}	
	}
	
	@Test
	public void testDebugMapping()
	{
		MyCalendar cl = new MyCalendar();
		assertTrue(cl.debugMapping());
	}


}
