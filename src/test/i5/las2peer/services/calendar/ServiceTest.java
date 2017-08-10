package i5.las2peer.services.calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.persistency.SharedStorage.STORAGE_MODE;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;
import i5.las2peer.testing.TestSuite;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

@SuppressWarnings("unused")
public class ServiceTest {

	private static final String HTTP_ADDRESS = "http://127.0.0.1";
	private static int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static ArrayList<PastryNodeImpl> nodes;
	private static PastryNodeImpl node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgentImpl testAgent;
	private static UserAgentImpl secondAgent;
	private static ServiceAgentImpl testService;
	private static final String testPass = "adamspass";
	private static final String secondPass = "evespass";

	private static final String mainPath = "calendar/";

	private static int getUnusedPort() {

		int port = HTTP_PORT;
		try {

			ServerSocket socket = new ServerSocket(0);
			port = socket.getLocalPort();
			socket.close();

		} catch (IOException e) {

		}
		return port;
	}

	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@Before
	public void startServer() throws Exception {

		nodes = TestSuite.launchNetwork(1, STORAGE_MODE.FILESYSTEM, true);
		node = nodes.get(0);
		// get unused port
		HTTP_PORT = getUnusedPort();

		// unlock agents
		UserAgentImpl adam = MockAgentFactory.getAdam();
		adam.unlock(testPass);
		// start node
		node.storeAgent(adam);
		testService = ServiceAgentImpl.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.services.calendar.MyCalendar@0.2"), "someNewPass");
		testService.unlock("someNewPass");

		node.registerReceiver(testService);

		// start connector
		logStream = new ByteArrayOutputStream();

		connector = new WebConnector(true, HTTP_PORT, false, 1000);
		connector.setLogStream(new PrintStream(logStream));
		connector.start(node);
		Thread.sleep(1000); // wait a second for the connector to become ready
		testAgent = MockAgentFactory.getAdam();

	}

	/**
	 * Called after the tests have finished. Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@After
	public void shutDownServer() throws Exception {

		connector.stop();
		node.shutDown();

		connector = null;
		node = null;

		System.out.println("Connector-Log:");
		System.out.println("--------------");

		System.out.println(logStream.toString());

	}

	@Test
	public void intervallTest() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {

			ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", ""); // assert there is one entry from
																						// the previous test

			c.setLogin(testAgent.getIdentifier(), testPass);
			String content = "{\"title\":\"zweiWochen\", \"description\":\"testEntry\",\"interval\":\"week\",\"number\":\"2\",\"comments\":\"1\", \"syear\":\"2012\", \"smonth\":\"12\", \"sday\":\"1\", \"shour\":\"16\", \"sminute\":\"12\", \"eyear\":\"2012\", \"emonth\":\"12\", \"eday\":\"1\", \"ehour\":\"20\", \"eminute\":\"12\"}";
			result = c.sendRequest("POST", mainPath + "createRegular", content); // create two
			// entries
			assertEquals(200, result.getHttpCode());

			result = c.sendRequest("GET", mainPath + "getDay/2012/12/1", "");
			assertEquals(200, result.getHttpCode());

			result = c.sendRequest("GET", mainPath + "getDay/2042/12/15", "");

			content = "{\"title\":\"monatsEintrag\", \"description\":\"jederMonat\",\"interval\":\"month\",\"number\":\"4\",\"comments\":\"1\", \"syear\":\"2012\", \"smonth\":\"4\", \"sday\":\"1\", \"shour\":\"16\", \"sminute\":\"12\", \"eyear\":\"2012\", \"emonth\":\"4\", \"eday\":\"1\", \"ehour\":\"20\", \"eminute\":\"12\"}";
			result = c.sendRequest("POST", mainPath + "createRegular", content); // create
																					// 4
			// entries
			assertEquals(200, result.getHttpCode());

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void dateTEST() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {

			c.setLogin(testAgent.getIdentifier(), testPass);
			String content = "{\"title\":\"hello\", \"description\":\"test\", \"groupID\":\"1\"}";
			ClientResponse result = c.sendRequest("POST", mainPath + "create", content); // create an entry
			assertEquals(200, result.getHttpCode());
			System.out.println(result.getResponse());
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject) parser.parse(result.getResponse());
			String dateID = (String) params.get("entry_id"); // get the id of the second entry
			content = "{\"year\":\"2013\", \"month\":\"12\", \"day\":\"12\", \"hour\":\"16\", \"minute\":\"12\"}";
			result = c.sendRequest("PUT", mainPath + "setStart/" + dateID, content);
			assertEquals(200, result.getHttpCode());
			System.out.println(result.getResponse());
			content = "{\"year\":\"2013\", \"month\":\"12\", \"day\":\"12\", \"hour\":\"20\", \"minute\":\"12\"}";
			result = c.sendRequest("PUT", mainPath + "setEnd/" + dateID, content);
			System.out.println(result.getResponse());
			assertEquals(200, result.getHttpCode());

			result = c.sendRequest("GET", mainPath + "getDay/2013/12/11", "");
			assertEquals(200, result.getHttpCode());

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

	@Test
	public void nameTest() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {

			c.setLogin(testAgent.getIdentifier(), testPass);
			ClientResponse result = c.sendRequest("GET", mainPath + "name/" + testAgent.getIdentifier(), "");
			assertEquals(result.getHttpCode(), 200);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

}
