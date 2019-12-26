package edu.northeastern.ccs.im;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opentest4j.AssertionFailedError;

import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.ScanNetNB;
import edu.northeastern.ccs.im.SocketNB;
import edu.northeastern.ccs.im.dao.GroupMessageDao;
import edu.northeastern.ccs.im.dao.GroupsDao;
import edu.northeastern.ccs.im.dao.MessageDao;
import edu.northeastern.ccs.im.dao.UserDao;
import edu.northeastern.ccs.im.model.Groups;
import edu.northeastern.ccs.im.model.User;
import edu.northeastern.ccs.im.server.ClientRunnable;
import edu.northeastern.ccs.im.server.Prattle;

public class ClientRunnablePart2Test {

	private static final Logger LOGGER = Logger.getLogger(
			Thread.currentThread().getStackTrace()[0].getClassName() );
	private static final int PORT = 4596;
	private static Queue<ClientRunnable> active;
	static {
		active = new ConcurrentLinkedQueue<ClientRunnable>();
	}
	static ServerSocketChannel serverSocket;
	static Selector selector;
	static ScheduledExecutorService threadPool;

	@Mock
	private UserDao mockUserDao;

	@Mock
	private GroupsDao mockGroupsDao;



	@BeforeEach
	void setUpMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@BeforeAll
	public static void setUp()
	{
		RunnableDemo server = new RunnableDemo();
		server.run();
	}

	@AfterAll
	public static void newclose()
	{
		RunnableDemo server2 = new RunnableDemo();
		//server2.close();
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
				threadPool = Executors.newScheduledThreadPool(300);
			}
			catch (Exception e) {
				LOGGER.log(Level.SEVERE,"Exception Occured", e);
			}
		}

		public void close() {
			try {
				Assertions.assertThrows(AssertionFailedError.class, () -> {
					assertTimeoutPreemptively(Duration.ofMillis(2000), () -> {
						SocketNB socket = new SocketNB("localhost", PORT);
						Prattle.startListen(selector, serverSocket, threadPool);
						return "success";
					});
				});
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

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}


	@Test
	void messageFunctionsDeleteTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteMessage("testName", "delete");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}
	
	@Test
	void messageFunctionsCreateGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeCreateGroupMessage("testName", "testGroup");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}

	@Test
	void messageFunctionsAddGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeAddToGroupMessage("testName", "test1-testGroup");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}
	
	@Test
	void messageFunctionsDeleteFromGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteFromGroupMessage("testName", "test1-testGroup");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}
	
	
	@Test
	void messageFunctionsUpdateGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeUpdateGroupMessage("testName", "testGroup-newName");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}
	
/*	@Test
	void messageFunctionsListGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("TestBad");
		Message testMessage = Message.makeListGroupMessage("TestBad", "TestBadGrp");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}*/
	
	@Test
	void messageFunctionsDeleteGroupTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteGroupMessage("testName", "testGroup");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}
	
	@Test
	void messageFunctionsSaveMessageTest() throws Exception {
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeMessage("UMG","recall1", "/message recall2-text");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}
	
/*	@Test
	void messageFunctionsSaveGrpMessageTest() throws Exception {
		getSocketAndClient();
		client.setName("testName2");
		Message testMessage = Message.makeMessage("GMG","recall1", "/msgGroup recall2-text");

		Method userUpdate = client.getClass().getDeclaredMethod("messageFunctions", Message.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage);
	}*/
	
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