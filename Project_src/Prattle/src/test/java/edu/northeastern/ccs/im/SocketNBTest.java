package edu.northeastern.ccs.im;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.time.Duration;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
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
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import edu.northeastern.ccs.im.Message;
import edu.northeastern.ccs.im.NextDoesNotExistException;
import edu.northeastern.ccs.im.PrintNetNB;
import edu.northeastern.ccs.im.ScanNetNB;
import edu.northeastern.ccs.im.SocketNB;
import edu.northeastern.ccs.im.server.ClientRunnable;
import edu.northeastern.ccs.im.server.Prattle;
import edu.northeastern.ccs.im.server.ServerConstants;

public class SocketNBTest {

	private static final Logger LOGGER = Logger.getLogger(
					Thread.currentThread().getStackTrace()[0].getClassName() );
	private static final int PORT = 4579;

	static ServerSocketChannel serverSocket;
	static Selector selector;
	static ScheduledExecutorService threadPool;

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
		server2.close();
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


	//SocketNB
	/****************************************************/

	@Test
	public void SocketNBConstructorTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);

			//Access SocketNB channel variable
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel soc = (SocketChannel) privateSocket.get(socket);

			assertTrue(privateSocket != null);
			socket.close();

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void SocketNBConstructorErrorTest() {
		try {
			Assertions.assertThrows(IOException.class, () -> {
				SocketNB socket = new SocketNB("localhost", 4554);
			});

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	public void SocketNBgetSocketTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);

			//Access SocketNB channel variable
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel channel = (SocketChannel) privateSocket.get(socket);


			//Access checkForInitialization function of ClientRunnable
			Method getSocketChannel = socket.getClass().getDeclaredMethod("getSocket", null);
			getSocketChannel.setAccessible(true);
			SocketChannel returnedSocket = (SocketChannel) getSocketChannel.invoke(socket, null);

			assertTrue(returnedSocket == channel);
			socket.close();

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	public void SocketNBcloseTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);

			//Access SocketNB channel variable
			Field privateSocket = socket.getClass().getDeclaredField("channel");
			privateSocket.setAccessible(true);
			SocketChannel channel = (SocketChannel) privateSocket.get(socket);
			assertTrue(channel.isOpen() == true);
			socket.close();
			assertTrue(channel.isOpen() == false);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

}