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
			
		    result = c.sendRequest("GET", mainPath + "create/hello/test", ""); //create an entry
			assertEquals(200,result.getHttpCode());
			result = c.sendRequest("GET", mainPath + "getNumber", ""); // should return one 
			
			assertTrue(result.getResponse().contains("1"));
			result = c.sendRequest("GET", mainPath + "create/neuereintrag/willkommen", ""); //create another entry
			
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject)parser.parse(result.getResponse());
			
			String deleteID = (String) params.get("entry_id"); //get the id of the second entry
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().contains("2"));
			
			result = c.sendRequest("POST", mainPath + "setStart/" + deleteID + "/2002/3/9/15/12", "");
			assertEquals(200,result.getHttpCode());
			 
			result = c.sendRequest("POST", mainPath + "setEnd/" + deleteID + "/2002/3/9/15/22", "");
			
			result = c.sendRequest("GET", mainPath + "getDay/2002/3/9", "");
			result = c.sendRequest("GET", mainPath + "getDay/2002/3/10", "");
			
			result = c.sendRequest("GET", mainPath + "deleteEntry/" + deleteID, ""); //delete the second entry
			assertEquals(200,result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getNumber", ""); //check if it really
			assertTrue(result.getResponse().contains("1"));
			
			result = c.sendRequest("GET", mainPath + "create/deletethis/iwanttobedeleted", "");
			params = (JSONObject)parser.parse(result.getResponse());
			
			deleteID = (String) params.get("entry_id"); // get the id
			
			result = c2.sendRequest("GET", mainPath + "deleteEntry/" + deleteID, ""); 
			assertEquals(400,result.getHttpCode()); //shouldn't be able to delete this time
			
			result = c.sendRequest("GET", mainPath + "deleteEntry/" + deleteID, ""); //should be able to delete
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
			result = c.sendRequest("POST", mainPath + "createWeekly/2012/2/3/2/14/22/15/24/wocheneintrag/jedewoche", ""); //create two entries
			assertEquals(200, result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().contains("3")); //check if they have been created
			
			result = c.sendRequest("GET", mainPath + "getDay/2042/2/3", ""); 
			
			result = c.sendRequest("POST", mainPath + "createMonthly/2002/5/10/4/12/12/17/34/monatseintrag/jedermonat", ""); //create 4 entries
			assertEquals(200, result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().contains("7")); //check if they have been created
		
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}		
	}
	
	@Test
	public void loadTEST()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try{
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", mainPath + "get", "");
			assertTrue(result.getResponse().trim().contains("hello"));
			
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
			ClientResponse result = c.sendRequest("GET", mainPath + "create/hello/test", ""); //create an entry
		
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject)parser.parse(result.getResponse());
			String dateID = (String) params.get("entry_id"); //get the id of the second entry
			
			result = c.sendRequest("POST", mainPath + "setStart/" + dateID + "/2013/12/11/15/12", "");
			result = c.sendRequest("POST", mainPath + "setEnd/" + dateID + "/2013/12/12/16/12", "");
			assertEquals(200,result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getDay/2013/12/11", "");
			assertEquals(200,result.getHttpCode());

			
			
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
