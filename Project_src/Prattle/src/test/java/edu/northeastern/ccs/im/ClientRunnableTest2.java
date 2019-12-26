package edu.northeastern.ccs.im;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.northeastern.ccs.im.server.ClientRunnable;

public class ClientRunnableTest2 {

	private static final Logger LOGGER = Logger.getLogger(
			Thread.currentThread().getStackTrace()[0].getClassName() );
	private static final int PORT = 4784;
	private static Queue<ClientRunnable> active;
	static {
		active = new ConcurrentLinkedQueue<ClientRunnable>();
	}
	static ServerSocketChannel serverSocket;
	static Selector selector;
	static ScheduledExecutorService threadPool;

	@BeforeAll
	public static void setUp()
	{
		RunnableDemo server = new RunnableDemo();
		server.run();
	}

	public static class RunnableDemo{

		public void run()
		{
			try {
				serverSocket = ServerSocketChannel.open();
				serverSocket.configureBlocking(false);
				serverSocket.socket().bind(new InetSocketAddress(PORT));
				selector = SelectorProvider.provider().openSelector();
				serverSocket.register(selector, SelectionKey.OP_ACCEPT);
				threadPool = Executors.newScheduledThreadPool(20);
			}
			catch (Exception e) {
				LOGGER.log(Level.SEVERE,"Exception Occured", e);
			}
		}
	}

	//****************************************************************************
	// client runnable

	static SocketNB socket;
	static ClientRunnable client;
	static void getSocketAndClient(){
		try {
			socket = new SocketNB("localhost", PORT);
			client = new ClientRunnable(socket.getSocket());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void messageFunctionsUpdateTest() throws Exception {
		getSocketAndClient();
		client.setName("newName");
		Message testMessage = Message.makeUpdateMessage("newName", "newName");

		client.messageFunctions(testMessage);
		/*    Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
    userUpdate.setAccessible(true);
    userUpdate.invoke(client, testMessage);*/
	}


	@Test
	void messageFunctionsDeleteTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteMessage("testName", "delete");

		client.messageFunctions(testMessage);
		/*    Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
    userUpdate.setAccessible(true);
    userUpdate.invoke(client, testMessage);*/
	}

	@Test
	void messageFunctionsCreateGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeCreateGroupMessage("testName", "testGroup");

		client.messageFunctions(testMessage);
		/*    Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
    userUpdate.setAccessible(true);
    userUpdate.invoke(client, testMessage);*/
	}

	@Test
	void messageFunctionsAddGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeAddToGroupMessage("testName", "test1-testGroup");

		client.messageFunctions(testMessage);
		/*    Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
    userUpdate.setAccessible(true);
    userUpdate.invoke(client, testMessage);*/
	}

	@Test
	void messageFunctionsDeleteFromGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteFromGroupMessage("testName", "test1-testGroup");

		client.messageFunctions(testMessage);
		/*    Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
    userUpdate.setAccessible(true);
    userUpdate.invoke(client, testMessage);*/
	}


	@Test
	void messageFunctionsUpdateGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeUpdateGroupMessage("testName", "testGroup-newName");

		client.messageFunctions(testMessage);
		/*    Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
    userUpdate.setAccessible(true);
    userUpdate.invoke(client, testMessage);*/
	}

	@Test
	void messageFunctionsListGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("TestBad");
		Message testMessage = Message.makeListGroupMessage("TestBad", "TestBadGrp");

		//client.messageFunctions(testMessage);

		/*		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);*/
	}

	@Test
	void messageFunctionsDeleteGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteGroupMessage("testName", "testGroup");
		client.messageFunctions(testMessage);

		/*    Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
    userUpdate.setAccessible(true);
    userUpdate.invoke(client, testMessage);*/
	}

	@Test
	void messageFunctionsSaveMessageTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeMessage("UMG","recall1", "/message recall2-text");

		client.messageFunctions(testMessage);

		/*    Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
    userUpdate.setAccessible(true);
    userUpdate.invoke(client, testMessage);*/
	}

	@Test
	void messageFunctionsSaveGrpMessageTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeMessage("GMG","recall1", "/msgGroup recall2-text");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}

	@Test
	void messageFunctionsRecallMessageTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeMessage("RCL","recall1", "/recall recall2-text");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}

}