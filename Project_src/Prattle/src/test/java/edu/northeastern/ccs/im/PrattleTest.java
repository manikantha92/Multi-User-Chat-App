package edu.northeastern.ccs.im;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.time.Duration;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.ScanNetNB;
import edu.northeastern.ccs.im.SocketNB;
import edu.northeastern.ccs.im.server.ClientRunnable;
import edu.northeastern.ccs.im.server.Prattle;

import java.util.List;
import java.util.ArrayList;

import javax.sound.midi.SysexMessage;


public class PrattleTest {

	private static final Logger LOGGER = Logger.getLogger(
					Thread.currentThread().getStackTrace()[0].getClassName() );
	private static final int PORT = 4576;
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

	/**
	 * Test broadcastMessage Method.
	 */
	@Test
	public void broadcastMessageTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Tommy";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");
			String msgTobeBroadCasted = "hii to all";
			Message testMsg = Message.makeBroadcastMessage(username,msgTobeBroadCasted);


			//Access SocketNB channel variable
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			//Access input variable of ClientRunnable
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);


			//Access input variable of ClientRunnable
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);

			//Access messages variable of ScanNetNB
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);

			String brdMsg = "This is new message";
			Message broadCastMsg = Message.makeBroadcastMessage("Tommy",brdMsg);
			msgParam.add(broadCastMsg);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.broadcastMessage(testMsg);
			assertFalse(waitingList.peek().getText().equals(msgTobeBroadCasted));
			active.clear();
			inputParam.close();
			soc.close();

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Tests test broadcastMessage method with Agency.
	 */
	@Test
	public void testBroadcastMessageWithAgency() {
		try {				// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Kevin";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");

			String msgBroadcast = "Hey ALL";
			Message testMsg = Message.makeBroadcastMessage(username,msgBroadcast);
			//Access SocketNB channel variable for 1st user
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Chris";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "Sam";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"abcd");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			// add 4th active user
			SocketNB socket4 = new SocketNB("localhost", PORT);
			String username4 = "AGENCYChris";
			Message loginMessage4 = Message.makeSimpleLoginMessage(username4,"AGENCY");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket4 = socket4.getClass().getDeclaredField("channel");
			privateSocket4.setAccessible(true);
			SocketChannel soc4 = (SocketChannel) privateSocket4.get(socket4);

			ClientRunnable tt4 = new ClientRunnable(soc4);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);


			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);


			//Access input variable of ClientRunnable 4
			Field input4 = tt4.getClass().getDeclaredField("input");
			input4.setAccessible(true);
			ScanNetNB inputParam4 = (ScanNetNB)input4.get(tt4);

			//Access input variable of ClientRunnable 4
			Field privateWaitingList4 = tt4.getClass().getDeclaredField("waitingList");
			privateWaitingList4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList4 = (Queue<Message>)privateWaitingList.get(tt4);

			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);


			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 3rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			//Access messages variable of ScanNetNB for 4th user
			Field messages4 = inputParam4.getClass().getDeclaredField("messages");
			messages4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam4 = (Queue<Message>)messages4.get(inputParam4);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);
			msgParam4.add(loginMessage4);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			Method privatecheckForInitializaton4 = tt4.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton4.setAccessible(true);
			privatecheckForInitializaton4.invoke(tt4, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);
			Prattle.setActiveConnection(tt4);

			Prattle.broadcastMessage(testMsg);

			// All will receive message, even the Agency who's target is sending message
			Assertions.assertEquals(msgBroadcast + "\n", waitingList.peek().getText());
			Assertions.assertEquals(msgBroadcast + "\n", waitingList2.peek().getText());
			Assertions.assertEquals(msgBroadcast + "\n", waitingList3.peek().getText());
			Assertions.assertEquals(msgBroadcast + "\n", waitingList4.peek().getText());

			active.clear();
			inputParam.close();
			soc.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Tests testSendOutUserMessage method with Agency and its target as sender.
	 */
	@Test
	public void testSendOutUserMessage() {
		try {				// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Kevin";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");

			String msgToUser = "Chris-Hi Crhis";
			Message testMsg = Message.makeUserMessage(username,msgToUser);
			//Access SocketNB channel variable for 1st user
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Chris";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "George";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"abcd");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			// add 4th active user
			SocketNB socket4 = new SocketNB("localhost", PORT);
			String username4 = "AGENCYKevin";
			Message loginMessage4 = Message.makeSimpleLoginMessage(username4,"AGENCY");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket4 = socket4.getClass().getDeclaredField("channel");
			privateSocket4.setAccessible(true);
			SocketChannel soc4 = (SocketChannel) privateSocket4.get(socket4);

			ClientRunnable tt4 = new ClientRunnable(soc4);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);


			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);


			//Access input variable of ClientRunnable 4
			Field input4 = tt4.getClass().getDeclaredField("input");
			input4.setAccessible(true);
			ScanNetNB inputParam4 = (ScanNetNB)input4.get(tt4);

			//Access input variable of ClientRunnable 4
			Field privateWaitingList4 = tt4.getClass().getDeclaredField("waitingList");
			privateWaitingList4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList4 = (Queue<Message>)privateWaitingList.get(tt4);

			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);


			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 3rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			//Access messages variable of ScanNetNB for 4th user
			Field messages4 = inputParam4.getClass().getDeclaredField("messages");
			messages4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam4 = (Queue<Message>)messages4.get(inputParam4);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);
			msgParam4.add(loginMessage4);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			Method privatecheckForInitializaton4 = tt4.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton4.setAccessible(true);
			privatecheckForInitializaton4.invoke(tt4, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);
			Prattle.setActiveConnection(tt4);

			boolean b = Prattle.sendOutUserMessage(testMsg);

			// boolean b is false because last active user in list is not Chris, the message receiver.
			assertFalse(b);

			// Kevin will not get any messages because she is the one sending out the message.
			assertTrue(waitingList.isEmpty());
			// Chris will get a message because the message receiver is her.
			Assertions.assertEquals(msgToUser, waitingList2.peek().getText());
			// George will not get the message because the message receiver is not Bob.
			assertTrue(waitingList3.isEmpty());
			// Agency will get the message because the sender is its target
			Assertions.assertEquals(msgToUser, waitingList4.peek().getText());

			active.clear();
			inputParam.close();
			soc.close();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE,"Exception Occured", e);
			}
		}

	/**
	 * Tests testSendOutUserMessage method with Agency and target as receiver.
	 */
	@Test
	public void testSendOutUserMessageAgencyTargetAsReceiver() {
		try {				// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Kevin";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");

			String msgToUser = "Chris-Hi Crhis";
			Message testMsg = Message.makeUserMessage(username,msgToUser);
			//Access SocketNB channel variable for 1st user
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Chris";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "George";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"abcd");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			// add 4th active user
			SocketNB socket4 = new SocketNB("localhost", PORT);
			String username4 = "AGENCYChris";
			Message loginMessage4 = Message.makeSimpleLoginMessage(username4,"AGENCY");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket4 = socket4.getClass().getDeclaredField("channel");
			privateSocket4.setAccessible(true);
			SocketChannel soc4 = (SocketChannel) privateSocket4.get(socket4);

			ClientRunnable tt4 = new ClientRunnable(soc4);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);


			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);

			//Access input variable of ClientRunnable 4
			Field input4 = tt4.getClass().getDeclaredField("input");
			input4.setAccessible(true);
			ScanNetNB inputParam4 = (ScanNetNB)input4.get(tt4);

			//Access input variable of ClientRunnable 4
			Field privateWaitingList4 = tt4.getClass().getDeclaredField("waitingList");
			privateWaitingList4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList4 = (Queue<Message>)privateWaitingList.get(tt4);

			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);


			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 3rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			//Access messages variable of ScanNetNB for 4th user
			Field messages4 = inputParam4.getClass().getDeclaredField("messages");
			messages4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam4 = (Queue<Message>)messages4.get(inputParam4);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);
			msgParam4.add(loginMessage4);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			Method privatecheckForInitializaton4 = tt4.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton4.setAccessible(true);
			privatecheckForInitializaton4.invoke(tt4, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);
			Prattle.setActiveConnection(tt4);

			boolean b = Prattle.sendOutUserMessage(testMsg);


			// boolean b is false because last active user in list is not Chris, the message receiver.
			assertFalse(b);
			// Kevin will not get any messages because she is the one sending out the message.
			assertTrue(waitingList.isEmpty());
			// Chris will get a message because the message receiver is her.
			Assertions.assertEquals(msgToUser, waitingList2.peek().getText());
			// George will not get the message because the message receiver is not Bob.
			assertTrue(waitingList3.isEmpty());
			// Agency will get the message because the sender is its target
			Assertions.assertEquals(msgToUser, waitingList4.peek().getText());

			active.clear();
			inputParam.close();
			soc.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Tests testSendOutGroupMessage method.
	 */
	@Test
	public void testSendOutGroupMessage() {
		try {
			// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Chelsea";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");

			String msgForGroup = "Team1-hii to all";
			Message testMsg = Message.makeGroupMessage(username,msgForGroup);

			//Access SocketNB channel variable for 1st user
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Deb";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "AGENCYDeb";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"AGENCY");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			// add 4th active user
			SocketNB socket4 = new SocketNB("localhost", PORT);
			String username4 = "Bob";
			Message loginMessage4 = Message.makeSimpleLoginMessage(username4,"Bob");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket4 = socket4.getClass().getDeclaredField("channel");
			privateSocket4.setAccessible(true);
			SocketChannel soc4 = (SocketChannel) privateSocket4.get(socket4);

			ClientRunnable tt4 = new ClientRunnable(soc4);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);


			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);

			//Access input variable of ClientRunnable 4
			Field input4 = tt4.getClass().getDeclaredField("input");
			input4.setAccessible(true);
			ScanNetNB inputParam4 = (ScanNetNB)input4.get(tt4);

			//Access input variable of ClientRunnable 4
			Field privateWaitingList4 = tt4.getClass().getDeclaredField("waitingList");
			privateWaitingList4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList4 = (Queue<Message>)privateWaitingList.get(tt4);


			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);


			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 3rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			//Access messages variable of ScanNetNB for 4th user
			Field messages4 = inputParam4.getClass().getDeclaredField("messages");
			messages4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam4 = (Queue<Message>)messages4.get(inputParam4);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);
			msgParam4.add(loginMessage4);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			Method privatecheckForInitializaton4 = tt4.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton4.setAccessible(true);
			privatecheckForInitializaton4.invoke(tt4, null);

			// members who are in the group
			List<String> groupMembers = new ArrayList<>();
			groupMembers.add("Deb");
			groupMembers.add("Bob");

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);
			Prattle.setActiveConnection(tt4);
			Prattle.sendOutGroupMessage(testMsg, groupMembers);

			// Chelsea should not get any messages because she is not in the group
			assertTrue(waitingList.isEmpty());
			// Deb will get a message because she is in the group
			Assertions.assertEquals(msgForGroup, waitingList2.peek().getText());
			// Agency will get a message because its target will receive the message
			Assertions.assertEquals(msgForGroup, waitingList3.peek().getText());
			// Bob will get a message because he is in the group
			Assertions.assertEquals(msgForGroup, waitingList4.peek().getText());

			active.clear();
			inputParam.close();
			soc.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Tests testSendOutGroupMessage method with Agency and its target as receiver.
	 */
	@Test
	public void testSendOutGroupMessageAgencyTargetAsReceiver() {
		try {
			// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Chelsea";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");

			//Access SocketNB channel variable for 1st user
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Deb";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "AGENCYTeam1";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"AGENCY");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			// add 4th active user
			SocketNB socket4 = new SocketNB("localhost", PORT);
			String username4 = "Bob";
			Message loginMessage4 = Message.makeSimpleLoginMessage(username4,"Bob");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket4 = socket4.getClass().getDeclaredField("channel");
			privateSocket4.setAccessible(true);
			SocketChannel soc4 = (SocketChannel) privateSocket4.get(socket4);

			ClientRunnable tt4 = new ClientRunnable(soc4);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);

			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);

			//Access input variable of ClientRunnable 4
			Field input4 = tt4.getClass().getDeclaredField("input");
			input4.setAccessible(true);
			ScanNetNB inputParam4 = (ScanNetNB)input4.get(tt4);

			//Access input variable of ClientRunnable 4
			Field privateWaitingList4 = tt4.getClass().getDeclaredField("waitingList");
			privateWaitingList4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList4 = (Queue<Message>)privateWaitingList.get(tt4);

			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);

			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 3rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			//Access messages variable of ScanNetNB for 4th user
			Field messages4 = inputParam4.getClass().getDeclaredField("messages");
			messages4.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam4 = (Queue<Message>)messages4.get(inputParam4);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);
			msgParam4.add(loginMessage4);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			Method privatecheckForInitializaton4 = tt4.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton4.setAccessible(true);
			privatecheckForInitializaton4.invoke(tt4, null);

			String msgForGroup = "Team1-hii to all";
			Message testMsg = Message.makeGroupMessage("Deb",msgForGroup);

			// members who are in the group
			List<String> groupMembers = new ArrayList<>();
			groupMembers.add("Deb");
			groupMembers.add("Bob");

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);
			Prattle.setActiveConnection(tt4);
			Prattle.sendOutGroupMessage(testMsg, groupMembers);

			// Chelsea should not get any messages because she is not in the group
			assertTrue(waitingList.isEmpty());
			// Deb will get a message because she is in the group
			Assertions.assertEquals(msgForGroup, waitingList2.peek().getText());
			// Agency will get a message because its target will receive the message
			Assertions.assertEquals(msgForGroup, waitingList3.peek().getText());
			// Bob will get a message because he is in the group
			Assertions.assertEquals(msgForGroup, waitingList4.peek().getText());

			active.clear();
			inputParam.close();
			soc.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Test method removeClientTest.
	 */
	@Test
	public void removeClientTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Tommy";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");
			String msgTobeBroadCasted = "hii to all";
			Message testMsg = Message.makeBroadcastMessage(username,msgTobeBroadCasted);


			//Access SocketNB channel variable
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable client = new ClientRunnable(soc);

			//Access input variable of ClientRunnable
			Field input = client.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(client);


			//Access input variable of ClientRunnable
			Field privateWaitingList = client.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(client);

			//Access messages variable of ScanNetNB
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);

			String brdMsg = "This is new message";
			Message broadCastMsg = Message.makeBroadcastMessage("Tommy",brdMsg);
			msgParam.add(broadCastMsg);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = client.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(client, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(client);
			Prattle.removeClient(client);
			assertTrue(active.isEmpty());

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void removeClientNotInListTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Tommy";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");
			String msgTobeBroadCasted = "hii to all";
			Message testMsg = Message.makeBroadcastMessage(username,msgTobeBroadCasted);

			//Access SocketNB channel variable
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable client = new ClientRunnable(soc);

			Prattle.removeClient(client);
			assertTrue(active.isEmpty());

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	/**
	 * Tests getAgencyMap method.
	 */
	@Test
	public void testGetAgencyMap() {
		try {				// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "AGENCYAli";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"AGENCY");

			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Ali";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "Peg";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"abcd");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);


			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);

			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);


			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 2rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);


			HashMap<String, String> agencyMap = Prattle.getAgencyMap();

			HashMap<String, String> expected = new HashMap<>();
			expected.put("AGENCYAli", "Ali");

			for (String agencyName: agencyMap.keySet()) {
				assertTrue(expected.containsKey(agencyName));
				assertEquals(expected.get(agencyName), agencyMap.get(agencyName));
			}


			active.clear();
			inputParam.close();
			soc.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	/**
	 * Tests userHasAgency method.
	 */
	@Test
	public void testUserHasAgency() {
		try {				// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "AGENCYAli";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"AGENCY");

			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Ali";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "Peg";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"abcd");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);


			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);

			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);


			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 2rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);

			// client 2 (tt2) is user Ali, who has an Agency
			assertTrue(Prattle.userHasAgency(tt2.getName()));
			// client 3 (tt3) is user Peg, does not have an Agency
			assertFalse(Prattle.userHasAgency(tt3.getName()));

			active.clear();
			inputParam.close();
			soc.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	/**
	 * Tests senderHasAgency method.
	 */
	@Test
	public void testSenderHasAgency() {
		try {				// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "AGENCYAli";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"AGENCY");

			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Ali";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "Peg";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"abcd");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);


			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);

			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);


			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 2rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);

			Message msg = Message.makeUserMessage("Ali", "Peg-Hi Peg");
			assertTrue(Prattle.senderHasAgency(msg));
			assertFalse(Prattle.receiverHasAgency(msg));

			Message msg2 = Message.makeUserMessage("Peg", "Ali-Hi Ali");
			assertFalse(Prattle.senderHasAgency(msg2));
			assertTrue(Prattle.receiverHasAgency(msg2));

			active.clear();
			inputParam.close();
			soc.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	/**
	 * Tests test sendToAgencyGivenTargetName method.
	 */
	@Test
	public void testSendToAgencyGivenTargetName() {
		try {				// add 1st active user
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "AGENCYAli";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"AGENCY");

			//Access SocketNB channel variable for 1st user
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			ClientRunnable tt = new ClientRunnable(soc);

			// add 2nd active user
			SocketNB socket2 = new SocketNB("localhost", PORT);
			String username2 = "Ali";
			Message loginMessage2 = Message.makeSimpleLoginMessage(username2,"abcd");

			//Access SocketNB channel variable for 2nd user
			Field privateSocket2 = socket2.getClass().getDeclaredField("channel");
			privateSocket2.setAccessible(true);
			SocketChannel soc2 = (SocketChannel) privateSocket2.get(socket2);

			ClientRunnable tt2 = new ClientRunnable(soc2);

			// add 3rd active user
			SocketNB socket3 = new SocketNB("localhost", PORT);
			String username3 = "AgencyBrad";
			Message loginMessage3 = Message.makeSimpleLoginMessage(username3,"AGENCY");

			//Access SocketNB channel variable for 3rd user
			Field privateSocket3 = socket3.getClass().getDeclaredField("channel");
			privateSocket3.setAccessible(true);
			SocketChannel soc3 = (SocketChannel) privateSocket3.get(socket3);

			ClientRunnable tt3 = new ClientRunnable(soc3);

			//Access input variable of ClientRunnable for 1st user
			Field input = tt.getClass().getDeclaredField("input");
			input.setAccessible(true);
			ScanNetNB inputParam = (ScanNetNB)input.get(tt);

			//Access input variable of ClientRunnable for 1st user
			Field privateWaitingList = tt.getClass().getDeclaredField("waitingList");
			privateWaitingList.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList = (Queue<Message>)privateWaitingList.get(tt);


			//Access input variable of ClientRunnable 2
			Field input2 = tt2.getClass().getDeclaredField("input");
			input2.setAccessible(true);
			ScanNetNB inputParam2 = (ScanNetNB)input2.get(tt2);

			//Access input variable of ClientRunnable 2
			Field privateWaitingList2 = tt2.getClass().getDeclaredField("waitingList");
			privateWaitingList2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList2 = (Queue<Message>)privateWaitingList.get(tt2);


			//Access input variable of ClientRunnable 3
			Field input3 = tt3.getClass().getDeclaredField("input");
			input3.setAccessible(true);
			ScanNetNB inputParam3 = (ScanNetNB)input3.get(tt3);

			//Access input variable of ClientRunnable 3
			Field privateWaitingList3 = tt3.getClass().getDeclaredField("waitingList");
			privateWaitingList3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> waitingList3 = (Queue<Message>)privateWaitingList.get(tt3);

			//Access messages variable of ScanNetNB for 1st user
			Field messages = inputParam.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(inputParam);


			//Access messages variable of ScanNetNB for 2nd user
			Field messages2 = inputParam2.getClass().getDeclaredField("messages");
			messages2.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam2 = (Queue<Message>)messages2.get(inputParam2);

			//Access messages variable of ScanNetNB for 2rd user
			Field messages3 = inputParam3.getClass().getDeclaredField("messages");
			messages3.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam3 = (Queue<Message>)messages3.get(inputParam3);

			msgParam.add(loginMessage);
			msgParam2.add(loginMessage2);
			msgParam3.add(loginMessage3);

			//Access checkForInitialization function of ClientRunnable
			Method privatecheckForInitialization = tt.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitialization.setAccessible(true);
			privatecheckForInitialization.invoke(tt, null);

			Method privatecheckForInitializaton2 = tt2.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton2.setAccessible(true);
			privatecheckForInitializaton2.invoke(tt2, null);

			Method privatecheckForInitializaton3 = tt3.getClass().getDeclaredMethod("checkForInitialization", null);
			privatecheckForInitializaton3.setAccessible(true);
			privatecheckForInitializaton3.invoke(tt3, null);

			active = Prattle.getActiveConnection();
			Prattle.setActiveConnection(tt);
			Prattle.setActiveConnection(tt2);
			Prattle.setActiveConnection(tt3);

			Message msg = Message.makeUserMessage("Ali", "Peg-Hi Peg");
			Prattle.sentToAgencyGivenTargetName(msg, "Ali");

			// Agency 1 can see message, since its target is the sender
			assertEquals("Peg-Hi Peg", waitingList.peek().getText());
			//Agency 2, cannot see message because its target is neither the receiver or sender
			assertTrue(waitingList3.isEmpty());

			active.clear();
			inputParam.close();
			soc.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


}