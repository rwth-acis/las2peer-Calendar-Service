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

@SuppressWarnings("unused")
public class ServiceTest {


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
		node.launch();

		ServiceAgent testService = ServiceAgent.createServiceAgent(testTemplateService, "a pass");
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
	
	@Test
	public void monthTest()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try{
			
			ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", ""); 

            result = c.sendRequest("POST", mainPath + "create/imMonat/Dieser Eintrag ist im Monat drinnen", ""); //create another entry
			
            JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject)parser.parse(result.getResponse());

			String entryID = (String) params.get("entry_id");
			
			result = c.sendRequest("PUT", mainPath + "setStart/" + entryID + "/2005/5/4/15/12", "");
			result = c.sendRequest("PUT", mainPath + "setEnd/" + entryID + "/2005/5/4/16/12", "");
			
			result = c.sendRequest("POST", mainPath + "create/outsideMonat/Dieser Eintrag ist nicht im Monat drinnen", "");
			params = (JSONObject)parser.parse(result.getResponse());
			
			String wrongID = (String) params.get("entry_id"); //get the id of the second entry
			
			result = c.sendRequest("PUT", mainPath + "setStart/" + wrongID + "/2005/6/4/15/12", "");
			result = c.sendRequest("PUT", mainPath + "setEnd/" + wrongID + "/2005/7/4/16/12", "");
		
			result = c.sendRequest("GET", mainPath + "getDay/2005/5/4", "");
			
			assertTrue(result.getResponse().contains(entryID));
			assertFalse(result.getResponse().contains(wrongID));
			
			result = c.sendRequest("POST", mainPath + "create/outsideMonat/Dieser Eintrag beginnt vor und endet nach Monat", "");
		
			params = (JSONObject)parser.parse(result.getResponse());
			String betweenID = (String) params.get("entry_id"); //get the id of the second entry
			
			result = c.sendRequest("PUT", mainPath + "setStart/" + betweenID + "/2005/2/4/15/12", "");
			result = c.sendRequest("PUT", mainPath + "setEnd/" + betweenID + "/2005/8/4/16/12", "");
			
//			result = c.sendRequest("GET", mainPath + "getMonth/2005/5", "");
//			assertTrue(result.getResponse().contains(betweenID));
			
		} catch(Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}		
	}
	
	@Test
	public void arrayTest()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try
		{
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("POST", mainPath + "create/testentry/forcomments/", "");
			assertEquals(200, result.getHttpCode());
			
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject)parser.parse(result.getResponse());
			String ID = (String) params.get("entry_id"); //get the id of the entry
			
			result = c.sendRequest("POST", mainPath + "createComment/" + ID + "/pleasework", "");
			assertEquals(200, result.getHttpCode());
			assertTrue(result.getResponse().contains("pleasework"));
			
			result = c.sendRequest("PUT", mainPath + "setStart/" + ID + "/2013/3/11/15/12", "");
			assertEquals(200, result.getHttpCode());
		}
		
		catch(Exception e) {
		e.printStackTrace();
		fail("Exception: " + e);
		
	}	
	}
//
//	@Test
//	public void saveTEST()
//	{
//		MiniClient c = new MiniClient();
//		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
//		MiniClient c2 = new MiniClient();
//		c2.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
//		
//		try{
//			
//			c.setLogin(Long.toString(testAgent.getId()), testPass);
//			c2.setLogin(Long.toString(secondAgent.getId()), secondPass);
//			ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", ""); //assert there are no entries
//
//			assertEquals(400,result.getHttpCode());
//			
//		    result = c.sendRequest("POST", mainPath + "create/hello/test", ""); //create an entry
//			assertEquals(200,result.getHttpCode());
//			result = c.sendRequest("GET", mainPath + "getNumber", ""); // should return one 
//			
//			result = c.sendRequest("POST", mainPath + "create/neuereintrag/willkommen", ""); //create another entry
//			
//			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
//			JSONObject params = (JSONObject)parser.parse(result.getResponse());
//			
//			String deleteID = (String) params.get("entry_id"); //get the id of the second entry
//			result = c.sendRequest("GET", mainPath + "getNumber", "");
//			
//			result = c.sendRequest("PUT", mainPath + "setStart/" + deleteID + "/2002/3/9/15/12", "");
//			assertEquals(200,result.getHttpCode());
//			 
//			result = c.sendRequest("PUT", mainPath + "setEnd/" + deleteID + "/2002/3/9/15/22", "");
//			
//			result = c.sendRequest("GET", mainPath + "getDay/2002/3/9", "");
//			result = c.sendRequest("GET", mainPath + "getDay/2002/3/8", "");
//			assertFalse(result.getResponse().contains("willkommen"));
//			
//			result = c.sendRequest("DELETE", mainPath + "deleteEntry/" + deleteID, ""); //delete the second entry
//			assertEquals(200,result.getHttpCode());
//			
//			result = c.sendRequest("POST", mainPath + "create/deletethis/iwanttobedeleted", "");
//			params = (JSONObject)parser.parse(result.getResponse());
//			
//			deleteID = (String) params.get("entry_id"); // get the id
//			
//			result = c2.sendRequest("DELETE", mainPath + "deleteEntry/" + deleteID, ""); 
//			assertEquals(403,result.getHttpCode()); //shouldn't be able to delete this time
//			
//			result = c.sendRequest("DELETE", mainPath + "deleteEntry/" + deleteID, ""); //should be able to delete
//			assertEquals(200,result.getHttpCode());
//			
//		} catch(Exception e) {
//			e.printStackTrace();
//			fail("Exception: " + e);
//		}
//		
//	}
//	
	@Test
	public void intervallTest()
	{
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);
		
		try{
			
			ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", ""); //assert there is one entry from the previous test
			
			c.setLogin(Long.toString(testAgent.getId()), testPass);
			result = c.sendRequest("POST", mainPath + "createRegular/2012/2/3/14/22/15/24/wocheneintrag/jedewoche/week/2", ""); //create two entries
			assertEquals(200, result.getHttpCode());
		
			result = c.sendRequest("GET", mainPath + "getDay/2012/2/10", "");
			assertEquals(200, result.getHttpCode());
			
			result = c.sendRequest("GET", mainPath + "getDay/2042/2/3", ""); 
			
			result = c.sendRequest("POST", mainPath + "createRegular/2002/5/10/12/12/17/34/monatseintrag/jedermonat/month/4", ""); //create 4 entries
			assertEquals(200, result.getHttpCode());
		
			
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
