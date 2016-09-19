package i5.las2peer.services.calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.ServiceNameVersion;
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
	private static int HTTP_PORT = WebConnector.DEFAULT_HTTP_PORT;

	private static LocalNode node;
	private static WebConnector connector;
	private static ByteArrayOutputStream logStream;

	private static UserAgent testAgent;
	private static UserAgent secondAgent;
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
	@BeforeClass
	public static void startServer() throws Exception {

		// get unused port
		HTTP_PORT = getUnusedPort();

		// unlock agents
		UserAgent adam = MockAgentFactory.getAdam();
		adam.unlockPrivateKey(testPass);

		// start node
		node = LocalNode.newNode();
		node.storeAgent(adam);
		node.launch();
		ServiceAgent testService = ServiceAgent.createServiceAgent(
				ServiceNameVersion.fromString("i5.las2peer.services.calendar.MyCalendar@0.3"), "a pass");
		testService.unlockPrivateKey("a pass");

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
	public void intervallTest() {
		MiniClient c = new MiniClient();
		c.setAddressPort(HTTP_ADDRESS, HTTP_PORT);

		try {

			ClientResponse result = c.sendRequest("GET", mainPath + "getNumber", ""); // assert there is one entry from
																						// the previous test

			c.setLogin(Long.toString(testAgent.getId()), testPass);
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

			c.setLogin(Long.toString(testAgent.getId()), testPass);
			String content = "{\"title\":\"hello\", \"description\":\"test\", \"groupID\":\"1\"}";
			ClientResponse result = c.sendRequest("POST", mainPath + "create", content); // create an entry

			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			JSONObject params = (JSONObject) parser.parse(result.getResponse());
			String dateID = (String) params.get("entry_id"); // get the id of the second entry
			content = "{\"year\":\"2013\", \"month\":\"12\", \"day\":\"12\", \"hour\":\"16\", \"minute\":\"12\"}";
			result = c.sendRequest("PUT", mainPath + "setStart/" + dateID, content);
			content = "{\"year\":\"2013\", \"month\":\"12\", \"day\":\"12\", \"hour\":\"20\", \"minute\":\"12\"}";
			result = c.sendRequest("PUT", mainPath + "setEnd/" + dateID, content);
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

			c.setLogin(Long.toString(testAgent.getId()), testPass);
			ClientResponse result = c.sendRequest("GET", mainPath + "name/" + testAgent.getId(), "");
			assertEquals(result.getHttpCode(), 200);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception: " + e);
		}
	}

}
