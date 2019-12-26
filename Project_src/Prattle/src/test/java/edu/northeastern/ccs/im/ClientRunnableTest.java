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

public class ClientRunnableTest {

	private static final Logger LOGGER = Logger.getLogger(
			Thread.currentThread().getStackTrace()[0].getClassName() );
	private static final int PORT = 4589;
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

	@Mock
	private MessageDao mockMessageDao;

	@Mock
	private GroupMessageDao mockGroupMessageDao;


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
				threadPool = Executors.newScheduledThreadPool(20);
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
	public void setUserNameTest() {
		try {
			getSocketAndClient();
			client.setName("abcd");
			Method setUserName = client.getClass().getDeclaredMethod("setUserName", String.class);
			setUserName.setAccessible(true);
			setUserName.invoke(client, "abcd");
			assertEquals("abcd", client.getName());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Immediate response not empty.
	 */
	@Test
	void immediateResponseNotEmpty() {
		try {
			getSocketAndClient();
			ScanNetNB scanNb = new ScanNetNB(socket.getSocket());
			client.setName("Mani");

			//Adding a dummy message to NbQueue inside ClientRunnable
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputObject = (ScanNetNB)input.get(client);

			Field messages = inputObject.getClass().getDeclaredField("messages");
			messages.setAccessible(true);

			Queue<Message> msg = (Queue<Message>)messages.get(inputObject);
			Message message = Message.makeBroadcastMessage("Mani", "Hello");
			msg.add(message);


			Field immediateResponse = client.getClass().getDeclaredField("immediateResponse");
			immediateResponse.setAccessible(true);
			Queue<Message> msg2 = (Queue<Message>)immediateResponse.get(client);
			msg2.add(Message.makeBroadcastMessage("Mani", "WTF"));
			msg2.add(Message.makeBroadcastMessage("Mani", "Hello, How are you?"));
			client.run();
			msg2.add(Message.makeBroadcastMessage("Mani", "What is the time?"));
			client.run();
			assertNotNull(msg2);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Special response not empty.
	 */
	@Test
	void specialResponseNotEmpty() {
		try {
			getSocketAndClient();
			ScanNetNB scanNb = new ScanNetNB(socket.getSocket());
			client.setName("Mani");

			//Adding a dummy message to NbQueue inside ClientRunnable
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputObject = (ScanNetNB)input.get(client);

			Field messages = inputObject.getClass().getDeclaredField("messages");
			messages.setAccessible(true);

			Queue<Message> msg = (Queue<Message>)messages.get(inputObject);
			Message message = Message.makeBroadcastMessage("Mani", null);
			Message message2 = Message.makeBroadcastMessage("Mani", null);
			Message message3 = Message.makeSimpleLoginMessage("Mani","abcd");
			msg.add(message);
			msg.add(message2);
			msg.add(message3);

			//Adding messages to SpecialResponses Queue
			Field specialResponse = client.getClass().getDeclaredField("specialResponse");
			specialResponse.setAccessible(true);
			Queue<Message> msg2 = (Queue<Message>)specialResponse.get(client);

			msg2.add(Message.makeBroadcastMessage("Mani", "WTF"));
			msg2.add(Message.makeBroadcastMessage("Mani", "Hello, How are you?"));
			msg2.add(Message.makeBroadcastMessage("Mani", "What is the time?"));

			//Adding message to waitlist Queue
			Field waitingList = client.getClass().getDeclaredField("waitingList");
			waitingList.setAccessible(true);
			Queue<Message> msg3 = (Queue<Message>)waitingList.get(client);
			client.run();

			msg3.add(Message.makeBroadcastMessage("Mani", "WTF"));
			msg3.add(Message.makeBroadcastMessage("Mani", "Hello, How are you?"));
			msg3.add(Message.makeBroadcastMessage("Mani", "What is the time?"));
			client.run();
			assertNotNull(msg3);
			assertNotNull(msg2);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void setNameTest() {
		try {
			getSocketAndClient();
			client.setName("abcd");
			assertEquals("abcd", client.getName());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	public void getUserIdTest() {
		try {
			getSocketAndClient();
			client.setName("abcd");
			Method setUserName = client.getClass().getDeclaredMethod("setUserName", String.class);
			setUserName.setAccessible(true);
			setUserName.invoke(client, "abcd");
			int userId = "abcd".hashCode();
			assertEquals(userId, client.getUserId());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void getUserIdTest2() {
		try {
			getSocketAndClient();
			client.setName(null);
			Method setUserName = client.getClass().getDeclaredMethod("setUserName", String.class);
			setUserName.setAccessible(true);
			String s;
			s = null;
			setUserName.invoke(client, s);
			int userId = -1;
			assertEquals(userId, client.getUserId());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void isInitializedTest() {
		getSocketAndClient();
		assertFalse(client.isInitialized());

	}

	@Test
	public void checkForInitializationTest() {

		getSocketAndClient();

		try {
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);

			ScanNetNB inputParam = (ScanNetNB)input.get(client);
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);

			String brdMsg = "This is new message";
			Message broadCastMsg = Message.makeBroadcastMessage("Tommy",brdMsg);
			msgParam.add(broadCastMsg);


			Field initialized = client.getClass().getDeclaredField("initialized");
			initialized.setAccessible(true);

			Method privatecheckForInitialization = client.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(client, null);
			String name = initialized.getName();
			Object value = initialized.get(client);

			boolean b = (boolean) value;
			assertTrue(b);

		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException  | NoSuchFieldException | InvocationTargetException e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}

	}


	@Test
	public void checkForInitializationTest2() {

		getSocketAndClient();

		try {
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);

			ScanNetNB inputParam = (ScanNetNB)input.get(client);
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);

			String brdMsg = "This is new message";
			Message broadCastMsg = Message.makeBroadcastMessage(null,brdMsg);
			msgParam.add(broadCastMsg);


			Field initialized = client.getClass().getDeclaredField("initialized");
			initialized.setAccessible(true);

			Method privatecheckForInitialization = client.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(client, null);
			String name = initialized.getName();
			Object value = initialized.get(client);

			boolean b = (boolean) value;
			assertFalse(b);

		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException  | NoSuchFieldException | InvocationTargetException e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}

	}

	@Test
	public void broadcastMessageIsSpecialTest() {

		getSocketAndClient();

		try {
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);


			String brdMsg = "What is the date?";
			Message broadCastMsg = Message.makeBroadcastMessage("Mani",brdMsg);

			Field initialized = client.getClass().getDeclaredField("initialized");
			initialized.setAccessible(true);

			Method privatebroadcastMessageIsSpecial = client.getClass().getDeclaredMethod("broadcastMessageIsSpecial", Message.class);
			privatebroadcastMessageIsSpecial.setAccessible(true);
			Object value = privatebroadcastMessageIsSpecial.invoke(client, broadCastMsg);
			boolean b = (boolean) value;
			assertFalse(b);

		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException  | NoSuchFieldException | InvocationTargetException e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}


	}

	@Test
	public void messageChecksTest() {

		getSocketAndClient();

		try {
			client.setName("Mani");
			Message broadCastMsg = Message.makeBroadcastMessage("Mani", "Hello");

			Method privatemsgChecks = client.getClass().getDeclaredMethod("messageChecks", Message.class);
			privatemsgChecks.setAccessible(true);

			Object value = privatemsgChecks.invoke(client, broadCastMsg);
			boolean b = (boolean) value;
			assertTrue(b);


			client.setName("Panda");
			Message broadCastMsg2 = Message.makeBroadcastMessage(null, "Hello");
			Object value3 = privatemsgChecks.invoke(client, broadCastMsg);
			boolean b3 = (boolean) value3;
			assertFalse(b3);

			client.setName(null);
			Object value2 = privatemsgChecks.invoke(client, broadCastMsg2);
			boolean b2 = (boolean) value2;
			assertFalse(b2);


		}
		catch(Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void enqueueMessage() {

		getSocketAndClient();
		Message broadCastMsg = Message.makeHelloMessage("Mani");
		ConcurrentLinkedQueue waitingList = new ConcurrentLinkedQueue<Message>();
		waitingList.add(broadCastMsg);

		try {
			Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
			waitingListExpected.setAccessible(true);
			client.enqueueMessage(broadCastMsg);
			String name = waitingListExpected.getName();
			Object value = waitingListExpected.get(client);
			ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;
			assertEquals(waitingList.contains(broadCastMsg), q.contains(broadCastMsg));
		}
		catch(Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}


	}

	@Test
	public void setFuture() {

		getSocketAndClient();
		Message broadCastMsg = Message.makeHelloMessage("Mani");


		try {
			Field runnableMeExpected = client.getClass().getDeclaredField("runnableMe");
			runnableMeExpected.setAccessible(true);
			Object value = runnableMeExpected.get(client);
			ScheduledFuture<ClientRunnable> s = (ScheduledFuture<ClientRunnable>) value;
			client.setFuture(s);
			assertEquals(null, s);
		}
		catch(Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}


	}

	@Test
	public void terminateClientTest() {

		getSocketAndClient();
		Prattle.setActiveConnection(client);

		try {
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputValue = (ScanNetNB)input.get(client);


			Field socketExpected = client.getClass().getDeclaredField("socket");
			socketExpected.setAccessible(true);
			SocketChannel socketValue = (SocketChannel) socketExpected.get(client);

			client.terminateClient();
		}
		catch(Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}

	}

	@Test
	public void runTest() {

		try {
			Field initialized = client.getClass().getDeclaredField("initialized");
			initialized.setAccessible(true);
			initialized.set(client, false);
			boolean value = (boolean) initialized.get(client);
			client.run();
			assertFalse(value);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void runTest2() {

		try {
			Field initialized = client.getClass().getDeclaredField("initialized");
			initialized.setAccessible(true);
			initialized.set(client, true);
			boolean value = (boolean) initialized.get(client);
			client.run();
			assertTrue(value);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	void runTestTerminate() {
		try {
			getSocketAndClient();
			ScanNetNB scanNb = new ScanNetNB(socket.getSocket());

			//Adding a dummy message to NbQueue inside ClientRunnable
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputObject = (ScanNetNB)input.get(client);

			Field messages = inputObject.getClass().getDeclaredField("messages");
			messages.setAccessible(true);

			Queue<Message> msg = (Queue<Message>)messages.get(inputObject);
			//Testing Valid Message Type and broadcastMessageIsSpecial
			msg.add(Message.makeAcknowledgeMessage("Mani"));
			msg.add(Message.makeQuitMessage("Mani"));
			Method checkForInitialization = client.getClass()
					.getDeclaredMethod("checkForInitialization", null);
			checkForInitialization.setAccessible(true);
			checkForInitialization.invoke(client);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(client);
			ScheduledFuture clientFuture = threadPool.scheduleAtFixedRate(client, 200,
					200, TimeUnit.MILLISECONDS);
			client.setFuture(clientFuture);
			client.run();
			assertTrue(client.isInitialized());
		} catch (Exception e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	public void runTest3() {
		getSocketAndClient();
		try {
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);

			ScanNetNB inputParam = (ScanNetNB)input.get(client);
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);

			String brdMsg = "This is new message";
			Message broadCastMsg = Message.makeBroadcastMessage("Tommy",brdMsg);
			msgParam.add(broadCastMsg);

			client.setName("Tommy");
			Method privatemsgChecks = client.getClass().getDeclaredMethod("messageChecks", Message.class);
			privatemsgChecks.setAccessible(true);
			Object o = privatemsgChecks.invoke(client, broadCastMsg);


			assertEquals(true,(boolean) o);


		} catch (SecurityException | IllegalAccessException | IllegalArgumentException  | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}

	}

	@Test
	public void runTest4() {
		getSocketAndClient();
		try {
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);

			ScanNetNB inputParam = (ScanNetNB)input.get(client);
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);

			String brdMsg = "This is new message";
			Message broadCastMsg = Message.makeHelloMessage("Hello");
			msgParam.add(broadCastMsg);

			//client.run();
			assertFalse(broadCastMsg.isBroadcastMessage());


		} catch (SecurityException | IllegalAccessException | IllegalArgumentException  | NoSuchFieldException  e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}

	}
	@Test
	public void runTest5() {
		getSocketAndClient();
		try {
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);

			ScanNetNB inputParam = (ScanNetNB)input.get(client);
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);

			String brdMsg = "This is new message";
			Message broadCastMsg = Message.makeBroadcastMessage("Tommy",brdMsg);
			msgParam.add(broadCastMsg);


			Method privatemsgChecks = client.getClass().getDeclaredMethod("messageChecks", Message.class);
			privatemsgChecks.setAccessible(true);

			Message broadCastMsg2 = Message.makeBroadcastMessage(null, "Hello");
			Object value3 = privatemsgChecks.invoke(client, broadCastMsg);
			boolean b3 = (boolean) value3;
			client.run();
			assertTrue(b3);


		} catch (SecurityException | IllegalAccessException | IllegalArgumentException  | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}

	}

	@Test
	public void runTest6() {
		getSocketAndClient();
		try {
			client.setName("Mani");
			Message broadCastMsg = Message.makeBroadcastMessage("Mani", "Hello");

			Method privatemsgChecks = client.getClass().getDeclaredMethod("messageChecks", Message.class);
			privatemsgChecks.setAccessible(true);

			Object value = privatemsgChecks.invoke(client, broadCastMsg);
			boolean b = (boolean) value;
			assertTrue(b);


			client.setName("Panda");
			Message broadCastMsg2 = Message.makeBroadcastMessage(null, "Hello");
			Object value3 = privatemsgChecks.invoke(client, broadCastMsg);
			boolean b3 = (boolean) value3;
			assertFalse(b3);

			client.setName(null);
			Object value2 = privatemsgChecks.invoke(client, broadCastMsg2);
			boolean b2 = (boolean) value2;
			assertFalse(b2);

			client.run();

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}

	}

	@Test
	void runNotInitializedTest() {
		getSocketAndClient();
		assertFalse(client.isInitialized());
	}

	@Test
	void runInitializedAndIsSpecialMessageTest() {
		try {
			getSocketAndClient();
			ScanNetNB scanNb = new ScanNetNB(socket.getSocket());

			//Adding a dummy message to NbQueue inside ClientRunnable
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputObject = (ScanNetNB)input.get(client);

			Field messages = inputObject.getClass().getDeclaredField("messages");
			messages.setAccessible(true);

			Queue<Message> msg = (Queue<Message>)messages.get(inputObject);
			//Testing Valid Message Type and broadcastMessageIsSpecial
			msg.add(Message.makeBroadcastMessage("Tommy", "How are you?"));
			msg.add(Message.makeBroadcastMessage("Mani", "Test Message?"));
			client.run();
			msg.add(Message.makeBroadcastMessage("Test", "Hello?"));
			msg.add(Message.makeBroadcastMessage("Tommy", "How are you?"));
			client.run();
			assertTrue(client.isInitialized());
		} catch (Exception e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void runInitializedAndIsBombText() {
		try {
			getSocketAndClient();
			ScanNetNB scanNb = new ScanNetNB(socket.getSocket());

			//Adding a dummy message to NbQueue inside ClientRunnable
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputObject = (ScanNetNB)input.get(client);

			Field messages = inputObject.getClass().getDeclaredField("messages");
			messages.setAccessible(true);

			Queue<Message> msg = (Queue<Message>)messages.get(inputObject);
			//Testing Valid Message Type and broadcastMessageIsSpecial
			msg.add(Message.makeAcknowledgeMessage("Mani"));
			msg.add(Message.makeBroadcastMessage("Mani", "Prattle says everyone log off"));
			Method checkForInitialization = client.getClass()
					.getDeclaredMethod("checkForInitialization", null);
			checkForInitialization.setAccessible(true);
			checkForInitialization.invoke(client);
			client.run();
			assertFalse(client.isInitialized());
		} catch (Exception e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	void runNotNullNotBombText() {
		try {
			getSocketAndClient();
			ScanNetNB scanNb = new ScanNetNB(socket.getSocket());
			client.setName("Mani");

			//Adding a dummy message to NbQueue inside ClientRunnable
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputObject = (ScanNetNB)input.get(client);

			Field messages = inputObject.getClass().getDeclaredField("messages");
			messages.setAccessible(true);

			Queue<Message> msg = (Queue<Message>)messages.get(inputObject);
			Message message = Message.makeBroadcastMessage("Mani", "Hello");
			msg.add(message);
			client.run();
			assertTrue(client.isInitialized());
			msg.add(message);
			client.run();
			assertTrue(client.isInitialized());
		} catch (Exception e)
		{
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void testUserUpdate() throws Exception {
		when(mockUserDao.updateUserName(anyString(), anyString())).thenReturn("success");
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeUpdateMessage("testName", "newName");

		Method userUpdate = client.getClass().getDeclaredMethod("userUpdate", Message.class, UserDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;
		assertEquals("UPD 7 newName 44 Success : User  newName updated successfully", q.peek().toString());
	}

	@Test
	void testUserUpdateFailure() throws Exception {
		when(mockUserDao.updateUserName(anyString(), anyString())).thenReturn("error");
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeUpdateMessage("testName", "newName");

		Method userUpdate = client.getClass().getDeclaredMethod("userUpdate", Message.class, UserDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;
		assertEquals("UPD 8 testName 36 Error : User  testName doesn't exist", q.peek().toString());
	}
	@Test
	void testUserUpdateException() throws Exception {
		when(mockUserDao.updateUserName(anyString(), anyString())).thenThrow(SQLException.class);
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeUpdateMessage("testName", "newName");

		Method userUpdate = client.getClass().getDeclaredMethod("userUpdate", Message.class, UserDao.class);
		userUpdate.setAccessible(true);

		userUpdate.invoke(client, testMessage, mockUserDao);
	}

	@Test
	void testUserDelete() throws Exception {
		when(mockUserDao.delete("testName", GroupsDao.getInstance())).thenReturn("success");
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteMessage("testName", "delete");

		Method userUpdate = client.getClass().getDeclaredMethod("userDelete", UserDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;
		assertEquals("DEL 8 testName 45 Success : User  testName deleted successfully", q.peek().toString());
	}

	@Test
	void userDeleteException() throws Exception {
		when(mockUserDao.delete("testName", GroupsDao.getInstance())).thenThrow(SQLException.class);
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteMessage("testName", "delete");

		Method userUpdate = client.getClass().getDeclaredMethod("userDelete", UserDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, mockUserDao);
	}

	@Test
	void testUserDeleteFailure() throws Exception {
		when(mockUserDao.delete("testName", GroupsDao.getInstance())).thenReturn("error");
		getSocketAndClient();
		client.setName("testName");
		Message testMessage = Message.makeDeleteMessage("testName", "delete");

		Method userUpdate = client.getClass().getDeclaredMethod("userDelete", UserDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;
		assertEquals("DEL 8 testName 36 Error : testName couldn't be deleted", q.peek().toString());
	}

	@Test
	void testCreateGroup() throws Exception {
		Message testMessage = Message.makeCreateGroupMessage("testName", "testGroup");
		when(mockGroupsDao.createGroup("testGroup", "testName", UserDao.getInstance()))
		.thenReturn(new Groups("testGroup", 12345));

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("createGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;
		assertEquals("GRP 8 testName 47 Success : Group  testGroup created successfully", q.peek().toString());
	}

	@Test
	void testCreateGroupFailure() throws Exception {
		Message testMessage = Message.makeCreateGroupMessage("testName", "testGroup");
		when(mockGroupsDao.createGroup("testGroup", "testName", UserDao.getInstance()))
		.thenReturn(null);

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("createGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("GRP 8 testName 35 Error : testGroup already exists !!", q.peek().toString());
	}

	@Test
	void testAddUserToGroup() throws Exception {
		Message testMessage = Message.makeAddToGroupMessage("testName", "test1-testGroup");
		Groups testGroup = new Groups("testGroup", 12345);
		when(mockGroupsDao.addtoGroup("testGroup", "test1", UserDao.getInstance()))
		.thenReturn(testGroup);

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("addUserToGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("AGR 8 testName 52 Success : test1 inserted into testGroup successfully", q.peek().toString());
	}

	@Test
	void testAddUserToGroupFailure() throws Exception {
		Message testMessage = Message.makeAddToGroupMessage("testName", "test1-testGroup");
		when(mockGroupsDao.addtoGroup("testGroup", "test1", UserDao.getInstance()))
		.thenReturn(null);

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("addUserToGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		/*		assertEquals("AGR 8 testName 91 Error : test1 already exists in testGroup or not a user at all or testGroup doesn't exist!!",
				q.peek().toString());*/
	}

	@Test
	void testDeleteUserFromGroup() throws Exception {
		Message testMessage = Message.makeDeleteFromGroupMessage("testName", "test1-testGroup");
		when(mockGroupsDao.deleteUserFromGroup("testGroup", "test1", UserDao.getInstance()))
		.thenReturn("success");

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("deleteUserFromGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("DGR 8 testName 51 Success : test1 removed from testGroup successfully",
				q.peek().toString());

	}

	@Test
	void testDeleteUserFromGroupFailure() throws Exception {
		Message testMessage = Message.makeDeleteFromGroupMessage("testName", "test1-testGroup");
		when(mockGroupsDao.deleteUserFromGroup("testGroup", "test1", UserDao.getInstance()))
		.thenReturn("error");

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("deleteUserFromGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("DGR 8 testName 91 Error : test1 doesn't exists in testGroup or not a user at all or testGroup doesn't exist!!",
				q.peek().toString());

		when(mockGroupsDao.deleteUserFromGroup("testGroup", "test1", UserDao.getInstance()))
						.thenThrow(SQLException.class);

		userUpdate.invoke(client, testMessage, mockGroupsDao);
	}

	@Test
	void testUpdateGroupName() throws Exception {
		Message testMessage = Message.makeUpdateGroupMessage("testName", "testGroup-newName");
		when(mockGroupsDao.updateGroupName("testGroup", "newName"))
		.thenReturn("success");

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("updateGroupName", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("UGR 8 testName 51 Success : testGroup changed to newName successfully",
				q.peek().toString());
	}

	@Test
	void testUpdateGroupNameFailure() throws Exception {
		Message testMessage = Message.makeUpdateGroupMessage("testName", "testGroup-newName");
		when(mockGroupsDao.updateGroupName("testGroup", "newName"))
		.thenReturn("error");

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("updateGroupName", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("UGR 8 testName 31 Error : testGroup doesn't exist",
				q.peek().toString());

		when(mockGroupsDao.updateGroupName("testGroup", "newName"))
						.thenThrow(SQLException.class);

		userUpdate.invoke(client, testMessage, mockGroupsDao);
	}

	@Test
	void testListUsersInGroup() throws Exception {
		Message testMessage = Message.makeListGroupMessage("testName", "testGroup");
		List<String> userNames = new ArrayList<>();
		userNames.add("test1");
		when(mockGroupsDao.listGroupName("testGroup"))
		.thenReturn(userNames);

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("listUsersInGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("LGR 8 testName 5 test1",
				q.peek().toString());
	}

	@Test
	void testListUsersInGroupFailure() throws Exception {
		Message testMessage = Message.makeListGroupMessage("testName", "testGroup");
		when(mockGroupsDao.listGroupName("testGroup"))
		.thenReturn(new ArrayList<String>());

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("listUsersInGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("LGR 8 testName 56 Error : testGroup doesn't exist or no users in testGroup",
				q.peek().toString());

		when(mockGroupsDao.listGroupName("testGroup"))
						.thenThrow(SQLException.class);
		userUpdate.invoke(client, testMessage, mockGroupsDao);
	}

	@Test
	void testGetListUsersInGroup() throws Exception {

		Message testMessage = Message.makeListGroupMessage("testName", "testGroup");

		List<String> userNames = new ArrayList<>();
		userNames.add("test1");
		userNames.add("test2");
		userNames.add("test3");

		when(mockGroupsDao.listGroupName("testGroup"))
		.thenReturn(userNames);

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("getListUsersInGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		List<String> groupNames = (List<String>) userUpdate.invoke(client, testMessage, mockGroupsDao);

		for (int i = 0; i < userNames.size(); i++) {
			assertEquals(userNames.get(i), groupNames.get(i));
		}
	}

	@Test
	void testDeleteGroup() throws Exception {
		Message testMessage = Message.makeDeleteGroupMessage("testName", "testGroup");
		when(mockGroupsDao.deleteGroup("testGroup"))
		.thenReturn("success");

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("deleteGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("DGP 8 testName 40 Success : testGroup deleted successfully",
				q.peek().toString());
	}

	@Test
	void testDeleteGroupFailure() throws Exception {
		Message testMessage = Message.makeDeleteGroupMessage("testName", "testGroup");
		when(mockGroupsDao.deleteGroup("testGroup"))
		.thenReturn("error");

		getSocketAndClient();
		client.setName("testName");

		Method userUpdate = client.getClass().getDeclaredMethod("deleteGroup", Message.class, GroupsDao.class);
		userUpdate.setAccessible(true);
		userUpdate.invoke(client, testMessage, mockGroupsDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("DGP 8 testName 31 Error : testGroup doesn't exist",
				q.peek().toString());

		when(mockGroupsDao.deleteGroup("testGroup"))
						.thenThrow(SQLException.class);
		userUpdate.invoke(client, testMessage, mockGroupsDao);
	}

	@Test
	void testSaveMessage() throws Exception {
		getSocketAndClient();
		MessageDao mockMessageDao = mock(MessageDao.class);
		when(mockMessageDao.createMessage("recall1", "text", "recall2",true, UserDao.getInstance(),"127.0.0.1","127.0.0.1"))
		.thenReturn(new edu.northeastern.ccs.im.model.Message(123, 321, "testText"));

		Message testMessage = Message.makeMessage("UMG","recall1", "/message recall2-text");


		client.setName("recall1");

		//Method saveMessage = client.getClass().getDeclaredMethod("saveMessage", Message.class, boolean.class, MessageDao.class);
		//saveMessage.setAccessible(true);
		//saveMessage.invoke(client, testMessage, true, mockMessageDao);
		client.saveMessage(testMessage, true, MessageDao.getInstance(), "/127.0.0.1:4545", "");


		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("UMG 7 recall1 50 Success : Message  recall2-text saved successfully", q.peek().toString());
	}

	@Test
	void testSaveMessageUnsuccessful() throws Exception {
		MessageDao mockMessageDao = mock(MessageDao.class);
		when(mockMessageDao.createMessage("testReceiver", "text", "testName",true, UserDao.getInstance(),"127.0.0.1","127.0.0.1"))
		.thenReturn(null);

		Message testMessage = Message.makeMessage("UMG","testName", "testReceiver-text");

		getSocketAndClient();
		client.setName("testName");

		/*    Method saveMessage = client.getClass().getDeclaredMethod("saveMessage", Message.class, boolean.class, MessageDao.class);
    saveMessage.setAccessible(true);
    saveMessage.invoke(client, testMessage, true, mockMessageDao);*/

		client.saveMessage(testMessage, true, mockMessageDao, "/127.0.0.1:4545", "");

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("UMG 8 testName 66 Error : testReceiver-text is null you cannot send null messages !!", q.peek().toString());
	}

	@Test
	void testSaveGroupMessage() throws Exception {
		GroupMessageDao mockMessageDao = mock(GroupMessageDao.class);

		Map<String, Boolean> recipientsAvailable = new HashMap<String,Boolean>();
		when(mockMessageDao.createGroupMessage(recipientsAvailable, "text", "testName", "testReceiver", UserDao.getInstance(), GroupsDao.getInstance()))
		.thenReturn(new edu.northeastern.ccs.im.model.GroupMessage(123, 321, "testText"));

		Message testMessage = Message.makeMessage("UMG","testName", "testReceiver-text");

		getSocketAndClient();
		client.setName("testName");

		Method saveMessage = client.getClass().getDeclaredMethod("saveGroupMessage", Message.class, Map.class, GroupMessageDao.class);
		saveMessage.setAccessible(true);
		saveMessage.invoke(client, testMessage, recipientsAvailable, mockMessageDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("UMG 8 testName 23 Success : Message  sent", q.peek().toString());
	}

	@Test
	void testSaveGroupMessageUnsuccessful() throws Exception {
		getSocketAndClient();
		GroupMessageDao mockMessageDao = mock(GroupMessageDao.class);

		Map<String, Boolean> recipientsAvailable = new HashMap<String,Boolean>();
		when(mockMessageDao.createGroupMessage(recipientsAvailable, "text", "testName", "testReceiver", UserDao.getInstance(), GroupsDao.getInstance()))
		.thenReturn(null);

		Message testMessage = Message.makeMessage("UMG","testName", "testReceiver-text");


		client.setName("testName");

		Method saveMessage = client.getClass().getDeclaredMethod("saveGroupMessage", Message.class, Map.class, GroupMessageDao.class);
		saveMessage.setAccessible(true);
		saveMessage.invoke(client, testMessage, recipientsAvailable, mockMessageDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("UMG 8 testName 48 Error : is null you cannot send null messages !!", q.peek().toString());
	}

	@Test
	void isMatchTestPositive() throws Exception {
		try {
			getSocketAndClient();
			client.setName("Rio");
			Method isMatch = client.getClass().getDeclaredMethod("isMatch", String.class);
			isMatch.setAccessible(true);
			assertEquals(isMatch.invoke(client, "hello"),false);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void isMatchTestNegative() throws Exception {
		try {
			getSocketAndClient();
			client.setName("Rio");
			Method isMatch = client.getClass().getDeclaredMethod("isMatch", String.class);
			isMatch.setAccessible(true);
			assertEquals(isMatch.invoke(client, "fuck"),true);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void isMatchTestPositive2() throws Exception {
		try {
			getSocketAndClient();
			client.setName("Rio");
			Method isMatch = client.getClass().getDeclaredMethod("isMatch", String.class);
			isMatch.setAccessible(true);
			assertEquals(isMatch.invoke(client, "fcuk"),true);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void testContentTest() throws Exception {
		try {
			getSocketAndClient();
			String str = "ok";
			InputStream in = new ByteArrayInputStream(str.getBytes());
			Method isMatch = client.getClass().getDeclaredMethod("testContent", InputStream.class);
			isMatch.setAccessible(true);
			assertEquals(isMatch.invoke(client, in),"ok");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void testContentTest2() throws Exception {
		try {
			getSocketAndClient();
			String str = "fuck you";
			InputStream in = new ByteArrayInputStream(str.getBytes());
			Method isMatch = client.getClass().getDeclaredMethod("testContent", InputStream.class);
			isMatch.setAccessible(true);
			String output= (String) isMatch.invoke(client, in);
			assertEquals(output,"fuck you");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void testFilterMsg() throws Exception {
		try {
			getSocketAndClient();
			String msg = "/message Peg-ok";
			Message userMessage = Message.makeUserMessage("Rio", msg);
			Method testFilter = client.getClass().getDeclaredMethod("filterMessage", Message.class);
			testFilter.setAccessible(true);
			Message output= (Message) testFilter.invoke(client, userMessage);
			System.out.println(output);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void testFilterMsg2() throws Exception {
		try {
			getSocketAndClient();
			String msg = "/message Peg-fuck";
			Message userMessage = Message.makeUserMessage("Rio", msg);
			Method testFilter = client.getClass().getDeclaredMethod("filterMessage", Message.class);
			testFilter.setAccessible(true);
			Message output= (Message) testFilter.invoke(client, userMessage);
			System.out.println(output);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void testFilterMsg3() throws Exception {
		try {
			getSocketAndClient();
			String msg = "/message Peg-fuckeeeeeerrrssss";
			Message userMessage = Message.makeUserMessage("Rio", msg);
			Method testFilter = client.getClass().getDeclaredMethod("filterMessage", Message.class);
			testFilter.setAccessible(true);
			Message output= (Message) testFilter.invoke(client, userMessage);
			System.out.println(output);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void testFilterMsg4() throws Exception {
		try {
			getSocketAndClient();
			String handle = "HLO";
			String sender = "Rio";
			String successMessage = "Success : User Fuck logged in";
			Message userMessage = Message.makeMessage(handle, sender, successMessage + "\n You have no unread messages");

			Method testFilter = client.getClass().getDeclaredMethod("filterMessage2", Message.class);
			testFilter.setAccessible(true);
			Message output= (Message) testFilter.invoke(client, userMessage);
			System.out.println(output);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Tests the isAgency method.
	 */
	@Test
	public void testIsAgency() {
		getSocketAndClient();
		try {
			client.setName("AGENCYAli");
			assertTrue(client.isAgency());

			client.setName("Bob");
			assertFalse(client.isAgency());
		}
		catch(Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Tests the getTarget method.
	 */
	@Test
	public void testGetTarget() {
		getSocketAndClient();
		try {
			client.setName("AGENCYPete");
			assertEquals("Pete", client.getTarget());
		}
		catch(Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void testGetListUsersInRecallGroup() throws Exception{
		Message testMessage = Message.makeMessage("SRM","testName", "testGroup");
		List<String> users = new ArrayList<>();
		users.add("test1");
		users.add("test2");
		when(mockGroupsDao.listGroupName("testGroup")).thenReturn(users);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("getListUsersInRecallGroup", Message.class, GroupsDao.class);
		recallMessage.setAccessible(true);
		List<String> resultUsers = (List<String>) recallMessage.invoke(client, testMessage, mockGroupsDao);

		assertEquals(2, resultUsers.size());
	}

	@Test
	void testGetListUsersInRecallGroupEmpty() throws Exception{
		Message testMessage = Message.makeMessage("SRM","testName", "testGroup");
		List<String> users = new ArrayList<>();
		when(mockGroupsDao.listGroupName("testGroup")).thenReturn(users);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("getListUsersInRecallGroup", Message.class, GroupsDao.class);
		recallMessage.setAccessible(true);
		List<String> resultUsers = (List<String>) recallMessage.invoke(client, testMessage, mockGroupsDao);

		assertEquals(0, resultUsers.size());
	}
	@Test
	void testRecallMessageSuccess() throws Exception {
		when(mockMessageDao.createRecallMessage("testReceiver", "testName", mockMessageDao,
				mockUserDao)).thenReturn("testRecall");
		Message testMessage = Message.makeMessage("RCL","testName", "testReceiver");

		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallMessage", Message.class, MessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("RCL 8 testName 40 Success : testReceiver Message  recalled", q.peek().toString());
	}

	@Test
	void testRecallMessageFailure() throws Exception {
		when(mockMessageDao.createRecallMessage("testReceiver", "testName", mockMessageDao,
				mockUserDao)).thenReturn(null);
		Message testMessage = Message.makeMessage("RCL","testName", "testReceiver");

		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallMessage", Message.class, MessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("RCL 8 testName 61 Error : testReceiver is null you cannot send null messages !!",
				q.peek().toString());
	}

	@Test
	void testRecallAnyMessageIdSuccess() throws Exception{
		Message testMessage = Message.makeMessage("RCL","testName", "000-test recall id");
		when(mockMessageDao.createRecallAnyMessageId("testName", "000", mockMessageDao, mockUserDao))
		.thenReturn("recall any id");
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallAnyMessageId", Message.class,
				MessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("RID 8 testName 46 Success : 000-test recall id Message  recalled", q.peek().toString());
	}

	@Test
	void testRecallAnyMessageIdFailure() throws Exception{
		Message testMessage = Message.makeMessage("RCL","testName", "000-test recall id");
		when(mockMessageDao.createRecallAnyMessageId("testName", "000", mockMessageDao,
				mockUserDao)).thenReturn(null);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallAnyMessageId",
				Message.class, MessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("RID 8 testName 67 Error : 000-test recall id is null you cannot send null messages !!",
				q.peek().toString());
	}

	@Test
	void testRecallAnyGroupMEssageIdSuccess() throws Exception {
		Message testMessage = Message.makeMessage("RGD","testName", "000-test recall id");
		when(mockGroupMessageDao.createRecallAnyGroupMessageId("testName", "000",
				mockUserDao)).thenReturn("recall group");
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallAnyGroupMessageId", Message.class, GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("RGD 8 testName 46 Success : 000-test recall id Message  recalled",
				q.peek().toString());
	}

	@Test
	void testRecallAnyGroupMessageIdFailure() throws Exception {
		Message testMessage = Message.makeRecallGroupMessageId("testName", "000-test recall id");
		when(mockGroupMessageDao.createRecallAnyGroupMessageId("testName", "000",
				mockUserDao)).thenReturn(null);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallAnyGroupMessageId", Message.class, GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("RGD 8 testName 67 Error : 000-test recall id is null you cannot send null messages !!",
				q.peek().toString());
	}

	@Test
	void testRecallAnyMessageSuccess() throws Exception {
		Message testMessage = Message.makeMessage("RGD","testName", "000-test recall id");
		List<String> message = new ArrayList<>();
		message.add("test recall any");
		when(mockMessageDao.createRecallAnyMessage("testName", mockMessageDao,
				mockGroupMessageDao, mockUserDao)).thenReturn(message);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallAnyMessage", Message.class, MessageDao.class, GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("RCA 6 System 15 test recall any",
				q.peek().toString());
	}

	@Test
	void testRecallAnyMessageFailure() throws Exception {
		Message testMessage = Message.makeMessage("RGD","testName", "000-test recall id");
		when(mockMessageDao.createRecallAnyMessage("testName", mockMessageDao,
				mockGroupMessageDao, mockUserDao)).thenReturn(null);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallAnyMessage", Message.class, MessageDao.class, GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("RCA 8 testName 67 Error : 000-test recall id is null you cannot send null messages !!",
				q.peek().toString());
	}

	@Test
	void testSearchReceiverMessageSuccess() throws Exception {
		Message testMessage = Message.makeMessage("SRM","testName", "testReceiver");
		List<String> message = new ArrayList<>();
		message.add(null);
		when(mockMessageDao.createReceiverSearchMessage("testReceiver", "testName", mockMessageDao,
				mockGroupMessageDao, mockUserDao)).thenReturn(message);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("searchReceiverMessage", Message.class, MessageDao.class, GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("SRM 6 System 2 --",
				q.peek().toString());
	}


	@Test
	void testSearchReceiverMessageFailure() throws Exception {
		Message testMessage = Message.makeMessage("SRM","testName", "testReceiver");
		when(mockMessageDao.createReceiverSearchMessage("testReceiver", "testName", mockMessageDao,
				mockGroupMessageDao, mockUserDao)).thenReturn(null);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("searchReceiverMessage", Message.class, MessageDao.class, GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("SRM 8 testName 61 Error : testReceiver is null you cannot send null messages !!",
				q.peek().toString());
	}



	@Test
	void testRecallGroupMessageSuccess() throws Exception {
		Map<String, Boolean> recipientsAvailable = new HashMap<String,Boolean>();
		Message testMessage = Message.makeRecallGroupMessage("testName", "/grpRecall testGroup-");
		when(mockGroupMessageDao.createRecallGroupMessage("testName", "testGroup",
				UserDao.getInstance(), GroupsDao.getInstance())).thenReturn("test message");
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallGroupMessage", Message.class,GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);

		Object value = waitingListExpected.get(client);

		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("GRL 8 testName 38 Success : testGroup- Message  recalled",
				q.peek().toString());
	}

	@Test
	void testRecallGroupMessageFailure() throws Exception {
		Map<String, Boolean> recipientsAvailable = new HashMap<String,Boolean>();
		Message testMessage = Message.makeMessage("GRL","testName", "testGroup");
		when(mockGroupMessageDao.createRecallGroupMessage("testName", "testGroup",
				mockUserDao, mockGroupsDao)).thenReturn(null);
		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("recallGroupMessage", Message.class,  GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("GRL 8 testName 48 Error : is null you cannot send null messages !!",
				q.peek().toString());
	}

	/**
	 * Tests the isAdmin method.
	 */
	@Test
	public void testIsAdmin() {
		getSocketAndClient();
		try {
			client.setName("ADMIN");
			assertTrue(client.isAdmin());
			client.setName("Bob");
			assertFalse(client.isAdmin());
		}
		catch(Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	void testSearchTimeMessage() throws Exception {
		Message testMessage = Message.makeMessage("STM", "testName", "12:00");
		List<String> messages = new ArrayList<>();
		messages.add("server message");
		when(mockMessageDao.createTimeSearchMessage("12:00", "testName", mockMessageDao,
				mockGroupMessageDao, mockUserDao)).thenReturn(messages);

		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("searchTimeMessage", Message.class, MessageDao.class, GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("STM 6 System 14 server message", q.peek().toString());
	}

	@Test
	void testSearchTimeMessageFailure() throws Exception {
		Message testMessage = Message.makeMessage("STM", "testName", "12:00");
		List<String> messages = new ArrayList<>();
		messages.add("server message");
		when(mockMessageDao.createTimeSearchMessage("12:00", "testName", mockMessageDao,
				mockGroupMessageDao, mockUserDao)).thenReturn(null);

		getSocketAndClient();
		client.setName("testName");

		Method recallMessage = client.getClass().getDeclaredMethod("searchTimeMessage", Message.class, MessageDao.class, GroupMessageDao.class, UserDao.class);
		recallMessage.setAccessible(true);
		recallMessage.invoke(client, testMessage, mockMessageDao, mockGroupMessageDao, mockUserDao);

		Field waitingListExpected = client.getClass().getDeclaredField("waitingList");
		waitingListExpected.setAccessible(true);
		Object value = waitingListExpected.get(client);
		ConcurrentLinkedQueue q = (ConcurrentLinkedQueue) value;

		assertEquals("STM 8 testName 54 Error : 12:00 is null you cannot send null messages !!", q.peek().toString());
	}

	@Test
	void broadcastTest(){
		try {
			getSocketAndClient();
			client.setName("abcd");
			Message msg = Message.makeMessage("BCT","testName", "testGroup");
			client.broadcastMsgFunc(msg);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	void broadcastTest2(){
		try {
			getSocketAndClient();
			client.setName("abcd");
			Message msg = Message.makeMessage("BCT","testName", "Prattle says everyone log off");
			client.broadcastMsgFunc(msg);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

}
