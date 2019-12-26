package edu.northeastern.ccs.im;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.time.Duration;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

import edu.northeastern.ccs.im.dao.GroupMessageDao;
import edu.northeastern.ccs.im.dao.MessageDao;
import edu.northeastern.ccs.im.dao.UserDao;
import edu.northeastern.ccs.im.model.User;
import edu.northeastern.ccs.im.server.ClientRunnable;
import edu.northeastern.ccs.im.server.Prattle;

public class ScanNetNBTest {

	private static final Logger LOGGER = Logger.getLogger(
					Thread.currentThread().getStackTrace()[0].getClassName() );
	private static final int PORT = 4582;

	private static ConcurrentLinkedQueue<ClientRunnable> active;
	static {
		active = new ConcurrentLinkedQueue<ClientRunnable>();
	}
	static ServerSocketChannel serverSocket;
	static Selector selector;
	static ScheduledExecutorService threadPool;

	@Mock
	private UserDao mockUserDao;

	@BeforeEach
	void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@BeforeAll
	public static void setUp()
	{

		RunnableDemo server = new RunnableDemo();
		server.run();
	}

	@Test
	public void LoginTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Demo1000";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");
			ScanNetNB scan = new ScanNetNB(socket);
			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			CharBuffer buffer = CharBuffer.allocate(100);
			String string = "4 1 2";
			for (int i = 0; i < string.length(); i++) {
				buffer.put(string.charAt(i));
			}
			buffer.flip();
			scan.loginRegisterFunc("HLO","Demo1000","Ednn2/1/","",buffer);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void LoginTest2() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			ScanNetNB scan = new ScanNetNB(socket);
			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			CharBuffer buffer = CharBuffer.allocate(100);
			String string = "4 1 2";
			for (int i = 0; i < string.length(); i++) {
				buffer.put(string.charAt(i));
			}
			buffer.flip();
			scan.loginRegisterFunc("HLO","Demo2501","Ednn344","",buffer);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void Register1() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			ScanNetNB scan = new ScanNetNB(socket);
			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			CharBuffer buffer = CharBuffer.allocate(100);
			String string = "4 1 1";
			for (int i = 0; i < string.length(); i++) {
				buffer.put(string.charAt(i));
			}
			buffer.flip();
			scan.loginRegisterFunc("HLO","Demo300","Ednn4/1","",buffer);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void Register2() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			ScanNetNB scan = new ScanNetNB(socket);
			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			CharBuffer buffer = CharBuffer.allocate(100);
			String string = "4 1 1";
			for (int i = 0; i < string.length(); i++) {
				buffer.put(string.charAt(i));
			}
			buffer.flip();
			scan.loginRegisterFunc("HLO","Demo306","Ednn4/1","",buffer);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	public void loginRegisterFuncTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			ScanNetNB scan = new ScanNetNB(socket);
			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			CharBuffer buffer = CharBuffer.allocate(100);
			String string = "4 1 5";
			for (int i = 0; i < string.length(); i++) {
				buffer.put(string.charAt(i));
			}
			buffer.flip();
			scan.loginRegisterFunc("NAK","Demo302","Ednn4/1","",buffer);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	public void ScanNetNBConstructorTest() {
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

			ScanNetNB scan = new ScanNetNB(soc);

			//Access SocketNB channel variable
			Field privateChannel = scan.getClass().getDeclaredField("channel");
			privateChannel.setAccessible(true);
			SocketChannel ScanNetNBChannel = (SocketChannel) privateChannel.get(scan);

			assertTrue(ScanNetNBChannel != null);
			soc.close();

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void ScanNetNBAltConstructorTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Tommy";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");


			ScanNetNB scan = new ScanNetNB(socket);

			//Access SocketNB channel variable
			Field privateChannel = scan.getClass().getDeclaredField("channel");
			privateChannel.setAccessible(true);
			SocketChannel ScanNetNBChannel = (SocketChannel) privateChannel.get(scan);
			assertTrue(ScanNetNBChannel != null);
			socket.close();

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void ScanNetHasNextMessageTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Tommy";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");

			ScanNetNB scan = new ScanNetNB(socket);

			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(scan);
			assertTrue(msgParam.isEmpty() == true);
			Boolean value = scan.hasNextMessage();
			assertTrue(value == false);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	public void ScanNetHasNextMessageTest2() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Tommy";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");

			ScanNetNB scan = new ScanNetNB(socket);

			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(scan);

			String msgTobeBroadCasted = "hii to all";
			Message testMsg = Message.makeBroadcastMessage(username,msgTobeBroadCasted);

			msgParam.add(testMsg);
			assertTrue(msgParam.isEmpty() == false);

			Boolean value = scan.hasNextMessage();

			assertTrue(value == true);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void ScanNetHasNextMessageTest3() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Tommy";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");

			ScanNetNB scan = new ScanNetNB(socket);

			Charset charset = Charset.forName("ISO-8859-1");
			CharsetDecoder decoder = charset.newDecoder();
			ByteBuffer buf = ByteBuffer.allocate(1000);
			CharBuffer charBuffer = decoder.decode(buf);
			String s ="test string";


			Method file = scan.getClass().getDeclaredMethod("manageFile", CharBuffer.class,byte[].class);
			file.setAccessible(true);
			Field buff = scan.getClass().getDeclaredField("buff");
			buff.setAccessible(true);

			file.invoke(charBuffer, s.getBytes());

		} catch (Exception e) {
			System.out.println(e.getMessage());
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}



	@Test
	public void ScanNetNBReadArgumentTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			String username = "Tommy";
			Message loginMessage = Message.makeSimpleLoginMessage(username,"abcd");
			String msgTobeBroadCasted = "hii to all";
			Message testMsg = Message.makeBroadcastMessage(username,msgTobeBroadCasted);

			ScanNetNB scan = new ScanNetNB(socket);
			String str = "1New buffer string";
			CharBuffer buffer = CharBuffer.allocate(1024);
			Reader reader = new StringReader(str);
			reader.read(buffer);
			buffer.flip();
			System.out.println(buffer.toString());
			reader.close();
			Method privateReadArgument = scan.getClass().getDeclaredMethod("readArgument", CharBuffer.class);
			privateReadArgument.setAccessible(true);
			String result = (String) privateReadArgument.invoke(scan, buffer);
			assertTrue(result.equals("e"));
			socket.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}


	@Test
	public void ScanNetNextMessageTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			ScanNetNB scan = new ScanNetNB(socket);
			String username = "Tommy";
			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<Message> msgParam = (Queue<Message>)messages.get(scan);

			String msgTobeBroadCasted = "hii to all";
			Message testMsg = Message.makeBroadcastMessage(username,msgTobeBroadCasted);

			msgParam.add(testMsg);
			Message message = scan.nextMessage();
			assertTrue(message.getText().equals(testMsg.getText()));
			assertTrue(message.getName().equals(testMsg.getName()));

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void ScanNetNextMessageExceptionTest() {
		try {
			Assertions.assertThrows(NextDoesNotExistException.class, () -> {
				SocketNB socket = new SocketNB("localhost", PORT);
				ScanNetNB scan = new ScanNetNB(socket);
				String username = "Tommy";
				//Access messages variable of ScanNetNB
				Field messages = scan.getClass().getDeclaredField("messages");
				messages.setAccessible(true);
				@SuppressWarnings("unchecked")
				Queue<Message> msgParam = (Queue<Message>)messages.get(scan);
				Message message = scan.nextMessage();
			});

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void ScanNetCloseTest() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			ScanNetNB scan = new ScanNetNB(socket);

			Field selector = scan.getClass().getDeclaredField("selector");
			selector.setAccessible(true);
			@SuppressWarnings("unchecked")
			Selector sel = (Selector) selector.get(scan);
			assertTrue(sel.isOpen() == true);
			scan.close();
			assertTrue(sel.isOpen() == false);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}



  void testUserExists() {
    try {
      SocketNB socket = new SocketNB("localhost", PORT);
      ScanNetNB scan = new ScanNetNB(socket);
      String username = "testName";

      when(mockUserDao.checkUserExists("testName", "testPass")).thenReturn(true);

      Method checkUserExists = scan.getClass().getDeclaredMethod("userExists", UserDao.class, User.class);
      assertTrue((boolean)checkUserExists.invoke(scan, mockUserDao, new User("testName", "testPass")));

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,"Exception Occured", e);
    }
  }

  @Test
  void testUserDoesNotExist() {
    try {
      SocketNB socket = new SocketNB("localhost", PORT);
      ScanNetNB scan = new ScanNetNB(socket);
      String username = "testName";

      when(mockUserDao.checkUserExists("testName", "testPass")).thenReturn(false);

      Method checkUserExists = scan.getClass().getDeclaredMethod("userExists", UserDao.class, User.class);
      assertFalse((boolean)checkUserExists.invoke(scan, mockUserDao, new User("testName", "testPass")));

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,"Exception Occured", e);
    }
  }

  @Test
  void testUserRegister() {
    try {
      SocketNB socket = new SocketNB("localhost", PORT);
      ScanNetNB scan = new ScanNetNB(socket);
      String username = "testName";

      when(mockUserDao.checkUserExists(anyString(), anyString())).thenReturn(false);
      when(mockUserDao.create(any(User.class))).thenReturn(new User("",""));

      Method userRegister = scan.getClass().getDeclaredMethod("userRegister", String.class, String.class, String.class, String.class, UserDao.class);
      userRegister.invoke(scan, "testSender", "testMessage", "testServerMessage", "HLO", mockUserDao);

      Queue<Message> messages = scan.getMessages();
      assertEquals("HLO 10 testSender 50 Success : User  testSender registered successfully",messages.peek().toString());

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,"Exception Occured", e);
    }
  }

  @Test
  void testUserRegisterUnsuccessful() {
    try {
      SocketNB socket = new SocketNB("localhost", PORT);
      ScanNetNB scan = new ScanNetNB(socket);
      String username = "testName";

      when(mockUserDao.checkUserExists(anyString(), anyString())).thenReturn(true);
      when(mockUserDao.create(any(User.class))).thenReturn(new User("",""));

      Method userRegister = scan.getClass().getDeclaredMethod("userRegister", String.class, String.class, String.class, String.class, UserDao.class);
      userRegister.invoke(scan, "testSender", "testMessage", "testServerMessage", "HLO", mockUserDao);

      Queue<Message> messages = scan.getMessages();
      assertEquals("HLO 10 testSender 92 Error : User already exists. Try giving the password on which user testSender was registered",
              messages.peek().toString());

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,"Exception Occured", e);
    }
  }

  @Test
  void testLoginNew() {
    try {
      SocketNB socket = new SocketNB("localhost", PORT);
      ScanNetNB scan = new ScanNetNB(socket);
      String username = "testName";

      List<String > unreadMessages = new ArrayList<String>();
      unreadMessages.add("test1");
      unreadMessages.add("test2");
      unreadMessages.add("test3");

      when(mockUserDao.checkUserExists(anyString(), anyString())).thenReturn(true);
      when(mockUserDao.getUnreadMessages("testSender", GroupMessageDao.getInstance())).thenReturn(unreadMessages);

      Method userRegister = scan.getClass().getDeclaredMethod("login", String.class, String.class, String.class, String.class, UserDao.class);
      userRegister.invoke(scan, " testSender", "testMessage", "testServerMessage", "HLO", mockUserDao);
      
      
      Queue<Message> messages = scan.getMessages();
      System.out.println(messages.toString());
      assertEquals("HLO 10 testSender 93 Success : User testSender logged in\n" +
                      " You have the below messages unread [test1, test2, test3]",
              messages.peek().toString());

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,"Exception Occured", e);
    }
  }

  @Test
  void testLoginNoMessages() {
    try {
      SocketNB socket = new SocketNB("localhost", PORT);
      ScanNetNB scan = new ScanNetNB(socket);
      String username = "testName";

      List<String > unreadMessages = new ArrayList<String>();

      when(mockUserDao.checkUserExists(anyString(), anyString())).thenReturn(true);
      when(mockUserDao.getUnreadMessages("testSender", GroupMessageDao.getInstance())).thenReturn(unreadMessages);

      Method userRegister = scan.getClass().getDeclaredMethod("login", String.class, String.class, String.class, String.class, UserDao.class);
      userRegister.invoke(scan, " testSender", "testMessage", "testServerMessage", "HLO", mockUserDao);

      Queue<Message> messages = scan.getMessages();
      assertEquals("HLO 10 testSender 64 Success : User testSender logged in\n" +
                      " You have no unread messages",
              messages.peek().toString());

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,"Exception Occured", e);
    }
  }

  @Test
  void testLoginInvalid() {
    try {
      SocketNB socket = new SocketNB("localhost", PORT);
      ScanNetNB scan = new ScanNetNB(socket);
      String username = "testName";

      when(mockUserDao.checkUserExists(anyString(), anyString())).thenReturn(false);

      Method userRegister = scan.getClass().getDeclaredMethod("login", String.class, String.class, String.class, String.class, UserDao.class);
      userRegister.invoke(scan, " testSender", "testMessage", "testServerMessage", "HLO", mockUserDao);

      Queue<Message> messages = scan.getMessages();
      assertEquals("HLO 10 testSender 47 Error : Invalid credentials for user testSender",
              messages.peek().toString());

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,"Exception Occured", e);
    }
  }


  @Test
  void testSetHandlee() {
	  ScanNetNB scan = null;
	  try {
	      SocketNB socket = new SocketNB("localhost", PORT);
	       scan = new ScanNetNB(socket);
	      scan.setHandlee("message");
	  } catch (Exception e) {
	      LOGGER.log(Level.SEVERE,"Exception Occured", e);
	    }
	  assertEquals("message", scan.getHandlee());
	      
  }
  
  @Test
  void testGetHandlee() {
	  ScanNetNB scan = null;
	  try {
	      SocketNB socket = new SocketNB("localhost", PORT);
	       scan = new ScanNetNB(socket);
	      scan.setHandlee("message");
	  } catch (Exception e) {
	      LOGGER.log(Level.SEVERE,"Exception Occured", e);
	    }
	  assertEquals("message", scan.getHandlee());
	      
  }
  
	@Test
	public void Register3() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			ScanNetNB scan = new ScanNetNB(socket);
			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			CharBuffer buffer = CharBuffer.allocate(100);
			String string = "4 1 3";
			for (int i = 0; i < string.length(); i++) {
				buffer.put(string.charAt(i));
			}
			buffer.flip();
			scan.loginRegisterFunc("HLO","AGENCYAli","Ednn4/1","",buffer);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}

	@Test
	public void Register4() {
		try {
			SocketNB socket = new SocketNB("localhost", PORT);
			ScanNetNB scan = new ScanNetNB(socket);
			//Access messages variable of ScanNetNB
			Field messages = scan.getClass().getDeclaredField("messages");
			messages.setAccessible(true);
			CharBuffer buffer = CharBuffer.allocate(100);
			String string = "4 1 0";
			for (int i = 0; i < string.length(); i++) {
				buffer.put(string.charAt(i));
			}
			buffer.flip();
			scan.loginRegisterFunc("HLO","AGENCYAli","Ednn4/1","",buffer);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
	}
	
	@Test
	public void ScanNetNBgetChannelTest() {
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
			

			ScanNetNB scan = new ScanNetNB(soc);
			
			System.out.println(scan.getChannel());
			System.out.println(soc);
			assertEquals(soc.getLocalAddress().toString(), scan.getChannel().getLocalAddress().toString());
			soc.close();

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE,"Exception Occured", e);
		}
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
}