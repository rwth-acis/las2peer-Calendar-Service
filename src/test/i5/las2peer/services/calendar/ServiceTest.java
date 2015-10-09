package i5.las2peer.services.calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.calendar.MyCalendar;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.webConnector.WebConnector;
import i5.las2peer.webConnector.client.ClientResponse;
import i5.las2peer.webConnector.client.MiniClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Example Test Class demonstrating a basic JUnit test structure.
 *
 */
public class ServiceTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static final int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgent testAgent;
	private static final String testPass = "adamspass";

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
	
	
	/**
	 * Sets the start and end dates of an entry. An end date cannot be earlier than the start date
	 * 
	 */
	
	@Test
	
	public void testDates(){
		
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", mainPath + "createEntry/testname/description", ""); 
			
			String[] test = result.getResponse().split(":"); // get the id
			String entryID = test[1];
			
			result = c.sendRequest("POST", mainPath + "setStart/" + entryID + "/2010/11/13/12/34" , "");
			assertEquals(200, result.getHttpCode());
			
			result = c.sendRequest("POST", mainPath + "setEnd/" + entryID + "/2009/11/13/12/34", "");
			assertEquals(400, result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "deleteEntry/" + entryID, "");
			
			
			
		}
		
		catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		
		
	}
	
	@Test
	public void testCreateEntry()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			
		    ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", "");
		    assertTrue(result.getResponse().trim().contains("0"));
		    
			result = c.sendRequest("GET", mainPath + "create/testname/description", ""); // testInput is															
			assertEquals(200, result.getHttpCode());
			assertTrue(result.getResponse().trim().contains("testname")); // "testInput" name is part of response
			
			String[] test = result.getResponse().split(":"); // get the id
			String entryID = test[1];
			
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().trim().contains("1"));
			
			result = c.sendRequest("POST", mainPath + "createComment/" + entryID + "/this is a test comment", "");
			
			result = c.sendRequest("GET", mainPath + "deleteEntry/" + entryID, "");
			assertEquals(200, result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().trim().contains("0"));
			
			
			System.out.println("Result of 'testExampleMethod': " + result.getResponse().trim());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}
	
	@Test
	public void testComments()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try{
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", mainPath + "createEntry/testname/description", "");
			
			String[] test = result.getResponse().split(":"); // get the id
			String entryID = test[1];
			
			result = c.sendRequest("POST", mainPath + "createComment/" + entryID + "/this is a test", "");
			test = result.getResponse().split(":");
			String commentID = test[2];
			
			result = c.sendRequest("POST", mainPath + "deleteComment/" + commentID, "");
			assertEquals(200, result.getHttpCode());
			result = c.sendRequest("POST", mainPath + "deleteComment/" + commentID, "");
			assertEquals(400, result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "deleteEntry/" + entryID, "");
			assertEquals(200, result.getHttpCode()); 
			
			result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().trim().contains("0"));
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		
	}
	
	/**
	 * Test the getDay Method
	 */
	
	@Test
	public void testDaySearch()
	{

		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try{
			
			ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", "");
			assertTrue(result.getResponse().trim().contains("0"));
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			result = c.sendRequest("GET", mainPath + "createEntry/testname/description", "");

			String[] test = result.getResponse().split(":"); // get the id
			String entryID = test[1];
			
			result = c.sendRequest("POST", mainPath + "setStart/" + entryID + "/2010/11/13/12/34" , "");
			
			result = c.sendRequest("POST", mainPath + "setEnd/" + entryID + "/2010/11/13/12/50", "");
			
			result = c.sendRequest("GET", mainPath + "getDay/2010/11/13", "");
			assertTrue(result.getResponse().trim().contains(entryID));			
			
			result = c.sendRequest("GET", mainPath + "createEntry/zweiter/description", "");
			test = result.getResponse().split(":"); // get the id
		    String entryIDsecond = test[1];
		    
		    result = c.sendRequest("POST", mainPath + "setStart/" + entryIDsecond + "/2011/12/13/12/34" , "");
		    result = c.sendRequest("POST", mainPath + "setEnd/" + entryIDsecond + "/2011/12/15/12/50", "");
		
		    result = c.sendRequest("GET", mainPath + "getDay/2011/12/14", "");
		    assertTrue(result.getResponse().trim().contains(entryIDsecond));
		    assertFalse(result.getResponse().trim().contains(entryID));
		    
		    result = c.sendRequest("GET", mainPath + "deleteEntry/" + entryIDsecond, "");
		    result = c.sendRequest("GET", mainPath + "deleteEntry/" + entryID, "");
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		
		
	}
	
	
	@Test
	public void xmlTEST()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
       try{
			
			Entry test = new Entry(testAgent.getId(), "hallo", "test", 10);
			test.setStart(2009, 9, 10, 10, 20);
			test.setEnd(2012, 12, 12, 11, 20);
			test.createComment(testAgent.getId(), "das ist ein kommentar");
			test.createComment(testAgent.getId(), "ein weiterer kommentar");
			String xml = test.toXmlString();
			
			Entry test2 = Entry.createFromXml(xml);
			
			assertTrue(test2.getTitle().equals("hallo"));
			assertTrue(test2.getComments().get(0).getMessage().equals("das ist ein kommentar"));
			assertTrue(test2.getComments().get(1).getMessage().equals("ein weiterer kommentar"));
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
		
	}
	
	/**
	 * Test the TemplateService for valid rest mapping.
	 * Important for development.
	 */
	@Test
	public void testDebugMapping()
	{
		MyCalendar cl = new MyCalendar();
		assertTrue(cl.debugMapping());
	}

}
