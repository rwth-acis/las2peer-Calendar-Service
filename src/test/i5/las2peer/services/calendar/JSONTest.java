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

public class JSONTest {


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
		node.storeAgent(MockAgentFactory.getEve());
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

	
	@Test
	public void testDebugMapping()
	{
		MyCalendar cl = new MyCalendar();
		assertTrue(cl.debugMapping());
	}


}
